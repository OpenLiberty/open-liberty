/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.callback;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

@WebServlet("/callbackServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String CONTEXT_ROOT = "callback";
    public static final String RESUMED = "Response resumed";
    public static final String ISE = "Illegal State Exception Thrown";
    public static final String NOE = "No Exception Thrown";
    public static final String FALSE = "A method returned false";
    public static final String TRUE = "A method return true";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        String testMethod = req.getParameter("test");
        System.out.println(testMethod);

        String serverIP = req.getLocalAddr();
        String serverPort = String.valueOf(req.getLocalPort());
        String BASE_URL = "http://" + serverIP + ":" + serverPort + "/" + CONTEXT_ROOT + "/";;

        PrintWriter pw = resp.getWriter();
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }
        try {
            String ret = runTest(testMethod, BASE_URL);

            pw.write(ret.toString());
        } catch (Exception e)
        {
        }
    }

    private String runTest(String testMethod, String BASE_URL) {
        try {
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { String.class, StringBuilder.class });
            StringBuilder result = new StringBuilder();
            System.out.println(testM.getName());
            testM.invoke(this, BASE_URL, result);
            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Failure";
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        this.doGet(req, resp);
    }

    public void testargumentContainsExceptionWhenSendingIoException(String BASE_URL, StringBuilder sb) throws Exception {

        Client client = ClientBuilder.newClient();

        invokeClear(client, BASE_URL);
        invokeReset(client, BASE_URL);

        Future<Response> suspend = client.target(BASE_URL + "rest/resource/suspend").request().async().get();

        Future<Response> register = client.target(BASE_URL + "rest/resource/register?stage=0").request().async().get();
        sb.append(compareResult(register, FALSE));
        Future<Response> exception = client.target(BASE_URL + "rest/resource/exception?stage=1").request().async().get();
        Response response2 = exception.get();

        Response suspendResponse = suspend.get();
        suspendResponse.close();
        Future<Response> error = client.target(BASE_URL + "rest/resource/error").request().async().get();
        System.out.println("from testargumentContainsExceptionWhenSendingIoException: " + sb);
//        return sb.toString();

    }

    //testargumentContainsExceptionInTwoCallbackObjects
    public void testargumentContainsExceptionInTwoCallbackObjects(String BASE_URL, StringBuilder sb) throws Exception {

        Client client = ClientBuilder.newClient();

        invokeClear(client, BASE_URL);
        invokeReset(client, BASE_URL);
        Future<Response> suspend1 = client.target(BASE_URL + "rest/resource/suspend").request().async().get();

        Future<Response> register1 = client.target(BASE_URL + "rest/resource/registerobjects?stage=0").request().async().get();
        sb.append(compareResult(register1, FALSE));

        Future<Response> exception1 = client.target(BASE_URL + "rest/resource/resumechecked?stage=1").request().async().get();
        Response response1 = exception1.get();

        Response suspendResponse1 = suspend1.get();
        sb.append(intequalCompare(suspendResponse1.getStatusInfo().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),true));
//        assertEquals(suspendResponse1.getStatusInfo().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        suspendResponse1.close();

        Future<Response> error1 = client.target(BASE_URL + "rest/resource/error").request().async().get();
        sb.append(compareResult(error1, RuntimeException.class.getName()));
        error1 = client.target(BASE_URL + "rest/resource/seconderror").request().async().get();
        sb.append(compareResult(error1, RuntimeException.class.getName()));
        System.out.println("from testargumentContainsExceptionInTwoCallbackObjects: " + sb);

    }

    //testargumentContainsExceptionInTwoCallbackClasses
    public void testargumentContainsExceptionInTwoCallbackClasses(String BASE_URL, StringBuilder sb) throws Exception {

        Client client = ClientBuilder.newClient();

        invokeClear(client, BASE_URL);
        invokeReset(client, BASE_URL);

        Future<Response> suspend2 = client.target(BASE_URL + "rest/resource/suspend").request().async().get();

        Future<Response> register2 = client.target(BASE_URL + "rest/resource/registerclasses?stage=0").request().async().get();
        sb.append(compareResult(register2, FALSE));

        Future<Response> exception2 = client.target(BASE_URL + "rest/resource/resumechecked?stage=1").request().async().get();
        Response response3 = exception2.get();

        Response suspendResponse3 = suspend2.get();
        sb.append(intequalCompare(suspendResponse3.getStatusInfo().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),true));
