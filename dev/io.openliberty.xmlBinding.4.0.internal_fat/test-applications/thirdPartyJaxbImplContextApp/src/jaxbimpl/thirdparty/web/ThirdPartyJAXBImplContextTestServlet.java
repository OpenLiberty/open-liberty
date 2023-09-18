/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package jaxbimpl.thirdparty.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.Test;

import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.SchemaOutputResolver;
import jakarta.xml.bind.Unmarshaller;
import jaxb.web.dataobjects.Items;
import jaxb.web.dataobjects.ShippingAddress;
import jaxb.web.utils.JAXBContextUtils;
import jaxb.web.utils.JAXBXMLSchemaConstants;
import jaxb.web.utils.StringSchemaOutputResolver;

/**
 * Testing third party XML Binding implementations
 *
 */
@SuppressWarnings("serial")
@WebServlet("/JAXBContextTestServlet")
public class ThirdPartyJAXBImplContextTestServlet extends FATServlet {

    private JAXBContext thirdPartySystemPropertyContext = null;
    private JAXBContext thirdPartyPropertyMapContext = null;

    private static ClassLoader classLoader = jaxb.web.dataobjects.ObjectFactory.class.getClassLoader();

    /*
    *
    */
    @Test
    public void testJAXBContextThirdPartyImplLoadedWithPropertyMap() throws Exception {
        if (thirdPartyPropertyMapContext == null)
            thirdPartyPropertyMapContext = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                            jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());

        Class<?> clazz = thirdPartyPropertyMapContext.getClass();

