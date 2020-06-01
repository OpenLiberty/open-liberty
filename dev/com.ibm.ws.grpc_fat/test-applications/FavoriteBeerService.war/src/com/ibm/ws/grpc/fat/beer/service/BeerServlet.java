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

    private static Beer beerList[];
    private static int numBeers;

    // implementation of the BeerService service
    private static final class BeerServiceImpl extends BeerServiceGrpc.BeerServiceImplBase {

        // a no-arg constructor is required for Liberty to start this service automatically
        BeerServiceImpl() {
            beerList = new Beer[20];
            numBeers = 0;
        }

        //@Override
        @Override
        public void addBeer(Beer newBeer, StreamObserver<BeerResponse> responseObserver) {
            boolean notFound = true;
            // Lame test, only 20 beers allowed
            if (numBeers < 20) {
                int i = 0;
                while (i < numBeers) {
                    if (beerList[i].getBeerName().equals(newBeer.getBeerName())) {
                        notFound = false;
                        break;
                    }
                    i++;
                }
                if (notFound) {
                    beerList[i] = newBeer;
                    numBeers++;
                }
            }
            BeerResponse resp = BeerResponse.newBuilder().setDone(notFound).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();

        }

        //@Override
        @Override
        public void deleteBeer(Beer deleteBeer, StreamObserver<BeerResponse> responseObserver) {
            boolean notFound = true;
            int i = 0;
            while (i < numBeers) {
                if (beerList[i].getBeerName().equals(deleteBeer.getBeerName())) {
                    notFound = false;
                    break;
                }
                i++;
            }
            if (!notFound) {
                while (i < numBeers) {
                    beerList[i] = beerList[i + 1];
                    i++;
                }
                beerList[i] = null;
            }
            BeerResponse resp = BeerResponse.newBuilder().setDone(notFound).build();
            responseObserver.onNext(resp);
            responseObserver.onCompleted();

        }

        //@Override
        @Override
        public void getBestBeer(RequestedBeerType type, StreamObserver<Beer> responseObserver) {
            int i = 0;
            int highestRated = 0;
            Beer bestBeer = null;
            while (i < numBeers) {
                if (beerList[i].getBeerType().equals(type)) {
                    if (beerList[i].getBeerRating() > highestRated) {
                        bestBeer = beerList[i];
                    }
                }
                i++;
            }

            responseObserver.onNext(bestBeer);
            responseObserver.onCompleted();

        }

        //Get a list of all the beers
        //@Override
        public void getBeers(com.google.protobuf.Empty na, StreamObserver<Beer> responseObserver) {
            int i = 0;
            while (i < numBeers) {
                responseObserver.onNext(beerList[i]);
                i++;
            }
            responseObserver.onCompleted();
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