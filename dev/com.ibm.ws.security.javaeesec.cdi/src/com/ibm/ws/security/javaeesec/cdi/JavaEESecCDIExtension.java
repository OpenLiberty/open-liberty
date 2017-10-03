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
package com.ibm.ws.security.javaeesec.cdi;

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
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessBean;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.security.enterprise.authentication.mechanism.http.BasicAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStoreHandler;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cdi.extension.WebSphereCDIExtension;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;
//import com.ibm.ws.security.jsr375.authentication.mechanism.http.FormAuthenticationMechanism;

// TODO:
// Find out how to release LoginToContinue annotation in LoginToContinueIntercepter by the one in FormAuthenticationMechanismDefinition.

/**
 * TODO: Add all JSR-375 API classes that can be bean types to api.classes.
 *
 * @param <T>
 */
@Component(service = WebSphereCDIExtension.class,
           property = { "api.classes=javax.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;javax.security.enterprise.identitystore.IdentityStore;javax.security.enterprise.identitystore.IdentityStoreHandler;javax.security.enterprise.identitystore.RememberMeIdentityStore;javax.security.enterprise.SecurityContext" },
           immediate = true)
public class JavaEESecCDIExtension<T> implements Extension, WebSphereCDIExtension {

    private static final TraceComponent tc = Tr.register(JavaEESecCDIExtension.class);

    // TODO: Track beans by annotated type
    private final Set<Bean> beansToAdd = new HashSet<Bean>();
    private boolean identityStoreHandlerRegistered = false;
    private boolean identityStoreRegistered = false;
    private Annotation loginToContinue = null;
    private AnnotatedTypeWrapper<?> savedAnnotationWrapper = null;

    public <T> void processManagedBean(@Observes ProcessManagedBean<T> processManagedBean, BeanManager beanManager) {
        AnnotatedType<T> annotatedType = processManagedBean.getAnnotatedBeanClass();
        Set<Annotation> annotations = annotatedType.getAnnotations();
        for (Annotation annotation : annotations) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Annotations found: " + annotation);
                Tr.debug(tc, "Annotation class: ", annotation.getClass());
            }
        }
    }

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> processAnnotatedType, BeanManager beanManager) {
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        if (annotatedType.getJavaClass() == LoginToContinueInterceptor.class) {
            AnnotatedTypeWrapper<T> annotationWrapper = new AnnotatedTypeWrapper<T>(annotatedType, annotatedType.getAnnotations());
            if (loginToContinue != null) { // if loginToContinue has been found, set it.
                annotationWrapper.addAnnotation(loginToContinue);
            }
            processAnnotatedType.setAnnotatedType(annotationWrapper);
            savedAnnotationWrapper = annotationWrapper;
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
            } else if (FormAuthenticationMechanismDefinition.class.equals(annotationType)) {
                getLoginToContinue(beanManager, annotation, annotationType, annotatedType);
//            } else if (LoginToContinue.class.equals(annotationType)) {
//                if (savedAnnotationWrapper != null) {
//                    savedAnnotationWrapper.addAnnotation(loginToContinue);
//                } else {
//                    loginToContinue = annotation;
//                }
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

    void processBean(@Observes ProcessBean<?> processBean, BeanManager beanManager) {
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
    private <T> void getLoginToContinue(BeanManager beanManager, Annotation annotation, Class<? extends Annotation> annotationType,
                                        AnnotatedType<T> annotatedType) {
        try {
            Method loginToContinueMethod = annotationType.getMethod("loginToContinue");
            Annotation ltcAnnotation = (Annotation) loginToContinueMethod.invoke(annotation);
            if (ltcAnnotation != null) {
                if (savedAnnotationWrapper != null) {
                    savedAnnotationWrapper.addAnnotation(ltcAnnotation);
                } else {
                    loginToContinue = ltcAnnotation;
                }
            }

//            Properties props = parseLoginToContinue(annotation, annotationType);
//            FormAuthenticationMechanismBean bean = new FormAuthenticationMechanismBean(props, beanManager);
//            beansToAdd.add(bean);

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
    private Properties parseLoginToContinue(Annotation annotation, Class<? extends Annotation> annotationType) throws Exception {
        Properties props = new Properties();
        Method loginToContinueMethod = annotationType.getMethod("loginToContinue");
        Annotation ltcAnnotation = (Annotation) loginToContinueMethod.invoke(annotation);
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
}
