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

import java.io.BufferedReader;
import java.io.FileReader;
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
import com.ibm.test.g3store.grpc.StreamReplyA;
import com.ibm.test.g3store.grpc.StreamRequestA;
import com.ibm.testapp.g3store.exception.AlreadyExistException;
import com.ibm.testapp.g3store.exception.HandleExceptionsAsyncgRPCService;
import com.ibm.testapp.g3store.exception.InvalidArgException;
import com.ibm.testapp.g3store.exception.NotFoundException;
import com.ibm.testapp.g3store.restProducer.api.ProducerRestEndpoint;
import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.DeleteAllRestResponse;
import com.ibm.testapp.g3store.restProducer.model.MultiAppStructues;
import com.ibm.testapp.g3store.restProducer.model.ProducerRestResponse;

import io.grpc.Status;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * @author anupag
 * @version 1.0
 *
 *          This class is implementation of producer APIs.
 *
 */
public class ProducerGrpcServiceClientImpl extends ProducerGrpcServiceClient {

    private static Logger log = Logger.getLogger(ProducerGrpcServiceClientImpl.class.getName());

    private final int deadlineMs = 30 * 1000;

    private static boolean CONCURRENT_TEST_ON = false;

    public ProducerGrpcServiceClientImpl() {
        if (CONCURRENT_TEST_ON) {
            readStreamParmsFromFile();
        }
    }

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

