/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.context.serialization.app;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.security.Principal;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ContextService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;

@SuppressWarnings("serial")
public class ContextServiceSerializationTestServlet extends HttpServlet {

    @Resource
    private UserTransaction tran;

    @EJB
    private ContextServiceSerializationTestBean testBean;

    @Resource(lookup = "concurrent/classloaderContextSvc")
    private ContextService classloaderContextSvc;

    @Resource(lookup = "java:comp/DefaultContextService")
    private ContextService defaultContextSvc;

    @Resource(lookup = "concurrent/jeeMetadataContextSvc")
    private ContextService jeeMetadataContextSvc;

    @Resource(lookup = "concurrent/securityContextSvc")
    private ContextService securityContextSvc;

    @Resource(lookup = "concurrent/transactionContextSvc")
    private ContextService transactionContextSvc;

    private static final ExecutorService unmanagedExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void destroy() {
        unmanagedExecutor.shutdownNow();
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String test = request.getParameter("test");
        PrintWriter out = response.getWriter();
        out.println("Starting " + test + "<br>");
        try {
            System.out.println("-----> " + test + " starting");
            getClass().getMethod(test, HttpServletRequest.class, HttpServletResponse.class).invoke(this, request, response);
            out.println(test + " COMPLETED SUCCESSFULLY");
            System.out.println("<----- " + test + " successful");
        } catch (Throwable x) {
            if (x instanceof InvocationTargetException)
                x = x.getCause();
            System.out.println("<----- " + test + " failed:");
            x.printStackTrace(System.out);
            out.println("<pre>ERROR in " + test + ":");
            x.printStackTrace(out);
            out.println("</pre>");
        }
    }

