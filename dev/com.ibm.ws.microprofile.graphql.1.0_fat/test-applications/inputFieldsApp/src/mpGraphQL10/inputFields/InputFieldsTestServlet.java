/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpGraphQL10.inputFields;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Before;
import org.junit.Test;

import componenttest.app.FATServlet;


@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/InputFieldsTestServlet")
public class InputFieldsTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(InputFieldsTestServlet.class.getName());

    @Inject
    private MyGraphQLEndpoint endpoint;
    
    private RestClientBuilder builder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/inputFieldsApp/" + contextPath;
        LOG.info("baseUrl = " + baseUriStr);
        URI baseUri = URI.create(baseUriStr);
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .register(LoggingFilter.class)
                        .baseUri(baseUri);
    }

    @Before
    public void resetEndpoint() {
        
    }

    @Test
    public void testSchemaContainsAnnotatedInputFieldsAndDescriptionsFromField(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        
        String snippet = getSchemaInputTypeSnippet();

        // check input fields from @InputField on Java fields
        assertTrue("Schema does not contain field name specified via @InputField on Java field", 
                   snippet.contains("qty:"));
        assertTrue("Schema does not contain field description specified via @InputField on Java field", 
                   snippet.contains("Number of units to ship"));
        assertFalse("Schema contains Java field name that should have been overwritten by @InputField annotation",
                    snippet.contains("quantity:"));

        // check input fields from @InputField on Java setters -- currently not allowed to put @InputField on methods
//        assertTrue("Schema does not contain field name specified via @InputField on Java setter", 
//                   snippet.contains("shippingWeight "));
//        assertTrue("Schema does not contain field description specified via @InputField on Java setter", 
//                   snippet.contains("Total tonnage to be shipped"));
//        assertFalse("Schema contains Java property name that should have been overwritten by @InputField annotation",
//                    snippet.contains("weight "));
    }

    @Test
    public void testSchemaContainsAnnotatedInputFieldsAndDescriptionsFromJsonbPropertyOnField(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String snippet = getSchemaInputTypeSnippet();
        // check input fields from @JsonbProperty on Java fields
        assertTrue("Schema does not contain field name specified via @JsonbProperty on Java field", 
                   snippet.contains("qty2:"));
        assertFalse("Schema contains Java field name that should have been overwritten by @InputField annotation",
                    snippet.contains("quantity2"));
    }

    @Test
    public void testSchemaContainsAnnotatedInputFieldsAndDescriptionsFromJsonbPropertyOnSetter(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String snippet = getSchemaInputTypeSnippet();
        // check input fields from @JsonbProperty on Java setters
        assertTrue("Schema does not contain field name specified via @JsonbProperty on Java setter", 
                   snippet.contains("shippingWeight2:"));
        assertFalse("Schema contains Java property name that should have been overwritten by @InputField annotation",
                    snippet.contains("weight2:"));
    }

    private String getSchemaInputTypeSnippet() {
        GraphQLClient client = builder.build(GraphQLClient.class);
        String schema = client.schema();
        assertNotNull("Null schema", schema);
        
        // check input types
        int index = schema.indexOf("input WidgetInput ");
        assertTrue(index > -1);
        String snippet = schema.substring(index, schema.indexOf("}", index + 1));
        return snippet;
    }
}
