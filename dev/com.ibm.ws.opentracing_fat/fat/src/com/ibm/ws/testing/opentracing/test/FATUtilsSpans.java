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

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonString;
import javax.json.JsonNumber;

/**
 * <p>Utilities for working with instances of {@link FATUtilsSpans.CompletedSpan}.</p>
 *
 * <p>Two general sets of utility are provided:</p>
 *
 * <p>A representation of a completed span is provided by {@link FATUtilsSpans.CompletedSpan}.</p>
 *
 * <p>Code to parse and marshall completed spans is provided, with the input being the Json
 * print string obtained from the mock tracer. The flow is that the mock tracer emits the
 * collection of all completed spans as its print string, using Json formatting for the print
 * string. The test application provides a service entry which obtains the print string from
 * the injected tracer, and provides that as a text string as the service response. The parse
 * utility code processes the Json print string and marshalls completed span instances which
 * should match the original completed spans of the tracer.</p>
 *
 * <p>See also {@link FATOpentracing#verifyContiguousSpans(List, int)} and
 * {@link FATOpentracing#verifyParents(List, int, com.ibm.ws.testing.opentracing.test.FATOpentracing.ParentCondition...)},
 * which perform detailed validation of completed span data.</p>
 */
public class FATUtilsSpans {
    /**
     * <p>Low level Json parsing: Parse a Json print string into a {@link JsonObject}.</p>
     *
     * @param source The text which is to be parsed into a {@link JsonObject}.
     *
     * @return The marshaled Json object.
     */
    public static JsonObject jsonParse(String source) {
        return jsonParse( source, 0, source.length() );
    }

    /**
     * <p>Low level Json parsing: Parse a Json print string into a {@link JsonObject}.</p>
     *
     * @param source The text which is to be parsed.
     * @param initialSourceOffset The offset at which to begin parsing.
     * @param finalSourceOffset The offset at which to stop parsing.
     *
     * @return The marshaled Json object.
     */
    public static JsonObject jsonParse(String source, int initialSourceOffset, int finalSourceOffset) {
        JsonParser jsonParser = createJsonParser( createReader(source, initialSourceOffset, finalSourceOffset) );
        @SuppressWarnings("unused")
        JsonParser.Event startEvent = jsonParser.next();
        return jsonParser.getObject();
    }

    /**
     * <p>Low level Json parser: Create a Json parser on a reader.</p>
     * 
     * @param reader The reader for the new Json parser.
     * 
     * @return A new Json parser on the reader.
     */
    public static JsonParser createJsonParser(Reader reader) {
        return Json.createParser(reader);
    }
    
    /**
     * <p>Low level utility: Create an offset reader on a string.</p>
     * 
     * @param source The value on which to create the reader.
     * @param initialOffset The offset at which to begin reading.
     * @param finalOffset The offset at which to stop reading.
     * 
     * @return A reader on the specified region of the source.
     */    
    public static Reader createReader(String source, int initialOffset, int finalOffset) {
        return new SubStringReader(source, initialOffset, finalOffset);
    }

    /**
     * <p>Convert a Json object which has only string valued attributes into
     * a mapping.</p>
     * 
     * @param jsonObject The Json object which is to be converted.
     * 
     * @return A string valued mapping of the attributes of the json object.
     */
    public static Map<String, String> convert(Map<String, JsonValue> jsonObject) {
        Map<String, String> converted = new HashMap<String, String>(jsonObject.size());
        for ( Map.Entry<String, JsonValue> jsonObjectEntry : jsonObject.entrySet() ) {
            String attributeName = jsonObjectEntry.getKey();
            JsonValue attributeValue = jsonObjectEntry.getValue();
            // 'attributeValue.toString' answers the Json text of the attribute
            // value.  For string values, the value includes enclosing quotes,
            // which is not what is wanted.
            String attributeText = ((JsonString) attributeValue).getString();
            converted.put(attributeName, attributeText);
        }
        return converted;
    }

    //

    public static CompletedSpan parseSpan(String text) {
        return parseSpan(text, 0, text.length());
    }

    public static CompletedSpan parseSpan(String source, int initialSourceOffset, int finalSourceOffset) {
        JsonObject jsonObject = jsonParse(source, initialSourceOffset, finalSourceOffset);
        CompletedSpan completedSpan = createSpan(jsonObject);
        return completedSpan;
    }

    public static CompletedSpan createSpan(Map<String, JsonValue> attributeData) {
        return new CompletedSpan(attributeData);
    }

    //

    public static List<CompletedSpan> parseSpans(String text) {
        return parseSpans(text, 0, text.length());
    }

    public static List<CompletedSpan> parseSpans(String text, int initialOffset, int finalOffset) {
        JsonObject jsonSpanState = jsonParse(text, initialOffset, finalOffset);
        JsonArray jsonCompletedSpans = (JsonArray) jsonSpanState.get("completedSpans");

        List<CompletedSpan> completedSpans = new ArrayList<CompletedSpan>( jsonCompletedSpans.size() );
        for ( JsonValue completedSpan : jsonCompletedSpans ) {
            completedSpans.add( createSpan( (JsonObject) completedSpan ) );
        }

        return completedSpans;
    }

