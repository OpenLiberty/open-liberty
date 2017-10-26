/*******************************************************************************
 * Copyright (c) 2017 IBM Corpo<ration and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.testing.opentracing.test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * <p>Open tracing FAT tests.</p>
 *
 * <p>The test suite:</p>
 *
 * <ul>
 * <li>{@link FATOpentracing#testImmediate}</li>
 * <li>{@link FATOpentracing#testManual}</li>
 * <li>{@link FATOpentracing#testDelayed2}</li>
 * <li>{@link FATOpentracing#testDelayed4}</li>
 * <li>{@link FATOpentracing#testDelayed6}</li>
 * <li>{@link FATOpentracing#testNested0}</li>
 * <li>{@link FATOpentracing#testNested1Sync}</li>
 * <li>{@link FATOpentracing#testNested1Async}</li>
 * <li>{@link FATOpentracing#testNested2Sync}</li>
 * <li>{@link FATOpentracing#testNested2Async}</li>
 * <li>{@link FATOpentracing#testNested4Sync}</li>
 * <li>{@link FATOpentracing#testNested4ASync}</li> *
 * </ul>
 *
 * <p>Each test invokes an API within the FAT test service.  Two
 * verifiable results are produced:</p>
 *
 * <p>The API must answer the expected response value.  This does
 * not exercise the open-tracing feature, but is necessary to
 * demonstrate that the service which is being traced is functioning
 * correctly.</p>
 *
 * <p>The trace state, as obtained as a list of completed spans from
 * the injected tracer, and obtained through a special service API,
 * must contain the expected completed spans.</p>
 *
 * <p>See the several <code>verifySpans</code> methods, for example,
 * {@link FATOpentracing#verifyImmediateSpans}, each of which describes
 * the expected completed spans.</p>
 *
 * <p>The tail of the list of completed spans is examined.  Generally,
 * the list of completed spans must have at least an expected number
 * of spans.  The tail of the completed spans must match the pattern
 * of activity which is performed by the API which was used.</p>
 *
 * <p>Search for "***" within comments for specific tested conditions.</p>
 */
@Mode(TestMode.FULL)
public class FATOpentracing implements FATOpentracingConstants {
    // Logging ...

    private static final Class<? extends FATOpentracing> CLASS = FATOpentracing.class;

    private static void info(String methodName, String text, Object value) {
        System.out.println(CLASS.getSimpleName() + "." + methodName + ": " + text + " [ " + value + " ]");
        FATLogging.info(CLASS, methodName, text, value);
    }

    private static void info(String methodName, String text) {
        System.out.println(CLASS.getSimpleName() + "." + methodName + ": " + text);
        FATLogging.info(CLASS, methodName, text);
    }

    // OpenTrace FAT servers ...

    private static final LibertyServer[] servers =
        new LibertyServer[] { null, null };
    private static final String[] SERVER_NAMES =
        new String [] { OPENTRACING_FAT_SERVER_NAME_EVEN, OPENTRACING_FAT_SERVER_NAME_ODD };
    private static final int NUM_SERVERS = SERVER_NAMES.length;

    private static final int SERVER_EVEN = 0;
    private static final int SERVER_ODD = 1;

    private static void setUpServer(int serverNo) {
        String methodName = "setUpServer";

        LibertyServer server =
            (servers[serverNo] = LibertyServerFactory.getLibertyServer( SERVER_NAMES[serverNo] ));

        if ( serverNo == SERVER_ODD ) {
            // The liberty server for the odd server MUST be informed that it uses
            // secondary ports.  Liberty servers default their ports to the java
            // properties for the default HTTP port.  Liberty servers DO NOT look
            // into the server logs or server configuration to figure out what ports
            // are actually in use.
            server.useSecondaryHTTPPort();
        }

        info(methodName,
            "Server [ " + Integer.toString(serverNo) + " ]" +
            " HTTP [ " + server.getHttpDefaultPort() + " ]" +
            " HTTPS [ " + server.getHttpDefaultSecurePort() + " ]");
    }

    private static LibertyServer getServer(int serverNo) {
        return servers[serverNo];
    }
    
    private static String getHostName(int serverNo) {
        return getServer(serverNo).getHostname();
    }

    private static int getPortNumber(int serverNo) {
        return getServer(serverNo).getHttpDefaultPort(); 
    }

    private static void startServer(int serverNo) throws Exception {
        getServer(serverNo).startServer(); // 'startServer' throws Exception
    }

    private static void stopServer(int serverNo) throws Exception {
        getServer(serverNo).stopServer(); // 'stopServer' throws Exception
    }

    private static void exportToServer(int serverNo, WebArchive serviceWar) throws Exception {
        ShrinkHelper.exportAppToServer( getServer(serverNo), serviceWar );
        // 'exportAppToServer' throws Exception
    }

    private static void prepareServer(int serverNo) throws Exception {
        setUpServer(serverNo);

        WebArchive serviceWar = createServiceWar();
        exportToServer(serverNo, serviceWar);
        startServer(serverNo);
    }

    // Test setup ...
    //
    // TODO: Maybe this should be done by the FAT suite.

    @BeforeClass
    public static void setUp() throws Exception {
        showPropertyPorts();
        
        for ( int serverNo = 0; serverNo < NUM_SERVERS; serverNo++ ) {
            prepareServer(serverNo);
        }
    }

    public static void showPropertyPorts() {
        showProperty("HTTP_default", "0");
        showProperty("HTTP_default.secure", "0");
        showProperty("HTTP_secondary", "0");
        showProperty("HTTP_secondary.secure", "0");
        showProperty("IIOP", "0");
    }

    public static void showProperty(String propertyName, String propertyDefault) {
        String methodName = "showProperty";

        String propertyValue = System.getProperty(propertyName);

        info(methodName,
             "Property [ " + propertyName + " ]" +
             " Actual [ " + propertyValue + " ]" +
             " Default [ " + propertyDefault + " ]"); 
    }

    @AfterClass
    public static void tearDown() throws Exception {
        for ( int serverNo = 0; serverNo < NUM_SERVERS; serverNo++ ) {
            stopServer(serverNo);
        }
    }

    /**
     * <p>Package up the service web module.</p>
     *
     * <p>The service classes are added, per
     * {@link FATOpentracingConstants#SERVICE_PACKAGE_NAME}.</p>
     *
     * <p>To make the tracer injection happen, a "beans.xml" must be added.</p>
     *
     * @return The packaged service web module.
     */
    private static WebArchive createServiceWar() {
        WebArchive serviceWar = ShrinkWrap.create(WebArchive.class, SERVICE_WAR_NAME);
        serviceWar.addPackages(true, SERVICE_PACKAGE_NAME);
        serviceWar.addAsWebInfResource(
            new File("test-applications/" + SERVICE_WAR_BASE_NAME + "/resources/beans.xml"));
        return serviceWar;
    }

    // Parent conditions ...
    //
    // These are used by inner class 'ParentCondition' to select completed
    // spans.
    //
    // See method 'verifyNestedSpans2' for a comprehensive example.

    public static final String[] GET_IMMEDIATE_CONDITION = new String[] { GET_IMMEDIATE_PATH };

    public static final String[] GET_MANUAL_CONDITION = new String[] { GET_MANUAL_PATH };
    public static final String[] MANUAL_CONDITION = new String[] { "manualSpan" };

    /**
     * <p>Answer the condition for a delayed get.</p>
     *
     * <p>Obtained using a method, since the condition depends on the particular
     * delay of the get.</p>
     *
     * @param delay The delay of the completed span.
     *
     * @return A string array of conditions for a delayed get.
     */
    public String[] getDelayedCondition(int delay) {
        return new String[] { GET_DELAYED_PATH, DELAY_PARAM_NAME + "=" + Integer.toString(delay) };
    }

    /**
     * <p>Answer the condition for a nested get.</p>
     *
     * <p>Obtained using a method, since the condition depends on the particular
     * nesting level of the get.</p>
     *
     * @param nestDepth The nesting depth of the completed span.
     *
     * @return A string array of conditions for a nested get.
     */
    public String[] getNestedCondition(int nestDepth) {
        return new String[] { GET_NESTED_PATH, NEST_DEPTH_PARAM_NAME + "=" + Integer.toString(nestDepth) };
    }

    // Verification utility ...

    /**
     * <p>Assertion utility for comparing two values.  Provides a more useful assertion
     * message to {@link Assert#assertEquals}.</p>
     *
     * @param valueName The name of the values which are being compared.
     * @param expectedValue The expected value.
     * @param actualValue The actual value.
     */
    public void assertEq(String valueName, Object expectedValue, Object actualValue) {
        Assert.assertEquals("EQ [ " + valueName + " ]", expectedValue, actualValue);
    }

    /**
     * <p>Assertion utility for comparing two values.  Provides a more useful assertion
     * message to {@link Assert#assertEquals}.</p>
     *
     * @param valueName The name of the values which are being compared.
     * @param expectedValue The expected value.
     * @param actualValue The actual value.
     */
    public void assertEq(String valueName, int expectedValue, int actualValue) {
        Assert.assertEquals("EQ [ " + valueName + " ]", expectedValue, actualValue);
    }

    /**
     * <p>Assertion utility for comparing two values with a less-than or equal-to test.
     * Provides a more useful assertion message to {@link Assert#assertTrue}.</p>
     *
     * @param valueName The name of the values which are being compared.
     * @param lesserValue The expected lesser value.
     * @param greaterValue The expected greater value.
     */
    public void assertLtEq(String valueName, long lesserValue, long greaterValue) {
        Assert.assertTrue(
                "LT_EQ [ " + valueName + " ] : [ " + Long.toString(lesserValue) + " ] [ " + greaterValue + " ]",
                (lesserValue <= greaterValue));
    }

