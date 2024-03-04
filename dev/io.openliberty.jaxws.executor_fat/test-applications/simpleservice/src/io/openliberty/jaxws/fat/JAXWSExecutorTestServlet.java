/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jaxws.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;
import javax.xml.ws.Service;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * These tests cover both managed and unmanaged synch/async calls to the SimpleWebService
 * The web service has a single hello world style method that the client will invoke with the method name
 * The Web Service Impl will return a string with this format:
 *
 * "Hello <METHOD NAME>!"
 *
 *
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JAXWSExecutorTestServlet")
public class JAXWSExecutorTestServlet extends FATServlet {

    private static final long TIMEOUT_NS = TimeUnit.MINUTES.toNanos(1);

    @Resource(name = "java:comp/env/concurrent/scheduledExecutorRef", lookup = "concurrent/scheduledExecutor")
    private ManagedScheduledExecutorService scheduledExecutor;

    private static final String thisClass = "SimpleDispatchTestServlet";
    private static final Logger LOG = Logger.getLogger("JAXWSExecutorTestServlet");

    private static final String SERVICE_NS = "http://fat.jaxws.openliberty.io";

    private static URL wsdlURL;
    private static String serviceClientUrl = "";

    private static QName serviceName = new QName(SERVICE_NS, "SimpleWebService");
    private static QName portName = new QName(SERVICE_NS, "SimpleWebServicePort");

    private static Service service;
    private static SimpleWebService proxy;

    private static String actualResponse = "";

    private static String method = "";

    private static String expectedResponse;

    private Exception handlerException;
    private boolean asyncHandlerCalled;

