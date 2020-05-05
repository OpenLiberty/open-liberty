/**
 *
 */
package com.ibm.ws.query.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.query.testlogic.JULoopQueryAnoTest;
import com.ibm.ws.query.utils.SetupQueryTestCase;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JULoopQueryAnoTestServlet")
public class JULoopQueryAnoTestServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "QUERY_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "QUERY_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "QUERY_RL")
    private EntityManagerFactory amrlEmf;

    private SetupQueryTestCase setup = null;

    @PostConstruct
    private void initFAT() {
        testClassName = JULoopQueryAnoTest.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));

    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test001_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test001_AMJTA_Web";
        final String testMethod = "testLoop001";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test002_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test002_AMJTA_Web";
        final String testMethod = "testLoop002";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test003_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test003_AMJTA_Web";
        final String testMethod = "testLoop003";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test004_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test004_AMJTA_Web";
        final String testMethod = "testLoop004";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test005_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test005_AMJTA_Web";
        final String testMethod = "testLoop005";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test006_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test006_AMJTA_Web";
        final String testMethod = "testLoop006";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test007_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test007_AMJTA_Web";
        final String testMethod = "testLoop007";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test008_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test008_AMJTA_Web";
        final String testMethod = "testLoop008";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test009_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test009_AMJTA_Web";
        final String testMethod = "testLoop009";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test010_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test010_AMJTA_Web";
        final String testMethod = "testLoop010";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test011_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test011_AMJTA_Web";
        final String testMethod = "testLoop011";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test012_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test012_AMJTA_Web";
        final String testMethod = "testLoop012";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test013_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test013_AMJTA_Web";
        final String testMethod = "testLoop013";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test014_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test014_AMJTA_Web";
        final String testMethod = "testLoop014";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test015_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test015_AMJTA_Web";
        final String testMethod = "testLoop015";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test016_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test016_AMJTA_Web";
        final String testMethod = "testLoop016";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test017_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test017_AMJTA_Web";
        final String testMethod = "testLoop017";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test018_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test018_AMJTA_Web";
        final String testMethod = "testLoop018";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    // TODO: Fails, review failure
//    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test019_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test019_AMJTA_Web";
        final String testMethod = "testLoop019";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test020_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test020_AMJTA_Web";
        final String testMethod = "testLoop020";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test021_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test021_AMJTA_Web";
        final String testMethod = "testLoop021";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test022_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test022_AMJTA_Web";
        final String testMethod = "testLoop022";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test023_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test023_AMJTA_Web";
        final String testMethod = "testLoop023";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test024_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test024_AMJTA_Web";
        final String testMethod = "testLoop024";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test025_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test025_AMJTA_Web";
        final String testMethod = "testLoop025";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test026_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test026_AMJTA_Web";
        final String testMethod = "testLoop026";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test027_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test027_AMJTA_Web";
        final String testMethod = "testLoop027";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test028_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test028_AMJTA_Web";
        final String testMethod = "testLoop028";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test029_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test029_AMJTA_Web";
        final String testMethod = "testLoop029";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test030_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test030_AMJTA_Web";
        final String testMethod = "testLoop030";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test031_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test031_AMJTA_Web";
        final String testMethod = "testLoop031";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test032_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test032_AMJTA_Web";
        final String testMethod = "testLoop032";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test033_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test033_AMJTA_Web";
        final String testMethod = "testLoop033";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test034_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test034_AMJTA_Web";
        final String testMethod = "testLoop034";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test035_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test035_AMJTA_Web";
        final String testMethod = "testLoop035";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test036_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test036_AMJTA_Web";
        final String testMethod = "testLoop036";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test037_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test037_AMJTA_Web";
        final String testMethod = "testLoop037";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test038_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test038_AMJTA_Web";
        final String testMethod = "testLoop038";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    // TODO: Follow up on test fail
    //@Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test039_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test039_AMJTA_Web";
        final String testMethod = "testLoop039";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test040_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test040_AMJTA_Web";
        final String testMethod = "testLoop040";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test041_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test041_AMJTA_Web";
        final String testMethod = "testLoop041";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test042_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test042_AMJTA_Web";
        final String testMethod = "testLoop042";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test043_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test043_AMJTA_Web";
        final String testMethod = "testLoop043";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test044_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test044_AMJTA_Web";
        final String testMethod = "testLoop044";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test045_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test045_AMJTA_Web";
        final String testMethod = "testLoop045";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test046_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test046_AMJTA_Web";
        final String testMethod = "testLoop046";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test047_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test047_AMJTA_Web";
        final String testMethod = "testLoop047";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    // TODO: Fails on SQL Server
    //@Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test048_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test048_AMJTA_Web";
        final String testMethod = "testLoop048";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test049_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test049_AMJTA_Web";
        final String testMethod = "testLoop049";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test050_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test050_AMJTA_Web";
        final String testMethod = "testLoop050";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test051_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test051_AMJTA_Web";
        final String testMethod = "testLoop051";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test052_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test052_AMJTA_Web";
        final String testMethod = "testLoop052";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test053_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test053_AMJTA_Web";
        final String testMethod = "testLoop053";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test054_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test054_AMJTA_Web";
        final String testMethod = "testLoop054";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test055_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test055_AMJTA_Web";
        final String testMethod = "testLoop055";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test056_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test056_AMJTA_Web";
        final String testMethod = "testLoop056";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test057_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test057_AMJTA_Web";
        final String testMethod = "testLoop057";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test058_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test058_AMJTA_Web";
        final String testMethod = "testLoop058";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test059_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test059_AMJTA_Web";
        final String testMethod = "testLoop059";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test060_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test060_AMJTA_Web";
        final String testMethod = "testLoop060";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test061_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test061_AMJTA_Web";
        final String testMethod = "testLoop061";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juloopquery_ano_test062_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_query_svlquery_juloopquery_ano_test062_AMJTA_Web";
        final String testMethod = "testLoop062";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }
}
