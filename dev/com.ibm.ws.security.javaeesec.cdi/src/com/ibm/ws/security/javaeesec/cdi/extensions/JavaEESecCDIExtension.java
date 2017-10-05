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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessBeanAttributes;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
import com.ibm.ws.security.javaeesec.cdi.beans.FormAuthenticationMechanism;
import com.ibm.ws.security.javaeesec.cdi.beans.CustomFormAuthenticationMechanism;

// TODO:
// Find out how to release LoginToContinue annotation in LoginToContinueIntercepter by the one in FormAuthenticationMechanismDefinition.

/**
 * TODO: Add all JSR-375 API classes that can be bean types to api.classes.
 *
 * @param <T>
 */
@Component(service = WebSphereCDIExtension.class,
           property = { "api.classes=javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;javax.security.enterprise.identitystore.IdentityStore;javax.security.enterprise.identitystore.IdentityStoreHandler;javax.security.enterprise.identitystore.RememberMeIdentityStore;javax.security.enterprise.SecurityContext;com.ibm.ws.security.javaeesec.authentication.mechanism.http.LoginToContinueProperties" },
           immediate = true)
public class JavaEESecCDIExtension<T> implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(JavaEESecCDIExtension.class);

    // TODO: Track beans by annotated type
    private final Set<Bean> beansToAdd = new HashSet<Bean>();
    private boolean identityStoreHandlerRegistered = false;
    private boolean identityStoreRegistered = false;
    private final Annotation loginToContinue = null;
    private final Set<Class<?>> authMechRegistered = new HashSet<Class<?>>();


    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType, BeanManager beanManager) {
        if (tc.isDebugEnabled()) Tr.debug(tc, "processAnnotatedType : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        Class<?> javaClass = annotatedType.getJavaClass();
        if (isApplicationAuthMech(javaClass)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Found an application specific HttpAuthenticationMechanism : " + javaClass);
            }
            authMechRegistered.add(javaClass);
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
                createBasicAuthenticationMechanismBeanToAdd(beanManager, annotation, annotationType);
                authMechRegistered.add(annotationType);
            } else if (FormAuthenticationMechanismDefinition.class.equals(annotationType) || CustomFormAuthenticationMechanismDefinition.class.equals(annotationType)) {
                createLoginToContinuePropertiesBeanToAdd(beanManager, annotation, annotationType, annotatedType);
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
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Registering producer." + securityContextProducerType);
    }

    <T> void afterBeanDiscovery(@Observes AfterBeanDiscovery afterBeanDiscovery, BeanManager beanManager) {
        if (tc.isDebugEnabled()) Tr.debug(tc, "afterBeanDiscovery : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        if (authMechRegistered.size() > 1) {
            // if multiple authmech is regsitered, set error condition.
            StringBuffer names = new StringBuffer();
            for (Class<?> authMech : authMechRegistered) {
                names.append(authMech.getName()).append(" ");
            }
            String msg = Tr.formatMessage(tc, "JAVAEESEC_CDI_ERROR_MULTIPLE_HTTPAUTHMECHS", names.toString());
            Tr.error(tc, "JAVAEESEC_CDI_ERROR_MULTIPLE_HTTPAUTHMECHS", names.toString());
            afterBeanDiscovery.addDefinitionError(new DeploymentException(msg));
        } else {
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
            // TODO: Validate beans to add.
            for (Bean bean : beansToAdd) {
                afterBeanDiscovery.addBean(bean);
            }
        }
    }

    <T> void processBeanAttributes(@Observes ProcessBeanAttributes processBeanAttributes, BeanManager beanManager) {
        if (tc.isDebugEnabled()) Tr.debug(tc, "processBeanAttributes : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
        Set <Type> toBeVetoed = new HashSet<Type>();
        if (isBasicAuthMechRegistered(authMechRegistered) || isApplicationAuthMechRegistered(authMechRegistered)) {
            toBeVetoed.add(FormAuthenticationMechanism.class);
            toBeVetoed.add(CustomFormAuthenticationMechanism.class);
        } else if (isFormAuthMechRegistered(authMechRegistered)) {
            toBeVetoed.add(CustomFormAuthenticationMechanism.class);
        } else {
            toBeVetoed.add(FormAuthenticationMechanism.class);
        }
        if (!toBeVetoed.isEmpty()) {
            // need to disable managed beans.
            BeanAttributes<T> attrs = processBeanAttributes.getBeanAttributes();
            Set<Type> types = attrs.getTypes();
            for (Type type : types) {
                for (Type veto : toBeVetoed) {
                    if (veto.equals(type)) {
                        processBeanAttributes.veto();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, veto + " is disabled since another HttpAuthorizationMechanism is registered.");
                    }
                }
            }
        }
    }

    void processBean(@Observes ProcessBean<?> processBean, BeanManager beanManager) {
        if (tc.isDebugEnabled()) Tr.debug(tc, "processBean : instance : " + Integer.toHexString(this.hashCode()) + " BeanManager : " + Integer.toHexString(beanManager.hashCode()));
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
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private void createBasicAuthenticationMechanismBeanToAdd(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType) {
        try {
            Method realmNameMethod = annotationType.getMethod("realmName");
            String realmName = (String) realmNameMethod.invoke(annotation);
            BasicAuthenticationMechanismBean bean = new BasicAuthenticationMechanismBean(realmName, beanManager);
            beansToAdd.add(bean);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
            e.printStackTrace();
        }
    }

    /**
     * @param <T>
     * @param beanManager
     * @param annotation
     * @param annotationType
     */
    private <T> void createLoginToContinuePropertiesBeanToAdd(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType,
                                                              AnnotatedType<T> annotatedType) {
        try {
            Method loginToContinueMethod = annotationType.getMethod("loginToContinue");
            Annotation ltcAnnotation = (Annotation) loginToContinueMethod.invoke(annotation);
            Properties props = parseLoginToContinue(ltcAnnotation);
            LoginToContinuePropertiesBean bean = new LoginToContinuePropertiesBean(beanManager, ltcAnnotation, props);
            beansToAdd.add(bean);
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
    private Properties parseLoginToContinue(Annotation ltcAnnotation) throws Exception {
        Properties props = new Properties();
        Class<? extends Annotation> ltcAnnotationType = ltcAnnotation.annotationType();
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE, getAnnotatedString(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_LOGINPAGE));
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE, getAnnotatedString(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_ERRORPAGE));
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION,
                  getAnnotatedString(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGINEXPRESSION));
        props.put(JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN, getAnnotatedBoolean(ltcAnnotation, ltcAnnotationType, JavaEESecConstants.LOGIN_TO_CONTINUE_USEFORWARDTOLOGIN));
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
            LdapIdentityStoreBean bean = new LdapIdentityStoreBean(beanManager);
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
    protected boolean isBasicAuthMechRegistered(Set<Class<?>> authMechRegistered) {
        return isAuthMechRegistered(authMechRegistered, BasicAuthenticationMechanismDefinition.class);
    }
    protected boolean isFormAuthMechRegistered(Set<Class<?>> authMechRegistered) {
        return isAuthMechRegistered(authMechRegistered, FormAuthenticationMechanismDefinition.class);
    }
    protected boolean isCustomFormAuthMechRegistered(Set<Class<?>> authMechRegistered) {
        return isAuthMechRegistered(authMechRegistered, CustomFormAuthenticationMechanismDefinition.class);
    }

    protected boolean isAuthMechRegistered(Set<Class<?>> authMechRegistered, Class<?> authMech) {
        if (authMechRegistered.size() == 1 && authMechRegistered.contains(authMech)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isApplicationAuthMechRegistered(Set<Class<?>> authMechRegistered) {
        if (authMechRegistered.size() == 1 && !authMechRegistered.contains(BasicAuthenticationMechanismDefinition.class) && !authMechRegistered.contains(FormAuthenticationMechanismDefinition.class) && !authMechRegistered.contains(CustomFormAuthenticationMechanismDefinition.class)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isApplicationAuthMech(Class<?> javaClass) {
        if (!FormAuthenticationMechanism.class.equals(javaClass) && !CustomFormAuthenticationMechanism.class.equals(javaClass)) {
            Class<?>[] interfaces = javaClass.getInterfaces();
            for (Class<?> interfaceClass : interfaces) {
                if (HttpAuthenticationMechanism.class.equals(interfaceClass)) {
                    return true;
                }
            }
        }
        return false;
    }
}
