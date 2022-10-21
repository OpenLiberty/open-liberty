/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.tests.ejb.sf;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.SerializableTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/Serializable_EJB_SF_TestServlet")
public class Serializable_EJB_SF_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = SerializableTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMTS"));
        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_AMJTA_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_AMJTA_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_AMRL_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_AMRL_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_CMTS_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_CMTS_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_AMJTA_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_AMJTA_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_AMRL_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_AMRL_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_CMTS_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_CMTS_EJB_SF";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
