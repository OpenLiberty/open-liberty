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
package com.ibm.ws.jaxrs.fat.beanvalidation;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.MessageBodyWriter;

@WebServlet("/TestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 6083257343172279063L;
    private static final String bvwar = "beanvalidation";
    private static final String PARAM_URL_PATTERN = "rest";
    private static String serverIP;
    private static String serverPort;

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

    private static String getAddress(String path) {
        return "http://" + serverIP + ":" + serverPort + "/" + bvwar + "/" + PARAM_URL_PATTERN + "/" + path;
    }

    public void testThatPatternValidationFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books/blabla");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatPatternValidationFails_Disabled(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books/blabla");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.NO_CONTENT.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatNotNullValidationFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).request().post(null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatNotNullValidationSkipped(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/booksNoValidate");
        Response resp = client.target(uri).request().post(null);
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatNotNullValidationNotSkipped(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/booksValidate");
        Response resp = client.target(uri).request().post(null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatSizeValidationFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).request().post(Entity.entity("id=", MediaType.APPLICATION_FORM_URLENCODED + "; charset=UTF-8"));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatMinValidationFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).queryParam("page", "0").request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatNoValidationConstraintsAreViolated(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books?page=2");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatNoValidationConstraintsAreViolatedWithDefaultValue(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    //TODO: Investigate why CXF JAXRS client is getting response code of 415 (UnsupportMediaType) rather than 201 (Created)
    public void testThatNoValidationConstraintsAreViolatedWithBook(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books/direct");

        BookWithValidation request = new BookWithValidation("BeanVal", "1");
        Response resp = client.target(uri).request().accept("application/xml").post(Entity.xml(request));
        assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    //TODO: Investigate why CXF JAXRS client is getting response code of 415 (UnsupportMediaType) rather than 400 (BadRequest)
    public void testThatValidationConstraintsAreViolatedWithBook(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books/direct");

        BookWithValidation request = new BookWithValidation("BeanVal");
        int code;
        try {
            WebTarget target = client.target(uri);
            Response resp = target.request().accept("application/xml").post(Entity.xml(request));
            code = resp.getStatus();
        } catch (Exception e) {
            code = -1;
        }
        assertEquals(Status.BAD_REQUEST.getStatusCode(), code);
        ret.append("OK");
    }

    //TODO: Investigate why CXF JAXRS client is getting response code of 400 (BadRequest) rather than 415 (UnsupportMediaType)
    public void testThatValidationConstraintsAreViolatedWithBooks(Map<String, String> param, StringBuilder ret) throws Exception {
        client.register(new MessageBodyWriter<List<BookWithValidation>>() {

            @Override
            public boolean isWriteable(Class<?> paramClass, Type paramType, Annotation[] paramArrayOfAnnotation, MediaType paramMediaType) {
                return true;
            }

            @Override
            public long getSize(List<BookWithValidation> paramT, Class<?> paramClass, Type paramType, Annotation[] paramArrayOfAnnotation, MediaType paramMediaType) {
                return 0;
            }

            @Override
            public void writeTo(List<BookWithValidation> paramT, Class<?> paramClass, Type paramType, Annotation[] paramArrayOfAnnotation, MediaType paramMediaType,
                                MultivaluedMap<String, Object> paramMultivaluedMap, OutputStream paramOutputStream) throws IOException, WebApplicationException {
                paramOutputStream.write("<books>".getBytes());
                for (BookWithValidation book : paramT) {
                    paramOutputStream.write(("<BookWithValidation id=\"" + book.getId() + "\"" + "name=\"" + book.getName() + "\"/>").getBytes());
                }
                paramOutputStream.write("</books>".getBytes());
            }
        });
        String uri = getAddress("bookstore/books/directmany");

        BookWithValidation request = new BookWithValidation("BeanVal");
        int code;
        try {
            // cannot map List to xml type, so keep UNSUPPORTED_MEDIA_TYPE status at this point, because get /books work fine before
            Response resp = client.target(uri).request().accept("application/xml").post(Entity.xml(Collections.singletonList(request)));
            code = resp.getStatus();
        } catch (Exception e) {
            code = -1;
        }
        assertEquals(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), code);
        ret.append("OK");
    }

    public void testThatResponseValidationForOneBookFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).request().post(Entity.entity("id=1234", MediaType.APPLICATION_FORM_URLENCODED + "; charset=UTF-8"));
        assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());

        uri = getAddress("bookstore/books/1234");
        Response cResp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), cResp.getStatus());
        ret.append("OK");
    }

    public void testThatResponseValidationForOneBookNotFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).request().post(Entity.entity("id=1234&name=cxf", MediaType.APPLICATION_FORM_URLENCODED + "; charset=UTF-8"));
        assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());

        uri = getAddress("bookstore/books/1234");
        Response cResp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), cResp.getStatus());
        ret.append("OK");
    }

    public void testThatResponseValidationForNullBookFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).request().post(Entity.entity("id=1234&name=cxf", MediaType.APPLICATION_FORM_URLENCODED + "; charset=UTF-8"));
        assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());

        uri = getAddress("bookstore/books/1235");
        Response cResp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), cResp.getStatus());
        ret.append("OK");
    }

    public void testThatResponseValidationForOneResponseBookFails(Map<String, String> param, StringBuilder ret) throws Exception {
        // Will double check why 1234 is not correct for this case later
        String uri = getAddress("bookstore/booksResponse/123");
        Response cResp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), cResp.getStatus());

        String uri2 = getAddress("bookstore/books");
        Response resp = client.target(uri2).request().post(Entity.entity("id=123", MediaType.APPLICATION_FORM_URLENCODED + "; charset=UTF-8"));
        assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());

        client = ClientBuilder.newClient();
        uri = getAddress("bookstore/booksResponse/123");
        cResp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), cResp.getStatus());
        ret.append("OK");
    }

    public void testThatResponseValidationForBookPassesWhenNoConstraintsAreDefined(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/booksResponseNoValidation/1234");
        Response cResp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), cResp.getStatus());

        String uri2 = getAddress("bookstore/books");
        Response resp = client.target(uri2).request().post(Entity.entity("id=1234", MediaType.APPLICATION_FORM_URLENCODED + "; charset=UTF-8"));
        assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());

        client = ClientBuilder.newClient();
        uri = getAddress("bookstore/booksResponseNoValidation/1234");
        cResp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), cResp.getStatus());
        ret.append("OK");
    }

    public void testThatResponseValidationForAllBooksFails(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books");
        Response resp = client.target(uri).request().post(Entity.entity("id=1234", MediaType.APPLICATION_FORM_URLENCODED + "; charset=UTF-8"));
        assertEquals(Status.CREATED.getStatusCode(), resp.getStatus());
        Response cResp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), cResp.getStatus());
        ret.append("OK");
    }

    public void testThatResponseValidationIsNotTriggeredForUnacceptableMediaType(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/books/direct");

        BookWithValidation request = new BookWithValidation("BeanVal", "1");
        int code;
        try {
            Response resp = client.target(uri).request(MediaType.APPLICATION_JSON).accept("application/xml").post(Entity.json(request));
            code = resp.getStatus();
        } catch (Exception e) {
            code = -1;
        }
        assertEquals(Status.UNSUPPORTED_MEDIA_TYPE.getStatusCode(), code);
        ret.append("OK");
    }

    public void testThatNoValidationConstraintsAreViolatedWhenBookIdIsSet(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/book?id=123");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookIdIsNotSet(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/book");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.BAD_REQUEST.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookIdIsNotSet_Disabled(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/book");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.NO_CONTENT.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExist(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/book?id=3333");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExist_Disabled(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/book?id=3333");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.NO_CONTENT.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/bookResponse?id=3333");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookDoesNotExistResponse_Disabled(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/bookResponse?id=3333");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookNameIsNotSet(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/bookResponse?id=124");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }

    public void testThatValidationConstraintsAreViolatedWhenBookNameIsNotSet_Disabled(Map<String, String> param, StringBuilder ret) throws Exception {
        String uri = getAddress("bookstore/bookResponse?id=124");
        Response resp = client.target(uri).request().get();
        assertEquals(Status.OK.getStatusCode(), resp.getStatus());
        ret.append("OK");
    }
}
