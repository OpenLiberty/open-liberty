/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.jackson;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebServlet("/TestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 1590626324074033327L;
    private String jacksonwar;
    private String serverIP;
    private String serverPort;

    private static Client client;

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
            client = ClientBuilder.newClient();
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator<String> itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            jacksonwar = m.get("jacksonwar");
            serverIP = req.getLocalAddr();
            serverPort = String.valueOf(req.getLocalPort());
            m.put("serverIP", serverIP);
            m.put("serverPort", serverPort);

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(); // print to the logs too since the test client only reads the first line of the pw output
            if (e instanceof InvocationTargetException) {
                e.getCause().printStackTrace(pw);
            } else {
                e.printStackTrace(pw);
            }
        } finally {
            client.close();
        }
    }

    private String getAddress(String path) {
        return "http://" + serverIP + ":" + serverPort + "/" + jacksonwar + "/" + path;
    }

    public void testGETPerson(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "{\"first\":\"first\",\"last\":\"last\"}";

        WebTarget target = client.target(getAddress("pojo/person/person"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testGETPersonWithNullValue(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "{\"first\":\"first\"}";

        WebTarget target = client.target(getAddress("pojo/person/person/last/null"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testPOSTPerson(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "{\"first\":\"first\",\"last\":\"last\"}";

        WebTarget target = client.target(getAddress("pojo/person/person"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(pattern));

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testGETCollection(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[\"string1\",\"\",\"string3\"]";

        WebTarget target = client.target(getAddress("pojo/person/string"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testPOSTCollection(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[\"string1\",\"\",\"string3\"]";

        WebTarget target = client.target(getAddress("pojo/person/string"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(pattern));

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testGETCollectionWithObject(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}]";

        WebTarget target = client.target(getAddress("pojo/person/personcollect"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testPOSTCollectionWithObject(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}]";

        WebTarget target = client.target(getAddress("pojo/person/personcollect"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(pattern));

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testGETCollectionWithCollection(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";

        WebTarget target = client.target(getAddress("pojo/person/collectionofcollection"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testPOSTCollectionWithCollection(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";

        WebTarget target = client.target(getAddress("pojo/person/collectionofcollection"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(pattern));

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testGETCollectionWithArray(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";

        WebTarget target = client.target(getAddress("pojo/person/collectionofarray"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    public void testPOSTCollectionWithArray(Map<String, String> param, StringBuilder ret) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";

        WebTarget target = client.target(getAddress("pojo/person/collectionofarray"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(pattern));

        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        if (!compareJSON(pattern, response.readEntity(String.class), ret)) {
            return;
        }
        ret.append("OK");
    }

    /**
     * Tests that a HashMap can be serialized and deserialized via Jackson.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testHashMap(Map<String, String> param, StringBuilder ret) throws Exception {
        WebTarget target = client.target(getAddress("pojo/person2/map"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        Map<String, String> pojo = response.readEntity(Map.class);

        Map<String, String> person = new HashMap<String, String>();
        person.put("name", "John Doe");
        person.put("age", "40");

        if (!person.equals(pojo.get("person"))) {
            ret.append("maps do not match");
            return;
        }

        List<String> arr = new ArrayList<String>();
        arr.add("firstArrValue");
        arr.add("secondArrValue");

        if (!arr.equals(pojo.get("arr"))) {
            ret.append("lists do not match");
            return;
        }
        if (pojo.keySet().size() != 2) {
            ret.append("expecting size to be 2, got " + pojo.keySet().size() + " instead");
            return;
        }
        ret.append("OK");
    }

    /**
     * Tests that a List can be serialized and deserialized via Jackson.
     *
     * @throws Exception
     */
    public void testList(Map<String, String> param, StringBuilder ret) throws Exception {
        WebTarget target = client.target(getAddress("pojo/person2/list"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        @SuppressWarnings("unchecked")
        List<String> listFromService = response.readEntity(List.class);

        List<String> arr = new ArrayList<String>();
        arr.add("firstArrValue");
        arr.add("secondArrValue");

        if (!arr.equals(listFromService)) {
            ret.append("lists do not match");
            return;
        }
        ret.append("OK");
    }

    public void testPOJO(Map<String, String> param, StringBuilder ret) throws Exception {
        WebTarget target = client.target(getAddress("pojo/person2/person"));
        Response response = target.request().accept(MediaType.APPLICATION_JSON).get();
        if (response.getStatus() != 200) {
            ret.append("response.getStatus() = " + response.getStatus());
            return;
        }
        Person p = response.readEntity(Person.class);

        if (!p.getName().equals("John Doe")) {
            ret.append(p.getName() + " != " + "John Doe");
            return;
        }
        if (p.getAge() != 40) {
            ret.append(p.getAge() + " != " + 40);
            return;
        }
        if (!p.randomProp().equals("randomValue")) {
            ret.append(p.randomProp() + " != randomValue");
            return;
        }
        if (!p.getManager().getManagerName().equals("Jane Smith")) {
            ret.append(p.getManager().getManagerName() + " != " + "Jane Smith");
            return;
        }
        if (p.getManager().getManagerId() != 123456789) {
            ret.append(p.getManager().getManagerId() + " != " + 123456789);
            return;
        }
        ret.append("OK");
    }

    private boolean compareJSON(String expected, String actual, StringBuilder ret) throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode exp = om.readTree(expected);
        JsonNode act = om.readTree(actual);
        if (exp.equals(act)) {
            return true;
        } else {
            ret.append("compareJSON - expected=" + expected + ", actual=" + actual);
            return false;
        }
    }
}
