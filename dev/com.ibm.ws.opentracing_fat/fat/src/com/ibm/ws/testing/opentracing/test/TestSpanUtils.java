package com.ibm.ws.testing.opentracing.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * <p>Unit test of the completed span parsing code.</p>
 *
 * <p>These tests use completed spans as test data.  The tests
 * generate JSON formatted print strings, following the format
 * used by the open tracing mock tracer, then parse and marshall
 * new completed spans, then verify that the marshalled spans are
 * the same as the initial spans.</p>
 *
 * <p>If the JSON print string of the mock tracer completed spans
 * changes, these tests, along with the span utilities in
 * {@link FATUtilsSpans} will need to be updated to match.</p>
 */
@Mode(TestMode.FULL)
@MinimumJavaLevel(javaLevel = 8)
public class TestSpanUtils {
    /**
     * <p>Completed spans for span print string and parse verification.</p>
     *
     * <p>Three completed spans are generated as test data: A span with no
     * tags, a span with exactly one tag, and a span with exactly two tags.</p>
     */
    private static List<FATUtilsSpans.CompletedSpan> SPANS;

    static {
        FATUtilsSpans.CompletedSpan span0_noTags = new FATUtilsSpans.CompletedSpan(
            "traceId0", "parentId0", "spanId0",
            "operation1",
            0L, 1L, 1L,
            null);

        Map<String, String> oneTag = new HashMap<String, String>(1);
        oneTag.put("tag10", "value10");
        FATUtilsSpans.CompletedSpan span1_oneTag = new FATUtilsSpans.CompletedSpan(
            "traceId1", "parentId1", "spanId1",
            "operation1",
            10L, 11L, 1L,
            oneTag);

        Map<String, String> twoTags = new HashMap<String, String>(2);
        twoTags.put("tag20", "value20");
        twoTags.put("tag21", "value21");
        FATUtilsSpans.CompletedSpan span2_twoTags = new FATUtilsSpans.CompletedSpan(
            "traceId2", "parentId2", "spanId2",
            "operation2",
            20L, 21L, 1L,
            twoTags);

        SPANS = new ArrayList<FATUtilsSpans.CompletedSpan>(3);
        SPANS.add(span0_noTags);
        SPANS.add(span1_oneTag);
        SPANS.add(span2_twoTags);
    }

    /**
     * <p>Standard validation: Validate parsing a span which has no tags.</p>
     *
     * @throws Exception Thrown in case of a test failure.
     */
    @Test
    public void testNoTags() throws Exception {
        validateSpan(SPANS.get(0));
        // 'validateSpan' throws IOException
    }

    /**
     * <p>Standard validation: Validate parsing a span which has one tag.</p>
     *
     * @throws Exception Thrown in case of a test failure.
     */
    @Test
    public void testOneTag() throws Exception {
        validateSpan(SPANS.get(1));
        // 'validateSpan' throws IOException
    }

    /**
     * <p>Standard validation: Validate parsing a span which
     * has more than one tag.</p>
     *
     * @throws Exception Thrown in case of a test failure.
     */
    @Test
    public void testMultipleTags() throws Exception {
        validateSpan(SPANS.get(2));
        // 'validateSpan' throws IOException
    }

    //

    /**
     * <p>Standard validation: Validate parsing the print string
     * for an empty collection of spans.</p>
     *
     * @throws Exception Thrown in case of a test failure.
     */
    @Test
    public void testSpans0() throws Exception {
        FATUtilsSpans.CompletedSpan[] noSpans =
            new FATUtilsSpans.CompletedSpan[0];
        validateSpans(noSpans); // throws IOException
    }

    /**
     * <p>Standard validation: Validate parsing the print string
     * for a singleton collection of spans.</p>
     *
     * @throws Exception Thrown in case of a test failure.
     */
    @Test
    public void testSpans1() throws Exception {
        FATUtilsSpans.CompletedSpan[] oneSpan =
            new FATUtilsSpans.CompletedSpan[] { SPANS.get(0) };
        validateSpans(oneSpan); // throws IOException
    }

