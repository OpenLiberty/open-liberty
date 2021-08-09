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
package com.ibm.ws.transport.iiop.security.config.css;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.config.ConfigUtil;
import com.ibm.ws.transport.iiop.security.config.tss.TSSTransportMechConfig;

/**
 * At the moment, this config class can only handle a single address.
 *
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class CSSSSLTransportConfig implements CSSTransportMechConfig {
    private static final TraceComponent tc = Tr.register(CSSSSLTransportConfig.class);

    private short supports;
    private short requires;

    private String sslConfigName;
    private String cantHandleMsg;

    private boolean lookupOutboundSSLRef = false;

    public void setOutboundSSLReference() {
        lookupOutboundSSLRef = true;
    }

    @Override
    public boolean getOutboundSSLReference() {
        return lookupOutboundSSLRef;
    }

    @Override
    public short getSupports() {
        return supports;
    }

    public void setSupports(short supports) {
        this.supports = supports;
    }

    @Override
    public short getRequires() {
        return requires;
    }

    public void setRequires(short requires) {
        this.requires = requires;
    }

    /**
     * @return the sslConfigName
     */
    @Override
    public String getSslConfigName() {
        return sslConfigName;
    }

    /**
     * @param sslConfigName the sslConfigName to set
     */
    public void setSslConfigName(String sslConfigName) {
        this.sslConfigName = sslConfigName;
    }

    @Override
    public boolean canHandle(TSSTransportMechConfig transMech, String clientMech) {
        cantHandleMsg = null;
        if ((supports & transMech.getRequires()) != transMech.getRequires()) {
            buildSupportsFailedMsg(transMech, clientMech);
            return false;
        }

        if ((requires & transMech.getSupports()) != requires) {
            buildRequiresFailedMsg(transMech, clientMech);
            return false;
        }

        return true;
    }

    @Override
    public String getCantHandleMsg() {
        return cantHandleMsg;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    private void buildSupportsFailedMsg(TSSTransportMechConfig transMech, String clientMech) {
        if (!clientMech.equalsIgnoreCase(CSSNULLASMechConfig.mechanism)) {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_TRANSPORT_SUPPORTS_FAILED",
                                                         new Object[] { clientMech, ConfigUtil.flags(supports), ConfigUtil.flags(transMech.getRequires()) },
                                                         "CWWKS9555E: The client security policy has the transport layer configured for {0} with <{1}> as Supported in the server.xml file and the server security policy is configured with <{2}> as Required.");
        } else {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_TRANSPORT_SUPPORTS_NO_AUTH_FAILED",
                                                         new Object[] { ConfigUtil.flags(supports), ConfigUtil.flags(transMech.getRequires()) },
                                                         "CWWKS9556E: The client security policy has the transport layer configured with <{0}> as Supported in the server.xml file and the server security policy is configured with <{1}> as Required.");
        }
    }

    private void buildRequiresFailedMsg(TSSTransportMechConfig transMech, String clientMech) {
        if (!clientMech.equalsIgnoreCase(CSSNULLASMechConfig.mechanism)) {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_TRANSPORT_REQUIRES_FAILED",
                                                         new Object[] { clientMech, ConfigUtil.flags(requires), ConfigUtil.flags(transMech.getSupports()) },
                                                         "CWWKS9557E: The client security policy has the transport layer configured for {0} with <{1}> as Required in the server.xml file and the server security policy is configured with <{2}> as Supported.");
        } else {
            cantHandleMsg = TraceNLS.getFormattedMessage(this.getClass(),
                                                         com.ibm.ws.security.csiv2.TraceConstants.MESSAGE_BUNDLE,
                                                         "CSIv2_CLIENT_COMPATIBLE_TRANSPORT_REQUIRES_NO_AUTH_FAILED",
                                                         new Object[] { ConfigUtil.flags(requires), ConfigUtil.flags(transMech.getSupports()) },
                                                         "CWWKS9558E: The client security policy has the transport layer configured with <{0}> as Required in the server.xml file and the server security policy is configured with <{1}> as Supported.");
        }
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("CSSSSLTransportConfig: [\n");
        buf.append(moreSpaces).append("SUPPORTS: ").append(ConfigUtil.flags(supports)).append("\n");
        buf.append(moreSpaces).append("REQUIRES: ").append(ConfigUtil.flags(requires)).append("\n");
        buf.append(moreSpaces).append("dynamicSSLEnabled: ").append(lookupOutboundSSLRef).append("\n");
        buf.append(spaces).append("]\n");
    }
}
