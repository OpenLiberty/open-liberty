/*******************************************************************************
 * Copyright (c) 2012,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.threadfactory.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextProvider;
import com.ibm.wsspi.threadcontext.WSContextService;

import test.threadfactory.ContextualThread;
import test.threadfactory.CurrentThreadExecutor;

/**
 * This a fake thread factory implementation that demonstrates how to use the context service,
 * and more importantly, allows us to test serialization/deserialization of context.
 */
public class ThreadFactoryImpl implements ThreadFactory {
    private ThreadContextProvider classloaderContextProvider;
    public WSContextService contextSvc;
    private ThreadContextProvider jeeMetadataContextProvider;

    String id;
    Callable<Class<?>> loadClass;
    ServiceRegistration<Executor> registration;

    protected void activate(ComponentContext context, Map<String, Object> properties) {
        id = (String) properties.get("id");

        // Put an executor into the service registry that will run tasks with the thread context
        // that we capture from component activate!
        Map<String, String> executionProperties = new LinkedHashMap<String, String>();
        executionProperties.put("com.ibm.ws.concurrent.TASK_OWNER", "myThreadFactory");
        executionProperties.put("jakarta.enterprise.concurrent.IDENTITY_NAME", "contextualized executor");
        executionProperties.put("javax.enterprise.concurrent.IDENTITY_NAME", "contextualized executor");
        ThreadContextDescriptor capturedThreadContext = contextSvc.captureThreadContext(executionProperties);
        capturedThreadContext.set("com.ibm.ws.classloader.context.provider",
                                  classloaderContextProvider.captureThreadContext(executionProperties, null));
        capturedThreadContext.set("com.ibm.ws.javaee.metadata.context.provider",
                                  jeeMetadataContextProvider.captureThreadContext(executionProperties, null));
        Executor contextualExecutor = contextSvc.createContextualProxy(capturedThreadContext, new CurrentThreadExecutor(), Executor.class);
        BundleContext bundleContext = FrameworkUtil.getBundle(ThreadFactoryImpl.class).getBundleContext();
        Dictionary<String, String> serviceProps = new Hashtable<String, String>();
        serviceProps.put("owner", "myThreadFactory");
        registration = bundleContext.registerService(Executor.class, contextualExecutor, serviceProps);
    }

    protected void deactivate(ComponentContext context) {
        registration.unregister();
    }

    /** {@inheritDoc} */
    @Override
    public Thread newThread(Runnable r) {
        Map<String, String> executionProperties = new LinkedHashMap<String, String>();
        executionProperties.put("com.ibm.ws.concurrent.TASK_OWNER", "myThreadFactory");
        executionProperties.put("jakarta.enterprise.concurrent.IDENTITY_NAME", r.getClass().getName());
        executionProperties.put("javax.enterprise.concurrent.IDENTITY_NAME", r.getClass().getName());
        try {
            // Reflectively invoke:
            // r = ((ContextService) contextSvc).createContextualProxy(r, executionProperties, Runnable.class);
            r = (Runnable) contextSvc.getClass()
                            .getMethod("createContextualProxy", Object.class, Map.class, Class.class)
                            .invoke(contextSvc, r, executionProperties, Runnable.class);
        } catch (RuntimeException x) {
            throw x;
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
        return new ContextualThread(id, r);
    }

    protected void setContextService(WSContextService svc) {
        contextSvc = svc;
    }

    protected void setClassloaderContextProvider(ThreadContextProvider svc) {
        classloaderContextProvider = svc;
    }

    protected void setJeeMetadataContextProvider(ThreadContextProvider svc) {
        jeeMetadataContextProvider = svc;
    }

    protected void unsetContextService(WSContextService svc) {
        contextSvc = null;
    }

    protected void unsetClassloaderContextProvider(ThreadContextProvider svc) {
        classloaderContextProvider = null;
    }

    protected void unsetJeeMetadataContextProvider(ThreadContextProvider svc) {
        jeeMetadataContextProvider = null;
    }
}