    /**
     * <p>Standard validation: Validate parsing the print string
     * for an collection of three spans.</p>
     *
     * @throws Exception Thrown in case of a test failure.
     */
    @Test
    public void testSpans3() throws Exception {
        FATUtilsSpans.CompletedSpan[] threeSpans =
            new FATUtilsSpans.CompletedSpan[] { SPANS.get(0), SPANS.get(1), SPANS.get(2) };
        validateSpans(threeSpans); // throws IOException
    }

    //

    /**
     * <p>Span validation utility: Obtain the print string for a span, then marshall
     * that print string back into a span, then make sure the original span is the same
     * as the marshalled span.</p>
     *
     * @param span The span which is to be validated.
     *
     * @throws IOException Thrown in case of a failure to parse the print string
     * of the span.
     */
    public void validateSpan(FATUtilsSpans.CompletedSpan span) throws IOException {
        String spanText = span.toString();
        FATUtilsSpans.CompletedSpan parsedSpan = FATUtilsSpans.parseSpan(spanText); // throws IOException
        Assert.assertTrue("Spans are equal", span.equals(parsedSpan));
    }

    /**
     * <p>Span validation utility: Generate a print string for the collection of
     * spans, parse and marshall that print string back into a collection of spans,
     * then verify that the final and initial collections have the same spans.</p>
     *
     * @param spans The initial collection of spans which is to be validated.
     *
     * @throws IOException Thrown if the print string of the spans cannot be parsed.
     */
    public void validateSpans(FATUtilsSpans.CompletedSpan[] spans) throws IOException {
        String spansText = toString(spans);
        List<FATUtilsSpans.CompletedSpan> parsedSpans =
            FATUtilsSpans.parseSpans(spansText); // throws IOException
        validateSpans(parsedSpans, spans);
    }

    /**
     * <p>Span validation utility: Validate that a collection of final spans
     * matches a collection of initial spans.</p>
     *
     * <p>The collections of spans must have the same number of elements.
     * Elements at the same offset in the two collections must be the same.</p>
     *
     * @param finalSpans A collection of spans which is to be validated.
     * @param initialSpans A collection of spans which is to be validated.
     */
    private void validateSpans(
        List<FATUtilsSpans.CompletedSpan> finalSpans,
        FATUtilsSpans.CompletedSpan... initialSpans) {

        Assert.assertTrue(
            "Parsed spans count [ " + Integer.toString(finalSpans.size()) + " ]" +
                " matches initial spans count [ " + Integer.toString(initialSpans.length) + " ]",
            (finalSpans.size() == initialSpans.length) );

        for ( int spanNo = 0; spanNo < initialSpans.length; spanNo++ ) {
            FATUtilsSpans.CompletedSpan initialSpan = initialSpans[spanNo];
            FATUtilsSpans.CompletedSpan finalSpan = finalSpans.get(spanNo);

            Assert.assertTrue(
                "Span [ " + Integer.toString(spanNo) + " ]" +
                    "Initial [ " + initialSpan + " ]" +
                    " matches final [ " + finalSpan + " ]",
                (initialSpan.equals(finalSpan)));
        }
    }

    /**
     * <p>Generate a print string for a collection of spans.</p>
     *
     * <p>The format must match that which is obtained from the mock
     * tracer, which emits its state as a JSON formatted string which
     * for the array of spans which have been completed by the mock
     * tracer.</p>
     *
     * <p>The current implementation generates the text:</p>
     *
     * <pre>
     * { "completedSpans": [] }
     * </pre>
     *
     * <p>The print string will be exactly as above for an empty collection
     * of completed spans.  For a non-empty collection of completed spans,
     * the JSON print strings of the spans will be inserted between the square
     * braces as a comma delimited list.</p>
     *
     * @param spans The spans for which to generate a JSON formatted print string.
     *
     * @return The JSON formatted print string of the spans.
     */
    private String toString(FATUtilsSpans.CompletedSpan... spans) {
        StringBuffer result = new StringBuffer("{ ");

        result.append("\"completedSpans\": [");

        String elementPrefix = "\n  ";
        for ( FATUtilsSpans.CompletedSpan completedSpan : spans ) {
            result.append(elementPrefix);
            result.append( completedSpan.toString() );
            elementPrefix = ",\n  ";
        }

        result.append(" ]");

        result.append(" }");

        return result.toString();
    }
}
