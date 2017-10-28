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
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;
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
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.wsspi.webcontainer.servlet.IServletConfig;

// TODO:
// Find out how to release LoginToContinue annotation in LoginToContinueIntercepter by the one in FormAuthenticationMechanismDefinition.

/**
 * TODO: Add all JSR-375 API classes that can be bean types to api.classes.
 *
 * @param <T>
 */
@Component(service = { WebSphereCDIExtension.class, ApplicationStateListener.class, ModuleMetaDataListener.class},
           property = { "api.classes=javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;javax.security.enterprise.identitystore.IdentityStore;javax.security.enterprise.identitystore.IdentityStoreHandler;javax.security.enterprise.identitystore.RememberMeIdentityStore;javax.security.enterprise.SecurityContext;com.ibm.ws.security.javaeesec.properties.ModulePropertiesProvider" },
           immediate = true)
public class JavaEESecCDIExtension<T> implements Extension, WebSphereCDIExtension, ApplicationStateListener, ModuleMetaDataListener {

    private static final TraceComponent tc = Tr.register(JavaEESecCDIExtension.class);

    // TODO: Track beans by annotated type
    private final Set<Bean> beansToAdd = new HashSet<Bean>();
    private boolean identityStoreHandlerRegistered = false;
    private boolean identityStoreRegistered = false;
    private final Annotation loginToContinue = null;
    private final Set<Class<?>> authMechRegistered = new HashSet<Class<?>>();
    private final Map<String, ModuleProperties> moduleMap = new HashMap<String, ModuleProperties>(); // map of module name and list of authmechs.

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType, BeanManager beanManager) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "processAnnotatedType : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        Class<?> javaClass = annotatedType.getJavaClass();

        if (isApplicationAuthMech(javaClass)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Found an application specific HttpAuthenticationMechanism : " + javaClass);
            }
            authMechRegistered.add(javaClass);
            createModulePropertiesProviderBeanForApplicationAuthMechToAdd(beanManager, javaClass);
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
//            } else if (DatabaseIdentityStoreDefinition.class.equals(annotationType)) {
//                createdatabaseIdentityStoreBeanToAdd(beanManager, annotation, annotationType);
//                identityStoreRegistered = true;
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
            verifyNoLoginConfigration();
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
            if (tc.isDebugEnabled()) Tr.debug(tc, "found the module in the map.");
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
            LdapIdentityStoreBean bean = new LdapIdentityStoreBean(beanManager, getInstanceOfAnnotation(identityStoreProperties));
            beansToAdd.add(bean);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "registering the default LdapIdentityStore.");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
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
                return (overrides != null && overrides.containsKey("priority")) ? (Integer) overrides.get("priority") : 80;
            }

            @Override
            public String priorityExpression() {
                return (overrides != null && overrides.containsKey("priorityExpression")) ? (String) overrides.get("priorityExpression") : "";
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
                return (overrides != null && overrides.containsKey("useFor")) ? (ValidationType[]) overrides.get("useFor") : new ValidationType[] { ValidationType.PROVIDE_GROUPS,
                                                                                                                                                    ValidationType.VALIDATE };
            }

            @Override
            public String useForExpression() {
                return (overrides != null && overrides.containsKey("useForExpression")) ? (String) overrides.get("useForExpression") : "";
            }
        };

        return annotation;
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
//                    WebAppConfig wac = ((WebModuleMetaData)mmd).getConfiguration();
//                    String moduleName = wac.getModuleName();
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

    // TODO: need to support multiple modules which contains non JSR375 authmech.
    private void verifyNoLoginConfigration() throws DeploymentException {
        List<ModuleMetaData> mmds = getModuleMetaDataList();
        if (mmds != null) {
            for(ModuleMetaData mmd : mmds) {
                if (mmd instanceof WebModuleMetaData) {
                    parseWMMD((WebModuleMetaData)mmd);
                    SecurityMetadata smd = (SecurityMetadata)((WebModuleMetaData)mmd).getSecurityMetaData();
                    if (smd != null) {
                        LoginConfiguration lc =  smd.getLoginConfiguration();
                        if (lc != null  && !lc.isAuthenticationMethodDefaulted()) {
                            String appName = mmd.getJ2EEName().getApplication();
                            String j2eeModuleName = mmd.getJ2EEName().getModule();
                            String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_LOGIN_CONFIG_EXISTS", appName, j2eeModuleName);
                            Tr.error(tc, "JAVAEESEC_CDI_ERROR_LOGIN_CONFIG_EXISTS", appName, j2eeModuleName);
//                            throw new DeploymentException(msg);
                        }
                    }
                }
            }
        }
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

    private void parseWMMD(WebModuleMetaData wmmd) {
        WebAppConfig wac = wmmd.getConfiguration();
        String j2eeModuleName = wmmd.getJ2EEName().getModule();
        String appName = wac.getApplicationName();
        String ctxRoot = wac.getContextRoot();
        List<Class<?>> classes = wac.getClassesToScan();

        Iterator<IServletConfig> itr = wac.getServletInfos();
        
        while(itr.hasNext()) {
            IServletConfig isc = itr.next();
            String className = isc.getClassName();
        }
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



    /**
     * Notification that an application is starting.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    @Override
    public void applicationStarting(ApplicationInfo appInfo) throws StateChangeException {
        String appName = appInfo.getDeploymentName();
    }
    /**
     * Notification that an application has started.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    @Override
    public void applicationStarted(ApplicationInfo appInfo) throws StateChangeException {
        String appName = appInfo.getDeploymentName();
    }

    /**
     * Notification that an application is stopping.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    @Override
    public void applicationStopping(ApplicationInfo appInfo) {
        String appName = appInfo.getDeploymentName();
    }

    /**
     * Notification that an application has stopped.
     * 
     * @param appInfo The ApplicationInfo of the app
     */
    @Override
    public void applicationStopped(ApplicationInfo appInfo) {
        String appName = appInfo.getDeploymentName();
    }

    /**
     * Notification that the metadata for a module has been created.
     * 
     * @param event the event, with {@link MetaDataEvent#getMetaData} returning {@link DeployedMod}
     */
    @Override
    public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) throws MetaDataException {
        ModuleMetaData mmd = event.getMetaData();
        if (mmd instanceof WebModuleMetaData) {
            parseWMMD((WebModuleMetaData)mmd);
        }
    }
    /**
     * Notification that the metadata for a module has been destroyed. This event might be fired without a corresponding {@link #moduleMetaDataCreated} event if an error occurred
     * while creating the metadata.
     * 
     * @param event the event
     */
    @Override
    public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {}

}
