/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api.testobjects;

import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.wsspi.http.channel.values.SchemeValues;

/**
 * Testable version of the HTTP request message that does not require an
 * underlying socket connection.
 */
public class MockRequestMessage extends HttpRequestMessageImpl {
    private HttpServiceContextImpl mySC;

    /**
     * Constructor.
     * 
     * @param sc
     */
    public MockRequestMessage(HttpServiceContextImpl sc) {
        super();
        mySC = sc;
        init(sc);
    }

    public void initScheme() {
        setScheme(SchemeValues.HTTP);
    }

    protected byte[] getResource() {
        return getRequestURIAsByteArray();
    }

    public HttpServiceContextImpl getServiceContext() {
        return this.mySC;
    }

}
