/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jaxws.client.injection;

import javax.naming.RefAddr;

import com.ibm.ws.jaxws.metadata.WebServiceRefInfo;

/**
 * This class serves as a "holder" for our WebServiceRefInfo clas. This holder object will be constructed with an
 * instance of our WebServiceRefInfo class (serializable class which holds service-ref related metadata) and added to
 * naming Reference object that represents a service-ref in the JNDI namespace.
 */
public class WebServiceRefInfoRefAddr extends RefAddr {

    private static final long serialVersionUID = 5645835508590997002L;

    // This is our address type key string
    public static final String ADDR_KEY = "WebServiceRefInfo";

    // Info object
    private WebServiceRefInfo wsrInfo = null;

    public WebServiceRefInfoRefAddr(WebServiceRefInfo info) {
        super(ADDR_KEY);
        this.wsrInfo = info;
    }

    @Override
    public Object getContent() {
        return wsrInfo;
    }
}
