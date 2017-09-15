/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.threadcontext.jca;

import java.util.Map;

import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * In order to handle inflow context for JCA, a component must register an implementation of both
 * JCAContextProvider and ThreadContextProvider.
 * 
 * A single implementation can provide both. This is useful in cases where inflow context is automatically
 * enabled whenever the general type of thread context is enabled.
 * For example,
 * 
 * com.ibm.ws.mycomponent.MyContextProvider;\
 * provide:='com.ibm.wsspi.threadcontext.JCAContextProvider,com.ibm.wsspi.threadcontext.ThreadContextProvider';\
 * implementation:=com.ibm.ws.mycomponent.internal.MyContextProvider;\
 * configuration-policy:=ignore;\
 * properties:='type=com.ibm.ws.mycomponent.MyWorkContext'
 * 
 * Or an implementation of each can be registered. This is useful in cases where the inflow context must be
 * manually enabled by a separate feature.
 * For example,
 * 
 * com.ibm.ws.mycomponent.YourContextProvider;\
 * provide:='com.ibm.wsspi.threadcontext.ThreadContextProvider';\
 * implementation:=com.ibm.ws.mycomponent.internal.YourContextProvider;\
 * configuration-policy:=ignore,\
 * 
 * com.ibm.ws.mycomponent.YourJCAContextProvider;\
 * provide:='com.ibm.wsspi.threadcontext.JCAContextProvider';\
 * implementation:=com.ibm.ws.mycomponent.internal.YourJCAContextProvider;\
 * configuration-policy:=ignore;\
 * properties:='context.name=com.ibm.ws.mycomponent.YourContextProvider,type=com.ibm.ws.mycomponent.YourWorkContext'
 * 
 * The context.name property identifies the type of thread context that corresponds to the inflow context created by this
 * JCA context provider. When inflow context is applied to a thread, it replaces any previously captured context of this type.
 * This property is optional for a single implementation that provides both interfaces.
 * 
 * The type property identifies which WorkContext (or ExecutionContext) is handled by this JCA context provider.
 * The getInflowContext(workContext) method will only be invoked with this particular type of context,
 * possibly including subclasses if there is no other handler for the subclass.
 * For example, a JCAContextProvider that handles type=javax.resource.spi.ExecutionContext
 * will also handle subclass javax.resource.spi.TransactionContext if no other
 * JCAContextProvider specifically declares that type.
 */
public interface JCAContextProvider {
    /**
     * Name of service property that identifies the type of thread context that corresponds to the inflow context created by this
     * JCA context provider. When inflow context is applied to a thread, it replaces any previously captured context of this type.
     */
    public static final String CONTEXT_NAME = "context.name";

    /**
     * Name of service property that identifies which WorkContext (or ExecutionContext) is handled by this JCA context provider.
     */
    public static final String TYPE = "type";

    /**
     * Creates thread context corresponding to the specified JCA ExecutionContext or WorkContext.
     * 
     * If the supplied work context implements javax.resource.spi.work.WorkContextLifecycleListener,
     * then the ThreadContext returned by this method must issue the contextSetupComplete/contextSetupFailed
     * notifications whenever the work context is applied (or fails to be applied) to a thread.
     * 
     * The inflow context must be applied to the thread upon ThreadContext.taskStarting and the previous thread
     * context restored upon ThreadContext.taskStopping.
     * If the context cannot be established for any reason, taskStarting must raise RejectedExecutionException
     * with a chained javax.resource.spi.work.WorkCompletedException with the appropriate error code from
     * javax.resource.spi.work.WorkContextErrorCodes.
     * 
     * @param workContext javax.resource.spi.work.ExecutionContext
     *            or a type of javax.resource.spi.work.WorkContext (such as javax.resource.spi.work.SecurityContext)
     * @param execProps execution properties that provide information about the task.
     * @return context that can be applied to a thread
     */
    ThreadContext getInflowContext(Object workContext, Map<String, String> execProps);
}