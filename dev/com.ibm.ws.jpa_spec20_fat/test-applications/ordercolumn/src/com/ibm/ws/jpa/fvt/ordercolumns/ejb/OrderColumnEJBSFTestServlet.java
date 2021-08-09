/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.ordercolumns.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.ordercolumns.OrderColumnTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/OrderColumnEJBSFTestServlet")
public class OrderColumnEJBSFTestServlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = OrderColumnTestLogic.class.getName();
        ejbJNDIName = "ejb/OrderColumnSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M_AMJTA_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M_AMJTA_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M_AMRL_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M_AMRL_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M_CMTS_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M_CMTS_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M__AMJTA_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M__AMJTA_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M__AMRL_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M__AMRL_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M__CMTS_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M__CMTS_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M_AMJTA_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M_AMJTA_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M_AMRL_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M_AMRL_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M_CMTS_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M_CMTS_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M__AMJTA_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M__AMJTA_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M__AMRL_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M__AMRL_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M__CMTS_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M__CMTS_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M_AMJTA_Annotated_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M_AMJTA_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M_AMRL_Annotated_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M_AMRL_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M_CMTS_Annotated_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M_CMTS_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M__AMJTA_XML_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M__AMJTA_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M__AMRL_XML_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M__AMRL_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M__CMTS_XML_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M__CMTS_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M_AMJTA_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M_AMJTA_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M_AMRL_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M_AMRL_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M_CMTS_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M_CMTS_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M__AMJTA_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M__AMJTA_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M__AMRL_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M__AMRL_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M__CMTS_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M__CMTS_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement_AMJTA_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement_AMJTA_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement_AMRL_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement_AMRL_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement_CMTS_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement_CMTS_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement__AMJTA_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement__AMJTA_XML_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement__AMRL_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement__AMRL_XML_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement__CMTS_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement__CMTS_XML_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }
}
