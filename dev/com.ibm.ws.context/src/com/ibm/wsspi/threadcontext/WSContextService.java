/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.threadcontext;

import java.util.Map;

/**
 * <p>Captures thread context and applies it to invocations of methods on any Java interface.
 * For example, Runnable.run and Callable.call methods can be made to run with a previously captured thread context.</p>
 * 
 * <p>The contextService-1.0 feature provides an unconfigurable singleton instance of this interface
 * (without any types of thread context capture/propagation configured),
 * which can be found in the OSGi service registry using the filter:<br>
 * (service.pid=com.ibm.ws.context.manager)</p>
 * 
 * <p>The concurrent-1.0 feature provides configurable instances of this interface (in addition to
 * the EE spec interface javax.enterprise.concurrent.ContextService), including a default instance,
 * which can be found in the OSGi service registry using the filter:<br>
 * (id=DefaultContextService)</p>
 * 
 * <p>A component with a dependency on WSContextService can be written in such a way that a configured
 * instance of contextService is used if specified, and otherwise the unconfigurable singleton instance
 * is used. This allows your component to function with only the contextService-1.0 feature enabled,
 * but to also take advantage of the concurrent-1.0 feature if enabled by the user.</p>
 * 
 * <p>To accomplish this, declare the dependency in your bnd file (or define annotatively),
 * <pre> {@literal @}Reference(service = WSContextService.class, target = "(id=unbound)", ...)
 * protected void setContextService(WSContextService contextSvc)</pre>
 * and include attribute definitions like the following in your metatype.xml,
 * <pre> {@literal <}AD id="contextServiceRef" type="String" required="false" ibm:type="pid" ibm:reference="com.ibm.ws.context.service" name="%contextService"
 * description="%contextService.desc"/></pre>
 * <pre> {@literal <}AD id="ContextService.target" type="String"
 * default="(|(service.pid=${contextServiceRef})({@literal &}amp;(service.pid=com.ibm.ws.context.manager)
 * (|(service.pid{@literal &}gt;=${contextServiceRef})(default.for{@literal &}lt;=${contextServiceRef}))))"
 * ibm:final="true" name="internal" description="internal use only"/>
 * </pre>
 * </p>
 * TODO: need a more usable pattern than the ugly filter above (design issue is open)
 */
public interface WSContextService {
    // TODO: give these constants a better home before we actually expose the SPI

    /**
     * Value for DEFAULT_CONTEXT that indicates all types of thread context (whether configured, unconfigured, or automatic)
     * should be defaulted.
     */
    static final String ALL_CONTEXT_TYPES = "ALL_CONTEXT_TYPES";

    /**
     * Name of an execution property that specifies the method names (delimited by comma) to which captured thread context should be applied.
     * When unspecified, previously captured thread context is applied to all interface methods that aren't defined on java.lang.Object.
     * So, for example, myTask.doSomething would run with context but .toString or .equals would not.
     */
    static final String CONTEXTUAL_METHODS = "com.ibm.ws.concurrent.CONTEXTUAL_METHODS";

    /**
     * Name of an execution property that specifies which, if any, default context to apply to threads.
     * Default thread context is either empty or constructed according to other execution properties.
     * If unspecified, default context is not applied to threads.
     */
    static final String DEFAULT_CONTEXT = "com.ibm.ws.concurrent.DEFAULT_CONTEXT";

    /**
     * Name of an execution property that specifies a list of thread context provider PIDs that we should
     * completely ignore, neither capturing thread context nor applying any sort of default context
     * from these thread context providers to the thread of execution.
     * The list of pids is delimited by the comma (,) separator.
     * Example value:
     * com.ibm.ws.javaee.metadata.context.provider,com.ibm.security.thread.zos.context.provider,com.ibm.ws.transaction.context.provider
     */
    static final String SKIP_CONTEXT_PROVIDERS = "com.ibm.ws.concurrent.SKIP_CONTEXT_PROVIDERS";

