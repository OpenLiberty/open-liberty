/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.interceptors;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Map;

import javax.ejb.EJBException;
import javax.ejb.Timer;
import javax.interceptor.InvocationContext;

import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSDeployedSupport;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.managedobject.ConstructionCallback;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectInvocationContext;

/**
 * This class provides the implementation of javax.interceptor.InvocationContext
 * interface that is defined by EJB 3 specification. The InvocationContext object
 * provides metadata that enables interceptor methods to control the behavior
 * of the invocation chain.
 * <p>
 * The same InvocationContext instance will be passed to each interceptor method
 * for a given business method or lifecycle event interception. This allows an
 * interceptor to save information in the context data property of the
 * InvocationContext that can be subsequently retrieved in other interceptors as
 * a means to pass contextual data between interceptors. The contextual data is
 * not sharable across separate business method invocations or lifecycle callback
 * events. The lifecycle of the InvocationContext instance is otherwise unspecified.
 * <p>
 * For WebSphere, the intent is EJSDeployedSupport object will contain a new
 * field for holding a reference to a InvocationContext instance when the
 * EJB has one or more AroundInvoke or lifecycle callback event interceptor
 * methods defined for the EJB. Thus the lifetime will be the same as the
 * lifetime of a EJSDeployedSupport object.
 */
public class InvocationContextImpl<T> implements InvocationContext, ManagedObjectInvocationContext<T> {
    private static final TraceComponent tc = Tr.register(InvocationContextImpl.class,
                                                         "EJB3Interceptors",
                                                         "com.ibm.ejs.container.container");

    /**
     * The Method object to be returned by the getMethod() method.
     * This is the Method object of business method being invoked or
     * null if a lifecycle method is being invoked.
     */
    private Method ivMethod;

    /**
     * The EJB instance the getTarget() method must return.
     */
    private T ivBean;

    /**
     * The array of arguments for the business method being invoked
     * or null if a lifecycle method is being invoked.
     * Array length of zero if no arguments.
     */
    private Object[] ivParameters = new Object[0];

    /**
     * The timer instance for the timer method being executed. This
     * field must be null if this is not an around-timeout interceptor.
     */
    private Timer ivTimer;

    /**
     * The current array of InterceptorProxy objects for invoking the
     * interceptor methods.
     */
    private InterceptorProxy[] ivInterceptorProxies;

    /**
     * The next index into ivInterceptorProxy array to use to invoke
     * the next interceptor method.
     */
    private int ivNextIndex;

    /**
     * Number of InterceptorProxy objects in ivInterceptorProxy array.
     */
    private int ivNumberOfInterceptors;

    /**
     * An ordered array of interceptor instances that must be passed
     * to a InterceptorProxy object when the proxy is invoked.
     */
    private Object[] ivInterceptors;

    /**
     * Set to false when doAroundInvoke is called and then set to
     * true if any of the around invoke interceptors calls the
     * setParameters method on this object.
     */
    private boolean ivParametersModified = false; //LIDB3294-41

    /**
     * The EJBDesploySupport to use when invoking EJSContainer.EJBinvokeProceed.
     */
    private EJSDeployedSupport ivEJSDeployedSupport; //LIDB3294-41

    /**
     * The EJSContainer instance to use when calling EJSContainer.EJBinvokeProcced.
     */
    private final EJSContainer ivContainer = EJSContainer.getDefaultContainer(); //LIDB3294-41

    /**
     * The constructor callback used to create the object instance for AroundConstruct.
     * Only set if {@link #ivIsAroundConstruct} is true.
     */
    private ConstructionCallback<T> ivConstructCallback;

    /**
     * ivIsAroundConstruct true if this is the InvocationContext of
     * an AroundConstruct interceptor method.
     */
    private boolean ivIsAroundConstruct;

    /**
     * Saves Exception from proceed() during bean creation after being
     * intercepted by AroundConstruct. Re-thrown in
     * ManagedBeanOBase.createInterceptorsAndInstance to
     * increase the accuracy of the exception being thrown.
     */
    public Exception ivAroundConstructException;

    /**
     * The ManagedObjectContext is used to create a EJB Managed Object and any associated interceptors
     */
    private ManagedObjectContext ivManagedObjectContext;

