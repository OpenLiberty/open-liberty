/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import javax.ejb.EJBException;
import javax.interceptor.InvocationContext;

import com.ibm.ejs.container.interceptors.InterceptorMetaData;
import com.ibm.ejs.container.interceptors.InterceptorProxy;
import com.ibm.ejs.container.interceptors.InvocationContextImpl;
import com.ibm.ejs.container.util.ExceptionUtil;
import com.ibm.ejs.util.Util;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ejbcontainer.CallbackKind;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ConstructionCallback;
import com.ibm.ws.managedobject.ManagedObject;
import com.ibm.ws.managedobject.ManagedObjectContext;
import com.ibm.ws.managedobject.ManagedObjectException;
import com.ibm.ws.managedobject.ManagedObjectFactory;
import com.ibm.wsspi.injectionengine.InjectionEngine;
import com.ibm.wsspi.injectionengine.InjectionTarget;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 * Base class for bean types that have the capabilities of "managed beans",
 * including interceptors and injection (including CDI).
 */
public abstract class ManagedBeanOBase extends BeanO implements ConstructionCallback {
    private static final String CLASS_NAME = ManagedBeanOBase.class.getName();
    private static final TraceComponent tc = Tr.register(ManagedBeanOBase.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * The actual bean instance; should only be set using {@link #setEnterpriseBean}.
     */
    public Object ivEjbInstance;

    /**
     * The managed object state of the bean instance, or null if not managed.
     */
    public ManagedObjectContext ivEjbManagedObjectContext;

    /**
     * The managed object 'wrapper' used for release.
     * Only set if a ManagedObjectFactory was used to create the ManagedObject instance.
     */
    public ManagedObject<?> ivManagedObject;

    /**
     * Array of Interceptor instances when ivCallbackKind is set to
     * Callback.InvocationContext. For all other CallbackKind values,
     * this instance variable is null since no interceptors are used
     * by the bean.
     */
    public Object[] ivInterceptors; // d367572.7

    public ManagedBeanOBase(EJSContainer c, EJSHome h) {
        super(c, h);
    }

    /**
     * Set the actual bean instance. The instance variable should only
     * be set by this method, allowing subclasses to override and
     * perform additional setup when the instance is set.
     *
     * @param instance the actual bean instance
     */
    public void setEnterpriseBean(Object bean) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "setEnterpriseBean : " + Util.identity(bean));
        ivEjbInstance = bean;
    }

