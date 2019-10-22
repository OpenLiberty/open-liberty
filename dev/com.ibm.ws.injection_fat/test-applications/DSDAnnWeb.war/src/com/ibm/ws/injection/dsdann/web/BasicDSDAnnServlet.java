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
package com.ibm.ws.injection.dsdann.web;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.annotation.sql.DataSourceDefinition;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.injection.dsdann.ejb.DSDSingletonBean;
import com.ibm.ws.injection.dsdann.ejb.DSDStatefulBean;

import componenttest.app.FATServlet;

@DataSourceDefinition(name = "java:module/ann_ServletModLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "memory:dsdAnnTest",
                      loginTimeout = 1625,
                      properties = { "createDatabase=create" })
@DataSourceDefinition(name = "java:comp/env/ann_ServletCompEnvLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "memory:dsdAnnTest",
                      loginTimeout = 1630,
                      properties = { "createDatabase=create" })
@DataSourceDefinition(name = "java:comp/ann_ServletCompLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "memory:dsdAnnTest",
                      loginTimeout = 1617,
                      properties = { "createDatabase=create" })
@DataSourceDefinition(name = "java:app/ann_ServletAppLevelDS",
                      className = "org.apache.derby.jdbc.EmbeddedXADataSource40",
                      databaseName = "memory:dsdAnnTest",
                      loginTimeout = 1602,
                      properties = { "createDatabase=create" })
