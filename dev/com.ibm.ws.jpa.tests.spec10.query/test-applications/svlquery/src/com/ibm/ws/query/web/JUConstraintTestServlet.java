/**
 *
 */
package com.ibm.ws.query.web;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;

import com.ibm.ws.query.testlogic.JUConstraintTest;
import com.ibm.ws.query.utils.SetupQueryTestCase;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JUConstraintTestServlet")
public class JUConstraintTestServlet extends JPATestServlet {
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
        testClassName = JUConstraintTest.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String isSetupDB = request.getParameter("setupDB");

        if (isSetupDB != null && "true".equalsIgnoreCase(isSetupDB.toLowerCase())) {
            portNumber = request.getLocalPort();
        } else {
            super.doGet(request, response);
        }

//        String isSetupDB = request.getParameter("setupDB");
//
//        if (isSetupDB != null && "true".equalsIgnoreCase(isSetupDB.toLowerCase())) {
//            portNumber = request.getLocalPort();
//            EntityManager em = amrlEmf.createEntityManager();
//            try {
//                setup = new SetupQueryTestCase(em, getDbProductName(), true);
//                setup.setUp("part-composite");
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            } finally {
//                em.close();
//            }
//        } else {
//            super.doGet(request, response);
//        }
    }

    @Test
    public void jpa_spec10_query_svlquery_juconstrainttest_testSelectAllParts_AMJTA_Web() throws Exception {
        executeDDL("JPA_SVLQUERY_DELETE_${dbvendor}.ddl");
        executeDDL("JPA_SVLQUERY_POPULATE_${dbvendor}.ddl");

        final String testName = "jpa_spec10_query_svlquery_juconstrainttest_testSelectAllParts_AMJTA_Web";
        final String testMethod = "testSelectAllParts";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juconstrainttest_testSelectExpensiveParts_AMJTA_Web() throws Exception {
        executeDDL("JPA_SVLQUERY_DELETE_${dbvendor}.ddl");
        executeDDL("JPA_SVLQUERY_POPULATE_${dbvendor}.ddl");

        final String testName = "jpa_spec10_query_svlquery_juconstrainttest_testSelectExpensiveParts_AMJTA_Web";
        final String testMethod = "testSelectExpensiveParts";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juconstrainttest_testCheckCompositePartAssemblyForCycle_AMJTA_Web() throws Exception {
        executeDDL("JPA_SVLQUERY_DELETE_${dbvendor}.ddl");
        executeDDL("JPA_SVLQUERY_POPULATE_${dbvendor}.ddl");

        final String testName = "jpa_spec10_query_svlquery_juconstrainttest_testCheckCompositePartAssemblyForCycle_AMJTA_Web";
        final String testMethod = "testCheckCompositePartAssemblyForCycle";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_query_svlquery_juconstrainttest_testCalculateCompositePartAssemblyTotalCostAndTotalMass_AMJTA_Web() throws Exception {
        executeDDL("JPA_SVLQUERY_DELETE_${dbvendor}.ddl");
        executeDDL("JPA_SVLQUERY_POPULATE_${dbvendor}.ddl");

        final String testName = "jpa_spec10_query_svlquery_juconstrainttest_testCalculateCompositePartAssemblyTotalCostAndTotalMass_AMJTA_Web";
        final String testMethod = "testCalculateCompositePartAssemblyTotalCostAndTotalMass";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }
}
