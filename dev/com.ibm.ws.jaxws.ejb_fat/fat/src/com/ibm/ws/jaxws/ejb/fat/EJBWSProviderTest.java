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
package com.ibm.ws.jaxws.ejb.fat;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 *
 */
@RunWith(FATRunner.class)
public class EJBWSProviderTest {

    @Server("com.ibm.ws.jaxws.ejb.fat.ejbwsprovider")
    public static LibertyServer server;

    private static final String ejbwsproviderjar = "EJBWSProvider";
    private static final String ejbwsproviderear = "EJBWSProvider";

    private static String ENDPOINT_URL = "/EJBWSProvider/UserQueryService";

    private static final QName SERVICE_NAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "UserQueryService");

    private static final QName PORT_NAME = new QName("http://ejbbasic.jaxws.ws.ibm.com/", "UserQueryPort");

    public static final String GET_USER_REQUEST_MESSAGE = "<?xml version=\"1.0\"?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                                                          + "<S:Body><p:getUser xmlns:p=\"http://ejbbasic.jaxws.ws.ibm.com/\" >"
                                                          + "<arg0>Arugal</arg0></p:getUser>"
                                                          + "</S:Body></S:Envelope>";

    public static final String GET_USER_REQUEST_NOT_FOUND_EXCEPTION_MESSAGE_BODY = "<p:getUser xmlns:p=\"http://ejbbasic.jaxws.ws.ibm.com/\">"
                                                                                   + "<arg0>none</arg0></p:getUser>";

    public static final String LIST_USERS_MESSAGE = "<?xml version=\"1.0\"?><S:Envelope xmlns:S=\"http://schemas.xmlsoap.org/soap/envelope/\"><S:Body><p:listUsers xmlns:p=\"http://ejbbasic.jaxws.ws.ibm.com/\"/>"
                                                    + "</S:Body></S:Envelope>";

    private static long MAX_ASYNC_WAIT_TIME = 30 * 1000;

    @BeforeClass
    public static void beforeAllTests() throws Exception {

        JavaArchive jar = ShrinkHelper.buildJavaArchive(ejbwsproviderjar + ".jar", "com.ibm.ws.jaxws.ejbwsprovider.*");

        ShrinkHelper.addDirectory(jar, "test-applications/EJBWSProvider/resources/");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, ejbwsproviderear + ".ear").addAsModule(jar);

        ShrinkHelper.exportDropinAppToServer(server, ear);

        try {
            server.startServer();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        Assert.assertNotNull("The application EJBWSProvider did not appear to have started",
                             server.waitForStringInLog("CWWKZ0001I.*EJBWSProvider"));
        ENDPOINT_URL = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/EJBWSProvider/UserQueryService";
    }

    @AfterClass
    public static void afterAllTests() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /*
     * This test is a basic invocation of a Dynamic Service that uses the @WebServiceProvider annotation
     * by invoking the WSP with a dynamic dispatch client.
     * TODO: Re-factor the SOAPAction tests into a separate Test Class.
     */
    @Test
    public void testQueryUserProvider() throws Exception {
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, ENDPOINT_URL);

        Dispatch<SOAPMessage> dispatch = service.createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE, new AddressingFeature());
        dispatch.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasic.jaxws.ws.ibm.com/UserQuery#getUser");

        SOAPMessage requestSOAPMessage = createGetUserSOAPMessage();

        SOAPMessage responseSOAPMessage = dispatch.invoke(requestSOAPMessage);

        assertQueryUserResponse(responseSOAPMessage);
    }

    /*
     * This test makes sure that with the fix, a dynamic client cannot still invoke a WSP based service even when the
     * SOAPAction header mismatches the expected value since the allowNonMatchingToDefaultSoapAction property isn't set
     * TODO: Re-factor the SOAPAction tests into a separate Test Class.
     */
    @Test
    public void testQueryUserProviderWithSOAPActionMismatch() throws Exception {
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, ENDPOINT_URL);

        Dispatch<SOAPMessage> dispatch = service.createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE, new AddressingFeature());

        dispatch.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasim/UserQuery#getUserMismatch");

        dispatch.getRequestContext().put("allowNonMatchingToDefaultSoapAction", "false");
        SOAPMessage requestSOAPMessage = createGetUserSOAPMessage();

        SOAPMessage responseSOAPMessage = dispatch.invoke(requestSOAPMessage);

        assertQueryUserResponse(responseSOAPMessage);
    }

    @Mode(TestMode.FULL)
    @Test
    @SkipForRepeat({ "EE9_FEATURES", "jaxws-2.3" })
    public void testUserNotFoundExceptionProvider() throws Exception {
        Service service = Service.create(new URL(ENDPOINT_URL + "?wsdl"), SERVICE_NAME);
        Dispatch<Source> dispatch = service.createDispatch(PORT_NAME, Source.class, Service.Mode.PAYLOAD, new AddressingFeature());

        dispatch.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasim/UserQuery#getUser");

        Source response = dispatch.invoke(createGetUserNotFoundExceptionPayloadSource());

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        DOMResult result = new DOMResult();
        transformer.transform(response, result);
        Node responseNode = ((Document) result.getNode()).getDocumentElement();

        Assert.assertEquals("Expected element UserNotFoundException is not found", "UserNotFoundException",
                            responseNode.getLocalName());

        Node userNameNode = null;
        NodeList nodeList = responseNode.getChildNodes();
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node node = nodeList.item(i);
            if ("userName".equals(node.getLocalName())) {
                userNameNode = node;
                break;
            }
        }

        Assert.assertNotNull("Expected userName node is not found", userNameNode);
        Assert.assertEquals("Expected user name none is not found", "none", userNameNode.getTextContent());
    };

    @Mode(TestMode.FULL)
    @Test
    public void testListUsersProvider() throws Exception {
        SOAPConnection soapConn = SOAPConnectionFactory.newInstance().createConnection();

        SOAPMessage responseSOAPMessage = soapConn.call(createListUsersSOAPMessage(), ENDPOINT_URL);

        SOAPBody responseSOAPBody = responseSOAPMessage.getSOAPBody();

        SOAPElement listUsersResponseElement = findChildElement(responseSOAPBody, "listUsersResponse");
        Assert.assertNotNull("Unable to find the expected listUsersResponse element in the response SOAP message", listUsersResponseElement);

        List<SOAPElement> userElements = findChildElements(listUsersResponseElement, "return");
        Assert.assertEquals("Three users should be returned, while only " + userElements.size() + " is found", 3, userElements.size());
    }

    @Test
    public void testQueryUserProviderAsyncResponse() throws Exception {
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, ENDPOINT_URL);

        Dispatch<SOAPMessage> dispatch = service.createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE, new AddressingFeature());

        dispatch.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasim/UserQuery#getUser");
        SOAPMessage requestSOAPMessage = createGetUserSOAPMessage();

        Response<SOAPMessage> response = dispatch.invokeAsync(requestSOAPMessage);

        long curWaitTime = 0;
        Object lock = new Object();

        while (!response.isDone() && curWaitTime < MAX_ASYNC_WAIT_TIME) {
            synchronized (lock) {
                try {
                    lock.wait(50L);
                } catch (InterruptedException e) {
                }
            }
            curWaitTime += 50;
        }

        if (!response.isDone()) {
            Assert.fail("Response is not received after waiting " + MAX_ASYNC_WAIT_TIME);
        }

        SOAPMessage responseSOAPMessage = response.get();
        assertQueryUserResponse(responseSOAPMessage);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testQueryUserProviderAsyncHandler() throws Exception {
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, ENDPOINT_URL);
        Dispatch<SOAPMessage> dispatch = service.createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE, new AddressingFeature());

        dispatch.getRequestContext().put(BindingProvider.SOAPACTION_URI_PROPERTY, "http://ejbbasim/UserQuery#getUser");

        dispatch.getRequestContext().put("allowNonMatchingToDefaultSoapAction", "true");
        SOAPMessage requestSOAPMessage = createGetUserSOAPMessage();

        AsyncHandlerImpl asyncHandlerImpl = new AsyncHandlerImpl();
        Future<?> future = dispatch.invokeAsync(requestSOAPMessage, asyncHandlerImpl);

        long curWaitTime = 0;
        Object lock = new Object();

        while (!future.isDone() && curWaitTime < MAX_ASYNC_WAIT_TIME) {
            synchronized (lock) {
                try {
                    lock.wait(50L);
                } catch (InterruptedException e) {
                }
            }
            curWaitTime += 50;
        }

        if (!asyncHandlerImpl.done) {
            Assert.fail("Response is not received after waiting " + MAX_ASYNC_WAIT_TIME);
        }

        if (!asyncHandlerImpl.successed) {
            Assert.fail("Response validation failed");
        }
    }

    private SOAPElement findChildElement(SOAPElement parentElement, String localName) {
        for (Iterator<javax.xml.soap.Node> it = parentElement.getChildElements(); it.hasNext();) {
            SOAPElement soapElement = (SOAPElement) it.next();
            if (localName.equals(soapElement.getLocalName())) {
                return soapElement;
            }
        }
        return null;
    }

    private List<SOAPElement> findChildElements(SOAPElement parentElement, String localName) {
        List<SOAPElement> childElements = new ArrayList<SOAPElement>(3);
        for (Iterator<javax.xml.soap.Node> it = parentElement.getChildElements(); it.hasNext();) {
            SOAPElement soapElement = (SOAPElement) it.next();
            if (localName.equals(soapElement.getLocalName())) {
                childElements.add(soapElement);
            }
        }
        return childElements;
    }

    private SOAPMessage createGetUserSOAPMessage() throws SOAPException {
        SOAPMessage requestSoapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();
        requestSoapMessage.getSOAPPart().setContent(new StreamSource(new StringReader(GET_USER_REQUEST_MESSAGE)));
        requestSoapMessage.saveChanges();
        return requestSoapMessage;
    }

    private Source createGetUserNotFoundExceptionPayloadSource() {
        return new StreamSource(new StringReader(GET_USER_REQUEST_NOT_FOUND_EXCEPTION_MESSAGE_BODY));
    }

    private SOAPMessage createListUsersSOAPMessage() throws SOAPException {
        SOAPMessage requestSoapMessage = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL).createMessage();

        requestSoapMessage.setProperty("allowNonMatchingToDefaultSoapAction", "true");
        requestSoapMessage.setProperty(ENDPOINT_URL, requestSoapMessage);
        requestSoapMessage.getSOAPPart().setContent(new StreamSource(new StringReader(LIST_USERS_MESSAGE)));
        requestSoapMessage.saveChanges();
        return requestSoapMessage;
    }

    private void assertQueryUserResponse(SOAPMessage responseSOAPMessage) throws SOAPException {

        SOAPBody responseSOAPBody = responseSOAPMessage.getSOAPBody();

        SOAPElement getUserResponseElement = findChildElement(responseSOAPBody, "getUserResponse");
        Assert.assertNotNull("Unable to find the expected getUserResponse element in the response SOAP message", getUserResponseElement);

        SOAPElement returnElement = findChildElement(getUserResponseElement, "return");
        Assert.assertNotNull("Unable to find the expected return element in the response SOAP message", returnElement);

        SOAPElement nameElement = findChildElement(returnElement, "name");
        Assert.assertNotNull("Unable to find the expected name element in the response SOAP message", nameElement);
        Assert.assertEquals("Expected user name Arugal is not found in the response message , but " + nameElement.getTextContent() + " is found", "Arugal",
                            nameElement.getTextContent());
    }

    class AsyncHandlerImpl implements AsyncHandler<SOAPMessage> {

        public volatile boolean done = false;;

        public volatile boolean successed = false;

        @Override
        public void handleResponse(Response<SOAPMessage> arg0) {
            try {
                assertQueryUserResponse(arg0.get());
                successed = true;
            } catch (Exception e) {
                successed = false;
            } finally {
                done = true;
            }
        }
    }
}