        System.out.println("testEE10PropertyMapContextMarshalling: using reflection to assert that thirdPartyPropertyMapContext's name, "
                           + clazz.getName() + ", is equal to "
                           + "org.eclipse.persistence.jaxb.JAXBContext");
        assertEquals("org.eclipse.persistence.jaxb.JAXBContext", clazz.getName());
    }

    /*
    *
    */
    @Test
    public void testJAXBContextThirdPartyImplLoadedWithSystemProperty() throws Exception {
        if (thirdPartySystemPropertyContext == null)
            thirdPartySystemPropertyContext = JAXBContextUtils.setupJAXBContextWithSystemProperty(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                                  classLoader);

        Class<?> clazz = thirdPartySystemPropertyContext.getClass();

        System.out.println("testEE10SystemPropertyContextMarshalling: using reflection to assert that thirdPartySystemPropertyContext's name, "
                           + clazz.getName() + ", is equal to "
                           + "org.eclipse.persistence.jaxb.JAXBContext");
        assertEquals("org.eclipse.persistence.jaxb.JAXBContext", clazz.getName());
    }

    // Checking the functionality of third party XML Binding marshalling implementations. Detailed test of third party implementations are
    // out of IBM's test scope
    @Test
    public void testEE10PropertyMapContextMarshalling() throws Exception {

        if (thirdPartyPropertyMapContext == null)
            thirdPartyPropertyMapContext = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                            jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());

        String purchaseOrderTypeMarshalledResult = JAXBContextUtils.marshallForTest(thirdPartyPropertyMapContext);

        assertNotNull("Property map context marshalling operation using third party XML Binding implementation failed", purchaseOrderTypeMarshalledResult);
        assertFalse("Property map context marshalling operation using third party XML Binding implementation failed", purchaseOrderTypeMarshalledResult.isBlank());
    }

    // Checking the functionality of third party XML Binding marshalling implementations. Detailed test of third party implementations are
    // out of IBM's test scope
    @Test
    public void testEE10SystemPropertyContextMarshalling() throws Exception {

        if (thirdPartySystemPropertyContext == null)
            thirdPartySystemPropertyContext = JAXBContextUtils.setupJAXBContextWithSystemProperty(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                                  classLoader);

        String purchaseOrderTypeMarshalledResult = JAXBContextUtils.marshallForTest(thirdPartySystemPropertyContext);

        assertNotNull("System property context marshalling operation using third party XML Binding implementation failed", purchaseOrderTypeMarshalledResult);
        assertFalse("System property context marshalling operation using third party XML Binding implementation failed", purchaseOrderTypeMarshalledResult.isBlank());
    }

    @Test
    public void testEE10SystemPropertyContextUnmarshallItemsObject() throws Exception {

        if (thirdPartySystemPropertyContext == null)
            thirdPartySystemPropertyContext = JAXBContextUtils.setupJAXBContextWithSystemProperty(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                                  jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());

        Unmarshaller unmarshaller = thirdPartySystemPropertyContext.createUnmarshaller();

        Items unmarshalledItems = (Items) unmarshaller.unmarshal(new StringReader(JAXBXMLSchemaConstants.EXPECTED_ITEM_MARSHALLED_RESULT));

        assertTrue("Expected unmarshalled version of the Items type to match the created version, but they did not",
                   JAXBContextUtils.compareItems(unmarshalledItems, JAXBContextUtils.getItems()));

    }

    @Test
    public void testEE10PropertyMapContextUnmarshallItemsObject() throws Exception {

        if (thirdPartyPropertyMapContext == null)
            thirdPartyPropertyMapContext = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY, this.getClass().getClassLoader());

        Unmarshaller unmarshaller = thirdPartyPropertyMapContext.createUnmarshaller();

        Items unmarshalledItems = (Items) unmarshaller.unmarshal(new StringReader(JAXBXMLSchemaConstants.EXPECTED_ITEM_MARSHALLED_RESULT));

        assertTrue("Expected unmarshalled version of the Items type to match the created version, but they did not",
                   JAXBContextUtils.compareItems(unmarshalledItems, JAXBContextUtils.getItems()));

    }

    @Test
    public void testEE10PropertyMapContextUnmarshallShippingAddressObject() throws Exception {

        if (thirdPartyPropertyMapContext == null)
            thirdPartyPropertyMapContext = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY, this.getClass().getClassLoader());

        Unmarshaller unmarshaller = thirdPartyPropertyMapContext.createUnmarshaller();

        JAXBElement<ShippingAddress> shippingAddressElement = (JAXBElement<ShippingAddress>) unmarshaller
                        .unmarshal(new StringReader(JAXBXMLSchemaConstants.EXPECTED_SHIPPINGADDRESS_MARSHALLED_RESULT));
        ShippingAddress unmarshalledShippingAddress = shippingAddressElement.getValue();

        assertTrue("Expected unmarshalled version of the ShippingAddress type to match the created version, but they did not",
                   JAXBContextUtils.compareShippingAddress(unmarshalledShippingAddress, JAXBContextUtils.getShippingAddress()));

    }

    @Test
    public void testEE10SystemPropertyContextUnmarshallShippingAddressObject() throws Exception {

        if (thirdPartySystemPropertyContext == null)
            thirdPartySystemPropertyContext = JAXBContextUtils.setupJAXBContextWithSystemProperty(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                                  jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());

        Unmarshaller unmarshaller = thirdPartySystemPropertyContext.createUnmarshaller();

        JAXBElement<ShippingAddress> shippingAddressElement = (JAXBElement<ShippingAddress>) unmarshaller
                        .unmarshal(new StringReader(JAXBXMLSchemaConstants.EXPECTED_SHIPPINGADDRESS_MARSHALLED_RESULT));
        ShippingAddress unmarshalledShippingAddress = shippingAddressElement.getValue();

        assertTrue("Expected unmarshalled version of the Items type to match the created version, but they did not",
                   JAXBContextUtils.compareShippingAddress(unmarshalledShippingAddress, JAXBContextUtils.getShippingAddress()));

    }

    @Test
    public void testEE10PropertyMapContextUnmarshallPurchaseOrderTypeObject() throws Exception {

        if (thirdPartyPropertyMapContext == null)
            thirdPartyPropertyMapContext = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY, this.getClass().getClassLoader());

        Unmarshaller unmarshaller = thirdPartyPropertyMapContext.createUnmarshaller();

        JAXBContextUtils.comparePurchaseOrderType(unmarshaller);

    }

    @Test
    public void testEE10SystemPropertyContextUnmarshallPurchaseOrderTypeObject() throws Exception {

        if (thirdPartySystemPropertyContext == null)
            thirdPartySystemPropertyContext = JAXBContextUtils.setupJAXBContextWithSystemProperty(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                                  jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());

        Unmarshaller unmarshaller = thirdPartySystemPropertyContext.createUnmarshaller();

        JAXBContextUtils.comparePurchaseOrderType(unmarshaller);
    }

    /*
    *
    */
    @Test
    public void testEE10PropertyMapContextSchemaOutputResolver() throws Exception {
        SchemaOutputResolver ssor = new StringSchemaOutputResolver();

        if (thirdPartyPropertyMapContext == null)
            thirdPartyPropertyMapContext = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY, this.getClass().getClassLoader());

        thirdPartyPropertyMapContext.generateSchema(ssor);
        String schemaString = ((StringSchemaOutputResolver) ssor).getSchema();
        String notFoundString = JAXBContextUtils.searchArrayInString(schemaString, JAXBXMLSchemaConstants.EXPECTED_SCHEMA_CONTENTS);

        assertNull(notFoundString + " is expected to be in generated schema: " + schemaString, notFoundString);
    }

    /*
    *
    */
    @Test
    public void testEE10SystemPropertyContextSchemaOutputResolver() throws Exception {
        SchemaOutputResolver ssor = new StringSchemaOutputResolver();

        if (thirdPartySystemPropertyContext == null)
            thirdPartySystemPropertyContext = JAXBContextUtils.setupJAXBContextWithSystemProperty(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                                  jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());

        thirdPartySystemPropertyContext.generateSchema(ssor);
        String schemaString = ((StringSchemaOutputResolver) ssor).getSchema();
        String notFoundString = JAXBContextUtils.searchArrayInString(schemaString, JAXBXMLSchemaConstants.EXPECTED_SCHEMA_CONTENTS);

        assertNull(notFoundString + " is expected to be in generated schema: " + schemaString, notFoundString);
    }

}