    /**
     * Deserialize default classloader context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeDefaultClassloaderContextV8_5_5_4(HttpServletRequest request, HttpServletResponse response) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();

        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/defaultContext_ALL_CONTEXT_TYPES-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        final AtomicReference<ClassLoader> result = new AtomicReference<ClassLoader>();
        final AtomicReference<Executor> contextualExecutor2Ref = new AtomicReference<Executor>();
        contextualExecutor.execute(new Runnable() {
            @Override
            public void run() {
                result.set(Thread.currentThread().getContextClassLoader());

                // capture context from here
                contextualExecutor2Ref.set(classloaderContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class));
            }
        });

        if (result.get() != ClassLoader.getSystemClassLoader())
            throw new Exception("Expecting system thread context classloader, not " + result.get());

        // Can we propagate the system class loader to the current thread?
        contextualExecutor2Ref.get().execute(new Runnable() {
            @Override
            public void run() {
                result.set(Thread.currentThread().getContextClassLoader());
            }
        });

        ClassLoader cl = result.get();
        try {
            Class<?> c = cl.loadClass(ContextServiceSerializationTestServlet.class.getName());
            throw new Exception("System class loader not propagated to thread. Should not be able to load " + c + ". Class loader is " + cl);
        } catch (ClassNotFoundException x) {
        }

        if (cl != ClassLoader.getSystemClassLoader())
            throw new Exception("System class loader not propagated to thread. Instead: " + cl);

        cl = Thread.currentThread().getContextClassLoader();
        if (cl != original)
            throw new Exception("Class loader of current thread not restored after contextual operation. Expecting " + original + " not " + cl);

        // Can we serialize/deserialize captured default class loader?
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(bout);
        oout.writeObject(contextualExecutor2Ref.get());
        oout.flush();
        byte[] bytes = bout.toByteArray();
        oout.close();

        ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Executor contextualExecutor2Deser = (Executor) oin.readObject();
        oin.close();

        contextualExecutor2Deser.execute(new Runnable() {
            @Override
            public void run() {
                result.set(Thread.currentThread().getContextClassLoader());
            }
        });

        cl = result.get();
        try {
            Class<?> c = cl.loadClass(ContextServiceSerializationTestServlet.class.getName());
            throw new Exception("System class loader did not survive deserialization. Should not be able to load " + c + ". Class loader is " + cl);
        } catch (ClassNotFoundException x) {
        }

        if (cl != ClassLoader.getSystemClassLoader())
            throw new Exception("System class loader did not survive deserialization. Instead: " + cl);
    }

    /**
     * Deserialize default Java EE metadata context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeDefaultJEEMetadataContextV8_5_5_4(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/defaultContext_ALL_CONTEXT_TYPES-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        contextualExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Integer result = (Integer) new InitialContext().lookup("java:comp/env/entry1");
                    throw new RuntimeException("Should not be able to access java:comp/env/entry1 from default (empty) jeeMetadataContext: " + result);
                } catch (NamingException x) {
                    // expected
                }
            }
        });
    }

    /**
     * Deserialize default security context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeDefaultSecurityContextV8_5_5_4(final HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Verify that we are deserializing with user2 already on the thread
        Principal principal = request.getUserPrincipal();
        String principalName = principal.getName();
        if (!"user2".equals(principalName))
            throw new Exception("Unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

        Subject callerSubject = WSSubject.getCallerSubject();
        principalName = callerSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("Unexpected principal name from caller subject: " + callerSubject);

        WSCredential credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
        String realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new Exception("Unexpected realm security name " + realmSecurityName + " in caller subject credential " + credential);

        Subject runAsSubject = WSSubject.getRunAsSubject();
        principalName = runAsSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("Unexpected principal name from runAs subject: " + runAsSubject);

        credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
        realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new Exception("Unexpected realm security name " + realmSecurityName + " in runAs subject credential " + credential);

        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/defaultContext_ALL_CONTEXT_TYPES-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        contextualExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Principal principal = request.getUserPrincipal();
                    if (principal != null) {
                        String principalName = principal.getName();
                        if (principalName != null)
                            throw new RuntimeException("Unexpected HttpServletRequest.getUserPrincipal: " + principal);
                    }

                    Subject callerSubject = WSSubject.getCallerSubject();
                    if (callerSubject != null) {
                        Set<Principal> principals = callerSubject.getPrincipals();
                        Set<Object> credentials = callerSubject.getPublicCredentials();
                        if (principals != null && !principals.isEmpty()
                            || credentials != null && !credentials.isEmpty())
                            throw new RuntimeException("Unexpected caller subject: " + callerSubject);
                    }

                    Subject runAsSubject = WSSubject.getRunAsSubject();
                    if (runAsSubject != null) {
                        Set<Principal> principals = runAsSubject.getPrincipals();
                        Set<Object> credentials = runAsSubject.getPublicCredentials();
                        if (principals != null && !principals.isEmpty()
                            || credentials != null && !credentials.isEmpty())
                            throw new RuntimeException("Unexpected runAs subject: " + runAsSubject);
                    }
                } catch (WSSecurityException x) {
                    throw new RuntimeException(x);
                }
            }
        });

        // Verify that previous context is restored
        principal = request.getUserPrincipal();
        principalName = principal.getName();
        if (!"user2".equals(principalName))
            throw new Exception("After restoring thread context, unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

        callerSubject = WSSubject.getCallerSubject();
        principalName = callerSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("After restoring thread context, unexpected principal name from caller subject: " + callerSubject);

        credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
        realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new Exception("After restoring thread context, unexpected realm security name " + realmSecurityName + " in caller subject credential " + credential);

        runAsSubject = WSSubject.getRunAsSubject();
        principalName = runAsSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("After restoring thread context, unexpected principal name from runAs subject: " + runAsSubject);

        credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
        realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new Exception("After restoring thread context, unexpected realm security name " + realmSecurityName + " in runAs subject credential " + credential);
    }

    /**
     * Deserialize default transaction context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeDefaultTransactionContextV8_5_5_4(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/defaultContext_ALL_CONTEXT_TYPES-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }
        tran.begin();
        try {
            contextualExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Nested transactions aren't allowed, so this will only work if transactionContext has
                        // successfully suspended the transaction on the thread of execution that invokes this task.
                        tran.begin();
                        tran.commit();
                    } catch (RuntimeException x) {
                        throw x;
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                }
            });
        } finally {
            tran.commit();
        }
    }

    /**
     * Deserialize a classloader context that was serialized previously.
     * 
     * @param request HTTP request
     * @param serializedContextName The name of the file containing the serialized context.
     * @param classToLoad The name of the class which should be loaded from the serialized context.
     * @throws Exception if an error occurs.
     */
    private void testDeserializeClassloaderContext(HttpServletRequest request, String serializedContextName, Class<?> classToLoad) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream(serializedContextName);
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        final String className = classToLoad.getName();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // switch the classloader to something that does NOT have access to classes from the app
            ClassLoader newClassLoader = classloaderContextSvc.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(newClassLoader);

