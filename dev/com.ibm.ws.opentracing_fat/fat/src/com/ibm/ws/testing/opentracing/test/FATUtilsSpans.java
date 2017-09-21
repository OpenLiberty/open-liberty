/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.testing.opentracing.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.JSONObject;

public class FATUtilsSpans {
    public static JSONArtifact jsonParse(String source) throws IOException {
        return jsonParse(source, 0, source.length());
    }

    public static JSONArtifact jsonParse(String source, int initialSourceOffset, int finalSourceOffset) throws IOException {
        return JSON.parse( new SubStringReader(source, initialSourceOffset, finalSourceOffset) ); // throws IOException
    }

    private static JSONObject asJSONObject(JSONArtifact jsonArtifact, String elementType) throws IOException {
        if ( jsonArtifact instanceof JSONArray ) {
            throw new IOException("Array received as [ " + elementType + " ] Span JSON text");
        } else if ( !(jsonArtifact instanceof JSONObject) ) {
            throw new IOException("Type [ " + jsonArtifact.getClass().getName() + " ] received when expecting [ " + JSONObject.class.getName() + " ] for [ " + elementType + " ] JSON text");
        } else {
            return (JSONObject) jsonArtifact;
        }
    }

    //

    // A note on JSONObject:
    //
    // A JSONObject is a subclass of HashMap, with the following conditions:
    //
    // Keys are always Strings
    // Values are always either Strings, Booleans, JSONObjects, JSONArrays, or Numbers.

    public static CompletedSpan parseSpan(String text) throws IOException {
        return parseSpan(text, 0, text.length() ); // throws IOException
    }

    public static CompletedSpan parseSpan(String source, int initialSourceOffset, int finalSourceOffset) throws IOException {
        JSONArtifact jsonArtifact = jsonParse(source, initialSourceOffset, finalSourceOffset); // throws IOException
        JSONObject jsonObject = asJSONObject( jsonArtifact, CompletedSpan.class.getSimpleName() ); // throws IOException

        @SuppressWarnings("unchecked")
        CompletedSpan completedSpan = createSpan(jsonObject);
        return completedSpan;
    }

    public static CompletedSpan createSpan(Map<String, ? extends Object> attributeData) {
        return new CompletedSpan(attributeData);
    }

    //

    public static List<CompletedSpan> parseSpans(String text) throws IOException {
        return parseSpans(text, 0, text.length() ); // throws IOException
    }

    @SuppressWarnings("unchecked")
    public static List<CompletedSpan> parseSpans(String text, int initialOffset, int finalOffset) throws IOException {
        JSONArtifact parsedSpans = jsonParse(text, initialOffset, finalOffset); // throws IOException
        JSONObject jsonObject = asJSONObject(parsedSpans, "completedSpans");
        JSONArray parsedSpansArray = (JSONArray) jsonObject.get("completedSpans");
        List<CompletedSpan> completedSpans = new ArrayList<CompletedSpan>( parsedSpansArray.size() );

        for ( JSONObject parsedSpan : (List<JSONObject>) parsedSpansArray ) {
            completedSpans.add( createSpan(parsedSpan) );
        }

        return completedSpans;
    }

    /**
     * <p>Span data as made available from
     * <code>com.ibm.ws.opentracing.mock.OpentracingMockTracer.toString</code>.
     * That creates a JSON formatted representation of completed spans.</p>
     *
     * <p>At the top:</p>
     *
     * <code>
     * { "completedSpans": [ completedSpanData, ... ] }
     * </code>
     *
     * (JSON uses '[' and ']' for array values.  JSON uses '{' and '}' for record
     * structures.  Numeric values are not quoted.  The representation of boolean
     * values is not clear.)
     *
     * <p>Then for each completed span:</p>
     *
     * <code>
     * { "traceId": "ID1", "parentId": "ID2", "spanId": "ID3", "operation": "name1",
     *   "start": "time1", "finish": "time2", "elapsed": "time3",
     *   "tags": { "tagKey1": "tagValue", "tagKey2": "tagValue2" } }
     * </code>
     *
     * <p>Key characters are '[', ']', '{', '}', '"', and ':".  White-space
     * within quoted regions is a part of the quoted value.  White-space in
     * other locations is not significant.</p>
     *
     * <p>White-space outside of quoted regions is not guaranteed to follow
     * a particular pattern.</p>
     */

    public static String toString(List<FATUtilsSpans.CompletedSpan> completedSpans) {
        StringBuilder result = new StringBuilder();

        result.append("{ ");
        result.append("\"completedSpans\": {");

        String prefix = " ";
        for ( FATUtilsSpans.CompletedSpan completedSpan : completedSpans ) {
            result.append(prefix);
            prefix = "\n ";
            result.append(completedSpan.toString());
        }
        result.append(" }");
        return result.toString();
    }

