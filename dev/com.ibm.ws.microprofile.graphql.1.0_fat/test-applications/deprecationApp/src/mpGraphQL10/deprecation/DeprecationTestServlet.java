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
package mpGraphQL10.deprecation;

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
@WebServlet(urlPatterns = "/DeprecationTestServlet")
public class DeprecationTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(DeprecationTestServlet.class.getName());

    @Inject
    private MyGraphQLEndpoint endpoint;
    
    private RestClientBuilder builder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/deprecationApp/" + contextPath;
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
    public void testSchemaContainsDeprecationEntityInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        GraphQLClient client = builder.build(GraphQLClient.class);
        String schema = client.schema();
        System.out.println("Schema: " + System.lineSeparator() + schema);
        assertNotNull(schema);
        //TODO: re-enable the following checks after deprecation issue is resolved
        //assertTrue(schema.contains("Deprecated mutation, please use \"createWidget\" instead.")); // from endpoint
        //assertTrue(schema.contains("Deprecated, use length, height, and depth instead.")); // from entity
    }

    @Test
    public void testCanStillQueryUsingDeprecatedField(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        endpoint.reset();
        GraphQLClient client = builder.build(GraphQLClient.class);
        GraphQLOperation graphQLOperation = new GraphQLOperation();
        
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("allWidgets");
        graphQLOperation.setQuery("query allWidgets {" + System.lineSeparator() +
                       "  allWidgets {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity," + System.lineSeparator() +
                       "    dimensions" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response = client.allWidgets(graphQLOperation);
        System.out.println("Query Response: " + response);
        List<Widget> widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        Widget widget = widgets.get(0);
        assertNotNull(widget);
        assertEquals("Keyboard", widget.getName());
        assertEquals(300, widget.getQuantity());
        assertEquals(-1.0, widget.getWeight(), 0.1);
        assertEquals("18.0x1.5x9.5", widget.getDimensions());
        
        graphQLOperation.setOperationName("createWidget");
        graphQLOperation.setQuery("mutation createWidget ($widget: WidgetInput) {" + System.lineSeparator() +
                                  "  createWidget(widget: $widget) {" + System.lineSeparator() +
                                  "    name," + System.lineSeparator() +
                                  "    weight," + System.lineSeparator() +
                                  "  }" + System.lineSeparator() +
                                  "}");
        graphQLOperation.setVariables("{" + System.lineSeparator() +
                                      "  \"widget\": {" + System.lineSeparator() +
                                      "    \"name\": \"Chess boards\"," + System.lineSeparator() +
                                      "    \"quantity\": 25," + System.lineSeparator() +
                                      "    \"weight\": 12.6," + System.lineSeparator() +
                                      "    \"length\": 14.0," + System.lineSeparator() +
                                      "    \"height\": 3.3," + System.lineSeparator() +
                                      "    \"depth\": 14.0," + System.lineSeparator() +
                                      "    \"dimensions\": \"14.0x3.3x14.0\"" + System.lineSeparator() +
                                      "  }" + System.lineSeparator() +
                                      "}");
        response = client.allWidgets(graphQLOperation);
        System.out.println("Mutation Response: " + response);
        widget = response.getData().getCreateWidget();
        assertNotNull(widget);
        assertEquals("Chess boards", widget.getName());
        assertEquals(-1, widget.getQuantity()); // Quantity was not specified in mutation request
        assertEquals(12.6, widget.getWeight(), 0.1);
        
        
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("allWidgets");
        graphQLOperation.setQuery("query allWidgets {" + System.lineSeparator() +
                       "  allWidgets {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity," + System.lineSeparator() +
                       "    dimensions" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        response = client.allWidgets(graphQLOperation);
        System.out.println("Query Response: " + response);
        widgets = response.getData().getAllWidgets();
        assertEquals(2, widgets.size());
        for (Widget w : widgets) {
            assertNotNull(w);
            if ("Chess boards".equals(w.getName())) {
                assertEquals(25, w.getQuantity());
                assertEquals(-1.0, w.getWeight(), 0.1); // weight wasn't specified in the query
                assertEquals("14.0x3.3x14.0", w.getDimensions());
            } else if("Keyboard".contentEquals(w.getName())) {
                assertEquals(300, w.getQuantity());
                assertEquals(-1.0, w.getWeight(), 0.1);
                assertEquals("18.0x1.5x9.5", w.getDimensions());
            } else {
                fail("Unexpected widget found " + w);
            }
        }
        
    }

    @Test
    public void testCanInvokeDeprecatedMutation(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        endpoint.reset();
        GraphQLClient client = builder.build(GraphQLClient.class);
        GraphQLOperation graphQLOperation = new GraphQLOperation();
        
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("allWidgets");
        graphQLOperation.setQuery("query allWidgets {" + System.lineSeparator() +
                       "  allWidgets {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity," + System.lineSeparator() +
                       "    depth" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response = client.allWidgets(graphQLOperation);
        System.out.println("Query Response: " + response);
        List<Widget> widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        Widget widget = widgets.get(0);
        assertNotNull(widget);
        assertEquals("Keyboard", widget.getName());
        assertEquals(300, widget.getQuantity());
        assertEquals(-1.0, widget.getWeight(), 0.1);
        assertEquals(9.5, widget.getDepth(), 0.1);
        
        graphQLOperation.setOperationName("createWidgetByHand");
        graphQLOperation.setQuery("mutation createWidgetByHand ($widgetString: String) {" + System.lineSeparator() +
                                  "  createWidgetByHand(widgetString: $widgetString) {" + System.lineSeparator() +
                                  "    name," + System.lineSeparator() +
                                  "    weight," + System.lineSeparator() +
                                  "  }" + System.lineSeparator() +
                                  "}");
        graphQLOperation.setVariables("{" + System.lineSeparator() +
                                      "    \"widgetString\": \"Widget(Chess boards,25,12.6,14.0,3.3,14.0,14.0x3.3x14.0)\"" + System.lineSeparator() +
                                      "}");
        response = client.allWidgets(graphQLOperation);
        System.out.println("Mutation Response: " + response);
        widget = response.getData().getCreateWidget();
        assertNotNull(widget);
        assertEquals("Chess boards", widget.getName());
        assertEquals(-1, widget.getQuantity()); // Quantity was not specified in mutation request
        assertEquals(12.6, widget.getWeight(), 0.1);
        
        
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("allWidgets");
        graphQLOperation.setQuery("query allWidgets {" + System.lineSeparator() +
                       "  allWidgets {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity," + System.lineSeparator() +
                       "    depth" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        response = client.allWidgets(graphQLOperation);
        System.out.println("Query Response: " + response);
        widgets = response.getData().getAllWidgets();
        assertEquals(2, widgets.size());
        
        for (Widget w : widgets) {
            assertNotNull(w);
            String name = w.getName();
            if ("Chess boards".contentEquals(name)) {
                assertEquals(25, w.getQuantity());
                assertEquals(-1.0, w.getWeight(), 0.1); // weight wasn't specified in the query
                assertEquals(14.0, w.getDepth(), 0.1);
            } else if ("Keyboard".contentEquals(name)) {
                assertEquals(300, w.getQuantity());
                assertEquals(-1.0, w.getWeight(), 0.1); // weight wasn't specified in the query
                assertEquals(9.5, w.getDepth(), 0.1);
            }
        }
    }
}
