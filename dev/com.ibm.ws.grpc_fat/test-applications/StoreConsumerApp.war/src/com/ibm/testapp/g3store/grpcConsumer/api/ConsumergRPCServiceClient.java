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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.test.g3store.grpc.AppConsumerServiceGrpc;
import com.ibm.testapp.g3store.grpcConsumer.security.TestAppCallCredentials;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * @author anupag
 *
 *         This class will help create a gRPC server connection secured and
 *         unsecured , blocking or async to StoreApp for gRPC requests.
 *
 *         This can be extended from any client i.e. REST or servlet client
 *
 */
public class ConsumergRPCServiceClient {

    private ManagedChannel _channel;

    private static Logger log = Logger.getLogger(ConsumergRPCServiceClient.class.getName());

    /**
     * 
     */
    private AppConsumerServiceGrpc.AppConsumerServiceBlockingStub _consumerBlockingStub;

    /**
     * 
     */
    private AppConsumerServiceGrpc.AppConsumerServiceStub _consumerAsyncStub;

    /**
     * 
     */
    public void stopService() {
        if (_channel != null)
            _channel.shutdownNow();
    }

    /**
     * @param address
     * @param port
     */
    public void startService_BlockingStub(String address, int port) {

        // create channel
        this.startService(address, port);
        // create service
        this.createBlockingStub(_channel);
    }

    /**
     * @param address
     * @param port
     */
    public void startService_AsyncStub(String address, int port) {
        // create channel
        this.startService(address, port);
        // create service
        this.createAsyncStub(_channel);
    }

    /**
     * @param address
     * @param port
     */
    private void startService(String address, int port) {

        // The client side gRPC code will use a gRPC ManagedChannel to connect to the
        // gRPC server side service.

        _channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();

        if (log.isLoggable(Level.FINE)) {
            log.finest("startService: " + _channel);
        }
    }

    /**
     * @param channel
     */
    private void createBlockingStub(ManagedChannel channel) {

        // build the stub for client to use to make gRPC calls to the server side
        // service
        set_consumerBlockingStub(AppConsumerServiceGrpc.newBlockingStub(channel));

    }

    /**
     * @param channel
     */
    private void createAsyncStub(ManagedChannel channel) {

        // build the stub for client to use to make gRPC calls to the server side
        // service
        // create async stub
        set_consumerAsyncStub(AppConsumerServiceGrpc.newStub(channel));

    }

    /**
     * @return
     */
    public AppConsumerServiceGrpc.AppConsumerServiceBlockingStub get_consumerService() {
        return _consumerBlockingStub;
    }

    /**
     * @return
     */
    public AppConsumerServiceGrpc.AppConsumerServiceStub get_asyncConsumerStub() {
        return _consumerAsyncStub;
    }

    /**
     * @param _consumerBlockingStub
     */
    private void set_consumerBlockingStub(AppConsumerServiceGrpc.AppConsumerServiceBlockingStub _consumerBlockingStub) {
        this._consumerBlockingStub = _consumerBlockingStub;
    }

    /**
     * @param _consumerAsyncStub
     */
    private void set_consumerAsyncStub(AppConsumerServiceGrpc.AppConsumerServiceStub _consumerAsyncStub) {
        this._consumerAsyncStub = _consumerAsyncStub;
    }

    /**
     * @param address
     * @param port
     * @param authHeader
     */
    protected void startServiceSecure_BlockingStub(String address, int port, String authHeader) {

//		channel = ManagedChannelBuilder.forAddress(address, port).useTransportSecurity().build();

        _channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();

        TestAppCallCredentials secureCred = new TestAppCallCredentials(authHeader);
        // build the stub for client to use to make gRPC calls to the server side
        // service
        set_consumerBlockingStub(AppConsumerServiceGrpc.newBlockingStub(_channel).withCallCredentials(secureCred));

        if (log.isLoggable(Level.FINE)) {
            log.finest("startServiceSecure: ");
        }

    }

}