    /**
     * Creates and the interceptor instances for the bean.
     */
    private void createInterceptors(InterceptorMetaData imd) {
        ivInterceptors = new Object[imd.ivInterceptorClasses.length];

        try {
            imd.createInterceptorInstances(getInjectionEngine(), ivInterceptors, ivEjbManagedObjectContext, this);
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".ManagedBeanOBase", "177", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "interceptor creation failure", t);
            throw ExceptionUtil.EJBException("Interceptor creation failure", t);
        }
    }

    @Override
    public <T> T getInjectionTargetContextData(Class<T> type) {
        // If we have a managed object, then see if the context data type is
        // available from its state.
        if (ivEjbManagedObjectContext != null) {
            T data = ivEjbManagedObjectContext.getContextData(type);
            if (data != null) {
                return data;
            }
        }

        return super.getInjectionTargetContextData(type);
    }

    /**
     * Performs resource injection for the managed object instance.
     *
     * Subclasses may override this method to perform additional injection
     * like processing, such as setting the EJBContext via the 2.x APIs.
     *
     * @param managedObject managed object instance
     * @param instance ejb object instance
     * @param injectionContext context data associated with the injection
     *
     * @throws EJBException if a failure occurs injecting into the managed object.
     */
    protected void injectInstance(ManagedObject<?> managedObject, Object instance, InjectionTargetContext injectionContext) throws EJBException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "injectInstance : " + Util.identity(managedObject) + ", " + injectionContext);

        // Its valid to just check the InjectionTargets visible to the bean itself,
        // because any InjectionTargets visible to the interceptors were already
        // processed when the interceptors were created.
        BeanMetaData bmd = home.beanMetaData;
        if (bmd.ivBeanInjectionTargets != null) {
            try {
                //if CDI is enabled then there will be a managedObject and it should be used for injection, otherwise go directly to the injection enging
                if (managedObject != null) {
                    managedObject.inject(bmd.ivBeanInjectionTargets, injectionContext);
                } else {
                    InjectionEngine injectionEngine = getInjectionEngine();
                    for (InjectionTarget injectionTarget : bmd.ivBeanInjectionTargets) {
                        injectionEngine.inject(ivEjbInstance, injectionTarget, injectionContext);
                    }
                }
            } catch (Throwable t) {
                if (! (t instanceof ManagedObjectException)) {
                    FFDCFilter.processException(t, CLASS_NAME + ".injectInstance", "151", this);
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "injectInstance : Injection failure", t);
                    throw ExceptionUtil.EJBException("Injection failure", t);
                } else {
                    Throwable cause = t.getCause();
                    FFDCFilter.processException(cause, CLASS_NAME + ".injectInstance", "157", this);
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "injectInstance : Injection failure", cause);
                    throw ExceptionUtil.EJBException("Injection failure", cause);
                }                
            }
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "injectInstance");
    }

    /**
     * Needs to be called for every SessionBean as it goes out of service
     * (i.e destroyed or discarded). This cleans up all the dependent CDI
     * beans injected into the EJB instance and the corresponding interceptor
     * instances. <p>
     *
     * If this is NOT called and JCDI injection was performed, there WILL be
     * a memory leak. <p>
     */
    protected void releaseManagedObjectContext() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "releaseManagedObjectContext : " + ivEjbManagedObjectContext);
        if (ivEjbManagedObjectContext != null) {
            ivEjbManagedObjectContext.release();
        }
    }

    /**
     * Method for creating interceptors prior to the creation of the bean
     * instance, in case @AroundConstruct exists. <p>
     */
    protected void createInterceptorsAndInstance(CallbackContextHelper contextHelper) throws InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createInterceptorsAndInstance : " + contextHelper);

        BeanMetaData bmd = home.beanMetaData;
        ManagedObjectFactory<?> managedObjectFactory = bmd.ivEnterpriseBeanFactory;
        if (managedObjectFactory != null) {
            try {
                ivEjbManagedObjectContext = managedObjectFactory.createContext();
            } catch (ManagedObjectException e) {
                throw ExceptionUtil.EJBException("AroundConstruct interceptors for the " + bmd.enterpriseBeanName +
                                                 " bean in the " + bmd._moduleMetaData.ivName +
                                                 " module in the " + bmd._moduleMetaData.ivAppName +
                                                 " application resulted in an exception being thrown from the ManagedObjectFactory.createContext() method", e);
            }
        }

        InterceptorMetaData imd = bmd.ivInterceptorMetaData;
        if (imd != null && !bmd.managedObjectManagesInjectionAndInterceptors) {
            createInterceptors(imd);
            if (bmd.ivCallbackKind == CallbackKind.InvocationContext) {
                InterceptorProxy[] aroundConstructProxies = imd.ivAroundConstructInterceptors;
                if (aroundConstructProxies != null) {
                    InvocationContextImpl<?> context = callAroundConstructInterceptors(aroundConstructProxies, contextHelper);
                    // If InvocationContext.proceed() was not called, the bean's
                    // constructor was never called and ivEjbInstance will be null.
                    if (ivEjbInstance == null) {
                        // If the interceptor swallowed an exception from
                        // proceed(), we re-throw it now to give a better
                        // error message than a generic "failed to create".
                        if (context.ivAroundConstructException != null) {
                            Throwable t = context.ivAroundConstructException.getCause();
                            if (t == null) {
                                // Don't lose the application exception if it doesn't happen to be nested
                                t = context.ivAroundConstructException;
                            }
                            throw ExceptionUtil.EJBException("AroundConstruct interceptors for the " + bmd.enterpriseBeanName +
                                                             " bean in the " + bmd._moduleMetaData.ivName +
                                                             " module in the " + bmd._moduleMetaData.ivAppName +
                                                             " application resulted in an exception being thrown from the constructor of the " +
                                                             bmd.enterpriseBeanClassName + " class.", t);
                        }

                        // Otherwise, the interceptor must not have called proceed().
                        throw new EJBException("AroundConstruct interceptors for the " + bmd.enterpriseBeanName +
                                               " bean in the " + bmd._moduleMetaData.ivName +
                                               " module in the " + bmd._moduleMetaData.ivAppName +
                                               " application did not call InvocationContext.proceed()");
                    }
                }
            }
        }

        // If no @AroundConstructor interceptor, create the instance now.
        if (ivEjbInstance == null) {
            createInstance();
        }
        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createInterceptorsAndInstance");
    }

    /**
     * Creates the bean instance using either the ManagedObjectFactory or constructor.
     */
    private void createInstance() throws InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createInstance");

        ManagedObjectFactory<?> ejbManagedObjectFactory = home.beanMetaData.ivEnterpriseBeanFactory;
        if (ejbManagedObjectFactory != null) {
            createInstanceUsingMOF(ejbManagedObjectFactory);
        } else {
            createInstanceUsingConstructor();
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createInstance");
    }

    /**
     * Creates the bean instance using either the ManagedObjectFactory.
     */
    @SuppressWarnings("unchecked")
    private void createInstanceUsingMOF(ManagedObjectFactory<?> ejbManagedObjectFactory) throws InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createInstanceUsingMOF");

        try {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "calling ManagedObjectFactory.createManagedObject(InvocationContext)");

            @SuppressWarnings("rawtypes")
            InvocationContextImpl invCtx = getInvocationContext();
            ivManagedObject = ejbManagedObjectFactory.createManagedObject(invCtx);

            ivEjbManagedObjectContext = ivManagedObject.getContext();
            setEnterpriseBean(ivManagedObject.getObject());
        } catch (ManagedObjectException e) {
            //the callstack is epecting a InvocationTargetException so unwrap the ManagedObjectException and rewrap as InvocationTargetException
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof Exception) {
                if (cause instanceof InvocationTargetException) {
                    throw (InvocationTargetException) cause;
                } else {
                    throw new InvocationTargetException(cause);
                }
            } else {
                throw new EJBException(home.beanMetaData.enterpriseBeanClassName, e);
            }
        } catch (Exception e) {
            // Reflection exceptions are unexpected.
            FFDCFilter.processException(e, CLASS_NAME + ".createInstanceUsingMOF", "321", this);
            throw new EJBException(home.beanMetaData.enterpriseBeanClassName, e);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createInstanceUsingMOF");
    }

    /**
     * Creates the bean instance using the constructor.
     */
    private void createInstanceUsingConstructor() throws InvocationTargetException {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createInstanceUsingConstructor");

        try {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "calling Constructor.newInstance");
            Constructor<?> con = home.beanMetaData.getEnterpriseBeanClassConstructor();
            setEnterpriseBean(con.newInstance());
        } catch (InvocationTargetException e) {
            FFDCFilter.processException(e, CLASS_NAME + ".createInstanceUsingConstructor", "360", this);
            throw e;
        } catch (Exception e) {
            // Reflection exceptions are unexpected.
            FFDCFilter.processException(e, CLASS_NAME + ".createInstanceUsingConstructor", "364", this);
            throw new EJBException(home.beanMetaData.enterpriseBeanClassName, e);
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createInstanceUsingConstructor");
    }

    /**
     * Returns an {@link InvocationContext} initialized for use with this bean context.
     */
    protected InvocationContextImpl getInvocationContext() {
        // does not seem useful to cache one of these per bean instance; create a new one
        InvocationContextImpl invocationContext = createInvocationContext();
        invocationContext.initialize(ivEjbInstance, ivEjbManagedObjectContext, ivInterceptors);
        return invocationContext;
    }

    /**
     * Creates a new uninitialized InvocationContext instance.
     */
    protected InvocationContextImpl createInvocationContext() {
        return new InvocationContextImpl();
    }

    /**
     * Method for calling AroundConstruct interceptors and setting the enterprise bean instance. <p>
     */
    private InvocationContextImpl<?> callAroundConstructInterceptors(InterceptorProxy[] proxies, CallbackContextHelper contextHelper) {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "callAroundConstructInterceptors");
        InvocationContextImpl invCtx = createInvocationContext();
        try {
            BeanMetaData bmd = home.beanMetaData;
            ManagedObjectFactory<?> managedObjectFactory = bmd.ivEnterpriseBeanFactory;
            invCtx.initializeForAroundConstruct(ivEjbManagedObjectContext, ivInterceptors, proxies);

            if (managedObjectFactory == null) {
                setEnterpriseBean(invCtx.aroundConstruct(this, new Object[0], null));
            } else {
                ivManagedObject = managedObjectFactory.createManagedObject(invCtx);
                setEnterpriseBean(ivManagedObject.getObject());
            }

            return invCtx;
        } catch (Throwable t) {
            if (! (t instanceof ManagedObjectException)) {
                FFDCFilter.processException(t, CLASS_NAME + ".callAroundConstructInterceptors", "377", this);
                // If a root cause exception was captured, then return the context and let the caller
                // properly report the failure and cause; otherwise throw generic EJBException.
                if (invCtx.ivAroundConstructException != null) {
                    return invCtx;
                }
                throw ExceptionUtil.EJBException("AroundConstruct interceptor failure", t);
            } else {
                Throwable cause = t.getCause();
                FFDCFilter.processException(cause, CLASS_NAME + ".callAroundConstructInterceptors", "386", this);
                // If a root cause exception was captured, then return the context and let the caller
                // properly report the failure and cause; otherwise throw generic EJBException.
                if (invCtx.ivAroundConstructException != null) {
                    return invCtx;
                }
                throw ExceptionUtil.EJBException("AroundConstruct interceptor failure", cause);
            }
        } finally {
            if (contextHelper != null) {
                contextHelper.resetContextData();
            }
            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "callAroundConstructInterceptors");
        }
    }

    @Override
    public Constructor<?> getConstructor() {
        return home.beanMetaData.getEnterpriseBeanClassConstructor();
    }

    @Override
    public Object proceed(Object[] parameters, Map data) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "proceed: calling Constructor.newInstance");
        return getConstructor().newInstance(parameters);
    }
}
