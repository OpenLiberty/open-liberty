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
package io.openliberty.grpc.internal.client.security.ssl;

import java.util.List;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.openliberty.grpc.internal.client.GrpcClientMessages;
import io.openliberty.grpc.internal.client.GrpcSSLService;

@Component(service = {GrpcSSLService.class}, property = { "service.vendor=IBM" })
public class LibertyGrpcClientSSLSupport implements GrpcSSLService{
	
	private static final TraceComponent tc = Tr.register(LibertyGrpcClientSSLSupport.class, GrpcClientMessages.GRPC_TRACE_NAME, GrpcClientMessages.GRPC_BUNDLE);
	
	/**
	 * Build a io.grpc.netty.shaded.io.netty.handler.ssl.SslContext for the given target
	 * 
	 * @param target
	 * @return
	 */
	public SslContext getOutboundClientSSLContext(String sslRef, String host, String port) {
		
		SslContext context = null;
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "getOutboundClientSSLContext ssl reference ID: {0}", sslRef);
		}
		if (sslRef != null) {
			
			Properties props = SSLSupportService.getSSLProps(sslRef, host, port);	
			if (props != null) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "attempting to build SslContext with props: {0}", props);
				}
				try {
					SslContextBuilder builder = GrpcSslContexts.forClient();
					
					TrustManagerFactory trustFactory = SSLSupportService.getTrustManagerFactory(props);
					if (trustFactory != null) {
						builder.trustManager(trustFactory);
					}
					
					KeyManagerFactory keyFactory = SSLSupportService.getKeyManagerFactory(props);
					if (keyFactory != null) {
						builder.keyManager(keyFactory);
					}
					
					String sslProtocol = SSLSupportService.getSSLProtocol(props);
					if (sslProtocol != null) {
						if (!!!(sslProtocol.equals("TLSv1.2") || sslProtocol.equals("TLSv1.3"))) {
							// TODO: message saying that ssl protocols less than TLSv1.2 are not supported by Netty for HTTP/2 
							Tr.warning(tc, "invalid.ssl.prop", new Object[] { sslRef, sslProtocol } );
						}
						builder.protocols(sslProtocol);
					}
					
					List<String> ciphers = SSLSupportService.getCiphers(props);
					if (ciphers != null && !ciphers.isEmpty()) {
						builder.ciphers(ciphers);
					}
					
					builder.clientAuth(ClientAuth.OPTIONAL);
					context = builder.build();
				} catch (SSLException e) {
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
						Tr.debug(tc, "getOutboundClientSSLContext failed to create SslContext due to: {0}", e);
					}
				}
			}		
		}
		return context;
	}
}
