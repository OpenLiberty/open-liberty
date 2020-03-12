/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container;

import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ejs.container.CallbackContextHelper.Tx;
import com.ibm.ejs.j2c.HandleListInterface;
import com.ibm.ejs.util.FastStack;
import com.ibm.ejs.util.Util;
import com.ibm.websphere.csi.CSIException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.threadContext.ConnectionHandleAccessorImpl;
import com.ibm.ws.threadContext.ThreadContext;
import com.ibm.ws.threadContext.ThreadContextImpl;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * EJB data for the currently running thread. This data structure is
 * particularly optimized for use by EJB methods. Do not allocate new objects
 * from the constructor (including member variables) unless those objects are
 * likely to be used by the EJB preInvoke/postInvoke code paths.
 */
public class EJBThreadData
{
    private static final TraceComponent tc = Tr.register(EJBThreadData.class, "EJBContainer", "com.ibm.ejs.container.container");

    static ThreadContextAccessor svThreadContextAccessor =
                    AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    /**
     * Obtains a context object optimized for accessing the current thread.
     */
    private static <T> ThreadContext<T> getCurrentThreadContext(ThreadContext<T> threadContext)
    {
        return ((ThreadContextImpl<T>) threadContext).get();
    }

    /**
     * A stack of objects used to manage the context class loader. When a new
     * class loader is set, the old class loader is pushed onto this stack. If
     * the new class loader and old class loader are the same, Boolean.FALSE is
     * pushed instead.
     */
    private final FastStack<Object> ivClassLoaderStack = new FastStack<Object>();

    final ThreadContext<com.ibm.ws.jca.cm.handle.HandleListInterface> ivHandleListContext =
                    getCurrentThreadContext(ConnectionHandleAccessorImpl.getConnectionHandleAccessor().getThreadContext()); // d662032

    /**
     * Stack of objects containing the actively running EJB methods or callbacks.
     */
    private final FastStack<EJSDeployedSupport> ivEJSDeployedSupportStack = new FastStack<EJSDeployedSupport>(); // RTC102449

    /**
     * A ThreadContext corresponding to the ComponentMetaData stack for the
     * current thread only.
     */
    @SuppressWarnings("deprecation")
    final ThreadContext<ComponentMetaData> ivComponentMetaDataContext =
                    getCurrentThreadContext(ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getThreadContext()); // d646139.1

    /**
     * A lazily constructed reference to a cached set of EJBMethodInfoImpl
     * objects used by EJSContainer.mapMethodInfo.
     */
    private EJBMethodInfoStack ivEJBMethodInfoStack;

    /**
     * Cached data from the EJBMethodInfoImpl corresponding to a finder method.
     * When a finder method is called run, an initial set of beans are hydrated,
     * and that process depends on settings from the finder EJBMethodInfoImpl as
     * found via EJSDeployedSupport on the thread. When a finder method
     * hydrates additional beans, the EJSDeployedSupport will no longer
     * correspond to the finder method, so those settings are set in this field.
     * See CMFinderHelper.enumerateIntoArray, which sets this state and
     * ContainerManagedBeanO.setCMP11LoadedForUpdate, which indirectly accesses
     * the settings.
     */
    CMP11CustomFinderAccIntentState ivCMP11CustomFinderAccIntentState; // d112604.5

    /**
     * Reference to the bean that the container is currently processing via a
     * business method or a lifecycle callback. This field is set to null when
     * there is not an active bean for the thread. This field is intended to be
     * used by code that needs to know the current bean on the thread, such as
     * the various object factories or the Timer methods.
     *
     * <p>This field differs from EJSDeployedSupport.beanO because either there
     * might not be an EJB method being invoked (background thread passivation,
     * remote transaction commit, etc.) or because callbacks should be performed
     * on a different bean, which primarily occurs during home.create of an
     * entity or stateful: the method bean is the home, but the callback bean is
     * the bean being created.
     */
    private final FastStack<BeanO> ivCallbackBeanOStack = new FastStack<BeanO>();

    /**
     * The method context index corresponding to the lifecycle callback that is
     * currently active on this thread, or <tt>-1</tt> if no lifecycle callback
     * method is active. This index is used to determine whether or not the
     * currently executing code on the thread is a lifecycle callback method, an
     * EJB method, or neither. This field has three relative values:
     *
     * <ul>
     * <li>If the index is <tt>-1</tt>, no lifecycle callback is active.
     * <li>If the index is equal to the method context index, then a lifecycle
     * callback is active.
     * <li>If the index is less than the method context index, then a lifecycle
     * callback method has called another EJB method.
     * </ul>
     */
    int ivLifecycleMethodContextIndex = -1; // d644886

