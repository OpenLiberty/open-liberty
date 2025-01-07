/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.channel;

import java.util.Map;
import java.util.Objects;

import com.ibm.ws.channelfw.internal.chains.Chain;

/**
 * Represents a common configuration for any implementation of {@link Chain},
 * encapsulating fields and logic for determining if a chain must
 * be stopped/restarted based on new config values.
 */
public class ChainConfiguration {

    private final boolean https;
    private final String host;
    private final int port;
    private final Map<String, Object> tcpOptions;
    private final Map<String, Object> sslOptions;
    private final Map<String, Object> httpOptions;
    private final Map<String, Object> endpointOptions;
    private final Map<String, Object> remoteIpOptions; 
    private final Map<String, Object> compressionOptions;
    private final Map<String, Object> samesiteOptions;
    private final Map<String, Object> headersOptions;

    /**
     * @param https              true if this config is for HTTPS
     * @param host               the configured hostname (e.g. '*' or 'localhost')
     * @param port               the endpoint configured port
     * @param tcpOptions         map of TCP channel configuration
     * @param sslOptions         map of SSL channel configuration
     * @param httpOptions        map of HTTP channel configuration
     * @param endpointOptions    the HTTP endpoint’s overall config map
     * @param remoteIpOptions    configuration for remote IP handling
     * @param compressionOptions configuration for compression
     * @param samesiteOptions    configuration for samesite cookies
     * @param headersOptions     configuration for response custom headers
     */
    public ChainConfiguration(
                              boolean https,
                              String host,
                              int port,
                              Map<String, Object> tcpOptions,
                              Map<String, Object> sslOptions,
                              Map<String, Object> httpOptions,
                              Map<String, Object> endpointOptions,
                              Map<String, Object> remoteIpOptions,
                              Map<String, Object> compressionOptions,
                              Map<String, Object> samesiteOptions,
                              Map<String, Object> headersOptions) {

        this.https = https;
        this.host = host;
        this.port = port;
        this.tcpOptions = tcpOptions;
        this.sslOptions = sslOptions;
        this.httpOptions = httpOptions;
        this.endpointOptions = endpointOptions;
        this.remoteIpOptions = remoteIpOptions;
        this.compressionOptions = compressionOptions;
        this.samesiteOptions = samesiteOptions;
        this.headersOptions = headersOptions;
    }

    /**
     * 
     * @return Specifies if this {@Link Chain} is configured for SSL
     */
    public boolean isHttps() {
        return https;
    }

    /**
     * @return The configured hostname, such as '*' or 'localhost'.
     */
    public String getHost() {
        return host;
    }

    /**
     * @return The configured HTTP/HTTPS port.
     */
    public int port() {
        return port;
    }

    /**
     * @return The configured TCP options
     */
    public Map<String, Object> tcpOptions() {
        return tcpOptions;
    }

    /**
     * @return The configured SSL options
     */
    public Map<String, Object> sslOptions() {
        return sslOptions;
    }

    /**
     * @return The configured HTTP options
     */
    public Map<String, Object> httpOptions() {
        return httpOptions;
    }

    /**
     * @return The configured endpoint options
     */
    public Map<String, Object> endpointOptions() {
        return endpointOptions;
    }

    /**
     * @return The configured remoteIp options
     */
    public Map<String, Object> remoteIpOptions() {
        return remoteIpOptions;
    }

    /**
     * @return The configured compression options
     */
    public Map<String, Object> compressionOptions() {
        return compressionOptions;
    }

    /**
     * @return The configured samesite options
     */
    public Map<String, Object> samesiteOptions() {
        return samesiteOptions;
    }

    /**
     * @return The configured headers options
     */
    public Map<String, Object> headersOptions() {
        return headersOptions;
    }

    /**
     * Determines if the minimal fields for starting a chain are available.
     * For HTTPS, sslOptions must exist. If port < 0, it’s unconfigured.
     */
    public boolean isComplete() {
        if (tcpOptions == null || httpOptions == null) return false;
        if (port < 0) return false;
        if (https && sslOptions == null) return false;
        return true;
    }

    /**
     * Returns whether we need to restart the chain due to a config change.
     * Checks all relevant fields like port, host, or changed reference maps.
     *
     * @param other The previous configuration or null if none.
     */
    public boolean requiresRestart(ChainConfiguration other) {
        if (other == null) return true;
        if (this.https != other.https) return true;
        if (!Objects.equals(this.host, other.host)) return true;
        if (this.port != other.port) return true;

        // Check whether reference maps differ:
        if (!Objects.equals(this.tcpOptions, other.tcpOptions)) return true;
        if (!Objects.equals(this.httpOptions, other.httpOptions)) return true;
        if (!Objects.equals(this.sslOptions, other.sslOptions)) return true;
        if (!Objects.equals(this.remoteIpOptions, other.remoteIpOptions)) return true;
        if (!Objects.equals(this.compressionOptions, other.compressionOptions)) return true;
        if (!Objects.equals(this.samesiteOptions, other.samesiteOptions)) return true;
        if (!Objects.equals(this.headersOptions, other.headersOptions)) return true;

        return false;
    }

    @Override
    public String toString() {
        return "ChainConfiguration{"
            + "https=" + https
            + ", host='" + host + '\''
            + ", port=" + port
            + ", tcp=" + (tcpOptions != null ? tcpOptions.hashCode() : "[N/A]")
            + ", ssl=" + (sslOptions != null ? sslOptions.hashCode() : "[N/A]")
            + ", http=" + (httpOptions != null ? httpOptions.hashCode() : "[N/A]")
            + ", remoteIp=" + (remoteIpOptions != null ? remoteIpOptions.hashCode() : "[N/A]")
            + ", compression=" + (compressionOptions != null ? compressionOptions.hashCode() : "[N/A]")
            + ", samesite=" + (samesiteOptions != null ? samesiteOptions.hashCode() : "[N/A]")
            + ", headers=" + (headersOptions != null ? headersOptions.hashCode() : "[N/A]")
            + "}";
    }
}
