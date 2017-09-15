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

import java.io.Serializable;

import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;

/**
 * @version $Rev: 503274 $ $Date: 2007-02-03 10:19:18 -0800 (Sat, 03 Feb 2007) $
 */
public interface CSSASMechConfig extends Serializable {

    short getSupports();

    short getRequires();

    boolean canHandle(TSSASMechConfig asMech);

    String getMechanism();

    /**
     * Encode the client authentication token
     * 
     * @param tssasMechConfig
     * @param sas_mech
     * @param ri
     * @param codec
     * @return the encoded client authentication token
     */
    byte[] encode(TSSASMechConfig tssasMechConfig, CSSSASMechConfig sas_mech, ClientRequestInfo ri, Codec codec);

    void toString(String spaces, StringBuilder buf);

}
