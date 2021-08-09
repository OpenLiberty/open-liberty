/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata.ejb;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.interceptor.AroundInvoke;
import javax.interceptor.AroundTimeout;

import com.ibm.ejs.container.LifecycleInterceptorWrapper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.javaee.dd.common.InterceptorCallback;
import com.ibm.ws.javaee.dd.ejb.Interceptor;
import com.ibm.ws.javaee.dd.ejb.MessageDriven;
import com.ibm.ws.javaee.dd.ejb.SessionInterceptor;

/**
 * Enum constants for the possible kinds of interceptor methods.
 */
public enum InterceptorMethodKind
{
    AROUND_CONSTRUCT(null, "around-construct", null, -1)
    {
        @Override
        public List<? extends InterceptorCallback> getMethods(SessionInterceptor interceptor)
        {
            return interceptor instanceof Interceptor ? ((Interceptor) interceptor).getAroundConstruct() : null;
        }

        @Override
        public List<? extends InterceptorCallback> getMethods(MessageDriven bean)
        {
            return null;
        }

        @Override
        public Class<? extends Annotation> getAnnotationClass()
        {
            return AROUND_CONSTRUCT_CLASS;
        }

        @Override
        public boolean isLifecycle()
        {
            return true;
        }

        @Override
        public boolean isEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
        {
            return false;
        }

    },

    AROUND_INVOKE(AroundInvoke.class, "around-invoke", null, -1)
    {
        @Override
        public List<InterceptorCallback> getMethods(SessionInterceptor interceptor)
        {
            return interceptor.getAroundInvoke();
        }

        @Override
        public List<InterceptorCallback> getMethods(MessageDriven bean)
        {
            return bean.getAroundInvoke();
        }
    },

    AROUND_TIMEOUT(AroundTimeout.class, "around-timeout", null, -1)
    {
        @Override
        public List<InterceptorCallback> getMethods(SessionInterceptor interceptor)
        {
            return interceptor.getAroundTimeoutMethods();
        }

        @Override
        public List<InterceptorCallback> getMethods(MessageDriven bean)
        {
            return bean.getAroundTimeoutMethods();
        }
    },

    POST_CONSTRUCT(PostConstruct.class, "post-construct", "ejbCreate", LifecycleInterceptorWrapper.MID_POST_CONSTRUCT)
    {
        @Override
        public List<? extends InterceptorCallback> getMethods(SessionInterceptor interceptor)
        {
            return interceptor.getPostConstruct();
        }

        @Override
        public List<? extends InterceptorCallback> getMethods(MessageDriven bean)
        {
            return bean.getPostConstruct();
        }

        @Override
        public boolean isEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
        {
            return factory.ivHasComponentInterface;
        }

        @Override
        public boolean isNonAnnotatedEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
        {
            // EJB 3 spec requires ejbCreate() method of both SLSB and MDB to be considered
            // a PostConstruct method. Only the ejbCreate that takes no parameters, does
            // not throw any checked exception, and has void as return type is
            // considered a PostConstruct method. This method for SFSB is a @Init method.
            // Note: this is true for Stateless and MessageDriven
            // regardless whether or not the bean implements the
            // SessionBean or MessageDriven interface.           d615004 F743-34301
            return factory.ivSLSB || factory.ivMDB;
        }
    },

    PRE_DESTROY(PreDestroy.class, "pre-destroy", "ejbRemove", LifecycleInterceptorWrapper.MID_PRE_DESTROY)
    {
        @Override
        public List<? extends InterceptorCallback> getMethods(SessionInterceptor interceptor)
        {
            return interceptor.getPreDestroy();
        }

        @Override
        public List<? extends InterceptorCallback> getMethods(MessageDriven bean)
        {
            return bean.getPreDestroy();
        }

        @Override
        public boolean isEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
        {
            return factory.ivHasComponentInterface;
        }
    },

    POST_ACTIVATE(PostActivate.class, "post-activate", "ejbActivate", LifecycleInterceptorWrapper.MID_POST_ACTIVATE)
    {
        @Override
        public List<? extends InterceptorCallback> getMethods(SessionInterceptor interceptor)
        {
            return interceptor.getPostActivate();
        }

        @Override
        public List<InterceptorCallback> getMethods(MessageDriven bean)
        {
            return null;
        }

        @Override
        public boolean isEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
        {
            return factory.ivHasComponentInterface && factory.ivSFSB;
        }
    },

    PRE_PASSIVATE(PrePassivate.class, "pre-passivate", "ejbPassivate", LifecycleInterceptorWrapper.MID_PRE_PASSIVATE)
    {
        @Override
        public List<? extends InterceptorCallback> getMethods(SessionInterceptor bean)
        {
            return bean.getPrePassivate();
        }

        @Override
        public List<InterceptorCallback> getMethods(MessageDriven bean)
        {
            return null;
        }

        @Override
        public boolean isEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
        {
            return factory.ivHasComponentInterface && factory.ivSFSB;
        }
    };

