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
package com.ibm.wsspi.threadcontext;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface for providing thread context that can be captured and applied to other threads.
 * Some examples: classloaderContext, jeeMetadataContext, securityContext, zosWLMContext.
 * 
 * All thread context providers must be singletons. For example:
 * <pre>
 * com.ibm.ws.example.context.provider;\
 * provide:='com.ibm.wsspi.threadcontext.ThreadContextProvider';\
 * implementation:=com.ibm.ws.example.context.internal.MyContextProviderImpl;\
 * configuration-policy:=optional;\
 * </pre>
 * 
 * A service property is available to automatically enable thread context that is not
 * configurable per contextService:
 * <pre>
 * properties:='alwaysCaptureThreadContext:Boolean=true'
 * </pre>
 * 
 * Configuration of thread context at the per-contextService level is possible by extending the
 * metatype for the com.ibm.wsspi.threadcontext.config pid. For example:
 * <pre>
 * Designate factoryPid="com.ibm.ws.example.context"
 * Object ocdref="com.ibm.ws.example.context"
 * OCD id="com.ibm.ws.example.context" ibm:childAlias="exampleContext" ibm:extends="com.ibm.wsspi.threadcontext.config" ibm:parentPid="com.ibm.ws.context.service"
 * AD id="exampleProperty1" type="String" default="value1" ...
 * AD id="exampleProperty2" type="Boolean" default="true" ...
 * </pre>
 * 
 * If the pattern of naming the thread context provider as the name of the thread context configuration
 * with .provider appended is not followed, then an internal "threadContextProvider" attribute is needed
 * to identify the thread context provider. For example:
 * AD id="threadContextProvider" type="String" default="com.ibm.ws.example.context.provider" ibm:final="true" name="internal"
 * 
 * If allowing configurable thread context per contextService, consider whether or not to merge
 * a thread context configuration onto the default instance of contextService (id=DefaultContextService).
 * If appropriate, this can be done by providing a defaultInstances.xml file.
 * 
 * Instances of contextService coordinate multiple thread context providers
 * to capture context from threads and apply the context to other threads.
 * 
 * <pre>
 * Simplest usage pattern:
 * Thread1: myContext = myContextProvider.captureThreadContext(...)
 * Thread2: myContext.taskStarting()
 * Thread2: do something useful that requires myContext
 * Thread2: myContext.taskStopping()
 * 
 * Usage pattern with multiple threads:
 * Thread1: myContext = myContextProvider.captureThreadContext(...)
 * Thread2: myContext.taskStarting()
 * Thread3: myContext.taskStarting()
 * Thread2: do something useful that requires myContext
 * Thread3: do something useful that requires myContext
 * Thread2: myContext.taskStopping()
 * Thread3: myContext.taskStopping()
 * 
 * Usage pattern with multiple contexts:
 * Thread1: myContext1 = myContextProvider.captureThreadContext(...)
 * Thread2: myContext2 = myContextProvider.captureThreadContext(...)
 * Thread3: myContext1.taskStarting()
 * Thread3: do something useful that requires myContext1
 * Thread3: myContext2.taskStarting()
 * Thread3: do something useful that requires myContext2
 * Thread3: myContext1.taskStarting()
 * Thread3: do something useful that requires myContext1
 * Thread3: myContext1.taskStopping()
 * Thread3: do something useful that requires myContext2
 * Thread3: myContext2.taskStopping()
 * Thread3: do something useful that requires myContext1
 * Thread3: myContext1.taskStopping()
 * 
 * Usage pattern with repeated use on pooled thread (Thread2)
 * Thread1: myContext = myContextProvider.captureThreadContext(...)
 * Thread2: myContext.taskStarting()
 * Thread2: do something useful that requires myContext
 * Thread2: myContext.taskStopping()
 * Thread2: myContext.taskStarting()
 * Thread2: do something useful that requires myContext
 * Thread2: myContext.taskStopping()
 * Thread2: myContext.taskStarting()
 * Thread2: do something useful that requires myContext
 * Thread2: myContext.taskStopping()
 * </pre>
 * 
 * ThreadContextProvider implementations must be capable of handling all of the above
 * usage patterns, as well as all combinations of these patterns.
 */
public interface ThreadContextProvider {
    /**
     * Name of an optional, Boolean type service property for thread context providers.
     * This property is useful for thread context providers for which thread context is not configurable
     * per contextService. A value of true for this property indicates that this type of context should
     * always be captured.
     * 
     * Example specification of property in bnd file:
     * <pre>properties:='alwaysCaptureThreadContext:Boolean=true'</pre>
     */
    public static final String ALWAYS_CAPTURE_THREAD_CONTEXT = "alwaysCaptureThreadContext";

    /**
     * Captures the context of the current thread or creates new thread context,
     * as determined by the execution properties and configuration of this thread context provider.
     * It is also possible that execution properties might indicate that the contextual task should
     * instead run under the context of the thread where it executes.
     * For example, transaction context can have the execution property ManagedTask.TRANSACTION set to ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.
     * 
     * @param execProps execution properties that provide information about the contextual task.
     * @param threadContextConfig configuration for the thread context to be captured. Null if not configurable per contextService.
     * @return non-null thread context.
     * @see javax.enterprise.concurrent.ManagedTask#getExecutionProperties()
     */
    ThreadContext captureThreadContext(Map<String, String> execProps, Map<String, ?> threadContextConfig);

    /**
     * Creates default context that can be applied to a thread.
     * The value returned must be a new instance if the thread context implementation stores any state information
     * (for example, previous thread context to restore after a contextual task ends).
     * 
     * @param execProps execution properties that provide information about the contextual task.
     * @return default context that can be applied to a thread.
     * @see javax.enterprise.concurrent.ManagedTask#getExecutionProperties()
     */
    ThreadContext createDefaultThreadContext(Map<String, String> execProps);

    /**
     * Deserialize context from bytes.
     * It's important for the ThreadContextProvider to control deserialization
     * so that it can be done with the correct class loader.
     * Also, this gives the thread context provider control over whether or not
     * changes to configuration properties impact deserialized context.
     * 
     * Typical implementation of this method:
     * <pre>
     * ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
     * return (ThreadContext) in.readObject();
     * </pre>
     * 
     * @param info Object encapsulating the execution properties that provide information about the contextual task, as well as the metadata identity.
     * @param bytes serialized context
     * @return deserialized context.
     * @throws ClassNotFoundException if unable to find a class
     * @throws IOException if an error occurs during deserialization
     * @see javax.enterprise.concurrent.ManagedTask#getExecutionProperties()
     */
    ThreadContext deserializeThreadContext(ThreadContextDeserializationInfo info, byte[] bytes) throws ClassNotFoundException, IOException;

    /**
     * Optional list of prerequisite thread context providers.
     * Thread context from the prerequisite thread context providers must first be applied to the thread
     * before applying context from the thread context provider that declares the prerequisites.
     * A ThreadContextProvider is expected to use declarative services to enforce that all of these
     * prerequisites are available. For example, in bnd:
     * <pre>
     * jeeMetadataContextProvider='com.ibm.wsspi.threadcontext.ThreadContextProvider(component.name=com.ibm.ws.javaee.metadata.context.provider)';\
     * securityContextProvider='com.ibm.wsspi.threadcontext.ThreadContextProvider(component.name=com.ibm.ws.security.context.provider)';\
     * </pre>
     * 
     * @return a list of prerequisite thread context providers.
     */
    List<ThreadContextProvider> getPrerequisites();
}
