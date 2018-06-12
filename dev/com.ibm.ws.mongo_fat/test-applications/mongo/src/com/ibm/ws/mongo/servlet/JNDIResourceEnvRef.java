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

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.mongodb.DB;

public class JNDIResourceEnvRef extends AbstractMongoServlet {
    private static final long serialVersionUID = -2753579289423112449L;

    @Override
    DB getDB(boolean authenticated) {
        try {
            return (DB) new InitialContext().lookup("java:comp/env/mongo/testdb-jndi-resource-env-ref");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
