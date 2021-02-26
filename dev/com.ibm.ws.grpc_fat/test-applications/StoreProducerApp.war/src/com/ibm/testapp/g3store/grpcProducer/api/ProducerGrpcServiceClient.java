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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.test.g3store.grpc.AppProducerServiceGrpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ProducerGrpcServiceClient {

    private static Logger log = Logger.getLogger(ProducerGrpcServiceClient.class.getName());

    private ManagedChannel _channel;
    protected AppProducerServiceGrpc.AppProducerServiceBlockingStub _producerBlockingStub;

    /**
     * 
     */
    protected AppProducerServiceGrpc.AppProducerServiceStub _producerAsyncStub;

    /**
     * @param address
     * @param port
     */
    protected void startService(String address, int port) {

        // The client side gRPC code will use a gRPC ManagedChannel to connect to the
        // gRPC server side service.

        _channel = ManagedChannelBuilder.forAddress(address, port).usePlaintext().build();

        if (log.isLoggable(Level.FINE)) {
            log.finest("ProducergRPCServiceClient: startService: " + _channel);
        }
    }

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
    public boolean startService_BlockingStub(String address, int port) {

        // create channel
        this.startService(address, port);
        // create service
        if (_channel != null)
            this.createBlockingStub(_channel);
        else {
            log.severe("ProducergRPCServiceClient: Failed to build channel");
            return false;
        }
        return true;
    }

    /**
     * @param address
     * @param port
     */
    public void startService_AsyncStub(String address, int port) {
        // create channel
        this.startService(address, port);
        // create service
        if (_channel != null)
            this.createAsyncStub(_channel);
        else {
            log.severe("ProducergRPCServiceClient: Failed to build channel");
        }
    }

    /**
     * @param channel
     */
    private void createBlockingStub(ManagedChannel channel) {

        // build the stub for client to use to make gRPC calls to the server side
        // service
        set_producerBlockingStub(AppProducerServiceGrpc.newBlockingStub(channel));

    }

    /**
     * @param channel
     */
    private void createAsyncStub(ManagedChannel channel) {

        // build the stub for client to use to make gRPC calls to the server side
        // service
        // create async stub
        set_producerAsyncStub(AppProducerServiceGrpc.newStub(channel));

    }

    public AppProducerServiceGrpc.AppProducerServiceStub get_producerAsyncStub() {
        return _producerAsyncStub;
    }

    public void set_producerAsyncStub(AppProducerServiceGrpc.AppProducerServiceStub _producerAsyncStub) {
        this._producerAsyncStub = _producerAsyncStub;
    }

    public AppProducerServiceGrpc.AppProducerServiceBlockingStub get_producerBlockingStub() {
        return _producerBlockingStub;
    }

    public void set_producerBlockingStub(AppProducerServiceGrpc.AppProducerServiceBlockingStub _producerBlockingStub) {
        this._producerBlockingStub = _producerBlockingStub;
    }

}
