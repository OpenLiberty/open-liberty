/*******************************************************************************
 * Copyright (c) 2011,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.context.app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedTask;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.threadcontext.ThreadContextDescriptor;
import com.ibm.wsspi.threadcontext.ThreadContextDeserializer;
import com.ibm.wsspi.threadcontext.WSContextService;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/ContextServiceTestServlet")
public class ContextServiceTestServlet extends FATServlet {
    TraceComponent tc = Tr.register(ContextServiceTestServlet.class);

    static final BundleContext bundleContext = FrameworkUtil.getBundle(ContextServiceTestServlet.class.getClassLoader().getClass()).getBundleContext();

    @Resource(lookup = "concurrent/MapContextSvc")
    private ContextService mapContextSvc;

    private final ExecutorService unmanagedExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void destroy() {
        AccessController.doPrivileged((PrivilegedAction<?>) () -> unmanagedExecutor.shutdownNow());
        super.destroy();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    /**
     * Test that regardless of whether context is specified, if the DEFAULT_CONTEXT=ALL_CONTEXT_TYPES execution property is specified,
     * then default context is applied to the thread.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testApplyDefaultContextForAllContextTypes() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();

        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);

        ServiceReference<?> numSvcRef = bundleContext.getServiceReference("test.numeration.NumerationService");
        final Object numSvc = bundleContext.getService(numSvcRef);
        final Method NumerationService_toString = numSvc.getClass().getMethod("toString", long.class);
        serviceRefs.add(numSvcRef);

        ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
        final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
        serviceRefs.add(bufferSvcRef);

        try {
            mapSvc.put("someKey", "someValue");
            bufferSvc.append("someText");

            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/BufferContextSvc");

            Map<String, String> execProps = new TreeMap<String, String>();
            execProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.ALL_CONTEXT_TYPES);
            execProps.put(ManagedTask.IDENTITY_NAME, "someTaskIdentityName");
            execProps.put("test.numeration.context.radix", "5");

            Callable<Object[]> task = new Callable<Object[]>() {
                @Override
                public Object[] call() throws Exception {
                    Object[] results = new Object[3];
                    results[0] = mapSvc.get("someKey");
                    results[1] = NumerationService_toString.invoke(numSvc, Long.valueOf(586)); // 4*125 + 3*25 + 2*5 + 1
                    results[2] = bufferSvc.toString();
                    return results;

                }
            };

            @SuppressWarnings("unchecked")
            Callable<Object[]> contextualTask = contextSvc.createContextualProxy(task, execProps, Callable.class);

            Object[] results = contextualTask.call();

            if (results[0] != null)
                throw new Exception("Map should be empty, not have " + results[0]);

            if (!"4321".equals(results[1]))
                throw new Exception("Result should be 4321, not " + results[1]);

            String str = (String) results[2];
            if (!str.contains("someTaskIdentityName"))
                throw new Exception("Result does not contain task identity name. Instead: " + str);

            if (!str.contains("BufferContextSvc"))
                throw new Exception("Result does not contain task owner. Instead: " + str);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Test that when context isn't specified, if the DEFAULT_CONTEXT=UNCONFIGURED_CONTEXT_TYPES execution property is specified,
     * then default context is applied to the thread.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testApplyDefaultContextForUnconfiguredContextTypes() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();

        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);

        ServiceReference<?> numSvcRef = bundleContext.getServiceReference("test.numeration.NumerationService");
        final Object numSvc = bundleContext.getService(numSvcRef);
        final Method NumerationService_toString = numSvc.getClass().getMethod("toString", long.class);
        serviceRefs.add(numSvcRef);

        ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
        final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
        serviceRefs.add(bufferSvcRef);

        try {
            mapSvc.put("myKey", "myValue");
            bufferSvc.append("myText");

            ContextService contextSvc = (ContextService) new InitialContext().lookup("java:comp/env/concurrent/EmptyContextSvc");

            Map<String, String> execProps = new TreeMap<String, String>();
            execProps.put(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
            execProps.put(ManagedTask.IDENTITY_NAME, "myTaskIdentityName");
            execProps.put("test.numeration.context.radix", "9");

            Callable<Object[]> task = new Callable<Object[]>() {
                @Override
                public Object[] call() throws Exception {
                    Object[] results = new Object[3];
                    results[0] = mapSvc.get("myKey");
                    results[1] = NumerationService_toString.invoke(numSvc, Long.valueOf(922)); // 1*729 + 2*81 + 3*9 + 4
                    results[2] = bufferSvc.toString();
                    return results;
                }
            };

            @SuppressWarnings("unchecked")
            Callable<Object[]> contextualTask = contextSvc.createContextualProxy(task, execProps, Callable.class);

            Object[] results = contextualTask.call();

            if (results[0] != null)
                throw new Exception("Map should be empty, not have " + results[0]);

            if (!"1234".equals(results[1]))
                throw new Exception("Result should be 1234, not " + results[1]);

            String str = (String) results[2];
            if (!str.contains("myTaskIdentityName"))
                throw new Exception("Result does not contain task identity name. Instead: " + str);

            if (!str.contains("EmptyContextSvc"))
                throw new Exception("Result does not contain task owner. Instead: " + str);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Test thread context that we captured from a service component activate method.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testCaptureContextOfServiceComponent() throws Exception {
        ServiceReference<Executor> svcRef = bundleContext.getServiceReferences(Executor.class, "(owner=myThreadFactory)").iterator().next();
        Executor contextualExecutor = bundleContext.getService(svcRef);
        try {
            contextualExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Load some class from the bundle of the service component
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        // TODO: don't enable this until we decide what we want to do about classloader context for components
                        // TODO: if we enable this, we should also test the deserialization path!
                        //if (loader != null)
                        //    loader.loadClass("test.threadfactory.internal.ThreadFactoryImpl");

                        // Look up something that should only be available to an application
                        try {
                            Object result = new InitialContext().lookup("java:comp/env/concurrent/EmptyContextSvc");
                            throw new RuntimeException("Should not be able to look up " + result);
                        } catch (NamingException x) {
                        } // pass
                    } catch (RuntimeException x) {
                        throw x;
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                }
            });

        } finally {
            bundleContext.ungetService(svcRef);
        }
    }

    /**
     * Test classloader context
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testClassloaderContext() throws Exception {
        final String className = AppTask.class.getName();

        final Callable<Class<?>> loadClass = new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                return loader == null ? null : loader.loadClass(className);
            }
        };

        // Should work fine when invoked directly
        Class<?> loadedClass = loadClass.call();
        if (!className.equals(loadedClass.getName()))
            throw new Exception("Failed to load class " + className + " before applying context. Instead " + loadedClass.getName());

        ContextService contextSvc = (ContextService) new InitialContext().lookup("java:comp/DefaultContextService");

        @SuppressWarnings("unchecked")
        Callable<Class<?>> contextualLoadClass = contextSvc.createContextualProxy(loadClass, Callable.class);

        Map<String, String> xpropsSkipClassloaderContext = Collections.singletonMap(WSContextService.SKIP_CONTEXT_PROVIDERS, "com.ibm.ws.classloader.context.provider");
        @SuppressWarnings("unchecked")
        Callable<Class<?>> skipContextLoadClass = mapContextSvc.createContextualProxy(loadClass, xpropsSkipClassloaderContext, Callable.class);

        // Skip of classloaderContext should leave current classloader on thread
        loadedClass = skipContextLoadClass.call();
        if (!className.equals(loadedClass.getName()))
            throw new Exception("Failed to load class " + className + " when skipping classloaderContext. Instead " + loadedClass.getName());

        // Temporarily switch the classloader context so that we can try things out on the current thread.
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // switch the classloader of the current thread to something that can't load classes from the app
            ClassLoader newClassLoader = contextSvc.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(newClassLoader);

            // Apply previously captured context and load the class
            loadedClass = contextualLoadClass.call();
            if (!className.equals(loadedClass.getName()))
                throw new Exception("Failed to load class " + className + " after applying context. Instead " + loadedClass.getName());

            // Skip applying context and expect failure to load the class
            try {
                loadedClass = skipContextLoadClass.call();
                throw new Exception("Should not be able to load class " + loadedClass + " when skipping classloaderContext");
            } catch (ClassNotFoundException x) {
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * Test context propagation of the "map" context
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testContextSnapshot() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("item1", "value1");

            // Take a snapshot of context
            Runnable runnable = mapContextSvc.createContextualProxy(new Runnable() {
                @Override
                public void run() {
                    String value1 = mapSvc.get("item1");
                    if (!"value1".equals(value1))
                        throw new RuntimeException("Wrong value in context snapshot. " + mapSvc);
                    mapSvc.put("item1", value1 + "append");
                }
            }, Runnable.class);

            // change the context
            mapSvc.clear();
            mapSvc.put("item2", "value2");

            // Run as the old context snapshot
            runnable.run();

            // Verify that current context is restored
            if (mapSvc.get("item1") != null)
                throw new Exception("Context not cleaned up after contextual operation. " + mapSvc);

            if (!"value2".equals(mapSvc.get("item2")))
                throw new Exception("Context not restored after contextual operation. " + mapSvc);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Contextualize a task interface defined by the application.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testContextualizeAppDefinedClass() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("one", "1");
            mapSvc.put("two", "II");

            // capture thread context
            AppTask task1 = mapContextSvc.createContextualProxy(new AppWorkerTask(), AppTask.class);

            mapSvc.clear();

            // run task
            Object result1 = task1.doTask("one");
            if (!"1".equals(result1))
                throw new Exception("Unexpected task1 result: " + result1);

            // serialize the task
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(byteOutput);
            oos.writeObject(task1);
            oos.flush();
            byte[] bytes = byteOutput.toByteArray();
            oos.close();

            // deserialize
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            AppTask deserializedTask = (AppTask) in.readObject();
            in.close();

            // run task
            Object result2 = deserializedTask.doTask("two");
            if (!"II".equals(result2))
                throw new Exception("Unexpected deserializedTask result: " + result2);

            Map<String, String> execProps = mapContextSvc.getExecutionProperties(task1);
            if (execProps != null)
                throw new Exception("Execution properties must be null because none were specified. Instead: " + execProps);
            execProps = mapContextSvc.getExecutionProperties(deserializedTask);
            if (execProps != null)
                throw new Exception("Execution properties should not change after deserialization: " + execProps);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Verify that the contextualMethods setting properly controls which methods are invoked with context.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testContextualMethods() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("abc", "xyz");
            mapSvc.put("def", "uvw");
            mapSvc.put("ghi", "rst");
            mapSvc.put("jkl", "opq");

            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/MapAndNumContextSvc");

            Map<String, String> executionProperties = new ConcurrentHashMap<String, String>();
            executionProperties.put("com.ibm.ws.concurrent.CONTEXTUAL_METHODS", "call,compare,run");

            // Take a snapshot of context
            @SuppressWarnings("unchecked")
            Comparator<String> comparator = contextSvc.createContextualProxy(new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    int result = ((Comparable<String>) mapSvc.get(object1)).compareTo(mapSvc.get(object2));
                    return result < 0 ? -1 : result == 0 ? 0 : 1;
                }
            }, executionProperties, Comparator.class);

            @SuppressWarnings("unchecked")
            Comparable<String> comparable = contextSvc.createContextualProxy(new Comparable<String>() {
                @Override
                public int compareTo(String str) {
                    return str.compareTo(mapSvc.get(str));
                }
            }, executionProperties, Comparable.class);

            // change the context
            mapSvc.put("abc", "abc");
            mapSvc.put("def", "def");
            mapSvc.put("ghi", "ghi");
            mapSvc.put("jkl", "jkl");

            // "compare" should run as context snapshot
            int result = comparator.compare("abc", "def");
            if (result != 1)
                throw new Exception("The compare method did not appear to run with context. Result: " + result);

            result = comparator.compare("ghi", "def");
            if (result != -1)
                throw new Exception("Second invocation of compare method did not appear to run with context. Result: " + result);

            // "compareTo" should NOT run as context snapshot
            result = comparable.compareTo("abc");
            if (result != 0)
                throw new Exception("The compareTo method should run with current context, not the context snapshot. Result: " + result);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }

    }

    /**
     * Test capture and propagation of a "numeration" context with a configured attribute (radix=2)
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testContextWithConfiguredAttribute() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        ServiceReference<?> numSvcRef = bundleContext.getServiceReference("test.numeration.NumerationService");
        final Object numSvc = bundleContext.getService(numSvcRef);
        final Method NumerationService_setRadix = numSvc.getClass().getMethod("setRadix", int.class);
        final Method NumerationService_toString = numSvc.getClass().getMethod("toString", long.class);
        serviceRefs.add(numSvcRef);
        try {
            NumerationService_setRadix.invoke(numSvc, 16);

            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/NumContextSvc2");

            // Take a snapshot of context
            @SuppressWarnings("unchecked")
            Callable<String> callable = contextSvc.createContextualProxy(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String s = (String) NumerationService_toString.invoke(numSvc, 210);
                    NumerationService_setRadix.invoke(numSvc, 8);
                    return s;
                }
            }, Callable.class);

            // change the context
            NumerationService_setRadix.invoke(numSvc, 12);

            // Run as the old context snapshot
            Object s = callable.call();
            if (!"11010010".equals(s))
                throw new Exception("Wrong value in default context: " + s);

            // Verify that current context is restored
            s = NumerationService_toString.invoke(numSvc, 210);
            if (!"156".equals(s))
                throw new Exception("Context not restored after contextual operation. Incorrect value: " + s);
        } finally {
            NumerationService_setRadix.invoke(numSvc, 10); // restore to default
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Test default classloader context
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testDefaultClassloaderContext() throws Exception {
        final String className = AppTask.class.getName();

        final Callable<Class<?>> loadClass = new Callable<Class<?>>() {
            @Override
            public Class<?> call() throws Exception {
                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                return loader == null ? null : loader.loadClass(className);
            }
        };

        // Should work fine when invoked directly
        Class<?> loadedClass = loadClass.call();
        if (!className.equals(loadedClass.getName()))
            throw new Exception("Failed to load class " + className + " before applying context. Instead " + loadedClass.getName());

        // When default context is applied, classloader should be removed from thread
        Map<String, String> xpropsApplyDefaultContext = Collections.singletonMap(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
        @SuppressWarnings("unchecked")
        Callable<Class<?>> defaultContextLoadClass = mapContextSvc.createContextualProxy(loadClass, xpropsApplyDefaultContext, Callable.class);
        try {
            loadedClass = defaultContextLoadClass.call();
            if (loadedClass != null)
                throw new Exception("Should not be able to load class " + loadedClass + " with default context applied to thread.");
        } catch (ClassNotFoundException x) {
        }

        // Should be able to load the class if overlaying thread context in a way that doesn't touch classloader context
        @SuppressWarnings("unchecked")
        Callable<Class<?>> overlayContextLoadClass = mapContextSvc.createContextualProxy(loadClass, Callable.class);
        loadedClass = overlayContextLoadClass.call();
        if (!className.equals(loadedClass.getName()))
            throw new Exception("Failed to load class when overlaying thread context");
    }

    /**
     * Test default Java EE metadata context
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testDefaultJEEMetadataContext() throws Exception {
        final Callable<?> javaCompLookup = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new InitialContext().lookup("java:comp/env/concurrent/EmptyContextSvc");
            }
        };

        // Should work fine when invoked directly
        Object result = javaCompLookup.call();
        if (result == null)
            throw new Exception("Failed to look up from java:comp before applying context!");

        // When default context is applied, java:comp namespace should not be accessible
        Map<String, String> xpropsApplyDefaultContext = Collections.singletonMap(WSContextService.DEFAULT_CONTEXT, WSContextService.UNCONFIGURED_CONTEXT_TYPES);
        Callable<?> contextualizedJavaCompLookup = mapContextSvc.createContextualProxy(javaCompLookup, xpropsApplyDefaultContext, Callable.class);
        try {
            result = contextualizedJavaCompLookup.call();
            throw new Exception("Should not be able to access java:comp with default context applied to thread. Lookup result is: " + result);
        } catch (NamingException x) {
        }

        // Should be able to access java:comp if overlaying thread context in a way that doesn't touch jeeMetadataContext
        contextualizedJavaCompLookup = mapContextSvc.createContextualProxy(javaCompLookup, Callable.class);
        result = contextualizedJavaCompLookup.call();
        if (result == null)
            throw new Exception("Failed to access java:comp when overlaying thread context");
    }

    /**
     * Try to use a context that has configuration errors.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testErrorsInConfig() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("fourteen", "14");

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    String value1 = mapSvc.get("fourteen");
                    if (!"14".equals(value1))
                        throw new RuntimeException("Wrong value in context snapshot. " + mapSvc);
                    mapSvc.put("fourteen", "XIV");
                }
            };

            // "badContext" has an unrecognized context provider
            ContextService contextSvc1 = (ContextService) new InitialContext().lookup("concurrent/BadContextSvc");

            try {
                runnable = contextSvc1.createContextualProxy(runnable, Runnable.class);
                // TODO: enable the next line if we add a stricter variant of onError
                //throw new Exception("Context with unrecognized context provider should not be usable.");
            } catch (IllegalArgumentException x) {
                if (x.getMessage().contains("CWWKC1001E"))
                    ; // pass, got the correct error for unavailable context providers
                else
                    throw x;
            }

            // "anotherBadContext has a duplicate context provider
            ContextService contextSvc2 = (ContextService) new InitialContext().lookup("concurrent/AnotherBadContextSvc");

            try {
                runnable = contextSvc2.createContextualProxy(runnable, Runnable.class);
                throw new Exception("Context with duplicate context providers should not be usable.");
            } catch (IllegalArgumentException x) {
                if (x.getMessage().contains("CWWKC1002E"))
                    ; // pass, got the correct error for duplicate context providers
                else
                    throw x;
            }

            // "moreBadContext" has an invalid property
            ContextService contextSvc3 = (ContextService) new InitialContext().lookup("concurrent/MoreBadContextSvc");

            try {
                runnable = contextSvc3.createContextualProxy(runnable, Runnable.class);
                // TODO: enable the next line if we add a stricter variant of onError
                //throw new Exception("Context with invalid property name should not be usable.");
            } catch (IllegalArgumentException x) {
                if (x.getMessage().contains("CWWKC1000E"))
                    ; // pass, got the correct error for invalid property name
                else
                    throw x;
            }
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Use the contextService.getContext interface to capture serializable context.
     * Serialize it and deserialize it, and make sure it works.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testGetContext() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
        final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
        serviceRefs.add(bufferSvcRef);

        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("mapContext1", "mapValue1");

            WSContextService contextSvc = (WSContextService) new InitialContext().lookup("concurrent/BufferContextSvc");

            Map<String, String> executionProperties = new HashMap<String, String>();
            executionProperties.put("com.ibm.ws.concurrent.TASK_OWNER", "TestApp_Owner");
            executionProperties.put(ManagedTask.IDENTITY_NAME, "testGetContext");

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor context = contextSvc.captureThreadContext(executionProperties);

            // Change the context after capturing a snapshot
            mapSvc.clear();
            bufferSvc.append('\u001B'); // ESC

            // Serialize the context
            byte[] bytes = context.serialize();

            // deserialize the context
            ThreadContextDescriptor deserializedContext = ThreadContextDeserializer.deserialize(bytes, executionProperties);

            @SuppressWarnings("unchecked")
            Callable<String> contextualTask = contextSvc.createContextualProxy(deserializedContext, new GetBufferTask(), Callable.class);

            String result = contextualTask.call();

            if (result.indexOf("TestApp_Owner") < 0 || result.indexOf("testGetContext") < 0 || result.indexOf("mapContext1=mapValue1") < 0)
                throw new Exception("Context did not serialize/deserialize properly. Result: " + result);
        } finally {
            mapSvc.clear();
            // hacky way of clearing the buffer service
            bufferSvc.append('\u001B'); // ESC

            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Test ContextService.getExecutionProperties
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testGetExecutionProperties() throws Exception {
        Map<String, String> execProps;

        // null contextual proxy
        try {
            execProps = mapContextSvc.getExecutionProperties(null);
            throw new Exception("Must raise IllegalArgumentException when parameter (null) is not a valid contextual proxy. Instead: " + execProps);
        } catch (IllegalArgumentException x) {
        } // pass

        // not a proxy
        try {
            execProps = mapContextSvc.getExecutionProperties(Integer.valueOf(5));
            throw new Exception("Must raise IllegalArgumentException when parameter (5) is not a valid contextual proxy. Instead: " + execProps);
        } catch (IllegalArgumentException x) {
        } // pass

        // proxy that is not contextual
        Object proxy = Proxy.newProxyInstance(AppWorkerTask.class.getClassLoader(), new Class[] { AppWork.class }, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                return null;
            }
        });
        try {
            execProps = mapContextSvc.getExecutionProperties(proxy);
            throw new Exception("Must raise IllegalArgumentException when parameter is a proxy that is not contextual. Instead: " + execProps);
        } catch (IllegalArgumentException x) {
        } // pass

        // Execution properties for contextual task
        Map<String, String> execProps1 = new TreeMap<String, String>();
        execProps1.put("xprop1a", "value1a");
        execProps1.put("xprop1b", "value1b");
        AppWork task1 = mapContextSvc.createContextualProxy(new AppWorkerTask(), execProps1, AppWork.class);
        execProps = mapContextSvc.getExecutionProperties(task1);
        if (!execProps1.equals(execProps))
            throw new Exception("task1 execution properties do not match. Instead: " + execProps);

        // each invocation of getExecutionProperties must return a new copy
        Map<String, String> execPropsCopy = mapContextSvc.getExecutionProperties(task1);
        if (execProps == execPropsCopy)
            throw new Exception("Multiple invocations of getExecutionProperties cannot return the same copy");

        if (!execProps.equals(execPropsCopy))
            throw new Exception("Copies must be equal. Instead: " + execPropsCopy);

        // modify the getExecutionProperties result
        execProps.remove("xprop1a");
        execProps.put("xprop1c", "value1c");
        if (execProps.equals(execPropsCopy))
            throw new Exception("After update, copies should no longer be equal: " + execPropsCopy);

        // get another copy
        execProps = mapContextSvc.getExecutionProperties(task1);
        if (!execProps1.equals(execProps))
            throw new Exception("task1 execution properties should not have been modified: " + execProps);

        // Run a task that accesses the execution properties from different context service
        Object result1 = task1.doWork(task1);
        if (!execProps.equals(result1))
            throw new Exception("task1 execution properties not accessible from different context service. Instead: " + result1);

        // Empty execution properties
        Map<String, String> execProps2 = new TreeMap<String, String>();
        AppTask task2 = (AppTask) mapContextSvc.createContextualProxy(new AppWorkerTask(), execProps2, AppTask.class, AppWork.class);
        execProps = mapContextSvc.getExecutionProperties(task2);
        if (!execProps.isEmpty())
            throw new Exception("task2 execution properties are not empty. Instead: " + execProps);

        // Modify the execution properties
        execProps2.put("xprop2a", "value2a");
        execProps = mapContextSvc.getExecutionProperties(task2);
        if (!execProps.isEmpty())
            throw new Exception("task2 execution properties should still be empty. Instead: " + execProps);
    }

    /**
     * Confirm that we don't hang when baseContextRef causes an infinite loop of dependencies
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testInfiniteBaseContext() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        try {
            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/InfiniteLoopContextSvc1");

            // Take a snapshot of context
            Runnable runnable = contextSvc.createContextualProxy(new Runnable() {
                @Override
                public void run() {
                }
            }, Runnable.class);

            runnable.run();
        } catch (IllegalArgumentException x) {
            if (x.getMessage().indexOf("CWWKC1020E") < 0)
                throw x;
        } finally {
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Contextualize a task to run on an unmanaged thread
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testJEEMetadataContext() throws Exception {
        final Callable<?> javaCompLookup = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                return new InitialContext().lookup("java:comp/env/concurrent/EmptyContextSvc");
            }
        };
        // should work fine from application thread
        javaCompLookup.call();

        // now, contextualize it and run from an unmanaged thread

        ContextService defaultContextSvc = (ContextService) new InitialContext().lookup("java:comp/DefaultContextService");
        final Callable<?> contextualJavaCompLookup = defaultContextSvc.createContextualProxy(javaCompLookup, Callable.class);

        Object result = unmanagedExecutor.submit(contextualJavaCompLookup).get();
        if (result == null)
            throw new Exception("lookup from contextual proxy returned null");

        // Skip jeeMetadataContext
        Map<String, String> xpropsSkipJeeMetadataContext = Collections
                        .singletonMap(WSContextService.SKIP_CONTEXT_PROVIDERS,
                                      "does.not.exist,com.ibm.ws.javaee.metadata.context.provider,com.ibm.security.thread.zos.context.provider");
        Callable<?> contextualizedJavaCompLookup = mapContextSvc.createContextualProxy(javaCompLookup, xpropsSkipJeeMetadataContext, Callable.class);
        // should not interfere with current thread
        result = contextualizedJavaCompLookup.call();
        if (result == null)
            throw new Exception("Failed to access java:comp from servlet thread context when skipping jeeMetadataContext");
        // also should not interfere with unmanaged thread
        Future<?> future = unmanagedExecutor.submit(contextualizedJavaCompLookup);
        try {
            result = future.get();
            throw new Exception("Should not be able to access java:comp from unmanaged thread when skipping jeeMetadataContext");
        } catch (ExecutionException x) {
            if (!(x.getCause() instanceof NamingException))
                throw x;
        }
    }

    /**
     * Test context propagation of different "numeration" contexts in combination
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testMultipleContextServices() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        // Get the numeration service
        ServiceReference<?> numSvcRef = bundleContext.getServiceReference("test.numeration.NumerationService");
        final Object numSvc = bundleContext.getService(numSvcRef);
        final Method NumerationService_setRadix = numSvc.getClass().getMethod("setRadix", int.class);
        final Method NumerationService_toString = numSvc.getClass().getMethod("toString", long.class);
        serviceRefs.add(numSvcRef);
        try {
            Object s = NumerationService_toString.invoke(numSvc, 12345);
            if (!"12345".equals(s))
                throw new Exception("Test isn't working right. Unexpected value: " + s);

            // Get two different context services
            final ContextService contextSvcN1 = (ContextService) new InitialContext().lookup("concurrent/NumContextSvc1");
            final ContextService contextSvcN2 = (ContextService) new InitialContext().lookup("concurrent/NumContextSvc2");

            // data structure to store results
            final ConcurrentLinkedQueue<Object> results = new ConcurrentLinkedQueue<Object>();
            final AtomicReference<Runnable> runAsSnapshot = new AtomicReference<Runnable>();

            final Runnable runToString2310 = new Runnable() {
                @Override
                public void run() {
                    try {
                        results.add(NumerationService_toString.invoke(numSvc, 2310));
                    } catch (Throwable x) {
                        throw new RuntimeException(x);
                    }
                }
            };

            final Runnable runToString2310AndTakeSnapshot = new Runnable() {
                @Override
                public void run() {
                    runToString2310.run();
                    try {
                        runAsSnapshot.set(contextSvcN1.createContextualProxy(runToString2310, Runnable.class));
                        NumerationService_setRadix.invoke(numSvc, 5);
                    } catch (Throwable x) {
                        throw new RuntimeException(x);
                    }
                    runAsSnapshot.get().run();
                }
            };

            // Run as default numeration context
            contextSvcN2.createContextualProxy(runToString2310AndTakeSnapshot, Runnable.class).run();
            NumerationService_setRadix.invoke(numSvc, 4);
            runAsSnapshot.get().run();
            runToString2310.run();

            // test results
            Object result = results.remove();
            if (!"100100000110".equals(result))
                throw new Exception("Default context not correctly propagated. Result: " + result);
            result = results.remove();
            if (!"100100000110".equals(result))
                throw new Exception("Snapshot of default context not working from same method. Result: " + result);
            result = results.remove();
            if (!"100100000110".equals(result))
                throw new Exception("Snapshot of default context not working form outside of method where it was taken. Result: " + result);
            result = results.remove();
            if (!"210012".equals(result))
                throw new Exception("Context not properly restored afterwards. Result: " + result);
            if (!results.isEmpty())
                throw new Exception("Extra results: " + results);

            // Run as a snapshot of current context
            contextSvcN1.createContextualProxy(runToString2310AndTakeSnapshot, Runnable.class).run();
            NumerationService_setRadix.invoke(numSvc, 6);
            runAsSnapshot.get().run();
            runToString2310.run();

            // test results
            result = results.remove();
            if (!"210012".equals(result))
                throw new Exception("Snapshot of current context not propagated correctly. Result: " + result);
            result = results.remove();
            if (!"210012".equals(result))
                throw new Exception("Snapshot of snapshot not working for same method. Result: " + result);
            result = results.remove();
            if (!"210012".equals(result))
                throw new Exception("Snapshot of snapshot not working from outside of method where it was taken. Result: " + result);
            result = results.remove();
            if (!"14410".equals(result))
                throw new Exception("Context not properly restored after. Result: " + result);
            if (!results.isEmpty())
                throw new Exception("Extra results: " + results);
        } finally {
            NumerationService_setRadix.invoke(numSvc, 10); // restore to default
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Tests of contextual proxy for multiple interfaces.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testMultipleInterfaces() throws Exception {
        // no interfaces
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask());
            throw new Exception("Should not be able to create contextual proxy for no interfaces: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), Collections.singletonMap("noInterfaces", "true"));
            throw new Exception("Should not be able to create contextual proxy with execution properties but for no interfaces: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // non-matching interface
        try {
            Map<String, String> execProps = Collections.singletonMap("wrongInterface", "true");
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), execProps, Runnable.class);
            throw new Exception("Should not be able to create contextual proxy for non-matching interface (Runnable): " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // non-matching interface
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), Iterable.class);
            throw new Exception("Should not be able to create contextual proxy for non-matching interface (Iterable): " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // instance does not implement one of the interfaces
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), AppTask.class, Runnable.class, AppWork.class);
            throw new Exception("Should not be able to create contextual proxy when one of the interfaces isn't implemented: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // interfaces from different classloaders, where JDK interface is listed first
        AppTask task1 = (AppTask) mapContextSvc.createContextualProxy(new AppWorkerTask(), Serializable.class, AppTask.class, AppWork.class);
        if (!(task1 instanceof AppWork))
            throw new Exception("Should also be instance of AppWork");
        if (!(task1 instanceof Serializable))
            throw new Exception("Should also be instance of Serializable");
        task1.doTask("not found");

        // null instance
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(null, Runnable.class, AppWork.class);
            throw new Exception("Should not be able to create contextual proxy when instance is null: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // null instance, no interfaces
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(null);
            throw new Exception("Should not be able to create contextual proxy when instance and interfaces are null: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // null instance, with execution properties
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(null, Collections.singletonMap("null instance", "true"), Runnable.class, AppWork.class);
            throw new Exception("Should not be able to create contextual proxy with execution properties when instance is null: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // null instance, no interfaces, with execution properties
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(null, Collections.singletonMap("null instance & no interfaces", "true"));
            throw new Exception("Should not be able to create contextual proxy with execution properties when instance and interfaces are null: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // one of interfaces is null
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), AppTask.class, null);
            throw new Exception("Should not be able to create contextual proxy when one of the interfaces is null: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // only interface in list is null
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), new Class<?>[] { null });
            throw new Exception("Should not be able to create contextual proxy the only interface is null: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // null interface list
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), (Class<?>[]) null);
            throw new Exception("Should not be able to create contextual proxy when the list of interfaces is null: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // one of the interfaces is not an interface
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), new Class<?>[] { AppWorkerTask.class });
            throw new Exception("Should not be able to create contextual proxy when the interfaces list contains a non-interface: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // multiple interfaces, with execution properties
        Map<String, String> execProps2 = Collections.singletonMap("multiple interfaces", "true");
        AppWork task2 = (AppWork) mapContextSvc.createContextualProxy(new AppWorkerTask(), execProps2, AppTask.class, AppWork.class);
        if (!(task2 instanceof AppTask))
            throw new Exception("Should also be instance of AppTask");
        Object result = task2.doWork(task2);
        if (!execProps2.equals(result))
            throw new Exception("Unexpected result for task2: " + result);

        // duplicate interfaces
        try {
            Object contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), AppTask.class, AppWork.class, AppTask.class);
            throw new Exception("Should not be able to create contextual proxy when there are duplicate interfaces: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // multiple interfaces, both from JDK
        Map<String, String> execProps3 = Collections.singletonMap("multiple JDK interfaces", "Runnable/Callable");
        Runnable task3 = (Runnable) mapContextSvc.createContextualProxy(new GetBufferTask(), execProps3, Runnable.class, Callable.class);
        task3.run();
        @SuppressWarnings("unchecked")
        Callable<String> task3c = (Callable<String>) task3;
        String result3 = task3c.call();
        if (result3 == null)
            throw new Exception("result of task3c.call is null");
    }

    /**
     * Test that when context isn't specified, it should neither be propagated nor removed from the thread.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testNoContext() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("this", "that");

            ContextService contextSvc = (ContextService) new InitialContext().lookup("java:comp/env/concurrent/EmptyContextSvc");

            // Take a snapshot of no context
            @SuppressWarnings("unchecked")
            Callable<String> callable = contextSvc.createContextualProxy(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    String value = mapSvc.get("this");
                    mapSvc.put("this", "these");
                    return value;
                }
            }, Callable.class);

            // Run on a different thread
            mapSvc.clear(); // just in case the executor service decides to run on the current thread
            ServiceReference<ExecutorService> execSvcRef = bundleContext.getServiceReference(ExecutorService.class);
            ExecutorService execSvc = bundleContext.getService(execSvcRef);
            serviceRefs.add(execSvcRef);
            Future<String> result = execSvc.submit(callable);
            String value = result.get(10, TimeUnit.SECONDS);

            if (value != null)
                throw new Exception("Context should not have been propagated to thread when context service isn't configured to do so. Value: " + value);

            // Change the context and run on current thread.
            mapSvc.put("this", "those");
            value = callable.call();

            if (!"those".equals(value))
                throw new Exception("Context should not have been altered upon invoking contextual operation when context service isn't configured to propagate it. Value: "
                                    + value);

            value = mapSvc.get("this");
            if (!"these".equals(value))
                throw new Exception("Context should not have been altered after invoking contextual operation when context service isn't configured to propagate it. Value: "
                                    + value);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Test propagation of the "buffer" context, which has dependencies on the
     * "map" context and "numeration" context being propagated to the thread first
     * and removed after.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testOrderOfContextPropagation() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
        final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
        serviceRefs.add(bufferSvcRef);

        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            // Update the character buffer context
            bufferSvc.append("item1 ");

            // Update the map context
            mapSvc.put("entry1", "value1");

            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/BufferContextSvc");

            // We are abusing the Iterator interface to return the contents of the character buffer
            // from the current context.
            Iterator<String> bufferReader = new Iterator<String>() {
                int counter = 0;

                @Override
                public boolean hasNext() {
                    counter++;
                    try {
                        bufferSvc.append("counter=" + counter + ' ');
                    } catch (IOException x) {
                        throw new RuntimeException(x);
                    }
                    return true;
                }

                @Override
                public String next() {
                    return bufferSvc.toString();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };

            // Take a snapshot of context
            @SuppressWarnings("unchecked")
            final Iterator<String> contextualBufferReader = contextSvc.createContextualProxy(bufferReader, Iterator.class);

            // Update the context
            bufferSvc.append("item2 ");
            mapSvc.put("entry2", "value2");

            // Run as the old context snapshot, on a different thread
            ServiceReference<ExecutorService> execSvcRef = bundleContext.getServiceReference(ExecutorService.class);
            ExecutorService execSvc = bundleContext.getService(execSvcRef);
            serviceRefs.add(execSvcRef);
            Future<String> result = execSvc.submit(new Callable<String>() {
                @Override
                public String call() {
                    return contextualBufferReader.next();
                }
            });
            String buffer = result.get(10, TimeUnit.SECONDS);

            if (buffer.indexOf("item1") < 0)
                throw new Exception("When running on executor thread, does not have value from original context. Buffer: " + buffer);

            if (buffer.indexOf("item2") >= 0)
                throw new Exception("When running on executor thread, Context snapshot should not contain changes from after the snapshot was taken. Buffer: " + buffer);

            if (buffer.indexOf("entry1=value1") < 0)
                throw new Exception("Map context appended to buffer should be the original snapshot of map context. Buffer: " + buffer);

            if (buffer.indexOf("entry2=value2") > 0)
                throw new Exception("Map context appended to buffer should not include updates made after the snapshot was taken. Buffer: " + buffer);

            // TODO: update this to check for the xpath style name once 94143 is implemented
            if (buffer.indexOf("BufferContextSvc is running") < 0)
                throw new Exception("Task owner is missing from buffer: " + buffer);

            if (buffer.indexOf("is running test.context.app.ContextServiceTestServlet$") < 0)
                throw new Exception("Task name is missing from buffer: " + buffer);

            // Run on current thread
            if (!contextualBufferReader.hasNext())
                throw new Exception("Wrapped contextual operation (hasNext) should always return true, not false.");

            buffer = contextualBufferReader.next();

            if (buffer.indexOf("item1") < 0)
                throw new Exception("When running on same thread, does not have value from original context. Buffer: " + buffer);

            if (buffer.indexOf("item2") >= 0)
                throw new Exception("When running on same thread, Context snapshot should not contain changes made after the snapshot was taken. Buffer: " + buffer);

            if (buffer.indexOf("counter") > 0)
                throw new Exception("Context not cleaned up after contextual operation (hasNext) finishes. Buffer: " + buffer);
        } finally {
            mapSvc.clear();
            // hacky way of clearing the buffer service
            bufferSvc.append('\u001B'); // ESC

            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Programmatically specify additional thread context to capture.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testProgrammaticallyAddContextConfiguration() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();

        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);

        ServiceReference<?> numSvcRef = bundleContext.getServiceReference("test.numeration.NumerationService");
        final Object numSvc = bundleContext.getService(numSvcRef);
        final Method NumerationService_setRadix = numSvc.getClass().getMethod("setRadix", int.class);
        final Method NumerationService_toString = numSvc.getClass().getMethod("toString", long.class);
        serviceRefs.add(numSvcRef);

        ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
        final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
        serviceRefs.add(bufferSvcRef);

        try {
            mapSvc.put("key-TPACC-1", "value-TPACC-1");
            bufferSvc.append("key-TPACC-text");

            WSContextService contextSvc = (WSContextService) new InitialContext().lookup("concurrent/NumContextSvc2");

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor capturedContextWithoutMap = contextSvc.captureThreadContext(null);

            Map<String, ?> mapContextConfig = Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "test.map.context.provider");
            @SuppressWarnings("unchecked")
            ThreadContextDescriptor capturedContextWithMap = contextSvc.captureThreadContext(null, mapContextConfig);

            Map<String, Object> num4ContextConfig = new HashMap<String, Object>();
            num4ContextConfig.put(WSContextService.THREAD_CONTEXT_PROVIDER, "test.numeration.context.provider");
            num4ContextConfig.put("radix", 4);

            Map<String, ?> badContextConfig = Collections.singletonMap(WSContextService.THREAD_CONTEXT_PROVIDER, "test.bad.context.provider");

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor capturedContextWithNum4 = contextSvc.captureThreadContext(null, badContextConfig, num4ContextConfig);

            @SuppressWarnings("unchecked")
            ThreadContextDescriptor capturedContextWithMapAndNum4 = contextSvc.captureThreadContext(null, mapContextConfig, num4ContextConfig);

            mapSvc.remove("key-TPACC-1");
            NumerationService_setRadix.invoke(numSvc, 18);
            // hacky way of clearing the buffer service
            bufferSvc.append('\u001B'); // ESC

            Callable<Object[]> task = new Callable<Object[]>() {
                @Override
                public Object[] call() throws Exception {
                    Object[] results = new Object[3];
                    results[0] = mapSvc.get("key-TPACC-1");
                    results[1] = NumerationService_toString.invoke(numSvc, Long.valueOf(41));
                    results[2] = bufferSvc.toString();
                    return results;
                }
            };

            @SuppressWarnings("unchecked")
            Callable<Object[]> contextualTaskWithoutMap = contextSvc.createContextualProxy(capturedContextWithoutMap, task, Callable.class);
            Object[] results = contextualTaskWithoutMap.call();

            if (results[0] != null)
                throw new Exception("contextualTaskWithoutMap: Map should be empty, not have " + results[0]);

            // 41 = 1*32 + 0*16 + 1*8 + 0*4 + 0*2 + 1
            if (!"101001".equals(results[1]))
                throw new Exception("contextualTaskWithoutMap: Result should be 101001, not " + results[1]);

            String str = (String) results[2];
            if (str != null && str.indexOf("TPACC") > 0)
                throw new Exception("contextualTaskWithoutMap: Unexpected text in buffer: " + str);

            @SuppressWarnings("unchecked")
            Callable<Object[]> contextualTaskWithMap = contextSvc.createContextualProxy(capturedContextWithMap, task, Callable.class);
            results = contextualTaskWithMap.call();

            if (!"value-TPACC-1".equals(results[0]))
                throw new Exception("contextualTaskWithMap: Missing or unexpected entry: " + results[0]);

            // 41 = 1*32 + 0*16 + 1*8 + 0*4 + 0*2 + 1
            if (!"101001".equals(results[1]))
                throw new Exception("contextualTaskWithMap: Result should be 101001, not " + results[1]);

            str = (String) results[2];
            if (str != null && str.indexOf("TPACC") > 0)
                throw new Exception("contextualTaskWithMap: Unexpected text in buffer: " + str);

            @SuppressWarnings("unchecked")
            Callable<Object[]> contextualTaskWithNum4 = contextSvc.createContextualProxy(capturedContextWithNum4, task, Callable.class);
            results = contextualTaskWithNum4.call();

            if (results[0] != null)
                throw new Exception("contextualTaskWithNum4: Map should be empty, not have " + results[0]);

            // 41 = 2*16 + 2*4 + 1
            if (!"221".equals(results[1]))
                throw new Exception("contextualTaskWithNum4: Result should be 221, not " + results[1]);

            str = (String) results[2];
            if (str != null && str.indexOf("TPACC") > 0)
                throw new Exception("contextualTaskWithNum4: Unexpected text in buffer: " + str);

            @SuppressWarnings("unchecked")
            Callable<Object[]> contextualTaskWithMapAndNum4 = contextSvc.createContextualProxy(capturedContextWithMapAndNum4, task, Callable.class);
            results = contextualTaskWithMapAndNum4.call();

            if (!"value-TPACC-1".equals(results[0]))
                throw new Exception("contextualTaskWithMapAndNum4: Missing or unexpected entry: " + results[0]);

            // 41 = 2*16 + 2*4 + 1
            if (!"221".equals(results[1]))
                throw new Exception("contextualTaskWithMapAndNum4: Result should be 221, not " + results[1]);

            str = (String) results[2];
            if (str != null && str.indexOf("TPACC") > 0)
                throw new Exception("contextualTaskWithMapAndNum4: Unexpected text in buffer: " + str);

        } finally {
            // hacky way of clearing the buffer service
            bufferSvc.append('\u001B'); // ESC

            NumerationService_setRadix.invoke(numSvc, 10); // restore to default

            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Attempt to propagate invalidly configured context that results in a RejectedExecutionException
     */
    @ExpectedFFDC(value = "java.util.concurrent.RejectedExecutionException")
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testRejectedExecutionException() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        try {
            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/NumContextSvc0");
            WSContextService wsContextSvc = (WSContextService) contextSvc;

            // Take a snapshot of context
            @SuppressWarnings("unchecked")
            Callable<String> callable = contextSvc.createContextualProxy(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    throw new Exception("Should not be able to run this Callable with invalid context");
                }
            }, Callable.class);

            // Attempt to run the contextual callable
            try {
                Object result = callable.call();
                throw new Exception("Contextual callable with invalid context returned a result: " + result);
            } catch (RejectedExecutionException x) {
                if (!"Unsupported radix: 0".equals(x.getMessage()))
                    throw x;
            }

            // Take another snapshot of context
            @SuppressWarnings("unchecked")
            ThreadContextDescriptor context = wsContextSvc.captureThreadContext(null);

            // Wrap a runnable with context
            Runnable runnable = wsContextSvc.createContextualProxy(context, new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException("Should not be able to run with invalid context");
                }
            }, Runnable.class);

            // Attempt to run the contextual runnable
            try {
                runnable.run();
            } catch (RejectedExecutionException x) {
                if (!"Unsupported radix: 0".equals(x.getMessage()))
                    throw x;
            }

            // Create an iterator to wrap with context
            Iterator<?> iterator = new Iterator<Integer>() {
                @Override
                public boolean hasNext() {
                    throw new RuntimeException("Should not be able to invoke 'hasNext' method with invalid context");
                }

                @Override
                public Integer next() {
                    throw new RuntimeException("Should not be able to invoke 'next' method with invalid context");
                }

                @Override
                public void remove() {
                    throw new RuntimeException("Should not be able to invoke 'remove' method with invalid context");
                }

            };
            iterator = wsContextSvc.createContextualProxy(context, iterator, Iterator.class);

            // Attempt to invoke a method on the contextual iterator
            try {
                Object result = iterator.next();
                throw new Exception("Contextual iterator with invalid context returned a result: " + result);
            } catch (RejectedExecutionException x) {
                if (!"Unsupported radix: 0".equals(x.getMessage()))
                    throw x;
            }
        } finally {
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Submit contextual tasks to a thread pool
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testRunOnThreadPool() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            // Create a bunch of Callables that we can schedule to an executor service to run with context
            Callable<Integer> getNumber = new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    String str = mapSvc.get("number");
                    Tr.debug(this, tc, "getNumber: " + str);
                    return Integer.parseInt(str);
                }
            };
            Collection<Callable<Integer>> tasks = new LinkedList<Callable<Integer>>();
            for (int i = 0; i < 50; i++) {
                mapSvc.put("number", Integer.toString(i));
                Map<String, String> execProps = Collections.singletonMap("task#", Integer.toString(i));

                switch (i % 4) {
                    case 0:
                        @SuppressWarnings("unchecked")
                        Callable<Integer> contextualTask0 = (Callable<Integer>) mapContextSvc.createContextualProxy(getNumber, new Class[] { Callable.class });
                        tasks.add(contextualTask0);
                        break;
                    case 1:
                        @SuppressWarnings("unchecked")
                        Callable<Integer> contextualTask1 = (Callable<Integer>) mapContextSvc.createContextualProxy(getNumber, execProps, new Class[] { Callable.class });
                        tasks.add(contextualTask1);
                        break;
                    case 2:
                        @SuppressWarnings("unchecked")
                        Callable<Integer> contextualTask2 = mapContextSvc.createContextualProxy(getNumber, Callable.class);
                        tasks.add(contextualTask2);
                        break;
                    case 3:
                        @SuppressWarnings("unchecked")
                        Callable<Integer> contextualTask3 = mapContextSvc.createContextualProxy(getNumber, execProps, Callable.class);
                        tasks.add(contextualTask3);
                        break;
                }
            }
            ServiceReference<ExecutorService> execSvcRef = bundleContext.getServiceReference(ExecutorService.class);
            ExecutorService execSvc = bundleContext.getService(execSvcRef);
            serviceRefs.add(execSvcRef);
            List<Future<Integer>> results = execSvc.invokeAll(tasks, 1, TimeUnit.MINUTES);

            // Validate results
            Set<Integer> missing = new HashSet<Integer>();
            for (int i = 0; i < 50; i++)
                missing.add(i);
            for (Future<Integer> result : results)
                missing.remove(result.get());

            int numMissing = missing.size();
            if (numMissing > 0)
                throw new Exception("The following tasks did not run with context " + missing);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Serialize a contextual task. Then deserialize it and use it.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testSerialization() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
        final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
        serviceRefs.add(bufferSvcRef);

        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("key1", "value1");

            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/BufferContextSvc");

            Map<String, String> executionProperties = new TreeMap<String, String>();
            executionProperties.put("com.ibm.ws.concurrent.TASK_OWNER", "TestOwner");
            executionProperties.put(ManagedTask.IDENTITY_NAME, "testSerialization");

            @SuppressWarnings("unchecked")
            Callable<String> contextualTask = contextSvc.createContextualProxy(new GetBufferTask(), executionProperties, Callable.class);

            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(byteOutput);
            oos.writeObject(contextualTask);
            oos.flush();
            byte[] bytes = byteOutput.toByteArray();
            oos.close();

            mapSvc.clear();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            @SuppressWarnings("unchecked")
            Callable<String> deserializedTask = (Callable<String>) in.readObject();
            in.close();

            String result = deserializedTask.call();

            if (result.indexOf("TestOwner") < 0 || result.indexOf("testSerialization") < 0 || result.indexOf("key1=value1") < 0)
                throw new Exception("Contextual task did not serialize/deserialize properly. Result: " + result);
        } finally {
            mapSvc.clear();
            // hacky way of clearing the buffer service
            bufferSvc.append('\u001B'); // ESC

            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }

    /**
     * Verify that ThreadContextManager instance gets returned before any other WSContextService.
     * Verify that DefaultContextService gets returned before any configured ContextService.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testServiceRanking() throws Exception {
        ServiceReference<WSContextService> ref1 = bundleContext.getServiceReference(WSContextService.class);
        WSContextService svc1 = bundleContext.getService(ref1);
        try {
            if (!"ThreadContextManager".equals(svc1.getClass().getSimpleName()))
                throw new Exception("Unexpected WSContextService given highest ranking: " + svc1);
        } finally {
            bundleContext.ungetService(ref1);
        }

        ServiceReference<ContextService> ref2 = bundleContext.getServiceReference(ContextService.class);
        ContextService svc2 = bundleContext.getService(ref2);
        try {
            Field ContextServiceImpl_name = svc2.getClass().getDeclaredField("name");
            ContextServiceImpl_name.setAccessible(true);
            String name = (String) ContextServiceImpl_name.get(svc2);
            if (!"contextService[DefaultContextService]".equals(name))
                throw new Exception("Unexpected ContextService given highest ranking: " + svc2 + ". Name is " + name);
        } finally {
            bundleContext.ungetService(ref2);
        }
    }

    /**
     * Error path tests for contextual proxy for a single interface.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testSingleInterface() throws Exception {
        // null instance
        try {
            Runnable contextualProxy = mapContextSvc.createContextualProxy(null, Runnable.class);
            throw new Exception("Should not be able to create contextual proxy for null instance: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // null interface
        try {
            AppTask contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), (Class<AppWorkerTask>) null);
            throw new Exception("Should not be able to create contextual proxy for null interface: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // non-interface
        try {
            AppTask contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), AppWorkerTask.class);
            throw new Exception("Should not be able to create contextual proxy for non-interface: " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // non-matching interface
        try {
            Class<?> Runnable_class = Runnable.class;
            @SuppressWarnings("unchecked")
            Class<AppTask> AppTask_class = (Class<AppTask>) Runnable_class;
            AppTask contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), AppTask_class);
            throw new Exception("Should not be able to create contextual proxy for non-matching interface (Runnable): " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass

        // another non-matching interface
        try {
            Class<?> Iterable_class = Iterable.class;
            @SuppressWarnings("unchecked")
            Class<AppTask> AppTask_class = (Class<AppTask>) Iterable_class;
            AppTask contextualProxy = mapContextSvc.createContextualProxy(new AppWorkerTask(), AppTask_class);
            throw new Exception("Should not be able to create contextual proxy for non-matching interface (Iterable): " + contextualProxy);
        } catch (IllegalArgumentException x) {
        } // pass
    }

    /**
     * Use a fake ThreadFactory that relies on the context service,
     * and allows for us to test serialization/deserialization of context.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testThreadFactory() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        @SuppressWarnings("rawtypes")
        ServiceReference<Map> mapSvcRef = bundleContext.getServiceReference(Map.class);
        @SuppressWarnings("unchecked")
        final Map<String, String> mapSvc = bundleContext.getService(mapSvcRef);
        serviceRefs.add(mapSvcRef);
        try {
            mapSvc.put("tf1", "value1");

            ServiceReference<ThreadFactory> threadFactorySvcRef = bundleContext.getServiceReferences(ThreadFactory.class, "(id=myThreadFactory)").iterator().next();
            ThreadFactory threadFactory = bundleContext.getService(threadFactorySvcRef);
            serviceRefs.add(threadFactorySvcRef);

            GetBufferTask.results.clear();
            Thread contextualThread = threadFactory.newThread(new GetBufferTask());

            // change the context
            mapSvc.clear();
            mapSvc.put("tf2", "value2");

            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(byteOutput);
            oos.writeObject(contextualThread);
            oos.flush();
            byte[] bytes = byteOutput.toByteArray();
            oos.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Thread deserializedContextualThread = (Thread) in.readObject();
            in.close();

            // Run as the old context snapshot
            deserializedContextualThread.start();

            // Wait up to 10 seconds for it to complete
            for (int i = 0; i < 100 && deserializedContextualThread.isAlive(); i++) {
                Thread.yield();
                Thread.sleep(100);
            }

            // Validate results
            if (GetBufferTask.results.size() != 1)
                throw new Exception("Contextual task should run exactly once. Instead: " + GetBufferTask.results);
            Object result = GetBufferTask.results.iterator().next();
            if (result instanceof Exception)
                throw (Exception) result;
            if (result instanceof Error)
                throw (Error) result;
            String str = (String) result;
            if (str.indexOf("myThreadFactory") < 0 || str.indexOf("test.context.app.GetBufferTask") < 0 || str.indexOf("tf1=value1") < 0 || str.indexOf("tf2") >= 0)
                throw new Exception("Contextual task did not serialize/deserialize properly. Result: " + result);
        } finally {
            mapSvc.clear();
            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
            GetBufferTask.results.clear();
        }
    }

    /**
     * Submit a task that implements WSIdentifiable and verify that the owner and taskName are honored.
     */
    @Test
    @AllowedFFDC("java.lang.ClassNotFoundException") // TODO remove once resource injection is consistently working for Jakarta
    public void testWSIdentifiable() throws Exception {
        List<ServiceReference<?>> serviceRefs = new LinkedList<ServiceReference<?>>();
        ServiceReference<Appendable> bufferSvcRef = bundleContext.getServiceReference(Appendable.class);
        final Appendable bufferSvc = bundleContext.getService(bufferSvcRef);
        serviceRefs.add(bufferSvcRef);
        try {
            ContextService contextSvc = (ContextService) new InitialContext().lookup("concurrent/BufferContextSvc");

            Map<String, String> executionProperties1 = new HashMap<String, String>();
            executionProperties1.put("com.ibm.ws.concurrent.TASK_OWNER", "wm/MyWorkManager");
            executionProperties1.put(ManagedTask.IDENTITY_NAME, "doSomethingUseful");

            Map<String, String> executionProperties2 = new HashMap<String, String>();
            executionProperties2.put("com.ibm.ws.concurrent.TASK_OWNER", "wm/AnotherWorkManager");
            executionProperties2.put(ManagedTask.IDENTITY_NAME, "doSomethingElse");

            String result = (String) contextSvc.createContextualProxy(new GetBufferTask(), executionProperties1, Callable.class).call();

            if (!result.contains("wm/MyWorkManager"))
                throw new Exception("WSIdentifiable owner does not appear to be honored. Result: " + result);

            if (!result.contains("doSomethingUseful"))
                throw new Exception("WSIdentifiable task name does not appear to be honored. Result: " + result);

            result = (String) contextSvc.createContextualProxy(new GetBufferTask(), executionProperties2, Callable.class).call();

            if (!result.contains("wm/AnotherWorkManager"))
                throw new Exception("WSIdentifiable owner does not appear to be honored. Result: " + result);

            if (!result.contains("doSomethingElse"))
                throw new Exception("WSIdentifiable task name does not appear to be honored. Result: " + result);

            if (result.contains("wm/MyWorkManager") || result.contains("doSomethingUseful"))
                throw new Exception("Snapshot of original context was not propagated. Result: " + result);
        } finally {
            // hacky way of clearing the buffer service
            bufferSvc.append('\u001B'); // ESC

            for (ServiceReference<?> ref : serviceRefs)
                bundleContext.ungetService(ref);
        }
    }
}