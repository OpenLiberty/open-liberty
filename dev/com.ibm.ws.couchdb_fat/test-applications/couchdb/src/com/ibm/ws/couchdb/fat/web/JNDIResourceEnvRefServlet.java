/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.couchdb.fat.web;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.junit.Test;

/**
 * Tests indirect JNDI lookup of CouchDB instance (resource-env-ref).
 */
@WebServlet("/JNDIResourceEnvRefServlet")
public class JNDIResourceEnvRefServlet extends AbstractCouchDbServlet {
    private static final long serialVersionUID = -2753579289423112449L;
    private final static Logger logger = Logger.getLogger(JNDIResourceEnvRefServlet.class.getName());

    @Override
    CouchDbConnector getDB() {
        try {
            CouchDbInstance db = (CouchDbInstance) new InitialContext().lookup("java:comp/env/couchdb/testdb-jndi-resource-env-ref");
            CouchDbConnector dbc = db.createConnector(DATABASE, true);
            return dbc;
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    CouchDbInstance getDBInstance() {
        try {
            return (CouchDbInstance) new InitialContext().lookup("java:comp/env/couchdb/testdb-jndi-resource-env-ref");
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests insert/find/delete operations using a CouchDB instance that has been indirectly
     * looked up in JNDI using a resource environment reference.
     */
    @Test
    public void testInsertFindJNDI() throws Exception {
        final String method = "testInsertFindJNDI";
        logger.info("entering " + method);
        insertFindScenario();
        logger.info("exiting " + method);
    }

    /**
     * Tests insert/query/delete operations of state documents using a CouchDB instance that
     * has been indirectly looked up in JNDI using a resource environment reference.
     */
    @Test
    public void testInsertStateDocuments() throws Exception {
        final String method = "testInsertStateDocuments";
        logger.info("entering " + method);
        insertStateDocument(getDB());
        logger.info("exiting " + method);
    }

}
