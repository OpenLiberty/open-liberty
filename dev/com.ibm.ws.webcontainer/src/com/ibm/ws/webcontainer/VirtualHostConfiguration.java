/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer;

import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.BaseConfiguration;
import com.ibm.ws.webcontainer.osgi.osgi.WebContainerConstants;
import com.ibm.wsspi.http.VirtualHost;

/**
 * The underlying VirtualHost configuration may change, or may not be known
 * at the time this VirtualHost configuration object is created.
 * <p>
 * In all cases, this configuration delegates where possible to the underlying
 * VirtualHost configuration (which will have the most accurate information).
 * 
 * @see #setConfiguration(VirtualHost);
 */
public class VirtualHostConfiguration extends BaseConfiguration {
    
    private static final TraceComponent tc = Tr.register(VirtualHostConfiguration.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);
    /** 
     * The underlying virtual host configuration may change: 
     * ensure the most recent copy is seen.
     */
    protected volatile VirtualHost config = null;
    
    /**
     * Construct the VirtualHostConfiguration object: use this when the
     * associated VirtualHost is unknown or unavailable at construction time.
     */
    public VirtualHostConfiguration(String name) {
        super(name);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "VirtualHostConfiguration", "name ->"+ name);
        }
    }

    /**
     * Construct the VirtualHostConfiguration object with the underlying VirtualHost
     * @param config VirtualHost object representing the new/updated VirtualHost
     *               configuration
     */
    public VirtualHostConfiguration(VirtualHost config) {
        super(config.getName());
        this.config = config;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "VirtualHostConfiguration", "config  ->"+ this.config);
        }
    }

    /**
     * Set or replace the underlying VirtualHost that provides host aliases
     * and default mime type configuration.
     * 
     * @param config VirtualHost object representing the new/updated VirtualHost
     *               configuration
     */
    public void setConfiguration(VirtualHost config) {
        this.config = config;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setConfiguration", "config set ->"+ this.config);
        }
    }
    
    /**
     * Return the port that should be used for secure redirect
     * from http to https. 
     * <p>
     * Host aliases are paired: *:80<->*:443. If the VirtualHost supports
     * several aliases, they may not all be paired for failover.
     * <p>
     * @param hostAlias The http host alias to find the partner for.
     * @return secure https port associated with the given alias (via endpoint configuration),
     *         or -1 if unconfigured.
     */
    public int getSecureHttpPort(String hostAlias) {
        com.ibm.wsspi.http.VirtualHost local = config;
        if ( local == null )
            return -1;
        else
            return local.getSecureHttpPort(hostAlias);
    }

    /**
     * Return the port that should be used for redirect back from
     * https to http.
     * <p>
     * Host aliases are paired: *:80<->*:443. If the VirtualHost supports
     * several aliases, they may not all be paired for failover.
     * <p>
     * @param hostAlias The https host alias to find the partner for.
     * @return http port associated with the given alias (via endpoint configuration),
     *         or -1 if unconfigured.
     */
    public int getHttpPort(String hostAlias) {
        com.ibm.wsspi.http.VirtualHost local = config;
        if ( local == null )
            return -1;
        else
            return local.getHttpPort(hostAlias);
    }
    
    public String getName() 
    {
        return getId();
    }

    /**
     * @return the list of known host aliases: host:port
     */
    public List<String> getAliases() 
    {
        com.ibm.wsspi.http.VirtualHost local = config;
        if ( local == null )
            return Collections.emptyList();
        else 
            return local.getAliases();
    }

    /**
     * @param extension
     * @return mime type configured for the extension, or null if the extension is
     *         unknown or unconfigured
     */
    public String getMimeType(String extension) 
    {
        com.ibm.wsspi.http.VirtualHost local = config;
        if ( local == null )
            return null;
        else 
            return local.getMimeType(extension);
    }

    public String toString() 
    {
        StringBuffer buf = new StringBuffer(getName());
        buf.append('[').append(config).append(']');
        return buf.toString();
    }

}
