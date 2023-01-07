/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.tests.ejb.sfex;

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
@WebServlet(urlPatterns = "/Serializable_EJB_SFEX_TestServlet")
public class Serializable_EJB_SFEX_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = SerializableTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaPrimitiveSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JavaWrapperSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_LargeNumericSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_CharArraySerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_ByteArraySerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_StringSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testStringSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_TemporalTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_JDBCTemporalTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_EnumeratedTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Serializable_SerializableTypeSerializableSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSerializableSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "SerializableXMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