    /**
     * The transaction context the current lifecycle method was requested to begin.
     * The field has three values:
     *
     * <ul>
     * <li>null - either no transaction context was requested to begin, or not in a lifecycle method
     * <li>LTC - a local transaction containment was requested
     * <li>Global - a global transaction was requested
     * </ul>
     */
    Tx ivLifecycleMethodBeginTx = null;

    /**
     * The context data associated with a lifecycle callback. This field is
     * lazily initialized by {@link #getContextData}. It must be saved and
     * cleared as {@link #ivLifecycleMethodContextIndex} is updated.
     */
    Map<String, Object> ivLifecycleContextData; // d644886

    /**
     * Pushes a method context on the thread.
     *
     * @return the previous method context, or null if none
     */
    // RTC102449
    public EJSDeployedSupport pushMethodContext(EJSDeployedSupport s)
    {
        EJSDeployedSupport previous = ivEJSDeployedSupportStack.peek();
        ivEJSDeployedSupportStack.push(s);
        return previous;
    }

    /**
     * Pops a method context off the thread.
     */
    // RTC102449
    public void popMethodContext()
    {
        ivEJSDeployedSupportStack.pop();
    }

    /**
     * Gets the current method context for this thread.
     *
     * @return the method context, or <tt>null</tt> if no EJB method is active
     *         on this thread
     */
    public EJSDeployedSupport getMethodContext() // d646139.1
    {
        return ivEJSDeployedSupportStack.peek();
    }

    /**
     * Returns the size of the method context stack.
     */
    // RTC102449
    public int getNumMethodContexts()
    {
        return ivEJSDeployedSupportStack.getTopOfStackIndex();
    }

    EJBMethodInfoStack getEJBMethodInfoStack()
    {
        if (ivEJBMethodInfoStack == null)
        {
            ivEJBMethodInfoStack = new EJBMethodInfoStack(8);
        }

        return ivEJBMethodInfoStack;
    }