    private static final TraceComponent tc = Tr.register(InterceptorMethodKind.class, "EJB3Interceptors", "com.ibm.ejs.container.container");

    private static final Class<? extends Annotation> AROUND_CONSTRUCT_CLASS = getAroundConstructClass();

    /**
     * Static lookup of the AroundConstructClass that returns it if interceptor 1.2 is enabled or null otherwise
     *
     * AroundConstruct was only introduced in javax.interceptor 1.2 (ejbLite-3.1, cdi-1.2),
     * so allow it to be missing in case we are running javax.interceptor 1.1
     *
     */
    private static Class<? extends Annotation> getAroundConstructClass() {
        try {
            return Class.forName("javax.interceptor.AroundConstruct").asSubclass(Annotation.class);
        } catch (ClassNotFoundException e) {
            final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "unable to load AroundConstruct class");
            return null;
        }
    }

    /**
     * Return true if Interceptors 1.2 is enabled
     */
    public static boolean isInterceptor1_2()
    {
        return AROUND_CONSTRUCT_CLASS != null;
    }

    private Class<? extends Annotation> ivAnnotationClass;
    private String ivXMLElementName;
    private String ivLifecycleCallbackMethodName;
    private int ivMethodID;

    /**
     * Constructs a new InterceptorMethodKind.
     *
     * @param annotationClass the method-level annotation class for declaring an
     *            interceptor
     * @param xmlElementName the XML element name for declaring an interceptor
     * @param lifecycleInterfaceMethodName the method name from the SessionBean
     *            or MessageDriven interfaces, or null if this is not a lifecycle
     *            interceptor
     * @param methodId a MID value from {@link LifecycleInterceptorWrapper}, or
     *            <tt>-1</tt> if this interceptor has no method ID
     */
    InterceptorMethodKind(Class<? extends Annotation> annotationClass
                          , String xmlElementName
                          , String lifecycleInterfaceMethodName
                          , int methodId)
    {
        ivAnnotationClass = annotationClass;
        ivXMLElementName = xmlElementName;
        ivLifecycleCallbackMethodName = lifecycleInterfaceMethodName;
        ivMethodID = methodId;
    }

    /**
     * Returns the method-level annotation class for declaring an annotation.
     *
     * @return the annotation class
     */
    public Class<? extends Annotation> getAnnotationClass()
    {
        return ivAnnotationClass;
    }

    /**
     * Returns the XML element name for declaring an interceptor
     *
     * @return the XML element name
     */
    public String getXMLElementName()
    {
        return ivXMLElementName;
    }

    /**
     * Returns <tt>true</tt> if this kind of interceptor is a lifecycle
     * interceptor.
     *
     * @return <tt>true</tt> if this kind of interceptor is a lifecycle
     *         interceptor
     */
    public boolean isLifecycle()
    {
        return ivLifecycleCallbackMethodName != null;
    }

    /**
     * Returns the name of the callback method from the component interface, or
     * <tt>null</tt> if this is not lifecycle interceptor kind.
     *
     * @return the callback method name
     */
    public String getLifecycleCallbackMethodName()
    {
        return ivLifecycleCallbackMethodName;
    }

    /**
     * Returns a MID value from {@link LifecycleInterceptorWrapper}, or
     * <tt>-1</tt> if this interceptor has no method ID.
     *
     * @return a MID or <tt>-1</tt> if not applicable
     */
    public int getMethodID()
    {
        return ivMethodID;
    }

    /**
     * Returns the list of LifecycleInterceptorType, AroundInvokeMethod, or
     * AroundTimeoutMethod objects for the interceptor or bean defined in XML.
     *
     * @param interceptor the interceptor
     * @return the list of methods
     */
    public abstract List<? extends InterceptorCallback> getMethods(SessionInterceptor interceptor);

    /**
     * Returns the list of LifecycleInterceptorType, AroundInvokeMethod, or
     * AroundTimeoutMethod objects for the bean defined in XML.
     *
     * @param interceptor the interceptor
     * @return the list of methods
     */
    public abstract List<? extends InterceptorCallback> getMethods(MessageDriven bean);

    /**
     * Returns true if a lifecycle interceptor declared via XML or annotation
     * needs to be validated to ensure that it matches the corresponding EJB
     * callback method (typically from a component interface).
     *
     * @param factory
     * @see InterceptorMetaDataFactory#validateEJBCallbackMethod
     */
    public boolean isEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if a method without a lifecycle interceptor annotation
     * needs to be checked to see if it should have been annotated because it is
     * an EJB callback method (typically from the component interface).
     *
     * @param factory
     */
    public boolean isNonAnnotatedEJBCallbackMethodValidationRequired(InterceptorMetaDataFactory factory)
    {
        return isEJBCallbackMethodValidationRequired(factory);
    }
}
