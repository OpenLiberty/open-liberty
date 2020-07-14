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
package com.ibm.testapp.g3store.grpcConsumer.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.ibm.test.g3store.grpc.AppNameRequest;
import com.ibm.test.g3store.grpc.NameResponse;
import com.ibm.test.g3store.grpc.Price;
import com.ibm.test.g3store.grpc.PriceResponse;
import com.ibm.test.g3store.grpc.RetailApp;
import com.ibm.test.g3store.grpc.RetailApp.Builder;
import com.ibm.testapp.g3store.exception.HandleExceptionsFromgRPCService;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.NotFoundException;
import com.ibm.testapp.g3store.restConsumer.model.AppNamewPriceListPOJO;
import com.ibm.testapp.g3store.restConsumer.model.PriceModel;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

/**
 * @author anupag
 * @version 1.0
 *
 *          This class is implementation of consumer APIs.
 *          The implementation will be called from REST APIs and Servlets.
 *          Each API will get the Grpc connection and call the Grpc service implementation.
 *
 */
public class ConsumerGrpcServiceClientImpl extends ConsumerGrpcServiceClient {

    private static Logger log = Logger.getLogger(ConsumerGrpcServiceClientImpl.class.getName());

    // gRPC client implementation(s)

    /**
     * @return
     * @throws NotFoundException
     */
    public List<String> getAllAppNameList() throws NotFoundException {

        List<String> nameList = null;
        try {
            // get the data back from grpc service
            NameResponse resp = get_consumerService()
                            .getAllAppNames(Empty.getDefaultInstance());

            if (log.isLoggable(Level.FINE)) {
                log.fine("Consumer: getAllAppNames: Received respnse, number of apps = " + resp.getNamesCount());
            }

            nameList = resp.getNamesList();

        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {
                e.printStackTrace();
                throw new NotFoundException(e.getMessage());
            }

        } catch (Exception e) {
            log.severe("Consumer: getAllAppNames : An exception is reported on getAllNames request");
            e.printStackTrace();
        }

        return nameList;
    }

    /**
     * @param appName
     * @return
     */
    public String getAppJSONStructure(String appName) throws InvalidArgException {

        AppNameRequest appReq = AppNameRequest.newBuilder().setName(appName).build();

        if (log.isLoggable(Level.FINE)) {
            log.finest("Consumer: getAppInfo: appReq for name " + appReq.getName());
        }
        // get results from rpc call
        // This is a Unary call

        String appStruct_JSONString = null;
        RetailApp appStruct_gRPCResponse = null;

        try {
            appStruct_gRPCResponse = get_consumerService().getAppInfo(appReq).getRetailApp();

        } catch (StatusRuntimeException e) {

            if (e.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
                log.severe("Consumer: An exception is reported on getAppInfo request, status = " + e.getStatus());
                e.printStackTrace();
                throw new InvalidArgException(e.getMessage());
            }
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Consumer: getAppInfo:  Received response to get AppInfo " + appStruct_gRPCResponse.getDesc());
        }

        // and respond as JSON
        // get the RetailApp Builder
        Builder b = RetailApp.newBuilder().mergeFrom(appStruct_gRPCResponse);
        if (log.isLoggable(Level.FINE)) {
            log.fine("Consumer: getAppInfo: from builder " + b.getName());
        }

        try {
            // need protobuf-java-util.jar for this
            appStruct_JSONString = JsonFormat.printer()
                            .includingDefaultValueFields()
                            .preservingProtoFieldNames()
                            .print(b);

        } catch (InvalidProtocolBufferException e) {
            log.severe("Consumer: getAppInfo: An exception is reported on updating to JSON structure");
            e.printStackTrace();
        }

        return appStruct_JSONString;
    }

