package com.ibm.ws.testing.opentracing.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@Mode(TestMode.FULL)
public class TestSpanUtils {
    private static List<FATUtilsSpans.CompletedSpan> SPANS;

    static {
        FATUtilsSpans.CompletedSpan span0_noTags = new FATUtilsSpans.CompletedSpan(
            "traceId0", "parentId0", "spanId0",
            "operation1",
            0L, 1L, 1L,
            null);

        Map<String, Object> oneTag = new HashMap<String, Object>(1);
        oneTag.put("tag10", "value10");
        FATUtilsSpans.CompletedSpan span1_oneTag = new FATUtilsSpans.CompletedSpan(
            "traceId1", "parentId1", "spanId1",
            "operation1",
            10L, 11L, 1L,
            oneTag);

        Map<String, Object> twoTags = new HashMap<String, Object>(2);
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

    @Test
    public void testNoTags() throws Exception {
        validateSpan(SPANS.get(0));
        // 'validateSpan' throws IOException
    }

    @Test
    public void testOneTag() throws Exception {
        validateSpan(SPANS.get(1));
        // 'validateSpan' throws IOException
    }

    @Test
    public void testMultipleTags() throws Exception {
        validateSpan(SPANS.get(2));
        // 'validateSpan' throws IOException
    }

    //

    @Test
    public void testSpans0() throws Exception {
        FATUtilsSpans.CompletedSpan[] noSpans =
            new FATUtilsSpans.CompletedSpan[0];
        validateSpans(noSpans); // throws IOException
    }

    @Test
    public void testSpans1() throws Exception {
        FATUtilsSpans.CompletedSpan[] oneSpan =
            new FATUtilsSpans.CompletedSpan[] { SPANS.get(0) };
        validateSpans(oneSpan); // throws IOException
    }

    @Test
    public void testSpans3() throws Exception {
        FATUtilsSpans.CompletedSpan[] threeSpans =
            new FATUtilsSpans.CompletedSpan[] { SPANS.get(0), SPANS.get(1), SPANS.get(2) };
        validateSpans(threeSpans); // throws IOException
    }

    //

    public void validateSpan(FATUtilsSpans.CompletedSpan span) throws IOException {
        String spanText = span.toString();
        FATUtilsSpans.CompletedSpan parsedSpan = FATUtilsSpans.parseSpan(spanText); // throws IOException
        Assert.assertTrue("Spans are equal", span.equals(parsedSpan));
    }

    public void validateSpans(FATUtilsSpans.CompletedSpan[] spans) throws IOException {
        String spansText = toString(spans);
        List<FATUtilsSpans.CompletedSpan> parsedSpans =
            FATUtilsSpans.parseSpans(spansText); // throws IOException
        validateSpans(parsedSpans, spans);
    }

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