    /**
     * Gets a reference to the bean that the container is currently processing
     * via a business method or lifecycle callback.
     *
     * @return the callback bean, or null if the container is not processing a
     *         bean
     */
    public BeanO getCallbackBeanO()
    {
        BeanO result = ivCallbackBeanOStack.peek();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "getCallbackBeanO: " + result);
        return result;
    }

    /**
     * Updates the bean that the container is currently processing via a
     * business method or lifecycle callback, and establishes a thread context
     * specific to the bean.
     *
     * @throws CSIException if an exception occurs while reassociating handles
     */
    public void pushCallbackBeanO(BeanO bean) // d662032
    throws CSIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "pushCallbackBeanO: " + bean);

        HandleListInterface hl = bean.reAssociateHandleList();

        ivHandleListContext.beginContext(hl);
        ivCallbackBeanOStack.push(bean);
    }

    /**
     * Restores the callback bean after removing the thread context established
     * by a previous call to {@link #pushCallbackBeanO}.
     */
    public void popCallbackBeanO() // d662032
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && tc.isDebugEnabled())
            Tr.debug(tc, "popCallbackBeanO: " + ivCallbackBeanOStack.peek());

        ivHandleListContext.endContext();
        BeanO beanO = ivCallbackBeanOStack.pop();
        beanO.parkHandleList();
    }

    /**
     * Sets the thread context class loader for the specified bean metadata, and
     * saves the current thread context class loader.
     */
    public void pushClassLoader(BeanMetaData bmd)
    {
        ClassLoader classLoader = bmd.ivContextClassLoader; // F85059
        Object origCL = svThreadContextAccessor.pushContextClassLoaderForUnprivileged(classLoader);
        ivClassLoaderStack.push(origCL);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "pushClassLoader: " +
                         (origCL == ThreadContextAccessor.UNCHANGED ?
                                         "already " + Util.identity(classLoader) :
                                         Util.identity(origCL) + " -> " + Util.identity(classLoader)));
    }

    private void popClassLoader(String method)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
            Object origCL = ivClassLoaderStack.peek();
            ClassLoader currentCL = svThreadContextAccessor.getContextClassLoaderForUnprivileged(Thread.currentThread());
            Tr.debug(tc, method + ": " +
                         (origCL == ThreadContextAccessor.UNCHANGED ?
                                         "leaving " + Util.identity(currentCL) :
                                         Util.identity(currentCL) + " -> " + Util.identity(origCL)));
        }

        Object origCL = ivClassLoaderStack.pop();
        svThreadContextAccessor.popContextClassLoaderForUnprivileged(origCL);
    }

    /**
     * Calls {@link Thread#setContextClassLoader} with the class loader saved by
     * the previous call to {@link #pushClassLoader}.
     */
    public void popClassLoader()
    {
        popClassLoader("popClassLoader");
    }

    /**
     * Calls {@link Thread#setContextClassLoader} with the class loader saved by
     * the previous call to {@link #pushClassLoader}, and causes the next call to
     * this method to do nothing.
     */
    void popORBWrapperClassLoader()
    {
        popClassLoader("popORBWrapperClassLoader");

        // When called from the wrapper postInvoke, we need to push UNCHANGED
        // because postInvokeORBDispatch will also call popClassLoader().
        ivClassLoaderStack.push(ThreadContextAccessor.UNCHANGED);
    }

    /**
     * Establishes thread contexts for the specified bean. The effects of this
     * method can be undone by calling {@link #popMetaDataContexts}.
     */
    public void pushMetaDataContexts(BeanMetaData bmd) {

        // Pushing ComponentMetaData and ClassLoader are infallible.
        ivComponentMetaDataContext.beginContext(bmd);
        pushClassLoader(bmd);
    }

    /**
     * Removes the contexts pushed by {@link #pushMetaDataContexts}.
     */
    public void popMetaDataContexts() {
        popClassLoader();
        ivComponentMetaDataContext.endContext();
    }

    /**
     * Establishes thread contexts for the specified bean. The effects of this
     * method can be undone by calling {@link #popContexts}.
     *
     * @throws CSIException if an exception occurs while pushing contexts
     */
    public void pushContexts(BeanO beanO) throws CSIException {
        // Push the callback BeanO first since this is fallible.
        pushCallbackBeanO(beanO);

        // Pushing ComponentMetaData and ClassLoader are infallible.
        BeanMetaData bmd = beanO.home.beanMetaData;
        pushMetaDataContexts(bmd);
    }

    /**
     * Removes the contexts pushed by {@link #pushContexts}.
     */
    public void popContexts() {
        popMetaDataContexts();
        popCallbackBeanO();
    }

    /**
     * Returns true if the thread is currently executing an EJB lifecycle
     * callback interceptor method.
     */
    public boolean isLifecycleMethodActive() // d704496
    {
        return ivLifecycleMethodContextIndex == getNumMethodContexts();
    }

    /**
     * Returns true if the thread is currently executing an EJB lifecycle callback
     * interceptor method and either an LTC or global transactions was started
     * for the lifecycle callback method.
     */
    public boolean isLifeCycleMethodTransactionActive() {
        return isLifecycleMethodActive() && ivLifecycleMethodBeginTx != null;
    }

    /**
     * Gets the context data associated with the current EJB method or EJB
     * lifecycle callback.
     *
     * @see BeanO#getContextData
     * @throws IllegalStateException if neither an EJB method nor an EJB
     *             lifecycle method is active on the thread
     */
    public Map<String, Object> getContextData() // d644886
    {
        final boolean isTraceOn = TraceComponent.isAnyTracingEnabled();

        Map<String, Object> contextData;
        if (isLifecycleMethodActive()) // d704496
        {
            if (ivLifecycleContextData == null)
            {
                ivLifecycleContextData = new HashMap<String, Object>();

                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "getContextData: created empty");
            }
            else
            {
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "getContextData: lifecycle");
            }

            contextData = ivLifecycleContextData;
        }
        else
        {
            if (isTraceOn && tc.isDebugEnabled())
                Tr.debug(tc, "getContextData: method");

            EJSDeployedSupport s = getMethodContext();
            if (s == null)
            {
                IllegalStateException ex = new IllegalStateException(
                                "Context data not available outside the scope of an EJB or lifecycle callback method");
                if (isTraceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "getContextData: " + ex);
                throw ex;
            }

            contextData = s.getContextData();
        }

        return contextData;
    }
}
