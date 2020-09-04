/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.testapp.g3store.grpcservice;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.ibm.test.g3store.grpc.AppProducerServiceGrpc;
import com.ibm.test.g3store.grpc.AppRequest;
import com.ibm.test.g3store.grpc.AppResponse;
import com.ibm.test.g3store.grpc.DeleteResponse;
import com.ibm.test.g3store.grpc.MultiCreateResponse;
import com.ibm.test.g3store.grpc.RetailApp;
import com.ibm.test.g3store.grpc.RetailApp.Builder;
import com.ibm.test.g3store.grpc.StreamReplyA;
import com.ibm.test.g3store.grpc.StreamRequestA;
import com.ibm.testapp.g3store.cache.AppCache;
import com.ibm.testapp.g3store.cache.AppCacheFactory;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.utilsStore.StoreUtils;

import io.grpc.Context;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author anupag
 * @version 1.0
 *
 *          This Store service will provide the implementations of AppProdcuer gRPC APIs.
 *
 */
public class StoreProducerService extends AppProducerServiceGrpc.AppProducerServiceImplBase {

    private static String CLASSNAME = StoreProducerService.class.getName();
    private static Logger log = Logger.getLogger(CLASSNAME);

    public static boolean PERF_LOGGING_ON = true;

    public StoreProducerService() {
        // this constructor is required to run the gRPC on Liberty server.

        if (PERF_LOGGING_ON) {
            readStreamParmsFromFile();
        }
    }

