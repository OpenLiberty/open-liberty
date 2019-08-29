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
package mpGraphQL10.ignore;

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
@WebServlet(urlPatterns = "/IgnoreTestServlet")
public class IgnoreTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(IgnoreTestServlet.class.getName());

    @Inject
    private MyGraphQLEndpoint endpoint;
    
    private RestClientBuilder builder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/ignoreApp/" + contextPath;
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
    public void testSchemaDoesNotContainIgnoredFields(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        GraphQLClient client = builder.build(GraphQLClient.class);
        String schema = client.schema();
        System.out.println("Schema: " + System.lineSeparator() + schema);
        assertNotNull(schema);
        
        // check output types
        int index = schema.indexOf("type Widget ");
        assertTrue(index > 0);
        String snippet = schema.substring(index, schema.indexOf("}", index + 1));
        
        // check output types for @Ignore fields
        assertTrue(snippet.contains("length")); // sanity check expected field is there
        assertFalse(snippet.contains("dimensions"));
        assertFalse(snippet.contains("weight"));
        assertTrue(snippet.contains("quantity")); // only ignored on input types
        
        // check output types for @JsonbTransient fields
        assertTrue(snippet.contains("length2")); // sanity check expected field is there
        assertFalse(snippet.contains("dimensions2"));
        assertFalse(snippet.contains("weight2"));
        assertTrue(snippet.contains("quantity2")); // only ignored on input types

        // check input types
        index = schema.indexOf("input WidgetInput ");
        snippet = schema.substring(index, schema.indexOf("}", index + 1));
        // check input types for @Ignore fields
        assertTrue(snippet.contains("length")); // sanity check expected field is there
        assertFalse(snippet.contains("dimensions"));
        assertFalse(snippet.contains("quantity"));
        assertTrue(snippet.contains("weight")); // only ignored on output types

        // check input types for @JsonbTransient fields
        assertTrue(snippet.contains("length2")); // sanity check expected field is there
        assertFalse(snippet.contains("dimensions2"));
        assertFalse(snippet.contains("quantity2"));
        assertTrue(snippet.contains("weight2")); // only ignored on output types
        

    }

}
