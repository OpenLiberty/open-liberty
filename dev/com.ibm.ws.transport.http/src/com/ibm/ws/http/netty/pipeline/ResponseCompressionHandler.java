/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.NettyHeaderUtils;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;

/**
 *
 */
public class ResponseCompressionHandler {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(ResponseCompressionHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final HttpChannelConfig config;
    private final HttpResponse response;
    private final HttpHeaders headers;

    private String acceptEncodingHeader;

    private Map<String, Float> acceptableEncodings;
    private final Set<String> unacceptableEncodings;
    private String preferredEncoding;

    private boolean starEncodingParsed = Boolean.FALSE;

    private final int contentLengthMinimum = 2048;

    public ResponseCompressionHandler(HttpChannelConfig config, HttpResponse response, String acceptEncodingHeader) {
        Objects.requireNonNull(config);
        this.config = config;

        Objects.requireNonNull(response);
        this.response = response;
        this.headers = response.headers();

        Objects.requireNonNull(acceptEncodingHeader);
        this.acceptEncodingHeader = acceptEncodingHeader;

        this.acceptableEncodings = new HashMap<String, Float>();
        this.unacceptableEncodings = new HashSet<String>();

        preferredEncoding = config.getPreferredCompressionAlgorithm();

    }

    public void process() {

        if (isAutoCompression()) {

            if ("zlib".equals(preferredEncoding)) {
                preferredEncoding = "deflate";
            }

            if (!"identity".equals(preferredEncoding)) {
                headers.set(HttpHeaderKeys.HDR_CONTENT_ENCODING.getName(), preferredEncoding);
                headers.remove(HttpHeaderKeys.HDR_CONTENT_LENGTH.getName());
            } else {
                headers.remove(HttpHeaderKeys.HDR_CONTENT_ENCODING.getName());
                preferredEncoding = null;
            }

        } else {
            preferredEncoding = null;

        }

    }

    /**
     * Method to check on whether autocompression is requested for this outgoing
     * message.
     *
     * @param msg
     * @return boolean
     */
    private boolean isAutoCompression() {

        boolean doCompression = Boolean.FALSE;

        MSP.debug("isAutoCompression");
        if (config.useAutoCompression()) {
            //set the Vary header
            NettyHeaderUtils.setVary(headers, HttpHeaderKeys.HDR_ACCEPT_ENCODING.getName());
            MSP.debug("isAutoCompression");
            //check and set highest priority compression encoding if set
            //on the Accept-Encoding header
            parseAcceptEncodingHeader();
            MSP.debug("isAutoCompression");
            if (headers.contains(HttpHeaderKeys.HDR_CONTENT_ENCODING.getName())
                && !ContentEncodingValues.IDENTITY.getName().equalsIgnoreCase(headers.get(HttpHeaderKeys.HDR_CONTENT_ENCODING.toString()))) {
                //Body has already been marked as compressed above the channel, do not attempt to compress
                doCompression = Boolean.FALSE;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Response already contains Content-Encoding: [" + headers.get(HttpHeaderKeys.HDR_CONTENT_ENCODING.toString()) + "]");
                }
                MSP.debug("isAutoCompressionA");
            }

            //Check if the message has the appropriate type and size before attempting compression
            else if (!isCompressionCompliant()) {
                doCompression = Boolean.FALSE;
                MSP.debug("isAutoCompressionB");
            }

//            else if (doCompression) {
//                preferredEncoding = outgoingMsgEncoding.getName();
//                if (!this.isSupportedEncoding() || !isCompressionAllowed()) {
//
//                    doCompression = false;
//                }
//            }

            else {
                MSP.debug("isAutoCompressionC");
                // check private compression header
                preferredEncoding = headers.get(HttpHeaderKeys.HDR_$WSZIP.getName());
                if (Objects.nonNull(preferredEncoding)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Header requests compression: [" + preferredEncoding + "]");
                    }

                    headers.remove(HttpHeaderKeys.HDR_$WSZIP.getName());

                    if (this.isEncodingSupported(preferredEncoding) && isCompressionAllowed() && !this.unacceptableEncodings.contains(preferredEncoding)) {
                        doCompression = Boolean.TRUE;

                    }

                }

                //if the private header didn't provide a valid compression
                //target, use the encodings from the accept-encoding header
                if (config.useAutoCompression() && !doCompression) {

                    String serverPreferredEncoding = config.getPreferredCompressionAlgorithm().toLowerCase(Locale.ENGLISH);
                    MSP.log("Should be config value: " + serverPreferredEncoding);
                    //if the compression element has a configured preferred compression
                    //algorithm, check that the client accepts it and the server supports it.
                    //If so, set this to be the compression algorithm.
                    if (!"none".equalsIgnoreCase(serverPreferredEncoding) &&
                        (acceptableEncodings.containsKey(serverPreferredEncoding) || (starEncodingParsed && !this.unacceptableEncodings.contains(serverPreferredEncoding)))) {

                        this.preferredEncoding = serverPreferredEncoding;
                        if (this.isEncodingSupported(preferredEncoding) && isCompressionAllowed()) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Setting server preferred encoding");
                            }
                            doCompression = Boolean.TRUE;
                        }

                    }
                    MSP.log("Should be going in here. do compression should be false: " + doCompression);
                    //At this point, find the compression algorithm by finding the first
                    //algorithm that is both supported by the client and server by iterating
                    //through the sorted list of compression algorithms specified by the
                    //Accept-Encoding header. This returns the first match or gzip in the case
                    //that gzip is tied with other algorithms for the highest quality value.
                    if (!doCompression) {

                        float gZipQV = 0F;
                        boolean checkedGZipCompliance = Boolean.FALSE;

                        //check if GZIP (preferred encoding) was provided
                        if (acceptableEncodings.containsKey(ContentEncodingValues.GZIP.getName())) {
                            gZipQV = acceptableEncodings.get(ContentEncodingValues.GZIP.getName());
                        } else {
                            checkedGZipCompliance = Boolean.TRUE;
                        }

                        for (String encoding : acceptableEncodings.keySet()) {

                            MSP.log("looping, checking encoding: " + encoding);
                            //if gzip has the same qv and we have yet to evaluate gzip,
                            //prioritize gzip over any other encoding.
                            if (acceptableEncodings.get(encoding) == gZipQV && !checkedGZipCompliance) {
                                preferredEncoding = ContentEncodingValues.GZIP.getName();
                                MSP.log("Encoding should be set to gzip");
                                checkedGZipCompliance = true;
                                if (this.isEncodingSupported(preferredEncoding) && isCompressionAllowed()) {
                                    doCompression = Boolean.TRUE;
                                    break;
                                }
                            }

                            preferredEncoding = encoding;
                            if (this.isEncodingSupported(preferredEncoding) && isCompressionAllowed()) {
                                MSP.log("setting allowed encoding: " + preferredEncoding);
                                doCompression = true;
                                break;
                            }
                        }
                        //If there aren't any explicit matches of acceptable encodings,
                        //check if the '*' character was set as acceptable. If so, default
                        //to gzip encoding. If not allowed, try deflate. If neither are allowed,
                        //disable further attempts.
                        if (starEncodingParsed) {
                            MSP.log("Star logic");
                            if (!this.unacceptableEncodings.contains(HttpConstants.GZIP)) {
                                preferredEncoding = ContentEncodingValues.GZIP.getName();
                                doCompression = Boolean.TRUE;
                            } else if (!this.unacceptableEncodings.contains(HttpConstants.DEFLATE)) {

                                preferredEncoding = ContentEncodingValues.DEFLATE.getName();
                                doCompression = Boolean.TRUE;
                            }

                        }
                    }
                }

            }

            if (!doCompression) {
                preferredEncoding = ContentEncodingValues.IDENTITY.getName();
                MSP.log("set identity, compression not allowed");
                // setOutgoingMsgEncoding(ContentEncodingValues.IDENTITY);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "Outgoing Encoding: [" + this.preferredEncoding + "]");
        }

        return doCompression;
    }

    private boolean isCompressionAllowed() {
        String method = "isCompressionAllowed";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, method);
        }

        boolean isAllowed = Boolean.FALSE;

        //If no Accept-Encoding header field is present in a request, the server MAY assume
        //that the client will accept any content coding.
        if (Objects.isNull(acceptEncodingHeader)) {

            isAllowed = Boolean.TRUE;
        }

        else if (acceptableEncodings.containsKey(this.preferredEncoding)) {
            //found encoding, verify if value is non-zero
            isAllowed = (acceptableEncodings.get(this.preferredEncoding) > 0f);
        }

        else if (ContentEncodingValues.GZIP.getName().equals(preferredEncoding)) {
            //gzip and x-gzip are functionally the same
            if (acceptableEncodings.containsKey(ContentEncodingValues.XGZIP.getName())) {
                isAllowed = (acceptableEncodings.get(ContentEncodingValues.XGZIP.getName()) > 0f);
            }
        }

        else if (ContentEncodingValues.IDENTITY.getName().equals(preferredEncoding)) {
            //Identity is always acceptable unless specifically set to 0. Since it
            //wasn't found in acceptableEncodings, return true

            isAllowed = true;
        }

        else {
            //The special symbol "*" in an Accept-Encoding field matches any available
            //content-coding not explicitly listed in the header field.

            isAllowed = this.starEncodingParsed;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, method, isAllowed);
        }
        return isAllowed;
    }

    /**
     * Parse the Accept-Encoding request header to map what compression types
     * the client accepts and attribute their configured quality value.
     */
    private void parseAcceptEncodingHeader() {
        String method = "parseAcceptEncodingHeader";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, method);
        }

        if (Objects.isNull(acceptEncodingHeader)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, method, "parsing [" + acceptEncodingHeader);
            }
            return;
        }

        acceptEncodingHeader = NettyHeaderUtils.stripWhiteSpaces(acceptEncodingHeader).toLowerCase();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, method, "parsing [" + acceptEncodingHeader + "]");
        }

        String[] codingParts;
        String encodingName;
        String qualityValueAsString;
        float qualityValue;
        int indexOfQValue;

        //As defined by section 14.3 Accept-Encoding, the header's possible
        //values constructed as a comma delimited list:
        // 1#( codings[ ";" "q" "=" qvalue ])
        // = ( content-coding | "*" )
        //Therefore, parse this header value by all defined codings

        for (String coding : acceptEncodingHeader.split(HttpConstants.COMMA)) {
            if (coding.endsWith(HttpConstants.SEMICOLON)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, method, "Encoding token was malformed with semicolon delimiter but no quality value. Skipping [" + coding + "]");
                }
                continue;
            }
            //If this coding contains a qvalue, it will be delimited by a semicolon
            codingParts = coding.split(HttpConstants.SEMICOLON);
            if (codingParts.length < 1 || codingParts.length > 2) {
                //If the codingParts contain less than 1 part or more than 2, it is malformed.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, method, "Encoding token was malformed with multiple semicolon delimeters. Skipping [" + coding + "]");
                }
                continue;
            }
            if (codingParts.length == 2) {
                indexOfQValue = codingParts[1].indexOf("q=");
                if (indexOfQValue != 0) {
                    //coding section was delimited by semicolon but had no quality value
                    //or did not start with the quality value identifier.
                    //Malformed section, ignoring and continue parsing
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, method, "Encoding token was malformed with a bad quality value location. Skipping [" + coding + "]");
                    }
                    continue;
                }
                //skip past "q=" to obtain the quality value and try to parse
                //first evaluate the value against the rules defined by section 5.3.1 on quality values
                //'The weight is normalized to a real number in the range 0 through 1.
                //' ... A sender of qvalue MUST NOT generate more than three digits after the decimal
                // point'.
                qualityValueAsString = codingParts[1].substring(indexOfQValue + 2);
                Matcher matcher = config.getCompressionQValueRegex().matcher(qualityValueAsString);

                if (!matcher.matches()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Encoding token was malformed with a bad quality value. Must be normalized between 0 and 1 with no more than three digits. Skipping ["
                                     + coding + "]");
                    }
                    continue;
                }

                try {

                    qualityValue = Float.parseFloat(qualityValueAsString);
                    if (qualityValue < 0) {
                        //Quality values should never be negative, but if a malformed negative
                        //value is parsed, set it as 0 (disallowed)
                        qualityValue = 0;
                    }
                } catch (NumberFormatException e) {
                    //Malformed quality value, ignore this coding and continue parsing
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Encoding token was malformed with a malformed quality value number. Skipping [" + coding + "]");
                    }
                    continue;
                }

            } else {
                //Following the convention for section 14.1 Accept, qvalue scale ranges from 0 to 1 with
                //the default value being q=1;
                qualityValue = 1f;
            }

            encodingName = codingParts[0];

            if (qualityValue == 0) {
                this.unacceptableEncodings.add(encodingName);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, method, "Parsed a non accepted content-encoding: [" + encodingName + "]");
                }
            }

            else if (HttpConstants.STAR.equals(encodingName)) {
                this.starEncodingParsed = (qualityValue > 0f);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, method, "Parsed Wildcard - * with value: " + starEncodingParsed);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, method, "Parsed Encoding - name: [" + encodingName + "] value: [" + qualityValue + "]");
                }
                //Save to key-value pair accept-encoding map
                acceptableEncodings.put(encodingName, qualityValue);
            }

        }
        //Sort map in decreasing order
        acceptableEncodings = sortAcceptableEncodings(acceptableEncodings);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, method);
        }
    }

    /**
     * Utility method used to sort the accept-encoding parsed encodings in descending order
     * or their quality values (qv)
     *
     * @param encodings
     * @return
     */
    private Map<String, Float> sortAcceptableEncodings(Map<String, Float> encodings) {

        final Map<String, Float> sortedEncodings = encodings.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey,
                                                                                                                                                                        Map.Entry::getValue,
                                                                                                                                                                        (e1,
                                                                                                                                                                         e2) -> e1,
                                                                                                                                                                        LinkedHashMap::new));

        return sortedEncodings;
    }

    /**
     * Used to determine if the chosen encoding is supported
     *
     * @param encoding
     * @return
     */
    private boolean isEncodingSupported(String encoding) {
        boolean result = Boolean.TRUE;

        switch (encoding.toLowerCase()) {
            case (HttpConstants.GZIP):
                break;
            case (HttpConstants.X_GZIP):
                break;
            case (HttpConstants.ZLIB):
                break;
            case (HttpConstants.DEFLATE):
                break;
            case (HttpConstants.IDENTITY):
                break;

            default:
                result = Boolean.FALSE;
        }

        return result;
    }

    /**
     * Verifies if the response meets all criteria for compression to take place
     */
    private boolean isCompressionCompliant() {
        String method = "isCompressionCompliant";

        boolean isCompliant = Boolean.TRUE;
        CharSequence mimeTypeChars = HttpUtil.getMimeType(response);
        String mimeType = null;
        String mimeTypeWildcard = null;

        if (Objects.nonNull(mimeTypeChars)) {
            mimeType = mimeTypeChars.toString();
            mimeTypeWildcard = new StringBuilder().append(mimeType.split(HttpConstants.SLASH)[0]).append(HttpConstants.SLASH).append(HttpConstants.STAR).toString();
        }

        if (HttpUtil.isContentLengthSet(response) && HttpUtil.getContentLength(response) < this.contentLengthMinimum) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, method, "Response content length is less than 2048 bytes, do not attempt to compress");
            }
            MSP.log("Invalid content-length: " + HttpUtil.getContentLength(response, HttpConstants.INT_UNDEFINED));
            isCompliant = Boolean.FALSE;
        }

        else if (Objects.isNull(mimeTypeChars)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, method, "No content type defined for this response, do not attempt to compress");
            }
            isCompliant = Boolean.FALSE;
        }

        else if (config.getExcludedCompressionContentTypes().contains(mimeType) ||
                 config.getExcludedCompressionContentTypes().contains(mimeTypeWildcard)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(tc, method, "The Content-Type: " + mimeType + " is not configured as a compressable content type");
            }
            isCompliant = Boolean.FALSE;
        }

        else if (!!!config.getCompressionContentTypes().contains(mimeType) &&
                 !!!config.getCompressionContentTypes().contains(mimeTypeWildcard)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, method, "The Content-Type: " + mimeType + "is not configured as a compressable content type");
            }
            isCompliant = Boolean.FALSE;

        }
        return isCompliant;
    }

    /**
     * @return
     */
    public String getEncoding() {
        return this.preferredEncoding;
    }

}
