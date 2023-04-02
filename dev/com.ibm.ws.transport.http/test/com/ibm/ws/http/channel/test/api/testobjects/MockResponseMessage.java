/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.test.api.testobjects;

import com.ibm.ws.http.channel.internal.HttpResponseMessageImpl;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;

/**
 * Testable version of the response message that does not require an
 * underlying socket connection.
 */
public class MockResponseMessage extends HttpResponseMessageImpl {
    /**
     * Constructor.
     * 
     * @param sc
     */
    public MockResponseMessage(HttpServiceContextImpl sc) {
        super();
        init(sc);
    }

}
