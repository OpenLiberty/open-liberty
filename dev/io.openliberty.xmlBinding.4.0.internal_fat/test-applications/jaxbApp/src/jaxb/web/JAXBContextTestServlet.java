/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxb.web;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.SchemaOutputResolver;
import jakarta.xml.bind.Unmarshaller;
import jaxb.web.dataobjects.ShippingAddress;
import jaxb.web.utils.JAXBContextUtils;
import jaxb.web.utils.JAXBXMLSchemaConstants;
import jaxb.web.utils.StringSchemaOutputResolver;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet("/JAXBContextTestServlet")
public class JAXBContextTestServlet extends FATServlet {
    // We cache the context in order to keep the total run time of the tests down
    private JAXBContext riContext = null;

    /*
    *
    */
    @Test
    public void testJakartaEE10JaxbContextSchemaOutputResolver() throws Exception {
        SchemaOutputResolver ssor = new StringSchemaOutputResolver();

        if (riContext == null)
            riContext = JAXBContextUtils.setupJAXBContext();

        riContext.generateSchema(ssor);
        String schemaString = ((StringSchemaOutputResolver) ssor).getSchema();

        Path file = Paths.get("./schema.xml");
        file.toAbsolutePath();
        List<String> content = Files.readAllLines(Paths.get("schemaEE10.xml"));

        if (content != null) {
            for (String s : content) {
                assertTrue("Expected generated schema " + schemaString + " to contain: " + s, schemaString.contains(s));
            }
        }
    }

    @Test
    @SkipForRepeat({})
    public void testJaxbContextUnmarshallingPurchaseOrderTypeObject() throws Exception {

        if (riContext == null)
            riContext = JAXBContextUtils.setupJAXBContext();

        Unmarshaller unmarshaller = riContext.createUnmarshaller();

        JAXBContextUtils.comparePurchaseOrderType(unmarshaller);
    }

    @Test
    @SkipForRepeat({})
    public void testJaxbContextUnmarshallingItemsObject() throws Exception {

        if (riContext == null)
            riContext = JAXBContextUtils.setupJAXBContext();

        Unmarshaller unmarshaller = riContext.createUnmarshaller();

        JAXBContextUtils.comparePurchaseOrderType(unmarshaller);
    }

    @Test
    @SkipForRepeat({})
    public void testJaxbContextUnmarshallingShippingAddressObject() throws Exception {

        if (riContext == null)
            riContext = JAXBContextUtils.setupJAXBContext();

        Unmarshaller unmarshaller = riContext.createUnmarshaller();

        JAXBElement<ShippingAddress> shippingAddressElement = (JAXBElement<ShippingAddress>) unmarshaller
                        .unmarshal(new StringReader(JAXBXMLSchemaConstants.EXPECTED_SHIPPINGADDRESS_MARSHALLED_RESULT));

        ShippingAddress unmarshalledShippingAddress = shippingAddressElement.getValue();

        assertTrue("Expected unmarshalled version of the Items type to match the created version, but they did not",
                   JAXBContextUtils.compareShippingAddress(unmarshalledShippingAddress, JAXBContextUtils.getShippingAddress()));
    }

    /*
     * Expect a ClassNotFoundExecption for the Glassfish RI because by using the properties map, you need to pass the classloader from the Application
     * The glassfish RI isn't availible to that classloader
     */
    @Test
    public void testJakartaEE10JaxbContextThirdPartyImplInPropertyMap() throws Exception {
        SchemaOutputResolver ssor = new StringSchemaOutputResolver();
        String ee10Exception = "Implementation of Jakarta XML Binding-API has not been found on module path or classpath";

        try {
            JAXBContext context = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                   jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());
        } catch (Exception e) {
            assertTrue("Expected Exception to contain : " + ee10Exception
                       + " but contained " + e.getMessage(), e.getMessage().contains(ee10Exception));
        }
    }

    /*
     * Expect a ClassNotFoundExecption for the Glassfish RI because by using the system property, you need to pass the classloader from the Application
     * That isn't availible to that classloader
     */
    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "JAXRS", "JAXB-2.3", SkipForRepeat.EE9_FEATURES })
    public void testJakartaEE10JaxbContextThirdPartyImplInSystemProperty() throws Exception {
        SchemaOutputResolver ssor = new StringSchemaOutputResolver();
        String ee10Exception = "Implementation of Jakarta XML Binding-API has not been found on module path or classpath";

        try {
            JAXBContext context = JAXBContextUtils.setupJAXBContextWithSystemProperty(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY,
                                                                                      jaxb.web.dataobjects.ObjectFactory.class.getClassLoader());
        } catch (Exception e) {
            assertTrue("Expected Exception to contain : " + ee10Exception
                       + " but contained " + e.getMessage(), e.getMessage().contains(ee10Exception));
        }
    }
}