    /**
     * @param appNames
     * @return
     */
    public List<AppNamewPriceListPOJO> getAppswPrices(List<String> appNames) {

        final CountDownLatch latch = new CountDownLatch(1);

        // Create a REST response , add all the responses in one list since REST request
        // does not support streaming , so we will need to add up all the streamed
        // responses and
        // send it as one response.

        List<AppNamewPriceListPOJO> listOfAppNames_w_PriceList = new ArrayList<AppNamewPriceListPOJO>(); // list

        HandleExceptionsFromgRPCService handleException = new HandleExceptionsFromgRPCService();

        // call rpc getPrices
        // This is BIDI streaming call

        StreamObserver<AppNameRequest> requestObserver = get_asyncConsumerStub()
                        .getPrices(new StreamObserver<PriceResponse>() {

                            @Override
                            public void onNext(PriceResponse gRPCResponse) {

                                // response from server

                                if (log.isLoggable(Level.FINE)) {
                                    log.fine("Consumer: getAppswPrices: Recvd a response from server ");
                                    log.fine("getPricesList = " + gRPCResponse.getPricesList()); // will not print the default enum value
                                }

                                // Now convert from grpc List<Price> to List<PriceModel>
                                AppNamewPriceListPOJO app_PriceBean = convert_gRPCPriceList_To_POJOPriceList(gRPCResponse);

                                // add the bean in the List
                                listOfAppNames_w_PriceList.add(app_PriceBean);

                                if (log.isLoggable(Level.FINE)) {
                                    log.fine("Consumer: getAppswPrices:  appNamewPriceList size = " + listOfAppNames_w_PriceList.size());
                                }

                            }

                            @Override
                            public void onError(Throwable t) {

                                Status status = Status.fromThrowable(t);

                                if (status.getCode() == Status.Code.INVALID_ARGUMENT) {
                                    log.log(Level.WARNING, "Consumer: getAppswPrices Failed: {0}", status);

                                    handleException.setArgException(new InvalidArgException(t));
                                } else {
                                    if (status.getCode() == Status.Code.NOT_FOUND) {
                                        log.log(Level.WARNING, "Consumer: getAppswPrices Failed: {0}", status);
                                        handleException.setNfException(new NotFoundException(t));
                                    }
                                }

                                latch.countDown();

                            }

                            @Override
                            public void onCompleted() {
                                if (log.isLoggable(Level.FINE)) {
                                    log.fine("Consumer: getAppswPrices: completed response from server ");
                                }

                            }

                        });

        try {

            appNames.forEach(name -> {

                if (log.isLoggable(Level.FINE)) {
                    log.fine("Consumer: getAppswPrices: Sending name: " + name);
                }
                requestObserver.onNext(AppNameRequest.newBuilder().setName(name).build());

                if (latch.getCount() == 0) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine("Consumer: getAppswPrices: Sending name stopped, RPC completed or errored before we finished sending.");
                    }
                    // Sending further requests won't error, but they will just be thrown away.
                    return;
                }

//			try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
            });

        } catch (RuntimeException e) {
            // Cancel RPC
            requestObserver.onError(e);
            throw e;
        }

        requestObserver.onCompleted();
        if (log.isLoggable(Level.FINE)) {
            log.fine("Consumer: getAppswPrices: done from client");
        }

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {

            e.printStackTrace();
        }

        return listOfAppNames_w_PriceList;
    }

    // AppNamewPriceList is the POJO for App with its price list

    /**
     * @param value
     * @return
     */
    private AppNamewPriceListPOJO convert_gRPCPriceList_To_POJOPriceList(PriceResponse value) {

        // get value from gRPC response
        List<Price> prices = value.getPricesList();

        List<PriceModel> priceRestList = new ArrayList<PriceModel>();

        prices.stream().forEach((listItem) -> {

            PriceModel priceRest = new PriceModel();
            priceRest.setPurchaseType(getEnumValue(listItem.getPurchaseType()));
            if (log.isLoggable(Level.FINE)) {
                log.fine("Consumer: convert_gRPCPriceList: the purchaseType: " + priceRest.getPurchaseType());
            }
            priceRest.setSellingPrice(listItem.getSellingPrice());

            priceRestList.add(priceRest);

        });

        // set in the app_PriceList POJO
        AppNamewPriceListPOJO app_PriceListBean = new AppNamewPriceListPOJO();
        if (log.isLoggable(Level.FINE)) {
            log.fine("Consumer: convert_gRPCPriceList:  name to be added in the list" + value.getName());
        }
        app_PriceListBean.setAppName(value.getName());
        if (log.isLoggable(Level.FINE)) {
            log.fine("Consumer: convert_gRPCPriceList: in the list = " + priceRestList.get(0));
        }
        app_PriceListBean.setPrices(priceRestList);

        return app_PriceListBean;

    }

    // Convert the enum types from REST response in grpc enum type for grpc request
    private com.ibm.testapp.g3store.restConsumer.model.PriceModel.PurchaseType getEnumValue(
                                                                                            com.ibm.test.g3store.grpc.PurchaseType pType) {

        switch (pType) {
            case BLUEPOINTS:
                return com.ibm.testapp.g3store.restConsumer.model.PriceModel.PurchaseType.BLUEPOINTS;

            case CREDITCARD:
                return com.ibm.testapp.g3store.restConsumer.model.PriceModel.PurchaseType.CREDITCARD;

            case PAYAPL:
                return com.ibm.testapp.g3store.restConsumer.model.PriceModel.PurchaseType.PAYAPL;

            default:
                return com.ibm.testapp.g3store.restConsumer.model.PriceModel.PurchaseType.BLUEPOINTS;

        }
    }

}