    /**
     * Initialize a InvocationContext object for a specified bean and
     * the corresponding {@link ManagedObjectContext} and array of
     * interceptor instances created for this bean instance.
     *
     * @param bean is the EJB instance for this invocation.
     * @param managedObjectContext The managed object state of the bean instance,
     *            or null if not managed.
     * @param interceptors is the ordered array of interceptor instances
     *            created for the bean.
     */
    public void initialize(T bean, ManagedObjectContext managedObjectContext,
                           Object[] interceptors) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "initialize for bean = " + bean +
                         ", context = " + managedObjectContext +
                         ", interceptors = " + Arrays.toString(interceptors));
        ivBean = bean;
        ivManagedObjectContext = managedObjectContext;
        ivInterceptors = interceptors;
        ivInterceptorProxies = null;
        ivTimer = null;
    }

    /**
     * Sets the timer associated with this invocation context.
     *
     * @param timer the timer, or <tt>null</tt> if this context is not being
     *            used for an around-timeout method invocation
     */
    public void setTimer(Timer timer) {
        ivTimer = timer;
    }

    /**
     * Initialize a InvocationContext for AroundConstruct with
     * an array of interceptor instances created for this bean instance
     * and the interceptor proxies.
     *
     * @param interceptors is the ordered array of interceptor instances
     *            created for the bean.
     * @param proxies is an array of InterceptorProxy objects that
     *            represent the list of AroundInvoke interceptor
     *            methods to be invoked.
     */
    public void initializeForAroundConstruct(ManagedObjectContext managedObjectContext,
                                             Object[] interceptors, InterceptorProxy[] proxies) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "initializeForAroundConstruct : context = " + managedObjectContext +
                         " interceptors = " + Arrays.toString(interceptors) + ", proxies = " + Arrays.toString(proxies));
        ivBean = null;
        ivManagedObjectContext = managedObjectContext;
        ivInterceptors = interceptors;
        ivInterceptorProxies = proxies;
        ivTimer = null;
    }

    @Override
    public T aroundConstruct(ConstructionCallback<T> constructionCallback, Object[] parameters, Map<String, Object> data) throws Exception {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "aroundConstruct : " + constructionCallback + ", " + com.ibm.ejs.util.Util.identity(parameters) + ", " + data);

        try {
            ivConstructCallback = constructionCallback;
            ivParameters = parameters;
            ivIsAroundConstruct = true;

            if (data != null && data.size() > 0) {
                getContextData().putAll(data);
            }

            doAroundInterceptor();

            return ivBean;
        } finally {
            ivConstructCallback = null;
            ivParameters = null;
            ivIsAroundConstruct = false;

            // Let the mapping strategy handle checked and unchecked exceptions
            // that occurs since it knows whether to treat unchecked exceptions
            // as an application exception or as a system exception.
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "aroundConstruct");
        }

    }

    /**
     * Invoke each AroundInvoke interceptor methods for a specified
     * business method of an EJB being invoked.
     *
     * @param proxies is an array of InterceptorProxy objects that
     *            represent the list of AroundInvoke interceptor
     *            methods to be invoked.
     * @param businessMethod is the Method object for invoking the business method.
     * @param parameters is the array of arguments to be passed to business method.
     *
     * @return the Object that is returned by business method.
     *
     * @throws Exception from around invoke or business method.
     */

    public Object doAroundInvoke(InterceptorProxy[] proxies, Method businessMethod, Object[] parameters, EJSDeployedSupport s) //LIDB3294-41
                    throws Exception {
        ivMethod = businessMethod;
        ivParameters = parameters;
        ivEJSDeployedSupport = s; //LIDB3294-41
        ivInterceptorProxies = proxies;
        ivIsAroundConstruct = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) // d367572.7
        {
            Tr.entry(tc, "doAroundInvoke for business method: " + ivMethod.getName());
        }
        try {
            return doAroundInterceptor();
        } finally // d367572.8
        {
            // Let the mapping strategy handle checked and unchecked exceptions
            // that occurs since it knows whether to treat unchecked exceptions
            // as an application exception or as a system exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) // d415968
            {
                Tr.exit(tc, "doAroundInvoke for business method: " + ivMethod.getName());
            }
            ivMethod = null;
            ivParameters = null;
        }
    }

    /**
     * Invoke each AroundInvoke or AroundConstruct interceptor methods
     *
     * @return the Object that is returned by business method.
     *
     * @throws Exception from around invoke or business method.
     */
    private Object doAroundInterceptor() throws Exception {
        // Note, we do not call setParameters since the assumption is the
        // wrapper code passes an Object array that always contains the
        // correct type.  If we want type checking to ensure wrapper
        // code is correct, we could call setParameters(parameters) instead
        // of just doing assignment here.
        ivNextIndex = 0;
        ivNumberOfInterceptors = ivInterceptorProxies == null ? 0 : ivInterceptorProxies.length;
        ivParametersModified = false; //LIDB3294-41
        return proceed();
    }

    /**
     * Invoke each of the lifecycle callback event interceptor method
     * in a specified list of interceptor methods to invoke.
     *
     * @param proxies is an array of InterceptorProxy objects that
     *            represent the list of interceptor callback event interceptor
     *            methods to be invoked.
     *
     * @param mmd the module metadata
     */
    // d450431 - add appExceptionMap parameter.
    public void doLifeCycle(InterceptorProxy[] proxies, EJBModuleMetaDataImpl mmd) // F743-14982
    {
        ivMethod = null;
        ivParameters = null; // d367572.8
        ivInterceptorProxies = proxies;
        ivNumberOfInterceptors = ivInterceptorProxies.length;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) // d367572.7
        {
            Tr.entry(tc, "doLifeCycle, number of interceptors = " + ivNumberOfInterceptors);
        }

        if (ivNumberOfInterceptors > 0) {
            ivNextIndex = 0;
            try {
                proceed();
            } catch (Throwable t) // d415968
            {
                // FFDCFilter.processException( t, CLASS_NAME + ".doLifeCycle", "260", this );
                lifeCycleExceptionHandler(t, mmd); // d367572.7, F743-14982
            } finally {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) // d415968
                {
                    Tr.exit(tc, "doLifeCycle");
                }
            }
        }
    }

    /**
     * Handle any InvocationTargetException that is thrown by the invoke
     * of a lifecycle interceptor method. Since the EJB 3 specification
     * requires the lifecycle methods to not throw application or checked
     * exceptions, this code will only throw unchecked exceptions.
     *
     * @param e is the InvocationTargetException that occured.
     * @param mmd the module metadata
     */
    // d367572.7
    // d450431 - ensure runtime exception is not an application exception.
    private void lifeCycleExceptionHandler(Throwable t, EJBModuleMetaDataImpl mmd) // F743-14982
    {
        if (t instanceof RuntimeException) {
            // Is the RuntimeException an application exception?
            RuntimeException rtex = (RuntimeException) t;
            if (mmd.getApplicationExceptionRollback(rtex) != null) // F743-14982
            {
                // Yes it is, which is not valid for lifecycle callback
                // methods.  So turn this into a system runtime exception.
                InterceptorProxy w = ivInterceptorProxies[ivNextIndex - 1];
                String lifeCycle = w.getMethodGenericString();
                EJBException ex = ExceptionUtil.EJBException(lifeCycle
                                                             + " is not allowed to throw an application exception", rtex);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "lifeCycleExceptionHandler throwing EJBException", ex);
                }
                throw ex;
            } else {
                // Not an application exception, so let the mapping
                // strategy handle this system runtime exception.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "lifeCycleExceptionHandler is rethrowing RuntimeException "
                                 + "from lifecycle callback method: " + t,
                             t);
                }
                throw rtex;
            }
        } else if (t instanceof Error) {
            // Let the mapping strategy handle this unchecked exception.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "lifeCycleExceptionHandler is rethrowing Error from "
                             + "lifecycle callback method: " + t,
                         t);
            }
            throw (Error) t;
        } else {
            // Must be a checked exception, which should never happen since
            // InterceptorMetaDataFactory throws EJBConfigurationException
            // and does a Tr.error using message key of INVALID_LIFECYCLE_SIGNATURE_CNTR0231E
            // if interceptor method throws clause is not empty. But if
            // it does happen, wrap the exception in a special EJBException
            // that does not include EJB container in the exception stack.
            InterceptorProxy w = ivInterceptorProxies[ivNextIndex - 1]; // d367572.7
            String lifeCycle = w.getMethodGenericString(); // d367572.7
            EJBException ex = ExceptionUtil.EJBException(lifeCycle
                                                         + " is not allowed to throw a checked exception", t);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) // d367572.7
            {
                Tr.debug(tc, "lifeCycleExceptionHandler throwing EJBException", ex);
            }
            throw ex;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.interceptor.InvocationContext#getTarget()
     */
    @Override
    public Object getTarget() {
        return ivBean;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.interceptor.InvocationContext#getMethod()
     */
    @Override
    public Method getMethod() {
        return ivMethod;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.interceptor.InvocationContext#getParameters()
     */
    @Override
    public Object[] getParameters() {
        //d470721 throw IllegalStateException if called from a
        // lifecycle callback interceptor method. ivMethod is only
        // set for AroundInvoke methods, so ivMethod being null
        // indicates a lifecycle callback interceptor method.
        if (ivMethod == null && !ivIsAroundConstruct) {
            throw new IllegalStateException("InvocationContext.getParameter can not be called by lifecycle callback methods");
        }
        return ivParameters;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.interceptor.InvocationContext#getTimer()
     */
    @Override
    public Object getTimer() // F743-17763
    {
        return ivTimer;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.interceptor.InvocationContext#setParameters(java.lang.Object[])
     */
    @Override
    public void setParameters(Object[] args) {
        //d470721 throw IllegalStateException if called from a
        // lifecycle callback interceptor method. ivMethod is only
        // set for AroundInvoke methods, so ivMethod being null
        // indicates a lifecycle callback interceptor method.
        if (ivMethod == null && !ivIsAroundConstruct) {
            throw new IllegalStateException("InvocationContext.setParameter can not be called by lifecycle callback methods");
        }

        Constructor<?> con = getConstructor();
        Class<?>[] parmTypes = con != null ? con.getParameterTypes() : ivMethod.getParameterTypes();

        String debug = con != null ? "constructor: " + con.getName() : "business method: " + ivMethod.getName();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setParameters for " + debug);
        }

        int n = parmTypes.length;
        if (args == null) // d386227
        {
            if (n > 0) {
                throw new IllegalArgumentException("null not valid as argument for setParameters method.");
            }
        } else {
            if (args.length != n) {
                throw new IllegalArgumentException("wrong number of parameters for method being invoked.");
            } else {
                for (int i = 0; i < n; ++i) {
                    Class<?> parmType = parmTypes[i];
                    if (parmType.isPrimitive() == false) // d367572.6
                    {
                        if (args[i] != null && // d611096
                            (!parmType.isInstance(args[i]))) // d386227
                        {
                            throw new IllegalArgumentException("wrong data type for parameter " + (i + 1));
                        }
                    } else {
                        // d367572.6
                        // Verify argument corresponds to correct java primitive type.
                        if ((parmType == Integer.TYPE) && (!(args[i] instanceof Integer))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Integer");
                        } else if ((parmType == Long.TYPE) && (!(args[i] instanceof Long))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Long");
                        } else if ((parmType == Short.TYPE) && (!(args[i] instanceof Short))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Short");
                        } else if ((parmType == Boolean.TYPE) && (!(args[i] instanceof Boolean))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Boolean");
                        } else if ((parmType == Byte.TYPE) && (!(args[i] instanceof Byte))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Byte");
                        } else if ((parmType == Character.TYPE) && (!(args[i] instanceof Character))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Character");
                        } else if ((parmType == Float.TYPE) && (!(args[i] instanceof Float))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Float");
                        } else if ((parmType == Double.TYPE) && (!(args[i] instanceof Double))) {
                            throw new IllegalArgumentException("parameter " + (i + 1) + " is a "
                                                               + args[i].getClass().getName()
                                                               + ", but it is required to be a Double");
                        }
                    }
                }

                // All of the arguments are of the correct data type, so set the parameters.
                ivParameters = args;
                ivParametersModified = true; //LIDB3294-41

            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.interceptor.InvocationContext#getContextData()
     */
    @Override
    public Map<String, Object> getContextData() {
        return EJSContainer.getThreadData().getContextData(); // d644886
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.interceptor.InvocationContext#proceed()
     */
    // d450431 - rewrote entire method to handle the special case of
    //           bean class and its superclasses are having lifecycle
    //           callback methods and none of them will call proceed
    //           since InvocationContext is not passed to them.
    // d454160 - try/catch InvocationTargetException and unwrapper.
    @Override
    public Object proceed() throws Exception {
        Object returnValue = null;
        try {
            if (ivNextIndex < ivNumberOfInterceptors) {
                // There is atleast 1 more interceptor method in the
                // chain to invoke. Determine if method signature is
                // one that is passed the InvocationContext.
                InterceptorProxy w = ivInterceptorProxies[ivNextIndex];
                if (w.ivRequiresInvocationContext) {
                    // Method signature is <METHOD>(InvocationContext).
                    // Therefore, only call this method it will call
                    // InvocationContext.proceed() to continue with
                    // the next method in the chain.
                    ++ivNextIndex;
                    returnValue = w.invokeInterceptor(ivBean, this, ivInterceptors);
                } else {
                    // Method signature is <METHOD>(), which means it must be
                    // a lifecycle callback method of the bean class or
                    // one of its superclasses.  As required by EJB 3 spec,
                    // call all remaining lifecycle methods in the chain.
                    while (w != null) {
                        ++ivNextIndex;
                        returnValue = w.invokeInterceptor(ivBean, this, ivInterceptors);
                        w = (ivNextIndex < ivNumberOfInterceptors) ? ivInterceptorProxies[ivNextIndex] : null;
                    }
                }
            } else if (ivMethod != null) {
                // The last around invoke in the chain has called proceed,
                // so now we must invoke the business method.
                returnValue = ivContainer.invokeProceed(ivEJSDeployedSupport, ivMethod, ivBean, ivParameters, ivParametersModified); //LIDB3294-41
            } else if (ivIsAroundConstruct) {
                try {
                    ivBean = ivConstructCallback.proceed(ivParameters, getContextData());
                } catch (Exception ex) {
                    ivAroundConstructException = ex;
                    throw ex;
                }
                return null;
            }

        } catch (UndeclaredThrowableException ude) {
            throwUndeclaredExceptionCause(ude);
        } catch (InvocationTargetException ite) {
            throwUndeclaredExceptionCause(ite);
        }

        // Per the interceptor 1.2 spec, "If a lifecycle callback interceptor
        // method returns a value, it is ignored by the container."
        return ivMethod == null ? null : returnValue;
    }

    /**
     * Since some interceptor methods cannot throw 'Exception', but the target
     * method on the bean can throw application exceptions, this method may be
     * used to unwrap the application exception from either an
     * InvocationTargetException or UndeclaredThrowableException.
     *
     * @param undeclaredException the InvocationTargetException or UndeclaredThrowableException
     *            that is wrapping the real application exception.
     * @throws Exception the application exception
     */
    private void throwUndeclaredExceptionCause(Throwable undeclaredException) throws Exception {
        Throwable cause = undeclaredException.getCause();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "proceed unwrappering " + undeclaredException.getClass().getSimpleName() + " : " + cause, cause);
        // CDI interceptors tend to result in a UndeclaredThrowableException wrapped in an InvocationTargetException
        if (cause instanceof UndeclaredThrowableException) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "proceed unwrappering " + cause.getClass().getSimpleName() + " : " + cause, cause.getCause());
            cause = cause.getCause();
        }
        if (cause instanceof RuntimeException) {
            // Let the mapping strategy handle this unchecked exception.
            throw (RuntimeException) cause;
        } else if (cause instanceof Error) {
            // Let the mapping strategy handle this unchecked exception.
            throw (Error) cause;
        } else {
            // Probably an application exception occurred, so just throw it. The mapping
            // strategy will handle if it turns out not to be an application exception.
            throw (Exception) cause;
        }
    }

    public Constructor<T> getConstructor() {
        if (ivIsAroundConstruct) {
            return ivConstructCallback.getConstructor();
        }
        return null;
    }

    @Override
    public ManagedObjectContext getManagedObjectContext() {
        return ivManagedObjectContext;
    }

    public void prePostConstruct(ManagedObject<T> mo) {
        // nothing to do
    }

}
