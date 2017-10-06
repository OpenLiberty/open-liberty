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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.JsonString;

/**
 * <p>Utilities for working with instances of {@link FATSpan}.</p>
 *
 * <p>Two general sets of utility are provided:</p>
 *
 * <p>A representation of a completed span is provided by {@link FATSpan}.</p>
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
public class FATSpanUtils {
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

    public static FATSpan parseSpan(String text) {
        return parseSpan(text, 0, text.length());
    }

    public static FATSpan parseSpan(String source, int initialSourceOffset, int finalSourceOffset) {
        JsonObject jsonObject = jsonParse(source, initialSourceOffset, finalSourceOffset);
        FATSpan completedSpan = createSpan(jsonObject);
        return completedSpan;
    }

    public static FATSpan createSpan(Map<String, JsonValue> attributeData) {
        return new FATSpan(attributeData);
    }

    //

    public static List<FATSpan> parseSpans(String text) {
        return parseSpans(text, 0, text.length());
    }

    public static List<FATSpan> parseSpans(String text, int initialOffset, int finalOffset) {
        JsonObject jsonSpanState = jsonParse(text, initialOffset, finalOffset);
        JsonArray jsonFATCompletedSpans = (JsonArray) jsonSpanState.get("completedSpans");

        List<FATSpan> completedSpans = new ArrayList<FATSpan>( jsonFATCompletedSpans.size() );
        for ( JsonValue completedSpan : jsonFATCompletedSpans ) {
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
    public static String toString(List<FATSpan> completedSpans) {
        StringBuilder result = new StringBuilder();

        result.append("{ ");
        result.append("\"completedSpans\": {");

        String prefix = " ";
        for ( FATSpan completedSpan : completedSpans ) {
            result.append(prefix);
            prefix = "\n ";
            result.append( completedSpan.toString() );
        }
        result.append(" }");
        return result.toString();
    }

    //

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