    /**
     * <p>Assertion utility for comparing two values with a less-than test.
     * Provides a more useful assertion message to {@link Assert#assertTrue}.</p>
     *
     * @param valueName The name of the values which are being compared.
     * @param lesserValue The expected lesser value.
     * @param greaterValue The expected greater value.
     */
    public void assertLt(String valueName, long lesserValue, long greaterValue) {
        Assert.assertTrue(
                "LT [ " + valueName + " ] : [ " + Long.toString(lesserValue) + " ] [ " + greaterValue + " ]",
                (lesserValue < greaterValue));
    }

    /**
     * <p>Assertion utility for comparing two values with a less-than or equal-to test.
     * Provides a more useful assertion message to {@link Assert#assertTrue}.</p>
     *
     * @param valueName The name of the values which are being compared.
     * @param lesserValue The expected lesser value.
     * @param greaterValue The expected greater value.
     */
    public void assertLtEq(String valueName, int lesserValue, int greaterValue) {
        Assert.assertTrue(
                "LT_EQ [ " + valueName + " ] : [ " + Long.toString(lesserValue) + " ] [ " + greaterValue + " ]",
                (lesserValue <= greaterValue));
    }

    /**
     * <p>Assertion utility for comparing two values with a less-than test.
     * Provides a more useful assertion message to {@link Assert#assertTrue}.</p>
     *
     * @param valueName The name of the values which are being compared.
     * @param lesserValue The expected lesser value.
     * @param greaterValue The expected greater value.
     */
    public void assertLt(String valueName, int lesserValue, int greaterValue) {
        Assert.assertTrue(
                "LT [ " + valueName + " ] : [ " + Long.toString(lesserValue) + " ] [ " + greaterValue + " ]",
                (lesserValue < greaterValue));
    }
    
    // Span verification ...

    /**
     * <p>Select a span from a completed spans collection.  Select based on a specified
     * span kind, and specified selection text.</p>
     *
     * <p>A subset of the completed spans collection is examined, working from the end
     * of the collection.</p>
     *
     * <p>Selection is current against the operation of the completed span.  See
     * {@link FATSpan#getOperation()}.  Additional selection
     * capabilities could be added.</p>
     *
     * <p>The first matching span is selected and returned.  No more than one matching
     * span is expected.  (This is not verified.)</p>
     *
     * @param completedSpans The overall collection of completed spans which is to
     *     be searched.
     * @param tailSize The number of spans to examine, starting from the end of the
     *     collection of completed spans.
     * @param spanKind The type of span to select.
     * @param selectText Text to search for in the operations of the completed spans.
     *
     * @return The span which matches the span kind and selection text.  Null if
     *     no span is selected.
     */
    public FATSpan findSpan(
        List<FATSpan> completedSpans, int tailSize,
        FATSpanKind spanKind, String... selectText) {

        String methodName = "findSpan";

        int numSpans = completedSpans.size();
        for ( int spanNo = numSpans - tailSize; spanNo < numSpans; spanNo++ ) {
            FATSpan completedSpan = completedSpans.get(spanNo);
            if ( !completedSpan.isSpanKind(spanKind) ) {
                continue;
            }

            String operation = completedSpan.getOperation();

            boolean foundAll = true;
            for ( String text : selectText ) {
                if ( !operation.contains(text) ) {
                    foundAll = false;
                    break;
                }
            }
            if ( foundAll ) {
                return completedSpan;
            }
        }

        info(methodName, "Span selection:");
        info(methodName, "SpanKind: " + spanKind + "(" + spanKind.getTagValue() + ")");
        info(methodName, "Selection: " + selectionText(selectText));
        info(methodName, "Total spans: " + Integer.toString(numSpans));
        info(methodName, "Tail size: " + Integer.toString(tailSize));
        for ( int spanNo = numSpans - tailSize; spanNo < numSpans; spanNo++ ) {
            FATSpan completedSpan = completedSpans.get(spanNo);
            info(methodName, "  [ " + Integer.toString(spanNo) + " ] [ " + completedSpan + " ]");
        }

        return null;
    }

    /**
     * <p>Verify that a contiguous collection of spans is present as the tail
     * of a collection of completed spans.</p>
     *
     * <p>All of the completed spans of the tail of the spans collection must have
     * the same span ID (see {@link FATSpan#getTraceId()}, and
     * exactly one of the spans must be a root span.</p>
     *
     * <p>All of the completed spans must have a non-null span ID, trace ID,
     * and parent ID.  (A span with no parent is assigned "0" as its parent ID.)</p>
     *
     * <p>Fail with an assertion error if the verification fails.</p>
     *
     * @param completedSpans The overall collection of completed spans which is to
     *    be examined.
     * @param expectedSpanCount The number of spans which must be present
     *    in the spans collection, and which must have the same span ID.
     */
    public void verifyContiguousSpans(
        int serverNo,
        List<FATSpan> completedSpans,
        int expectedSpanCount) {

        String methodName = "verifyContiguousSpans";

        // *** The expected number of spans must be present. ***

        int actualSpanCount = completedSpans.size();
        assertLtEq("Completed Spans", expectedSpanCount, actualSpanCount);

        int rootCount = 0;

        String initialTraceId = null;

        for ( int spanNo = actualSpanCount - expectedSpanCount; spanNo < actualSpanCount; spanNo++ ) {
            FATSpan nextSpan = completedSpans.get(spanNo);
            String nextTraceId = nextSpan.getTraceId();
            String nextSpanId = nextSpan.getSpanId();
            String nextParentId = nextSpan.getParentId();

            // *** The trace, span, and parent IDs of each span cannot be null. ***

            Assert.assertNotNull("Trace id", nextTraceId);
            Assert.assertNotNull("Span id", nextSpanId);
            Assert.assertNotNull("Parent id", nextParentId);

            if ( initialTraceId == null ) {
                initialTraceId = nextTraceId;
            } else {
                assertEq("Trace id", nextTraceId, initialTraceId);
            }

            if ( nextSpan.isRoot() ) {
                rootCount++;
            }
        }

        if ( rootCount != 1 ) {
            for ( int spanNo = actualSpanCount - expectedSpanCount; spanNo < actualSpanCount; spanNo++ ) {
                FATSpan nextSpan = completedSpans.get(spanNo);
                info(methodName,
                    "Span [ " + Integer.toString(spanNo) + " ]" +
                    " SpanId [ " + nextSpan.getSpanId() + " ]" +
                    " ParentId [ " + nextSpan.getParentId() + " ]");
            }
        }

        // *** Exactly one root span must be present. ***

        assertEq("Root spans", Integer.valueOf(1), Integer.valueOf(rootCount));
    }

    public static final boolean IS_CONTAINER = true;
    public static final boolean IS_CLIENT = false;

