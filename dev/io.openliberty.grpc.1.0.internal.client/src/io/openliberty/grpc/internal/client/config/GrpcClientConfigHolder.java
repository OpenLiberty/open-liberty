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
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.grpc.internal.client.GrpcClientConstants;
import io.openliberty.grpc.internal.client.GrpcClientMessages;

/**
 * Adapted from com.ibm.ws.jaxrs20.clientconfig.JAXRSClientConfigHolder
 */
public class GrpcClientConfigHolder {
	private static final TraceComponent tc = Tr.register(GrpcClientConfigHolder.class, GrpcClientMessages.GRPC_TRACE_NAME, GrpcClientMessages.GRPC_BUNDLE);

	// a map of configuration properties keyed on hostname.
	// note that treemap sorts by key order, which we need to properly deal with the
	// wildcards
	private static volatile Map<String, Map<String, String>> configInfo = new TreeMap<>();

	// a map of service object id's to host, so we can track which service added
	// which host.
	private static volatile Map<String, String> hostMap = new HashMap<>();

	// a cached map of search results
	// that have been processed to look up the best match for the host strings
	private static volatile Map<String, Map<String, String>> resolvedConfigInfoCacheByHost = new HashMap<>();
	private static volatile Map<String, Map<String, String>> resolvedConfigInfoCacheByPath = new HashMap<>();


	private static boolean wildcardsPresentInConfigInfo = false;

	/**
	 * add a configuration for a hostname. We'd like a set of hashmaps keyed by host,
	 * however when osgi calls deactivate, we have no arguments, so we have to
	 * associate a hostname with the object id of the service. That allows us to remove
	 * the right one.
	 *
	 * @param id     - the object id of the service instance that added this.
	 * @param host    - the hostname of the record being added. Might end with *
	 * @param params - the properties applicable to this host.
	 */
	public static synchronized void addConfig(String id, String host, Map<String, String> params) {
		hostMap.put(id, host);
		configInfo.put(host, params);
		resolvedConfigInfoCacheByHost.clear();
		resolvedConfigInfoCacheByPath.clear();
		if (host.startsWith("*") || host.endsWith("$")) {
			wildcardsPresentInConfigInfo = true;
		}
	}

	/**
	 * remove a configuration (we'll look up the host from the objectId)
	 *
	 * @param id - the object id of the service that called us
	 */
	public static synchronized void removeConfig(String objectId) {
		String host = hostMap.get(objectId);
		if (host != null) {
			configInfo.remove(host);
		}
		hostMap.remove(objectId);
		resolvedConfigInfoCacheByHost.clear();
		resolvedConfigInfoCacheByPath.clear();
	}

	public static String getHeaderPropagationSupport(String host, String method) {
		Map<String, String> props = getHostProps(host, method);
		if (props != null) {
			return props.get(GrpcClientConstants.HEADER_PROPAGATION_PROP);
		} else {
			return null;
		}
	}

