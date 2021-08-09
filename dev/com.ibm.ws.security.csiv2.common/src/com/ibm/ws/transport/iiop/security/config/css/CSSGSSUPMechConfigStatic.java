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

import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 * @version $Revision: 503493 $ $Date: 2007-02-04 13:47:55 -0800 (Sun, 04 Feb 2007) $
 */
public class CSSGSSUPMechConfigStatic implements CSSASMechConfig {

    private final String username;
    private final SerializableProtectedString password;
    private final String domain;
    private transient byte[] encoding;
    private final String mechanism = "GSSUP";

    public CSSGSSUPMechConfigStatic(String username, SerializableProtectedString password, String domain) {
        this.username = username;
        this.password = password;
        this.domain = domain;
    }

    @Override
    public short getSupports() {
        return 0;
    }

    @Override
    public short getRequires() {
        return 0;
    }

    @Override
    public boolean canHandle(TSSASMechConfig asMech) {
        if (asMech instanceof TSSGSSUPMechConfig)
            return true;
        if (asMech.getRequires() == 0)
            return true;

        return false;
    }

    @Override
    public String getMechanism() {
        return mechanism;
    }

    @Override
    public byte[] encode(TSSASMechConfig tssasMechConfig, CSSSASMechConfig sas_mech, ClientRequestInfo ri, Codec codec) {
        byte[] currentEncoding = null;

        if (tssasMechConfig instanceof TSSGSSUPMechConfig) {
            if (encoding == null) {
                String scopedUserName = Util.buildScopedUserName(username, domain);
                encoding = Util.encodeGSSUPToken(codec, scopedUserName, password.getChars(), domain);
            }
            currentEncoding = encoding;
        }

        if (currentEncoding == null) {
            currentEncoding = new byte[0];
        }
        return currentEncoding;
    }

    @Override
    @Trivial
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toString("", buf);
        return buf.toString();
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        String moreSpaces = spaces + "  ";
        buf.append(spaces).append("CSSGSSUPMechConfigStatic: [\n");
        buf.append(moreSpaces).append("username: ").append(username).append("\n");
        buf.append(moreSpaces).append("password: ").append(password.toTraceString()).append("\n");
        buf.append(moreSpaces).append("domain:   ").append(domain).append("\n");
        buf.append(spaces).append("]\n");
    }

}
