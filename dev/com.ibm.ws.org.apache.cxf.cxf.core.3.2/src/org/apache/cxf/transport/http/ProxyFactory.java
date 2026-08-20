/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.http;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.regex.Pattern;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

public class ProxyFactory {
    /**
     * JVM/System property name holding the hostname of the http proxy.
     */
    private static final String HTTP_PROXY_HOST = "http.proxyHost";

    /**
     * JVM/System property name holding the port of the http proxy.
     */
    private static final String HTTP_PROXY_PORT = "http.proxyPort";

    /**
     * JVM/System property name holding the list of hosts/patterns that
     * should not use the proxy configuration.
     */
    private static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    /**
     * This field holds ONLY the static System proxy configuration:
     * + http.proxyHost
     * + http.proxyPort (default 8080)
     * + http.nonProxyHosts (default null)
     * It is initialized at the instance creation (and may be null
     * if there is no appropriate System properties)
     */
    private HTTPClientPolicy systemProxyConfiguration;

    public ProxyFactory() {
        this.systemProxyConfiguration = createSystemProxyConfiguration();
    }

    private static HTTPClientPolicy createSystemProxyConfiguration() {
        // Retrieve system properties (if any)
        HTTPClientPolicy systemProxyConfiguration = null;
        String proxyHost = SystemPropertyAction.getPropertyOrNull(HTTP_PROXY_HOST);
        if (StringUtils.isEmpty(proxyHost)) {
            proxyHost = null;
        }
        if (proxyHost != null) {
            // System is configured with a proxy, use it

            systemProxyConfiguration = new HTTPClientPolicy();
            systemProxyConfiguration.setProxyServer(proxyHost);
            systemProxyConfiguration.setProxyServerType(ProxyServerType.HTTP);

            // 8080 is the default proxy port value as per some documentation
            String proxyPort = SystemPropertyAction.getProperty(HTTP_PROXY_PORT, "8080");
            if (StringUtils.isEmpty(proxyPort)) {
                proxyPort = "8080";
            }

            systemProxyConfiguration.setProxyServerPort(Integer.parseInt(proxyPort));

            // Load non proxy hosts
            String nonProxyHosts = SystemPropertyAction.getPropertyOrNull(HTTP_NON_PROXY_HOSTS);
            if (!StringUtils.isEmpty(nonProxyHosts)) {
                systemProxyConfiguration.setNonProxyHosts(nonProxyHosts);
            }
        }
        return systemProxyConfiguration;
    }

    /**
     * This method returns the Proxy server should it be set on the
     * Client Side Policy.
     *
     * @return The proxy server or null, if not set.
     */
    public Proxy createProxy(HTTPClientPolicy policy, URI currentUrl) {
        if (policy != null) {
            // Maybe the user has provided some proxy information
            if (policy.isSetProxyServer()
                && !StringUtils.isEmpty(policy.getProxyServer())) {
                return getProxy(policy, currentUrl.getHost());
            }
            // There is a policy but no Proxy configuration,
            // fallback on the system proxy configuration
            return getSystemProxy(currentUrl.getHost());
        }
        // Use system proxy configuration
        return getSystemProxy(currentUrl.getHost());
    }

    /**
     * Get the system proxy (if any) for the given URL's host.
     */
    private Proxy getSystemProxy(String hostname) {
        if (systemProxyConfiguration != null) {
            return getProxy(systemProxyConfiguration, hostname);
        }

        // No proxy configured
        return null;
    }

    /**
     * Honor the nonProxyHosts property value (if set).
     */
    private Proxy getProxy(final HTTPClientPolicy policy, final String hostname) {
        if (policy.isSetNonProxyHosts()) {

            // Try to match the URL hostname with the exclusion pattern
            Pattern pattern = PatternBuilder.build(policy.getNonProxyHosts());
            if (pattern.matcher(hostname).matches()) {
                // Excluded hostname -> no proxy
                return Proxy.NO_PROXY;
            }
        }
        // Either nonProxyHosts is not set or the pattern did not match
        return createProxy(policy);
    }

    /**
     * Construct a new {@code Proxy} instance from the given policy.
     */
    private Proxy createProxy(final HTTPClientPolicy policy) {
        return new Proxy(Proxy.Type.valueOf(policy.getProxyServerType().toString()),
                         new InetSocketAddress(policy.getProxyServer(),
                                               policy.getProxyServerPort()));
    }
}
