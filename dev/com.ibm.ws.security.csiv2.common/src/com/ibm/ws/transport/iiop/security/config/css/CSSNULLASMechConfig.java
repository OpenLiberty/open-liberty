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
import com.ibm.ws.transport.iiop.security.config.tss.TSSNULLASMechConfig;

/**
 * @version $Revision: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public class CSSNULLASMechConfig implements CSSASMechConfig {

    public static final String mechanism = "DISABLED";

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
        if (asMech instanceof TSSNULLASMechConfig)
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
        return new byte[0];
    }

    @Override
    @Trivial
    public void toString(String spaces, StringBuilder buf) {
        buf.append(spaces).append("CSSNULLASMechConfig\n");
    }
}
