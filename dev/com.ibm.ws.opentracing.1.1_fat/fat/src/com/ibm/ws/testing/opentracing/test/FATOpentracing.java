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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.MinimumJavaLevel;
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
 * <li>{@link FATOpentracing#testNested4ASync}</li>
 * <li>{@link FATOpentracing#testExcludes}</li>
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
@MinimumJavaLevel(javaLevel = 8)
public class FATOpentracing implements FATOpentracingConstants {
    private static final String FEATURE_NAME = "com.ibm.ws.opentracing.mock-0.31.mf";
    private static final String BUNDLE_NAME = "com.ibm.ws.opentracing.mock-0.31.jar";
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

    // OpenTrace FAT server ...

    private static LibertyServer server;
    private static final boolean usingMicroProfile = false;

    private static void setUpServer() throws Exception {
        server = LibertyServerFactory.getLibertyServer(OPENTRACING_FAT_SERVER1_NAME);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/features/", "features/" + FEATURE_NAME);
        server.copyFileToLibertyInstallRoot("usr/extension/lib/", "bundles/" + BUNDLE_NAME);
    }

    private static LibertyServer getServer() {
        return server;
    }

    private static String getHostName() {
        return server.getHostname();
    }

    private static int getPortNumber() {
        return server.getHttpDefaultPort();
    }

    private static void startServer() throws Exception {
        getServer().startServer(); // 'startServer' throws Exception
    }

    private static void stopServer() throws Exception {
        getServer().stopServer(); // 'stopServer' throws Exception
    }

    // Test setup ...
    //
    // TODO: Maybe this should be done by the FAT suite.

    @BeforeClass
    public static void setUp() throws Exception {
        setUpServer();

        WebArchive serviceWar = createServiceWar();
        exportToServer(serviceWar);
        startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
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

    private static void exportToServer(WebArchive serviceWar) throws Exception {
        ShrinkHelper.exportAppToServer( getServer(), serviceWar );
        // 'exportAppToServer' throws Exception
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
    public static final String[] GET_EXCLUDE_TEST_CONDITION = new String[] { GET_EXCLUDE_TEST_PATH };

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

    // Span verification ...

    /**
     * <p>Select a span from a completed spans collection.  Select based on a specified
     * span kind, and specified selection text.</p>
     *
     * <p>A subset of the completed spans collection is examined, working from the end
     * of the collection.</p>
     *
     * <p>Selection is current against the operation of the completed span.  See
     * {@link FATUtilsSpans.CompletedSpan#getOperation()}.  Additional selection
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
    public FATUtilsSpans.CompletedSpan findSpan(
        List<FATUtilsSpans.CompletedSpan> completedSpans, int tailSize,
        FATUtilsSpans.SpanKind spanKind, String... selectText) {

        String methodName = "findSpan";

        int numSpans = completedSpans.size();
        for ( int spanNo = numSpans - tailSize; spanNo < numSpans; spanNo++ ) {
            FATUtilsSpans.CompletedSpan completedSpan = completedSpans.get(spanNo);
            if ( !completedSpan.isSpanKind(spanKind) ) {
                continue;
            }

            String operation = completedSpan.getTag("http.url");
            
            // If operation is null, it's a manual span
            
            if (operation == null) {
                operation = completedSpan.getOperation();
            }

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
            FATUtilsSpans.CompletedSpan completedSpan = completedSpans.get(spanNo);
            info(methodName, "  [ " + Integer.toString(spanNo) + " ] [ " + completedSpan + " ]");
        }

        return null;
    }
    
    public void verifyContiguousSpans(
                                      List<FATUtilsSpans.CompletedSpan> completedSpans,
                                      int expectedSpanCount) {
        verifyContiguousSpans(completedSpans, expectedSpanCount, 1);
    }

    /**
     * <p>Verify that a contiguous collection of spans is present as the tail
     * of a collection of completed spans.</p>
     *
     * <p>All of the completed spans of the tail of the spans collection must have
     * the same span ID (see {@link FATUtilsSpans.CompletedSpan#getTraceId()}, and
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
        List<FATUtilsSpans.CompletedSpan> completedSpans,
        int expectedSpanCount, int expectedRootSpanCount) {

        String methodName = "verifyContiguousSpans";

        // *** The expected number of spans must be present. ***

        int actualSpanCount = completedSpans.size();
        assertLtEq("Completed Spans", expectedSpanCount, actualSpanCount);

        int rootCount = 0;

        String initialTraceId = null;

        for ( int spanNo = actualSpanCount - expectedSpanCount; spanNo < actualSpanCount; spanNo++ ) {
            FATUtilsSpans.CompletedSpan nextSpan = completedSpans.get(spanNo);
            
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
                FATUtilsSpans.CompletedSpan nextSpan = completedSpans.get(spanNo);
                info(methodName,
                    "Span [ " + Integer.toString(spanNo) + " ]" +
                    " SpanId [ " + nextSpan.getSpanId() + " ]" +
                    " ParentId [ " + nextSpan.getParentId() + " ]");
            }
        }

        // *** Exactly one root span must be present. ***

        assertEq("Root spans", Integer.valueOf(expectedRootSpanCount), Integer.valueOf(rootCount));
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
        public final FATUtilsSpans.SpanKind parentKind;
        public final String[] parentSelection;

        public final FATUtilsSpans.SpanKind childKind;
        public final String[] childSelection;

        public ParentCondition(
            FATUtilsSpans.SpanKind parentKind, String[] parentSelection,
            FATUtilsSpans.SpanKind childKind, String[] childSelection) {

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
     * @param completedSpans The overall collection of spans which is to
     *     be tested.
     * @param tailSize The number of spans to examine within the completed
     *     spans collection.
     * @param parentConditions Conditions to verify within the completed
     *     spans collection.
     */
    public void verifyParents(
            List<FATUtilsSpans.CompletedSpan> completedSpans, int tailSize,
            ParentCondition... parentConditions) {

        String methodName = "verifyParents";

        for ( ParentCondition parentCondition : parentConditions ) {
            // *** Verify that a child span was selected.  A child span must always be selected. ***

            FATUtilsSpans.CompletedSpan childSpan =
                findSpan(completedSpans, tailSize,
                         parentCondition.childKind,
                         parentCondition.childSelection);
            Assert.assertNotNull("Child span selection [ " + selectionText(parentCondition.childSelection) + " ]", childSpan);

            FATUtilsSpans.CompletedSpan parentSpan;
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
     * open tracing FAT service.</p>
     *
     * @param priorRequestPath The immediately preceding request made to the
     *     open tracing FAT service.  Used for to match this request within
     *     the overall flow of FAT service request.
     *
     * @return The list of completed spans obtained from the opening tracing FAT
     *     service.
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    public List<FATUtilsSpans.CompletedSpan> getCompletedSpans(String priorRequestPath) throws Exception {
        String methodName = "getCompletedSpans";

        String requestUrl = getRequestUrl(GET_TRACER_STATE_PATH);
        FATLogging.info(CLASS, methodName, "Request", priorRequestPath);

        List<String> responseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl); // throws Exception

        FATLogging.info(CLASS, methodName, "Response:");

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
        List<FATUtilsSpans.CompletedSpan> spans =
            FATUtilsSpans.parseSpans(responseText); // throws IOException
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
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    public void verifyTracerStateEvent() throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_IMMEDIATE_PATH);

        verifyTracerStateEvent(completedSpans);

        verifyContiguousSpans(completedSpans, 1);
    }

    public void verifyTracerStateEvent(List<FATUtilsSpans.CompletedSpan> completedSpans) throws Exception {
        verifyTracerStateEvent( completedSpans.get(completedSpans.size() - 1) );
    }

    public void verifyTracerStateEvent(FATUtilsSpans.CompletedSpan completedSpan) throws Exception {
        String requestUrl = getRequestUrl(GET_TRACER_STATE_PATH); // throws UnsupportedEncodingException

        // *** The completed span event must be a root span. ***

        assertEq("parent", FATUtilsSpans.NULL_PARENT_ID, completedSpan.getParentId());

        // *** The completed span event must be be for a get tracer state request. ***

        String operationName;
        if (usingMicroProfile) {
            operationName = "GET:com.ibm.ws.testing.opentracing.service.FATOpentracingService.getTracerState";
        } else {
            operationName = requestUrl;
        }
        assertEq("Operation", operationName, completedSpan.getOperation());

        // *** The completed span must have valid state and finish times. ***

        assertLtEq("Operation time", completedSpan.getStart(), completedSpan.getFinish());

        // *** The tags of the completed span must be as expected for a get tracer state request. ***

        verifyTags(completedSpan,
                FATUtilsSpans.TAG_HTTP_METHOD, "GET",
                FATUtilsSpans.TAG_HTTP_STATUS_CODE, "200",
                FATUtilsSpans.TAG_HTTP_URL, requestUrl,
                FATUtilsSpans.TAG_SPAN_KIND, "server");
    }

    public void verifyTags(FATUtilsSpans.CompletedSpan completedSpan, String... tagData) {
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testImmediate() throws Exception {
        verifyImmediateService();
        verifyImmediateSpans();
        verifyTracerStateEvent();
    }

    public void verifyImmediateService() throws Exception {
        String methodName = "verifyImmediateService";

        String responseText = "immediate";
        Map<String, Object> requestParms = new HashMap<String, Object>();
        requestParms.put(RESPONSE_PARAM_NAME, responseText);

        String requestUrl = getRequestUrl(GET_IMMEDIATE_PATH, requestParms);

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

        assertEq("Response text",
                 responseText, actualResponseLines.get(0));
    }

    public void verifyImmediateSpans() throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_IMMEDIATE_PATH);

        int tailSize = 1;

        // *** The immediate request is expected to generate exactly one completed span. ***

        verifyContiguousSpans(completedSpans, tailSize);

        // *** The single completed span must be a root span, and must be for the immediate get. ***

        ParentCondition getImmediateCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, GET_IMMEDIATE_CONDITION);
        verifyParents(completedSpans, tailSize, getImmediateCondition);
    }

    /**
     * <p>Test of a simple request which contains a single manually generated span..</p>
     *
     * <p>Perform  the request; verify the results of that request.  Obtain
     * complete spans for the request, and verify that these are as expected.
     * Verify that the completed spans request has valid data.</p>
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testManual() throws Exception {
        verifyManualService();
        verifyManualSpans();
        verifyTracerStateEvent();
    }

    public void verifyManualService() throws Exception {
        String methodName = "verifyManualService";

        String responseText = "manualResponse";
        Map<String, Object> requestParms = new HashMap<String, Object>();
        requestParms.put(RESPONSE_PARAM_NAME, responseText);

        String requestUrl = getRequestUrl(GET_MANUAL_PATH, requestParms);

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

        assertEq("Response text",
                 responseText, actualResponseLines.get(0));
    }

    public void verifyManualSpans() throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_MANUAL_PATH);

        int tailSize = 2;

        // *** The manual request is expected to generate exactly two completed spans. ***

        verifyContiguousSpans(completedSpans, tailSize);

        ParentCondition getManualCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, GET_MANUAL_CONDITION);
        ParentCondition manualCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, GET_MANUAL_CONDITION,
                FATUtilsSpans.SpanKind.MANUAL, MANUAL_CONDITION);

        verifyParents(
            completedSpans, tailSize,
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testDelayed2() throws Exception {
        verifyDelayedService(2);
        verifyDelayedSpans(2);
        verifyTracerStateEvent();
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testDelayed4() throws Exception {
        verifyDelayedService(4);
        verifyDelayedSpans(4);
        verifyTracerStateEvent();
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testDelayed6() throws Exception {
        verifyDelayedService(6);
        verifyDelayedSpans(6);
        verifyTracerStateEvent();
    }

    public void verifyDelayedService(int delaySec) throws Exception {
        String methodName = "verifyDelayedService";

        String responseText = "delayed [ " + Integer.toString(delaySec) + " ]";
        Map<String, Object> requestParms = new HashMap<String, Object>();
        requestParms.put(RESPONSE_PARAM_NAME, responseText);
        requestParms.put(DELAY_PARAM_NAME, Integer.valueOf(delaySec));

        String requestUrl = getRequestUrl(GET_DELAYED_PATH, requestParms);

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

        assertEq("Response text",
                 responseText, actualResponseLines.get(0));
   }

    public void verifyDelayedSpans(int delaySec) throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_DELAYED_PATH);

        int tailSize = 1;

        // *** The delayed request is expected to generate exactly one completed span. ***

        verifyContiguousSpans(completedSpans, tailSize);

        ParentCondition getDelayedCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, getDelayedCondition(delaySec));
        verifyParents(completedSpans, tailSize, getDelayedCondition);
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testNested0() throws Exception {
        verifyNestedService0();
        verifyNestedSpans0();
        verifyTracerStateEvent();
    }

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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testNested1Sync() throws Exception {
        verifyNestedService1(IS_SYNC);
        verifyNestedSpans1();
        verifyTracerStateEvent();
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testNested2Sync() throws Exception {
        verifyNestedService2(IS_SYNC);
        verifyNestedSpans2();
        verifyTracerStateEvent();
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testNested4Sync() throws Exception {
        verifyNestedService4(IS_SYNC);
        verifyNestedSpans4();
        verifyTracerStateEvent();
    }

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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testNested1Async() throws Exception {
        verifyNestedService1(IS_ASYNC);
        verifyNestedSpans1();
        verifyTracerStateEvent();
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testNested2Async() throws Exception {
        verifyNestedService2(IS_ASYNC);
        verifyNestedSpans2();
        verifyTracerStateEvent();
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
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    @Test
    public void testNested4Async() throws Exception {
        verifyNestedService4(IS_ASYNC);
        verifyNestedSpans4();
        verifyTracerStateEvent();
    }

    public static final boolean IS_ASYNC = true;
    public static final boolean IS_SYNC = false;

    public Map<String, Object> getNestedParms(int nestDepth, boolean async, String responseText) {
        Map<String, Object> nestedParms = new HashMap<String, Object>();
        nestedParms.put(RESPONSE_PARAM_NAME, responseText);
        nestedParms.put(NEST_DEPTH_PARAM_NAME, Integer.valueOf(nestDepth));
        nestedParms.put(ASYNC_PARAM_NAME, Boolean.valueOf(async));

        nestedParms.put(HOST_PARAM_NAME, getHostName());
        nestedParms.put(PORT_PARAM_NAME, Integer.valueOf(getPortNumber()));
        nestedParms.put(CONTEXT_ROOT_PARAM_NAME, SERVICE_CONTEXT_ROOT);
        return nestedParms;
    }

    public void verifyNestedService0() throws Exception {
        String methodName = "verifyNestedService0";

        int nestDepth = 0;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, IS_SYNC, responseText);

        String requestUrl = getRequestUrl(GET_NESTED_PATH, requestParms);

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

        assertEq("Response text",
                 responseText, actualResponseLines.get(0));
    }

    public void verifyNestedSpans0() throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_NESTED_PATH);

        int tailSize = 1;

        // *** The delayed request is expected to generate exactly one completed span. ***

        verifyContiguousSpans(completedSpans, tailSize);

        ParentCondition getNestedCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, getNestedCondition(0));
        verifyParents(completedSpans, tailSize, getNestedCondition);
    }

    public void verifyNestedService1(boolean async) throws Exception {
        String methodName = "verifyNestedService1";

        int nestDepth = 1;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ] async [ " + Boolean.toString(async) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, async, responseText);

        String requestUrl = getRequestUrl(GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // TODO: Verify the lines in detail.
    }

    public void verifyNestedSpans1() throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_NESTED_PATH);

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

        verifyContiguousSpans(completedSpans, tailSize);

        // *** A root span for the initial get nested request. ***

        String[] getNested1Text = getNestedCondition(1);
        ParentCondition getNested1Condition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, getNested1Text);

        String[] getDelayed2Text = getDelayedCondition(2);
        String[] getDelayed4Text = getDelayedCondition(4);
        String[] getDelayed6Text = getDelayedCondition(6);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the two second delay request. ***

        ParentCondition getDelay2ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text);
        ParentCondition getDelay2ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed2Text);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the four second delay request. ***

        ParentCondition getDelay4ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text);
        ParentCondition getDelay4ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed4Text);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the six second delay request. ***

        ParentCondition getDelay6ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text);
        ParentCondition getDelay6ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed6Text);

        verifyParents(
            completedSpans, tailSize,
            getNested1Condition,
            getDelay2ClientCondition, getDelay2ContainerCondition,
            getDelay4ClientCondition, getDelay4ContainerCondition,
            getDelay6ClientCondition, getDelay6ContainerCondition);
    }

    public void verifyNestedService2(boolean async) throws Exception {
        String methodName = "verifyNestedService2";

        int nestDepth = 2;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ] async [ " + Boolean.toString(async) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, async, responseText);

        String requestUrl = getRequestUrl(GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // TODO: Verify the lines in detail.
    }

    public void verifyNestedSpans2() throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_NESTED_PATH);

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

        verifyContiguousSpans(completedSpans, tailSize);

        // *** A root span for the initial depth 2 get nested request. ***

        String[] getNested2Text = getNestedCondition(2);
        ParentCondition getNested2Condition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, getNested2Text);

        // *** A pair of completed spans for the call from nest level 2 ***
        // *** request to the nest level 1 request. ***

        String[] getNested1Text = getNestedCondition(1);
        ParentCondition getNested1ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested2Text,
                FATUtilsSpans.SpanKind.CLIENT, getNested1Text);
        ParentCondition getNested1ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getNested1Text,
                FATUtilsSpans.SpanKind.SERVER, getNested1Text);

        String[] getDelayed2Text = getDelayedCondition(2);
        String[] getDelayed4Text = getDelayedCondition(4);
        String[] getDelayed6Text = getDelayedCondition(6);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the two second delay request. ***

        ParentCondition getDelay2ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text);
        ParentCondition getDelay2ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed2Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the four second delay request. ***

        ParentCondition getDelay4ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text);
        ParentCondition getDelay4ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed4Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the six second delay request. ***

        ParentCondition getDelay6ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text);
        ParentCondition getDelay6ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed6Text);

        verifyParents(
            completedSpans, tailSize,
            getNested2Condition,
            getNested1ClientCondition, getNested1ContainerCondition,
            getDelay2ClientCondition, getDelay2ContainerCondition,
            getDelay4ClientCondition, getDelay4ContainerCondition,
            getDelay6ClientCondition, getDelay6ContainerCondition);
    }

    public void verifyNestedService4(boolean async) throws Exception {
        String methodName = "verifyNestedService4";

        int nestDepth = 4;
        String responseText = "nested [ " + Integer.toString(nestDepth) + " ] async [ " + Boolean.toString(async) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, async, responseText);

        String requestUrl = getRequestUrl(GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // TODO: Verify the lines in detail.
    }

    public void verifyNestedSpans4() throws Exception {
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_NESTED_PATH);

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
        // *** completed span (per the preceding comment. ***

        verifyContiguousSpans(completedSpans, tailSize);

        // *** The completed spans of the nested request have a complex relationship, ***
        // *** as described by the preceding comment. ***

        // *** A root span for the initial depth 2 get nested request. ***

        String[] getNested4Text = getNestedCondition(4);
        ParentCondition getNested4Condition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, getNested4Text);

        // *** A pair of completed spans for the call from nest level 4 ***
        // *** request to the nest level 3 request. ***

        String[] getNested3Text = getNestedCondition(3);
        ParentCondition getNested3ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested4Text,
                FATUtilsSpans.SpanKind.CLIENT, getNested3Text);
        ParentCondition getNested3ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getNested3Text,
                FATUtilsSpans.SpanKind.SERVER, getNested3Text);

        // *** A pair of completed spans for the call from nest level 3 ***
        // *** request to the nest level 2 request. ***

        String[] getNested2Text = getNestedCondition(2);
        ParentCondition getNested2ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested3Text,
                FATUtilsSpans.SpanKind.CLIENT, getNested2Text);
        ParentCondition getNested2ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getNested2Text,
                FATUtilsSpans.SpanKind.SERVER, getNested2Text);

        // *** A pair of completed spans for the call from nest level 2 ***
        // *** request to the nest level 1 request. ***

        String[] getNested1Text = getNestedCondition(1);
        ParentCondition getNested1ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested2Text,
                FATUtilsSpans.SpanKind.CLIENT, getNested1Text);
        ParentCondition getNested1ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getNested1Text,
                FATUtilsSpans.SpanKind.SERVER, getNested1Text);

        String[] getDelayed2Text = getDelayedCondition(2);
        String[] getDelayed4Text = getDelayedCondition(4);
        String[] getDelayed6Text = getDelayedCondition(6);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the two second delay request. ***

        ParentCondition getDelay2ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text);
        ParentCondition getDelay2ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed2Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the four second delay request. ***

        ParentCondition getDelay4ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text);
        ParentCondition getDelay4ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed4Text);

        // *** A pair of completed spans for the call from nest level 1 ***
        // *** request to the six second delay request. ***

        ParentCondition getDelay6ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text);
        ParentCondition getDelay6ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed6Text);

        verifyParents(
            completedSpans, tailSize,
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

    private String getRequestUrl(String servicePath)
        throws UnsupportedEncodingException {
        return getRequestUrl(servicePath, null); // throws UnsupportedEncodingException
    }

    private String getRequestUrl(
        String endpointPath,
        Map<String, Object> serviceParameters) throws UnsupportedEncodingException {

        String requestPath = FATUtilsServer.getRequestPath(
            SERVICE_CONTEXT_ROOT,
            APP_PATH, SERVICE_PATH, endpointPath,
            serviceParameters);
        // throws UnsupportedEncodingException

        LibertyServer useServer = getServer();

        return FATUtilsServer.getRequestUrl(
            useServer.getHostname(),
            useServer.getHttpDefaultPort(),
            requestPath );
    }

    /**
     * Removed filter processing until microprofile spec for it is approved. Expect to add back in 1Q18 - smf
     * Disable the tests until we add the code back in.
     * 
     * A collection of tests of the exclude and include filters.
     *
     * There is a single endpoint, /excludeTest which takes a query parameter `response`
     * which is what's sent back to the client. We use this query parameter as a way
     * to test the various filters because filter matching occurs on the full URI,
     * including query parameters, so we can simply vary the parameter.
     * 
     * To test exclude filters, we call {@link FATOpentracing#testExcludedPath(String)}
     * which ensures that the number of completed spans is the same before and after
     * the call (taking into account that the call to get the completed spans itself counts).
     * 
     * To test include filters, we call {@link FATOpentracing#testIncludedPath(String)}
     * which ensures that the expected span is created.
     * 
     * To test that the filters correctly pass through spans besides those that are
     * excluded and that filters work with nested calls, we call
     * {@link FATOpentracing#testNestedExcludePath(String, int, boolean)} for both
     * an excluded span and an included span, and confirm the proper spans.
     * 
     * To understand the tests in full, find the filters configuration in the test
     * server.xml based on the query parameter passed to the test*Path methods.
     *
     * @throws Exception Thrown if the service request failed, or if the completed
     *     spans could not be marshalled from the text obtained from the FAT service.
     */
    // @Test
    public void testExcludes() throws Exception {
        testExcludedPath("simple");
        testExcludedPath("wildcardTest");
        testExcludedPath("absoluteUri");
        testExcludedPath("regexTest123");
        testIncludedPath("wildcardInclude");
        testNestedExcludePath("nestedSuccess", 1, false);
        testNestedExcludePath("nestedExcludeTest1", 1, true);
        testExcludedPath("incomingExcluded");
        testIncludedPath("incomingIncluded");
    }

    private void testExcludedPath(String param) throws Exception, UnsupportedEncodingException {
        int initialCompletedSpansSize = getCompletedSpans(GET_EXCLUDE_TEST_PATH).size();

        sendRequest(GET_EXCLUDE_TEST_PATH, param);
        
        int newCompletedSpansCount = getCompletedSpans(GET_EXCLUDE_TEST_PATH).size();
        
        // Subtract one because getCompletedSpans itself creates another span for /serviceApp/rest/testService/getTracerState
        assertEq("Completed Spans Count", initialCompletedSpansSize, newCompletedSpansCount - 1);
    }

    private void testIncludedPath(String param) throws Exception, UnsupportedEncodingException {
        sendRequest(GET_EXCLUDE_TEST_PATH, param);
        
        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_EXCLUDE_TEST_PATH);

        int tailSize = 1;

        // *** The included request is expected to generate exactly one completed span. ***

        verifyContiguousSpans(completedSpans, tailSize);

        // *** The single completed span must be a root span, and must be for the get. ***

        ParentCondition getExcludeTestCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, GET_EXCLUDE_TEST_CONDITION);
        verifyParents(completedSpans, tailSize, getExcludeTestCondition);
        
        completedSpans = getCompletedSpans(GET_IMMEDIATE_PATH);
        
        verifyTracerStateEvent(completedSpans);

        verifyContiguousSpans(completedSpans, 1);
    }

    private void sendRequest(String path, String responseText) throws UnsupportedEncodingException, Exception {
        String methodName = "sendRequest";
        Map<String, Object> requestParms = new HashMap<String, Object>();
        requestParms.put(RESPONSE_PARAM_NAME, responseText);

        String requestUrl = getRequestUrl(path, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        // throws Exception
        info(methodName, "Actual Response", actualResponseLines);

        // *** The excludeBoth request is expected to have exactly one line of text. ***

        assertEq("Line count",
                 Integer.valueOf(1), Integer.valueOf(actualResponseLines.size()));

        // *** And is expected to have the response text as specified through the request parameter. ***

        assertEq("Response text",
                 responseText, actualResponseLines.get(0));
    }
    
    private void testNestedExcludePath(String param, int nestDepth, boolean excludingDelay4) throws Exception {
        String methodName = "testNestedExcludePath";

        String responseText = param + " nested [ " + Integer.toString(nestDepth) + " ]";

        Map<String, Object> requestParms = getNestedParms(nestDepth, false, responseText);

        String requestUrl = getRequestUrl(GET_NESTED_PATH, requestParms);

        info(methodName, "Request URL", requestUrl);
        info(methodName, "Expected Response", responseText);

        List<String> actualResponseLines =
            FATUtilsServer.gatherHttpRequest(FATUtilsServer.HttpRequestMethod.GET, requestUrl);
        info(methodName, "Actual Response", actualResponseLines);

        List<FATUtilsSpans.CompletedSpan> completedSpans = getCompletedSpans(GET_NESTED_PATH);

        int tailSize = excludingDelay4 ? 5 : 7;

        verifyContiguousSpans(completedSpans, tailSize);

        // *** A root span for the initial get nested request. ***

        String[] getNested1Text = getNestedCondition(1);
        ParentCondition getNested1Condition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, null,
                FATUtilsSpans.SpanKind.SERVER, getNested1Text);

        String[] getDelayed2Text = getDelayedCondition(2);
        String[] getDelayed4Text = getDelayedCondition(4);
        String[] getDelayed6Text = getDelayedCondition(6);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the two second delay request. ***

        ParentCondition getDelay2ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text);
        ParentCondition getDelay2ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed2Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed2Text);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the four second delay request. ***

        ParentCondition getDelay4ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text);
        ParentCondition getDelay4ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed4Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed4Text);

        // *** A pair of completed spans for the call from root get nested ***
        // *** request to the six second delay request. ***

        ParentCondition getDelay6ClientCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.SERVER, getNested1Text,
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text);
        ParentCondition getDelay6ContainerCondition = new ParentCondition(
                FATUtilsSpans.SpanKind.CLIENT, getDelayed6Text,
                FATUtilsSpans.SpanKind.SERVER, getDelayed6Text);

        if (excludingDelay4) {
            verifyParents(
                          completedSpans, tailSize,
                          getNested1Condition,
                          getDelay2ClientCondition, getDelay2ContainerCondition,
                          getDelay6ClientCondition, getDelay6ContainerCondition);
        } else {
            verifyParents(
                          completedSpans, tailSize,
                          getNested1Condition,
                          getDelay2ClientCondition, getDelay2ContainerCondition,
                          getDelay4ClientCondition, getDelay4ContainerCondition,
                          getDelay6ClientCondition, getDelay6ContainerCondition);
        }
        
        completedSpans = getCompletedSpans(GET_IMMEDIATE_PATH);

        verifyTracerStateEvent(completedSpans);

        verifyContiguousSpans(completedSpans, 1);
    }
}
