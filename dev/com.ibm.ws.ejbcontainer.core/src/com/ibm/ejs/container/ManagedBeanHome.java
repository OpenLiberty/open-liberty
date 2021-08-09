/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.rmi.RemoteException;

import javax.ejb.CreateException;

import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.managedobject.ManagedObjectContext;

/**
 * Specialized EJSHome for creating ManagedBean instances.
 */
public final class ManagedBeanHome extends EJSHome
{
    private static final long serialVersionUID = -2989564306972856906L;

    private static final String CLASS_NAME = ManagedBeanHome.class.getName();

    private static final TraceComponent tc = Tr.register(ManagedBeanHome.class, "EJBContainer",
                                                         "com.ibm.ejs.container.container");

    /**
     * A weak reference cache between ManagedBean wrapper and the ManagedBean
     * instance used to call PreDestroy when the application has dropped all
     * references to the ManagedBean wrapper, or the application is uninstalled.
     * 
     * This field is only set when a PreDestroy callback exists.
     */
    // F743-34301.1
    private WeakManagedBeanCache ivWeakCache;

    /**
     * Default no-argument constructor.
     */
    public ManagedBeanHome() throws RemoteException
    {
        super();
    }

    /**
     * Overridden to avoid almost all of the normal EJSHome initialization,
     * as ManagedBeans have very little metadata. <p>
     * 
     * Specifically, this avoids creating a bean pool, but also avoids
     * the checking that provides no value for ManagedBeans. <p>
     */
    @Override
    public void completeInitialization()
                    throws RemoteException
    {
        enterpriseBeanClass = beanMetaData.enterpriseBeanClass;
        localEJBObjectClass = beanMetaData.localImplClass;

        // If a PreDestroy callback exists, then a weak reference cache is used
        // to keep track of instances, so PreDestroy may be called when all
        // references are dropped, or the application is uninstalled. F743-34301.1
        if (beanMetaData.ivInterceptorMetaData != null &&
            beanMetaData.ivInterceptorMetaData.ivPreDestroyInterceptors != null)
        {
            ivWeakCache = WeakManagedBeanCache.instance();
        }
    }

