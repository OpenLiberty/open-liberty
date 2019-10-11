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
 * Tests injection of CouchDB instance using a URL in configuration.
 */
@WebServlet("/InjectedURLCouchDbServlet")
public class InjectedURLCouchDbServlet extends AbstractCouchDbServlet {
    private static final long serialVersionUID = 1L;
    private final static Logger logger = Logger.getLogger(InjectedURLCouchDbServlet.class.getName());

    @Resource(name = "couchdb/testdb-url")
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
     * Tests insert/find/delete operations using a CouchDB instance that has been injected using a URL.
     */
    @Test
    public void testInsertFindInjectURL() throws Exception {
        final String method = "testInsertFindInjectURL";
        logger.info("entering " + method);
        insertFindScenario();
        logger.info("exiting " + method);
    }

}
