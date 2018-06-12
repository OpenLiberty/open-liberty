/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo.servlet;

import javax.annotation.Resource;

import com.mongodb.DB;

public class SSLCertAuthSSLEnabledFalseServlet extends AbstractMongoServlet {
    private static final long serialVersionUID = 1L;

    @Resource(name = "mongo/testdb-invalid-certauth-sslenabled-false")
    protected DB _db;

    @Override
    DB getDB(boolean authenticated) {
        return _db;
    }
}