    /**
     * <p>Build a print string for a selection array.  This is
     * used for building strings for assertions.</p>
     *
     * @param selections An array of strings to convert to a single print string.
     *
     * @return A print string for the string array.
     */
    public static String selectionText(String[] selections) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        boolean isFirst = true;
        for ( String selection : selections ) {
            String prefix;
            if ( isFirst ) {
                prefix = " ";
                isFirst = false;
            } else {
                prefix = ", ";
            }
            builder.append(prefix);

            builder.append("\"");
            builder.append(selection);
            builder.append("\"");
        }
        builder.append(" }");
        return builder.toString();
    }

    /**
     * <p>Type used for selecting completed spans.</p>
     *
     * <p>Conditions are specified for a pair of spans which are
     * expected to have a parent-child relationship.</p>
     *
     * <p>A null condition may be specified for the parent span,
     * in which case the child span is expected to be a root span.</p>
     *
     * <p>See {@link #findSpan}, which select spans based on the
     * span kind and selection array as specified by the parent
     * and child condition information expressed by a parent
     * condition.</p>
     */
    public static class ParentCondition {
        public final FATSpanKind parentKind;
        public final String[] parentSelection;

        public final FATSpanKind childKind;
        public final String[] childSelection;

        public ParentCondition(
            FATSpanKind parentKind, String[] parentSelection,
            FATSpanKind childKind, String[] childSelection) {

            this.parentKind = parentKind;
            this.parentSelection = parentSelection;

            this.childKind = childKind;
            this.childSelection = childSelection;
        }
    }

    /**
     * <p>Verify the parent-child relationships of a collection of
     * completed spans against a collection of parent-child relationship
     * conditions.</p>
     *
     * <p>Examine a subset of the spans, starting from the end of the spans
     * collection.</p>
     *
     * <p>For each of the specified parent-child relationship conditions,
     * select the parent and child spans according to that condition, and
     * verify that parent ID of the child span is the same as the span ID of
     * the parent span.</p>
     *
     * @param serverNo The number of server from which the spans were
     *     retrieved.
     * @param completedSpans The overall collection of spans which is to
     *     be tested.
     * @param tailSize The number of spans to examine within the completed
     *     spans collection.
     * @param parentConditions Conditions to verify within the completed
     *     spans collection.
     */
    public void verifyParents(
            int serverNo,
            List<FATSpan> completedSpans, int tailSize,
            ParentCondition... parentConditions) {

        String methodName = "verifyParents";

        for ( ParentCondition parentCondition : parentConditions ) {
            // *** Verify that a child span was selected.  A child span must always be selected. ***

            FATSpan childSpan =
                findSpan(completedSpans, tailSize,
                         parentCondition.childKind,
                         parentCondition.childSelection);
            Assert.assertNotNull("Child span selection [ " + selectionText(parentCondition.childSelection) + " ]", childSpan);

            FATSpan parentSpan;
            if ( parentCondition.parentSelection == null ) {
                // *** When the parent selection is null, verify that the child span is a root span. ***

                parentSpan = null;

                if ( !childSpan.isRoot() ) {
                    info(methodName, "Span [ " + selectionText(parentCondition.childSelection) + " ] [ " + childSpan + " ] should be root");
                }
                Assert.assertTrue("Span [ " + selectionText(parentCondition.childSelection) + " ] [ " + childSpan + " ] is root", childSpan.isRoot());

            } else {
                // *** When the parent selection is not null, verify that a parent span was selected. ***

                parentSpan = findSpan(
                    completedSpans, tailSize,
                     parentCondition.parentKind,
                     parentCondition.parentSelection);
                Assert.assertNotNull("Parent span selection [ " + selectionText(parentCondition.parentSelection) + " ]", parentSpan);

                // *** And verify that the parent ID of the child span matches the span ID of the parent span. ***

                String expectedParentId = parentSpan.getSpanId();
                String actualParentId = childSpan.getParentId();
                assertEq(
                    "Parent [ " + selectionText(parentCondition.parentSelection) + " ] [ " + parentSpan + " ]" +
                    " of [ " + selectionText(parentCondition.childSelection) + " ] [ " + childSpan + " ]",
                    expectedParentId, actualParentId);
            }
        }
    }

    /**
     * <p>Retrieve the completed span state by making a service call to the
     * opening tracing FAT service.</p>
     *
     * @param serverNo The number of the server from which to get the spans.
     * @param priorRequestPath The immediately preceding request made to the
     *     open tracing FAT service.  Used for to match this request within
     *     the overall flow of FAT service request.
     *
     * @return The list of completed spans obtained from the opening tracing FAT
     *     service.
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    public List<FATSpan> getCompletedSpans(int serverNo, String priorRequestPath) throws Exception {
        String methodName = "getCompletedSpans";

        String requestUrl = getRequestUrl(serverNo, GET_TRACER_STATE_PATH);
        FATLogging.info(CLASS, methodName, "Request", priorRequestPath);

        List<String> responseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl); // throws Exception

        FATLogging.info(CLASS, methodName, "Reponse:");

        int lineNo = 0;
        for ( String responseLine : responseLines ) {
            FATLogging.info(CLASS, methodName, "  [ " + Integer.toString(lineNo) + " ]", responseLine);
            lineNo++;
        }

        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for ( String responseLine : responseLines ) {
            if ( !isFirst ) {
                builder.append("\n");
            } else {
                isFirst = false;
            }
            builder.append(responseLine);
        }

        String responseText = builder.toString();
        List<FATSpan> spans =
            FATSpanUtils.parseSpans(responseText); // throws IOException
        return spans;
    }

    //

    /**
     * <p>Verify the spans of a prior request to obtain the tracer state.</p>
     *
     * <p>This is tested because it provides a simple, one span collection of
     * completed spans.</p>
     *
     * <p>This is tested, also, because a problem of the parent span ID being
     * incorrect set was noticed on the completed span information for the
     * trace state request.</p>
     *
     * @param serverNo The number of the server from which to retrieve spans.
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    public void verifyTracerStateEvent(int serverNo) throws Exception {
        List<FATSpan> completedSpans =
            getCompletedSpans(serverNo, GET_IMMEDIATE_PATH);

        verifyTracerStateEvent(serverNo, completedSpans);

        verifyContiguousSpans(serverNo, completedSpans, 1);
    }

    public void verifyTracerStateEvent(
        int serverNo,
        List<FATSpan> completedSpans) throws Exception {
        
        verifyTracerStateEvent( serverNo, completedSpans.get(completedSpans.size() - 1) );
    }

    public void verifyTracerStateEvent(int serverNo, FATSpan completedSpan) throws Exception {
        String requestUrl = getRequestUrl(serverNo, GET_TRACER_STATE_PATH); // throws UnsupportedEncodingException

        // *** The completed span event must be a root span. ***

        assertEq("parent", FATSpan.NULL_PARENT_ID, completedSpan.getParentId());

        // *** The completed span event must be be for a get tracer state request. ***

        assertEq("Operation", requestUrl, completedSpan.getOperation());

        // *** The completed span must have valid state and finish times. ***

        assertLtEq("Operation time", completedSpan.getStart(), completedSpan.getFinish());

        // *** The tags of the completed span must be as expected for a get tracer state request. ***

        verifyTags(completedSpan,
                FATSpan.TAG_HTTP_METHOD, "GET",
                FATSpan.TAG_HTTP_STATUS_CODE, "200",
                FATSpan.TAG_HTTP_URL, requestUrl,
                FATSpan.TAG_SPAN_KIND, "server");
    }

    public void verifyTags(FATSpan completedSpan, String... tagData) {
        int numTags = tagData.length / 2;
        for ( int tagNo = 0; tagNo < numTags; tagNo++ ) {
            String tag = tagData[tagNo * 2];
            String expectedValue = tagData[tagNo * 2 + 1];

            Object actualValue = completedSpan.getTag(tag);
            assertEq("Value for tag [ " + tag + " ]", expectedValue, actualValue);
        }
    }

    // Immediate request tests ...

    /**
     * <p>Test of a simple, immediate, request.</p>
     *
     * <p>Perform an immediate request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testImmediate() throws Exception {
        verifyImmediateService(SERVER_EVEN);
        verifyImmediateSpans(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    public void verifyImmediateService(int serverNo) throws Exception {
        String methodName = "verifyImmediateService";

        String responseText = "immediate";
        Map<String, Object> requestParms = new HashMap<String, Object>();
        requestParms.put(RESPONSE_PARAM_NAME, responseText);

        String requestUrl = getRequestUrl(serverNo, GET_IMMEDIATE_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // *** The immediate request is expected to have exactly one line of text. ***

        assertEq("Line count",
                 Integer.valueOf(1), Integer.valueOf(actualResponseLines.size()));

        // *** And is expected to have the response text as specified through the request parameter. ***

        assertEq("Reponse text",
                 responseText, actualResponseLines.get(0));
    }

    public void verifyImmediateSpans(int serverNo) throws Exception {
        List<FATSpan> completedSpans =
            getCompletedSpans(serverNo, GET_IMMEDIATE_PATH);

        int tailSize = 1;

        // *** The immediate request is expected to generate exactly one completed span. ***

        verifyContiguousSpans(serverNo, completedSpans, tailSize);

        // *** The single completed span must be a root span, and must be for the immediate get. ***

        ParentCondition getImmediateCondition = new ParentCondition(
                FATSpanKind.SERVER, null,
                FATSpanKind.SERVER, GET_IMMEDIATE_CONDITION);
        verifyParents(serverNo, completedSpans, tailSize, getImmediateCondition);
    }

    /**
     * <p>Test of a simple request which contains a single manually generated span..</p>
     *
     * <p>Perform  the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testManual() throws Exception {
        verifyManualService(SERVER_EVEN);
        verifyManualSpans(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    public void verifyManualService(int serverNo) throws Exception {
        String methodName = "verifyManualService";

        String responseText = "manualResponse";
        Map<String, Object> requestParms = new HashMap<String, Object>();
        requestParms.put(RESPONSE_PARAM_NAME, responseText);

        String requestUrl = getRequestUrl(serverNo, GET_MANUAL_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // *** The manual request is expected to have exactly one line of text. ***

        assertEq("Line count",
                 Integer.valueOf(1), Integer.valueOf(actualResponseLines.size()));

        // *** And is expected to have the response text as specified through the request parameter. ***

        assertEq("Reponse text",
                 responseText, actualResponseLines.get(0));
    }

    public void verifyManualSpans(int serverNo) throws Exception {
        List<FATSpan> completedSpans =
            getCompletedSpans(serverNo, GET_MANUAL_PATH);

        int tailSize = 2;

        // *** The manual request is expected to generate exactly two completed spans. ***

        verifyContiguousSpans(serverNo, completedSpans, tailSize);

        ParentCondition getManualCondition = new ParentCondition(
            FATSpanKind.SERVER, null,
            FATSpanKind.SERVER, GET_MANUAL_CONDITION);
        ParentCondition manualCondition = new ParentCondition(
            FATSpanKind.SERVER, GET_MANUAL_CONDITION,
            FATSpanKind.MANUAL, MANUAL_CONDITION);

        verifyParents(
            serverNo, completedSpans, tailSize,
            getManualCondition, manualCondition);
    }

    // Asynchronous request tests ...

    /**
     * <p>Test of a simple request which has an asynchronous response.</p>
     *
     * <p>Test with a delay of two (2) seconds on the response.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testDelayed2() throws Exception {
        verifyDelayedService(SERVER_EVEN, 2);
        verifyDelayedSpans(SERVER_EVEN, 2);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    /**
     * <p>Test of a simple request which has an asynchronous response.</p>
     *
     * <p>Test with a delay of four (4) seconds on the response.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testDelayed4() throws Exception {
        verifyDelayedService(SERVER_EVEN, 4);
        verifyDelayedSpans(SERVER_EVEN, 4);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    /**
     * <p>Test of a simple request which has an asynchronous response.</p>
     *
     * <p>Test with a delay of six (6) seconds on the response.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testDelayed6() throws Exception {
        verifyDelayedService(SERVER_EVEN, 6);
        verifyDelayedSpans(SERVER_EVEN, 6);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    public void verifyDelayedService(int serverNo, int delaySec) throws Exception {
        String methodName = "verifyDelayedService";

        String responseText = "delayed [ " + Integer.toString(delaySec) + " ]";
        Map<String, Object> requestParms = new HashMap<String, Object>();
        requestParms.put(RESPONSE_PARAM_NAME, responseText);
        requestParms.put(DELAY_PARAM_NAME, Integer.valueOf(delaySec));

        String requestUrl = getRequestUrl(serverNo, GET_DELAYED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // *** The delayed request is expected to have exactly one line of text. ***

        assertEq("Line count",
                 Integer.valueOf(1), Integer.valueOf(actualResponseLines.size()));

        // *** And is expected to have the response text as specified through the request parameter. ***

        assertEq("Reponse text",
                 responseText, actualResponseLines.get(0));
   }

    public void verifyDelayedSpans(int serverNo, int delaySec) throws Exception {
        List<FATSpan> completedSpans =
            getCompletedSpans(serverNo, GET_DELAYED_PATH);

        int tailSize = 1;

        // *** The delayed request is expected to generate exactly one completed span. ***

        verifyContiguousSpans(serverNo, completedSpans, tailSize);

        ParentCondition getDelayedCondition = new ParentCondition(
                FATSpanKind.SERVER, null,
                FATSpanKind.SERVER, getDelayedCondition(delaySec));
        verifyParents(serverNo, completedSpans, tailSize, getDelayedCondition);
    }

    // Complex (nested) request tests ...

    /**
     * <p>Test of a simple request through the nesting service.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testNested0() throws Exception {
        verifyNestedService0(SERVER_EVEN);
        verifyNestedSpans0(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    //

    /**
     * <p>Test requests through the nesting service.</p>
     *
     * <p>At nesting level 1, call out three times to the delayed service.
     * Make synchronous (in-order) calls to the delayed service.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * <p>For verification details, see {@link #verifyNestedSpans1}, which examines
     * the generated completed spans in detail.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testNested1Sync() throws Exception {
        verifyNestedService1(SERVER_EVEN, IS_SYNC);
        verifyNestedSpans1(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    /**
     * <p>Test requests through the nesting service.</p>
     *
     * <p>At nesting level 2, the service calls back to itself with the nest level
     * changed to 1, which then call out three times to the delayed service.  Make
     * synchronous (in-order) calls to the delayed service.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * <p>For verification details, see {@link #verifyNestedSpans2}, which examines
     * the generated completed spans in detail.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testNested2Sync() throws Exception {
        verifyNestedService2(SERVER_EVEN, IS_SYNC);
        verifyNestedSpans2(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    /**
     * <p>Test requests through the nesting service.</p>
     *
     * <p>At nesting level 4, the service calls back to itself three times,
     * then calls out three times to the delayed service.  Make synchronous
     * (in-order) calls to the delayed service.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * <p>For verification details, see {@link #verifyNestedSpans4}, which examines
     * the generated completed spans in detail.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testNested4Sync() throws Exception {
        verifyNestedService4(SERVER_EVEN, IS_SYNC);
        verifyNestedSpans4(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    //

    /**
     * <p>Test requests through the nesting service.</p>
     *
     * <p>At nesting level 1, call out three times to the delayed service.
     * Make asynchronous calls to the delayed service.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * <p>For verification details, see {@link #verifyNestedSpans1}, which examines
     * the generated completed spans in detail.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testNested1Async() throws Exception {
        verifyNestedService1(SERVER_EVEN, IS_ASYNC);
        verifyNestedSpans1(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    /**
     * <p>Test requests through the nesting service.</p>
     *
     * <p>At nesting level 2, the service calls back to itself with the nest level
     * changed to 1, which then call out three times to the delayed service.
     * Make asynchronous calls to the delayed service.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * <p>For verification details, see {@link #verifyNestedSpans2}, which examines
     * the generated completed spans in detail.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testNested2Async() throws Exception {
        verifyNestedService2(SERVER_EVEN, IS_ASYNC);
        verifyNestedSpans2(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    /**
     * <p>Test requests through the nesting service.</p>
     *
     * <p>At nesting level 4, the service calls back to itself three times,
     * then calls out three times to the delayed service.  Make asynchronous
     * calls to the delayed service.</p>
     *
     * <p>Perform the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * <p>For verification details, see {@link #verifyNestedSpans4}, which examines
     * the generated completed spans in detail.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshaled from the text obtained from the FAT service.
     */
    @Test
    public void testNested4Async() throws Exception {
        verifyNestedService4(SERVER_EVEN, IS_ASYNC);
        verifyNestedSpans4(SERVER_EVEN);
        verifyTracerStateEvent(SERVER_EVEN);
    }

    //

    public static final boolean IS_ASYNC = true;
    public static final boolean IS_SYNC = false;

    public static final boolean IS_ALTERNATE = true;
    public static final boolean IS_NOT_ALTERNATE = false;
    
    public Map<String, Object> getNestedParms(
        int nestDepth,
        boolean async, boolean alternate,
        String responseText) {

        Map<String, Object> nestedParms = new HashMap<String, Object>();
        nestedParms.put(RESPONSE_PARAM_NAME, responseText);
        nestedParms.put(NEST_DEPTH_PARAM_NAME, Integer.valueOf(nestDepth));
        nestedParms.put(ASYNC_PARAM_NAME, Boolean.valueOf(async));

        nestedParms.put(HOST_PARAM_NAME_EVEN, getHostName(SERVER_EVEN));
        nestedParms.put(PORT_PARAM_NAME_EVEN, Integer.valueOf(getPortNumber(SERVER_EVEN)));
        
        if ( alternate ) {
            nestedParms.put(HOST_PARAM_NAME_ODD, getHostName(SERVER_ODD));
            nestedParms.put(PORT_PARAM_NAME_ODD, Integer.valueOf(getPortNumber(SERVER_ODD)));
        } else {
            nestedParms.put(HOST_PARAM_NAME_ODD, getHostName(SERVER_EVEN));
            nestedParms.put(PORT_PARAM_NAME_ODD, Integer.valueOf(getPortNumber(SERVER_EVEN)));
        }

        nestedParms.put(CONTEXT_ROOT_PARAM_NAME, SERVICE_CONTEXT_ROOT);
        return nestedParms;
    }

    public void verifyNestedService0(int serverNo) throws Exception {
        String methodName = "verifyNestedService0";

        int nestDepth = 0;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, IS_SYNC, IS_NOT_ALTERNATE, responseText);

        String requestUrl = getRequestUrl(serverNo, GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // *** The delayed request is expected to have exactly one line of text. ***

        assertEq("Line count",
                 Integer.valueOf(1), Integer.valueOf(actualResponseLines.size()));

        // *** And is expected to have the response text as specified through the request parameter. ***

        assertEq("Reponse text",
                 responseText, actualResponseLines.get(0));
    }

    public void verifyNestedSpans0(int serverNo) throws Exception {
        List<FATSpan> completedSpans =
            getCompletedSpans(serverNo, GET_NESTED_PATH);

        int tailSize = 1;

        // *** The delayed request is expected to generate exactly one completed span. ***

        verifyContiguousSpans(serverNo, completedSpans, tailSize);

        ParentCondition getNestedCondition = new ParentCondition(
                FATSpanKind.SERVER, null,
                FATSpanKind.SERVER, getNestedCondition(0));
        verifyParents(serverNo, completedSpans, tailSize, getNestedCondition);
    }

    public void verifyNestedService1(int serverNo, boolean async) throws Exception {
        String methodName = "verifyNestedService1";

        int nestDepth = 1;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ] async [ " + Boolean.toString(async) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, async, IS_NOT_ALTERNATE, responseText);

        String requestUrl = getRequestUrl(serverNo, GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // TODO: Verify the lines in detail.
    }

    public void verifyNestedSpans1(int serverNo) throws Exception {
        List<FATSpan> completedSpans =
            getCompletedSpans(serverNo, GET_NESTED_PATH);

        // -> *TopInFromAbove
        //   -> *TopOutToBelow -> *Delay2InFromAbove -> Delay2OutToAbove -> TopInFromBelow
        //   -> *TopOutToBelow -> *Delay4InFromAbove -> Delay4OutToAbove -> TopInFromBelow
        //   -> *TopOutToBelow -> *Delay6InFromAbove -> Delay6OutToAbove -> TopInFromBelow
        // -> TopOutToAbove
        //
        // Each '*' shows a new span; we expect seven (7) spans to be completed.

        int tailSize = 7;

        // *** The nested request is expected to generate exactly seven (7) ***
        // *** completed span (per the preceding comment). ***

        verifyContiguousSpans(serverNo, completedSpans, tailSize);

        // *** A root span for the initial get nested request. ***

        String[] getNested1Text = getNestedCondition(1);
        ParentCondition getNested1Condition = new ParentCondition(
                FATSpanKind.SERVER, null,
                FATSpanKind.SERVER, getNested1Text);

        String[] getDelayed2Text = getDelayedCondition(2);
        String[] getDelayed4Text = getDelayedCondition(4);
        String[] getDelayed6Text = getDelayedCondition(6);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the two second delay request. ***

        ParentCondition getDelay2ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed2Text);
        ParentCondition getDelay2ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed2Text,
            FATSpanKind.SERVER, getDelayed2Text);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the four second delay request. ***

        ParentCondition getDelay4ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed4Text);
        ParentCondition getDelay4ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed4Text,
            FATSpanKind.SERVER, getDelayed4Text);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the six second delay request. ***

        ParentCondition getDelay6ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed6Text);
        ParentCondition getDelay6ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed6Text,
            FATSpanKind.SERVER, getDelayed6Text);

        verifyParents(
            serverNo, completedSpans, tailSize,
            getNested1Condition,
            getDelay2ClientCondition, getDelay2ContainerCondition,
            getDelay4ClientCondition, getDelay4ContainerCondition,
            getDelay6ClientCondition, getDelay6ContainerCondition);
    }

    public void verifyNestedService2(int serverNo, boolean async) throws Exception {
        String methodName = "verifyNestedService2";

        int nestDepth = 2;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ] async [ " + Boolean.toString(async) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, async, IS_NOT_ALTERNATE, responseText);

        String requestUrl = getRequestUrl(serverNo, GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // TODO: Verify the lines in detail.
    }

    public void verifyNestedSpans2(int serverNo) throws Exception {
        List<FATSpan> completedSpans =
            getCompletedSpans(serverNo, GET_NESTED_PATH);

        // -> *TopInFromAbove
        //   -> *TopOutToBelow
        //     -> *TopInFromAbove
        //       -> *TopOutToBelow -> *Delay2InFromAbove -> Delay2OutToAbove -> TopInFromBelow
        //       -> *TopOutToBelow -> *Delay4InFromAbove -> Delay4OutToAbove -> TopInFromBelow
        //       -> *TopOutToBelow -> *Delay6InFromAbove -> Delay6OutToAbove -> TopInFromBelow
        //     -> TopOutToAbove
        //   -> TopInFromBelow
        // -> TopOutToAbove

        // Each '*' shows a new span; we expect nine (9) spans to be completed.

        int tailSize = 9;

        // *** The nested request is expected to generate exactly nine (9) ***
        // *** completed span (per the preceding comment). ***

        verifyContiguousSpans(serverNo, completedSpans, tailSize);

        // *** A root span for the initial depth 2 get nested request. ***

        String[] getNested2Text = getNestedCondition(2);
        ParentCondition getNested2Condition = new ParentCondition(
            FATSpanKind.SERVER, null,
            FATSpanKind.SERVER, getNested2Text);

        // *** A pair of completed spans for the call from nest level 2 ***
        // *** request to the nest level 1 request. ***

        String[] getNested1Text = getNestedCondition(1);
        ParentCondition getNested1ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested2Text,
            FATSpanKind.CLIENT, getNested1Text);
        ParentCondition getNested1ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getNested1Text,
            FATSpanKind.SERVER, getNested1Text);

        String[] getDelayed2Text = getDelayedCondition(2);
        String[] getDelayed4Text = getDelayedCondition(4);
        String[] getDelayed6Text = getDelayedCondition(6);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the two second delay request. ***

        ParentCondition getDelay2ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed2Text);
        ParentCondition getDelay2ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed2Text,
            FATSpanKind.SERVER, getDelayed2Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the four second delay request. ***

        ParentCondition getDelay4ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed4Text);
        ParentCondition getDelay4ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed4Text,
            FATSpanKind.SERVER, getDelayed4Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the six second delay request. ***

        ParentCondition getDelay6ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed6Text);
        ParentCondition getDelay6ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed6Text,
            FATSpanKind.SERVER, getDelayed6Text);

        verifyParents(
            serverNo, completedSpans, tailSize,
            getNested2Condition,
            getNested1ClientCondition, getNested1ContainerCondition,
            getDelay2ClientCondition, getDelay2ContainerCondition,
            getDelay4ClientCondition, getDelay4ContainerCondition,
            getDelay6ClientCondition, getDelay6ContainerCondition);
    }

    public void verifyNestedService4(int serverNo, boolean async) throws Exception {
        String methodName = "verifyNestedService4";

        int nestDepth = 4;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ] async [ " + Boolean.toString(async) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, async, IS_NOT_ALTERNATE, responseText);

        String requestUrl = getRequestUrl(serverNo, GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // TODO: Verify the lines in detail.
    }

    public void verifyNestedSpans4(int serverNo) throws Exception {
        verifyNestedSpans4( serverNo, getCompletedSpans(serverNo, GET_NESTED_PATH) );
    }
    
    public void verifyNestedSpans4(
        int serverNo,
        List<FATSpan> completedSpans) throws Exception {

        // -> *TopInFromAbove [d=4]
        //   -> *TopOutToBelow
        //     -> *TopInFromAbove [d=3]
        //       -> *TopOutToBelow
        //
        // -> *TopInFromAbove [d=2]
        //   -> *TopOutToBelow
        //     -> *TopInFromAbove [d=1]
        //       -> *TopOutToBelow -> *Delay2InFromAbove -> Delay2OutToAbove -> TopInFromBelow
        //       -> *TopOutToBelow -> *Delay4InFromAbove -> Delay4OutToAbove -> TopInFromBelow
        //       -> *TopOutToBelow -> *Delay6InFromAbove -> Delay6OutToAbove -> TopInFromBelow
        //     -> TopOutToAbove
        //   -> TopInFromBelow
        // -> TopOutToAbove
        //
        //       -> TopInFromToBelow
        //     -> TopOutToAbove [d=3]
        //   -> TopInFromBelow
        // -> TopOutToAbove [d=4]

        // Each '*' shows a new span; we expect thirteen (13) spans to be completed.

        int tailSize = 13;

        // *** The nested request is expected to generate exactly thirteen (13) ***
        // *** completed span (per the preceding comment). ***

        verifyContiguousSpans(serverNo, completedSpans, tailSize);

        // *** The completed spans of the nested request have a complex relationship, ***
        // *** as described by the preceding comment. ***

        // *** A root span for the initial depth 2 get nested request. ***

        String[] getNested4Text = getNestedCondition(4);
        ParentCondition getNested4Condition = new ParentCondition(
            FATSpanKind.SERVER, null,
            FATSpanKind.SERVER, getNested4Text);

        // *** A pair of completed spans for the call from nest level 4 ***
        // *** request to the nest level 3 request. ***

        String[] getNested3Text = getNestedCondition(3);
        ParentCondition getNested3ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested4Text,
            FATSpanKind.CLIENT, getNested3Text);
        ParentCondition getNested3ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getNested3Text,
            FATSpanKind.SERVER, getNested3Text);

        // *** A pair of completed spans for the call from nest level 3 ***
        // *** request to the nest level 2 request. ***

        String[] getNested2Text = getNestedCondition(2);
        ParentCondition getNested2ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested3Text,
            FATSpanKind.CLIENT, getNested2Text);
        ParentCondition getNested2ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getNested2Text,
            FATSpanKind.SERVER, getNested2Text);

        // *** A pair of completed spans for the call from nest level 2 ***
        // *** request to the nest level 1 request. ***

        String[] getNested1Text = getNestedCondition(1);
        ParentCondition getNested1ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested2Text,
            FATSpanKind.CLIENT, getNested1Text);
        ParentCondition getNested1ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getNested1Text,
            FATSpanKind.SERVER, getNested1Text);

        String[] getDelayed2Text = getDelayedCondition(2);
        String[] getDelayed4Text = getDelayedCondition(4);
        String[] getDelayed6Text = getDelayedCondition(6);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the two second delay request. ***

        ParentCondition getDelay2ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed2Text);
        ParentCondition getDelay2ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed2Text,
            FATSpanKind.SERVER, getDelayed2Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the four second delay request. ***

        ParentCondition getDelay4ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed4Text);
        ParentCondition getDelay4ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed4Text,
            FATSpanKind.SERVER, getDelayed4Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the six second delay request. ***

        ParentCondition getDelay6ClientCondition = new ParentCondition(
            FATSpanKind.SERVER, getNested1Text,
            FATSpanKind.CLIENT, getDelayed6Text);
        ParentCondition getDelay6ContainerCondition = new ParentCondition(
            FATSpanKind.CLIENT, getDelayed6Text,
            FATSpanKind.SERVER, getDelayed6Text);

        verifyParents(
            serverNo, completedSpans, tailSize,
            getNested4Condition,
            getNested3ClientCondition, getNested3ContainerCondition,
            getNested2ClientCondition, getNested2ContainerCondition,
            getNested1ClientCondition, getNested1ContainerCondition,
            getDelay2ClientCondition, getDelay2ContainerCondition,
            getDelay4ClientCondition, getDelay4ContainerCondition,
            getDelay6ClientCondition, getDelay6ContainerCondition);
    }