//        assertEquals(suspendResponse3.getStatusInfo().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());

        suspendResponse3.close();

        Future<Response> error3 = client.target(BASE_URL + "rest/resource/error").request().async().get();
        sb.append(compareResult(error3, RuntimeException.class.getName()));
        error3 = client.target(BASE_URL + "rest/resource/seconderror").request().async().get();
        sb.append(compareResult(error3, RuntimeException.class.getName()));

        System.out.println("from testargumentContainsExceptionInTwoCallbackClasses: " + sb);
//        return sb.toString();

    }

//    @Test
//    public void testargumentContainsExceptionInTwoCallbackObjects() throws Exception {
//        invokeClear();
//        invokeReset();;
//        Future<Response> suspend = client.target(BASE_URL + "rest/resource/suspend").request().async().get();
//
//        Future<Response> register = client.target(BASE_URL + "rest/resource/registerobjects?stage=0").request().async().get();
//        compareResult(register, FALSE);
//
//        Future<Response> exception = client.target(BASE_URL + "rest/resource/resumechecked?stage=1").request().async().get();
//        Response response2 = exception.get();
//
//        Response suspendResponse = suspend.get();
//        assertEquals(suspendResponse.getStatusInfo().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//
//        suspendResponse.close();
//
//        Future<Response> error = client.target(BASE_URL + "rest/resource/error").request().async().get();
//        compareResult(error, RuntimeException.class.getName());
//        error = client.target(BASE_URL + "rest/resource/seconderror").request().async().get();
//        compareResult(error, RuntimeException.class.getName());
//    }

//    @Test
//    public void testargumentContainsExceptionInTwoCallbackClasses() throws Exception {
//        invokeClear();
//        invokeReset();
//
//        Future<Response> suspend = client.target(BASE_URL + "rest/resource/suspend").request().async().get();
//
//        Future<Response> register = client.target(BASE_URL + "rest/resource/registerclasses?stage=0").request().async().get();
//        compareResult(register, FALSE);
//
//        Future<Response> exception = client.target(BASE_URL + "rest/resource/resumechecked?stage=1").request().async().get();
//
//        Response suspendResponse = suspend.get();
//        assertEquals(suspendResponse.getStatusInfo().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
//
//        suspendResponse.close();
//
//        Future<Response> error = client.target(BASE_URL + "rest/resource/error").request().async().get();
//        compareResult(error, RuntimeException.class.getName());
//        error = client.target(BASE_URL + "rest/resource/seconderror").request().async().get();
//        compareResult(error, RuntimeException.class.getName());
//    }

    protected static String compareResult(Future<Response> future, String check) throws Exception
    {
        //277486 adding delay to allow CompletionCallback.onComplete() to execute
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            System.out.println("Thread.sleep threw exception: " + e.getMessage());
        }
        Response response = future.get();
        //System.out.println(response.getStatus());
        intequalCompare(200, response.getStatus(),false);
//        assertEquals(200, response.getStatus());

        String entity = response.readEntity(String.class);
        // System.out.println(response.readEntity(String.class));
//        assertEquals(check, entity);
        if (check.compareToIgnoreCase(entity) != 0)
        {
            System.out.println("failure: check = " + check + ": entity = " + entity);
            return "failure: check = " + check + ": entity = " + entity;
        }
        else
        {
            System.out.println("success");
            return "success";
        }
    }

    protected String invokeClear(Client client, String BASE_URL)
    {
        Response response = client.target(BASE_URL + "rest/resource/clear").request().get();
        int code = response.getStatus();
        response.close();
        if (204 != code)
        {
            return "failure: code = " + code;
        }
        else
        {
            return "success";
        }
        //assertEquals(204, response.getStatus());

    }

    protected String invokeReset(Client client, String BASE_URL)
    {
        Response response = client.target(BASE_URL + "rest/resource/reset").request().get();
        int code = response.getStatus();
        response.close();
        if (204 != code)
        {
            return "failure: code = " + code;
        }
        else
        {
            return "success";
        }
//        assertEquals(204, response.getStatus());
//        response.close();
    }

    private static String intequalCompare(int a, int b, boolean delay)
    {
        //282545 Delay to allow CompletionCallback.onComplete() to execute
        if (delay) {
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Thread.sleep threw exception: " + e.getMessage());
            }
        }

        System.out.println(a);
        System.out.println(b);
        if (a != b)
        {
            System.out.println("failure: a = " + a + ": b = " + b);
            return "failure: a = " + a + ": b = " + b;
        }
        else
        {
            System.out.println("success: a = " + a + ": b = " + b);
            return "success";
        }
    }

    private static String StringqualCompare(String a, String b)

    {
        System.out.println(a);
        System.out.println(b);
        if (a.compareToIgnoreCase(b) != 0)
        {
            return "failure: a = " + a + ": b = " + b;
        }
        else
        {
            return "success";
        }
    }
}