    //

    public static final String NULL_PARENT_ID = "0";

    //

    public static final String TAG_HTTP_URL = "http.url";
    public static final String TAG_HTTP_STATUS_CODE = "http.status_code";
    public static final String TAG_HTTP_METHOD = "http.method";

    public static final String TAG_SPAN_KIND = "span.kind";
    public static final String SPAN_KIND_SERVER = "server";
    public static final String SPAN_KIND_CLIENT = "client";

    public static enum SpanKind {
        SERVER(SPAN_KIND_SERVER),
        CLIENT(SPAN_KIND_CLIENT),
        MANUAL(null);

        private SpanKind(String tagValue) {
            this.tagValue = tagValue;
        }

        private final String tagValue;

        public String getTagValue() {
            return tagValue;
        }

        public boolean matches(String testTagValue) {
            if ( this.tagValue == null ) {
                return ( testTagValue == null );
            } else {
                return ( this.tagValue.equals(testTagValue) );
            }
        }
    }

    //

    public static final class CompletedSpan {
        // Correlation ID shared between spans descending from the same initial
        // request.
        private final String traceId;
        // The ID of the parent span; 0 if this is an initial request.
        private final String parentId;
        // The span's unique ID.  Never 0.
        private final String spanId;

        // Request URL, or the specified value for an explicitly created span.
        private final String operation;

        // Start time of the span, in MS.
        private final long start;
        // Finish time of the span, in MS.
        private final long finish;
        // Elapsed time of the span, in MS.
        private final long elapsed;

        // Span tags:
        // The Liberty implementation always stores:
        //   'span.kind': the request kind (server, client, or null for explicit spans),
        //   'span.url': the request URL
        //   'span.httpMethod': the request method
        //   'span.httpStatus': the request return code
        //   'span.error': true or false, telling if the request completed with an error
        private final Map<String, Object> tags;

        //

        public String getTraceId() {
            return traceId;
        }

        public String getParentId() {
            return parentId;
        }

        public boolean isRoot() {
            String useParentId = getParentId();
            return ( (useParentId != null) && useParentId.equals(NULL_PARENT_ID) );
        }

        public String getSpanId() {
            return spanId;
        }

        public String getOperation() {
            return operation;
        }

        public long getStart() {
            return start;
        }

        public long getFinish() {
            return finish;
        }

        public long getElapsed() {
            return elapsed;
        }

        public Map<String, ? extends Object> getTags() {
            return tags;
        }

        public Object getTag(String tag) {
            return getTags().get(tag);
        }

        public String getSpanKind() {
            return (String) getTags().get(TAG_SPAN_KIND);
        }

        public boolean isSpanKind(SpanKind spanKind) {
            return spanKind.matches( getSpanKind() );
        }

        //

        public Object getAttribute(String attributeName) {
            if ( attributeName == null ) {
                throw new IllegalArgumentException("Null field name not allowed");
            }

            if ( attributeName.equals("traceId") ) {
                return getTraceId();
            } else if ( attributeName.equals("parentId") ) {
                return getParentId();
            } else if ( attributeName.equals("getSpanId") ) {
                return getSpanId();
            } else if ( attributeName.equals("operation") ) {
                return getOperation();
            } else if ( attributeName.equals("start") ) {
                return Long.valueOf( getStart() );
            } else if ( attributeName.equals("finish") ) {
                return Long.valueOf( getFinish() );
            } else if ( attributeName.equals("elapsed") ) {
                return Long.valueOf( getElapsed() );
            } else if ( attributeName.equals("tags") ) {
                return getTags();

            } else {
                throw new IllegalArgumentException("Field name [ " + attributeName + " ] is not valid");
            }
        }

        //

        public CompletedSpan(
            String traceId, String parentId, String spanId,
            String operation,
            long startTime, long finishTime, long elapsedTime,
            Map<String, ? extends Object> tags) {

            this.traceId = traceId;
            this.parentId = parentId;
            this.spanId = spanId;
            this.operation = operation;
            this.start = startTime;
            this.finish = finishTime;
            this.elapsed = elapsedTime;

            if ( tags == null ) {
                this.tags = Collections.emptyMap();
            } else {
                this.tags = new HashMap<String, Object>(tags);
            }

            this.hashCode = computeHashCode();
        }

        //

        @SuppressWarnings("unchecked")
        public CompletedSpan(Map<String, ? extends Object> attributeData) {
            this( (String) attributeData.get("traceId"),
                  (String) attributeData.get("parentId"),
                  (String) attributeData.get("spanId"),
                  (String) attributeData.get("operation"),
                  ((Number) attributeData.get("start")).longValue(),
                  ((Number) attributeData.get("finish")).longValue(),
                  ((Number) attributeData.get("elapsed")).longValue(),
                  (Map<String, ? extends Object>) attributeData.get("tags") );
        }

