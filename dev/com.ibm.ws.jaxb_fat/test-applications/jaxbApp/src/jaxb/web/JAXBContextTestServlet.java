/**
 *
 */
package jaxb.web;

import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
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

    // We cache the context in order to keep the total runtime of the tests down
    private JAXBContext riContext = null;

    /*
     *
     */
    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "JAXB-2.3" })
    public void testJakartaEE9JaxbContextSchemaOutputResolver() throws Exception {
        SchemaOutputResolver ssor = new StringSchemaOutputResolver();

        if (riContext == null)
            riContext = JAXBContextUtils.setupJAXBContext();

        riContext.generateSchema(ssor);
        String schemaString = ((StringSchemaOutputResolver) ssor).getSchema();

        List<String> content = Files.readAllLines(Paths.get("schemaEE9.xml"));

        if (content != null) {
            for (String s : content) {
                assertTrue("Expected generated schema " + schemaString + " to contain: " + s, schemaString.contains(s));
            }
        }
    }

    /*
    *
    */
    //@Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "JAXRS", "JAXB-2.3", SkipForRepeat.EE9_FEATURES })
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
    *
    */
    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "JAXRS", "JAXB-2.3", SkipForRepeat.EE10_FEATURES })
    public void testJakartaEE9JaxbContextThirdPartyImplInPropertyMap() throws Exception {
        SchemaOutputResolver ssor = new StringSchemaOutputResolver();
        String ee9Exception = "property \"jakarta.xml.bind.JAXBContextFactory\" is not supported";

        try {
            JAXBContext context = JAXBContextUtils.setupJAXBContextWithPropertyMap(JAXBContextUtils.MOXY_JAXB_CONTEXT_FACTORY, this.getClass().getClassLoader());
        } catch (Exception e) {
            assertTrue("Expected Exception to contain : " + ee9Exception
                       + " but contained " + e.getMessage(), e.getMessage().contains(ee9Exception));
        }
    }

    /*
     * Expect a ClassNotFoundExecption for the Glassfish RI because by using the properties map, you need to pass the classloader from the Application
     * That isn't availible to that classloader
     */
    @Test
    @SkipForRepeat({ SkipForRepeat.NO_MODIFICATION, "JAXRS", "JAXB-2.3", SkipForRepeat.EE9_FEATURES })
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
     * Expect a ClassNotFoundExecption for the Glassfish RI because by using the properties map, you need to pass the classloader from the Application
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
