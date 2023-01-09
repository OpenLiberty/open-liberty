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

import com.ibm.ws.jpa.fvt.entity.testlogic.DatatypeSupportTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DatatypeSupport_EJB_SFEX_TestServlet")
public class DatatypeSupport_EJB_SFEX_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = DatatypeSupportTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFExEJB";
        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
    }

    @Test
    public void jpa10_Entity_JavaPrimitiveSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaPrimitiveSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JavaPrimitiveSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaPrimitiveSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JavaPrimitiveSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaPrimitiveSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JavaPrimitiveSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaPrimitiveSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaPrimitiveSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JavaWrapperSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaWrapperSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JavaWrapperSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaWrapperSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JavaWrapperSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaWrapperSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JavaWrapperSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JavaWrapperSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJavaWrapperSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LargeNumericTypeSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_LargeNumericTypeSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LargeNumericTypeSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_LargeNumericTypeSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LargeNumericTypeSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_LargeNumericTypeSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LargeNumericTypeSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_LargeNumericTypeSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testLargeNumericTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_CharArraySupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_CharArraySupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_CharArraySupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_CharArraySupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_CharArraySupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_CharArraySupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_CharArraySupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_CharArraySupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testCharArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ByteArraySupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ByteArraySupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ByteArraySupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ByteArraySupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ByteArraySupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ByteArraySupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ByteArraySupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ByteArraySupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testByteArraySupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_StringSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_StringSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testStringSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_StringSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_StringSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testStringSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_StringSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_StringSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testStringSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_StringSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_StringSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testStringSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_TemporalTypeSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_TemporalTypeSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_TemporalTypeSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_TemporalTypeSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_TemporalTypeSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_TemporalTypeSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_TemporalTypeSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_TemporalTypeSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JDBCTemporalTypeSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JDBCTemporalTypeSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JDBCTemporalTypeSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JDBCTemporalTypeSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JDBCTemporalTypeSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JDBCTemporalTypeSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_JDBCTemporalTypeSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_JDBCTemporalTypeSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testJDBCTemporalTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EnumeratedTypeSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_EnumeratedTypeSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EnumeratedTypeSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_EnumeratedTypeSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EnumeratedTypeSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_EnumeratedTypeSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EnumeratedTypeSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_EnumeratedTypeSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testEnumeratedTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_SerializableTypeSupport_Property_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_SerializableTypeSupport_Property_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_SerializableTypeSupport_Property_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_SerializableTypeSupport_Property_XML_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportPropertyTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_SerializableTypeSupport_Field_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_SerializableTypeSupport_Field_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "DatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_SerializableTypeSupport_Field_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_SerializableTypeSupport_Field_XML_CMEX_EJB_SFEX";
        final String testMethod = "testSerializableTypeSupport";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLDatatypeSupportTestEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
