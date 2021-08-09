/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.plugins;

/**
 * Variant of OIDC Discovery Configuration Metadata, which includes properties
 * required by Jazz and WAS
 */
public class OIDCWASDiscoveryModel extends OIDCAbstractDiscoveryModel {
    private String introspection_endpoint;
    private String coverage_map_endpoint;
    private String proxy_endpoint;
    private String backing_idp_uri_prefix;

    public OIDCWASDiscoveryModel() {
        super();
    }

    /**
     * @return the introspectionEndpoint
     */
    public String getIntrospectionEndpoint() {
        return introspection_endpoint;
    }

    /**
     * @param introspectionEndpoint the introspectionEndpoint to set
     */
    public void setIntrospectionEndpoint(String introspectionEndpoint) {
        this.introspection_endpoint = introspectionEndpoint;
    }

    /**
     * @return the coverageMapEndpoint
     */
    public String getCoverageMapEndpoint() {
        return coverage_map_endpoint;
    }

    /**
     * @param coverageMapEndpoint the coverageMapEndpoint to set
     */
    public void setCoverageMapEndpoint(String coverageMapEndpoint) {
        this.coverage_map_endpoint = coverageMapEndpoint;
    }

    /**
     * @return the proxyEndpoint
     */
    public String getProxyEndpoint() {
        return proxy_endpoint;
    }

    /**
     * @param proxyEndpoint the proxyEndpoint to set
     */
    public void setProxyEndpoint(String proxyEndpoint) {
        this.proxy_endpoint = proxyEndpoint;
    }

    /**
     * @return the backing_idp_uri_prefix
     */
    public String getBackingIdpUriPrefix() {
        return backing_idp_uri_prefix;
    }

    /**
     * @param backingIdpUriPrefix the backing_idp_uri_prefix to set
     */
    public void setBackingIdpUriPrefix(String backingIdpUriPrefix) {
        this.backing_idp_uri_prefix = backingIdpUriPrefix;
    }
}
