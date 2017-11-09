/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.cdi.extensions;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.PasswordHash;
import javax.security.enterprise.identitystore.Pbkdf2PasswordHash;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.security.javaeesec.CDIHelper;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.CustomFormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.FormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.properties.ModuleProperties;
import com.ibm.ws.threadContext.ModuleMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.security.metadata.LoginConfiguration;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;

/**
 * TODO: Add all JSR-375 API classes that can be bean types to api.classes.
 *
 * @param <T>
 */
@Component(service = { WebSphereCDIExtension.class},
           property = { "api.classes=javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;javax.security.enterprise.identitystore.IdentityStore;javax.security.enterprise.identitystore.IdentityStoreHandler;javax.security.enterprise.identitystore.RememberMeIdentityStore;javax.security.enterprise.SecurityContext;com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider" },
           immediate = true)
public class JavaEESecCDIExtension<T> implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(JavaEESecCDIExtension.class);

    // TODO: Track beans by annotated type
    private final Set<Bean> beansToAdd = new HashSet<Bean>();
    private boolean identityStoreHandlerRegistered = false;
    private boolean identityStoreRegistered = false;
    private final Annotation loginToContinue = null;
    private final Set<Class<?>> authMechRegistered = new HashSet<Class<?>>();
    private final Map<String, ModuleProperties> moduleMap = new HashMap<String, ModuleProperties>(); // map of module name and list of authmechs.
    private final List<LdapIdentityStoreDefinition> ldapDefinitionList = new ArrayList<LdapIdentityStoreDefinition>();

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType, BeanManager beanManager) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "processAnnotatedType : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "processAnnotatedType : annotation : " + annotatedType);

        Class<?> javaClass = annotatedType.getJavaClass();
        if (isApplicationAuthMech(javaClass)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Found an application specific HttpAuthenticationMechanism : " + javaClass);
            }
            authMechRegistered.add(javaClass);
            createModulePropertiesProviderBeanForApplicationAuthMechToAdd(beanManager, javaClass);
            // need to create a HAMProperties
        }
        //look at the class level annotations
        Set<Annotation> annotations = annotatedType.getAnnotations();
        for (Annotation annotation : annotations) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Annotations found: " + annotation);
                Tr.debug(tc, "Annotation class: ", annotation.getClass());
            }

            // TODO: If I see my annotations, create beans by type. Add more bean types.
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (BasicAuthenticationMechanismDefinition.class.equals(annotationType)) {
                createModulePropertiesProviderBeanForBasicToAdd(beanManager, annotation, annotationType, javaClass);
                authMechRegistered.add(annotationType);
            } else if (FormAuthenticationMechanismDefinition.class.equals(annotationType) || CustomFormAuthenticationMechanismDefinition.class.equals(annotationType)) {
                createModulePropertiesProviderBeanForFormToAdd(beanManager, annotation, annotationType, javaClass);
                authMechRegistered.add(annotationType);
            } else if (LdapIdentityStoreDefinition.class.equals(annotationType)) {
                createLdapIdentityStoreBeanToAdd(beanManager, annotation, annotationType);
                identityStoreRegistered = true;
            } else if (DatabaseIdentityStoreDefinition.class.equals(annotationType)) {
                createDatabaseIdentityStoreBeanToAdd(beanManager, annotation, annotationType);
                identityStoreRegistered = true;
            }
        }
    }

    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery beforeBeanDiscovery, BeanManager beanManager) {
        AnnotatedType<SecurityContextProducer> securityContextProducerType = beanManager.createAnnotatedType(SecurityContextProducer.class);
        beforeBeanDiscovery.addAnnotatedType(securityContextProducerType);
        AnnotatedType<AutoApplySessionInterceptor> autoApplySessionInterceptorType = beanManager.createAnnotatedType(AutoApplySessionInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(autoApplySessionInterceptorType);
        AnnotatedType<RememberMeInterceptor> rememberMeInterceptorInterceptorType = beanManager.createAnnotatedType(RememberMeInterceptor.class);
        beforeBeanDiscovery.addAnnotatedType(rememberMeInterceptorInterceptorType);
    }

    <T> void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (tc.isDebugEnabled()) Tr.debug(tc, "afterBeanDiscovery : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        try {
            verifyConfiguration();
            if (!identityStoreHandlerRegistered) {
                if (identityStoreRegistered) {
                    beansToAdd.add(new IdentityStoreHandlerBean(beanManager));
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "registering the default IdentityStoreHandler.");
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "IdentityStoreHander is not registered because none of the identity store has been registered.");
                }
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "IdentityStoreHandler is not registered because a custom IdentityStoreHandler has been registered,");
            }
        } catch (DeploymentException de) {
            afterBeanDiscovery.addDefinitionError(de);
        }
        if (!moduleMap.isEmpty()) {
            ModulePropertiesProviderBean bean = new ModulePropertiesProviderBean(beanManager, moduleMap);
            beansToAdd.add(bean);
        }

        // TODO: Validate beans to add.
        for (Bean bean : beansToAdd) {
            afterBeanDiscovery.addBean(bean);
        }
    }

    void processBean(@Observes ProcessBean<?> processBean, BeanManager beanManager) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "processBean : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        if (!identityStoreHandlerRegistered) {
            if (isIdentityStoreHandler(processBean)) {
                identityStoreHandlerRegistered = true;
            }
        }
        if (!identityStoreRegistered) {
            if (isIdentityStore(processBean)) {
                identityStoreRegistered = true;
            }
        }
    }

    /**
     * @param <T>
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private <T> void createModulePropertiesProviderBeanForFormToAdd(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType, Class annotatedClass) {
        try {
            Method loginToContinueMethod = annotationType.getMethod("loginToContinue");
            Annotation ltcAnnotation = (Annotation) loginToContinueMethod.invoke(annotation);
            Properties props = parseLoginToContinue(ltcAnnotation);
            Class implClass;
            if (FormAuthenticationMechanismDefinition.class.equals(annotationType)) {
                implClass = FormAuthenticationMechanism.class;
            } else {
                implClass = CustomFormAuthenticationMechanism.class;
            }
            addAuthMech(annotatedClass, implClass, props);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
    }

    /**
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private void createModulePropertiesProviderBeanForBasicToAdd(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType, Class annotatedClass) {
        try {
            Method realmNameMethod = annotationType.getMethod("realmName");
            String realmName = (String) realmNameMethod.invoke(annotation);
            Properties props = new Properties();
            props.put(JavaEESecConstants.REALM_NAME, realmName);
            addAuthMech(annotatedClass, BasicHttpAuthenticationMechanism.class, props);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
    }

    /**
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private void createModulePropertiesProviderBeanForApplicationAuthMechToAdd(BeanManager beanManager, Class implClass) {
        Map<String, ModuleProperties> moduleMap = getModuleMap();
        String moduleName = getModuleFromClass(implClass);
        // if there is a match for the module name, place the class in it, otherwise, place the class
        // to all of the modules. It is OK to do so since it can be instanciated only in the appropriate module.
        if (moduleMap.containsKey(moduleName)) {
            ModuleProperties mp = moduleMap.get(moduleName);
            mp.putToAuthMechMap(implClass, null);
            if (tc.isDebugEnabled()) Tr.debug(tc, "found the module in the map. " + mp);
        } else {
            for (Map.Entry<String, ModuleProperties> entry : moduleMap.entrySet()) {
                entry.getValue().putToAuthMechMap(implClass, null);
            }
        }
    }

    private void addAuthMech(Class annotatedClass, Class implClass, Properties props) {
        Map<String, ModuleProperties> moduleMap = getModuleMap();
        String moduleName = getModuleFromClass(annotatedClass);
        if (moduleMap.containsKey(moduleName)) {
            moduleMap.get(moduleName).putToAuthMechMap(implClass, props);
        } else {
            if (tc.isDebugEnabled()) Tr.debug(tc, "The module is not found. A new entry is created. Module: " + moduleName);
            ModuleProperties mp = new ModuleProperties();
            mp.getAuthMechMap().put(implClass, props);
            moduleMap.put(moduleName, mp);
        }
    }

    private Map<Class, Properties> getAuthMechs(String moduleName) {
        Map<Class, Properties> authMechs = null;
        Map<String, ModuleProperties> moduleMap = getModuleMap();
        if (moduleMap.containsKey(moduleName)) {
            authMechs = moduleMap.get(moduleName).getAuthMechMap();
        }
        return authMechs;
    }

    /**
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private Properties parseLoginToContinue(Annotation ltcAnnotation) throws Exception {
        Properties props = new Properties();
        Class<? extends Annotation> ltcAnnotationType = ltcAnnotation.annotationType();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, getAnnotatedString(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE));
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, getAnnotatedString(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE));
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION,
                  getAnnotatedString(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION));
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN,
                  getAnnotatedBoolean(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN));
        return props;
    }

    private String getAnnotatedString(final Annotation annotation, final Class<? extends Annotation> annotationType, final String element) throws Exception {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
                    return (String) annotationType.getMethod(element).invoke(annotation);
                }
            });
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
    }

    private boolean getAnnotatedBoolean(final Annotation annotation, final Class<? extends Annotation> annotationType, final String element) throws Exception {
        try {
            Boolean result = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
                    return (Boolean) annotationType.getMethod(element).invoke(annotation);
                }
            });
            return result.booleanValue();
        } catch (PrivilegedActionException e) {
            throw e.getException();
        }
    }

    /**
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private void createLdapIdentityStoreBeanToAdd(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType) {
        try {
            Map<String, Object> identityStoreProperties = new HashMap<String, Object>();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "JavaEESec.createLdapISBeanToAdd");
            Method[] methods = annotationType.getMethods();
            for (Method m : methods) {
                Tr.debug(tc, m.getName());
                if (!m.getName().equals("equals"))
                    identityStoreProperties.put(m.getName(), m.invoke(annotation));
            }
            LdapIdentityStoreDefinition ldapDefinition = getInstanceOfAnnotation(identityStoreProperties);
            if (!containsLdapDefinition(ldapDefinition, ldapDefinitionList)) {
                ldapDefinitionList.add(ldapDefinition);
                LdapIdentityStoreBean bean = new LdapIdentityStoreBean(beanManager, ldapDefinition);
                beansToAdd.add(bean);
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "registering the default LdapIdentityStore.");
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "the same annotation exists, skip registering..");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
    }

    private boolean containsLdapDefinition(LdapIdentityStoreDefinition ldapDefinition, List<LdapIdentityStoreDefinition> ldapDefinitionList) {
        for(LdapIdentityStoreDefinition lisd : ldapDefinitionList) {
            if (equalsLdapDefinition(ldapDefinition, lisd)) {
                return true;
            }
        }
        return false;
    }

    private LdapIdentityStoreDefinition getInstanceOfAnnotation(final Map<String, Object> overrides) {
        LdapIdentityStoreDefinition annotation = new LdapIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String bindDn() {
                return (overrides != null && overrides.containsKey("bindDn")) ? (String) overrides.get("bindDn") : "";
            }

            @Override
            public String bindDnPassword() {
                return (overrides != null && overrides.containsKey("bindDnPassword")) ? (String) overrides.get("bindDnPassword") : "";
            }

            @Override
            public String callerBaseDn() {
                return (overrides != null && overrides.containsKey("callerBaseDn")) ? (String) overrides.get("callerBaseDn") : "";
            }

            @Override
            public String callerNameAttribute() {
                return (overrides != null && overrides.containsKey("callerNameAttribute")) ? (String) overrides.get("callerNameAttribute") : "uid";
            }

            @Override
            public String callerSearchBase() {
                return (overrides != null && overrides.containsKey("callerSearchBase")) ? (String) overrides.get("callerSearchBase") : "";
            }

            @Override
            public String callerSearchFilter() {
                return (overrides != null && overrides.containsKey("callerSearchFilter")) ? (String) overrides.get("callerSearchFilter") : "";

            }

            @Override
            public LdapSearchScope callerSearchScope() {
                return (overrides != null && overrides.containsKey("callerSearchScope")) ? (LdapSearchScope) overrides.get("callerSearchScope") : LdapSearchScope.SUBTREE;
            }

            @Override
            public String callerSearchScopeExpression() {
                return (overrides != null && overrides.containsKey("callerSearchScopeExpression")) ? (String) overrides.get("callerSearchScopeExpression") : "";
            }

            @Override
            public String groupMemberAttribute() {
                return (overrides != null && overrides.containsKey("groupMemberAttribute")) ? (String) overrides.get("groupMemberAttribute") : "member";
            }

            @Override
            public String groupMemberOfAttribute() {
                return (overrides != null && overrides.containsKey("groupMemberOfAttribute")) ? (String) overrides.get("groupMemberOfAttribute") : "memberOf";
            }

            @Override
            public String groupNameAttribute() {
                return (overrides != null && overrides.containsKey("groupNameAttribute")) ? (String) overrides.get("groupNameAttribute") : "cn";
            }

            @Override
            public String groupSearchBase() {
                return (overrides != null && overrides.containsKey("groupSearchBase")) ? (String) overrides.get("groupSearchBase") : "";
            }

            @Override
            public String groupSearchFilter() {
                return (overrides != null && overrides.containsKey("groupSearchFilter")) ? (String) overrides.get("groupSearchFilter") : "";
            }

            @Override
            public LdapSearchScope groupSearchScope() {
                return (overrides != null && overrides.containsKey("groupSearchScope")) ? (LdapSearchScope) overrides.get("groupSearchScope") : LdapSearchScope.SUBTREE;
            }

            @Override
            public String groupSearchScopeExpression() {
                return (overrides != null && overrides.containsKey("groupSearchScopeExpression")) ? (String) overrides.get("groupSearchScopeExpression") : "";
            }

            @Override
            public int maxResults() {
                return (overrides != null && overrides.containsKey("maxResults")) ? (Integer) overrides.get("maxResults") : 1000;
            }

            @Override
            public String maxResultsExpression() {
                return (overrides != null && overrides.containsKey("maxResultsExpression")) ? (String) overrides.get("maxResultsExpression") : "";
            }

            @Override
            public int priority() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY)) ? (Integer) overrides.get(JavaEESecConstants.PRIORITY) : 80;
            }

            @Override
            public String priorityExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.PRIORITY_EXPRESSION) : "";
            }

            @Override
            public int readTimeout() {
                return (overrides != null && overrides.containsKey("readTimeout")) ? (Integer) overrides.get("readTimeout") : 0;
            }

            @Override
            public String readTimeoutExpression() {
                return (overrides != null && overrides.containsKey("readTimeoutExpression")) ? (String) overrides.get("readTimeoutExpression") : "";
            }

            @Override
            public String url() {
                return (overrides != null && overrides.containsKey("url")) ? (String) overrides.get("url") : "";
            }

            @Override
            public ValidationType[] useFor() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.USE_FOR)) ? (ValidationType[]) overrides.get(JavaEESecConstants.USE_FOR) : new ValidationType[] { ValidationType.PROVIDE_GROUPS,
                                                                                                                                                                      ValidationType.VALIDATE };
            }

            @Override
            public String useForExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.USE_FOR_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.USE_FOR_EXPRESSION) : "";
            }

        };

        return annotation;
    }

    protected boolean equalsLdapDefinition(final LdapIdentityStoreDefinition lisd1, final LdapIdentityStoreDefinition lisd2) {
        return lisd1.bindDn().equals(lisd2.bindDn()) &&
               lisd1.bindDnPassword().equals(lisd2.bindDnPassword()) &&
               lisd1.callerBaseDn().equals(lisd2.callerBaseDn()) &&
               lisd1.callerNameAttribute().equals(lisd2.callerNameAttribute()) &&
               lisd1.callerSearchBase().equals(lisd2.callerSearchBase()) &&
               lisd1.callerSearchFilter().equals(lisd2.callerSearchFilter()) &&
               lisd1.callerSearchScope().equals(lisd2.callerSearchScope()) &&
               lisd1.callerSearchScopeExpression().equals(lisd2.callerSearchScopeExpression()) &&
               lisd1.groupMemberAttribute().equals(lisd2.groupMemberAttribute()) &&
               lisd1.groupMemberOfAttribute().equals(lisd2.groupMemberOfAttribute()) &&
               lisd1.groupNameAttribute().equals(lisd2.groupNameAttribute()) &&
               lisd1.groupSearchBase().equals(lisd2.groupSearchBase()) &&
               lisd1.groupSearchFilter().equals(lisd2.groupSearchFilter()) &&
               lisd1.groupSearchScope().equals(lisd2.groupSearchScope()) &&
               lisd1.groupSearchScopeExpression().equals(lisd2.groupSearchScopeExpression()) &&
               (lisd1.maxResults() == lisd2.maxResults()) &&
               lisd1.maxResultsExpression().equals(lisd2.maxResultsExpression()) &&
               (lisd1.priority() == lisd2.priority()) &&
               lisd1.priorityExpression().equals(lisd2.priorityExpression()) &&
               (lisd1.readTimeout() == lisd2.readTimeout()) &&
               lisd1.readTimeoutExpression().equals(lisd2.readTimeoutExpression()) &&
               lisd1.url().equals(lisd2.url()) &&
               equalsUseFor(lisd1.useFor(), lisd2.useFor()) &&
               lisd1.useForExpression().equals(lisd2.useForExpression());
    }

    private boolean equalsUseFor(ValidationType[] vt1, ValidationType[] vt2) {
        if (vt1 == vt2) {
            return true;
        } else if ((vt1.length == vt2.length) && (vt1.length ==1)) {
            return vt1[0] == vt2[0];
        } else {
            List<ValidationType> list1 = Arrays.asList(vt1);
            List<ValidationType> list2 = Arrays.asList(vt2);
            return (list1.contains(ValidationType.PROVIDE_GROUPS) == list2.contains(ValidationType.PROVIDE_GROUPS)) &&
                   (list1.contains(ValidationType.VALIDATE) == list2.contains(ValidationType.VALIDATE));
        }
    }

    /**
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private void createDatabaseIdentityStoreBeanToAdd(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType) {
        try {
            Map<String, Object> identityStoreProperties = new HashMap<String, Object>();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "JavaEESec.createDatabaseIdentityStoreBeanToAdd");
            Method[] methods = annotationType.getMethods();
            for (Method m : methods) {
                Tr.debug(tc, m.getName());
                if (!m.getName().equals("equals"))
                    identityStoreProperties.put(m.getName(), m.invoke(annotation));
            }
            DatabaseIdentityStoreBean bean = new DatabaseIdentityStoreBean(beanManager, getInstanceOfDBAnnotation(identityStoreProperties));
            beansToAdd.add(bean);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "registering the default DatabaseIdentityStore.");
        } catch (InvocationTargetException | IllegalAccessException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "unexpected", e);
            }
        }
    }

    protected boolean isIdentityStoreHandler(ProcessBean<?> processBean) {
        Bean<?> bean = processBean.getBean();
        // skip interface class.
        if (bean.getBeanClass() != IdentityStoreHandler.class) {
            Set<Type> types = bean.getTypes();
            if (types.contains(IdentityStoreHandler.class)) {
                // if it's not already registered.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "found a custom IdentityStoreHandler : " + bean.getBeanClass());
                return true;
            }
        }
        return false;
    }

    protected boolean isIdentityStore(ProcessBean<?> processBean) {
        Bean<?> bean = processBean.getBean();
        // skip interface class.
        if (bean.getBeanClass() != IdentityStore.class) {
            Set<Type> types = bean.getTypes();
            if (types.contains(IdentityStore.class)) {
                // if it's not already registered.
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "found a custom IdentityStore : " + bean.getBeanClass());
                return true;
            }
        }
        return false;
    }

    /**
     * This is for the unit test.
     */
    protected Set<Bean> getBeansToAdd() {
        return beansToAdd;
    }

    /**
     * This is for the unit test.
     */
    protected boolean getIdentityStoreHandlerRegistered() {
        return identityStoreHandlerRegistered;
    }

    /**
     * This is for the unit test.
     */
    protected boolean getIdentityStoreRegistered() {
        return identityStoreRegistered;
    }

    protected boolean isApplicationAuthMech(Class<?> javaClass) {
        if (!BasicHttpAuthenticationMechanism.class.equals(javaClass) && !FormAuthenticationMechanism.class.equals(javaClass)
            && !CustomFormAuthenticationMechanism.class.equals(javaClass)) {
            Class<?>[] interfaces = javaClass.getInterfaces();
            for (Class<?> interfaceClass : interfaces) {
                if (HttpAuthenticationMechanism.class.equals(interfaceClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    private DatabaseIdentityStoreDefinition getInstanceOfDBAnnotation(final Map<String, Object> overrides) {
        DatabaseIdentityStoreDefinition annotation = new DatabaseIdentityStoreDefinition() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String callerQuery() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.CALLER_QUERY)) ? (String) overrides.get(JavaEESecConstants.CALLER_QUERY) : "";
            }

            @Override
            public String dataSourceLookup() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.DS_LOOKUP)) ? (String) overrides.get(JavaEESecConstants.DS_LOOKUP) : JavaEESecConstants.DEFAULT_DS_NAME;
            }

            @Override
            public String groupsQuery() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.GROUPS_QUERY)) ? (String) overrides.get(JavaEESecConstants.GROUPS_QUERY) : "";
            }

            @Override
            public Class<? extends PasswordHash> hashAlgorithm() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.PWD_HASH_ALGORITHM)) ? (Class<? extends PasswordHash>) overrides.get(JavaEESecConstants.PWD_HASH_ALGORITHM) : Pbkdf2PasswordHash.class;
            }

            @Override
            public String[] hashAlgorithmParameters() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.PWD_HASH_PARAMETERS)) ? (String[]) overrides.get(JavaEESecConstants.PWD_HASH_PARAMETERS) : new String[] {};
            }

            @Override
            public int priority() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY)) ? (Integer) overrides.get(JavaEESecConstants.PRIORITY) : 70;
            }

            @Override
            public String priorityExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.PRIORITY_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.PRIORITY_EXPRESSION) : "";
            }

            @Override
            public ValidationType[] useFor() {
                return (overrides != null
                        && overrides.containsKey(JavaEESecConstants.USE_FOR)) ? (ValidationType[]) overrides.get(JavaEESecConstants.USE_FOR) : new ValidationType[] { ValidationType.PROVIDE_GROUPS,
                                                                                                                                                                      ValidationType.VALIDATE };
            }

            @Override
            public String useForExpression() {
                return (overrides != null && overrides.containsKey(JavaEESecConstants.USE_FOR_EXPRESSION)) ? (String) overrides.get(JavaEESecConstants.USE_FOR_EXPRESSION) : "";
            }
        };
        return annotation;
    }

    private Map<String, ModuleProperties> getModuleMap() {
        if (moduleMap.isEmpty()) {
            initModuleMap();
        }
        return moduleMap;
    }

    protected void initModuleMap() {
        if (moduleMap.isEmpty()) {
            List<String> wml = getWebModuleList();
            if (wml != null) {
                for(String name : wml) {
                    moduleMap.put(name, new ModuleProperties());
                }
            }
        }
    }

    private List<String> getWebModuleList() {
        List<ModuleMetaData> mmds = getModuleMetaDataList();
        List<String> list = null; 
        if (mmds != null) {
            list = new ArrayList<String>();
            for(ModuleMetaData mmd : mmds) {
                if (mmd instanceof WebModuleMetaData) {
                    String j2eeModuleName = mmd.getJ2EEName().getModule();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "j2ee module name  : " + j2eeModuleName);
                    }
                    list.add(j2eeModuleName);
                }
            }
        }
        return list;
    }

    /**
      * make sure that there is one HAM for each modules, and if there is a HAM in a module, make sure there is no login configuration in web.xml.
     **/
    private void verifyConfiguration() throws DeploymentException {
        List<ModuleMetaData> mmds = getModuleMetaDataList();
        if (mmds != null) {
            for(ModuleMetaData mmd : mmds) {
                if (mmd instanceof WebModuleMetaData) {
                    String j2eeModuleName = mmd.getJ2EEName().getModule();
                    Map<Class, Properties> authMechs = getAuthMechs(j2eeModuleName);
                    if (authMechs != null && !authMechs.isEmpty()) {
                        // make sure that only one HAM.
                        if (authMechs.size() != 1) {
                            String appName = mmd.getJ2EEName().getApplication();
                            String authMechNames = getAuthMechNames(authMechs);
                            Tr.error(tc, "JAVAEESEC_CDI_ERROR_MULTIPLE_HTTPAUTHMECHS", j2eeModuleName, appName, authMechNames);
                            String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_MULTIPLE_HTTPAUTHMECHS", j2eeModuleName, appName, authMechNames);
                            throw new DeploymentException(msg);
                        }
                    
                        SecurityMetadata smd = (SecurityMetadata)((WebModuleMetaData)mmd).getSecurityMetaData();
                        if (smd != null) {
                            LoginConfiguration lc =  smd.getLoginConfiguration();
                            if (lc != null  && !lc.isAuthenticationMethodDefaulted()) {
                                String appName = mmd.getJ2EEName().getApplication();
                                String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_LOGIN_CONFIG_EXISTS", j2eeModuleName, appName);
                                Tr.error(tc, "JAVAEESEC_CDI_ERROR_LOGIN_CONFIG_EXISTS", j2eeModuleName, appName);
                                throw new DeploymentException(msg);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getAuthMechNames(Map<Class, Properties> authMechs) {
        StringBuffer result = new StringBuffer();
        boolean first = true;
        for (Class authMech : authMechs.keySet()) {
            if (first) {
                first = false;
            } else {
                result.append(", ");
            }
            result.append(authMech.getName());
        }
        return result.toString();
    }

    private String getApplicationName() {
        String result = null;
        List<ModuleMetaData> mmds = getModuleMetaDataList();
        if (mmds != null && !mmds.isEmpty()) {
            result = mmds.get(0).getJ2EEName().getApplication();
        }
        return result;
    }

    protected List<ModuleMetaData> getModuleMetaDataList() {
        return ModuleMetaDataAccessorImpl.getModuleMetaDataAccessor().getModuleMetaDataList();
    }

    private String getModuleFromClass(Class<?> klass) {
        String file = klass.getProtectionDomain().getCodeSource().getLocation().getFile();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "File name  : " + file);
        }
        int index = file.lastIndexOf("/");
        String moduleName;
        if (index > 0) {
            moduleName = file.substring(index + 1);
        } else {
            moduleName = file;
        }
        return moduleName;
    }

}
