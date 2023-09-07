/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport;

import java.util.Map;

import org.osgi.framework.Constants;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

/**
 * This class contains the information about active configuration related to the specific GenericCahin.
 * There is one instance for TCP/TLS, one for TCP and one for UDP.
 * @author anatf
 *
 */
public class ActiveConfiguration {
    
	/** TRUE when this active configuration is for TLS */
    public boolean isTls = false;
    
	/** configured port */
	public final int configPort;
    
	/** configured host */
	public final String configHost;
	
	/** TCP options */
	public Map<String, Object> tcpOptions = null;

	/** SSL Options */
	public Map<String, Object> sslOptions = null;

	/** UDP Options */
	public Map<String, Object> udpOptions = null;

	/** Endpoint Options. */
	public final Map<String, Object> endpointOptions;

	/** Reference to the Generic Chain */
    private GenericChainBase _chain = null;

    /** Active listening port. */
    public volatile int activePort = -1;

	/** Active listening host */
    public String activeHost = null;
    
	/** Is this a valid configuration */
    public boolean validConfiguration = false;

    /**
     * Ctor for TCP/TLS
     * 
     * @param isTls
     * @param tcp
     * @param ssl
     * @param endpoint
     * @param chain
     */
    public ActiveConfiguration(boolean isTls,
                        Map<String, Object> tcp,
                        Map<String, Object> ssl,
                        Map<String, Object> endpoint,
                        GenericChainBase chain) {
        this.isTls = isTls;
        tcpOptions = tcp;
        sslOptions = ssl;
        endpointOptions = endpoint;
        _chain = chain;

        String attribute = isTls ? GenericEndpointImpl.s_TLS_PORT : GenericEndpointImpl.s_TCP_PORT;
        configPort = MetatypeUtils.parseInteger(GenericServiceConstants.ENPOINT_FPID_ALIAS, attribute,
                                                getEndpointOptions().get(attribute),
                                                -1);
        configHost = (String) getEndpointOptions().get("host");
    }
    
    /**
     * Ctor for UDP
     * @param udp
     * @param endpoint
     * @param chain
     */
    public ActiveConfiguration(Map<String, Object> udp, Map<String, Object> endpoint, GenericChainBase chain) {
		udpOptions = udp;
		endpointOptions = endpoint;
		_chain = chain;
		
		String attribute = GenericEndpointImpl.s_UDP_PORT;
		configPort = MetatypeUtils.parseInteger(GenericServiceConstants.ENPOINT_FPID_ALIAS, attribute,
		                                    getEndpointOptions().get(attribute),
		                                    -1);
		configHost = (String) getEndpointOptions().get("host");
    }

    /**
     * 
     * @return
     */
    public int getActivePort() {
        if (configPort < 0)
            return -1;
        return configPort;
    }

    /**
     * @return true if the ActiveConfiguration contains the required
     *         configuration to start the http chains. The base sip
     *         chain needs tcp or usp options. The sips chain
     *         additionally needs tcp and ssl options.
     */
    @Trivial
    public boolean isReady() {
        if (tcpOptions == null && udpOptions == null)
            return false;

        if (isTls && sslOptions == null)
            return false;

        return true;
    }

    /**
     * CHeck to see if all of the maps are the same as they
     * were the last time: ConfigurationAdmin returns unmodifiable
     * maps: if the map instances are the same, there have been no
     * updates.
     */
    public boolean unchanged(ActiveConfiguration other) {
        if (other == null)
            return false;

        // Only look at ssl options if this is an https chain
        if (isTls) {
            return getConfigHost().equals(other.getConfigHost()) &&
                   getConfigPort() == other.getConfigPort() &&
                   tcpOptions == other.tcpOptions &&
                   sslOptions == other.sslOptions &&
                   udpOptions == other.udpOptions &&
                   !endpointChanged(other);
        } else {
            return getConfigHost().equals(other.getConfigHost()) &&
                   getConfigPort() == other.getConfigPort() &&
                   tcpOptions == other.tcpOptions &&
                   udpOptions == other.udpOptions &&
                   !endpointChanged(other);
        }
    }

    /**
     * Returns true if TCP options were changed.
     * @param other
     * @return
     */
    public boolean tcpChanged(ActiveConfiguration other) {
        if (other == null)
            return false;

        return !getConfigHost().equals(other.getConfigHost()) ||
               getConfigPort() != other.getConfigPort() ||
               tcpOptions != other.tcpOptions;
    }

    /**
     * Returns true if TLS options were changed.
     * @param other
     * @return
     */
    public boolean sslChanged(ActiveConfiguration other) {
        if (other == null)
            return false;

        return sslOptions != other.sslOptions;
    }

    /**
     * Returns true if UDP options were changed.
     * @param other
     * @return
     */
    public boolean udpChanged(ActiveConfiguration other) {
        if (other == null)
            return false;

        return !getConfigHost().equals(other.getConfigHost()) ||
                getConfigPort() != other.getConfigPort() ||
                udpOptions != other.udpOptions;
    }

    /**
     * Returns true if Endpoint was changed.
     * In this method service PID is compared.
     * @param other
     * @return
     */
    public boolean endpointChanged(ActiveConfiguration other) {
        if (other == null)
            return false;
        // Instance equality doesn't work for this one, because the endpoint options
        // are the httpEndpoint's service properties, and they will change for reasons
        // that shouldn't cause a chain to restart
        return !getEndpointOptions().get(Constants.SERVICE_PID).equals(other.getEndpointOptions().get(Constants.SERVICE_PID));
    }

    @Override
    /** 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getClass().getSimpleName()
               + "[host=" + getConfigHost()
               + ",port=" + getConfigPort()
               + ",complete=" + isReady()
               + ",tcpOptions=" + System.identityHashCode(tcpOptions)
               + ",udpOptions=" + System.identityHashCode(udpOptions)
               + ",sslOptions=" + (isTls ? System.identityHashCode(sslOptions) : "0")
               + "]";
    }

    public String getActiveHost() {
        return activeHost;
    }

    public void setActiveHost(String activeHost) {
        this.activeHost = activeHost;
    }

    public boolean isValidConfiguration() {
        return validConfiguration;
    }

    public void setValidConfiguration(boolean validConfiguration) {
        this.validConfiguration = validConfiguration;
    }

    public int getConfigPort() {
        return configPort;
    }

    public String getConfigHost() {
        return configHost;
    }

    public Map<String, Object> getEndpointOptions() {
        return endpointOptions;
    }
}

