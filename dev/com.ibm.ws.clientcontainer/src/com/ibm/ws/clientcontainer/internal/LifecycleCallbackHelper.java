/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.clientcontainer.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.LifecycleCallback;
import com.ibm.wsspi.injectionengine.InjectionException;

/**
 * This class is used as a helper function to call the lifecycle callback method based on the annotation in
 * the class and/or the deployment descriptor for a given application client module.
 */
public class LifecycleCallbackHelper {
    private static final TraceComponent tc = Tr.register(LifecycleCallbackHelper.class, "clientContainer", "com.ibm.ws.clientcontainer.resources.Messages");
    private String mainClassName = "";
    private boolean metadataComplete = false;

    public LifecycleCallbackHelper(boolean metadataComplete) {
        this.metadataComplete = metadataComplete;
    }

    /**
     * Processes the PostConstruct callback method for the application main class
     * 
     * @param clazz the application main class object
     * @param postConstructs a list of PostConstruct metadata in the application client module
     * @throws InjectionException
     */
    @SuppressWarnings("rawtypes")
    public void doPostConstruct(Class clazz, List<LifecycleCallback> postConstructs) throws InjectionException {
        mainClassName = clazz.getName();
        doPostConstruct(clazz, postConstructs, null);
    }

    /**
     * Processes the PostConstruct callback method for the login callback handler class
     * 
     * @param instance the instance object of the login callback handler class
     * @param postConstructs a list of PostConstruct metadata in the application client module
     * @throws InjectionException
     */
    public void doPostConstruct(Object instance, List<LifecycleCallback> postConstructs) throws InjectionException {
        doPostConstruct(instance.getClass(), postConstructs, instance);
    }

    /**
     * Processes the PreDestroy callback method for the login callback handler class
     * 
     * @param instance the instance object of the login callback handler class
     * @param postConstructs a list of PreDestroy metadata in the application client module
     * @throws InjectionException
     */
    public void doPreDestroy(Object instance, List<LifecycleCallback> preDestroy) throws InjectionException {
        doPreDestroy(instance.getClass(), preDestroy, instance);
    }

    /**
     * Processes the PostConstruct callback method
     * 
     * @param clazz the callback class object
     * @param postConstructs a list of PostConstruct application client module deployment descriptor.
     * @param instance the instance object of the callback class. It can be null for static method.
     * @throws InjectionException
     */
    @SuppressWarnings("rawtypes")
    private void doPostConstruct(Class clazz, List<LifecycleCallback> postConstructs, Object instance) throws InjectionException {
        if (!metadataComplete && clazz.getSuperclass() != null) {
            doPostConstruct(clazz.getSuperclass(), postConstructs, instance);
        }

        String classname = clazz.getName();
        String methodName = getMethodNameFromDD(postConstructs, classname);
        if (methodName != null) {
            invokeMethod(clazz, methodName, instance);
        } else if (!metadataComplete) {
            Method method = getAnnotatedPostConstructMethod(clazz);
            if (method != null) {
                invokeMethod(clazz, method.getName(), instance);
            }
        }
    }

    /**
     * Processes the PreDestroy callback method.
     * 
     * @param clazz the callback class object
     * @param preDestroy a list of PreDestroy metadata in the application client module
     * @param instance the instance object of the callback class
     * @throws InjectionException
     */
    @SuppressWarnings("rawtypes")
    private void doPreDestroy(Class clazz, List<LifecycleCallback> preDestroy, Object instance) throws InjectionException {
        if (!metadataComplete && clazz.getSuperclass() != null) {
            doPostConstruct(clazz.getSuperclass(), preDestroy, instance);
        }

        String classname = clazz.getName();
        String methodName = getMethodNameFromDD(preDestroy, classname);
        if (methodName != null) {
            invokeMethod(clazz, methodName, instance);
        } else if (!metadataComplete) {
            Method method = getAnnotatedPreDestroyMethod(clazz);
            if (method != null) {
                invokeMethod(clazz, method.getName(), instance);
            }
        }
    }

    /**
     * Get the @PostConstruct method from the class object.
     * 
     * @param clazz an Class object
     * @return a Method object
     */
    @SuppressWarnings("rawtypes")
    public Method getAnnotatedPostConstructMethod(Class clazz) {
        return getAnnotatedMethod(clazz, PostConstruct.class);
    }

    /**
     * Gets the @PreDestroy method from the class object.
     * 
     * @param clazz an Class object
     * @return a Method object
     */
    @SuppressWarnings("rawtypes")
    public Method getAnnotatedPreDestroyMethod(Class clazz) {
        return getAnnotatedMethod(clazz, PreDestroy.class);
    }

    /**
     * Gets the annotated method from the class object.
     * 
     * @param clazz the Class to be inspected.
     * @param annotationClass the annotation class object
     * @return a Method object or null if there is no annotated method.
     */
    @SuppressWarnings("rawtypes")
    public Method getAnnotatedMethod(Class clazz, Class<? extends Annotation> annotationClass) {
        Method m = null;

        Method[] methods = clazz.getDeclaredMethods();

        for (int i = 0; i < methods.length; i++) {
            Annotation[] a = methods[i].getAnnotations();
            if (a != null) {
                for (int j = 0; j < a.length; j++) {
                    if (a[j].annotationType() == annotationClass) {
                        if (m == null) {
                            m = methods[i];
                        } else {
                            Tr.warning(tc, "DUPLICATE_CALLBACK_METHOD_CWWKC2454W", new Object[] { methods[i].getName(), clazz.getName() });
                        }
                    }
                }
            }
        }
        return m;
    }

    /**
     * Invokes the class method. The object instance can be null for the application main class.
     * 
     * @param clazz the Class object
     * @param methodName the Method name
     * @param instance the instance object of the class. It can be null if the class is the application Main.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void invokeMethod(final Class clazz, final String methodName, final Object instance) {
        // instance can be null for the static application main method
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                try {
                    final Method m = clazz.getDeclaredMethod(methodName);
                    if (!m.isAccessible()) {
                        m.setAccessible(true);
                        m.invoke(instance);
                        m.setAccessible(false);
                        return m;
                    } else {
                        m.invoke(instance);
                        return m;
                    }
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, e.getMessage());
                    }
                    return null;
                }
            }
        });
    }

    /**
     * Gets the lifecycle callback method name from the application client module deployment descriptor
     * 
     * @param callbacks a list of lifecycle-callback in the application client module deployment descriptor
     * @param classname the Class name
     * @return the Mehtod name
     */
    public String getMethodNameFromDD(List<LifecycleCallback> callbacks, String classname) {
        String methodName = null;
        for (LifecycleCallback callback : callbacks) {
            // lifecycle-callback-class default to the enclosing component class Client
            String callbackClassName;
            callbackClassName = callback.getClassName();
            if (callbackClassName == null) {
                callbackClassName = mainClassName;
            }
            if (callbackClassName.equals(classname)) {
                if (methodName == null) {
                    methodName = callback.getMethodName();
                } else {
                    Tr.warning(tc, "DUPLICATE_CALLBACK_METHOD_CWWKC2454W", new Object[] { methodName, classname });
                }
            }
        }
        return methodName;
    }

}