    /**
     * Name of an execution property that identifies the submitter of the task.
     * When unspecified, this defaults to the name of the resource
     * (for example, the ContextService or ManagedExecutorService) that is used to submit the task.
     */
    static final String TASK_OWNER = "com.ibm.ws.concurrent.TASK_OWNER";

    /**
     * Name of an execution property that will override section 3.3.4 of the EE Concurrency spec:<br>
     * <i>&quot;All invocations to any of the proxied interface methods will fail with a
     * java.lang.IllegalStateException exception if the application component is not started or deployed.&quot;</i>
     * <p>Specifying a value of &quot;false&quot; will override this requirement and allow context to be applied
     * to an application component that is not started or deployed, without throwing an IllegalStateException.
     */
    static final String REQUIRE_AVAILABLE_APP = "com.ibm.ws.concurrent.REQUIRE_AVAILABLE_APP";

    /**
     * Name of the reference to thread context providers.
     */
    static final String THREAD_CONTEXT_PROVIDER = "threadContextProvider";

    /**
     * Value for DEFAULT_CONTEXT that indicates that types of thread context which are neither configured nor automatically captured
     * should be defaulted.
     */
    static final String UNCONFIGURED_CONTEXT_TYPES = "UNCONFIGURED_CONTEXT_TYPES";

    /**
     * <p>Captures context from the thread that invokes this method, or creates new thread context as determined
     * by the thread context configuration and execution properties.</p>
     * 
     * <p>The thread context configuration maps consist of name value pairs for configuration attributes
     * of a configurable thread context type. Each map must also include an entry with the
     * key of WSContextService.THREAD_CONTEXT_PROVIDER and its value set to the name of the
     * corresponding thread context provider.</p>
     * 
     * Some known thread context provider names include:<ul>
     * <li>com.ibm.ws.classloader.context.provider
     * <li>com.ibm.ws.javaee.metadata.context.provider
     * <li>com.ibm.ws.security.context.provider
     * <li>com.ibm.ws.security.thread.zos.context.provider
     * <li>com.ibm.ws.zos.wlm.context.provider
     * </ul>
     * 
     * An example that captures Java EE Metadata context and z/OS WLM context in addition to whatever context (if any)
     * was configured for the thread context service instance:
     * <pre>
     * Map<String, Object> jeeMetadataContextConfig = Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.javaee.metadata.context.provider");
     * Map<String, Object> zosWLMContextConfig = new HashMap<String, Object>();
     * zosWLMContextConfig.put(WSContextService.THREAD_CONTEXT_PROVIDER, "com.ibm.ws.zos.wlm.context.provider");
     * zosWLMContextConfig.put("daemonTransactionClass", "MYDMNCLS");
     * zosWLMContextConfig.put("defaultTransactionClass", "MYDFTCLS");
     * ThreadContextDescriptor capturedThreadContext = contextSvc.captureThreadContext(null, jeeMetadataContextConfig, zosWLMContextConfig);
     * </pre>
     * 
     * @param executionProperties execution properties. Custom property keys must not begin with "javax.enterprise.concurrent."
     * @param additionalThreadContextConfig list of additional thread context configurations to use when capturing thread context.
     *            If a type of thread context specified in the list is already present in the contextService configuration,
     *            then the entry from the list replaces it when capturing the thread context that is return by this method.
     * @return captured thread context.
     */
    ThreadContextDescriptor captureThreadContext(Map<String, String> executionProperties, Map<String, ?>... additionalThreadContextConfig);

    /**
     * Wrap an object with the specified thread context. When methods on the interface are invoked,
     * they run with that context.
     * 
     * @param threadContextDescriptor previously captured thread context.
     * @param instance instance to wrap with thread context.
     * @param intf the interface.
     * @return contextual proxy.
     */
    <T> T createContextualProxy(ThreadContextDescriptor threadContextDescriptor, T instance, Class<T> intf);
}