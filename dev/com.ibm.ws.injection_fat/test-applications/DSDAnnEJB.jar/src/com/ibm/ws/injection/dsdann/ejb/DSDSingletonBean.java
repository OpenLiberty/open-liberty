/*******************************************************************************
 * Copyright (c) 2010, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.dsdann.ejb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.logging.Logger;

import javax.annotation.sql.DataSourceDefinition;
import javax.ejb.Singleton;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.sql.DataSource;

@DataSourceDefinition(name = "java:module/ann_SingletonModLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "memory:dsdAnnTest",
                      loginTimeout = 1825,
                      properties = { "createDatabase=create" })
@DataSourceDefinition(name = "java:comp/env/ann_SingletonCompLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "memory:dsdAnnTest",
                      loginTimeout = 1830,
                      properties = { "createDatabase=create" })
@Singleton
public class DSDSingletonBean {
    private static String CLASSNAME = DSDSingletonBean.class.getName();
    private static Logger svLogger = Logger.getLogger(CLASSNAME);

    public void test() {
        svLogger.info("--> Called the Singleton bean.");
    }

    public void testModule() throws Exception {
        svLogger.info("--> Attempting to lookup the DS defined via annotations using java:module");

        InitialContext ctx = new InitialContext();

        DataSource modDS = (DataSource) ctx.lookup("java:module/ann_SingletonModLevelDS");
        assertNotNull("Failed to lookup DS via java:module", modDS);

        DataSource compDS = null;

        try {
            compDS = (DataSource) ctx.lookup("java:comp/ann_SingletonModLevelDS");
        } catch (NameNotFoundException nnfEx) {
        }

        assertNull("Successfully looked up module DS via java:comp", compDS);
    }

    public void testCompEnv() throws Exception {
        svLogger.info("--> Attempting to lookup the DS defined via annotations using java:comp/env");

        InitialContext ctx = new InitialContext();

        DataSource compEnvDS = (DataSource) ctx.lookup("java:comp/env/ann_SingletonCompLevelDS");
        assertNotNull("Failed to lookup DS via java:comp/env", compEnvDS);

        DataSource modDS = null;

        try {
            modDS = (DataSource) ctx.lookup("java:module/env/ann_SingletonCompLevelDS");
        } catch (NameNotFoundException nnfEx) {
        }

        assertNull("Successfully looked up comp/env DS via java:module", modDS);
    }
}