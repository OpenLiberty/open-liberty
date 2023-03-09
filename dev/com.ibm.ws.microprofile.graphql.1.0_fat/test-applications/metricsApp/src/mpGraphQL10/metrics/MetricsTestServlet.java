/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package mpGraphQL10.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;
import mpGraphQL10.metrics.Stats.SimpleTimerStat;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MetricsTestServlet")
public class MetricsTestServlet extends FATServlet {
    private static final Logger LOG = Logger.getLogger(MetricsTestServlet.class.getName());
    private static final long PADDING_TIME = 5000; //milliseconds

    private static final String METRICS_URL_STRING_PREFIX = "/metrics/vendor/";
    private static final String METRICS5_URL_STRING_PREFIX = "/metrics?scope=vendor&name=";

    private static boolean ee10;
    static {
       try {
           Class.forName("jakarta.ws.rs.core.EntityPart");
           ee10 = true;
       } catch(Throwable t) {
           ee10 = false;
       }
    }

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
        
        // MP Metrics 5.0 removed the JSON format for metrics
        if (!ee10) {
            Stats stats = statsBuilder.register(LoggingFilter.class).build(StatsClient.class).getVendorStats();
            assertNotNull(stats);
            SimpleTimerStat stat = stats.getCount();
            assertEquals(3, stat.getCount());
        }

        // Check also with the normal text metrics format as well so can test EE 10 where JSON format isn't enabled any longer.
        List<String> metrics = getMetricsStrings(200, (ee10 ? METRICS5_URL_STRING_PREFIX : METRICS_URL_STRING_PREFIX) 
                                                 + "mp_graphql_QUERY_getCountWidget");
        int count = -1;

        for (String metric : metrics) {
            if (metric.startsWith("#")) {
                continue;
            }
            if (ee10 ? metric.contains("mp_graphql_QUERY_getCountWidget_seconds_count") : 
                metric.contains("mp_graphql_QUERY_getCountWidget_total")) {
                count = Float.valueOf(metric.substring(metric.indexOf(' ') +1)).intValue();
                break;
            }
        }

        assertTrue("Count statistic not found", count != -1);

        assertEquals(3, count);
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
        // MP Metrics 5.0 removed the JSON format for metrics
        if (!ee10) {
            Stats stats = statsBuilder.build(StatsClient.class).getVendorStats();
            assertNotNull(stats);
            SimpleTimerStat stat = stats.getTime();

            assertEquals(3, stat.getCount());
            Duration metricsDuration = Duration.ofNanos((long)stat.getElapsedTime());
            Duration maxTimeDuration = Duration.ofMillis(1500 + PADDING_TIME);
            assertTrue( maxTimeDuration.compareTo(metricsDuration) >= 0 );
            assertTrue( Math.abs(metricsDuration.minus(runningDuration).toMillis()) <= PADDING_TIME);
        }

        // Check also with the normal text metrics format as well so can test EE 10 where JSON format isn't enabled any longer.
        List<String> metrics = getMetricsStrings(200, (ee10 ? METRICS5_URL_STRING_PREFIX : METRICS_URL_STRING_PREFIX) 
                                                 + "mp_graphql_QUERY_getTimeWidget");
        int count = -1;
        float seconds = -1;

        for (String metric : metrics) {
            if (metric.startsWith("#")) {
                continue;
            }
            if (ee10 ? metric.contains("mp_graphql_QUERY_getTimeWidget_seconds_count") : 
                metric.contains("mp_graphql_QUERY_getTimeWidget_total")) {
                count = Float.valueOf(metric.substring(metric.indexOf(' ') +1)).intValue();
            } else if (ee10 ? metric.contains("mp_graphql_QUERY_getTimeWidget_seconds_sum") : 
                metric.contains("mp_graphql_QUERY_getTimeWidget_elapsedTime_seconds")) {
                seconds = Float.parseFloat(metric.substring(metric.indexOf(' ') +1));
            }
        }

        assertTrue("Count statistic not found", count != -1);
        assertTrue("Elapsed Time statistic not found", seconds != -1);

        assertEquals(3, count);
        Duration metricsDuration = Duration.ofNanos((long)(seconds * 1000000));
        Duration maxTimeDuration = Duration.ofMillis(1500 + PADDING_TIME);
        assertTrue( maxTimeDuration.compareTo(metricsDuration) >= 0 );
        assertTrue( Math.abs(metricsDuration.minus(runningDuration).toMillis()) <= PADDING_TIME);
    }

    private ArrayList<String> getMetricsStrings(int expectedRc, String metricString) {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://localhost:" + getSysProp("bvt.prop.HTTP_default", "8010") + metricString);
            int retcode;
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            // Since these tests an run asynchronously it is possible that a testcase that does not expect metrics to be
            // collected (for example testAbort) may run first or be specified as the only test to run.  In
            // this case a 404 is expected.
            if (retcode != expectedRc){

                fail("Bad return Code from Metrics method call. Expected " + expectedRc + ": Received "
                     + retcode + ", URL = " + url.toString());

                return null;
            }

            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);

                BufferedReader br = new BufferedReader(isr);

                ArrayList<String> lines = new ArrayList<String>();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.add(line);
                }
                return lines;
            }
            return null;

        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

    }
}
