/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.nester.Nester;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.wsspi.channelfw.ChannelConfiguration;

/**
 * A IIOPEndpointImpl is a main CORBA server configuration. The
 * IIOPEndpointImpl is the hosting ORB to which additional TSSBeans
 * attach to export EJBs. The IIOPEndpointImpl may be configured
 * to use either plain socket listeners or SSL listeners
 * 
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
                property = { "service.vendor=IBM" })
public class IIOPEndpointImpl implements IIOPEndpoint {

    private static final TraceComponent tc = Tr.register(IIOPEndpointImpl.class);

    private String host;

    private int iiopPort;

    private List<Map<String, Object>> iiopsOptions;

    private ChannelConfiguration tcpOptions;

    @Reference( //policy = ReferencePolicy.DYNAMIC,
    cardinality = ReferenceCardinality.OPTIONAL,
                    policyOption = ReferencePolicyOption.GREEDY)
    protected void setTcpOptions(ChannelConfiguration service) {
        tcpOptions = service;
    }

    /**
     * Start the ORB associated with this bean instance.
     * 
     * @exception Exception
     */
    @Activate
    protected void activate(Map<String, Object> properties) throws Exception {
        this.host = (String) properties.get("host");
        Integer iiopPort = (Integer) properties.get("iiopPort");
        this.iiopPort = iiopPort == null ? -1 : iiopPort;

        iiopsOptions = Nester.nest("iiopsOptions", properties);
    }

//    private void processOptions(Map<String, Object> properties) {
//        ChannelConfiguration c = sslOptions.getService();
//        String sslRef = c != null ? (String) c.getConfiguration().get("sslRef") : null;
//        if (sslRef != null && !sslRef.trim().isEmpty()) {
//            extraConfig.put("sslConfigName", sslRef);
//        }
//        c = tcpOptions.getService();
//        Boolean soReuseAddr = c != null ? (Boolean) c.getConfiguration().get("soReuseAddr") : null;
//        if (soReuseAddr != null) {
//            extraConfig.put("soReuseAddr", soReuseAddr);
//        }
//        //TODO idle timeout???
//    }

    @FFDCIgnore(PrivilegedActionException.class)
    private void resolveListenerAddress() throws PrivilegedActionException {
        if (host == null) {
            try {
                host = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws UnknownHostException {
                        return InetAddress.getLocalHost().getHostName();
                    }
                });
            } catch (PrivilegedActionException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "IiopEndpoint cannot determine name of local host", e);
                }
                // just punt an use localhost as an absolute fallback.
                host = "localhost";
            }
        }

    }

    /** {@inheritDoc} */
    @Override
    public String getHost() {
        return host;
    }

    /** {@inheritDoc} */
    @Override
    public int getIiopPort() {
        return iiopPort;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Object> getTcpOptions() {
        return tcpOptions.getConfiguration();
    }

    /** {@inheritDoc} */
    @Override
    public List<Map<String, Object>> getIiopsOptions() {
        return iiopsOptions;
    }

}
