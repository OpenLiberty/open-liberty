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

/**
 * <p>Utilities for working with instances of {@link FATUtilsSpans.CompletedSpan}.</p>
 *
 * <p>Two general sets of utility are provided:</p>
 *
 * <p>A representation of a completed span is provided by {@link FATUtilsSpans.CompletedSpan}.</p>
 *
 * <p>Code to parse and marshall completed spans is provided, with the input being the JSON
 * print string obtained from the mock tracer.  The flow is that the mock tracer emits the
 * collection of all completed spans as its print string, using JSON formatting for the print
 * string.  The test application provides a service entry which obtains the print string from
 * the injected tracer, and provides that as a text string as the service response.  The parse
 * utility code processes the JSON print string and marshalls completed span instances which
 * should match the original completed spans of the tracer.</p>
 *
 * <p>See also {@link FATOpentracing#verifyContiguousSpans(List, int)} and
 * {@link FATOpentracing#verifyParents(List, int, com.ibm.ws.testing.opentracing.test.FATOpentracing.ParentCondition...)},
 * which perform detailed validation of completed span data.</p>
 */
public class FATUtilsSpans {
    /**
     * <p>Low level JSON parsing: Parse a JSON print string into a {@link JSONArtifact}.</p>
     *
     * @param source The text which is to be parsed into a {@link JSONArtifact}.
     *
     * @return The marshalled JSON artifact.
     *
     * @throws IOException Thrown if parsing failed.
     */
    public static JSONArtifact jsonParse(String source) throws IOException {
        return jsonParse(source, 0, source.length()); // 'jsonParse' throws IOException
    }

    /**
     * <p>Low level JSON parsing: Parse a JSON print string into a {@link JSONArtifact}.</p>
     *
     * @param source The text which is to be parsed into a {@link JSONArtifact}.
     * @param initialSourceOffset The offset at which to begin parsing the JSON
     *     print string.
     * @param finalSourceOffset The offset at which to stop parsing the JSON print
     *     string.
     *
     * @return The marshalled JSON artifact.
     *
     * @throws IOException Thrown if parsing failed.
     */
    public static JSONArtifact jsonParse(String source, int initialSourceOffset, int finalSourceOffset) throws IOException {
        return JSON.parse( new SubStringReader(source, initialSourceOffset, finalSourceOffset) ); // throws IOException
    }

    /**
     * <p>Convert a {@link JSONArtifact} into a {@JSONObject}.  Throw an {@link IOException}
     * if the JSON artifact is not a JSON object.</p>
     *
     * <p>This could be done as a simple type conversion.  This method is provided to generate
     * more detailed exception text, and to throw an {@link IOException} instead of a
     * {@link ClassCastException}.</p>
     *
     * @param jsonArtifact The JSON artifact which is to be converted into a JSON object.
     * @param elementType A description of the type of the JSON object which is expected.
     *
     * @return The JSON artifact type converted to a JSON object.
     *
     * @throws IOException Thrown if the JSON artifact is not a JSON object.
     */
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

    /**
     * <p>Enumeration of the types of completed spans which are possible.</p>
     *
     * <p>The span type is expected to be stored using tag {@link #TAG_SPAN_KIND},
     * as either {@link SPAN_KIND_SERVER}, {@link SPAN_KIND_CLIENT}, or unstored
     * for manually created spans.</p>
     */
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

    /**
     * <p>Fat testing encoding of completed span information.</p>
     *
     * <p>Data obtained from the mock tracer is a JSON formatted print
     * string which encodes an array of completed spans.</p>
     */
    public static final class CompletedSpan {
        /** <p>Correlation ID shared between spans descending from the same initial request.</p> */
        private final String traceId;
        /** <p>The ID of the parent span; 0 if this is an initial request.</p> */
        private final String parentId;
        /** <p>he span's unique ID.  Never 0.</p> */
        private final String spanId;

        /** <p>Request URL, or the specified value for an explicitly created span.</p> */
        private final String operation;

        /** <p>Start time of the span, in MS.</p> */
        private final long start;
        /** <p>Finish time of the span, in MS.</p> */
        private final long finish;
        /** <p>Elapsed time of the span, in MS.</p> */
        private final long elapsed;

        /** <p>Span tags:</p>
         *
         * <p>The Liberty implementation always stores:</p>
         *
         * <ul>
         * <li>'span.kind': the request kind (server, client, or null for explicit spans),</ul>
         * <li>'span.url': the request URL</li>
         * <li>'span.httpMethod': the request method</li>
         * <li>'span.httpStatus': the request return code</li>
         * <li>'span.error': true or false, telling if the request completed with an error</li>
         * </ul>
         */
        private final Map<String, Object> tags;

        //

        public String getTraceId() {
            return traceId;
        }

        public String getParentId() {
            return parentId;
        }

        /**
         * <p>Tell if this span is a root span.  A request which arrives to a service
         * which was not sent while handling a prior request is a root span.</p>
         *
         * <p>A root span has the null parent ID, "0".</p>
         *
         * @return True or false telling if this is a root span.
         */
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

        /**
         * <p>Create a completed span using attribute data obtained
         * from a JSON parse of a completed span print string.</p>
         *
         * <p>See {@link #ATTRIBUTE_NAMES} for the names of the expected
         * attribute values.</p>
         *
         * <p>The "tags" value is expected to be a mapping.  Other values
         * are {@link String} or {@link Number}.</p>
         *
         * @param attributeData Attribute data from which to create the
         *     completed span.
         */
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

        /**
         * <p>Override of hash code to an implementation based on the completed
         * span attribute values.</p>
         *
         * <p>An override is necessary because {@link #hashCode} was re-implemented.</p>
         *
         * @return The integer hash code of this complete span.
         */
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

        /**
         * <p>Tell if an object is equal to this span.  The object is equal if
         * it is non-null, is a completed span, and has attribute values equal
         * to this span's attribute values.</p>
         *
         * @param other An object to compare against this span.
         *
         * @return True or false telling if the object is equal to this span.
         */
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
