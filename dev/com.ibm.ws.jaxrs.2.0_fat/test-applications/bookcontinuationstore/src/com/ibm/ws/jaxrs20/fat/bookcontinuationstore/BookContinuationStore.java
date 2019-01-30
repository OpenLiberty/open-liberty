/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.ibm.ws.jaxrs20.fat.bookcontinuationstore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.CompletionCallback;
import javax.ws.rs.container.ConnectionCallback;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;

@Path("/bookstore")
public class BookContinuationStore {

    private final Map<String, String> books = new HashMap<String, String>();
    private final Executor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(10));

    public BookContinuationStore() {
        init();
    }

    @GET
    @Path("/books/defaulttimeout")
    public void getBookDescriptionWithTimeout(@Suspended AsyncResponse async) {
        async.register(new CallbackImpl());
        async.setTimeout(2000, TimeUnit.MILLISECONDS);
    }

    @GET
    @Path("/books/resume")
    @Produces("text/plain")
    public void getBookDescriptionImmediateResume(@Suspended AsyncResponse async) {
        async.resume("immediateResume");
    }

    @GET
    @Path("/books/cancel")
    public void getBookDescriptionWithCancel(@PathParam("id") String id,
                                             @Suspended AsyncResponse async) {
        async.setTimeout(2000, TimeUnit.MILLISECONDS);
        async.setTimeoutHandler(new CancelTimeoutHandlerImpl());
    }

    @GET
    @Path("/books/timeouthandler/{id}")
    public void getBookDescriptionWithHandler(@PathParam("id") String id,
                                              @Suspended AsyncResponse async) {
        async.setTimeout(1000, TimeUnit.MILLISECONDS);
        async.setTimeoutHandler(new TimeoutHandlerImpl(id, false));
    }

    @GET
    @Path("/books/timeouthandlerresume/{id}")
    public void getBookDescriptionWithHandlerResumeOnly(@PathParam("id") String id,
                                                        @Suspended AsyncResponse async) {
        async.setTimeout(1000, TimeUnit.MILLISECONDS);
        async.setTimeoutHandler(new TimeoutHandlerImpl(id, true));
    }

    @GET
    @Path("/books/{id}")
    public void getBookDescription(@PathParam("id") String id,
                                   @Suspended AsyncResponse async) {
        handleContinuationRequest(id, async);
    }

    @GET
    @Path("/books/nonvoidreturn/{id}")
    public String getBookDescription_nonVoidReturn(@PathParam("id") String id,
                                                   @Suspended AsyncResponse async) {
        // according to the spec, here: http://docs.oracle.com/javaee/7/api/javax/ws/rs/container/Suspended.html
        // this method should cause a warning message, but should still execute.
        // the returned string should be ignored
        handleContinuationRequest(id, async);
        return "IGNOREABLE";
    }

    @Path("/books/subresources/")
    public BookContinuationStore getBookStore() {

        return this;

    }

    @GET
    @Path("{id}")
    public void handleContinuationRequest(@PathParam("id") String id,
                                          @Suspended AsyncResponse response) {
        resumeSuspended(id, response);
    }

    @GET
    @Path("books/notfound")
    @Produces("text/plain")
    public void handleContinuationRequestNotFound(@Suspended AsyncResponse response) {
        response.register(new CallbackImpl());
        resumeSuspendedNotFound(response);
    }

    @GET
    @Path("books/notfound/unmapped")
    @Produces("text/plain")
    public void handleContinuationRequestNotFoundUnmapped(@Suspended AsyncResponse response) {
        response.register(new CallbackImpl());
        resumeSuspendedNotFoundUnmapped(response);
    }

    @GET
    @Path("books/suspend/unmapped")
    @Produces("text/plain")
    public void handleNotMappedAfterSuspend(@Suspended AsyncResponse response) throws BookNotFoundFault {
        response.setTimeout(2000, TimeUnit.MILLISECONDS);
        response.setTimeoutHandler(new CancelTimeoutHandlerImpl());
        throw new BookNotFoundFault("");
    }

    @GET
    @Path("/disconnect")
    public void handleClientDisconnects(@Suspended AsyncResponse response) {
        response.setTimeout(0, TimeUnit.SECONDS);

        response.register(new ConnectionCallback() {
            @Override
            public void onDisconnect(AsyncResponse disconnected) {
                System.out.println("ConnectionCallback: onDisconnect, client disconnects");
            }
        });

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            // ignore 
        }

        response.resume(books.values().toString());
    }

    private void resumeSuspended(final String id, final AsyncResponse response) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                response.resume(books.get(id));
            }
        });

    }

    private void resumeSuspendedNotFound(final AsyncResponse response) {

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                response.resume(new NotFoundException());
            }
        });

    }

    private void resumeSuspendedNotFoundUnmapped(final AsyncResponse response) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    // ignore
                }
                response.resume(new BookNotFoundFault(""));
            }
        });

    }

    private void init() {
        books.put("1", "CXF in Action1");
        books.put("2", "CXF in Action2");
        books.put("3", "CXF in Action3");
        books.put("4", "CXF in Action4");
        books.put("5", "CXF in Action5");
    }

    private class TimeoutHandlerImpl implements TimeoutHandler {
        private final boolean resumeOnly;
        private final String id;
        private final AtomicInteger timeoutExtendedCounter = new AtomicInteger();

        public TimeoutHandlerImpl(String id, boolean resumeOnly) {
            this.id = id;
            this.resumeOnly = resumeOnly;
        }

        @Override
        public void handleTimeout(AsyncResponse asyncResponse) {
            if (!resumeOnly && timeoutExtendedCounter.addAndGet(1) <= 2) {
                asyncResponse.setTimeout(1, TimeUnit.SECONDS);
            } else {
                asyncResponse.resume(books.get(id));
            }
        }

    }

    private class CancelTimeoutHandlerImpl implements TimeoutHandler {

        @Override
        public void handleTimeout(AsyncResponse asyncResponse) {
            asyncResponse.cancel(10);

        }

    }

    private class CallbackImpl implements CompletionCallback {

        @Override
        public void onComplete(Throwable throwable) {
            System.out.println("CompletionCallback: onComplete, throwable: " + throwable);
        }

    }
}
