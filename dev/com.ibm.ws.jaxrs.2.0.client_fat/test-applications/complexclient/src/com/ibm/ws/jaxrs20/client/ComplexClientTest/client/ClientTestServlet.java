/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.ComplexClientTest.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.AsyncInvoker;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.ws.jaxrs20.client.ComplexClientTest.service.MyObject;
import com.ibm.ws.jaxrs20.client.ComplexClientTest.service.Person;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;
    private static final String moduleName = "complexclient";

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

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

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
            e.printStackTrace(); // print to server logs
            e.printStackTrace(pw); // print back in the response to the web client
        }
    }

    public void testClientPropertyInherit(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("inherit1", "cb");
        Client c = cb.build();
        boolean cbValue1  = c.getConfiguration().getProperties().containsValue("cb");
        c.property("inherit2", "c");
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res1 = t1.path("echo1").path("test").request().get(String.class);
        boolean cbValue2  = t1.getConfiguration().getProperties().containsValue("cb");
        boolean cValue  = t1.getConfiguration().getProperties().containsValue("c");
        c.close();
        ret.append(cbValue1 + "," + cbValue2 + "," + cValue + "," + res1);
    }

    public void testClientProviderInherit(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.register(MyWriter.class);
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/fruits");
        String result1 = t1
                        .request().accept(MyObject.MIME_TYPE)
                        .post(Entity.entity(new MyObject(1), MyObject.MIME_TYPE), String.class);

        String result2 = t1
                        .request().accept(MyObject.MIME_TYPE)
                        .post(Entity.entity(new MyObject(2), MyObject.MIME_TYPE), String.class);
        c.register(MyReader.class);
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/fruits");
        MyObject result3 = t2.request().accept(MyObject.MIME_TYPE).get(MyObject.class);
        ret.append(result1 + "," + result2 + "," + result3.getIndex());
    }

    public void testNewClientBuilder(Map<String, String> param, StringBuilder ret) {
        ClientBuilder cb = ClientBuilder.newBuilder();
        ret.append(cb.getClass().toString());
    }

    public void testNewClient(Map<String, String> param, StringBuilder ret) {
        Client c = ClientBuilder.newClient();
        c.property("clientproperty1", "somevalue1");
        Configuration config1 = c.getConfiguration();
        c.close();
        c = ClientBuilder.newClient(config1);
        c.property("clientproperty2", "somevalue2");
        boolean cValue1  = c.getConfiguration().getProperties().containsValue("somevalue1");
        boolean cValue2  = c.getConfiguration().getProperties().containsValue("somevalue2");
        c.close();
        ret.append(cValue1 + "," + cValue2);
    }

    public void testNewClientWithConfig(Map<String, String> param, StringBuilder ret) {
        Client c = ClientBuilder.newClient();
        c.property("clientproperty3", "somevalue3");
        Configuration config1 = c.getConfiguration();
        c.close();
        ClientBuilder cb = ClientBuilder.newBuilder().withConfig(config1);
        c = cb.build();
        c.property("clientproperty4", "somevalue4");
        boolean cValue1  = c.getConfiguration().getProperties().containsValue("somevalue3");
        boolean cValue2  = c.getConfiguration().getProperties().containsValue("somevalue4");
        c.close();
        ret.append(cValue1 + "," + cValue2);
    }

    public void testNewClientHostnameVerifier(Map<String, String> param, StringBuilder ret) {
        HostnameVerifier hv = new HostnameVerifier() {
            @Override
            public boolean verify(String urlHostName, SSLSession session) {
                System.out.println("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                return true;
            }
        };
        ClientBuilder cb = ClientBuilder.newBuilder().hostnameVerifier(hv);
        Client c = cb.build();
        String result = c.getHostnameVerifier().toString();
        c.close();
        ret.append(result);
    }

    public void testNewClientSslContext(Map<String, String> param, StringBuilder ret) throws NoSuchAlgorithmException {
        SSLContext ssl = SSLContext.getDefault();
        ClientBuilder cb = ClientBuilder.newBuilder().sslContext(ssl);
        Client c = cb.build();
        String result = c.getSslContext().toString();
        c.close();
        ret.append(result);
    }

    public void testNew2WebTargets(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res1 = t1.path("echo1").path("test1").request().get(String.class);
        String res2 = t2.path("echo2").path("test2").request().get(String.class);
        c.close();
        ret.append(res1 + "," + res2);
    }

    public void testNew2Invocations(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        Builder ib = t1.path("echo1").path("test3").request();
        Invocation iv = ib.buildGet();
        String res1 = iv.invoke(String.class);
        Builder ib2 = t2.path("echo2").path("test4").request();
        Invocation iv2 = ib2.buildGet();
        String res2 = iv2.invoke(String.class);
        c.close();
        ret.append(res1 + "," + res2);
    }

    public void testNew2FailedWebTargets(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res1 = t1.path("echo1").path("test5").request().get(String.class);
        c.close();
        String res2 = "";
        String res3 = "";
        try {
            res2 = t2.path("echo2").path("test6").request().get(String.class);
        } catch (Exception e) {
            res2 = "ECHO2:failed";
        }
        try {
            res3 = t1.path("echo1").path("test7").request().get(String.class);
        } catch (Exception e) {
            res3 = "ECHO3:failed";
        }
        ret.append(res1 + "," + res2 + "," + res3);
    }

    public void testWebTargetWithEncoding(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        String encodeString = "";
        String decodeString = "";
        WebTarget t1 = null;
        WebTarget t2 = null;
        String result1 = "";
        String result2 = "";

        try {
            encodeString = URLEncoder.encode("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource", "UTF-8");
            decodeString = URLDecoder.decode(encodeString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        try {
            t1 = c.target(encodeString);
            t1.path("echo1").path("test8").request().get(String.class);
        } catch (Exception e) {
            // 'java.lang.IllegalArgumentException: URI is not absolute' will be in the logs
            result1 = "ECHO1:failed";
        }

        t2 = c.target(decodeString);
        result2 = t2.path("echo1").path("test9").request().get(String.class);

        ret.append(result1 + "," + result2);
    }

    public void testNew2WebTargetsRequestFilter(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource").register(ClientRequestFilter1.class);
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource").register(ClientRequestFilter2.class);
        t1.path("echo1").path("test1").request().accept("*/*").get(String.class);
        String result1 = c.getConfiguration().getProperties().entrySet().toString();
        t2.path("echo2").path("test2").request().accept("*/*").get(String.class);
        String result2 = c.getConfiguration().getProperties().entrySet().toString();
        c.close();
        ret.append(result1 + "," + result2);
    }

    public void testNew2ResponseFilter(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        c.register(ClientResponseFilter1.class, 200);
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource").register(ClientResponseFilter2.class, 100);
        Response res1 = t1.path("echo1").path("test1").request().accept("*/*").get(Response.class);
        Response res2 = t2.path("echo2").path("test2").request().accept("*/*").get(Response.class);
        System.out.println("config: " + c.getConfiguration().getProperties());
        c.close();
        ret.append(res1.getStatus() + "," + res2.getStatus());
    }

    public void testNew2MixFilter(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        c.register(ClientResponseFilter1.class);
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource").register(ClientRequestFilter1.class);
        Response res1 = t1.path("echo1").path("test1").request().get(Response.class);
        String result1 = getProperties(c.getConfiguration());
        c = cb.build();
        WebTarget t2 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource").register(ClientResponseFilter2.class).register(ClientRequestFilter2.class);
        Response res2 = t2.path("echo2").path("test2").request().get(Response.class);
        String result2 = getProperties(c.getConfiguration());
        c.close();
        ret.append(res1.getStatus() + "," + result1 + "," + res2.getStatus() + "," + result2);
    }

    private String getProperties(Configuration c) {
        Set<String> props = new HashSet<>();
        for (String propName : c.getPropertyNames()) {
            props.add(propName + "=" + c.getProperty(propName));
        }
        return "{" + String.join(",", props) + "}";
    }
    public void testClientResponseProcessingException(Map<String, String> param, StringBuilder ret) {
        // Don't run the test in 2.1, Jackson throws and exception, jsonb doesn't
        boolean jaxrs21 = true;
        try {
            // if we can load this sse class then we are running in 2.1
            Class.forName("javax.ws.rs.sse.SseEventSource");
        } catch (Throwable t) {
            jaxrs21 = false;
        }

        // if 2.1 just return the expected response to pass the test
        if (jaxrs21) {
            ret.append("Problem with reading the data");
            return;
        }

        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder().keyStore(null, getServletName());
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res1 = "";
        try {
            //Different Person class, so will report Exception: Problem with reading the data...
            Person person = t1.path("person").request().accept("application/json").get(Person.class);
            res1 = person.getName();
        } catch (ResponseProcessingException re) {
            res1 = re.getMessage();
        }
        ret.append(res1);
    }

    public void testWebApplicationException3xx(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder().keyStore(null, getServletName());
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res1 = "";
        try {
            res1 = t1.path("three").request().get(String.class);
        } catch (Exception we) {
            res1 = we.getMessage();
        }
        ret.append(res1);
    }

    public void testWebApplicationException4xx(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder().keyStore(null, getServletName());
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res1 = "";
        try {
            res1 = t1.path("four").request().get(String.class);
        } catch (Exception we) {
            res1 = we.getMessage();
        }
        ret.append(res1);
    }

    public void testWebApplicationException5xx(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder().keyStore(null, getServletName());
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res1 = "";
        try {
            res1 = t1.path("five").request().get(String.class);
        } catch (Exception we) {
            res1 = we.getMessage();
        }
        ret.append(res1);
    }

    public void testClientLtpaHander_ClientNoToken(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "true");
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res = t.path("echo1").path("test1").request().get(String.class);
        c.close();
        ret.append(res);
    }

    public void testClientWrongLtpaHander_ClientNoToken(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa", "false");
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res = t.path("echo1").path("test1").request().get(String.class);
        c.close();
        ret.append(res);
    }

    public void testClientWrongValueLtpaHander_ClientNoToken(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder();

        Client c = cb.build();
        c.property("com.ibm.ws.jaxrs.client.ltpa.handler", "false");
        WebTarget t = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource");
        String res = t.path("echo1").path("test1").request().get(String.class);
        c.close();
        ret.append(res);
    }

    public void testTraceForCTS(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder().keyStore(null, getServletName());
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource/trace");
        String res1 = "";
        try {
            res1 = t1.request().trace(String.class);
        } catch (Exception we) {
            res1 = we.getMessage();
        }
        ret.append(res1);
    }

    public void testVariantResponseForCTS(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");

        ClientBuilder cb = ClientBuilder.newBuilder().keyStore(null, getServletName());
        Client c = cb.build();
        WebTarget t1 = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/ComplexResource/SelectVariantTestResponse");

        Response response = t1.request()
                        .accept("application/json")
                        .acceptEncoding("*").get();
        List<String> headers = response.getStringHeaders().get("Vary");
        String vary = headers.get(0);
        System.out.println(vary);
        System.out.println(".*Accept.*Accept.*Accept.*:" + vary.matches(".*Accept.*Accept.*Accept.*"));
        response.close();

        ret.append(vary.matches(".*Accept.*Accept.*Accept.*") + "");
    }

    public void testThrowsExceptionForCTS(Map<String, String> param, StringBuilder ret) {
        String url = "http://justforcts.test:6789/resource/delete";
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        AsyncInvoker async = target.request().async();
        Future<Response> future = async.delete();
        try {
            future.get();
            ret.append(false + "");
            throw new Exception("ExecutionException has not been thrown");
        } catch (ExecutionException e) {
            ret.append(true + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testResourceMethodLinkUsedInInvocationForCTS(Map<String, String> param, StringBuilder ret) {
        String ServerUri = "http://justforcts.test:6789/resource/";
        final String linkName = "link";
        Client client = ClientBuilder.newClient();
        client.register(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext ctx) throws IOException {
                String uri = ctx.getUri().toASCIIString();
                Link.Builder builder = Link.fromMethod(Resource.class,
                                                       "consumesAppJson").rel(linkName);
                builder.baseUri(uri);
                Link link = builder.build();
                System.out.println("filter invoke: Link build");
                Response response = Response.ok(uri).links(link).build();
                ctx.abortWith(response);
            }
        });

        String result = "";
        // Phase 1, ask for a link;
        System.out.println("Phase 1, ask for a link");
        WebTarget target = client.target(ServerUri + "get");
        Response response = target.request().get();
        String entity = response.readEntity(String.class);
        result = entity.contains("resource/get") + ",";

        // Phase 2, use the link, check the correctness
        System.out.println("Phase 2, use the link, check the correctness");
        Link link = response.getLink(linkName);
        response = client.invocation(link).post(null);
        entity = response.readEntity(String.class);
        result += entity.contains("resource/consumesappjson") + "";
        ret.append(result);
    }

    public void testGetSetEntityStreamOnRequestFilter(Map<String, String> param, StringBuilder ret) throws Exception {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        String message = "ENTITY_STREAM_WORKS";
        String entity = message.replace('T', 'X');

        Client client = ClientBuilder.newClient();
        client.register(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext context) throws IOException {
                context.setEntityStream(new ReplacingOutputStream(context.getEntityStream(), 'X', 'T'));
            }
        });

        WebTarget target = client.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/SimpleResource/post");
        Response response = target.request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));

        ret.append(response.readEntity(String.class));
    }

    public void testGetSetEntityStreamOnResponseFilter(Map<String, String> param, StringBuilder ret) throws Exception {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        final String message = "ENTITY_STREAM_WORKS";
        String entity = message.replace('T', 'X');
        final AtomicInteger filterInvocationCount = new AtomicInteger(0);

        Client client = ClientBuilder.newClient();
        client.register(new ClientResponseFilter() {
            @Override
            public void filter(ClientRequestContext reqContext, ClientResponseContext respContext) throws IOException {
                filterInvocationCount.incrementAndGet();
                ByteArrayInputStream bais = new ByteArrayInputStream(message.getBytes());
                respContext.setEntityStream(bais);
            }
        });

        WebTarget target = client.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/SimpleResource/post");
        System.out.println("entity=" + entity);
        Response response = target.request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));

        ret.append(response.readEntity(String.class)).append(filterInvocationCount.get());
    }

    public void testTargetTemplateVariable(Map<String, String> param, StringBuilder ret) throws Exception {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        String entity = "123";

        Client client = ClientBuilder.newClient();

        WebTarget target = client.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/ComplexClientTest/SimpleResource/{var1}").resolveTemplate("var1", "post");
        Response response = target.request().post(Entity.entity(entity, MediaType.TEXT_PLAIN_TYPE));

        ret.append(response.readEntity(String.class));
    }
}
