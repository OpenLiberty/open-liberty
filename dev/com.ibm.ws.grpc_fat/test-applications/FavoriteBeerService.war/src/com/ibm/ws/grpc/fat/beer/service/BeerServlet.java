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
package com.ibm.ws.grpc.fat.beer.service;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.grpc.examples.beer.BeerServiceGrpc;
import io.grpc.examples.beer.GetFavoriteBeer;
import io.grpc.examples.beer.BeerFavoriteResponse;
import io.grpc.stub.StreamObserver;

/**
 * A simple gRPC service that can be deployed on Liberty with the grpcServlet-1.0 feature.
 *
 * This implementation specifies a "user" role. Any calls to addBeer that are not authenticated
 * with a user in the "user" role will fail with an UNAUTHENTICATED status.
 */
@WebServlet(urlPatterns = { "/beer" }, asyncSupported = true)
public class BeerServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    

    // implementation of the BeerService service
    private static final class BeerServiceImpl extends BeerServiceGrpc.BeerServiceImplBase {

        // a no-arg constructor is required for Liberty to start this service automatically
        BeerServiceImpl() {
        }

        @Override
        public void addBeer(Beer newBeer, StreamObserver<BeerResponse> responseObserver) {

        }
        @Override
        public void deleteBeer(Beer newBeer, StreamObserver<BeerResponse> responseObserver) {

        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from BeerServlet!");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html");
        response.getWriter().println("Hello from BeerServlet!");
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}