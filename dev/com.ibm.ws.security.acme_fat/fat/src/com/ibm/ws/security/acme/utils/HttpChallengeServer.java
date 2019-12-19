/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.utils;

import java.io.IOException;
import java.net.BindException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.junit.Assert;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.internal.AcmeClient;

/**
 * A simple HTTP server that will use authorizations stored in the
 * {@link AcmeClient} to respond to HTTP-01 challenges from the ACME CA. This
 * class is useful for testing {@link AcmeClient} outside of Liberty.
 */
public class HttpChallengeServer {
	private static final String ROOT_CONTEXT = "/.well-known/acme-challenge/";
	private HttpServer server = null;
	private AcmeClient acmeClient = null;
	private int listenerPort;

	/**
	 * Construct a new {@link HttpChallengeServer} that will bind to the
	 * specified port.
	 * 
	 * @param listenerPort
	 *            The port to bind and listen for HTTP requests on.
	 */
	public HttpChallengeServer(int listenerPort) {
		this.listenerPort = listenerPort;
	}

	/**
	 * Start the server.
	 * 
	 * @throws IOException
	 *             If we could not start the server.
	 * 
	 */
	public void start() throws IOException {
		if (server != null) {
			throw new IllegalStateException("Server is already running.");
		}

		boolean retry = true;
		int remainingRetries = 20;
		while (retry && remainingRetries > 0) {
			retry = false;
			try {
				server = ServerBootstrap.bootstrap().registerHandler("*", new HttpRequestHandler() {

					@Override
					public void handle(HttpRequest request, HttpResponse response, HttpContext context)
							throws HttpException, IOException {

						Log.info(HttpChallengeServer.class, "handle", "Received request: " + request.toString());

						String uri = request.getRequestLine().getUri();
						if (!uri.startsWith(ROOT_CONTEXT)) {
							response.setStatusCode(HttpStatus.SC_NOT_FOUND);
						} else {

							if (acmeClient == null) {
								response.setStatusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
							} else {

								String challenge = uri.replace(ROOT_CONTEXT, "");
								Log.info(HttpChallengeServer.class, "handle",
										"Processing HTTP-01 Challenge: " + challenge);

								/*
								 * Make sure we have a token for the challenge.
								 * If so, send it back.
								 */
								String token = acmeClient.getHttp01Authorization(challenge);
								Log.info(HttpChallengeServer.class, "handle",
										"Found HTTP-01 Challenge Token: " + token);
								if (token == null) {
									response.setStatusCode(HttpStatus.SC_NOT_FOUND);
								} else {
									response.setStatusCode(HttpStatus.SC_OK);
									response.setEntity(new StringEntity(token));
								}
							}
						}

						Log.info(HttpChallengeServer.class, "handle", "Sending response: " + response.toString());
					}
				}).setListenerPort(listenerPort).create();
				server.start();
			} catch (BindException e) {
				retry = true;
				remainingRetries--;

				if (remainingRetries == 0) {
					Assert.fail("Unable to bind to port " + listenerPort + ". Reason: " + e);
				}

				try {
					Thread.sleep(3000);
				} catch (InterruptedException e1) {
					// Ignore
				}
			}
		}
	}

	/**
	 * Stop the server.
	 * 
	 * @throws InterruptedException
	 *             If the shutdown is interrupted.
	 */
	public void stop() throws InterruptedException {
		if (server != null) {
			server.shutdown(10, TimeUnit.SECONDS);
			server = null;
		}
	}

	/**
	 * Set the ACME client that should be used to lookup authorizations.
	 * 
	 * @param acmeClient
	 *            The ACME client to lookup authorizations from.
	 */
	public void setAcmeClient(AcmeClient acmeClient) {
		this.acmeClient = acmeClient;
	}
}