	public static String getSSLConfig(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.SSL_CFG_PROP);
		} else {
			return null;
		}
	}

	public static String getKeepAliveWithoutCalls(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.KEEP_ALIVE_WITHOUT_CALLS_PROP);
		} else {
			return null;
		}
	}

	public static String getKeepAliveTime(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.KEEP_ALIVE_TIME_PROP);
		} else {
			return null;
		}
	}

	public static String getKeepAliveTimeout(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.KEEP_ALIVE_TIMEOUT_PROP);
		} else {
			return null;
		}
	}

	public static String getClientInterceptors(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.CLIENT_INTERCEPTORS_PROP);
		} else {
			return null;
		}
	}

	public static String getMaxInboundMessageSize(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.MAX_INBOUND_MSG_SIZE_PROP);
		} else {
			return null;
		}
	}

	public static String getMaxInboundMetadataSize(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.MAX_INBOUND_METADATA_SIZE_PROP);
		} else {
			return null;
		}
	}

	public static String getOverrideAuthority(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.OVERRIDE_AUTHORITY_PROP);
		} else {
			return null;
		}
	}

	public static String getUserAgent(String host) {
		Map<String, String> props = getHostProps(host);
		if (props != null) {
			return props.get(GrpcClientConstants.USER_AGENT_PROP);
		} else {
			return null;
		}
	}

	/**
	 * Find the applicable set of properties for a given host, and return them. Note
	 * that config hosts ending with * might match more than one host.
	 *
	 * First we look for exact matches, then if applicable wildcard matches are
	 * found, we merge those into the results in order of most general to most
	 * specific.
	 *
	 *
	 * @param host - the host to retrieve the props for, probably of form
	 *            *.domain.io
	 * @return - map of merged applicable properties, or null if no applicable props
	 *         found
	 */
	public static Map<String, String> getHostProps(String host) {
		return getHostProps(host, null);
	}

	/**
	 * @param host
	 * @param path
	 * @return
	 */
	public static Map<String, String> getHostProps(String host, String path) {
		boolean debug = tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled();
		String fullPath = getPathID(host, path);
		if (configInfo.isEmpty()) {
			if (debug) {
				Tr.debug(tc, "getHostProps is empty, return null");
			}
			return null;
		}

		// look in the cache in case we've resolved this before
		if (path == null) {
			synchronized (resolvedConfigInfoCacheByHost) {
				Map<String, String> props = resolvedConfigInfoCacheByHost.get(host);
				if (props != null) {
					if (debug) {
						Tr.debug(tc, "resolvedConfigInfoCacheByHost cache hit, host: " + host + " path " + path + " props: " + props);
					}
					return (props.isEmpty() ? null : props);
				}
			}
		} else {
			synchronized (resolvedConfigInfoCacheByPath) {
				Map<String, String> props = resolvedConfigInfoCacheByPath.get(fullPath);
				if (props != null) {
					if (debug) {
						Tr.debug(tc, "resolvedConfigInfoCacheByPath cache hit, host: " + host + " path " + path + " props: " + props);
					}
					return (props.isEmpty() ? null : props);
				}
			}
		}

		// at this point we might have to merge something, set up a new hashmap to hold
		// the results
		HashMap<String, String> mergedProps = new HashMap<>();
		synchronized (GrpcClientConfigHolder.class) {
			// try for exact match
			Map<String, String> props = configInfo.get(fullPath);
			if (props != null) {
				if (debug) {
					Tr.debug(tc, "getHostProps exact match: " + host + " path " + path);
				}
				mergedProps.putAll(props);
			}
		}

		if (!wildcardsPresentInConfigInfo) {
			if (debug) {
				Tr.debug(tc, "getHostProps no wildcards, cache and return what we've got: " + mergedProps);
			}
			synchronized (GrpcClientConfigHolder.class) {
				if (path == null) {
					resolvedConfigInfoCacheByHost.put(host, mergedProps);
				} else {
					resolvedConfigInfoCacheByPath.put(fullPath, mergedProps);
				}
			}
			return (mergedProps.isEmpty() ? null : mergedProps);
		}

		// if a wildcard match exists, merge it to what we have already.
		if (debug) {
			Tr.debug(tc, "begin wildcard search");
		}
		// try to find a match from wildcards
		synchronized (GrpcClientConfigHolder.class) {
			Iterator<String> it = configInfo.keySet().iterator();
			String trimmedKey = null;
			while (it.hasNext()) {
				String key = it.next();
				String hostFromKey = key.substring(0, key.indexOf("$"));

				if (hostFromKey.startsWith("*")) {
					trimmedKey = hostFromKey.substring(1, hostFromKey.length());
				} else {
					trimmedKey = hostFromKey;
				}
				// if we got here, we have a wildcard
				// since configinfo is a sorted treemap,
				// general results should come first, followed by more specific results
				// if we have two keys of same length and both match, last one wins
				Map<String, String> props = configInfo.get(key);
				if (host.endsWith(trimmedKey) && pathMatches(path, props.get(GrpcClientConstants.PATH_PROP))) {
					if (debug) {
						Tr.debug(tc, "getHostProps match = " + hostFromKey + ", will merge these props: " + props);
					}
					mergedProps.putAll(props); // merge props for this wildcard.
				}
			}
			// at this point everything has been merged, cache and return what we have.
			// if there was no match anywhere, this is an empty map.
			if (path == null) {
				resolvedConfigInfoCacheByHost.put(host, mergedProps);
			} else {
				resolvedConfigInfoCacheByPath.put(fullPath, mergedProps);
			}
			if (debug) {
				Tr.debug(tc, "getHostProps final result for host: " + host + " path " + path + " values: " + mergedProps);
			}

			return (mergedProps.isEmpty() ? null : mergedProps);
		} // end sync block
	}

	/**
	 * Check to see if an absolute path matches a configured path, which might contain
	 * a wildcard.
	 * 
	 * @param path the absolute path
	 * @param configPath the configured path, which might contain a wildcard
	 * @return true if path is null or if a match is found
	 */
	private static boolean pathMatches(String path, String configPath) {
		if (tc.isDebugEnabled() && TraceComponent.isAnyTracingEnabled()) {
			Tr.debug(tc, "pathMatches checking path " + path + " against config path " + configPath);
		}
		if (path == null || configPath == null) {
			return true;
		}
		int wildcardPosition = configPath.indexOf("*");
		if (wildcardPosition == 0) {
			// path="*"
			return true;
		} else if (wildcardPosition == -1 ) {
			// path does not contain any wildcards
			return path.equals(configPath);
		} else {
			// path contains a wildcard
			String trimmedConfigPath = configPath.substring(0, wildcardPosition);
			return path.startsWith(trimmedConfigPath);
		}
	}

	protected static String getPathID(String host, String path) {
		return host + "$" + path;
	}
}
