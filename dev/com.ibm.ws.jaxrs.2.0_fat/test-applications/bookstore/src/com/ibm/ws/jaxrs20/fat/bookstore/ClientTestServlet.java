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
package com.ibm.ws.jaxrs20.fat.bookstore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.apache.cxf.jaxrs.client.WebClient;

@WebServlet("/TestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = -6241534589062372914L;
    private static final String CONTEXT_ROOT = "bookstore";
    private static String serverIP;
    private static String serverPort;

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
        }
    }

    private static String getAddress(String path) {
        return "http://" + serverIP + ":" + serverPort + "/" + CONTEXT_ROOT + "/" + path;
    }

    public void testGetGenericBook(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/genericbooks/123");
        doTestGetGenericBook(address, 124L, false);
        ret.append("OK");
    }

    public void testGetGenericBook2(Map<String, String> param, StringBuilder ret) throws Exception {

        String address = getAddress("bookstore/genericbooks2/123");
        doTestGetGenericBook(address, 123L, true);
        ret.append("OK");
    }

    private void doTestGetGenericBook(String address, long bookId, boolean checkAnnotations) throws Exception {

        WebClient wc = WebClient.create(address);
        wc.accept("application/xml");
        Book book = wc.get(Book.class);
        assertEquals(bookId, book.getId());
        MediaType mt = wc.getResponse().getMediaType();
        assertEquals("application/xml;charset=ISO-8859-1", mt.toString());
        if (checkAnnotations) {
            assertEquals("OK", wc.getResponse().getHeaderString("Annotations"));
        } else {
            assertNull(wc.getResponse().getHeaderString("Annotations"));
        }
    }

    public void testGetBook(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("bookstore/bookheaders/simple");
        doTestGetBook(address, false);
        ret.append("OK");
    }

    public void testGetBookSyncLink(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("bookstore/bookheaders/simple");
        WebClient wc = createWebClient(address);
        Book book = wc.sync().get(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);
        ret.append("OK");
    }

    public void testGetBookSpec(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("bookstore/bookheaders/simple");
        Client client = ClientBuilder.newClient();
        client.register((Object) ClientFilterClientAndConfigCheck.class);
        client.property("clientproperty", "somevalue");
        Book book = client.target(address).request("application/xml").get(Book.class);
        assertEquals(124L, book.getId());
        ret.append("OK");
    }

    public void testGetBookSyncWithAsync(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("bookstore/bookheaders/simple");
        doTestGetBook(address, true);
        ret.append("OK");
    }

    public void testGetBookAsync(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/bookheaders/simple");
        doTestGetBookAsync(address, false);
        ret.append("OK");
    }

    public void testGetBookAsyncNoCallback(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/bookheaders/simple");
        WebClient wc = createWebClient(address);
        Future<Book> future = wc.async().get(Book.class);
        Book book = future.get();
        assertEquals(124L, book.getId());
        validateResponse(wc);
        ret.append("OK");
    }

    public void testGetBookAsyncResponse(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/bookheaders/simple");
        doTestGetBookAsyncResponse(address, false);
        ret.append("OK");
    }

    public void testGetBookAsyncInvoker(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/bookheaders/simple");
        doTestGetBookAsync(address, true);
        ret.append("OK");
    }

    public void testPreMatchContainerFilterThrowsException(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("throwException");
        WebClient wc = WebClient.create(address);
        Response response = wc.get();
        assertEquals(500, response.getStatus());
        assertEquals("Prematch filter error", response.readEntity(String.class));
        assertEquals("prematch", response.getHeaderString("FilterException"));
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("OK2", response.getHeaderString("Response2"));
        assertNull(response.getHeaderString("DynamicResponse"));
        assertNull(response.getHeaderString("Custom"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("serverWrite2", response.getHeaderString("ServerWriterInterceptor2"));
        assertEquals("serverWriteHttpResponse",
                     response.getHeaderString("ServerWriterInterceptorHttpResponse"));
        assertEquals("text/plain;charset=us-ascii", response.getMediaType().toString());
        ret.append("OK");
    }

    public void testPostMatchContainerFilterThrowsException(Map<String, String> param, StringBuilder ret) {
        // By default webcontainer ignores query parameters with no value and no '='
        String address = getAddress("bookstore/bookheaders/simple?throwException=");
        WebClient wc = WebClient.create(address);
        Response response = wc.get();
        assertEquals(500, response.getStatus());
        assertEquals("Postmatch filter error", response.readEntity(String.class));
        assertEquals("postmatch", response.getHeaderString("FilterException"));
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("OK2", response.getHeaderString("Response2"));
        assertEquals("Dynamic", response.getHeaderString("DynamicResponse"));
        assertEquals("custom", response.getHeaderString("Custom"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("text/plain;charset=us-ascii", response.getMediaType().toString());
        ret.append("OK");
    }

    public void testGetBookWrongPath(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("wrongpath");
        doTestGetBook(address, false);
        ret.append("OK");
    }

    public void testGetBookWrongPathAsync(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("wrongpath");
        doTestGetBookAsync(address, false);
        ret.append("OK");
    }

    public void testPostCollectionGenericEntity(Map<String, String> param, StringBuilder ret) throws Exception {
        String endpointAddress = getAddress("bookstore/collections3");
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");

        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);

        Future<Book> future = wc.post(collectionEntity, callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, holder.value);
        assertNotSame(collectionEntity.getEntity().get(0), book);
        assertEquals(collectionEntity.getEntity().get(0).getName(), book.getName());
        ret.append("OK");
    }

    public void testPostCollectionGenericEntityAsEntity(Map<String, String> param, StringBuilder ret) throws Exception {
        String endpointAddress = getAddress("bookstore/collections3");
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml");

        GenericEntity<List<Book>> collectionEntity = createGenericEntity();

        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);

        Future<Book> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"),
                                              callback);
        Book book = future.get();
        assertEquals(200, wc.getResponse().getStatus());
        assertSame(book, holder.value);
        assertNotSame(collectionEntity.getEntity().get(0), book);
        assertEquals(collectionEntity.getEntity().get(0).getName(), book.getName());
        ret.append("OK");
    }

    public void testPostReplaceBook(Map<String, String> param, StringBuilder ret) throws Exception {
        String endpointAddress = getAddress("bookstore/books2");
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/xml").type("application/xml");
        Book book = wc.post(new Book("book", 555L), Book.class);
        assertEquals(561L, book.getId());
        ret.append("OK");
    }

    public void testPostReplaceBookMistypedCT(Map<String, String> param, StringBuilder ret) throws Exception {
        String endpointAddress = getAddress("bookstore/books2");
        WebClient wc = WebClient.create(endpointAddress,
                                        Collections.singletonList(new ReplaceBodyFilter()));
        wc.accept("text/mistypedxml").type("text/xml");
        Book book = wc.post(new Book("book", 555L), Book.class);
        assertEquals(561L, book.getId());
        ret.append("OK");
    }

    public void testPostGetCollectionGenericEntityAndType(Map<String, String> param, StringBuilder ret) throws Exception {
        String endpointAddress = getAddress("bookstore/collections");
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        final Holder<List<Book>> holder = new Holder<List<Book>>();
        InvocationCallback<List<Book>> callback = new CustomInvocationCallback(holder);

        Future<List<Book>> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"),
                                                    callback);

        List<Book> books2 = future.get();
        assertNotNull(books2);

        List<Book> books = collectionEntity.getEntity();
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
        assertEquals(200, wc.getResponse().getStatus());
        ret.append("OK");
    }

    public void testPostGetCollectionGenericEntityAndType2(Map<String, String> param, StringBuilder ret) throws Exception {
        String endpointAddress = getAddress("bookstore/collections");
        WebClient wc = WebClient.create(endpointAddress);
        wc.accept("application/xml").type("application/xml");
        GenericEntity<List<Book>> collectionEntity = createGenericEntity();
        GenericType<List<Book>> genericResponseType = new GenericType<List<Book>>() {};

        Future<List<Book>> future = wc.async().post(Entity.entity(collectionEntity, "application/xml"),
                                                    genericResponseType);

        List<Book> books2 = future.get();
        assertNotNull(books2);

        List<Book> books = collectionEntity.getEntity();
        assertNotSame(books, books2);
        assertEquals(2, books2.size());
        Book b11 = books.get(0);
        assertEquals(123L, b11.getId());
        assertEquals("CXF in Action", b11.getName());
        Book b22 = books.get(1);
        assertEquals(124L, b22.getId());
        assertEquals("CXF Rocks", b22.getName());
        assertEquals(200, wc.getResponse().getStatus());
        ret.append("OK");
    }

    private GenericEntity<List<Book>> createGenericEntity() {
        Book b1 = new Book("CXF in Action", 123L);
        Book b2 = new Book("CXF Rocks", 124L);
        List<Book> books = new ArrayList<Book>();
        books.add(b1);
        books.add(b2);
        return new GenericEntity<List<Book>>(books) {};
    }

    private InvocationCallback<Book> createCallback(final Holder<Book> holder) {
        return new InvocationCallback<Book>() {
            @Override
            public void completed(Book response) {
                holder.value = response;
            }

            @Override
            public void failed(Throwable error) {
                error.printStackTrace();
            }
        };
    }

    private static class CustomInvocationCallback implements InvocationCallback<List<Book>> {
        private final Holder<List<Book>> holder;

        public CustomInvocationCallback(Holder<List<Book>> holder) {
            this.holder = holder;
        }

        @Override
        public void completed(List<Book> books) {
            holder.value = books;

        }

        @Override
        public void failed(Throwable arg0) {

        }

    }

    private void doTestGetBook(String address, boolean useAsync) {
        WebClient wc = createWebClient(address);
        if (useAsync) {
            WebClient.getConfig(wc).getRequestContext().put("use.async.http.conduit", true);
        }
        Book book = wc.get(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    private WebClient createWebClient(String address) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientHeaderRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        return WebClient.create(address, providers);
    }

    private WebClient createWebClientPost(String address) {
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientHeaderRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        providers.add(new ClientReaderInterceptor());
        providers.add(new ClientWriterInterceptor());
        return WebClient.create(address, providers);
    }

    private void doTestGetBookAsync(String address, boolean asyncInvoker) throws InterruptedException, ExecutionException {

        WebClient wc = createWebClient(address);

        final Holder<Book> holder = new Holder<Book>();
        InvocationCallback<Book> callback = createCallback(holder);

        Future<Book> future = asyncInvoker ? wc.async().get(callback) : wc.get(callback);
        Book book = future.get();
        assertSame(book, holder.value);
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    private void doTestPostBookAsyncHandler(String address) throws InterruptedException, ExecutionException {

        WebClient wc = createWebClientPost(address);
        wc.header("Content-Type", "application/xml");

        final Holder<Book> holder = new Holder<Book>();
        final InvocationCallback<Book> callback = new InvocationCallback<Book>() {
            @Override
            public void completed(Book response) {
                holder.value = response;
            }

            @Override
            public void failed(Throwable error) {}
        };

        Future<Book> future = wc.post(new Book("async", 126L), callback);
        Book book = future.get();
        assertSame(book, holder.value);
        assertEquals(124L, book.getId());
        validatePostResponse(wc, true);
    }

    private void doTestGetBookAsyncResponse(String address, boolean asyncInvoker) throws InterruptedException, ExecutionException {

        WebClient wc = createWebClient(address);
        wc.accept(MediaType.APPLICATION_XML_TYPE);

        final Holder<Response> holder = new Holder<Response>();
        final InvocationCallback<Response> callback = new InvocationCallback<Response>() {
            @Override
            public void completed(Response response) {
                holder.value = response;
            }

            @Override
            public void failed(Throwable error) {}
        };

        Future<Response> future = asyncInvoker ? wc.async().get(callback) : wc.get(callback);
        Book book = future.get().readEntity(Book.class);
        assertEquals(124L, book.getId());
        validateResponse(wc);
    }

    private void validateResponse(WebClient wc) {
        Response response = wc.getResponse();
        assertEquals("OK", response.getHeaderString("Response"));
        assertEquals("OK2", response.getHeaderString("Response2"));
        assertEquals("Dynamic", response.getHeaderString("DynamicResponse"));
        assertEquals("Dynamic2", response.getHeaderString("DynamicResponse2"));
        assertEquals("custom", response.getHeaderString("Custom"));
        assertEquals("simple", response.getHeaderString("Simple"));
        assertEquals("serverWrite", response.getHeaderString("ServerWriterInterceptor"));
        assertEquals("application/xml;charset=us-ascii", response.getMediaType().toString());
        assertEquals("http://localhost/redirect", response.getHeaderString(HttpHeaders.LOCATION));
    }

    private void validatePostResponse(WebClient wc, boolean async) {
        validateResponse(wc);
        Response response = wc.getResponse();
        assertEquals(!async ? "serverRead" : "serverReadAsync",
                     response.getHeaderString("ServerReaderInterceptor"));
        assertEquals("clientWrite", response.getHeaderString("ClientWriterInterceptor"));
        assertEquals("clientRead", response.getHeaderString("ClientReaderInterceptor"));
    }

    public void testClientFiltersLocalResponse(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("bookstores");
        List<Object> providers = new ArrayList<Object>();
        providers.add(new ClientCacheRequestFilter());
        providers.add(new ClientHeaderResponseFilter());
        WebClient wc = WebClient.create(address, providers);
        Book theBook = new Book("Echo", 123L);
        Response r = wc.post(theBook);
        assertEquals(201, r.getStatus());
        assertEquals("http://localhost/redirect", r.getHeaderString(HttpHeaders.LOCATION));
        Book responseBook = r.readEntity(Book.class);
        assertSame(theBook, responseBook);
        ret.append("OK");
    }

    public void testPostBook(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("bookstore/bookheaders/simple");
        WebClient wc = createWebClientPost(address);
        wc.header("Content-Type", "application/xml");
        Book book = wc.post(new Book("Book", 126L), Book.class);
        assertEquals(124L, book.getId());
        validatePostResponse(wc, false);
        ret.append("OK");
    }

    public void testPostBookNewMediaType(Map<String, String> param, StringBuilder ret) {
        String address = getAddress("bookstore/bookheaders/simple");
        WebClient wc = createWebClientPost(address);
        wc.header("newmediatype", "application/v1+xml");
        Book book = wc.post(new Book("Book", 126L), Book.class);
        assertEquals(124L, book.getId());
        validatePostResponse(wc, false);
        assertEquals("application/v1+xml", wc.getResponse().getHeaderString("newmediatypeused"));
        ret.append("OK");
    }

    public void testBookExistsServerStreamReplace(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/books/check2");
        WebClient wc = WebClient.create(address);
        wc.accept("text/plain").type("text/plain");
        assertTrue(wc.post("s", Boolean.class));
        ret.append("OK");
    }

    public void testBookExistsServerAddressOverwrite(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/books/checkN");
        WebClient wc = WebClient.create(address);
        wc.accept("text/plain").type("text/plain");
        assertTrue(wc.post("s", Boolean.class));
        ret.append("OK");
    }

    public void testPostBookAsync(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/bookheaders/simple/async");
        WebClient wc = createWebClientPost(address);
        Future<Book> future = wc.async().post(Entity.xml(new Book("Book", 126L)), Book.class);
        assertEquals(124L, future.get().getId());
        validatePostResponse(wc, true);
        ret.append("OK");
    }

    public void testPostBookAsyncHandler(Map<String, String> param, StringBuilder ret) throws Exception {
        String address = getAddress("bookstore/bookheaders/simple/async");
        doTestPostBookAsyncHandler(address);
        ret.append("OK");
    }

    /**
     * Tests whether the PostConstruct method is invoked - even if it has private visibility.
     */
    public void testPrivatePostConstructMethodInvoked(Map<String, String> param, StringBuilder ret) throws Exception {
        // start with a simple request to ensure that the resource has been started
        testGetBookSpec(param, ret);
        ret.append("OK");
    }

    private static class ReplaceBodyFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext rc) throws IOException {
            String method = rc.getMethod();
            String expectedMethod = null;
            if (rc.getAcceptableMediaTypes().contains(MediaType.valueOf("text/mistypedxml"))
                && rc.getHeaders().getFirst("THEMETHOD") != null) {
                expectedMethod = "DELETE";
                rc.setUri(URI.create(getAddress("/bookstore/books2")));
                rc.setMethod(rc.getHeaders().getFirst("THEMETHOD").toString());
            } else {
                expectedMethod = "POST";
            }

            if (!expectedMethod.equals(method)) {
                throw new RuntimeException();
            }
            rc.setEntity(new Book("book", ((Book) rc.getEntity()).getId() + 5));
        }
    }

    private static class ClientCacheRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.abortWith(Response.status(201).entity(context.getEntity()).type(MediaType.TEXT_XML_TYPE).build());
        }
    }

    private static class ClientHeaderRequestFilter implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            context.getHeaders().putSingle("Simple", "simple");
        }
    }

    public static class ClientFilterClientAndConfigCheck implements ClientRequestFilter {

        @Override
        public void filter(ClientRequestContext context) throws IOException {
            String prop = context.getClient().getConfiguration().getProperty("clientproperty").toString();
            String prop2 = context.getConfiguration().getProperty("clientproperty").toString();
            if (!prop2.equals(prop) || !"somevalue".equals(prop2)) {
                throw new RuntimeException();
            }

        }
    }

    private static class ClientHeaderResponseFilter implements ClientResponseFilter {

        @Override
        public void filter(ClientRequestContext reqContext,
                           ClientResponseContext respContext) throws IOException {
            respContext.getHeaders().putSingle(HttpHeaders.LOCATION, "http://localhost/redirect");

        }

    }

    public static class ClientReaderInterceptor implements ReaderInterceptor {

        @Override
        public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
            if (context.getInputStream() != null) {
                context.getHeaders().add("ClientReaderInterceptor", "clientRead");
            }
            return context.proceed();
        }

    }

    public static class ClientWriterInterceptor implements WriterInterceptor {

        @Override
        public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
            context.getHeaders().add("ClientWriterInterceptor", "clientWrite");
            context.proceed();
        }

    }

    private class Holder<T> {
        public T value;
    }
}