        //

        public static final String[] ATTRIBUTE_NAMES;

        static {
            ATTRIBUTE_NAMES = new String[] {
                "traceId", "parentId", "spanId",
                "operation",
                "start", "finish", "elapsed",
                "tags"
            };
        }

        public static String[] getJsonAttributeNames() {
            return ATTRIBUTE_NAMES;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();

            result.append("{ ");
            result.append("\"traceId\": \"" + traceId + "\", ");
            result.append("\"parentId\": \"" + parentId + "\", ");
            result.append("\"spanId\": \"" + spanId + "\", ");
            result.append("\"operation\": \"" + operation + "\", ");
            result.append("\"start\": " + Long.toString(start) + ", ");
            result.append("\"finish\": " + Long.toString(finish) + ", ");
            result.append("\"elapsed\": " + Long.toString(elapsed) + ", ");

            result.append("\"tags\": {");

            if ( tags != null ) {
                String tagPrefix = " ";
                for ( Map.Entry<String, ? extends Object> tagEntry : tags.entrySet() ) {
                    result.append(tagPrefix);
                    result.append("\"" + tagEntry.getKey() + "\": \"" + tagEntry.getValue() + "\"");
                    tagPrefix = ", ";
                }
            }

            result.append(" }");

            result.append(" }");

            return result.toString();
        }

        //

        private final int hashCode;

        @Override
        public int hashCode() {
            return hashCode;
        }

        public int computeHashCode() {
            int useHashCode = 1;
            useHashCode = (useHashCode * 31) + traceId.hashCode();
            useHashCode = (useHashCode * 31) + parentId.hashCode();
            useHashCode = (useHashCode * 31) + spanId.hashCode();
            useHashCode = (useHashCode * 31) + operation.hashCode();
            useHashCode = (useHashCode * 31) + FATUtilsSpans.hashCode(start);
            useHashCode = (useHashCode * 31) + FATUtilsSpans.hashCode(finish);
            useHashCode = (useHashCode * 31) + FATUtilsSpans.hashCode(elapsed);
            if ( tags != null ) {
                useHashCode = (useHashCode * 31) + tags.hashCode();
            }
            return useHashCode;
        }

        @Override
        public boolean equals(Object other) {
            if ( other == null ) {
                return false;
            } else if ( !(other instanceof CompletedSpan) ) {
                return false;
            }
            CompletedSpan otherSpan = (CompletedSpan) other;

            if ( !strEquals(this.traceId, otherSpan.traceId) ) {
                return false;
            } else if ( !strEquals(this.parentId, otherSpan.parentId) ) {
                return false;
            } else if ( !strEquals(this.spanId, otherSpan.spanId) ) {
                return false;
            } else if ( !strEquals(this.operation, otherSpan.operation) ) {
                return false;
            } else if ( this.start != otherSpan.start ) {
                return false;
            } else if ( this.finish != otherSpan.finish ) {
                return false;
            } else if ( this.elapsed != otherSpan.elapsed ) {
                return false;
            } else if ( !mapEquals(this.tags, otherSpan.tags) ) {
                return false;

            } else {
                return true;
            }
        }
    }

    // 'Long.hashCode(long)' is not available until java 1.8.
    public static int hashCode(long value) {
        return (int)(value ^ (value >>> 32));
    }

    public static boolean strEquals(String str1, String str2) {
        if ( str1 == null ) {
            return ( str2 == null );
        } else if ( str2 == null ) {
            return false;
        } else {
            return str1.equals(str2);
        }
    }

    public static boolean mapEquals(
        Map<String, ? extends Object> map1,
        Map<String, ? extends Object> map2) {

        if ( map1 == null ) {
            return ( map2 == null );
        } else if ( map2 == null ) {
            return false;
        }

        if ( map1.size() != map2.size() ) {
            return false;
        }

        for ( Map.Entry<String, ? extends Object> map1Entry : map1.entrySet() ) {
            String map1Key = map1Entry.getKey();
            Object map1Value = map1Entry.getValue();

            if ( !map2.containsKey(map1Key) ) {
                return false;
            } else if ( !valueEquals(map1Value, map2.get(map1Key) ) ) {
                return false;
            } else {
                // continue
            }
        }

        return true;
    }

    public static boolean valueEquals(Object value1, Object value2) {
        if ( value1 == null ) {
            return ( value2 == null );
        } else if ( value2 == null ) {
            return false;
        } else {
            return value1.equals(value2);
        }
    }
}