            // verify the class doesn't load with the class loader
            try {
                newClassLoader.loadClass(className);
                throw new Exception("Test won't be valid if we can load the class here!");
            } catch (ClassNotFoundException x) {
                // expected
            }

            // Run under the deserialized thread context
            contextualExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ClassLoader loader = Thread.currentThread().getContextClassLoader();
                        Class<?> loadedClass = loader.loadClass(className);
                        if (!className.equals(loadedClass.getName()))
                            throw new RuntimeException("Unexpected class name for loaded class: " + loadedClass);
                    } catch (ClassNotFoundException x) {
                        throw new RuntimeException(x);
                    }
                }
            });
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

    }

    /**
     * Deserialize classloader context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeClassloaderContextV8_5_5_4(HttpServletRequest request, HttpServletResponse response) throws Exception {
        testDeserializeClassloaderContext(request, "/WEB-INF/serialized/classloaderContext-v8.5.5.4.ser", ContextServiceSerializationTestServlet.class);
    }

    /**
     * Deserialize classloader context that was serialized on v8.5.5.4 from an EJB
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeClassloaderContextV8_5_5_4_EJB(HttpServletRequest request, HttpServletResponse response) throws Exception {
        testDeserializeClassloaderContext(request, "/WEB-INF/serialized/classloaderContext-EJB-v8.5.5.4.ser", ContextServiceSerializationTestBean.class);
    }

    /**
     * Deserialize a contextual proxy that was serialized on v8.5.5.4 and verify the execution properties.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeExecutionPropertiesV8_5_5_4(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/defaultContext_ALL_CONTEXT_TYPES-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        Map<String, String> expected = Collections.singletonMap("com.ibm.ws.concurrent.DEFAULT_CONTEXT", "ALL_CONTEXT_TYPES");
        Map<String, String> execProps;

        execProps = defaultContextSvc.getExecutionProperties(contextualExecutor);
        if (!expected.equals(execProps))
            throw new Exception("defaultContextSvc.getExecutionProperties: " + execProps);
        execProps.clear();

        execProps = defaultContextSvc.getExecutionProperties(contextualExecutor);
        if (!expected.equals(execProps))
            throw new Exception("defaultContextSvc.getExecutionProperties: " + execProps);
        execProps.put("key1", "value1");

        // any ContextService should be able to obtain the execution properties

        execProps = classloaderContextSvc.getExecutionProperties(contextualExecutor);
        if (!expected.equals(execProps))
            throw new Exception("classloaderContextSvc.getExecutionProperties: " + execProps);

        execProps = jeeMetadataContextSvc.getExecutionProperties(contextualExecutor);
        if (!expected.equals(execProps))
            throw new Exception("jeeMetadataContextSvc.getExecutionProperties: " + execProps);

        execProps = transactionContextSvc.getExecutionProperties(contextualExecutor);
        if (!expected.equals(execProps))
            throw new Exception("transactionContextSvc.getExecutionProperties: " + execProps);

        if (!contextualExecutor.equals(contextualExecutor))
            throw new Exception("Deserialized contextual proxy does not equal self");
    }

    /**
     * Deserialize Java EE metadata context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeJEEMetadataContextV8_5_5_4(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/jeeMetadataContext-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        // Start from an unmanaged thread to prove that the deserialized thread context is properly applied
        Object result = unmanagedExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final List<Object> results = new LinkedList<Object>();
                // Apply the deserialized thread context
                contextualExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            results.add(new InitialContext().lookup("java:comp/env/entry1"));
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }
                });
                Object result = results.get(0);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else
                    return result;
            }
        }).get();

        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected value: " + result);
    }

    /**
     * Deserialize Java EE metadata context that was serialized on v8.5.5.4 from a JSP.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeJEEMetadataContextV8_5_5_4_JSP(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/jeeMetadataContext-JSP-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        // Start from an unmanaged thread to prove that the deserialized thread context is properly applied
        Object result = unmanagedExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final List<Object> results = new LinkedList<Object>();
                // Apply the deserialized thread context
                contextualExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            results.add(new InitialContext().lookup("java:comp/env/entry1"));
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }
                });
                Object result = results.get(0);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else
                    return result;
            }
        }).get();

        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected value: " + result);
    }

    /**
     * Deserialize Java EE metadata context that was serialized on v8.5.5.4 from an EJB.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeJEEMetadataContextV8_5_5_4_EJB(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/jeeMetadataContext-EJB-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        // Start from an unmanaged thread to prove that the deserialized thread context is properly applied
        Object result = unmanagedExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final List<Object> results = new LinkedList<Object>();
                // Apply the deserialized thread context
                contextualExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            results.add(new InitialContext().lookup(ContextServiceSerializationTestBean.ENV_ENTRY_NAME));
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }
                });
                Object result = results.get(0);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else
                    return result;
            }
        }).get();

        if (!Integer.valueOf(ContextServiceSerializationTestBean.ENV_ENTRY_VALUE).equals(result))
            throw new Exception("Unexpected value: " + result);
    }

    /**
     * Deserialize security context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeSecurityContextV8_5_5_4(final HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Verify that we are deserializing from a different context (user2) than we serialized from
        Principal principal = request.getUserPrincipal();
        String principalName = principal.getName();
        if (!"user2".equals(principalName))
            throw new Exception("Unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

        Subject callerSubject = WSSubject.getCallerSubject();
        principalName = callerSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("Unexpected principal name from caller subject: " + callerSubject);

        WSCredential credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
        String realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new Exception("Unexpected realm security name " + realmSecurityName + " in caller subject credential " + credential);

        Subject runAsSubject = WSSubject.getRunAsSubject();
        principalName = runAsSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("Unexpected principal name from runAs subject: " + runAsSubject);

        credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
        realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new Exception("Unexpected realm security name " + realmSecurityName + " in runAs subject credential " + credential);

        // Deserialize
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/securityContext-user3-user3-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        final Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }

        // Run from current thread
        final String deserializedUserName = "user3";
        contextualExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Principal principal = request.getUserPrincipal();
                    String principalName = principal.getName();
                    if (!deserializedUserName.equals(principalName))
                        throw new RuntimeException("Unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

                    Subject callerSubject = WSSubject.getCallerSubject();
                    principalName = callerSubject.getPrincipals().iterator().next().getName();
                    if (!deserializedUserName.equals(principalName))
                        throw new RuntimeException("After applying deserialized thread context, unexpected principal name from caller subject: " + callerSubject);

                    WSCredential credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
                    String realmSecurityName = credential.getRealmSecurityName();
                    if (!"TestRealm/user3".equals(realmSecurityName))
                        throw new RuntimeException("After applying deserialized thread context, unexpected realm security name " + realmSecurityName
                                                   + " in caller subject credential " + credential);

                    Subject runAsSubject = WSSubject.getRunAsSubject();
                    principalName = runAsSubject.getPrincipals().iterator().next().getName();
                    if (!deserializedUserName.equals(principalName))
                        throw new RuntimeException("After applying deserialized thread context, unexpected principal name from runAs subject: " + runAsSubject);

                    credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
                    realmSecurityName = credential.getRealmSecurityName();
                    if (!"TestRealm/user3".equals(realmSecurityName))
                        throw new RuntimeException("After applying deserialized thread context, unexpected realm security name " + realmSecurityName
                                                   + " in runAs subject credential " + credential);

                    // Apply context again on top of existing context (so that we can see if it restores properly)
                    contextualExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Principal principal = request.getUserPrincipal();
                                String principalName = principal.getName();
                                if (!deserializedUserName.equals(principalName))
                                    throw new RuntimeException("Nested context propagation: Unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

                                Subject callerSubject = WSSubject.getCallerSubject();
                                principalName = callerSubject.getPrincipals().iterator().next().getName();
                                if (!deserializedUserName.equals(principalName))
                                    throw new RuntimeException("Nested context propagation: Unexpected principal name from caller subject: " + callerSubject);

                                WSCredential credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
                                String realmSecurityName = credential.getRealmSecurityName();
                                if (!"TestRealm/user3".equals(realmSecurityName))
                                    throw new RuntimeException("Nested context propagation: Unexpected realm security name " + realmSecurityName
                                                               + " in caller subject credential " + credential);

                                Subject runAsSubject = WSSubject.getRunAsSubject();
                                principalName = runAsSubject.getPrincipals().iterator().next().getName();
                                if (!deserializedUserName.equals(principalName))
                                    throw new RuntimeException("Nested context propagation: Unexpected principal name from runAs subject: " + runAsSubject);

                                credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
                                realmSecurityName = credential.getRealmSecurityName();
                                if (!"TestRealm/user3".equals(realmSecurityName))
                                    throw new RuntimeException("Nested context propagation: Unexpected realm security name " + realmSecurityName
                                                               + " in runAs subject credential " + credential);
                            } catch (WSSecurityException x) {
                                throw new RuntimeException(x);
                            } catch (CredentialExpiredException x) {
                                throw new RuntimeException(x);
                            } catch (CredentialDestroyedException x) {
                                throw new RuntimeException(x);
                            }
                        }
                    });

                    principalName = principal.getName();
                    if (!deserializedUserName.equals(principalName))
                        throw new RuntimeException("After nested context removed: Unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

                    callerSubject = WSSubject.getCallerSubject();
                    principalName = callerSubject.getPrincipals().iterator().next().getName();
                    if (!deserializedUserName.equals(principalName))
                        throw new RuntimeException("After nested context removed: Unexpected principal name from caller subject: " + callerSubject);

                    credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
                    realmSecurityName = credential.getRealmSecurityName();
                    if (!"TestRealm/user3".equals(realmSecurityName))
                        throw new RuntimeException("After nested context removed: Unexpected realm security name " + realmSecurityName
                                                   + " in caller subject credential " + credential);

                    runAsSubject = WSSubject.getRunAsSubject();
                    principalName = runAsSubject.getPrincipals().iterator().next().getName();
                    if (!deserializedUserName.equals(principalName))
                        throw new RuntimeException("After nested context removed: Unexpected principal name from runAs subject: " + runAsSubject);

                    credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
                    realmSecurityName = credential.getRealmSecurityName();
                    if (!"TestRealm/user3".equals(realmSecurityName))
                        throw new RuntimeException("After nested context removed: Unexpected realm security name " + realmSecurityName
                                                   + " in runAs subject credential " + credential);
                } catch (WSSecurityException x) {
                    throw new RuntimeException(x);
                } catch (CredentialExpiredException x) {
                    throw new RuntimeException(x);
                } catch (CredentialDestroyedException x) {
                    throw new RuntimeException(x);
                }
            }
        });

        // Verify that context is restored on current thread after contextual task
        principal = request.getUserPrincipal();
        principalName = principal.getName();
        if (!"user2".equals(principalName))
            throw new Exception("After contextual task, unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

        callerSubject = WSSubject.getCallerSubject();
        principalName = callerSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("After contextual task, unexpected principal name from caller subject: " + callerSubject);

        credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
        realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new Exception("After contextual task, unexpected realm security name " + realmSecurityName + " in caller subject credential " + credential);

        runAsSubject = WSSubject.getRunAsSubject();
        principalName = runAsSubject.getPrincipals().iterator().next().getName();
        if (!"user2".equals(principalName))
            throw new Exception("After contextual task, unexpected principal name from runAs subject: " + runAsSubject);

        credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
        realmSecurityName = credential.getRealmSecurityName();
        if (!"TestRealm/user2".equals(realmSecurityName))
            throw new RuntimeException("After contextual task, unexpected realm security name " + realmSecurityName
                                       + " in runAs subject credential " + credential);

        // Run from unmanaged thread
        unmanagedExecutor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                contextualExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String principalName;

                            Subject callerSubject = WSSubject.getCallerSubject();
                            principalName = callerSubject.getPrincipals().iterator().next().getName();
                            if (!deserializedUserName.equals(principalName))
                                throw new RuntimeException("After applying context to unmanaged thread, unexpected principal name from caller subject: " + callerSubject);

                            WSCredential credential = callerSubject.getPublicCredentials(WSCredential.class).iterator().next();
                            String realmSecurityName = credential.getRealmSecurityName();
                            if (!"TestRealm/user3".equals(realmSecurityName))
                                throw new RuntimeException("After applying context to unmanaged thread, unexpected realm security name " + realmSecurityName
                                                           + " in caller subject credential " + credential);

                            Subject runAsSubject = WSSubject.getRunAsSubject();
                            principalName = runAsSubject.getPrincipals().iterator().next().getName();
                            if (!deserializedUserName.equals(principalName))
                                throw new RuntimeException("After applying context to unmanaged thread, unexpected principal name from runAs subject: " + runAsSubject);

                            credential = runAsSubject.getPublicCredentials(WSCredential.class).iterator().next();
                            realmSecurityName = credential.getRealmSecurityName();
                            if (!"TestRealm/user3".equals(realmSecurityName))
                                throw new RuntimeException("After applying context to unmanaged thread, unexpected realm security name " + realmSecurityName
                                                           + " in runAs subject credential " + credential);
                        } catch (WSSecurityException x) {
                            throw new RuntimeException(x);
                        } catch (CredentialExpiredException x) {
                            throw new RuntimeException(x);
                        } catch (CredentialDestroyedException x) {
                            throw new RuntimeException(x);
                        }
                    }
                });
                return null;
            }
        }).get();
    }

    /**
     * Deserialize transaction context that was serialized on v8.5.5.4.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testDeserializeTransactionContextV8_5_5_4(HttpServletRequest request, HttpServletResponse response) throws Exception {
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/transactionContext-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        Executor contextualExecutor;
        try {
            contextualExecutor = (Executor) objectInput.readObject();
        } finally {
            objectInput.close();
        }
        tran.begin();
        try {
            contextualExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Nested transactions aren't allowed, so this will only work if transactionContext has
                        // successfully suspended the transaction on the thread of execution that invokes this task.
                        tran.begin();
                        tran.commit();
                    } catch (RuntimeException x) {
                        throw x;
                    } catch (Exception x) {
                        throw new RuntimeException(x);
                    }
                }
            });
        } finally {
            tran.commit();
        }

        Map<String, String> execProps = transactionContextSvc.getExecutionProperties(contextualExecutor);
        if (execProps != null)
            throw new Exception("Execution properties should be null, not " + execProps);
    }

    /**
     * Serialize and deserialize a contextual proxy of a proxy.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testProxyForAProxy(HttpServletRequest request, HttpServletResponse response) throws Exception {
        final CurrentThreadExecutor instance = new CurrentThreadExecutor();
        Executor proxy = (Executor) Proxy.newProxyInstance(CurrentThreadExecutor.class.getClassLoader(),
                                                           new Class[] { Executor.class },
                                                           new TestInvocationHandler(instance));

        Executor contextualExecutor = jeeMetadataContextSvc.createContextualProxy(proxy, Executor.class);

        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOutput);
        out.writeObject(contextualExecutor);
        out.flush();
        byte[] bytes = byteOutput.toByteArray();
        out.close();

        // deserialize again
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        final Executor contextualExecutorProxy = (Executor) in.readObject();
        in.close();

        // Start from an unmanaged thread to prove that the deserialized thread context is properly applied
        Object result = unmanagedExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final List<Object> results = new LinkedList<Object>();
                // Apply the deserialized thread context
                contextualExecutorProxy.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            results.add(new InitialContext().lookup("java:comp/env/entry1"));
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }
                });
                Object result = results.get(0);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else
                    return result;
            }
        }).get();

        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected value: " + result);
    }

    /**
     * Deserialize and reserialize Java EE metadata context.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testReserializeJEEMetadataContext(HttpServletRequest request, HttpServletResponse response) throws Exception {
        // deserialize
        InputStream input = request.getServletContext().getResourceAsStream("/WEB-INF/serialized/jeeMetadataContext-v8.5.5.4.ser");
        ObjectInputStream objectInput = new ObjectInputStream(input);
        Object object;
        try {
            object = objectInput.readObject();
        } finally {
            objectInput.close();
        }

        // reserialize
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOutput);
        out.writeObject(object);
        out.flush();
        byte[] bytes = byteOutput.toByteArray();
        out.close();

        // deserialize again
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
        final Executor contextualExecutor = (Executor) in.readObject();
        in.close();

        // Start from an unmanaged thread to prove that the deserialized thread context is properly applied
        Object result = unmanagedExecutor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                final List<Object> results = new LinkedList<Object>();
                // Apply the deserialized thread context
                contextualExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            results.add(new InitialContext().lookup("java:comp/env/entry1"));
                        } catch (Throwable x) {
                            results.add(x);
                        }
                    }
                });
                Object result = results.get(0);
                if (result instanceof Exception)
                    throw (Exception) result;
                else if (result instanceof Error)
                    throw (Error) result;
                else
                    return result;
            }
        }).get();

        if (!Integer.valueOf(1).equals(result))
            throw new Exception("Unexpected value: " + result);
    }

    /**
     * Serialize classloader context.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeClassloaderContext(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Executor contextualExecutor = classloaderContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("classloaderContext-vNext.ser")));
        try {
            outfile.writeObject(contextualExecutor);
        } finally {
            outfile.close();
        }
    }

    /**
     * Serialize Java EE metadata context.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeJEEMetadataContext(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Executor contextualExecutor = jeeMetadataContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("jeeMetadataContext-vNext.ser")));
        try {
            outfile.writeObject(contextualExecutor);
        } finally {
            outfile.close();
        }
    }

    /**
     * Serialize default context.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeDefaultContext_ALL_CONTEXT_TYPES(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, String> execProps = Collections.singletonMap("com.ibm.ws.concurrent.DEFAULT_CONTEXT", "ALL_CONTEXT_TYPES");
        Executor contextualExecutor = defaultContextSvc.createContextualProxy(new CurrentThreadExecutor(), execProps, Executor.class);
        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("defaultContext_ALL_CONTEXT_TYPES-vNext.ser")));
        try {
            outfile.writeObject(contextualExecutor);
        } finally {
            outfile.close();
        }
    }

    /**
     * Serialize security context.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeSecurityContext(final HttpServletRequest request, HttpServletResponse response) throws Exception {
        // Verify that we will be serializing what we expect
        Principal principal = request.getUserPrincipal();
        String principalName = principal.getName();
        String username = "user3";
        if (!username.equals(principalName))
            throw new Exception("Unexpected result for HttpServletRequest.getUserPrincipal: " + principal);

        Subject callerSubject = WSSubject.getCallerSubject();
        principalName = callerSubject.getPrincipals().iterator().next().getName();
        if (!username.equals(principalName))
            throw new Exception("Unexpected principal name from caller subject: " + callerSubject);

        Subject runAsSubject = WSSubject.getRunAsSubject();
        principalName = runAsSubject.getPrincipals().iterator().next().getName();
        if (!username.equals(principalName))
            throw new Exception("Unexpected principal name from runAs subject: " + runAsSubject);

        final Executor contextualExecutor = securityContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);

        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("securityContext-user3-user3-vNext.ser")));
        try {
            outfile.writeObject(contextualExecutor);
        } finally {
            outfile.close();
        }
    }

    /**
     * Serialize transaction context.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeTransactionContext(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Executor contextualExecutor = transactionContextSvc.createContextualProxy(new CurrentThreadExecutor(), Executor.class);
        ObjectOutputStream outfile = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("transactionContext-vNext.ser")));
        try {
            outfile.writeObject(contextualExecutor);
        } finally {
            outfile.close();
        }
    }

    /**
     * Serialize Java EE metadata context from within an EJB.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeJEEMetadataContext_EJB(HttpServletRequest reqeust, HttpServletResponse response) throws Exception {
        testBean.testSerializeJEEMetadataContext();
    }

    /**
     * Serialize classloader context from within an EJB.
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @throws Exception if an error occurs.
     */
    public void testSerializeClassloaderContext_EJB(HttpServletRequest reqeust, HttpServletResponse response) throws Exception {
        testBean.testSerializeClassloaderContext();
    }

}