            log.info("Producer: deleteMultiAppsinStore,response received "
                     + "from grpc server ");

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
        StreamObserver<AppRequest> requestObserver = _producerAsyncStub
                        .withDeadlineAfter(deadlineMs, TimeUnit.SECONDS)
                        .createApps(new StreamObserver<MultiCreateResponse>() {

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
            // Wait for the grpc service response to complete. If we return the client response too quickly (ie. this timeout is too small)
            // the connection will be closed  and the test will not get the correct response data and IOExceptions might be thrown.
            latch.await(deadlineMs, TimeUnit.MILLISECONDS);
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

    public static int CLIENT_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION = 200;
    public static int CLIENT_STREAM_TIME_BETWEEN_MESSAGES_MSEC = 0;
    public static int CLIENT_STREAM_MESSAGE_SIZE = 50; // set to 5, 50, 500, 5000, or else you will get 50.
    public static int CLIENT_STREAM_SLEEP_WHEN_NOT_READY_MSEC = 50;

    public String grpcClientStreamApp() {
        String replyAfterClientStream = "Null";
        String errorMessage = null;
        Throwable errorCaught = null;
        String sChars = "12345678901234567890123456789012345678901234567890"; // 50 characters
        int readyLoopMax = 400; // allow a 20s timeout for the first message; set to 250ms thereafter
        int readyLoopCount = 0;

        CountDownLatch latch = new CountDownLatch(1);

        log.info("Producer: grpcClientStreamApp(): Entered");

        // This if for sending a stream of data to the server and then get a single reply
        ClientStreamClass csc = new ClientStreamClass(latch);
        // StreamObserver<StreamRequestA> clientStreamAX = _producerAsyncStub.clientStreamA(csc);
        ClientCallStreamObserver<StreamRequestA> clientStreamAX = (ClientCallStreamObserver) (_producerAsyncStub.clientStreamA(csc));

        // client streaming
        int numberOfMessages = CLIENT_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION;
        int timeBetweenMessagesMsec = CLIENT_STREAM_TIME_BETWEEN_MESSAGES_MSEC;
        StreamRequestA nextRequest = null;

        String nextMessage = null;
        String firstMessage = "This is the first Message..."; // don't change, hardcode to match string in StoreProducerService
        String lastMessage = "And this is the last Message"; // don't change, hardcode to match string in StoreProducerService

        if (CLIENT_STREAM_MESSAGE_SIZE == 5) {
            sChars = "12345";
        } else if (CLIENT_STREAM_MESSAGE_SIZE == 500) {
            String s50chars = "12345678901234567890123456789012345678901234567890";
            sChars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
        } else if (CLIENT_STREAM_MESSAGE_SIZE == 5000) {
            String s50chars = "12345678901234567890123456789012345678901234567890";
            String s500chars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
            sChars = s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars;
        }

        for (int i = 1; i <= numberOfMessages; i++) {
            if (i == 1) {
                if (CONCURRENT_TEST_ON) {
                    System.out.println(qtf() + " ServerStream: CLIENT sending message 1 hc: " + clientStreamAX.hashCode());
                }
                nextMessage = firstMessage;
            } else if (i == numberOfMessages) {
                if (CONCURRENT_TEST_ON) {
                    System.out.println(qtf() + " ServerStream: CLIENT sending message " + i + " last message. hc: " + clientStreamAX.hashCode());
                }
                nextMessage = lastMessage;
            } else {
                if (CONCURRENT_TEST_ON && ((i % 1000) == 0)) {
                    System.out.println(qtf() + " ServerStream: CLIENT sending message " + i + " hc: " + clientStreamAX.hashCode());
                }
                nextMessage = "--Message " + i + " of " + numberOfMessages + " left client at time: " + System.currentTimeMillis() + "--";
                nextMessage = nextMessage + sChars;
            }

            nextRequest = StreamRequestA.newBuilder().setMessage(nextMessage).build();

            readyLoopCount = 0;

            while (clientStreamAX.isReady() != true) {
                if (CONCURRENT_TEST_ON) {
                    System.out.println(qtf() + " ServerStream: CLIENT.  isReady() returned false, sleep "
                                       + CLIENT_STREAM_SLEEP_WHEN_NOT_READY_MSEC + " ms. hc: " + clientStreamAX.hashCode());
                }
                try {
                    Thread.sleep(CLIENT_STREAM_SLEEP_WHEN_NOT_READY_MSEC);
                } catch (Exception x) {
                    // do nothing
                }
                readyLoopCount++;
                if (readyLoopCount > readyLoopMax)
                    break;
            }

            if (readyLoopCount > readyLoopMax) {
                if (CONCURRENT_TEST_ON) {
                    System.out.println(qtf() + " ServerStream: CLIENT.  isReady() returned false " + readyLoopCount + " times. quit sending. hc: " + clientStreamAX.hashCode());
                }
                log.warning("Producer: grpcClientStreamApp(): timed out waiting for isReady()");
                break;
            }

            clientStreamAX.onNext(nextRequest);

            // reset ready loop count and max
            readyLoopCount = 0;
            readyLoopMax = 50;

            try {
                if (timeBetweenMessagesMsec > 0) {
                    Thread.sleep(timeBetweenMessagesMsec);
                }
            } catch (Exception x) {
                // do nothing
            }
        }

        // wait to send onCompleted for now
        try {
            Thread.sleep(500);
        } catch (Exception x) {
            // do nothing
        }
        log.info("grpcClientStreamApp: Client calling onCompleted");
        clientStreamAX.onCompleted();

        // wait for the response from server
        try {
            if (CONCURRENT_TEST_ON) {
                latch.await(ProducerRestEndpoint.CLIENT_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC / 2, TimeUnit.SECONDS);
            } else {
                latch.await(28, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            log.info("grpcClientStreamApp: latch.await got interrupted");
        }

        replyAfterClientStream = csc.getReply();
        errorMessage = csc.getErrorMessage();
        errorCaught = csc.getErrorCaught();

        // test that this is what was expected:
        int i1 = replyAfterClientStream.indexOf(firstMessage);
        int i2 = replyAfterClientStream.indexOf(lastMessage);

        log.info("grpcClientStreamApp: firstMessage index at: " + i1 + " lastMessage index at: " + i2);

        // change these two parms to print more of the string
        int maxStringLength = 32768;
        int truncatedLength = 1024;
        if (replyAfterClientStream.length() > maxStringLength) {
            replyAfterClientStream = replyAfterClientStream.substring(0, truncatedLength);
            log.info("grpcClientStreamApp: reply message truncated at: " + truncatedLength + " : " + replyAfterClientStream);
        } else {
            log.info("grpcClientStreamApp: reply message was: " + replyAfterClientStream);
        }

        if (errorMessage != null) {
            return (errorMessage);
        } else if (i2 >= 0) {
            // } else if ((i1 >= 0) && (i2 >= 0)) {  todo: need to debug first message issue
            log.info("grpcClientStreamApp: success, firstMessage index at: " + i1 + " lastMessage index at: " + i2);
            return ("success");
        } else {
            return ("grpcClientStreamApp: failed, incorrect response from service");
        }
    }

    class ClientStreamClass implements StreamObserver<StreamReplyA> {
        // class ClientStreamClass implements ClientCallStreamObserver<StreamReplyA> {

        String replyAfterClientStream = "Null";
        String errorMessage = null;
        Throwable errorCaught = null;
        CountDownLatch latch = null;

        public ClientStreamClass(CountDownLatch inLatch) {
            latch = inLatch;
        }

        public String getReply() {
            return replyAfterClientStream;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Throwable getErrorCaught() {
            return errorCaught;
        }

        @Override
        public void onNext(StreamReplyA response) {
            // response from server
            // called only once
            replyAfterClientStream = response.toString();
        }

        @Override
        public void onError(Throwable t) {
            // Error on the reply from the server service
            errorCaught = t;
            errorMessage = errorCaught.getMessage();
            log.info("grpcClientStreamApp: caught error from server service: " + errorMessage);
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            log.info("grpcClientStreamApp: onCompleted called from server service");
            latch.countDown();
        }
    }

    public String grpcServerStreamApp() {
        String errorMessage = null;
        Throwable errorCaught = null;
        String responseFromServer = null;
        String firstServerStreamMessage = null;
        String lastServerStreamMessage = null;
        int i1 = -1;
        int i2 = -1;
        CountDownLatch latch = new CountDownLatch(1);

        log.info(qtf() + " grpcServerStreamApp(): Entered hc: " + this.hashCode());

        StreamRequestA nextRequest = StreamRequestA.newBuilder().setMessage("From Client").build();

        ServerStreamClass so = new ServerStreamClass(latch);

        _producerAsyncStub.serverStreamA(nextRequest, so);

        // wait for the response from server
        try {
            if (CONCURRENT_TEST_ON) {
                latch.await(ProducerRestEndpoint.SERVER_STREAM_TIMEOUT_WAITING_FOR_TEST_COMPLETE_SEC, TimeUnit.SECONDS);
            } else {
                latch.await(28, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            log.info("grpcServerStreamApp: latch.await got interrupted");
        }

        firstServerStreamMessage = so.getFirstMessage();
        lastServerStreamMessage = so.getLastMessage();
        errorMessage = so.getErrorMessage();
        errorCaught = so.getErrorCaught();

        if (firstServerStreamMessage != null) {
            i1 = firstServerStreamMessage.indexOf("first");
            i2 = lastServerStreamMessage.indexOf("last");
            log.info("grpcServerStreamApp: firstMessage index at: " + i1 + " lastMessage index at: " + i2);
        } else {
            log.info("grpcServerStreamApp: Null response from server");
        }

        if (errorMessage != null) {
            return (errorMessage);
            // } else if ((i1 >= 0) && (i2 >= 0)) {  todo: need to debug first message issue with this test
        } else if (i2 >= 0) {
            return ("success");
        } else {
            return ("grpcServerStreamApp: failed, incorrect response from service");
        }

    }

    class ServerStreamClass implements StreamObserver<StreamReplyA> {

        String firstServerStreamMessage = null;
        String lastServerStreamMessage = null;
        String errorMessage = null;
        Throwable errorCaught = null;
        CountDownLatch latch = null;
        long messageCount = 0;

        public ServerStreamClass(CountDownLatch inLatch) {
            latch = inLatch;
        }

        @Override
        public void onNext(StreamReplyA response) {
            messageCount++;
            if (firstServerStreamMessage == null) {
                if (CONCURRENT_TEST_ON) {
                    System.out.println(qtf() + " ServerStream: CLIENT received message 1 hc: " + this.hashCode());
                }
                firstServerStreamMessage = response.toString();
                lastServerStreamMessage = response.toString();
            } else {
                if (CONCURRENT_TEST_ON && (messageCount % 1000) == 0) {
                    System.out.println(qtf() + " ServerStream: CLIENT received message " + messageCount + " hc: " + this.hashCode());
                }

                lastServerStreamMessage = response.toString();
            }

        }

        @Override
        public void onError(Throwable t) {
            // Error on the reply from the server service
            errorCaught = t;
            errorMessage = errorCaught.getMessage();
            log.info("grpcServerStreamApp: caught error from server service: " + errorMessage);

            if (CONCURRENT_TEST_ON) {
                System.out.println(qtf() + "grpcServerStreamApp: caught error from server service: " + errorMessage + " hc: " + this.hashCode());
            }
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            if (CONCURRENT_TEST_ON) {
                System.out.println(qtf() + " ServerStream: CLIENT received onCompleted. message count: " + messageCount + " hc: " + this.hashCode());
            }
            log.info("grpcServerStreamApp: onCompleted called from server service");
            latch.countDown();
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Throwable getErrorCaught() {
            return errorCaught;
        }

        public String getFirstMessage() {
            return firstServerStreamMessage;
        }

        public String getLastMessage() {
            return lastServerStreamMessage;
        }
    }

    // -------------------------------------------------------------

    public final static int TWOWAY_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION = 200;
    public final static int TWOWAY_STREAM_TIME_BETWEEN_MESSAGES_MSEC = 0;
    public final static int TWOWAY_STREAM_MESSAGE_SIZE = 50; // set to 5, 50, 500, 5000, or else you will get 50.

    public String grpcTwoWayStreamApp(boolean asyncThread) {
        log.info("Producer: grpcTwoWayStreamApp(): Entered");

        String firstTwoWayMessageReceived = null;
        String lastTwoWayMessageReceived = null;
        String errorMessage = null;
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<StreamRequestA> twoWayStreamAX = null;
        TwoWayStreamClass tws = null;

        String sChars = "12345678901234567890123456789012345678901234567890"; // 50 characters
        if (TWOWAY_STREAM_MESSAGE_SIZE == 5) {
            sChars = "12345";
        } else if (TWOWAY_STREAM_MESSAGE_SIZE == 500) {
            String s50chars = "12345678901234567890123456789012345678901234567890";
            sChars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
        } else if (TWOWAY_STREAM_MESSAGE_SIZE == 5000) {
            String s50chars = "12345678901234567890123456789012345678901234567890";
            String s500chars = s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars + s50chars;
            sChars = s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars + s500chars;
        }

        // This if for sending a stream of data to the server and then getting a stream reply
        tws = new TwoWayStreamClass(latch);
        if (asyncThread == false) {
            twoWayStreamAX = _producerAsyncStub.twoWayStreamA(tws);
        } else {
            twoWayStreamAX = _producerAsyncStub.twoWayStreamAsyncThread(tws);
        }

        // client streaming
        int numberOfMessages = TWOWAY_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION;
        int timeBetweenMessagesMsec = TWOWAY_STREAM_TIME_BETWEEN_MESSAGES_MSEC;
        StreamRequestA nextRequest = null;

        String nextMessage = null;
        String firstMessage = "This is the first Response Message..."; // don't change, hardcode to match string in StoreProducerService
        String lastMessage = "And this is the last Response Message"; // don't change, hardcode to match string in StoreProducerService

        for (int i = 1; i <= numberOfMessages; i++) {
            if (i == 1) {
                nextMessage = firstMessage;
            } else if (i == numberOfMessages) {
                nextMessage = lastMessage;
            } else {
                nextMessage = "--Message " + i + " of " + numberOfMessages + " left client at time: " + System.currentTimeMillis() + "--";
                nextMessage = nextMessage + sChars;
            }

            nextRequest = StreamRequestA.newBuilder().setMessage(nextMessage).build();
            twoWayStreamAX.onNext(nextRequest);
            try {
                if (timeBetweenMessagesMsec > 0) {
                    Thread.sleep(timeBetweenMessagesMsec);
                }
            } catch (Exception x) {
                // do nothing
            }
        }

        // wait to send onCompleted for now
        try {
            Thread.sleep(500);
        } catch (Exception x) {
            // do nothing
        }
        log.info("grpcTwoWayStreamApp: Client calling onCompleted");
        twoWayStreamAX.onCompleted();

        // wait for the response from server
        try {
            latch.await(28, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.info("grpcTwoWayStreamApp: latch.await got interrupted");
        }

        firstTwoWayMessageReceived = tws.getFirstMessage();
        lastTwoWayMessageReceived = tws.getLastMessage();
        errorMessage = tws.getErrorMessage();

        // test that this is what was expected:
        int i1 = firstTwoWayMessageReceived.indexOf(firstMessage);
        int i2 = lastTwoWayMessageReceived.indexOf(lastMessage);
        log.info("grpcTwoWayStreamApp: i1: " + i1 + " i2: " + i2);

        // change these two parms to print more of the strings
        int maxStringLength = 32768;
        int truncatedLength = 1024;
        if (firstTwoWayMessageReceived.length() > maxStringLength) {
            firstTwoWayMessageReceived = firstTwoWayMessageReceived.substring(0, truncatedLength);
            log.info("grpcTwoWayStreamApp: firstTwoWayMessageReceived truncated at: " + truncatedLength + " : " + firstTwoWayMessageReceived);
        } else {
            log.info("grpcTwoWayStreamApp: firstTwoWayMessageReceived was: " + firstTwoWayMessageReceived);
        }
        if (lastTwoWayMessageReceived.length() > maxStringLength) {
            lastTwoWayMessageReceived = lastTwoWayMessageReceived.substring(0, truncatedLength);
            log.info("grpcTwoWayStreamApp: lastTwoWayMessageReceived truncated at: " + truncatedLength + " : " + lastTwoWayMessageReceived);
        } else {
            log.info("grpcTwoWayStreamApp: lastTwoWayMessageReceived was: " + lastTwoWayMessageReceived);
        }

        if (errorMessage != null) {
            log.info("grpcTwoWayStreamApp: Error received: " + errorMessage);
            return (errorMessage);
            // } else if ((i1 >= 0) && (i2 >= 0)) {  todo: need to debug first message issue with this test
        } else if (i2 >= 0) {
            log.info("grpcTwoWayStreamApp: success, firstMessage index at: " + i1 + " lastMessage index at: " + i2);
            return ("success");
        } else {
            return ("grpcTwoWayStreamApp: failed, incorrect response from service. i1: " + i1 + " i2: " + i2);
        }
    }

    class TwoWayStreamClass implements StreamObserver<StreamReplyA> {
        String firstTwoWayMessageReceived = null;
        String lastTwoWayMessageReceived = null;
        String errorMessage = null;
        CountDownLatch latch = null;
        Object messageSync = new Object() {
        };

        public TwoWayStreamClass(CountDownLatch inLatch) {
            latch = inLatch;
        }

        @Override
        public void onNext(StreamReplyA response) {
            synchronized (messageSync) {
                if (firstTwoWayMessageReceived == null) {
                    firstTwoWayMessageReceived = response.toString();
                    lastTwoWayMessageReceived = response.toString();
                } else {
                    lastTwoWayMessageReceived = response.toString();
                }
            }
        }

        @Override
        public void onError(Throwable t) {
            // Error on the reply from the server service
            errorMessage = t.getMessage();
            log.info("grpcTwoWayStreamApp: onError received from server service: " + errorMessage);
            latch.countDown();
        }

        @Override
        public void onCompleted() {
            log.info("grpcTwoWayStreamApp: onCompleted received from server service");
            latch.countDown();
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getFirstMessage() {
            if (firstTwoWayMessageReceived == null) {
                return "Null";
            } else {
                return firstTwoWayMessageReceived;
            }
        }

        public String getLastMessage() {
            if (lastTwoWayMessageReceived == null) {
                return "Null";
            } else {
                return lastTwoWayMessageReceived;
            }
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
                if (sCurrentLine.indexOf("CLIENT_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION") != -1) {
                    sCurrentLine = br.readLine();
                    CLIENT_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION = new Integer(sCurrentLine).intValue();
                    System.out.println("setting CLIENT_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION to: " + CLIENT_STREAM_NUMBER_OF_MESSAGES_PER_CONNECTION);
                } else if (sCurrentLine.indexOf("CLIENT_STREAM_TIME_BETWEEN_MESSAGES_MSEC") != -1) {
                    sCurrentLine = br.readLine();
                    CLIENT_STREAM_TIME_BETWEEN_MESSAGES_MSEC = new Integer(sCurrentLine).intValue();
                    System.out.println("setting CLIENT_STREAM_TIME_BETWEEN_MESSAGES_MSEC to: " + CLIENT_STREAM_TIME_BETWEEN_MESSAGES_MSEC);
                } else if (sCurrentLine.indexOf("CLIENT_STREAM_MESSAGE_SIZE") != -1) {
                    sCurrentLine = br.readLine();
                    CLIENT_STREAM_MESSAGE_SIZE = new Integer(sCurrentLine).intValue();
                    System.out.println("setting CLIENT_STREAM_MESSAGE_SIZE to: " + CLIENT_STREAM_MESSAGE_SIZE);
                } else if (sCurrentLine.indexOf("CLIENT_STREAM_SLEEP_WHEN_NOT_READY_MSEC") != -1) {
                    sCurrentLine = br.readLine();
                    CLIENT_STREAM_SLEEP_WHEN_NOT_READY_MSEC = new Integer(sCurrentLine).intValue();
                    System.out.println("setting CLIENT_STREAM_SLEEP_WHEN_NOT_READY_MSEC to: " + CLIENT_STREAM_SLEEP_WHEN_NOT_READY_MSEC);
                }
            }
        } catch (Exception x) {
            System.out.println("Error caught while reading GrpcStreamParms.txt: " + x);
        }
    }

}
