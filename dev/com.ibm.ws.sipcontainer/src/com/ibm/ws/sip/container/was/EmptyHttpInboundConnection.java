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
package com.ibm.ws.sip.container.was;

import java.util.Map;

import com.ibm.wsspi.http.EncodingUtils;
import com.ibm.wsspi.http.HttpDateFormat;
import com.ibm.wsspi.http.HttpInboundConnection;
import com.ibm.wsspi.http.HttpRequest;
import com.ibm.wsspi.http.HttpResponse;
import com.ibm.wsspi.http.SSLContext;

/**
 * @author SAGIA
 * Empty implementation of the HttpInboundConection.
 *  we use it to pass the connection to the web container to prevent NPE when the message processing is finished.
 */
public class EmptyHttpInboundConnection implements HttpInboundConnection {

	@Override
	public String getLocalHostName(boolean canonical) {
		
		return null;
	}

	@Override
	public String getLocalHostAddress() {
	
		return null;
	}

	@Override
	public int getLocalPort() {
		
		return 0;
	}

	@Override
	public String getLocalHostAlias() {
		
		return null;
	}

	@Override
	public String getRemoteHostName(boolean canonical) {

		return null;
	}

	@Override
	public String getRemoteHostAddress() {

		return null;
	}

	@Override
	public int getRemotePort() {

		return 0;
	}

	@Override
	public HttpRequest getRequest() {

		return null;
	}

	@Override
	public HttpResponse getResponse() {

		return null;
	}

	@Override
	public SSLContext getSSLContext() {

		return null;
	}

	@Override
	public EncodingUtils getEncodingUtils() {

		return null;
	}

	@Override
	public HttpDateFormat getDateFormatter() {

		return null;
	}

	@Override
	public void finish(Exception e) {


	}

	@Override
	public boolean useTrustedHeaders() {

		return false;
	}

	@Override
	public String getTrustedHeader(String headerKey) {

		return null;
	}

	@Override
	public String getRequestedHost() {

		return null;
	}

	@Override
	public int getRequestedPort() {

		return 0;
	}

}
