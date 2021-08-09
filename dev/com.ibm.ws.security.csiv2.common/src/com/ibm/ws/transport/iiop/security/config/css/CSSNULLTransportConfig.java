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
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.config.ConfigUtil;
import com.ibm.ws.transport.iiop.security.config.tss.TSSTransportMechConfig;

/**
 * @version $Revision: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class CSSNULLTransportConfig implements CSSTransportMechConfig {

    private short supports;
    private short requires;
    private String cantHandleMsg = null;

    @Override
    public short getSupports() {
        return supports;
    }

    @Override
    public short getRequires() {
        return requires;
    }

    @Override
    public boolean canHandle(TSSTransportMechConfig transMech, String clientMech) {
        if ((supports & transMech.getRequires()) != transMech.getRequires()) {
            buildRequiresFailedMsg(transMech, clientMech);
            return false;
        }
        if ((requires & transMech.getSupports()) != requires) {
            buildSupportsFailedMsg(transMech, clientMech);
            return false;
        }

        return true;
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
        buf.append(spaces).append("CSSNULLTransportConfig\n");
    }

    @Override
    public String getSslConfigName() {
        return null;
    }

    @Override
    public String getCantHandleMsg() {
        return cantHandleMsg;
    }

    @Override
    public boolean getOutboundSSLReference() {
        return false;
    }

}
