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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * <p>Fat testing encoding of completed span information.</p>
 *
 * <p>Data obtained from the mock tracer is a Json formatted print
 * string which encodes an array of completed spans.</p>
 */
public class FATSpan {

    public FATSpan(
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
    public FATSpan(Map<String, JsonValue> attributeData) {
        this( ((JsonString) attributeData.get("traceId")).getString(),
              ((JsonString) attributeData.get("parentId")).getString(),
              ((JsonString) attributeData.get("spanId")).getString(),
              ((JsonString) attributeData.get("operation")).getString(),
              ((JsonNumber) attributeData.get("start")).longValue(),
              ((JsonNumber) attributeData.get("finish")).longValue(),
              ((JsonNumber) attributeData.get("elapsed")).longValue(),
              FATSpanUtils.convert( (JsonObject) attributeData.get("tags") ) );
    }

    //

    /** <p>Correlation ID shared between spans descending from the same initial request.</p> */
    public String traceId;
    /** <p>The ID of the parent span; 0 if this is an initial request.</p> */
    public String parentId;
    /** <p>he span's unique ID. Never 0.</p> */
    public String spanId;

    /** <p>Request URL, or the specified value for an explicitly created span.</p> */
    public String operation;

    /** <p>Start time of the span, in MS.</p> */
    public long start;
    /** <p>Finish time of the span, in MS.</p> */
    public long finish;
    /** <p>Elapsed time of the span, in MS.</p> */
    public long elapsed;

    //

    public String getTraceId() {
        return traceId;
    }

    public static final String NULL_PARENT_ID = "0";

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

    public boolean isSpanKind(FATSpanKind spanKind) {
        return spanKind.matches( getSpanKind() );
    }
    
    //
    
    public static final String TAG_HTTP_URL = "http.url";
    public static final String TAG_HTTP_STATUS_CODE = "http.status_code";
    public static final String TAG_HTTP_METHOD = "http.method";

    public static final String TAG_SPAN_KIND = "span.kind";

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
    public Map<String, String> tags;

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

    public int hashCode;

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
        useHashCode = (useHashCode * 31) + FATSpanUtils.hashCode(start);
        useHashCode = (useHashCode * 31) + FATSpanUtils.hashCode(finish);
        useHashCode = (useHashCode * 31) + FATSpanUtils.hashCode(elapsed);
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
        } else if ( !(other instanceof FATSpan) ) {
            return false;
        }
        FATSpan otherSpan = (FATSpan) other;

        if ( !FATSpanUtils.strEquals(this.traceId, otherSpan.traceId) ) {
            return false;
        } else if ( !FATSpanUtils.strEquals(this.parentId, otherSpan.parentId) ) {
            return false;
        } else if ( !FATSpanUtils.strEquals(this.spanId, otherSpan.spanId) ) {
            return false;
        } else if ( !FATSpanUtils.strEquals(this.operation, otherSpan.operation) ) {
            return false;
        } else if ( this.start != otherSpan.start ) {
            return false;
        } else if ( this.finish != otherSpan.finish ) {
            return false;
        } else if ( this.elapsed != otherSpan.elapsed ) {
            return false;
        } else if ( !FATSpanUtils.mapEquals(this.tags, otherSpan.tags) ) {
            return false;

        } else {
            return true;
        }
    }
    
}