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
package com.ibm.ws.security.csiv2.config;

import com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig;

/**
 * Pair of compatible client and server CSIv2 compound sec mechs.
 */
public class CompatibleMechanisms {

    private final CSSCompoundSecMechConfig clientMech;
    private final TSSCompoundSecMechConfig serverMech;

    /**
     * @param clientMech The client side compound sec mech configuration.
     * @param serverMech The server side compound sec mech configuration.
     */
    public CompatibleMechanisms(CSSCompoundSecMechConfig clientMech, TSSCompoundSecMechConfig serverMech) {
        this.clientMech = clientMech;
        this.serverMech = serverMech;
    }

    public CSSCompoundSecMechConfig getCSSCompoundSecMechConfig() {
        return clientMech;
    }

    public TSSCompoundSecMechConfig getTSSCompoundSecMechConfig() {
        return serverMech;
    }

}