    /**
     * <p>Span data as made available from
     * <code>com.ibm.ws.opentracing.mock.OpentracingMockTracer.toString</code>.
     * That creates a Json formatted representation of completed spans.</p>
     *
     * <p>At the top:</p>
     *
     * <code>
     * { "completedSpans": [ completedSpanData, ... ] }
     * </code>
     *
     * (Json uses '[' and ']' for array values. Json uses '{' and '}' for record
     * structures. Numeric values are not quoted. The representation of boolean
     * values is not clear.)
     *
     * <p>Then for each completed span:</p>
     *
     * <code>
     * { "traceId": "ID1", "parentId": "ID2", "spanId": "ID3", "operation": "name1",
     * "start": "time1", "finish": "time2", "elapsed": "time3",
     * "tags": { "tagKey1": "tagValue", "tagKey2": "tagValue2" } }
     * </code>
     *
     * <p>Key characters are '[', ']', '{', '}', '"', and ':". White-space
     * within quoted regions is a part of the quoted value. White-space in
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
            result.append( completedSpan.toString() );
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
            if ( tagValue == null ) {
                return ( testTagValue == null );
            } else {
                return ( tagValue.equals(testTagValue) );
            }
        }
    }

    //

    /**
     * <p>Fat testing encoding of completed span information.</p>
     *
     * <p>Data obtained from the mock tracer is a Json formatted print
     * string which encodes an array of completed spans.</p>
     */
    public static final class CompletedSpan {
        /** <p>Correlation ID shared between spans descending from the same initial request.</p> */
        private final String traceId;
        /** <p>The ID of the parent span; 0 if this is an initial request.</p> */
        private final String parentId;
        /** <p>he span's unique ID. Never 0.</p> */
        private final String spanId;

        /** <p>Request URL, or the specified value for an explicitly created span.</p> */
        private final String operation;

        /** <p>Start time of the span, in MS.</p> */
        private final long start;
        /** <p>Finish time of the span, in MS.</p> */
        private final long finish;
        /** <p>Elapsed time of the span, in MS.</p> */
        private final long elapsed;

        /**
         * <p>Span tags:</p>
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
        private final Map<String, String> tags;

        //

        public String getTraceId() {
            return traceId;
        }

        public String getParentId() {
            return parentId;
        }

        /**
         * <p>Tell if this span is a root span. A request which arrives to a service
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

        public Map<String, String> getTags() {
            return tags;
        }

        public String getTag(String tag) {
            return getTags().get(tag);
        }

        public String getSpanKind() {
            return getTags().get(TAG_SPAN_KIND);
        }

        public boolean isSpanKind(SpanKind spanKind) {
            return spanKind.matches( getSpanKind() );
        }

        //

        public Object getAttribute(String attributeName) {
            if ( attributeName == null) {
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
                return Long.valueOf(getStart());
            } else if ( attributeName.equals("finish") ) {
                return Long.valueOf(getFinish());
            } else if ( attributeName.equals("elapsed") ) {
                return Long.valueOf(getElapsed());
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
            Map<String, String> tags) {

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
                this.tags = new HashMap<String, String>(tags);
            }

            this.hashCode = computeHashCode();
        }

        //

        /**
         * <p>Create a completed span using attribute data obtained
         * from a Json parse of a completed span print string.</p>
         *
         * <p>See {@link #ATTRIBUTE_NAMES} for the names of the expected
         * attribute values.</p>
         *
         * <p>The "tags" value is expected to be a mapping. Other values
         * are {@link String} or {@link Number}.</p>
         *
         * @param attributeData Attribute data from which to create the
         *            completed span.
         */
        public CompletedSpan(Map<String, JsonValue> attributeData) {
            this( ((JsonString) attributeData.get("traceId")).getString(),
                  ((JsonString) attributeData.get("parentId")).getString(),
                  ((JsonString) attributeData.get("spanId")).getString(),
                  ((JsonString) attributeData.get("operation")).getString(),
                  ((JsonNumber) attributeData.get("start")).longValue(),
                  ((JsonNumber) attributeData.get("finish")).longValue(),
                  ((JsonNumber) attributeData.get("elapsed")).longValue(),
                  convert( (JsonObject) attributeData.get("tags") ) );
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

            result.append( "{ " );
            result.append( "\"traceId\": \"" + traceId + "\", " );
            result.append( "\"parentId\": \"" + parentId + "\", " );
            result.append( "\"spanId\": \"" + spanId + "\", " );
            result.append( "\"operation\": \"" + operation + "\", " );
            result.append( "\"start\": " + Long.toString(start) + ", " );
            result.append( "\"finish\": " + Long.toString(finish) + ", " );
            result.append( "\"elapsed\": " + Long.toString(elapsed) + ", " );

            result.append( "\"tags\": {" );

            if (tags != null) {
                String tagPrefix = " ";
                for ( Map.Entry<String, ? extends Object> tagEntry : tags.entrySet() ) {
                    result.append(tagPrefix);
                    result.append( "\"" + tagEntry.getKey() + "\": \"" + tagEntry.getValue() + "\"" );
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
         * <p>Tell if an object is equal to this span. The object is equal if
         * it is non-null, is a completed span, and has attribute values equal
         * to this span's attribute values.</p>
         *
         * @param other An object to compare against this span.
         *
         * @return True or false telling if the object is equal to this span.
         */
        @Override
        public boolean equals(Object other) {
            if ( other == null) {
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
        return (int) (value ^ (value >>> 32));
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
            } else if ( !valueEquals(map1Value, map2.get(map1Key)) ) {
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
