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
package com.ibm.testapp.g3store.grpcProducer.api;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Empty;
import com.ibm.test.g3store.grpc.AppRequest;
import com.ibm.test.g3store.grpc.AppResponse;
import com.ibm.test.g3store.grpc.Creator;
import com.ibm.test.g3store.grpc.DeleteRequest;
import com.ibm.test.g3store.grpc.DeleteResponse;
import com.ibm.test.g3store.grpc.GenreType;
import com.ibm.test.g3store.grpc.MultiCreateResponse;
import com.ibm.test.g3store.grpc.Price;
import com.ibm.test.g3store.grpc.RetailApp;
import com.ibm.test.g3store.grpc.RetailApp.Builder;
import com.ibm.test.g3store.grpc.SecurityType;
import com.ibm.testapp.g3store.exception.AlreadyExistException;
import com.ibm.testapp.g3store.exception.HandleExceptionsAsyncgRPCService;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.NotFoundException;
import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.DeleteAllRestResponse;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;
import com.ibm.testapp.g3store.restProducer.model.ProducerRestResponse;

import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/**
 * @author anupag
 * @version 1.0
 *
 *          This class is implementation of producer APIs.
 * 
 */
public class ProducergRPCServiceClientImpl extends ProducergRPCServiceClient {

    private static Logger log = Logger.getLogger(ProducergRPCServiceClientImpl.class.getName());

    // gRPC client implementation(s)
    /**
     * @param reqPOJO
     * @return
     * @throws Exception
     */
    public String createSingleAppinStore(AppStructure reqPOJO) throws Exception {

        // create the app structure to send to grpc server from the values sent by REST
        // request
        String id = null;
        RetailApp app = createAppBuilder_forGRPC(reqPOJO);

        if (log.isLoggable(Level.FINE)) {
            log.fine("Producer: createApp: RetailApp object created , "
                     + "send it to Store, name=[" + app.getName() + "]");
        }
        AppResponse response = null;
        int deadlineMs = 20 * 1000;

        try {
            // and send the request
            // create the request
            response = _producerBlockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                            .createApp(AppRequest.newBuilder()
                                            .setRetailApp(app)
                                            .build());

            id = response.getId();

            log.info("Producer: createApp,response received "
                     + "from grpc server id = " + id);

        } catch (StatusRuntimeException e) {
            handleStatusRunTimeException(e, "createSingleApp", null, null);
        }
        return id;
    }

    /**
     * @param name
     * @return
     */
    public String deleteSingleAppinStore(String name) throws Exception {

        // create the request
        DeleteRequest appReq = DeleteRequest.newBuilder().setAppName(name).build();
        DeleteResponse appResp = null;
        int deadlineMs = 20 * 1000;

        if (log.isLoggable(Level.FINE)) {
            log.fine("Producer: deleteApp, prodcuer ,request sent  to grpc server to remove app " + name);
        }
        try {
            appResp = _producerBlockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS).deleteApp(appReq);

            log.info("Producer: deleteApp,response received "
                     + "from grpc server ");

        } catch (StatusRuntimeException e) {
            handleStatusRunTimeException(e, "deleteSingleApp", null, null);
        }

