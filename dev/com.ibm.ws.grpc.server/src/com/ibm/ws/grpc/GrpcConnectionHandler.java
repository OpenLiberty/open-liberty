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

package com.ibm.ws.grpc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.ee8.Http2Connection;
import com.ibm.wsspi.http.ee8.Http2ConnectionHandler;
import com.ibm.wsspi.http.ee8.Http2Stream;
import com.ibm.wsspi.http.ee8.Http2StreamHandler;

import io.grpc.Metadata;
import io.grpc.internal.ServerStream;
import io.grpc.internal.ServerTransportListener;

/**
 * HTTP/2 Connection handler for gRPC connections 
 */
public class GrpcConnectionHandler implements Http2ConnectionHandler {
	
	private final ServerTransportListener listener;
	
	public GrpcConnectionHandler (ServerTransportListener l) {
		listener = l;
	}

	/**
	 * Pass a new gRPC request and HTTP/2 stream to gRPC
	 */
	@Override
	public Http2StreamHandler onStreamCreated(HttpRequestMessage request, Http2Stream stream, Http2Connection connection) {
				
		ServerStream grpcH2Stream = new LibertyServerStream(stream, connection);
		
		// grab the path and remove the leading slash
		String path = request.getRequestURI().substring(1);
		
		// put the headers into a format usable by gRPC
		List<HeaderField> reqHeaders =  request.getAllHeaders();
		Metadata grpcHeaders = new Metadata();
        for (HeaderField h : reqHeaders) {
        	grpcHeaders.put(Metadata.Key.of(h.getName(), Metadata.ASCII_STRING_MARSHALLER), h.asString());
        }
        
		listener.streamCreated(grpcH2Stream, path, grpcHeaders);
		return (Http2StreamHandler) grpcH2Stream;
	}

	/**
	 * @return Set<String> containing "application/grpc"
	 */
	@Override
	public Set<String> getSupportedContentTypes() {
		Set<String> contentTypes = new HashSet<String>();
		contentTypes.add("application/grpc");
		return contentTypes;
	}
}
