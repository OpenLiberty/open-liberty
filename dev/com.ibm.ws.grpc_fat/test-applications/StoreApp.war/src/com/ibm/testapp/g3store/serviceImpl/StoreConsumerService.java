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
package com.ibm.testapp.g3store.serviceImpl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.test.g3store.grpc.AppConsumerServiceGrpc;
import com.ibm.test.g3store.grpc.AppNameRequest;
import com.ibm.test.g3store.grpc.NameResponse;
import com.ibm.test.g3store.grpc.Price;
import com.ibm.test.g3store.grpc.PriceResponse;
import com.ibm.test.g3store.grpc.RetailApp;
import com.ibm.test.g3store.grpc.RetailAppResponse;
import com.ibm.testapp.g3store.cache.AppCache;
import com.ibm.testapp.g3store.cache.AppCacheFactory;
import com.ibm.testapp.g3store.utilsStore.StoreUtils;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

/**
 * @author anupag
 * @version 1.0
 *
 *          This service will provide the implementations of AppConsumer APIs.
 *
 */
public class StoreConsumerService extends AppConsumerServiceGrpc.AppConsumerServiceImplBase {

    private static Logger log = Logger.getLogger(StoreConsumerService.class.getName());

    public StoreConsumerService() {
    }

    /**
     * runtime exception as Status.NOTFOUND
     */
    @Override
    public void getAllAppNames(com.google.protobuf.Empty request,
                               io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.NameResponse> responseObserver) {

        AppCache cacheInstance = AppCacheFactory.getInstance();

        if (cacheInstance.getAllKeys().size() > 0) {
            NameResponse response = NameResponse.newBuilder()
                            .addAllNames(cacheInstance.getAllKeys())
                            .build();

            if (log.isLoggable(Level.FINE)) {
                log.fine("getAllAppNames AppConsumerService is returning the list of app names, count: "
                         + response.getNamesCount() + "]");
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            responseObserver.onError(
                                     Status.NOT_FOUND
                                                     .withDescription("There are no apps in the cache.")
                                                     .augmentDescription("First run the producerService to create the apps.")
                                                     .asRuntimeException());
        }

    }

    /**
     * runtime exception as Status.INVALID_ARGUMENT
     *
     */
    @Override
    public void getAppInfo(com.ibm.test.g3store.grpc.AppNameRequest request,
                           io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.RetailAppResponse> responseObserver) {

        String name = request.getName();
        AppCache cacheInstance = AppCacheFactory.getInstance();

        RetailApp app = cacheInstance.getEntryValue(name);

        if (!StoreUtils.isBlank(app)) {
            RetailAppResponse response = RetailAppResponse.newBuilder().setRetailApp(app).build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } else {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("The app name sent is not in the cache.")
                            .augmentDescription("Name sent = " + name)
                            .asRuntimeException());
        }

    }

    /**
     * runtime exception as Status.INVALID_ARGUMENT
     * runtime exception as Status.NOT_FOUND
     */
    @Override
    public io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.AppNameRequest> getPrices(
                                                                                           io.grpc.stub.StreamObserver<com.ibm.test.g3store.grpc.PriceResponse> responseObserver) {

        StreamObserver<AppNameRequest> requestObserver = new StreamObserver<AppNameRequest>() {

            AppCache cacheInstance = AppCacheFactory.getInstance();

            @Override
            public void onNext(AppNameRequest value) {

                String name = value.getName();
                if (!StoreUtils.isBlank(name)) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("getPrices: name of the app = " + name);
                    }

                    RetailApp app = cacheInstance.getEntryValue(name);

                    if (app != null) {
                        List<Price> prices = app.getPricesList();
                        if (log.isLoggable(Level.FINE)) {
                            log.fine("getPrices: prices = " + prices);
                        }
                        // get the Price list and return with the name
                        responseObserver.onNext(PriceResponse.newBuilder()
                                        .setName(name)
                                        .addAllPrices(prices)
                                        .build());
                    } else {

                        responseObserver
                                        .onError(Status.NOT_FOUND.withDescription("The app name sent is not in the cache.")
                                                        .augmentDescription("Name sent = " + name)
                                                        .asRuntimeException());
                    }
                } else {
                    responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("The name sent is empty.")
                                    .augmentDescription("Name sent = " + name)
                                    .asRuntimeException());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.log(Level.WARNING, "AppConsumerService: Encountered error in getPrices = ", t);
            }

            @Override
            public void onCompleted() {

                responseObserver.onCompleted();
                if (log.isLoggable(Level.FINE)) {
                    log.fine("getPrices: completed on server");
                }
            }
        };
        return requestObserver;

    }

}
