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
package com.ibm.ws.ejbcontainer.remote.client.internal.injection;

import javax.naming.RefAddr;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A RefAddr to a EJBLinkInfo object. (Client version)
 */
public class EJBLinkClientInfoRefAddr extends RefAddr {
    private static final TraceComponent tc = Tr.register(EJBLinkClientInfoRefAddr.class);

    private static final long serialVersionUID = -1172693812040793208L;

    static final String ADDR_TYPE = "EJBLinkClientInfo";

    private final EJBLinkClientInfo ivInfo;

    /**
     * Constructs a new instance.
     */
    @Trivial
    public EJBLinkClientInfoRefAddr(EJBLinkClientInfo info) {
        super(ADDR_TYPE);
        ivInfo = info;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "EJBLinkClientInfoRefAddr.<init> : " + ivInfo);
        }
    }

    /**
     * @see javax.naming.RefAddr#getContent()
     */
    @Override
    @Trivial
    public Object getContent() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "EJBLinkClientInfoRefAddr.getContent() returning : " + ivInfo);
        }

        return ivInfo;
    }
}
