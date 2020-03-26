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
package mpGraphQL10.metrics;

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
import java.time.Duration;
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
@WebServlet(urlPatterns = "/MetricsTestServlet")
public class MetricsTestServlet extends FATServlet {
    private static final Logger LOG = Logger.getLogger(MetricsTestServlet.class.getName());
    private static final long PADDING_TIME = 5000; //milliseconds

    private RestClientBuilder builder;
    private RestClientBuilder statsBuilder;

    private static String getSysProp(String key, String defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(key, defaultValue));
    }

    @Override
    public void init() throws ServletException {
        String contextPath = getSysProp("com.ibm.ws.microprofile.graphql.fat.contextpath", "graphql");
        String baseUriStr = "http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010");
        String graphQLURI = baseUriStr + "/metricsApp/" + contextPath;
        LOG.info("graphQLURI = " + graphQLURI);
        URI baseUri = URI.create(graphQLURI);
        builder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUri(baseUri);
        
        URI metricsUri = URI.create(baseUriStr);
        statsBuilder = RestClientBuilder.newBuilder()
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .baseUri(metricsUri);
    }

    @Test
    public void testCount(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        QueryClient client = builder.register(LoggingFilter.class).build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("getCountWidget");
        query.setQuery("query getCountWidget {" + System.lineSeparator() +
                       "  getCountWidget {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    count" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response = client.count(query);
        LOG.info("Response: " + response);
        List<Widget> widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        Widget widget = widgets.get(0);
        assertEquals("Count", widget.getName());
        assertEquals(1, widget.getCount());

        response = client.count(query);
        LOG.info("Response: " + response);
        widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        widget = widgets.get(0);
        assertEquals("Count", widget.getName());
        assertEquals(2, widget.getCount());
        
        response = client.count(query);
        LOG.info("Response: " + response);
        widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        widget = widgets.get(0);
        assertEquals("Count", widget.getName());
        assertEquals(3, widget.getCount());
        
        Stats stats = statsBuilder.build(StatsClient.class).getVendorStats();
        assertNotNull(stats);
        assertEquals(3, stats.getCount().getCount());
    }

    @Test
    public void testTime(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Duration HALF_SECOND = Duration.ofMillis(500);
        QueryClient client = builder.register(LoggingFilter.class).build(QueryClient.class);
        Query query = new Query();
        query.setOperationName("getTimeWidget");
        query.setQuery("query getTimeWidget {" + System.lineSeparator() +
                       "  getTimeWidget {" + System.lineSeparator() +
                       "    name," + System.lineSeparator() +
                       "    time" + System.lineSeparator() +
                       "  }" + System.lineSeparator() +
                       "}");

        WidgetQueryResponse response = client.time(query);
        LOG.info("Response: " + response);
        List<Widget> widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        Widget widget = widgets.get(0);
        assertEquals("Time", widget.getName());
        Duration duration = Duration.ofNanos(widget.getTime());
        Duration runningDuration = Duration.ofNanos(widget.getTime());
        assertTrue(duration.compareTo(HALF_SECOND) >=0 );

        response = client.time(query);
        LOG.info("Response: " + response);
        widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        widget = widgets.get(0);
        assertEquals("Time", widget.getName());
        duration = Duration.ofNanos(widget.getTime());
        runningDuration = runningDuration.plus(duration);
        assertTrue(duration.compareTo(HALF_SECOND.multipliedBy(2)) >=0 );

        response = client.time(query);
        LOG.info("Response: " + response);
        widgets = response.getData().getAllWidgets();
        assertEquals(1, widgets.size());
        widget = widgets.get(0);
        assertEquals("Time", widget.getName());
        duration = Duration.ofNanos(widget.getTime());
        runningDuration = runningDuration.plus(duration);
        assertTrue( duration.compareTo(HALF_SECOND.multipliedBy(3)) >=0 );
        
        //TODO: check mpMetrics shows that (1.5s + someBuffer) >= time_from_mpMetrics >= time_from_widget 
        Stats stats = statsBuilder.build(StatsClient.class).getVendorStats();
        assertNotNull(stats);
        assertNotNull(stats.getTime());
        assertEquals(3, stats.getTime().getCount());
        Duration metricsDuration = Duration.ofNanos((long)stats.getTime().getElapsedTime());
        Duration maxTimeDuration = Duration.ofMillis(1500 + PADDING_TIME);
        assertTrue( maxTimeDuration.compareTo(metricsDuration) >= 0 );
        assertTrue( Math.abs(metricsDuration.minus(runningDuration).toMillis()) <= PADDING_TIME);
    }
}
