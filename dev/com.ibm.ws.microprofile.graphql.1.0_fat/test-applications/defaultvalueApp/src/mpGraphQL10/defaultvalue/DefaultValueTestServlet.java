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
package mpGraphQL10.defaultvalue;

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
@WebServlet(urlPatterns = "/DefaultValueTestServlet")
public class DefaultValueTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(DefaultValueTestServlet.class.getName());

    @Inject
    private MyGraphQLEndpoint endpoint;
    
    private RestClientBuilder builder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/defaultvalueApp/" + contextPath;
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
    public void testSchemaContainsDefaultValueInfo(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        GraphQLClient client = builder.build(GraphQLClient.class);
        String schema = client.schema();
        System.out.println("Schema: " + System.lineSeparator() + schema);
        assertNotNull(schema);
        assertTrue(schema.contains("\"Widget(Oven,12,120.1,36.2,3.3,14.0)\"")); // on MyGraphQLEndpoint
        assertTrue(schema.contains("\"Crockpot\"")); // on WidgetInput
        assertTrue(schema.contains("5"));    // on WidgetInput
        assertTrue(schema.contains("10.0")); // on WidgetInput
        assertTrue(schema.contains("30.1")); // on WidgetInput
        assertTrue(schema.contains("20.4")); // on WidgetInput
        assertFalse(schema.contains("SHOULD BE IGNORED")); // on Widget
    }

    @Test
    public void testQueryWithDefaultValueArgument(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        endpoint.reset();
        GraphQLClient client = builder.build(GraphQLClient.class);
        GraphQLOperation graphQLOperation = new GraphQLOperation();
        
        // Test that passed-in value is used, not default value
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("widgetByName");
        graphQLOperation.setQuery("query widgetByName {" + System.lineSeparator() +
                       "  widgetByName(name:\"Eraser\") {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response = client.allWidgets(graphQLOperation);
        System.out.println("Query Response: " + response);
        Widget widget = response.getData().getWidgetByName();
        assertNotNull(widget);
        assertEquals("Eraser", widget.getName());
        assertEquals(5, widget.getQuantity());

        // Test that if no value is passed-in, default value is used
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("widgetByName");
        graphQLOperation.setQuery("query widgetByName {" + System.lineSeparator() +
                       "  widgetByName {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        response = client.allWidgets(graphQLOperation);
        System.out.println("Query Response: " + response);
        widget = response.getData().getWidgetByName();
        assertNotNull(widget);
        assertEquals("Pencil", widget.getName());
        assertEquals(10, widget.getQuantity());
    }

    @Test
    public void testMutationWithDefaultValueArgument(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        endpoint.reset();
        GraphQLClient client = builder.build(GraphQLClient.class);
        GraphQLOperation graphQLOperation = new GraphQLOperation();
        
        // Test that passed-in value is used, not default value
        graphQLOperation.setOperationName("createWidgetByString");
        graphQLOperation.setQuery("mutation createWidgetByString ($widgetString: String) {" + System.lineSeparator() +
                                  "  createWidgetByString(widgetString: $widgetString) {" + System.lineSeparator() +
                                  "    name," + System.lineSeparator() +
                                  "    weight," + System.lineSeparator() +
                                  "  }" + System.lineSeparator() +
                                  "}");
        graphQLOperation.setVariables(VariablesAsString.newVars("Widget(Chess boards,25,12.6,14.0,3.3,14.0)"));
        WidgetQueryResponse response = client.allWidgets(graphQLOperation);
        System.out.println("Mutation Response: " + response);
        Widget widget = response.getData().getCreateWidgetByString();
        assertNotNull(widget);
        assertEquals("Chess boards", widget.getName());
        assertEquals(-1, widget.getQuantity()); // Quantity was not specified in mutation request
        assertEquals(12.6, widget.getWeight(), 0.1);
        
        endpoint.reset();
        
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("createWidgetByString");
        graphQLOperation.setQuery("mutation createWidgetByString {" + System.lineSeparator() +
                                  "  createWidgetByString{" + System.lineSeparator() +
                                  "    name," + System.lineSeparator() +
                                  "    weight," + System.lineSeparator() +
                                  "  }" + System.lineSeparator() +
                                  "}");
        response = client.allWidgets(graphQLOperation);
        System.out.println("Mutation Response: " + response);
        widget = response.getData().getCreateWidgetByString();
        assertNotNull(widget);
        assertEquals("Oven", widget.getName());
        assertEquals(-1, widget.getQuantity()); // Quantity was not specified in mutation request
        assertEquals(120.1, widget.getWeight(), 0.1);

    }
    
    @Test
    public void testDefaultValuesOnInputType(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        GraphQLClient client = builder.build(GraphQLClient.class);
        GraphQLOperation graphQLOperation = new GraphQLOperation();
        
        // Test that passed-in values are used, not defaults
        graphQLOperation.setOperationName("createWidget");
        graphQLOperation.setQuery("mutation createWidget ($widget: WidgetInput) {" + System.lineSeparator() +
                                  "  createWidget(widget: $widget) {" + System.lineSeparator() +
                                  "    name," + System.lineSeparator() +
                                  "    quantity," + System.lineSeparator() +
                                  "    weight," + System.lineSeparator() +
                                  "    length," + System.lineSeparator() +
                                  "    height," + System.lineSeparator() +
                                  "    depth" + System.lineSeparator() +
                                  "  }" + System.lineSeparator() +
                                  "}");
        graphQLOperation.setVariables(VariablesIndividualProps.newVars("Earbuds", 20, 1.2, 1.0, 0.8, 0.6));
        WidgetQueryResponse response = client.allWidgets(graphQLOperation);
        System.out.println("Mutation Response: " + response);
        Widget widget = response.getData().getCreateWidget();
        assertNotNull(widget);
        assertEquals("Earbuds", widget.getName());
        assertEquals(20, widget.getQuantity());
        assertEquals(1.2, widget.getWeight(), 0.1);
        assertEquals(1.0, widget.getLength(), 0.1);
        assertEquals(0.8, widget.getHeight(), 0.1);
        assertEquals(0.6, widget.getDepth(), 0.1);
        
        // Test that default values are used
        graphQLOperation = new GraphQLOperation();
        graphQLOperation.setOperationName("createWidget");
        graphQLOperation.setQuery("mutation createWidget ($widget: WidgetInput) {" + System.lineSeparator() +
                                  "  createWidget(widget: $widget) {" + System.lineSeparator() +
                                  "    name," + System.lineSeparator() +
                                  "    quantity," + System.lineSeparator() +
                                  "    weight," + System.lineSeparator() +
                                  "    length," + System.lineSeparator() +
                                  "    height," + System.lineSeparator() +
                                  "    depth" + System.lineSeparator() +
                                  "  }" + System.lineSeparator() +
                                  "}");
        graphQLOperation.setVariables(VariablesWeightOnly.newVars(2.4));
        response = client.allWidgets(graphQLOperation);
        System.out.println("Mutation Response: " + response);
        widget = response.getData().getCreateWidget();
        assertNotNull(widget);
        assertEquals("Crockpot", widget.getName());
        assertEquals(5, widget.getQuantity());
        assertEquals(2.4, widget.getWeight(), 0.1);
        assertEquals(20.4, widget.getLength(), 0.1);
        assertEquals(30.1, widget.getHeight(), 0.1);
        assertEquals(10.0, widget.getDepth(), 0.1);

    }
}
