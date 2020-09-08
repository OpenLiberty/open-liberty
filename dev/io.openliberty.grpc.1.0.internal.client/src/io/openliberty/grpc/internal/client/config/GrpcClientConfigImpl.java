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
package io.openliberty.grpc.internal.client.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.grpc.internal.client.GrpcClientConstants;
import io.openliberty.grpc.internal.client.GrpcClientMessages;

/**
 * Adapted from com.ibm.ws.jaxrs20.clientconfig.JAXRSClientConfig
 */
@Component(immediate = true, service = {
		GrpcClientConfig.class }, configurationPid = "io.openliberty.grpc.clientConfig", configurationPolicy = ConfigurationPolicy.REQUIRE, property = {
				"service.vendor=IBM" })
public class GrpcClientConfigImpl implements GrpcClientConfig {
	private static final TraceComponent tc = Tr.register(GrpcClientConfigImpl.class, GrpcClientMessages.GRPC_TRACE_NAME, GrpcClientMessages.GRPC_BUNDLE);

	private static final HashSet<String> propertiesToRemove = new HashSet<>();

	static {
		// this is stuff the framework always adds, we don't need it so we'll filter it
		// out.
		propertiesToRemove.add("defaultSSHPublicKeyPath");
		propertiesToRemove.add("defaultSSHPrivateKeyPath");
		propertiesToRemove.add("config.overrides");
		propertiesToRemove.add("config.id");
		propertiesToRemove.add("component.id");
		propertiesToRemove.add("config.displayId");
		propertiesToRemove.add("component.name");
		propertiesToRemove.add("config.source");
		propertiesToRemove.add("service.pid");
		propertiesToRemove.add("service.vendor");
		propertiesToRemove.add("service.factoryPid");
		propertiesToRemove.add(GrpcClientConstants.HOST_PROP);

	}

	/**
	 * given the map of properties, remove ones we don't care about, and translate
	 * some others. If it's not one we're familiar with, transfer it unaltered
	 *
	 * @param props - input list of properties
	 * @return - a new Map of the filtered properties.
	 */
	private Map<String, String> filterProps(Map<String, Object> props) {
		HashMap<String, String> filteredProps = new HashMap<>();
		Iterator<String> it = props.keySet().iterator();
		boolean debug = tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled();

		while (it.hasNext()) {
			String key = it.next();

			if (debug) {
				Tr.debug(tc, "key: " + key + " value: " + props.get(key));
			}
			// skip stuff we don't care about
			if (propertiesToRemove.contains(key)) {
				continue;
			}
			if (key.compareTo(GrpcClientConstants.KEEP_ALIVE_TIME_PROP) == 0) {
				if (!GrpcClientConfigValidation.validateKeepAliveTime(props.get(key).toString()))
					continue;
			}
			if (key.compareTo(GrpcClientConstants.KEEP_ALIVE_TIMEOUT_PROP) == 0) {
				if (!GrpcClientConfigValidation.validateKeepAliveTimeout(props.get(key).toString()))
					continue;
			}
			if (key.compareTo(GrpcClientConstants.MAX_INBOUND_MSG_SIZE_PROP) == 0) {
				if (!GrpcClientConfigValidation.validateMaxInboundMessageSize(props.get(key).toString()))
					continue;
			}
			if (key.compareTo(GrpcClientConstants.SSL_CFG_PROP) == 0) {
				if (!GrpcClientConfigValidation.validateSslConfig(props.get(key).toString()))
					continue;
			}
			filteredProps.put(key, props.get(key).toString());

		}
		return filteredProps;
	}

	/**
	 * find the host parameter which we will key off of
	 *
	 * @param props
	 * @return value of host param within props, or null if no host param
	 */
	private String getHost(Map<String, Object> props) {
		if (props == null)
			return null;
		if (props.keySet().contains(GrpcClientConstants.HOST_PROP)) {
			return (props.get(GrpcClientConstants.HOST_PROP).toString());
		} else {
			return null;
		}
	}

	/**
	 * find the path parameter which we will key off of
	 *
	 * @param props
	 * @return value of path param within props, or null if no path param
	 */
	private String getPath(Map<String, Object> props) {
		if (props == null)
			return null;
		if (props.keySet().contains(GrpcClientConstants.PATH_PROP)) {
			return (props.get(GrpcClientConstants.PATH_PROP).toString());
		} else {
			return null;
		}
	}

	@Activate
	protected void activate(Map<String, Object> properties) {
		if (properties == null)
			return;
		String host = getHost(properties);
		String path = getPath(properties);
		if (host == null)
			return;
		GrpcClientConfigHolder.addConfig(this.toString(), GrpcClientConfigHolder.getPathID(host, path),
				filterProps(properties));
	}

	@Modified
	protected void modified(Map<String, Object> properties) {
		if (properties == null)
			return;
		GrpcClientConfigHolder.removeConfig(this.toString());
		// if they deleted the uri attribute, no point in adding.
		String host = getHost(properties);
		String path = getPath(properties);
		if (host == null)
			return;
		GrpcClientConfigHolder.addConfig(this.toString(), GrpcClientConfigHolder.getPathID(host, path),
				filterProps(properties));
	}

	@Deactivate
	protected void deactivate() {
		GrpcClientConfigHolder.removeConfig(this.toString());
	}
}