    // Construct a single instance of the service client
    static {
        try {
            wsdlURL = new URL(new StringBuilder().append("http://localhost:")
                            .append(Integer.getInteger("bvt.prop.HTTP_default"))
                            .append("/simpleservice/SimpleWebService?wsdl")
                            .toString());

            service = Service.create(wsdlURL, serviceName);

            proxy = service.getPort(portName, SimpleWebService.class);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    // Util method to prevent dup asserting the response across test methods
    private void assertResponse(String method, String actualResponse, String expectedResponse) {
        assertTrue("The " + method + " test failed: the actual response - " + actualResponse + " did not equal the expected response - " + expectedResponse,
                   actualResponse.equals(expectedResponse));
    }

    // Util method to prevent dup asserting the queue across test methods
    private void assertQueue(Object actualResponse, ConcurrentLinkedQueue<Object> result) throws Exception {
        // Check result queue first value has the response
        Object r = result.poll();
        assertNotNull(r);
        if (r instanceof Throwable)
            throw new Exception("Callback received failure. See cause.", (Throwable) r);
        assertEquals(actualResponse, r);

        // Check result queue second value was set by a thread with the correct thread name
        assertNotNull(r = result.poll());
        String callbackThreadName = (String) r;
        assertTrue(callbackThreadName, callbackThreadName.startsWith("Default Executor-thread-"));

        // Check result queue third value has an instance of the ScheduledExecutorService
        assertNotNull(r = result.poll());
        if (r instanceof ScheduledExecutorService)
            ; // test passes, servlet's java:comp should be available to callback thread
        else if (r instanceof Throwable)
            throw new Exception("Unexpected failure for InvocationCallback lookup attempt. See cause.", (Throwable) r);
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service without a managed executor
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl and asserts that the response
     * matches: "Hello testSimpleNonManagedClient!"
     *
     */
    @Test
    public void testSimpleNonManagedClient() throws Exception {

        method = "testSimpleNonManagedClient";

        setExpectedResponse(method);

        actualResponse = proxy.simpleHello(method);
        assertResponse(method, actualResponse, expectedResponse);

    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service without a managed executor
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.simpleHelloAsync(String) method. This method
     * returns a javax.xml.ws.Response object.
     *
     * We wait for the response to finish, and once finished, we assert that the response
     * matches the expected: "Hello testAsyncClientWithResponse!"
     *
     */
    @Test
    public void testAsyncClientWithResponse() throws Exception {

        method = "testAsyncClientWithResponse";

        setExpectedResponse(method);

        // Get Async Response Object
        Response response = proxy.simpleHelloAsync(method);

        while (!response.isDone()) {
            // Wait for response to finish
        }

        // Get the returned string from the Web Service endpoint
        actualResponse = (String) response.get();

        assertResponse(method, actualResponse, expectedResponse);
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service without a managed executor
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.simpleHelloAsync(String, AsyncHandler) method. This method
     * returns a java.util.concurrent.Future object. Which we can then call future.get() on to invoke the AsyncHandler code.
     *
     * Once future.get() finishes, we assert that the response
     * matches the expected: "Hello testAsyncClientWithAsyncHandler!"
     *
     */
    @Test
    public void testAsyncClientWithAsyncHandler() throws Exception {

        method = "testAsyncClientWithAsyncHandler";

        setExpectedResponse(method);

        // Instantiate a AsyncHandler object
        AsyncHandler handler = new AsyncHandler() {
            @Override
            public void handleResponse(Response response) {
                try {
                    // Get the response from the web service
                    actualResponse = (String) response.get(1000, TimeUnit.MILLISECONDS);

                    // do the assert in the handler code
                    assertResponse(method, actualResponse, expectedResponse);
                    // set the asyncHandlerCalled boolean to true
                    asyncHandlerCalled = true;
                } catch (Exception ex) {
                    // catch any exception
                    handlerException = ex;
                }
            }
        };

        // Obtain the Future object by invoking the Async method on the Web Service
        Future future = proxy.simpleHelloAsync(method, handler);
        future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS); // Set the timeout on the get method

        if (handlerException != null)
            throw handlerException; // Throw an exception if the handler caught an exception
        assertTrue("Async handler called", asyncHandlerCalled); // Make sure handler is actually called.

    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service with a managed executor
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.simpleHelloA(String, AsyncHandler) method.
     * Since this is a synchronous method it simple returns a string.
     *
     * We have to write a new Callable<String> object that the Executor can submit
     *
     * This callback simply invokes the web service implementation
     *
     * We use the Client to set the Executor and the obtain the Excutuor from the client to submit the callback
     * That returns a Future object, that we can then call future.get() on web service.
     *
     * Once future.get() finishes, we assert that the response
     * matches the expected: "Hello testClientSubmitCallViaManagedExecutor!"
     *
     */
    @Test
    public void testClientSubmitCallViaManagedExecutor() throws Exception {

        method = "testClientSubmitCallViaManagedExecutor";

        setExpectedResponse(method);
        // Prove that the resource is available to the servlet thread
        Object scheduledExecutorRef = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
        assertNotNull(scheduledExecutorRef);
        LOG.info(method + " scheduledExecutorRef=" + scheduledExecutorRef);
        LOG.info(method + " scheduledExecutor=" + scheduledExecutor);

        // Create a new JAX-WS service object and setExecutor with our ManagedScheduledExecutorService
        Service scheduledServiceClient = service;
        scheduledServiceClient.setExecutor(scheduledExecutor);

        // Build a new proxy object with the scheduledServiceclient
        SimpleWebService scheduledServiceClientProxy = scheduledServiceClient.getPort(portName, SimpleWebService.class);

        // Instantiate the simpleHelloCall CallBack.
        Callable<String> simpleHelloCall = new Callable<String>() {

            @Override
            public String call() throws Exception {
                // Simply invoke the non-async Web Service
                return scheduledServiceClientProxy.simpleHello(method);
            }
        };

        // Use the Excecutor set on the client to submit the simpleHelloCall CallBack
        Future<String> future = ((ManagedScheduledExecutorService) scheduledServiceClient.getExecutor()).submit(simpleHelloCall);

        // Get the String response from the Web Service Impl
        actualResponse = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // Assert that the response is what we expect
        assertResponse(method, actualResponse, expectedResponse);

    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service with a managed executor
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.simpleHelloA(String, AsyncHandler) method.
     * Since this is a synchronous method it simple returns a string.
     *
     * We have to write a new Callable<String> object that the Executor can submit
     *
     * This callback simply invokes the web service implementation
     *
     * We use the Client to set the Executor and the obtain the Excutuor from the client to submit the callback
     * That returns a Future object, that we can then call future.get() on web service.
     *
     * We also use a ConcurrentLinkedQueue to store details from the thread to make sure the thread has the context we except
     * These details verify that the thread name is what CXF should set it to when a Executor is supplied, and that the
     * thread can do a JNDI look up since it's on a managed thread in the Java EE environment
     *
     * Once future.get() finishes, we assert that the response
     * matches the expected: "Hello testClientSubmitCallViaManagedExecutorThreadAndInstance!"
     *
     */
    @Test
    public void testClientSubmitCallViaManagedExecutorThreadAndInstance() throws Exception {

        method = "testClientSubmitCallViaManagedExecutorThreadAndInstance";

        setExpectedResponse(method);
        // Prove that the resource is available to the servlet thread
        Object scheduledExecutorRef = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
        assertNotNull(scheduledExecutorRef);
        LOG.info(method + " scheduledExecutorRef=" + scheduledExecutorRef);
        LOG.info(method + " scheduledExecutor=" + scheduledExecutor);

        // Create a new JAX-WS service object and setExecutor with our ManagedScheduledExecutorService
        Service scheduledServiceClient = service;
        scheduledServiceClient.setExecutor(scheduledExecutor);

        // Build a new proxy object with the scheduledServiceclient
        SimpleWebService scheduledServiceClientProxy = scheduledServiceClient.getPort(portName, SimpleWebService.class);

        // Build a ConcurrentLinkedQueue that will store the response from the Web Service, the Thread Name, and the scheduledExecutorRef after lookup
        final ConcurrentLinkedQueue<Object> result = new ConcurrentLinkedQueue<Object>();

        // Instantiate the simpleHelloCall CallBack
        Callable<String> simpleHelloCall = new Callable<String>() {
            @Override
            public String call() throws Exception {
                // the response from the Web Service
                String response = scheduledServiceClientProxy.simpleHello(method);
                LOG.info(method + " Response from inside simpleHelloCall: " + response);
                // Add response to the Queue
                result.add(response);
                // Get the thread name
                String threadName = Thread.currentThread().getName();
                LOG.info(method + " Thread name inside InvocationCallback: " + threadName);
                // Add threadName to the Queue
                result.add(threadName);
                try {
                    result.add(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));
                } catch (NamingException x) {
                    result.add(x);
                }
                return response;
            }
        };

        // Use the Excecutor set on the client to submit the simpleHelloCall CallBack
        Future<String> future = ((ManagedScheduledExecutorService) scheduledServiceClient.getExecutor()).submit(simpleHelloCall);

        // Get the String response from the Web Service Impl
        actualResponse = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // Assert that the response is what we expect
        assertResponse(method, actualResponse, expectedResponse);

        // Assert that the queue contains what we expect
        assertQueue(actualResponse, result);

    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service with a managed executor
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.simpleHelloAsync(String, AsyncHandler) method.
     * Since this is a async method it returns a javax.xml.ws.Response object
     *
     * We have to write a new Callable<Response> object that the Executor can submit
     *
     * This callback simply invokes the web service implementation and returns the Response object
     *
     * We use the Client to set the Executor and the obtain the Executuor from the client to submit the callback
     * That returns a Future object, that we can then call future.get() on web service.
     *
     * We also use a ConcurrentLinkedQueue to store details from the thread to make sure the thread has the context we except
     * These details verify that the thread name is what CXF should set it to when a Executor is supplied, and that the
     * thread can do a JNDI look up since it's on a managed thread in the Java EE environment
     *
     * We wait for the response to finish, and once finished, we assert that the response
     * matches the expected: "Hello testAsyncClientWithResponse!"
     *
     */
    @Test
    public void testAsyncClientSubmitCallViaManagedExecutorThreadAndInstance() throws Exception {

        method = "testAsyncClientSubmitCallViaManagedExecutorThreadAndInstance";

        setExpectedResponse(method);
        // Prove that the resource is available to the servlet thread
        Object scheduledExecutorRef = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
        assertNotNull(scheduledExecutorRef);
        LOG.info(method + " scheduledExecutorRef=" + scheduledExecutorRef);
        LOG.info(method + " scheduledExecutor=" + scheduledExecutor);

        // Create a new JAX-WS service object and setExecutor with our ManagedScheduledExecutorService
        Service scheduledServiceClient = service;
        scheduledServiceClient.setExecutor(scheduledExecutor);

        // Build a new proxy object with the scheduledServiceclient
        SimpleWebService scheduledServiceClientProxy = scheduledServiceClient.getPort(portName, SimpleWebService.class);

        // Build a ConcurrentLinkedQueue that will store the response from the Web Service, the Thread Name, and the scheduledExecutorRef after lookup
        final ConcurrentLinkedQueue<Object> result = new ConcurrentLinkedQueue<Object>();

        // Instantiate the simpleAsyncHelloCall CallBack
        Callable<Response> simpleAsyncHelloCall = new Callable<Response>() {
            @Override
            public Response call() throws Exception {
                // Get the async response from the Web Service
                Response response = scheduledServiceClientProxy.simpleHelloAsync(method);
                LOG.info(method + " Response from inside simpleHelloCall: " + response);
                // Add response to the Queue
                result.add(response);
                // Get the thread name
                String threadName = Thread.currentThread().getName();
                LOG.info(method + " Thread name inside InvocationCallback: " + threadName);
                // Add threadName to the Queue
                result.add(threadName);
                try {
                    // Store the JNDI lookup of the scheduledExecutorRef
                    result.add(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));
                } catch (NamingException x) {
                    result.add(x);
                }
                return response;
            }
        };

        // Use the Excecutor set on the client to submit the simpleHelloCall CallBack
        Future<Response> future = ((ManagedScheduledExecutorService) scheduledServiceClient.getExecutor()).submit(simpleAsyncHelloCall);

        // Get the async Response object from the Web Service Impl
        Response response = future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        while (!response.isDone()) {
            // Wait for response to finish
        }

        // Get the string respone from the Web Service async Response object
        actualResponse = (String) response.get();

        // Assert that the response is what we expect
        assertResponse(method, actualResponse, expectedResponse);

        // Assert that the queue contains what we expect
        assertQueue(response, result);

    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service with a managed executor. It wraps the AnsyncHandler object
     * in a CallBack Object
     *
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.simpleHelloAsync(String, AsyncHandler) method.
     * Since this is a async method it returns a javax.xml.ws.Response object
     *
     * We have to write a new Callable<Response> object that the Executor can submit
     *
     * This callback simply invokes the web service implementation and returns the Response object
     *
     * We use the Client to set the Executor and the obtain the Executuor from the client to submit the callback
     * That returns a Future object, that we can then call future.get() on web service.
     *
     * We also use a ConcurrentLinkedQueue to store details from the thread to make sure the thread has the context we except
     * These details verify that the thread name is what CXF should set it to when a Executor is supplied, and that the
     * thread can do a JNDI look up since it's on a managed thread in the Java EE environment
     *
     * We wait for the response to finish, and once finished, we assert that the response
     * matches the expected: "Hello testAsyncClientWithResponse!"
     *
     */
    @Test
    public void testAsyncClientWithAsyncHandlerViaManagedExecutorWrappedCallThreadAndInstance() throws Exception {

        method = "testAsyncClientWithAsyncHandlerViaManagedExecutorWrappedCallThreadAndInstance";

        setExpectedResponse(method);
        // Prove that the resource is available to the servlet thread
        Object scheduledExecutorRef = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
        assertNotNull(scheduledExecutorRef);
        LOG.info(method + " scheduledExecutorRef=" + scheduledExecutorRef);
        LOG.info(method + " scheduledExecutor=" + scheduledExecutor);

        // Create a new JAX-WS service object and setExecutor with our ManagedScheduledExecutorService
        Service scheduledServiceClient = service;
        scheduledServiceClient.setExecutor(scheduledExecutor);

        // Build a new proxy object with the scheduledServiceclient
        SimpleWebService scheduledServiceClientProxy = scheduledServiceClient.getPort(portName, SimpleWebService.class);

        // Instantiate a AsyncHandler object
        AsyncHandler handler = new AsyncHandler() {
            @Override
            public void handleResponse(Response response) {
                try {
                    // Get the response from the web service
                    actualResponse = (String) response.get(1000, TimeUnit.MILLISECONDS);

                    // Assert that the response is what we expect
                    assertResponse(method, actualResponse, expectedResponse);
                    // set the asyncHandlerCalled boolean to true
                    asyncHandlerCalled = true;
                } catch (Exception ex) {
                    // catch any exception
                    handlerException = ex;
                }
            }
        };

        // Build a ConcurrentLinkedQueue that will store the response from the Web Service, the Thread Name, and the scheduledExecutorRef after lookup
        final ConcurrentLinkedQueue<Object> result = new ConcurrentLinkedQueue<Object>();

        // Instantiate the simpleAsyncHelloCall CallBack to onboke the AsyncHandler object defined aboved
        Callable<Future> simpleHelloAsyncCall = new Callable<Future>() {

            @Override
            public Future call() throws Exception {
                // TODO Auto-generated method stub
                Future response = scheduledServiceClientProxy.simpleHelloAsync(method, handler);
                LOG.info(method + " Response from inside simpleHelloAsyncCall: " + response.get());
                result.add(response);
                String threadName = Thread.currentThread().getName();
                LOG.info(method + " Thread name inside InvocationCallback: " + threadName);
                result.add(threadName);
                try {
                    result.add(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));
                } catch (NamingException x) {
                    result.add(x);
                }
                return response;
            }
        };

        // Use the Excecutor set on the client to submit the simpleHelloCall CallBack
        Future future = ((ManagedScheduledExecutorService) scheduledServiceClient.getExecutor()).submit(simpleHelloAsyncCall);
        // Get the Asyc response object from Future.get()
        Response response = (Response) future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // Assert that the queue contains what we expect
        assertQueue(response, result);
    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service without a managed executor. It
     *
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.simpleHelloAsync(String, AsyncHandler) method.
     * Since this is a async method it returns a javax.xml.ws.Response object
     *
     * We have to write a new Callable<Response> object that the Executor can submit
     *
     * This callback simply invokes the web service implementation and returns the Response object
     *
     *
     * We also use a ConcurrentLinkedQueue to store details from the thread to make sure the thread has the context we except
     * These details verify that the thread name is what CXF should set it to when a Executor is supplied, and that the
     * thread can do a JNDI look up since it's on a managed thread in the Java EE environment
     *
     * Because we don't use a Exccutor to invoke a CallBack, the thread is no longer managed, and so the JNDI look up should
     * throw a NamingException.
     *
     * We wait for the response to finish, and once finished, we assert that the response
     * matches the expected: "Hello testAsyncClientWithResponse!"
     *
     */
    @Test
    public void testAsyncClientWithAsyncHandlerWithoutManagedExecutor() throws Exception {

        method = "testAsyncClientWithAsyncHandlerViaManagedExecutorThreadAndInstance";

        setExpectedResponse(method);
        // Prove that the resource is available to the servlet thread
        Object scheduledExecutorRef = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
        assertNotNull(scheduledExecutorRef);
        LOG.info(method + " scheduledExecutorRef=" + scheduledExecutorRef);
        LOG.info(method + " scheduledExecutor=" + scheduledExecutor);

        // Create a new JAX-WS service object and setExecutor with our ManagedScheduledExecutorService
        Service scheduledServiceClient = service;
        scheduledServiceClient.setExecutor(scheduledExecutor);

        // Build a new proxy object with the scheduledServiceclient
        SimpleWebService scheduledServiceClientProxy = scheduledServiceClient.getPort(portName, SimpleWebService.class);

        // Build a ConcurrentLinkedQueue that will store the response from the Web Service, the Thread Name, and the scheduledExecutorRef after lookup
        final ConcurrentLinkedQueue<Object> result = new ConcurrentLinkedQueue<Object>();

        AsyncHandler handler = new AsyncHandler() {
            @Override
            public void handleResponse(Response response) {
                try {

                    // Get the response from the web service
                    actualResponse = (String) response.get(1000, TimeUnit.MILLISECONDS);

                    LOG.info(method + " Response from inside simpleHelloCall: " + actualResponse);

                    // Add response to the Queue
                    result.add(actualResponse);

                    // Assert that the response is what we expect
                    assertResponse(method, actualResponse, expectedResponse);

                    String threadName = Thread.currentThread().getName();
                    LOG.info(method + " Thread name inside InvocationCallback: " + threadName);

                    // Store the thread name
                    result.add(threadName);
                    try {
                        // Store the scheduledExecutorRef
                        result.add(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));
                    } catch (NamingException x) {
                        result.add(x);
                    }
                    asyncHandlerCalled = true;
                } catch (Exception ex) {
                    handlerException = ex;
                }
            }
        };

        // Get the future object from the async call
        Future future = proxy.simpleHelloAsync(method, handler);
        // Parse the response from the Web Service Impl
        String response = (String) future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        if (handlerException != null)
            throw handlerException; // Throw an exception if the handler caught an exception
        assertTrue("Async handler called", asyncHandlerCalled); // Make sure handler is actually called.

        // Check result queue first value has the response
        Object r = result.poll();
        assertNotNull(r);
        if (r instanceof Throwable)
            throw new Exception("Callback received failure. See cause.", (Throwable) r);
        assertEquals(response, actualResponse);

        // Check result queue second value was set by a thread with the correct thread name
        assertNotNull(r = result.poll());
        String callbackThreadName = (String) r;
        assertTrue(callbackThreadName, callbackThreadName.startsWith("Default Executor-thread-"));

        // Check result queue third value has javax.naming.NameingException
        assertNotNull(r = result.poll());
        if (r instanceof Throwable)
            ; // test passes, servlet's java:comp should be available to callback thread
        else if (r instanceof ScheduledExecutorService)
            fail("The test was successfully look-up the ScheduledExecutorService, which it shouldn't be able to do");

    }

    /**
     * TestDescription:
     *
     * This test invokes a simple jax-ws cxf web service with a managed executor
     * Invokes the io.openliberty.jaxws.fat.SimpleWebServiceImpl.oneWaySimplHello(String) method.
     * Since this is a @OneWay method it returns no response.
     *
     * Since there is nothing returned from the Web Service Impl, We have to write a new Runnable object that the Executor can submit.
     *
     * This callback simply invokes the web service implementation.
     *
     * We also use a ConcurrentLinkedQueue to store details from the thread to make sure the thread has the context we except
     * These details verify that the thread name is what CXF should set it to when a Executor is supplied, and that the
     * thread can do a JNDI look up since it's on a managed thread in the Java EE environment
     *
     * Since this is a OneWay request, there is no response to validate.
     *
     */
    @Test
    public void testOneWayClientSubmitCallViaManagedExecutor() throws Exception {

        method = "testClientSubmitCallViaManagedExecutor";

        setExpectedResponse(method);
        // Prove that the resource is available to the servlet thread
        Object scheduledExecutorRef = new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef");
        assertNotNull(scheduledExecutorRef);
        LOG.info(method + " scheduledExecutorRef=" + scheduledExecutorRef);
        LOG.info(method + " scheduledExecutor=" + scheduledExecutor);

        // Create a new JAX-WS service object and setExecutor with our ManagedScheduledExecutorService
        Service scheduledServiceClient = service;
        scheduledServiceClient.setExecutor(scheduledExecutor);

        // Build a new proxy object with the scheduledServiceclient
        SimpleWebService scheduledServiceClientProxy = scheduledServiceClient.getPort(portName, SimpleWebService.class);

        // Build a ConcurrentLinkedQueue that will store the Thread Name, and the scheduledExecutorRef after lookup
        final ConcurrentLinkedQueue<Object> result = new ConcurrentLinkedQueue<Object>();

        // Instantiate the simpleHelloCall CallBack.
        Runnable simpleHelloCall = new Runnable() {
            @Override
            public void run() {
                // Simply invoke the non-async Web Service
                scheduledServiceClientProxy.oneWaySimpleHello(method);

                // Get the thread name
                String threadName = Thread.currentThread().getName();
                LOG.info(method + " Thread name inside InvocationCallback: " + threadName);
                // Add threadName to the Queue
                result.add(threadName);
                try {
                    // Store the JNDI lookup of the scheduledExecutorRef
                    result.add(new InitialContext().lookup("java:comp/env/concurrent/scheduledExecutorRef"));
                } catch (NamingException x) {
                    result.add(x);
                }
            }
        };

        // Use the Excecutor set on the client to submit the simpleHelloCall CallBack
        Future future = ((ManagedScheduledExecutorService) scheduledServiceClient.getExecutor()).submit(simpleHelloCall);

        future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);

        // Check result queue first value was set by a thread with the correct thread name
        Object r = result.poll();
        // Check result queue second value
        String callbackThreadName = (String) r;
        assertTrue(callbackThreadName, callbackThreadName.startsWith("Default Executor-thread-"));

        // Check result queue third value has an instance of the ScheduledExecutorService
        assertNotNull(r = result.poll());
        if (r instanceof ScheduledExecutorService)
            ; // test passes, servlet's java:comp should be available to callback thread
        else if (r instanceof Throwable)
            throw new Exception("Unexpected failure for InvocationCallback lookup attempt. See cause.", (Throwable) r);

    }

    // Used to updated the expected response with the method name of the test.
    public void setExpectedResponse(String method) {
        expectedResponse = "Hello " + method + "!";
    }

}