    /**
     * Implementation of the createApp rpc
     *
     * It is a unary call from the client to create one application entry in the db
     */
    @Override
    public void createApp(com.ibm.test.g3store.grpc.AppRequest request,
                          io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppResponse> responseObserver) {

        final String m = "createApp";
        log.info(m + " ------------------------------------------------------------");
        log.info(m + " -----------------------createApp----------------------------");
        log.info(m + " ----- request received by StoreProducer grpcService to create the app. ");

        boolean useServerInterceptor = false;

        try {
            if (log.isLoggable(Level.FINE)) {
                try {
                    log.info(m + ": JSON structure sent = "
                             + JsonFormat.printer()
                                             .includingDefaultValueFields()
                                             .preservingProtoFieldNames()
                                             .print(RetailApp.newBuilder().mergeFrom(request.getRetailApp())));
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                    throw e;
                }
            }

            // db or cahce
            AppCache cacheInstance = AppCacheFactory.getInstance();

            Timestamp time = Timestamp.newBuilder()
                            .setSeconds(new Date().getTime())
                            .build();

            // set this timestamp when app is created.
            log.info(m + ": Here is the timestamp = " + time);

            String appName = request.getRetailApp().getName();

            if (StoreUtils.isBlank(appName)) {

                if (useServerInterceptor) {
                    throw new InvalidArgException("The name sent is empty. Name sent = [" + appName + "]");
                } else {

                    log.severe(m + "-----  appName sent is null or empty, appName=[" + appName + "]");
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("The name sent is empty.")
                                    .augmentDescription("Name sent = [" + appName + "]")
                                    .asRuntimeException());

                }

                return;
            }

            if (cacheInstance.getEntryValue(appName) != null) {

                log.warning(m + "-----  appName sent already exists, appName=[" + appName + "]");
                responseObserver.onError(Status.ALREADY_EXISTS.withDescription("The app already exist in the Store.")
                                .augmentDescription("First run the ProducerService to delete the app. AppName = " + appName)
                                .asRuntimeException());

                return;
            }

            Context.current().addListener(new Context.CancellationListener() {
                @Override
                public void cancelled(Context context) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine(m + "-----  Call was cancelled!");
                    }
                }
            }, Executors.newCachedThreadPool());

            if (Context.current().isCancelled()) {
                responseObserver.onError(Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException());
                return;
            }

            // Now create the entry of the app
            UUID uuid = UUID.randomUUID();
            String value = uuid.toString();

            // setID in the App
            Builder updatedApp = RetailApp.newBuilder().mergeFrom(request.getRetailApp()).setId(value);

            RetailApp rApp = updatedApp.build();

            if (log.isLoggable(Level.FINE)) {
                log.fine(m + ": id [" + value + "] created for app [" + rApp.getName() + "]");
            }
            cacheInstance.setEntryValue(appName, rApp, -1);

            AppResponse response = AppResponse.newBuilder().setId(value).build();
            responseObserver.onNext(response);

            log.info(m + "-----  request to create app has been completed " + response.getId());

            responseObserver.onCompleted();

        } catch (Exception e) {
            e.printStackTrace();

            // if any exception occurs , make sure the client gets it
            responseObserver.onError(Status.INTERNAL.withDescription("The exception in creating the entry in Store.")
                            .augmentDescription("Check the logs. " + e.getMessage())
                            .asRuntimeException());
        }

        log.info(m + " -----------------------createApp----------------------------");
        log.info(m + " ------------------------------------------------------------");

    }

    /**
     * Implementation of the deleteApp rpc
     * rpc deleteApp(DeleteRequest) returns (DeleteResponse)
     *
     * It is a unary call from the client to remove one application entry in the db
     */
    @Override
    public void deleteApp(com.ibm.test.g3store.grpc.DeleteRequest request,
                          io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse> responseObserver) {

        final String m = "deleteApp";

        log.info(m + " ------------------------------------------------------------");
        log.info(m + " -----------------------deleteApp----------------------------");
        log.info(m + " ----- request received by StoreProducer grpcService to delete the app names. ");

        // db or cahce
        AppCache cacheInstance = AppCacheFactory.getInstance();

        // get App to be removed
        String appName = request.getAppName();

        // this should not happen as client should get NPE
        if (StoreUtils.isBlank(appName)) {
            log.severe(m + " -----  appName sent is null or empty, appName=[" + appName + "]");
            responseObserver.onError(
                                     Status.INVALID_ARGUMENT
                                                     .withDescription("The name sent is empty.")
                                                     .augmentDescription("Name sent = [" + appName + "]")
                                                     .asRuntimeException());

            return;
        }

        if (cacheInstance.getEntryValue(appName) == null) {
            log.severe(m + " -----  appName sent does not exist, appName=[" + appName + "]");

            responseObserver.onError(
                                     Status.NOT_FOUND
                                                     .withDescription("The app does not exist in the cache.")
                                                     .augmentDescription("Nothing to be done. AppName sent = " + appName)
                                                     .asRuntimeException());

            return;
        }

        Context currentContext = Context.current();

        if (currentContext.isCancelled()) {
            responseObserver.onError(Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException());
            return;
        }

        cacheInstance.removeEntryValue(appName);

        DeleteResponse response = DeleteResponse.newBuilder()
                        .setResult("The app [" + appName + "] has been removed from the server")
                        .build();

        // send the response
        responseObserver.onNext(response);
        log.info(m + " ----- request to remove app has been completed by StoreProducer");

        // complete it
        responseObserver.onCompleted();

        log.info(m + " -----------------------deleteApp----------------------------");
        log.info(m + " ------------------------------------------------------------");

    }

    /**
     * Implementation of the deleteAllApps rpc
     * rpc deleteAllApps(google.protobuf.Empty) returns (stream DeleteResponse)
     *
     * It is a server streaming call from the client to remove all application entries in the db
     */
    @Override
    public void deleteAllApps(com.google.protobuf.Empty request,
                              io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.DeleteResponse> responseObserver) {

        final String m = "deleteAllApps";

        log.info(m + " ------------------------------------------------------------");
        log.info(m + " -----------------------deleteAllApps------------------------");
        log.info(m + " ----- request received by StoreProducer grpcService to delete all the app names. ");

        // db or cahce
        AppCache cacheInstance = AppCacheFactory.getInstance();

        Context currentContext = Context.current();

        if (currentContext.isCancelled()) {
            responseObserver.onError(Status.CANCELLED.withDescription("Cancelled by client").asRuntimeException());
            return;
        }

        Iterator<String> it = cacheInstance.getAllKeys().iterator();
        while (it.hasNext()) {
            String name = it.next();

            log.info(m + ": appName to be removed= " + name);

            cacheInstance.removeEntryValue(name);

            DeleteResponse response = DeleteResponse.newBuilder()
                            .setResult("The app [" + name + "] has been removed from the Store. ")
                            .build();

            log.info(m + ": request to remove app " + name + " has been completed ");

            // everytime we have data send it
            responseObserver.onNext(response);
        }

        // now we are done , complete so , client can finish
        responseObserver.onCompleted();

        log.info(m + " -----------------------deleteAllApps------------------------");
        log.info(m + " ------------------------------------------------------------");

    }

    /**
     * Implementation of the createApps rpc
     * rpc createApps(stream AppRequest) returns (MultiCreateResponse)
     *
     * It is a client streaming call to create all application entries in the db
     */
    @Override
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppRequest> createApps(
                                                                                        final io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.MultiCreateResponse> responseObserver) {

        final String m = "createApps";
        log.info(m + " ------------------------------------------------------------");
        log.info(m + " -----------------------createApps---------------------------");
        log.info(m + " ----- request received by StoreProducer grpcService to create the app names  ----- ");

        StreamObserver<AppRequest> requestObserver = new StreamObserver<AppRequest>() {

            // db or cahce
            AppCache cacheInstance = AppCacheFactory.getInstance();

            @Override
            public void onNext(AppRequest request) {

                String appName = request.getRetailApp().getName();

                if (StoreUtils.isBlank(appName)) {
                    log.severe(m + " -----appName sent is null or empty, appName=[" + appName + "]");

                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("The name sent is empty.")
                                    .augmentDescription("Name sent = [" + appName + "]")
                                    .asRuntimeException());

                    return;
                }

                if (cacheInstance.getEntryValue(appName) != null) {
                    log.warning(m + "-----  appName sent already exists, appName=[" + appName + "]");
                    responseObserver.onError(Status.ALREADY_EXISTS.withDescription("The app already exist in the cache.")
                                    .augmentDescription("First run the ProducerService to delete the app. AppName sent = " + appName)
                                    .asRuntimeException());

                    return;
                }

                // get the request value , do the work
                UUID uuid = UUID.randomUUID();
                String value = uuid.toString();

                // setID in the App
                Builder updatedApp = RetailApp.newBuilder().mergeFrom(request.getRetailApp()).setId(value);

                if (log.isLoggable(Level.FINE)) {
                    try {
                        log.info(m + ": JSON structure sent = " + JsonFormat.printer()
                                        .includingDefaultValueFields()
                                        .preservingProtoFieldNames()
                                        .print(updatedApp));
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }
                }

                RetailApp rApp = updatedApp.build();

                boolean success = cacheInstance.setEntryValue(appName, rApp, -1);

                if (success && log.isLoggable(Level.INFO)) {
                    log.info(m + ": id [" + value + "] created for app [" + appName
                             + "]");
                }

            }

            @Override
            public void onError(Throwable t) {
                log.log(Level.SEVERE, "Store: createApps: Encountered error in createApps", t);

            }

            @Override
            public void onCompleted() {

                String result = "";

                Iterator<String> it = cacheInstance.getAllKeys().iterator();
                while (it.hasNext()) {
                    String name = it.next();

                    if (log.isLoggable(Level.FINE)) {
                        log.fine(m + ":  appName=[" + name + "]");
                    }

                    String id = cacheInstance.getEntryValue(name).getId();
                    result += "Store has successfully added the app [" + name + "] with id [ " + id + " ] " + "\n";

                }
                responseObserver.onNext(MultiCreateResponse.newBuilder().setResult(result).build());

                log.info(m + " -----  complete, result [" + result + "]");

                responseObserver.onCompleted();

                log.info(m + " -----------------------createApps---------------------------");
                log.info(m + " ------------------------------------------------------------");
            }

        };
        return requestObserver;
    }

    @Override
    public StreamObserver<StreamRequestA> clientStreamA(final StreamObserver<StreamReplyA> responseObserver) {

        log.info("clientStreamA: Service Entry --------------------------------------------------");

        return new ClientStreamClass(responseObserver);
    }

    class ClientStreamClass implements StreamObserver<StreamRequestA> {
        String lastClientMessage = "Nothing yet";
        String responseString = "Response from Server: ";
        int count = 0;
        StreamObserver<StreamReplyA> responseObserver = null;

        public ClientStreamClass(StreamObserver<StreamReplyA> ro) {
            responseObserver = ro;
        }

        @Override
        public void onNext(StreamRequestA request) {
            String s = request.toString();
            lastClientMessage = s;

            s = "<br>...(( " + s + " onNext at server called at: " + System.currentTimeMillis() + " ))";
            // limit string to first 200 characters
            if (s.length() > 200) {
                s = s.substring(0, 200);
            }

            //print out first first message
            count++;
            if (count == 1) {
                log.info("clientStreamA: count: " + count + " received: " + s);
            }

            if (PERF_LOGGING_ON) {
                if (count == 1)
                    System.out.println(qtf() + " ClientStream: SERVER received message 1 hc: " + responseObserver.hashCode());
                else if ((count % 1000) == 0)
                    System.out.println(qtf() + " ClientStream: SERVER received message " + count + " hc: " + responseObserver.hashCode());
            }

            // If response is greater than 64K, let's take some off of it
            if (responseString.length() > 65536) {
                responseString = responseString.substring(0, 32768);
            }

            responseString = responseString + s;
        }

        @Override
        public void onError(Throwable t) {
            log.log(Level.SEVERE, "Store: Encountered error in clientStreamA: ", t);
            if (PERF_LOGGING_ON)
                System.out.println(qtf() + " ClientStream: SERVER received onError: hc: " + responseObserver.hashCode() + " throwable: " + t);
        }

        @Override
        public void onCompleted() {
            log.info("clientStreamA: onComplete() called");

            if (PERF_LOGGING_ON)
                System.out.println(qtf() + " ClientStream: SERVER received onCompleted hc: " + responseObserver.hashCode());

            String s = responseString + "...[[time response sent back to Client: " + System.currentTimeMillis() + "]]";

            int maxStringLength = 32768 - lastClientMessage.length() - 1;
            // limit response string to 32K, make sure the last message concatenated at the end
            if (s.length() > maxStringLength) {
                s = s.substring(0, maxStringLength);
                s = s + lastClientMessage;
            } else {
                s = s + lastClientMessage;
            }
            log.info("clientStreamA: onComplete() sending string of length: " + s.length());

            StreamReplyA reply = StreamReplyA.newBuilder().setMessage(s).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        }
    }

    // -------------------------------------------------------------------------

    public static int SERVER_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION = 10000000;
    public static int SERVER_STREAM_TIME_BETWEEN_MESSAGES_MSEC = 0;
    public static int SERVER_STREAM_MESSAGE_SIZE = 50; // set to 5, 50, 500, 5000, or else you will get 50.

    @Override
    public void serverStreamA(StreamRequestA req, StreamObserver<StreamReplyA> responseObserver) {

        log.info("serverStreamA: Service Entry ----------------------------------------------------------");

        // server streaming
        int numberOfMessages = SERVER_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION;
        int timeBetweenMessagesMsec = SERVER_STREAM_TIME_BETWEEN_MESSAGES_MSEC;
        StreamReplyA nextRequest = null;

        String nextMessage = null;
        String firstMessage = "This is the first Message..."; // don't change, hardcode to match string in ProducerGrpcServiceClientImpl
        String lastMessage = "And this is the last Message"; // don't change, hardcode to match string in ProducerGrpcServiceClientImpl

        String sChars = "12345678901234567890123456789012345678901234567890"; // 50 characters

        if (SERVER_STREAM_MESSAGE_SIZE == 5) {
            sChars = "12345";
        } else if (SERVER_STREAM_MESSAGE_SIZE == 500) {
            String s50chars = "12345678901234567890123456789012345678901234567890";
            sChars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
        } else if (SERVER_STREAM_MESSAGE_SIZE == 5000) {
            String s50chars = "12345678901234567890123456789012345678901234567890";
            String s500chars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
            sChars = s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars;
        }

        for (int i = 1; i <= numberOfMessages; i++) {
            if (i == 1) {
                log.info("serverStreamA: sending first message");
                nextMessage = firstMessage;
                if (PERF_LOGGING_ON)
                    System.out.println(qtf() + " ServerStream: SERVER sending message 1 hc: " + responseObserver.hashCode());
            } else if (i == numberOfMessages) {
                log.info("serverStreamA: sending last message. number of messages was: " + numberOfMessages);
                nextMessage = lastMessage;
                if (PERF_LOGGING_ON)
                    System.out.println(qtf() + " ServerStream: SERVER sending message " + i + " last message. hc: " + responseObserver.hashCode());
            } else {
                if (PERF_LOGGING_ON && (i % 1000) == 0) {
                    System.out.println(qtf() + " ServerStream: SERVER sending message " + i + " hc: " + responseObserver.hashCode());
                }
                nextMessage = "--Message " + i + " of " + numberOfMessages + " left server at time: " + System.currentTimeMillis() + "--";
                nextMessage = nextMessage + sChars;
            }

            nextRequest = StreamReplyA.newBuilder().setMessage(nextMessage).build();
            responseObserver.onNext(nextRequest);
            try {
                if (timeBetweenMessagesMsec > 0) {
                    Thread.sleep(timeBetweenMessagesMsec);
                }
            } catch (Exception x) {
                log.info("serverStreamA: caught exception sleeping, ignoring it");
            }
        }

        // wait to send onCompleted for now
        try {
            Thread.sleep(10);
        } catch (Exception x) {
            // do nothing
        }
        log.info("serverStreamA: calling onCompleted");
        responseObserver.onCompleted();

    }

    // -------------------------------------------------------------------------

    public final static int TWOWAY_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION = 200;
    public final static int TWOWAY_STREAM_TIME_BETWEEN_MESSAGES_MSEC = 0;
    public final static int TWOWAY_STREAM_MESSAGE_SIZE = 50; // set to 5, 50, 500, 5000, or else you will get 50.

    @Override
    public StreamObserver<StreamRequestA> twoWayStreamA(StreamObserver<StreamReplyA> responseObserver) {

        log.info("twoWayStreamA: Service Entry --------------------------------------------------");

        return new TwoWayStreamClass(responseObserver);
    }

    class TwoWayStreamClass implements StreamObserver<StreamRequestA> {
        Object messageSync = new Object() {
        };
        String lastClientMessage = "Nothing yet";
        String responseStringTwoWay = "Response from Server: ";
        int count = 0;

        StreamObserver<StreamReplyA> responseObserver = null;

        public TwoWayStreamClass(StreamObserver<StreamReplyA> ro) {
            responseObserver = ro;
        }

        @Override
        public void onNext(StreamRequestA request) {
            synchronized (messageSync) {

                String s = request.toString();
                lastClientMessage = s;

                s = "<br>...(( " + s + " onNext at server called at: " + System.currentTimeMillis() + " ))";
                // limit string to first 200 characters
                if (s.length() > 200) {
                    s = s.substring(0, 200);
                }
                //print out first 10 messages
                if (count < 10) {
                    count++;
                    log.info("twoWayStreamA: count: " + count + " received: " + s);
                }

                // If response is greater than 64K, let's take some off of it
                if (responseStringTwoWay.length() > 65536) {
                    responseStringTwoWay = responseStringTwoWay.substring(0, 32768);
                }

                responseStringTwoWay = responseStringTwoWay + s;

                // turnaround the message back to the client
                StreamReplyA reply = StreamReplyA.newBuilder().setMessage(s).build();
                responseObserver.onNext(reply);
            }
        }

        @Override
        public void onError(Throwable t) {
            log.log(Level.SEVERE, "Store: Encountered error in twoWayStreamA: ", t);
        }

        @Override
        public void onCompleted() {
            log.info("twoWayStreamA: onComplete() called");
            String s = responseStringTwoWay + "...[[time response sent back to Client: " + System.currentTimeMillis() + "]]";

            int maxStringLength = 32768 - lastClientMessage.length() - 1;
            // limit response string to 32K, make sure the last message concatenated at the end
            if (s.length() > maxStringLength) {
                s = s.substring(0, maxStringLength);
                s = s + lastClientMessage;
            } else {
                s = s + lastClientMessage;
            }
            log.info("twoWayStreamA: onComplete() sending string of length: " + s.length());

            StreamReplyA reply = StreamReplyA.newBuilder().setMessage(s).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

        }
    }

    @Override
    public StreamObserver<StreamRequestA> twoWayStreamAsyncThread(StreamObserver<StreamReplyA> responseObserver) {

        log.info("twoWayStreamAsyncThread: Service Entry --------------------------------------------------");

        return new TwoWayStreamAsyncThreadClass(responseObserver);

    }

    class TwoWayStreamAsyncThreadClass implements StreamObserver<StreamRequestA> {
        Object messageSync = new Object() {
        };
        String lastClientMessage = "Nothing yet";
        String responseStringTwoWay = "Response from Server: ";
        int count = 0;
        boolean twoWayThreadStarted = false;
        CountDownLatch twoWayAsyncThreadLatch = new CountDownLatch(1);

        StreamObserver<StreamReplyA> responseObserver = null;

        public TwoWayStreamAsyncThreadClass(StreamObserver<StreamReplyA> ro) {
            responseObserver = ro;
        }

        @Override
        public void onNext(StreamRequestA request) {
            synchronized (messageSync) {

                if (!twoWayThreadStarted) {
                    twoWayThreadStarted = true;
                    Thread t = new Thread(new AsyncStreaming(responseObserver, twoWayAsyncThreadLatch));
                    t.start();
                }

                String s = request.toString();
                lastClientMessage = s;

                s = "<br>...(( " + s + " onNext at server called at: " + System.currentTimeMillis() + " ))";
                // limit string to first 200 characters
                if (s.length() > 200) {
                    s = s.substring(0, 200);
                }
                //print out first 10 messages
                if (count < 10) {
                    count++;
                    log.info("twoWayStreamAsyncThread: count: " + count + " received: " + s);
                }

                // If response is greater than 64K, let's take some off of it
                if (responseStringTwoWay.length() > 65536) {
                    responseStringTwoWay = responseStringTwoWay.substring(0, 32768);
                }

                responseStringTwoWay = responseStringTwoWay + s;
            }
        }

        @Override
        public void onError(Throwable t) {
            log.log(Level.SEVERE, "Store: Encountered error in twoWayStreamA: ", t);
        }

        @Override
        public void onCompleted() {
            log.info("twoWayStreamAsyncThread: onComplete() called - wait for response thread to finish");

            // wait till AsyncStreaming is done
            try {
                twoWayAsyncThreadLatch.await(29, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

            String s = responseStringTwoWay + "...[[time response sent back to Client: " + System.currentTimeMillis() + "]]";

            int maxStringLength = 32768 - lastClientMessage.length() - 1;
            // limit response string to 32K, make sure the last message concatenated at the end
            if (s.length() > maxStringLength) {
                s = s.substring(0, maxStringLength);
                s = s + lastClientMessage;
            } else {
                s = s + lastClientMessage;
            }

            // Print out message in the logs, but don't send the message to the client since the async thread
            // needs to send a hardcoded string for the last message for the client to verify
            log.info("twoWayStreamAsyncThread: onComplete() sending string of length: " + s.length());

            responseObserver.onCompleted();
        }
    }

    class AsyncStreaming implements Runnable {
        StreamObserver<StreamReplyA> responseObserver = null;
        CountDownLatch twoWayAsyncThreadLatch = null;

        public AsyncStreaming(StreamObserver<StreamReplyA> observer, CountDownLatch inLatch) {
            responseObserver = observer;
            twoWayAsyncThreadLatch = inLatch;
        }

        @Override
        public void run() {
            log.info("twoWayStreamAsyncThread: AsyncStreaming Thread: run() entered");
            int numberOfMessages = TWOWAY_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION;
            int timeBetweenMessagesMsec = TWOWAY_STREAM_TIME_BETWEEN_MESSAGES_MSEC;

            String sChars = "12345678901234567890123456789012345678901234567890"; // 50 characters
            if (SERVER_STREAM_MESSAGE_SIZE == 5) {
                sChars = "12345";
            } else if (SERVER_STREAM_MESSAGE_SIZE == 500) {
                String s50chars = "12345678901234567890123456789012345678901234567890";
                sChars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
            } else if (SERVER_STREAM_MESSAGE_SIZE == 5000) {
                String s50chars = "12345678901234567890123456789012345678901234567890";
                String s500chars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
                sChars = s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars;
            }

            String nextMessage = null;
            String firstMessage = "This is the first Response Message..."; // don't change, hardcode to match string in ProducerGrpcServiceClientImpl
            String lastMessage = "And this is the last Response Message"; // don't change, hardcode to match string in ProducerGrpcServiceClientImpl

            for (int i = 1; i <= numberOfMessages; i++) {

                if (i == 1) {
                    nextMessage = firstMessage;
                } else if (i == numberOfMessages) {
                    nextMessage = lastMessage;
                } else {
                    nextMessage = "--Message " + i + " of " + numberOfMessages + " left client at time: " + System.currentTimeMillis() + "--";
                    nextMessage = nextMessage + sChars;
                }

                StreamReplyA reply = StreamReplyA.newBuilder().setMessage(nextMessage).build();
                responseObserver.onNext(reply);

                try {
                    if (timeBetweenMessagesMsec > 0) {
                        Thread.sleep(timeBetweenMessagesMsec);
                    } else if ((i % 100) == 0) {
                        // throw in a small delay every 100 iterations
                        Thread.sleep(200);
                    }
                } catch (Exception x) {
                    // do nothing
                }
            }

            log.info("twoWayStreamAsyncThread: AsyncStreaming Thread: run() completed, countDown the latch");
            twoWayAsyncThreadLatch.countDown();
        }
    }

    public String qtf() {
        long time = System.currentTimeMillis() & 0xfffffff;
        long msec = time % 1000;
        long sec = time / 1000;
        String result = sec + "." + msec;
        return result;
    }

    public void readStreamParmsFromFile() {

        BufferedReader br = null;
        FileReader fr = null;
        String sCurrentLine;

        System.out.println("Reading parms in from: GrpcStreamParms.txt");
        try {
            fr = new FileReader("GrpcStreamParms.txt");
            if (fr == null)
                return;
            br = new BufferedReader(fr);
            if (br == null)
                return;
            while ((sCurrentLine = br.readLine()) != null) {

                if (sCurrentLine.indexOf("SERVER_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION") != -1) {
                    sCurrentLine = br.readLine();
                    SERVER_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION = new Integer(sCurrentLine).intValue();
                    System.out.println("setting SERVER_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION to: " + SERVER_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION);
                } else if (sCurrentLine.indexOf("SERVER_STREAM_TIME_BETWEEN_MESSAGES_MSEC") != -1) {
                    sCurrentLine = br.readLine();
                    SERVER_STREAM_TIME_BETWEEN_MESSAGES_MSEC = new Integer(sCurrentLine).intValue();
                    System.out.println("setting SERVER_STREAM_TIME_BETWEEN_MESSAGES_MSEC to: " + SERVER_STREAM_TIME_BETWEEN_MESSAGES_MSEC);
                } else if (sCurrentLine.indexOf("SERVER_STREAM_MESSAGE_SIZE") != -1) {
                    sCurrentLine = br.readLine();
                    SERVER_STREAM_MESSAGE_SIZE = new Integer(sCurrentLine).intValue();
                    System.out.println("setting SERVER_STREAM_MESSAGE_SIZE to: " + SERVER_STREAM_MESSAGE_SIZE);
                }
            }
        } catch (Exception x) {
            System.out.println("Error caught while reading GrpcStreamParms.txt: " + x);
        }
    }

}