@SuppressWarnings("serial")
@WebServlet("/BasicDSDAnnServlet")
public class BasicDSDAnnServlet extends FATServlet {
    private static final String CLASSNAME = BasicDSDAnnServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASSNAME);

    /**
     * Lookup the bean and call the testDS method with the jndi name provided by
     * the test method and verify it returns the expected loginTimeout value of
     * the defined DataSource
     *
     * @param jndi
     * @param loginTO
     * @throws Exception
     */
    public void getAndVerifyResult(String jndi, int loginTO) throws Exception {
        svLogger.info("--> Looking up bean...");
        DSDStatefulBean bean = (DSDStatefulBean) FATHelper.lookupDefaultBindingEJBJavaApp(DSDStatefulBean.class.getName(), "DSDAnnEJB", "DSDStatefulBean");

        svLogger.info("--> Calling test() on the SFSB that defines the DS...");
        int result = bean.testDS(jndi);
        svLogger.info("--> result = " + result);

        assertTrue("--> Expecting the returned login timeout for the DS to be " + loginTO + ". Actual value = " + result, loginTO == result);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the
     * java:module namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testDSDModLevel() throws Exception {
        getAndVerifyResult("java:module/ann_BasicModLevelDS", 1814);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the java:app
     * namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testDSDAppLevel() throws Exception {
        getAndVerifyResult("java:app/ann_BasicAppLevelDS", 1819);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the
     * java:global namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testDSDGlobalLevel() throws Exception {
        getAndVerifyResult("java:global/ann_BasicGlobalLevelDS", 1806);
    }

    /**
     * Verify that a DS defined in a SFSB using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the java:comp
     * namespace
     *
     * @throws Exception
     */
    @Test
    public void testDSDCompLevel() throws Exception {
        getAndVerifyResult("java:comp/env/ann_BasicCompLevelDS", 1815);
    }

    /**
     * Verify that a DS defined in a Singleton using the DataSourceDefinition
     * annotation can be successfully looked up from the SFSB using the
     * java:module
     * namespace
     *
     * @throws Exception
     *
     */
    @Test
    public void testSingletonDSDModLevel() throws Exception {
        svLogger.info("--> Looking up the Singleton bean...");
        DSDSingletonBean bean = (DSDSingletonBean) FATHelper.lookupDefaultBindingEJBJavaApp(DSDSingletonBean.class.getName(), "DSDAnnEJB", "DSDSingletonBean");

        svLogger.info("-->The DS defined in the Singleton bean should have been created when the Singleton was looked up (i.e. initialized), just to be sure we call its test()...");
        bean.test();

        getAndVerifyResult("java:module/ann_SingletonModLevelDS", 1825);
    }

    /**
     * Verify that a DS defined in a Singleton using java:module can be
     * looked up via java:module, but not java:comp
     *
     * @throws Exception
     *
     */
    @Test
    public void testSingletonModuleDSD() throws Exception {
        svLogger.info("--> Looking up the Singleton bean...");
        DSDSingletonBean bean = (DSDSingletonBean) FATHelper.lookupDefaultBindingEJBJavaApp(DSDSingletonBean.class.getName(), "DSDAnnEJB", "DSDSingletonBean");

        svLogger.info("-->The DS defined in the Singleton bean should have been created when the Singleton was looked up (i.e. initialized), just to be sure we call its test()...");
        bean.test();

        bean.testModule();
    }

    /**
     * Verify that a DS defined in a Singleton using java:comp/env can be
     * looked up via java:comp/env, but not java:module
     *
     * @throws Exception
     *
     */
    @Test
    public void testSingletonCompEnvDSD() throws Exception {
        svLogger.info("--> Looking up the Singleton bean...");
        DSDSingletonBean bean = (DSDSingletonBean) FATHelper.lookupDefaultBindingEJBJavaApp(DSDSingletonBean.class.getName(), "DSDAnnEJB", "DSDSingletonBean");

        svLogger.info("-->The DS defined in the Singleton bean should have been created when the Singleton was looked up (i.e. initialized), just to be sure we call its test()...");
        bean.test();

        bean.testCompEnv();
    }

    @Test
    public void testServletModuleDSD() throws Exception {
        svLogger.info("--> Attempting to lookup the java:module DS defined via annotations using java:module");

        InitialContext ctx = new InitialContext();

        DataSource modDS = (DataSource) ctx.lookup("java:module/ann_ServletModLevelDS");
        assertNotNull("Failed to lookup DS via java:module", modDS);

        svLogger.info("--> Attempting to lookup the java:module DS defined via annotations using java:comp");

        DataSource compDS = (DataSource) ctx.lookup("java:comp/ann_ServletModLevelDS");
        assertNotNull("Failed to look up module DS via java:comp", compDS);
    }

    @Test
    public void testServletCompEnvDSD() throws Exception {
        svLogger.info("--> Attempting to lookup the java:comp/env DS defined via annotations using java:comp/env");

        InitialContext ctx = new InitialContext();

        DataSource compEnvDS = (DataSource) ctx.lookup("java:comp/env/ann_ServletCompEnvLevelDS");
        assertNotNull("Failed to lookup DS via java:comp/env", compEnvDS);

        svLogger.info("--> Attempting to lookup the java:comp/env DS defined via annotations using java:module");

        DataSource modDS = (DataSource) ctx.lookup("java:module/env/ann_ServletCompEnvLevelDS");
        assertNotNull("Failed to look up comp/env DS via java:module", modDS);
    }

    @Test
    public void testServletCompDSD() throws Exception {
        svLogger.info("--> Attempting to lookup the java:comp DS defined via annotations using java:comp");

        InitialContext ctx = new InitialContext();

        DataSource compDS = (DataSource) ctx.lookup("java:comp/ann_ServletCompLevelDS");
        assertNotNull("Failed to lookup DS via java:comp", compDS);

        svLogger.info("--> Attempting to lookup the java:comp DS defined via annotations using java:module");

        DataSource modDS = (DataSource) ctx.lookup("java:module/ann_ServletCompLevelDS");
        assertNotNull("Failed to look up comp DS via java:module", modDS);
    }

    @Test
    public void testServletAppDSD() throws Exception {
        svLogger.info("--> Attempting to lookup the java:app DS defined via annotations using java:app");

        InitialContext ctx = new InitialContext();

        DataSource appDS = (DataSource) ctx.lookup("java:app/ann_ServletAppLevelDS");
        assertNotNull("Failed to lookup DS via java:app", appDS);

        svLogger.info("--> Attempting to lookup the java:app DS defined via annotations using java:comp");

        DataSource compDS = null;
        try {
            compDS = (DataSource) ctx.lookup("java:comp/ann_ServletAppLevelDS");
        } catch (NameNotFoundException nnfEx) {
        }

        assertNull("Successfully looked up app DS via java:comp", compDS);
    }
}