/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.http;

public class HttpResponseNot200Exception extends AbstractHttpResponseException {

    private static final long serialVersionUID = 1L;

    public HttpResponseNot200Exception(String url, int statusCode, String errMsg) {
        super(url, statusCode, errMsg);
    }
    
}