        return appResp.getResult();

    }

    /**
     * @return
     */
    public DeleteAllRestResponse deleteMultiAppsinStore() throws Exception {

        DeleteAllRestResponse response = new DeleteAllRestResponse();
        int deadlineMs = 20 * 1000;

        if (log.isLoggable(Level.FINE)) {
            log.fine(
                     "Producer: deleteMultiAppsinStore ,request sent to grpc server to remove apps using server streaming");
        }

        try {
            _producerBlockingStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                            .deleteAllApps(Empty.getDefaultInstance())
                            .forEachRemaining(DeleteResponse -> {
                                response.addDeleteResults(DeleteResponse.getResult());
                            });

        } catch (StatusRuntimeException e) {
            handleStatusRunTimeException(e, "deleteMultiApps", null, null);
        }
        return response;
    }

    /**
     * @param reqPOJO
     * @param asyncServiceException
     * @return
     * @throws Exception
     */
    public ProducerRestResponse createMultiAppsinStore(MultiAppStructues reqPOJO, HandleExceptionsAsyncgRPCService asyncServiceException) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        ProducerRestResponse response = new ProducerRestResponse();
        StreamObserver<AppRequest> requestObserver = _producerAsyncStub.createApps(new StreamObserver<MultiCreateResponse>() {

            @Override
            public void onNext(MultiCreateResponse value) {
                // response from server
                // called only once
                log.info("Producer: createMultiAppsinStore:: Recvd a response from server " + value.getResult());
                // now send this response back to REST client				
                response.concatProducerResults(value.getResult());
            }

            @Override
            public void onError(Throwable t) {
                try {
                    handleStatusRunTimeException(null, "createMultiApps", t, asyncServiceException);
                } catch (Exception e) {
                    // nothing to do, exception set in HandleExceptionsFromgRPCService
                }
                log.info("Producer: createMultiAppsinStore:: completed response from server due to error");
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                // omComplete
                // called after onNext
                log.info("Producer: createMultiAppsinStore:: completed response from server ");
                latch.countDown();

            }

        });

        // now get the data from the input and send it to Store service
        reqPOJO.getStructureList().stream().forEach((appStructListItem) -> {

            // create the app structure to send to grpc server from the values sent by REST
            // request
            RetailApp app = createAppBuilder_forGRPC(appStructListItem);

            if (log.isLoggable(Level.FINE)) {
                log.fine("Producer: createMultiAppsinStore: RetailApp object created " + app.getName());
            }

            requestObserver.onNext(AppRequest.newBuilder().setRetailApp(app).build());

        });

        requestObserver.onCompleted();
        log.info("Producer: createMultiAppsinStore:: done from client");

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * @param e
     * @param methodName
     * @param t
     * @param asyncServiceException
     * @throws Exception
     */
    private void handleStatusRunTimeException(StatusRuntimeException e, String methodName, Throwable t,
                                              HandleExceptionsAsyncgRPCService asyncServiceException) throws Exception {

        Status status = null;
        boolean exception = false;

        if (e != null) {
            status = e.getStatus();
            // code from exception
            exception = true;
        } else if (t != null) {
            status = Status.fromThrowable(t);
            if (t instanceof StatusRuntimeException) {
                e = new StatusRuntimeException(status, ((StatusRuntimeException) t).getTrailers());
            }
        } else {
            // nothing to do, both e and t are null
            return;
        }

        Code code = status.getCode();
        String message = "An " + code + " status with exception is reported on " + methodName
                         + " request, status = " + status;
        if (e != null) {
            log.log(Level.SEVERE, message, e);
        }

        if (code == Status.Code.NOT_FOUND) {
            throw new NotFoundException(status.getDescription(), e);

        } else if (code == Status.Code.INVALID_ARGUMENT) {
            if (exception) {
                throw new InvalidArgException(status.getDescription(), e);
            } else {
                asyncServiceException.setArgException(new InvalidArgException(status.getDescription(), t));
            }

        } else if (code == Status.Code.ALREADY_EXISTS) {
            if (exception) {
                throw new AlreadyExistException(status.getDescription(), e);
            } else {
                asyncServiceException.setAlExException(new AlreadyExistException(status.getDescription(), t));
            }
            // handle cases 		
        } else if (code == Status.Code.DEADLINE_EXCEEDED) {
            throw e;

        } else if (code == Status.Code.INTERNAL) {
            throw e;

        } else if (code == Status.Code.UNKNOWN) {
            throw e;

        } else if (code == Status.Code.UNAUTHENTICATED) {
            throw e;

        } else if (code == Status.Code.PERMISSION_DENIED) {
            throw e;

        } else if (code == Status.Code.CANCELLED) {
            throw e;
        }

    }

    /**
     * @param reqPOJO
     * @return
     */
    private RetailApp createAppBuilder_forGRPC(AppStructure reqPOJO) {

        // set SecurityType enum in grpc from REST request
        SecurityType sType = getEnumValue(reqPOJO.getSecurityType());

        // set GenreType enum in grpc from REST request
        GenreType gType = getEnumValue(reqPOJO.getGenreType());

        // add Creator if not provided
        if (reqPOJO.getCreator() == null) {
            reqPOJO.setCreator(new com.ibm.testapp.g3store.restProducer.model.Creator());
        }
        if (reqPOJO.getCreator().getCompanyName() == null) {
            reqPOJO.getCreator().setCompanyName("TBD");
        }
        if (reqPOJO.getCreator().getEmail() == null) {
            reqPOJO.getCreator().setEmail("@TBD");
        }

        // set Creator in grpc from REST request
        com.ibm.test.g3store.grpc.Creator creat = Creator.newBuilder()
                        .setCompanyName(reqPOJO.getCreator().getCompanyName())
                        .setEmail(reqPOJO.getCreator().getEmail())
                        .build();

        // add Desc if not provided
        if (reqPOJO.getDesc() == null) {
            reqPOJO.setDesc("TBD");
        }
        if (reqPOJO.getFree() == null) {
            reqPOJO.setFree(true);
        }
        // build app Builder
        Builder appBuilder = RetailApp.newBuilder()
                        .setName(reqPOJO.getName())
                        .setDesc(reqPOJO.getDesc())
                        .setFree(reqPOJO.getFree())
                        .setSecurityType(sType)
                        .setGenreType(gType)
                        .setCreator(creat);

        // set Price List in grpc from REST request
        Price.Builder priceBuilder = Price.newBuilder();
        reqPOJO.getPriceList().stream().forEach((listItem) -> {

            log.info("Producer:createAppBuilder_forGRPC item = " + listItem);
            com.ibm.test.g3store.grpc.PurchaseType pType = getEnumValue(listItem.getPurchaseType());
            priceBuilder.setSellingPrice(listItem.getSellingPrice()).setPurchaseType(pType);
            // add in App
            appBuilder.addPrices(priceBuilder.build());
        });

        return appBuilder.build();

    }

    // Convert the enum types from REST response in grpc enum type for grpc request
    /**
     * @param sType
     * @return
     */
    private com.ibm.test.g3store.grpc.SecurityType getEnumValue(
                                                                com.ibm.testapp.g3store.restProducer.model.AppStructure.SecurityType sType) {

        if (sType == null) {
            return com.ibm.test.g3store.grpc.SecurityType.NO_SECURITY;
        }
        switch (sType) {
            case BASIC:
                return com.ibm.test.g3store.grpc.SecurityType.BASIC;

            case TOKEN_OAUTH2:
                return com.ibm.test.g3store.grpc.SecurityType.TOKEN_OAUTH2;

            case TOKEN_JWT:
                return com.ibm.test.g3store.grpc.SecurityType.TOKEN_JWT;

            case CUSTOM_AUTH:
                return com.ibm.test.g3store.grpc.SecurityType.CUSTOM_AUTH;

            default:
                return com.ibm.test.g3store.grpc.SecurityType.NO_SECURITY;

        }

    }

    // Convert the enum types from REST response in grpc enum type for grpc request
    /**
     * @param gType
     * @return
     */
    private com.ibm.test.g3store.grpc.GenreType getEnumValue(
                                                             com.ibm.testapp.g3store.restProducer.model.AppStructure.GenreType gType) {

        if (gType == null) {
            return com.ibm.test.g3store.grpc.GenreType.GAME;
        }

        switch (gType) {
            case GAME:
                return com.ibm.test.g3store.grpc.GenreType.GAME;

            case NEWS:
                return com.ibm.test.g3store.grpc.GenreType.NEWS;

            case SOCIAL:
                return com.ibm.test.g3store.grpc.GenreType.SOCIAL;

            case TECH:
                return com.ibm.test.g3store.grpc.GenreType.TECH;

            default:
                return com.ibm.test.g3store.grpc.GenreType.GAME;

        }
    }

    // Convert the enum types from REST response in grpc enum type for grpc request
    private com.ibm.test.g3store.grpc.PurchaseType getEnumValue(
                                                                com.ibm.testapp.g3store.restProducer.model.Price.PurchaseType pType) {

        if (pType == null) {
            return com.ibm.test.g3store.grpc.PurchaseType.BLUEPOINTS;
        }

        switch (pType) {
            case BLUEPOINTS:
                return com.ibm.test.g3store.grpc.PurchaseType.BLUEPOINTS;

            case CREDITCARD:
                return com.ibm.test.g3store.grpc.PurchaseType.CREDITCARD;

            case PAYAPL:
                return com.ibm.test.g3store.grpc.PurchaseType.PAYAPL;

            default:
                return com.ibm.test.g3store.grpc.PurchaseType.BLUEPOINTS;

        }
    }

}