    /**
     * Disable this home instance and free all resources associated with it. <p>
     * 
     * Overridden to remove any ManagedBean instances from the weak reference
     * cache, and destroy them (i.e. PreDestroy called). <p>
     */
    // F743-34301.1
    @Override
    public synchronized void destroy()
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "destroy home " + j2eeName);

        if (!enabled) {
            return;
        }

        if (ivWeakCache != null) {
            ivWeakCache.remove(this);
        }

        super.destroy();

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "destroy home " + j2eeName);
    }

    /**
     * Return true iff this home contains managed beans. <p>
     */
    @Override
    public boolean isManagedBeanHome()
    {
        return true;
    }

    /**
     * Returns an Object 'representing' the specified ManagedBean, which
     * is managed by this home. This may or may not be the actual ManagedBean
     * instance. Generally, a wrapper object will be returned when AroundInvoke
     * interceptors are present for the ManagedBean. <p>
     * 
     * This method provides a Business Object (wrapper) factory capability,
     * for use during business interface injection or lookup. It is
     * called directly for Managed beans. <p>
     * 
     * @param businessInterface One of the local business interfaces or remote
     *            business interfaces for this session bean.
     * 
     * @return The business object (instance or wrapper) corresponding to the
     *         given business interface.
     */
    @Override
    public Object createBusinessObject(String businessInterface, boolean useSupporting)
                    throws CreateException,
                    RemoteException,
                    CSIException
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled(); // d532639.2

        if (isTraceOn && tc.isEntryEnabled())
            Tr.entry(tc, "createBusinessObject: " + businessInterface); // d367572.7

        homeEnabled();

        Object bean;

        try
        {
            BeanO beanO = beanOFactory.create(container, this, false);

            // In the normal case, just return the bean instance, or        d743117
            // if a wrapper is needed (PreDestroy or AroundInvoke) then create it
            // now to return instead of the bean instance. Unlike other wrappers,
            // this wrapper will contain a reference to the instance.  F743-34301.1
            if (beanMetaData.ivManagedBeanBeanOField == null)
            {
                bean = beanO.getBeanInstance();
            }
            else
            {
                bean = createManagedBeanWrapper(beanO);
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, CLASS_NAME + ".createBusinessObject",
                                        "129", this);

            for (Throwable cause = t.getCause(); cause != null; cause = cause.getCause()) {
                if (cause instanceof RuntimeException) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "createBusinessObject returning: " + cause);
                    throw (RuntimeException) cause;
                }

                if (cause instanceof Error) {
                    if (isTraceOn && tc.isEntryEnabled())
                        Tr.exit(tc, "createBusinessObject returning: " + cause);
                    throw (Error) cause;
                }
            }

            RuntimeException rex = new RuntimeException("Failure creating instance of " +
                                                        beanMetaData.j2eeName +
                                                        " ManagedBean : " + t, t);

            if (isTraceOn && tc.isEntryEnabled())
                Tr.exit(tc, "createBusinessObject returning: " + rex);
            throw rex;
        }

        if (isTraceOn && tc.isEntryEnabled())
            Tr.exit(tc, "createBusinessObject returning: " + Util.identity(bean));

        return bean;
    }

    /**
     * Create a ManagedBean wrapper for the specified BeanO. <p>
     * 
     * Managed bean wrappers are generated (only when needed) and are very
     * similar to No-Interface View wrappers in that they subclass the bean
     * implementation class, but are different from other wrappers in that
     * there is a direct link between the wrapper instance and the bean instance
     * (i.e. 1-to-1). <p>
     * 
     * A ManagedBean class is only generated when a PreDestroy or AroundInvoke
     * interceptor is present. This method should only be called when a
     * ManagedBean wrapper class has been generated. <p>
     * 
     * ManagedBean wrappers are returned to the application and are used to
     * control the bean instance lifecycle. When all references to a ManagedBean
     * wrapper are dropped, the bean instance becomes eligible for PreDestroy,
     * if a PreDestroy method exists. <p>
     * 
     * @param beanO managed bean instance wrapper and context.
     * 
     * @return a ManagedBean wrapper for the given bean.
     * @throws InstantiationException if the wrapper fails to create.
     * @throws IllegalAccessException if fields are the wrapper may not be accessed.
     **/
    // F743-34301.1
    private Object createManagedBeanWrapper(BeanO beanO)
                    throws IllegalAccessException,
                    InstantiationException
    {
        Object wrapper = beanMetaData.ivBusinessLocalImplClasses[0].newInstance();
        BusinessLocalWrapper bLocal = new BusinessLocalWrapper();
        beanMetaData.ivLocalBeanWrapperField.set(wrapper, bLocal);
        beanMetaData.ivManagedBeanBeanOField.set(wrapper, beanO);

        // set the invariant fields
        bLocal.container = container;
        bLocal.wrapperManager = wrapperManager;
        bLocal.ivCommon = null; // Not cached
        bLocal.isManagedWrapper = false; // Not managed
        bLocal.ivInterface = WrapperInterface.BUSINESS_LOCAL;

        // fill in the wrapper based on this home
        bLocal.beanId = beanO.beanId;
        bLocal.bmd = beanMetaData;
        bLocal.methodInfos = beanMetaData.localMethodInfos;
        bLocal.methodNames = beanMetaData.localMethodNames;
        bLocal.isolationAttrs = null; // not used for EJB 2.x
        bLocal.ivPmiBean = pmiBean;

        if (ivWeakCache != null) {
            ivWeakCache.add(bLocal, beanO);
        }

        return wrapper;
    }

    /**
     * Method to create a local business reference object. Override EJSHome
     * to ensure to handle managed beans properly. Use the createBusinessObject
     * method specific to managed beans.
     * 
     * @param interfaceName Remote interface name used instead of the class to avoid class loading
     * @param useSupporting (not used for Managed Bean's)
     * 
     * @throws RemoteException
     * @throws CreateException
     */
    @Override
    public Object createLocalBusinessObject(String interfaceName, boolean useSupporting)
                    throws RemoteException,
                    CreateException
    {
        return createBusinessObject(interfaceName, useSupporting);
    }

    /**
     * Method to create a local business reference object. Override EJSHome
     * to ensure to handle managed beans properly. Use the createBusinessObject
     * method specific to managed beans.
     */
    @Override
    public Object createLocalBusinessObject(int interfaceIndex, ManagedObjectContext context)
                    throws RemoteException,
                    CreateException
    {
        return createBusinessObject(null, false);
    }

    /**
     * Method to create a remote business reference object. Override EJSHome
     * to ensure to handle managed beans properly. Use the createBusinessObject
     * method specific to managed beans.
     * 
     * @param interfaceName Remote interface name used instead of the class to avoid class loading
     * @param useSupporting (not used for Managed Bean's)
     * 
     * @throws RemoteException
     * @throws CreateException
     */
    @Override
    public Object createRemoteBusinessObject(String interfaceName, boolean useSupporting)
                    throws RemoteException,
                    CreateException
    {
        return createBusinessObject(interfaceName, useSupporting);
    }
}
