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
package mpGraphQL10.basicQuery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BasicQueryTestServlet")
public class BasicQueryTestServlet extends FATServlet {
    Logger LOG = Logger.getLogger(BasicQueryTestServlet.class.getName());

    private RestClientBuilder builder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + "/basicQueryApp/" + contextPath;
        LOG.info("baseUrl = " + baseUriStr);
        URI baseUri = URI.create(baseUriStr);
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .register(LoggingFilter.class)
                        .baseUri(baseUri);
    }

    @Test
    public void testSchemaIsGenerated(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        String schema = client.schema();
        System.out.println("Schema: " + System.lineSeparator() + schema);
        assertNotNull(schema);
        assertTrue(schema.contains("type Query"));
        assertTrue(schema.contains("allQueryInstancesAppScope"));
        assertTrue(schema.contains("allQueryInstancesRequestScope"));
        assertTrue(schema.contains("allWidgets"));
        assertTrue(schema.contains("type Widget"));
    }

    @Test
    public void testSimpleQuery(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("allWidgets");
        query.setQuery("query allWidgets {" + System.lineSeparator() +
                       "  allWidgets {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response = client.allWidgets(query);
        System.out.println("Response: " + response);
        List<Widget> widgets = response.getData().getAllWidgets();
        assertEquals(2, widgets.size());
        for (Widget widget : widgets) {
            String name = widget.getName();
            assertNotNull(name);
            if ("Notebook".equals(name)) {
                assertEquals(20, widget.getQuantity());
            } else if ("Pencil".equals(name)) {
                assertEquals(200, widget.getQuantity());
            } else {
                fail("Unexpected widget: " + widget);
            }
            assertEquals(-1.0, widget.getWeight(), 0.1); // weight wasn't specified in the query
        }
    }

    @Test
    public void testSimpleQueryWithSet(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("allWidgetsSet");
        query.setQuery("query allWidgetsSet {" + System.lineSeparator() +
                       "  allWidgetsSet {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    weight" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response = client.allWidgets(query);
        System.out.println("Response: " + response);
        Set<Widget> widgets = response.getData().getAllWidgetsSet();
        assertEquals(2, widgets.size());
        for (Widget widget : widgets) {
            String name = widget.getName();
            assertNotNull(name);
            if ("Notebook".equals(name)) {
                assertEquals(2.0, widget.getWeight(), 0.1);
            } else if ("Pencil".equals(name)) {
                assertEquals(0.5, widget.getWeight(), 0.1);
            } else {
                fail("Unexpected widget: " + widget);
            }
            assertEquals(-1, widget.getQuantity()); // quantity wasn't specified in the query
        }
    }

    @Test
    public void testSimpleQueryWhenUnableToSerializeEntity(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("allWidgetsUnableToSerialize");
        query.setQuery("query allWidgetsUnableToSerialize {" + System.lineSeparator() +
                       "  allWidgetsUnableToSerialize {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    quantity" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");


        WidgetQueryResponse response = client.allWidgets(query);
        System.out.println("Response: " + response);
        // check partial (null) results:
        List<Widget> widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        assertNull(widgets.get(0));

        //check error message
        List<Error> errors = response.getErrors();
        assertEquals(1, errors.size());
        Error e = errors.get(0);
        assertTrue(e.getMessage().contains("Server Error"));
    }

    @Test
    public void testRequestScopedQuery(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("allQueryInstancesRequestScope");
        query.setQuery("query allQueryInstancesRequestScope {" + System.lineSeparator() +
                       "  allQueryInstancesRequestScope {" + System.lineSeparator() +
                       "    instanceId" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        QueryInfoQueryResponse response = client.getQueryInfo(query);
        System.out.println("Response1: " + response);
        List<QueryInfo> queryInfos1 = response.getData().getAllQueryInstances();

        response = client.getQueryInfo(query);
        System.out.println("Response2: " + response);
        List<QueryInfo> queryInfos2 = response.getData().getAllQueryInstances();

        assertNotNull(queryInfos1);
        assertEquals(1, queryInfos1.size());
        assertNotNull(queryInfos2);
        assertEquals(2, queryInfos2.size());
        assertTrue(queryInfos2.contains(queryInfos1.get(0)));
    }

    @Test
    public void testApplicationScopedQuery(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("allQueryInstancesAppScope");
        query.setQuery("query allQueryInstancesAppScope {" + System.lineSeparator() +
                       "  allQueryInstancesAppScope {" + System.lineSeparator() +
                       "    instanceId" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        QueryInfoQueryResponse response = client.getQueryInfo(query);
        System.out.println("Response1: " + response);
        List<QueryInfo> queryInfos1 = response.getData().getAllQueryInstances();

        response = client.getQueryInfo(query);
        System.out.println("Response2: " + response);
        List<QueryInfo> queryInfos2 = response.getData().getAllQueryInstances();

        assertNotNull(queryInfos1);
        assertEquals(1, queryInfos1.size());
        assertNotNull(queryInfos2);
        assertEquals(1, queryInfos2.size());
        assertTrue(queryInfos2.contains(queryInfos1.get(0)));
    }
}
