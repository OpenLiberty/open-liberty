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

import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.Executors;
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

    public StoreProducerService() {
        // this constructor is required to run the gRPC on Liberty server.
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

    String lastClientMessage = "Nothing yet";
    private static String responseString = "Response from Server: ";

    @Override
    public StreamObserver<StreamRequestA> clientStreamA(final StreamObserver<StreamReplyA> responseObserver) {

        // two way streaming, maybe return new StreamObserver<StreamRequest> requestObserver =
        //      new StreamObserver<StreamRequest>() {
        return new StreamObserver<StreamRequestA>() {

            @Override
            public void onNext(StreamRequestA request) {
                String s = request.toString();
                lastClientMessage = s;

                s = "<br>...(( " + s + " onNext at server called at: " + System.currentTimeMillis() + " ))";
                // limit string to first 200 characters
                if (s.length() > 200) {
                    s = s.substring(0, 200);
                }
                responseString = responseString + s;
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                String s = responseString + "...[[time response sent back to Client: " + System.currentTimeMillis() + "]]";

                int maxStringLength = 32768 - lastClientMessage.length() - 1;
                // limit response string to 32K, make sure the last message concatentated at the end
                if (s.length() > maxStringLength) {
                    s = s.substring(0, maxStringLength);
                    s = s + lastClientMessage;
                }
                log.info(s);

                StreamReplyA reply = StreamReplyA.newBuilder().setMessage(s).build();
                responseObserver.onNext(reply);
                responseObserver.onCompleted();

            }
        };
    }

}
