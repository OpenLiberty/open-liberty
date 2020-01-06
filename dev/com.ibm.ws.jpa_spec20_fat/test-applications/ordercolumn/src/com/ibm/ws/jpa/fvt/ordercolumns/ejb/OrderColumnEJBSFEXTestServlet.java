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
@WebServlet(urlPatterns = "/OrderColumnEJBSFEXTestServlet")
public class OrderColumnEJBSFEXTestServlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = OrderColumnTestLogic.class.getName();
        ejbJNDIName = "ejb/OrderColumnSFEXEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.FIELD, "cmexEm"));
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M_CMEX_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M_CMEX_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_O2M__CMEX_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_O2M__CMEX_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "uo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M_CMEX_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M_CMEX_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnUni_M2M__CMEX_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnUni_M2M__CMEX_XML_EJB_SF";
        final String testMethod = "testOrderColumnUni";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "um2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M_CMEX_Annotated_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M_CMEX_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_O2M__CMEX_XML_EJB_SF() throws Exception {
        if (!isUsingJPA20Feature()) {
            // TODO: Investigate why this fails on EclipseLink
            return;
        }
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_O2M__CMEX_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bo2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M_CMEX_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M_CMEX_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnBi_M2M__CMEX_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnBi_M2M__CMEX_XML_EJB_SF";
        final String testMethod = "testOrderColumnBi";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "bm2mNames");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement_CMEX_Annotated_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement_CMEX_Annotated_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "Annotated");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa_spec20_ordercolumn_testOrderColumnElement__CMEX_XML_EJB_SF() throws Exception {
        final String testName = "jpa_spec20_ordercolumn_testOrderColumnElement__CMEX_XML_EJB_SF";
        final String testMethod = "testOrderColumnElement";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XML");
        properties.put("listFieldName", "listElements");

        executeDDL("JPA_ORDERCOLUMN_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }
}
