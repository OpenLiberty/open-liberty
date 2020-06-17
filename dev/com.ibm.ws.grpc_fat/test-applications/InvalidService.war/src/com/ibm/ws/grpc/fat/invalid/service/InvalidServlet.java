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
package com.ibm.ws.grpc.fat.invalid.service;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.ibm.ws.grpc.fat.beer.service.BeerServiceGrpc;
import com.ibm.ws.grpc.fat.beer.service.Beer;
import com.ibm.ws.grpc.fat.beer.service.BeerResponse;
import com.ibm.ws.grpc.fat.beer.service.RequestedBeerType;


import io.grpc.stub.StreamObserver;

/**
 * A simple gRPC service that can be deployed on Liberty with the grpcServlet-1.0 feature.
 * This service is invalid and is used in a negative test.
 * Grpc services must have a constructor without parameters to be valid.
 *
 * This implementation specifies a "user" role. Any calls to addBeer that are not authenticated
 * with a user in the "user" role will fail with an UNAUTHENTICATED status.
 */
@WebServlet(urlPatterns = { "/invalid" }, asyncSupported = true)
public class InvalidServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
<<<<<<< HEAD
    
=======

>>>>>>> cf6c2762e2... 12609: Add grpc server config tests
    // implementation of the InvalidService service
    private static final class InvalidGrpcServiceImpl extends BeerServiceGrpc.BeerServiceImplBase {

        // a no-arg constructor is required for Liberty to start this grpc service automatically
        // Make this gprc service invalid by having a constructor that takes an arg
        InvalidGrpcServiceImpl(int i) {
<<<<<<< HEAD
           
=======

>>>>>>> cf6c2762e2... 12609: Add grpc server config tests
        }

        @Override
        public void addBeer(Beer newBeer, StreamObserver<BeerResponse> responseObserver) {

        }

        @Override
        public void deleteBeer(Beer deleteBeer, StreamObserver<BeerResponse> responseObserver) {

        }

        @Override
        public void getBestBeer(RequestedBeerType type, StreamObserver<Beer> responseObserver) {


        }

        //Get a list of all the beers
        @Override
        public void getBeers(com.google.protobuf.Empty na, StreamObserver<Beer> responseObserver) {
<<<<<<< HEAD
 
=======

>>>>>>> cf6c2762e2... 12609: Add grpc server config tests
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from InvalidGrpcServlet!");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from InvalidGrpcServlet!");
    }

    @Override
    public void destroy() {
        super.destroy();
    }

<<<<<<< HEAD
}
=======
}
>>>>>>> cf6c2762e2... 12609: Add grpc server config tests
