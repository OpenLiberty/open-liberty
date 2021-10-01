/*******************************************************************************
 * Copyright (c) 2018,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.JAXRSClientStandalone.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;
    private static final String moduleName = "jaxrsclientstandalone";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = (String) itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            m.put("serverIP", req.getLocalAddr());
            m.put("serverPort", String.valueOf(req.getLocalPort()));

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }

    public void testNewClientBuilder_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        ret.append("OK");
    }

    public void testNewClient_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        c.close();
        ret.append("OK");
    }

    public void testNewWebTarget_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        c.close();
        ret.append("OK");
    }

    public void testNewInvocationBuilder_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        c.close();
        ret.append("OK");
    }

    public void testNewInvocation_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target("http://localhost:8010/dumpservice/test");
        Builder ib = t.request();
        Invocation iv = ib.buildGet();
        c.close();
        ret.append("OK");
    }

    public void testFlowProgram_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        String res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/Test/BasicResource")
                        .path("echo")
                        .path(param.get("param"))
                        .request()
                        .get(String.class);
        System.out.println("testFlowProgram_ClientStandalone - recvd " + res);
        c.close();
        ret.append(res);
    }


    static class ClientFunction implements Function<Client, Client> {
        @Override
        public Client apply(Client client) {
            return client;
        }
    }

    static class WebTargetFunction implements Function<Client, WebTarget> {
        @Override
        public WebTarget apply(Client client) {
            return client.target("http://localhost:8010/dumpservice/test");
        }
    }

    static class InvocationBuilderFunction implements Function<Client, Builder> {
        @Override
        public Builder apply(Client client) {
            WebTarget t = client.target("http://localhost:8010/dumpservice/test");
            return t.request();
        }
    }

    static class InvocationFunction implements Function<Client, Invocation> {
        @Override
        public Invocation apply(Client client) {
            WebTarget t = client.target("http://localhost:8010/dumpservice/test");
            return t.request().buildGet();
        }
    }

    static class AsyncInvokerFunction implements Function<Client, AsyncInvoker> {
        @Override
        public AsyncInvoker apply(Client client) {
            WebTarget t = client.target("http://localhost:8010/dumpservice/test");
            return t.request().async();
        }
    }

    static class CompletionStageRxInvokerFunction implements Function<Client, Object> {
        @Override
        public Object apply(Client client) {
            WebTarget t = client.target("http://localhost:8010/dumpservice/test");
            Builder builder = t.request();
            try {
                Method m = builder.getClass().getMethod("rx");
                return m.invoke(builder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This test makes sure that if an application doesn't call Client.close() on their
     * JAXRS Client object that the runtime will not leak.  This was a problem in the past 
     * and resolved by asking customers to update their application.  This was partially fixed
     * by using a WeakHashMap in JAXRSClientImpl, but the clientsPerModule collection in JAXRSClientImpl
     * still has a hard reference.  With the introduction of using weak references in clientsPerModule, 
     * the liberty runtime now remove the weak reference object on the next time a Client is created
     * or closed.  This test validates that that function works and will continue to work into the future.
     */
    public void testMemoryLeak_ClientStandalone(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();

        Map<String, List<?>> clientsPerModule = null;
        if (c.getClass().getName().equals("com.ibm.ws.jaxrs20.client.JAXRSClientImpl")) {
            try {
                Field clientsPerModuleField = c.getClass().getDeclaredField("clientsPerModule");
                clientsPerModuleField.setAccessible(true);
                clientsPerModule = (Map<String, List<?>>) clientsPerModuleField.get(null);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        c.close();

        List<Function<Client, ?>> functions = new ArrayList<>();
        functions.add(new ClientFunction());
        functions.add(new WebTargetFunction());
        functions.add(new InvocationBuilderFunction());
        functions.add(new InvocationFunction());
        functions.add(new AsyncInvokerFunction());

        // Add the 2.1 specific methods if they are found
        boolean rxMethodsFound = false;
        Method[] builderMethods = Builder.class.getMethods();
        for (Method builderMethod : builderMethods) {
            if (builderMethod.getName().equals("rx")) {
                rxMethodsFound = true;
                break;
            }
        }

        if (rxMethodsFound) {
            System.out.println("ClientTestServlet.testMemoryLeak_ClientStandalone Adding reactive method functions");
            functions.add(new CompletionStageRxInvokerFunction());
        }

        boolean[] useNewClientOption = new boolean[] {true, false};
        for (Function<Client, ?> function : functions) {
            for (boolean useNewClient : useNewClientOption) {
                memoryLeakTest(function, clientsPerModule, ret, useNewClient);
                if (ret.length() != 0) {
                    return;
                }
            }
        }

        ret.append("OK");
    }

    private void memoryLeakTest(Function<Client, ?> testFunction, Map<String, List<?>> clientsPerModule, StringBuilder ret, boolean useNewClientToPoll) {
        
        System.out.println("ClientTestServlet.memoryLeakTest entry");
        System.out.println("    testFunction " + testFunction);
        if (clientsPerModule == null) {
            System.out.println("    clientsPerModule null " );
        } else {
            System.out.println("    clientsPerModule " + clientsPerModule.entrySet());
        }        
        System.out.println("    ret " + ret);
        System.out.println("    useNewClientToPoll " + useNewClientToPoll);
        
        int startingStoredClients = clientsPerModule == null ? 0 : getStoredClientCount(clientsPerModule);
        System.out.println("ClientTestServlet.memoryLeakTest startingStoredClients " + startingStoredClients);
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();

        Object memoryLeakObject = testFunction.apply(c);

        boolean isMemoryLeakObjectClient = memoryLeakObject != c;
        WeakReference<Client> weakRef = new WeakReference<>(c);

        // If using close we need to new up a new Client.  Doing it here so aren't triggering
        // the clean from the new.
        c = useNewClientToPoll ? null : cb.build();

        WeakReference<Object> weakRef2 = null;
        
        if (!isMemoryLeakObjectClient) {
            doGarbageCollectCheck(weakRef, false);

            // At this point the underlying JAXRSClientImpl object is still in memory
            // because the memoryLeakObject should still have a reference to it
            if (weakRef.get() == null) {
                ret.append("Expected JAXRSClientImpl reference still existing was not detected. " + memoryLeakObject);
                System.out.println("ClientTestServlet.memoryLeakTest exit " + ret);
                return;
            }
            weakRef2 = new WeakReference<>(memoryLeakObject);
        }

        memoryLeakObject = null;

        doGarbageCollectCheck(weakRef, true);

        // At this point the Client object should be garbage collected..
        if (weakRef.get() != null) {
            ret.append("Expected Client to be garbage collected.");
            System.out.println("ClientTestServlet.memoryLeakTest exit " + ret);
            return;
        }

        if (weakRef2 != null && weakRef2.get() != null) {
            ret.append("Expected object that references the Client to be garbage collected. " + weakRef2.get());
            System.out.println("ClientTestServlet.memoryLeakTest exit " + ret);
            return;
        }

        if (useNewClientToPoll) {
            // making a new Client should trigger cleanup of the ClientWeakReference
            // from the JAXRSClientImpl clientsPerModule Map.
            c = cb.build();
        } else {
            c.close();
        }

        if (clientsPerModule != null) {
            int endStoredClients = getStoredClientCount(clientsPerModule); 

            if (endStoredClients > (useNewClientToPoll ? startingStoredClients + 1 : startingStoredClients)) {
                ret.append("Leak was detected in clientsPerModule. " + useNewClientToPoll + " " + startingStoredClients + " " + endStoredClients);
            }
        }

        if (useNewClientToPoll) {
            c.close();
        }
        
        System.out.println("ClientTestServlet.memoryLeakTest exit " + ret);
    }

    private int getStoredClientCount(Map<String, List<?>> clientsPerModule) {
        int count = 0;
        for (List<?> clients : clientsPerModule.values()) {
            count += clients.size();
        }
        return count;
    }
    
    private void doGarbageCollectCheck(WeakReference<Client> weakRef, boolean nullExpected) {
        for (int i = 0; i < 10; ++i) {
            System.out.println("ClientTestServlet.doGarbageCollectCheck calling system gc i " + i);
            System.gc();
            // give the GC some time to actually do its thing
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (nullExpected && weakRef.get() == null) {
                break;
            }

            if (!nullExpected && weakRef.get() != null) {
                break;
            }
        }
    }
}