// The complete spans from the "nested=4" case should have this pattern:
//
//    { "traceId": "72", "parentId": "80", "spanId": "81",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=2&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+2+%5D",
//      "start": 1505841639876000, "finish": 1505841641904000, "elapsed": 2028000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=2&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+2+%5D", "http.status_code": "200", "span.kind": "server", "http.method": "GET" } },
//    { "traceId": "72", "parentId": "79", "spanId": "80",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=2&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+2+%5D",
//      "start": 1505841639876000, "finish": 1505841641904000, "elapsed": 2028000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=2&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+2+%5D", "http.status_code": "200", "span.kind": "client", "http.method": "GET" } },
//
//    { "traceId": "72", "parentId": "82", "spanId": "83",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=4&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+4+%5D",
//      "start": 1505841639891000, "finish": 1505841643916000, "elapsed": 4025000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=4&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+4+%5D", "http.status_code": "200", "span.kind": "server", "http.method": "GET" } },
//    { "traceId": "72", "parentId": "79", "spanId": "82",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=4&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+4+%5D",
//      "start": 1505841639891000, "finish": 1505841643916000, "elapsed": 4025000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=4&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+4+%5D", "http.status_code": "200", "span.kind": "client", "http.method": "GET" } },
//
//    { "traceId": "72", "parentId": "84", "spanId": "85",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=6&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+6+%5D",
//      "start": 1505841639907000, "finish": 1505841645929000, "elapsed": 6022000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=6&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+6+%5D", "http.status_code": "200", "span.kind": "server", "http.method": "GET" } },
//    { "traceId": "72", "parentId": "79", "spanId": "84",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=6&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+6+%5D",
//      "start": 1505841639907000, "finish": 1505841645929000, "elapsed": 6022000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getDelayed?delay=6&response=nested+%5B+4+%5D+async+%5B+true+%5D+%5B+6+%5D", "http.status_code": "200", "span.kind": "client", "http.method": "GET" } },
//
//    { "traceId": "72", "parentId": "78", "spanId": "79",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=1&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp",
//      "start": 1505841639845000, "finish": 1505841645929000, "elapsed": 6084000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=1&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp", "http.status_code": "200", "span.kind": "server", "http.method": "GET" } },
//    { "traceId": "72", "parentId": "77", "spanId": "78",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=1&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp",
//      "start": 1505841639845000, "finish": 1505841645929000, "elapsed": 6084000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=1&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp", "http.status_code": "200", "span.kind": "client", "http.method": "GET" } },
//
//    { "traceId": "72", "parentId": "76", "spanId": "77",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=2&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp",
//      "start": 1505841639829000, "finish": 1505841645929000, "elapsed": 6100000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=2&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp", "http.status_code": "200", "span.kind": "server", "http.method": "GET" } },
//    { "traceId": "72", "parentId": "75", "spanId": "76",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=2&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp",
//      "start": 1505841639813000, "finish": 1505841645929000, "elapsed": 6116000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=2&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp", "http.status_code": "200", "span.kind": "client", "http.method": "GET" } },
//
//    { "traceId": "72", "parentId": "74", "spanId": "75",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=3&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp",
//      "start": 1505841639782000, "finish": 1505841645929000, "elapsed": 6147000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=3&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp", "http.status_code": "200", "span.kind": "server", "http.method": "GET" } },
//    { "traceId": "72", "parentId": "73", "spanId": "74",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=3&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp",
//      "start": 1505841639782000, "finish": 1505841645929000, "elapsed": 6147000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=3&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp", "http.status_code": "200", "span.kind": "client", "http.method": "GET" } },
//
//    { "traceId": "72", "parentId": "0", "spanId": "73",
//      "operation": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=4&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp",
//      "start": 1505841639751000, "finish": 1505841645944000, "elapsed": 6193000,
//      "tags": { "http.url": "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=4&async=true&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp", "http.status_code": "200", "span.kind": "server", "http.method": "GET" } } ] }

    // Request utilities ...

    private String getRequestUrl(int serverNo, String servicePath)
        throws UnsupportedEncodingException {
        return getRequestUrl(serverNo, servicePath, null); // throws UnsupportedEncodingException
    }

    private String getRequestUrl(
        int serverNo, 
        String endpointPath,
        Map<String, Object> serviceParameters) throws UnsupportedEncodingException {

        String requestPath = FATUtilsServer.getRequestPath(
            SERVICE_CONTEXT_ROOT,
            APP_PATH, SERVICE_PATH, endpointPath,
            serviceParameters);
        // throws UnsupportedEncodingException

        LibertyServer server = getServer(serverNo);

        return FATUtilsServer.getRequestUrl(
            server.getHostname(),
            server.getHttpDefaultPort(),
            requestPath );
    }

    //

    public static int requestNumber;
    public static class RequestNumberLock {
        // EMPTY
    }
    public static final RequestNumberLock REQUEST_NUMBER_LOCK = new RequestNumberLock();
    
    public static int nextRequestNumber() {
        synchronized( REQUEST_NUMBER_LOCK ) {
            return requestNumber++;
        }
    }

    public static int getRequestCount() {
        synchronized( REQUEST_NUMBER_LOCK ) {
            return requestNumber;
        }
    }

    //

    public class RequestSpinner implements Runnable {
        public RequestSpinner(int planNo, int requestCount, boolean alternate) {
            this.planNo = planNo;
            this.requestCount = requestCount;
            this.alternate = alternate;
        }

        private final int planNo;

        public int getPlanNo() {
            return planNo;
        }

        private final int requestCount;

        public int getRequestCount() {
            return requestCount;
        }

        private final boolean alternate;
        
        public boolean getAlternate() {
            return alternate;
        }

        public void run() {
            String methodName = "run";

            int useRequestCount = getRequestCount();
            for ( int requestNo = 0; requestNo < useRequestCount; requestNo++ ) {
                try {
                    spin(requestNo); // throws Exception
                } catch ( Exception e ) {
                    info(methodName,
                        "Spin failure at [ " + Integer.toString(planNo) + " ] [ " + Integer.toString(requestNo) + " ]",
                        e.getMessage());
                    break;
                }
            }
        }

        public void spin(int localRequestNo) throws Exception {
            String methodName = "spin";

            int globalRequestNo = nextRequestNumber();

            int nestDepth = 4;
            String responseText =
                "nest [ " + Integer.toString(nestDepth) + " ]" +
                " local [ " + Integer.toString(localRequestNo) + " ]" +
                " global [ " + Integer.toString(globalRequestNo) + " ]" +
                " plan [ " + Integer.toString(planNo) + " ]" +
                " alt [ " + Boolean.toString(getAlternate()) + " ]";

            Map<String, Object> requestParms =
                getNestedParms(nestDepth, IS_ASYNC, getAlternate(), responseText);

            String requestUrl = getRequestUrl(SERVER_EVEN, GET_NESTED_PATH, requestParms);
            // throws UnsupportedEncodingException

            info(methodName, "Request URL", requestUrl);
            info(methodName, "Expected Response", responseText);

            List<String> actualResponseLines =
                FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
            // throws Exception
            info(methodName, "Actual Response", actualResponseLines);

            // *** The request must have the specified response text. ***

            String matchLine = null;
            for ( String responseLine : actualResponseLines ) {
                if ( responseLine.contains(responseText) ) {
                    matchLine = responseLine;
                    break;
                }
            }
            if ( matchLine == null ) {
                Assert.assertNotNull("Expected response [ " + responseText + " ]", matchLine);
            }
        }
    }

    public Thread spin(String spinnerName, int planNo, int spinCount, boolean alternate) {
        String methodName = "spin";

        info(methodName, "Launch", spinnerName);

        Runnable spinRunner = new RequestSpinner(planNo, spinCount, alternate);
        Thread spinThread = new Thread(spinRunner, spinnerName);
        spinThread.start();

        return spinThread;
    }

    public static final int[] SPIN_TEST_PLAN = { 10, 20, 10, 40, 10 };

    /**
     * <p>Test span generation for concurrent requests.</p>
     * 
     * <p>A single thread is created per entry of {@link #SPIN_TEST_PLAN}.
     * The entry of the test plan is the number of times the thread performs
     * a nested get at depth four (4) ({@link FATOpentracingConstants#GET_NESTED_PATH}).</p>
     * 
     * @throws Exception Thrown in case of a failure to run the requests.
     */
    @Test
    public void testManyRequests() throws Exception {
        String methodName = "testManyRequests";
        info(methodName, "ENTER");

        int initialRequestCount = getRequestCount();
        int expectedTotal = runSpinners(IS_NOT_ALTERNATE);
        int finalRequestCount = getRequestCount();

        // *** All request must have run ***

        assertEq("Expected count of requests", expectedTotal, (finalRequestCount - initialRequestCount));

        verifySpinners();

        info(methodName, "RETURN");
    }

    /**
     * <p>Test span generation for concurrent requests.</p>
     * 
     * <p>A single thread is created per entry of {@link #SPIN_TEST_PLAN}.
     * The entry of the test plan is the number of times the thread performs
     * a nested get at depth four (4) ({@link FATOpentracingConstants#GET_NESTED_PATH}).</p>
     * 
     * @throws Exception Thrown in case of a failure to run the requests.
     */
    @Test
    public void testManyDistrbutedRequests() throws Exception {
        String methodName = "testManyDistributedRequests";
        info(methodName, "ENTER");

        int initialRequestCount = getRequestCount();
        int expectedTotal = runSpinners(IS_ALTERNATE);
        int finalRequestCount = getRequestCount();        

        // *** All request must have run ***

        assertEq("Expected count of requests", expectedTotal, finalRequestCount - initialRequestCount);

        verifyDistributedSpinners();

        info(methodName, "RETURN");
    }

    public int runSpinners(boolean isAlternate) {
        String methodName = "runSpinners";
        info(methodName, "ENTER");
        
        Thread[] spinners = new Thread[SPIN_TEST_PLAN.length];
        int expectedTotal = 0;

        for ( int planNo = 0; planNo < SPIN_TEST_PLAN.length; planNo++ ) {
            int spinCount = SPIN_TEST_PLAN[planNo];

            info(methodName, "Plan", Integer.valueOf(planNo));
            info(methodName, "Request count", Integer.valueOf(spinCount));

            expectedTotal += spinCount;

            String spinnerName = "Plan [ " + Integer.toString(planNo) + " ] Spin [ " + Integer.toString(spinCount) + " ]";
            spinners[planNo] = spin(spinnerName, planNo, spinCount, isAlternate);
        }

        info(methodName, "Total request count", Integer.valueOf(expectedTotal));

        for ( Thread spinner : spinners ) {
            try {
                spinner.join(); // InterruptedException
            } catch ( InterruptedException e ) {
                info(methodName, "Failed spinner", spinner.getName());
            }
        }

        info(methodName, "RETURN");
        return expectedTotal;
    }

    public void verifySpinners() throws Exception {
        String methodName = "verifySpinners";

        List<FATSpan> completedSpans =
            getCompletedSpans(SERVER_EVEN, GET_NESTED_PATH);
        info(methodName, "Completed spans", Integer.valueOf(completedSpans.size()));

        filterSpans(completedSpans, IS_NOT_ALTERNATE);

        verifySpinners(completedSpans);
    }

    public void filterSpans(List<FATSpan> completedSpans, boolean isAlternate) {
        String methodName = "filterSpans";

        info(methodName, "Initial spans", Integer.valueOf(completedSpans.size()));

        Iterator<FATSpan> useCompletedSpans = completedSpans.iterator();
        while ( useCompletedSpans.hasNext() ) {
            FATSpan completedSpan = useCompletedSpans.next();
            if ( isAlternate != isAlternate(completedSpan) ) {
                useCompletedSpans.remove();
            }
        }

        info(methodName, "Final spans", Integer.valueOf(completedSpans.size()));
    }

    public static final String ALT_PREFIX = "alt+%5B+";
    public static final String ALT_SUFFIX = "+%5D";
    public static final String ALT_RESPONSE_TRUE = ALT_PREFIX + Boolean.TRUE.toString() + ALT_SUFFIX;
    public static final String ALT_RESPONSE_FALSE = ALT_PREFIX + Boolean.FALSE.toString() + ALT_SUFFIX;

    public boolean isAlternate(FATSpan span) {
        String url = span.getTag(FATSpan.TAG_HTTP_URL);
        return ( (url != null) && (url.indexOf(ALT_RESPONSE_TRUE) != -1) );
    }

    public void verifyDistributedSpinners() throws Exception {
        String methodName = "verifyDistributedSpinners";

        List<FATSpan> completedSpansEven =
            getCompletedSpans(SERVER_EVEN, GET_NESTED_PATH);
        info(methodName,
             "Completed spans (" + SERVER_NAMES[SERVER_EVEN] + ")",
             Integer.valueOf(completedSpansEven.size()));

        // There will be many completed spans ... pull out only those which are for
        // the alternating case.

        filterSpans(completedSpansEven, IS_ALTERNATE);

        List<FATSpan> completedSpansOdd =
            getCompletedSpans(SERVER_ODD, GET_NESTED_PATH);
        info(methodName,
             "Completed spans (" + SERVER_NAMES[SERVER_ODD] + ")",
             Integer.valueOf(completedSpansOdd.size()));

        // There will be many completed spans ... pull out only those which are for
        // the alternating case.

        filterSpans(completedSpansOdd, IS_ALTERNATE);

        verifyDistributedSpinners(completedSpansEven, completedSpansOdd);

        List<FATSpan> completedSpans =
            new ArrayList<FATSpan>( completedSpansEven.size() + completedSpansOdd.size() );
        completedSpans.addAll(completedSpansEven);
        completedSpans.addAll(completedSpansOdd);

        verifySpinners(completedSpans);
    }

    public void verifyDistributedSpinners(
        List<FATSpan> completedSpansEven,
        List<FATSpan> completedSpansOdd) throws Exception {
        String methodName = "verifyDistributedSpinners";

        info(methodName, "ENTER");

        for ( FATSpan evenSpan : completedSpansEven ) {
            validateDistributedSpan(evenSpan, SERVER_EVEN);
        }
        for ( FATSpan oddSpan : completedSpansOdd ) {
            validateDistributedSpan(oddSpan, SERVER_ODD);
        }

        info(methodName, "RETURN");
    }

    // Sample trimmed span data, for the alternating case:
    //
    // { traceId: 1399, spanId: 1400, parentId: 0,    operation: localhost:8010/getNested?nestDepth=4,    { localhost:8010/getNested?nestDepth=4, server } }
    //
    // { traceId: 1399, spanId: 1402, parentId: 1400, operation: localhost:8030/getNested?nestDepth=3,    { localhost:8030/getNested?nestDepth=3, client } }
    // { traceId: 1399, spanId: 3,    parentId: 1402, operation: localhost:8030/getNested?nestDepth=3,    { localhost:8030/getNested?nestDepth=3, server } }
    //
    // { traceId: 1399, spanId: 6,    parentId: 3,    operation: localhost:8010/getNested?nestDepth=2,    { localhost:8010/getNested?nestDepth=2, client } }
    // { traceId: 1399, spanId: 1403, parentId: 6,    operation: localhost:8010/getNested?nestDepth=2,    { localhost:8010/getNested?nestDepth=2, server } }
    //
    // { traceId: 1399, spanId: 1409, parentId: 1403, operation: localhost:8030/getNested?nestDepth=1,    { localhost:8030/getNested?nestDepth=1, client } }
    // { traceId: 1399, spanId: 12,   parentId: 1409, operation: localhost:8030/getNested?nestDepth=1,    { localhost:8030/getNested?nestDepth=1, server } }
    //
    // { traceId: 1399, spanId: 14,   parentId: 12,   operation: localhost:8010/getDelayed?delay=2 [ 2 ], { localhost:8010/getDelayed?delay=2 [ 2 ], client } }
    // { traceId: 1399, spanId: 1413, parentId: 14,   operation: localhost:8010/getDelayed?delay=2 [ 2 ], { localhost:8010/getDelayed?delay=2 [ 2 ], server } }
    //
    // { traceId: 1399, spanId: 15,   parentId: 12,   operation: localhost:8010/getDelayed?delay=4 [ 4 ], { localhost:8010/getDelayed?delay=4 [ 4 ], client } }
    // { traceId: 1399, spanId: 1414, parentId: 15,   operation: localhost:8010/getDelayed?delay=4 [ 4 ], { localhost:8010/getDelayed?delay=4 [ 4 ], server } }
    //
    // { traceId: 1399, spanId: 16,   parentId: 12,   operation: localhost:8010/getDelayed?delay=6 [ 6 ], { localhost:8010/getDelayed?delay=6 [ 6 ], client } }
    // { traceId: 1399, spanId: 1415, parentId: 16,   operation: localhost:8010/getDelayed?delay=6 [ 6 ], { localhost:8010/getDelayed?delay=6 [ 6 ], server } }

    // Rules:
    //   Start on an even server with an even nesting depth:
    //
    //   getNested: even depth: (even server => server span, odd server -> client span)
    //              odd depth: (even server -> client span, odd server -> server span)
    //   getDelayed: (even server -> server span, odd server -> client span)

    public void validateDistributedSpan(FATSpan span, int serverNo) {
        boolean isEvenServer = ( serverNo == SERVER_EVEN );

        // *** Only server and client spans should be present.  (There should be no manual spans.) ***

        boolean isServerSpan = span.isSpanKind(FATSpanKind.SERVER); 
        boolean isClientSpan = span.isSpanKind(FATSpanKind.CLIENT);
        if ( !isServerSpan && !isClientSpan ) {
            Assert.assertTrue("Must be either server or client [ " + span + " ]", (isServerSpan || isClientSpan) );
        }

        String url = span.getTag(FATSpan.TAG_HTTP_URL);
        boolean foundUrl = (url != null);
        if ( !foundUrl ) {
            Assert.assertTrue("Found tag [ " + FATSpan.TAG_HTTP_URL + " ] on [ " + span + " ]", foundUrl);
        }

        @SuppressWarnings("null") // Can't be null, but the compiler doesn't know that the foundUrl assertion must always fail.
        boolean isNestedGet = url.contains(GET_NESTED_PATH);
        boolean isDelayedGet = url.contains(GET_DELAYED_PATH);

        // *** Only spans for nested get and delayed get should present. *** 

        if ( !isServerSpan && !isClientSpan ) {
            Assert.assertTrue("Must be either [ " + GET_NESTED_PATH + " ] or [ " + GET_DELAYED_PATH + " ] [ " + span + " ]", (isNestedGet || isDelayedGet));
        }

        int nestDepth;
        if ( !isNestedGet ) {
            nestDepth = -1; // Unused
        } else {
            nestDepth = getNestDepth(url);
        }
        boolean isEvenDepth = (nestDepth % 2 == 0);

        boolean shouldBeServerSpan;
        if ( isNestedGet ) {
            // even servers receive (in, server) even nesting gets
            // odd servers receive (in, server), odd nesting gets
            // even servers send (out, client) odd nesting gets
            // odd servers send (out, client) even nested request
            shouldBeServerSpan = ( isEvenServer == isEvenDepth );
        } else {
            // (out, client) from nesting 1, which is the odd server
            // (in, server) only from the even server                
            shouldBeServerSpan = isEvenServer;
        }

        // *** The span kind must be correct for the server which generated it. ***

        if ( shouldBeServerSpan != isServerSpan ) {
            Assert.assertEquals(
                "Expecting [ " + (shouldBeServerSpan ? FATSpanKind.SERVER : FATSpanKind.CLIENT) + " ] for [ " + span + " ]",
                Boolean.valueOf(shouldBeServerSpan),
                Boolean.valueOf(isServerSpan));
        }
    }

    public static final String NEST_DEPTH_PREFIX = "nestDepth=";
    
    public int getNestDepth(String url) {
        // "http://localhost:8010/serviceApp/rest/testService/getNested?nestDepth=4"
        //   "&async=true"
        //   "&port=8010&response=nested+%5B+4+%5D+async+%5B+true+%5D&host=localhost&contextRoot=serviceApp"

        // *** A nested get must have a depth depth encoded in the request URL. ***

        int depthStart = url.indexOf(NEST_DEPTH_PREFIX);
        if ( depthStart == -1 ) {
            assertLtEq("Nested depth attribute location in [ " + url + " ]", 0, depthStart);
        }
        depthStart += NEST_DEPTH_PREFIX.length();

        int depthEnd = url.indexOf("&", depthStart);
        if ( depthEnd == -1 ) {
            assertLt("Nested depth attribute location in [ " + url + " ]", depthStart, depthEnd);
        }

        String nestText = url.substring(depthStart, depthEnd);

        return Integer.parseInt(nestText);
    }
    
    public void verifySpinners(List<FATSpan> completedSpans) throws Exception {
        String methodName = "verifySpinners";

        // *** Each plan entry must have a collection of spans. ***

        Map<Integer, SpansForPlan> splitSpans = splitSpans(completedSpans);
        assertEq("Number of span buckets", SPIN_TEST_PLAN.length, splitSpans.size());

        // *** Verify the completed spans for each entry of the span plan. ***

        for ( Map.Entry<Integer, SpansForPlan> spansEntry : splitSpans.entrySet() ) {
            Integer planNoInt = spansEntry.getKey(); 
            int planNo = planNoInt.intValue();
            SpansForPlan spansForPlan = spansEntry.getValue();

            info(methodName, "Verify plan", planNoInt);

            // *** Each collected plan number must be in the test plan range. ***

            assertLtEq("Plan number", 0, planNo);
            assertLt("Plan number", planNo, SPIN_TEST_PLAN.length);

            int expectedTraceIds = SPIN_TEST_PLAN[planNo];
            info(methodName, "Plan completed trace IDs", Integer.valueOf(expectedTraceIds));

            Map<String, List<FATSpan>> spansForAllTraceIds =
                spansForPlan.getSpansForAllTraceIds();

            int completedTraceIds = spansForAllTraceIds.size();
            
            // *** One trace ID must be recorded for each request made for this test plan entry. ***

            assertEq("Completed trace IDs", expectedTraceIds, completedTraceIds); 

            for ( List<FATSpan> spansForTraceId : spansForAllTraceIds.values() ) {

                // *** Each of the span collections for a single trace ID must have exactly one root span. ***

                verifyContiguousSpans( SERVER_EVEN, spansForTraceId, spansForTraceId.size() );

                /// *** Each of the span collections for a single trace ID must be a nested 4 spans collection. ***

                verifyNestedSpans4(SERVER_EVEN, spansForTraceId);
            }
        }
    }

    /**
     * <p>Utility class for partitioning completed spans.</p>
     * 
     * <p>Each instance will contain the completed spans for a specific
     * test plan number.</p>
     * 
     * <p>Each instance partitions its spans into a separate list based
     * on the trace IDs of the spans.</p>
     */
    public static class SpansForPlan {
        public SpansForPlan(int planNo) {
            this.planNo = planNo;

            this.spansForAllTraceIds =
                new HashMap<String, List<FATSpan>>();
        }

        //

        private final int planNo;

        public int getPlanNo() {
            return planNo;
        }

        //

        private final Map<String, List<FATSpan>> spansForAllTraceIds;

        public Map<String, List<FATSpan>> getSpansForAllTraceIds() {
            return spansForAllTraceIds;
        }

        //

        public static final boolean DO_FORCE = true;
        public static final boolean DO_NOT_FORCE = false;

        /**
         * <p>Add a span to this completed span bucket.</p>
         * 
         * <p>Locate the list for the thread ID of the span, and add the
         * span to that list.</p>
         * 
         * <p>If no list is present for the thread ID of the span, either,
         * create a new list and add the span to that new list, or, do not
         * add the span.</p>
         * 
         * @param completedSpan The span to add to this span bucket.
         * @param force Control parameter: Tell if a list is to be created for
         *     the span if none already exists for the trace ID of the span.
         * @return True or false according to whether the span was added.
         *     Always true if the force parameter is true.
         */
        public boolean add(FATSpan completedSpan, boolean force) {
            String traceId = completedSpan.getTraceId();
            List<FATSpan> spansForTraceId =
                spansForAllTraceIds.get(traceId);

            if ( spansForTraceId == null ) {
                if ( !force ) {
                    return false;
                } else {
                    spansForTraceId = new ArrayList<FATSpan>();
                    spansForAllTraceIds.put(traceId, spansForTraceId);
                }
            }

            spansForTraceId.add(completedSpan);
            return true;
        }
    }

    /**
     * <p>Partition completed spans according to their plan number and their thread ID.</p>
     * 
     * <p>The response text which is is provided to the nested request contains the plan number.
     * The response text is encoded in the nested request URL.  Completed requests for the nested
     * request are recognizable by the encoded plan text.</p>
     * 
     * <p>Partition in two layers: First by plan number; second by thread ID.</p>
     * 
     * @param completedSpans The spans which are to be partitioned.
     * 
     * @return The spans partitions by plan number and by thread ID.
     */
    public Map<Integer, SpansForPlan> splitSpans(List<FATSpan> completedSpans) {
        String methodName = "splitSpans";

        Map<Integer, SpansForPlan> spansForAllPlans =
            new HashMap<Integer, SpansForPlan>();
        List<FATSpan> unknownPlanSpans =
            new ArrayList<FATSpan>();

        // Add each span into a bucket based on its plan ID.

        int numUnknownPlanSpans = 0;
        int numKnownPlanSpans = 0;
        int numUniquePlanSpans = 0;

        for ( FATSpan completedSpan : completedSpans ) {
            int planNo = getPlanNumber(completedSpan);
            if ( planNo == UNKNOWN_PLAN_NO ) {
                unknownPlanSpans.add(completedSpan);
                numUnknownPlanSpans++;

            } else {
                Integer planNoInt = Integer.valueOf(planNo);

                SpansForPlan spansForPlan = spansForAllPlans.get(planNoInt);
                if ( spansForPlan == null ) {
                    spansForPlan = new SpansForPlan(planNo);
                    spansForAllPlans.put(planNoInt, spansForPlan);
                    numUniquePlanSpans++;
                }

                spansForPlan.add(completedSpan, SpansForPlan.DO_FORCE);
                numKnownPlanSpans++;
            }
        }

        info(methodName, "Unknown Plan Spans", Integer.valueOf(numUnknownPlanSpans));
        info(methodName, "Known Plan Spans", Integer.valueOf(numKnownPlanSpans));
        info(methodName, "Unique Plan Spans", Integer.valueOf(numUniquePlanSpans));

        // If the span does not have a URL, it may be associated with
        // a span for an initial request, which must have a URL.
        //
        // We allow some spans to not be associated with a plan.

        int numOrphanSpans = 0;
        
        for ( FATSpan completedSpan : unknownPlanSpans ) {
            boolean orphaned = true;
            for ( SpansForPlan spansForPlan : spansForAllPlans.values() ) {
                if ( spansForPlan.add(completedSpan, SpansForPlan.DO_NOT_FORCE) ) {
                    orphaned = false;
                    break;
                }
            }
            if ( orphaned ) {
                numOrphanSpans++;
            }
        }

        info(methodName, "Orphan Plan Spans", Integer.valueOf(numOrphanSpans));

        return spansForAllPlans;
    }

    public static final int UNKNOWN_PLAN_NO = -1;

    public static final String PLAN_PREFIX = "plan+%5B+";
    public static final String PLAN_SUFFIX = "+%5D";

    public int getPlanNumber(FATSpan completedSpan) {
        String methodName = "getPlanNumber";

        String spanUrl = completedSpan.getTag(FATSpan.TAG_HTTP_URL);
        if ( spanUrl == null ) {
            return UNKNOWN_PLAN_NO;
        }

        // Have to take into account URL encoding:
        // The response text "plan [ 3 ]" encodes as "plan+%5B+3+%5D".
        // For example:
        // response=nest+%5B+4+%5D+local+%5B+35+%5D+global+%5B+85+%5D+plan+%5B+3+%5D

        int planStart = spanUrl.indexOf(PLAN_PREFIX);
        if ( planStart == -1 ) {
            return UNKNOWN_PLAN_NO;
        } else {
            planStart += PLAN_PREFIX.length();
        }

        int planEnd = spanUrl.indexOf(PLAN_SUFFIX, planStart);
        if ( planEnd == -1 ) {
            return UNKNOWN_PLAN_NO;
        }
        String planNoText = spanUrl.substring(planStart,  planEnd);
        int planNo;

        try {
            planNo = Integer.parseInt(planNoText); // throws NumberFormatException
        } catch ( NumberFormatException e ) {
            info(methodName,
                "Failed to parse plan text [ " + planNoText + " ]" +
                " from URL [ " + spanUrl + " ]" +
                "(" + e.getMessage() + ")");
            throw e;
        }

        return planNo;
    }
}
