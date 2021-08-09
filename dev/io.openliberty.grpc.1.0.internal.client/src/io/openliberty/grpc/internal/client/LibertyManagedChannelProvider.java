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
package io.openliberty.grpc.internal.client;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.managedobject.ManagedObjectException;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelProvider;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContext;
import io.openliberty.grpc.internal.GrpcManagedObjectProvider;
import io.openliberty.grpc.internal.client.config.GrpcClientConfigHolder;

/**
 * io.grpc.ManagedChannelProvider that takes care of any required Liberty
 * configuration, and then delegates to the NettyChannelBuilder
 */
public class LibertyManagedChannelProvider extends ManagedChannelProvider {

	private static final TraceComponent tc = Tr.register(LibertyManagedChannelProvider.class, GrpcClientMessages.GRPC_TRACE_NAME, GrpcClientMessages.GRPC_BUNDLE);

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public int priority() {
		return 10;
	}

	@Override
	public NettyChannelBuilder builderForAddress(String name, int port) {

		NettyChannelBuilder builder = NettyChannelBuilder.forAddress(name, port);
		configureLibertyBuilder(builder, name, String.valueOf(port));

		return builder;
	}

	@Override
	public NettyChannelBuilder builderForTarget(String target) {
		NettyChannelBuilder builder = NettyChannelBuilder.forTarget(target);
		configureLibertyBuilder(builder, target, "");
		return builder;
	}

	private void configureLibertyBuilder(NettyChannelBuilder builder, String target, String port) {
		addLibertyInterceptors(builder);
		// set up any user config from server.xml
		Map<String, String> config = GrpcClientConfigHolder.getHostProps(target);
		if (config != null && !config.isEmpty()) {
			addUserInterceptors(builder, config);
			addKeepAliveConfiguration(builder, config);
			addMaxInboundMessageSize(builder, config);
			addMaxInboundMetadataSize(builder, config);
			addUserAgent(builder, config);
			addOverrideAuthority(builder, config);
		}
		// only create an SSL context if usePlaintext is disabled (default)
		if (!isUsePlaintextEnabled(config)) {
			addLibertySSLConfig(builder, target, port, config);
		}
	}

	private void addLibertyInterceptors(NettyChannelBuilder builder) {
		builder.intercept(new LibertyClientInterceptor());
		ClientInterceptor monitoringInterceptor = createMonitoringClientInterceptor();
		if (monitoringInterceptor != null) {
			builder.intercept(monitoringInterceptor);
		}
	}

	private void addLibertySSLConfig(NettyChannelBuilder builder, String target, String port, Map<String, String> config) {
		String sslRef = null;
		if (config != null && !config.isEmpty()) {
			sslRef = config.get(GrpcClientConstants.SSL_CFG_PROP);
		}
		SslContext context = null;
		GrpcSSLService sslService = GrpcClientComponent.getGrpcSSLService();
		if (sslService != null) {
			context = sslService.getOutboundClientSSLContext(sslRef, target, port);
			if (context != null) {
				builder.sslContext(context);
			}
		}
	}

	private void addKeepAliveConfiguration(NettyChannelBuilder builder, Map<String, String> config) {
		String keepAliveTime = config.get(GrpcClientConstants.KEEP_ALIVE_TIME_PROP);
		String keepAliveWithoutCalls = config.get(GrpcClientConstants.KEEP_ALIVE_WITHOUT_CALLS_PROP);
		String keepAliveTimeout = config.get(GrpcClientConstants.KEEP_ALIVE_TIMEOUT_PROP);

		if (keepAliveTime != null && !keepAliveTime.isEmpty()) {
			int time = Integer.parseInt(keepAliveTime);
			builder.keepAliveTime(time, TimeUnit.SECONDS);
		}
		if (keepAliveWithoutCalls != null && !keepAliveWithoutCalls.isEmpty()) {
			Boolean enabled = Boolean.parseBoolean(keepAliveWithoutCalls);
			builder.keepAliveWithoutCalls(enabled);
		}
		if (keepAliveTimeout != null && !keepAliveTimeout.isEmpty()) {
			int timeout = Integer.parseInt(keepAliveTimeout);
			builder.keepAliveTimeout(timeout, TimeUnit.SECONDS);
		}
	}

	private void addMaxInboundMessageSize(NettyChannelBuilder builder, Map<String, String> config) {
		String maxMsgSizeString = config.get(GrpcClientConstants.MAX_INBOUND_MSG_SIZE_PROP);
		if (maxMsgSizeString != null && !maxMsgSizeString.isEmpty()) {
			int maxSize = Integer.parseInt(maxMsgSizeString);
			if (maxSize == -1) {
				builder.maxInboundMessageSize(Integer.MAX_VALUE);
			} else if (maxSize > 0) {
				builder.maxInboundMessageSize(maxSize);
			}
		}
	}

	private void addMaxInboundMetadataSize(NettyChannelBuilder builder, Map<String, String> config) {
		String maxMetaString = config.get(GrpcClientConstants.MAX_INBOUND_METADATA_SIZE_PROP);
		if (maxMetaString != null && !maxMetaString.isEmpty()) {
			int maxSize = Integer.parseInt(maxMetaString);
			if (maxSize == -1) {
				builder.maxInboundMetadataSize(Integer.MAX_VALUE);
			} else if (maxSize > 0) {
				builder.maxInboundMetadataSize(maxSize);
			}
		}
	}

	private void addUserAgent(NettyChannelBuilder builder, Map<String, String> config) {
		String userAgent = config.get(GrpcClientConstants.USER_AGENT_PROP);
		if (userAgent != null && !userAgent.isEmpty()) {
			builder.userAgent(userAgent);
		}
	}

	private void addOverrideAuthority(NettyChannelBuilder builder, Map<String, String> config) {
		String authority = config.get(GrpcClientConstants.OVERRIDE_AUTHORITY_PROP);
		if (authority != null && !authority.isEmpty()) {
			builder.overrideAuthority(authority);
		}
	}

	private boolean isUsePlaintextEnabled(Map<String, String> config) {
		if (config != null) {
			String usePlaintext = config.get(GrpcClientConstants.USE_PLAINTEXT_PROP);
			if (usePlaintext != null && !usePlaintext.isEmpty()) {
				return Boolean.parseBoolean(usePlaintext);
			}
		}
		return false;
	}

	private void addUserInterceptors(NettyChannelBuilder builder, Map<String, String> config) {
		String interceptorListString = config.get(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP);

		if (interceptorListString != null) {
			List<String> items = Arrays.asList(interceptorListString.split("\\s*,\\s*"));
			if (!items.isEmpty()) {
				for (String className : items) {
					try {
						// use the managed object service to load the interceptor 
						ClientInterceptor interceptor = 
								(ClientInterceptor) GrpcManagedObjectProvider.createObjectFromClassName(className);
						if (interceptor != null) {
							builder.intercept(interceptor);
						}
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
							IllegalArgumentException | InvocationTargetException | NoSuchMethodException |
							SecurityException | ManagedObjectException e) {
						Tr.warning(tc, "invalid.clientinterceptor", e.getMessage());
					}
				}
			}
		}
	}

	private ClientInterceptor createMonitoringClientInterceptor() {
		// create the interceptor only if the monitor feature is enabled
		return GrpcClientComponent.getMonitoringClientInterceptor();
	}
}
