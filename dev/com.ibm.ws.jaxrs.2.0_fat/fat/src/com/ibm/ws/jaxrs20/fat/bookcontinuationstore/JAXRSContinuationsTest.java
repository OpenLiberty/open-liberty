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
package com.ibm.ws.jaxrs20.fat.bookcontinuationstore;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getHeaderString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxrs20.fat.TestUtils;
import com.ibm.ws.jaxrs20.fat.bookstore.IOUtils;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class JAXRSContinuationsTest {

    @Server("com.ibm.ws.jaxrs.fat.bookcontinuationstore")
    public static LibertyServer server;

    private static final String CONTEXT_ROOT = "bookcontinuationstore";
    private static final String cxf = "publish/shared/resources/cxf/";
    private static final String NON_VOID_RETURN_WARNING = "com.ibm.ws.jaxrs20.fat.bookcontinuationstore.BookContinuationStore#getBookDescription_nonVoidReturn method is not a valid JAX-RS AsyncResponse method as it has a non-void response type";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp(CONTEXT_ROOT, "com.ibm.ws.jaxrs20.fat.bookcontinuationstore");
        app.addAsLibraries(new File(cxf).listFiles());
        ShrinkHelper.exportDropinAppToServer(server, app);
        server.addInstalledAppForValidation(CONTEXT_ROOT);

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Test
    public void testDefaultTimeout() throws Exception {
        WebClient wc = WebClient.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/defaulttimeout"));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response r = wc.get();
        assertEquals(503, r.getStatus());
    }

    @Test
    public void testImmediateResume() throws Exception {
        WebClient wc = WebClient.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/resume"));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        wc.accept("text/plain");
        String str = wc.get(String.class);
        assertEquals("immediateResume", str);
    }

    @Test
    public void testUnmappedAfterTimeout() throws Exception {
        WebClient wc = WebClient.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/suspend/unmapped"));
        Response r = wc.get();
        assertEquals(500, r.getStatus());
    }

    @Test
    public void testImmediateResumeSubresource() throws Exception {
        WebClient wc = WebClient.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/subresources/books/resume"));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        wc.accept("text/plain");
        String str = wc.get(String.class);
        assertEquals("immediateResume", str);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetBookNotFound() throws Exception {
        WebClient wc = WebClient.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/notfound"));
        wc.accept("text/plain");
        Response r = wc.get();
        assertEquals(404, r.getStatus());
    }

    @Mode(TestMode.FULL)
    @Test
    public void testGetBookNotFoundUnmapped() throws Exception {
        WebClient wc = WebClient.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/notfound/unmapped"));
        wc.accept("text/plain");
        Response r = wc.get();
        assertEquals(500, r.getStatus());
    }

    @Mode(TestMode.FULL)
    @Test
    public void testTimeoutAndCancel() throws Exception {
        doTestTimeoutAndCancel(getBaseAddress());
    }

    protected void doTestTimeoutAndCancel(String baseAddress) throws Exception {
        WebClient wc = WebClient.create(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/cancel"));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(1000000L);
        Response r = wc.get();
        assertEquals(503, r.getStatus());
        String retryAfter = getHeaderString(HttpHeaders.RETRY_AFTER, r);
        assertNotNull(retryAfter);
        assertEquals("10", retryAfter);
    }

    @Test
    public void testContinuationWithTimeHandler() throws Exception {

        doTestContinuation("bookstore/books/timeouthandler");
    }

    @Test
    public void testContinuationWithTimeHandlerResumeOnly() throws Exception {

        doTestContinuation("bookstore/books/timeouthandlerresume");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testContinuation() throws Exception {

        doTestContinuation("bookstore/books");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testContinuationSubresource() throws Exception {

        doTestContinuation("bookstore/books/subresources");
    }

    protected void doTestContinuation(String pathSegment) throws Exception {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                        new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);
        List<BookWorker> workers = new ArrayList<BookWorker>(5);
        for (int x = 1; x < 6; x++) {
            workers.add(new BookWorker(TestUtils.getBaseTestUri(CONTEXT_ROOT, pathSegment + "/" + x),
                            Integer.toString(x),
                            "CXF in Action" + x, startSignal, doneSignal));
        }
        for (BookWorker w : workers) {
            executor.execute(w);
        }

        startSignal.countDown();
        doneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();
        assertEquals("Not all invocations have completed", 0, doneSignal.getCount());
        for (BookWorker w : workers) {
            w.checkError();
        }
    }

    @Test
    public void testAsyncMethodWithNonVoidReturnType() throws Exception {
        checkBook(TestUtils.getBaseTestUri(CONTEXT_ROOT, "bookstore/books/nonvoidreturn/3"), "3", "CXF in Action3");
        server.findStringsInLogs(NON_VOID_RETURN_WARNING);
    }

    private void checkBook(String address, String id, String expected) throws Exception {
        GetMethod get = new GetMethod(address);
        HttpClient httpclient = new HttpClient();
        try {
            int result = httpclient.executeMethod(get);
            assertEquals(200, result);
            assertEquals("Book description for id " + id + " is wrong",
                         expected, IOUtils.toString(get.getResponseBodyAsStream()));
        } finally {
            // Release current connection to the connection pool once you are done
            get.releaseConnection();
        }
    }

    @Ignore
    private class BookWorker implements Runnable {

        private final String address;
        private final String id;
        private final String expected;
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;
        private Exception error;

        public BookWorker(String address,
                          String id,
                          String expected,
                          CountDownLatch startSignal,
                          CountDownLatch doneSignal) {
            this.address = address;
            this.id = id;
            this.expected = expected;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        public void checkError() throws Exception {
            if (error != null) {
                throw error;
            }
        }

        @Override
        public void run() {

            try {
                startSignal.await();
                checkBook(address, id, expected);
                doneSignal.countDown();
            } catch (InterruptedException ex) {
                // ignore
            } catch (Exception ex) {
                ex.fillInStackTrace();
                error = ex;
            }

        }

    }

    protected String getBaseAddress() {
        return "/bookstore";
    }

}
