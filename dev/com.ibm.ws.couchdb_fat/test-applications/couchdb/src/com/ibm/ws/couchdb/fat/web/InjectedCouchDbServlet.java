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

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.junit.Test;

/**
 * Tests injection of CouchDB instance using host and port configuration.
 */
@WebServlet("/InjectedCouchDbServlet")
public class InjectedCouchDbServlet extends AbstractCouchDbServlet {
    private static final long serialVersionUID = 1L;
    private final static Logger logger = Logger.getLogger(InjectedCouchDbServlet.class.getName());

    @Resource(name = "couchdb/testdb")
    protected CouchDbInstance _db;

    @Override
    CouchDbConnector getDB() {
        CouchDbConnector dbc = _db.createConnector(DATABASE, true);
        return dbc;
    }

    @Override
    CouchDbInstance getDBInstance() {
        return _db;
    }

    /**
     * Tests insert/find/delete operations using a CouchDB instance that has been injected using host and port.
     */
    @Test
    public void testInsertFindInject() throws Exception {
        final String method = "testInsertFindInject";
        logger.info("entering " + method);
        insertFindScenario();
        logger.info("exiting " + method);
    }

}
