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

import java.util.Locale;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpBaseMessageImpl;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.ws.http.netty.NettyHeaderUtils;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;

/**
 *
 */
public class ResponseCompressionHandler {

    /** RAS tracing variable */
    private static final TraceComponent tc = Tr.register(ResponseCompressionHandler.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final HttpChannelConfig config;
    private final HttpResponse response;
    private final HttpHeaders headers;

    private final String acceptEncodingHeader;

    public ResponseCompressionHandler(HttpChannelConfig config, HttpResponse response, String acceptEncodingHeader) {
        Objects.requireNonNull(config);
        this.config = config;

        Objects.requireNonNull(response);
        this.response = response;
        this.headers = response.headers();

        Objects.requireNonNull(acceptEncodingHeader);

    }

    public void process() {

    }

    /**
     * Method to check on whether autocompression is requested for this outgoing
     * message.
     *
     * @param msg
     * @return boolean
     */
    private boolean isAutoCompression() {

        if (config.useAutoCompression()) {
            //set the Vary header
            NettyHeaderUtils.setVary(headers, HttpHeaderKeys.HDR_ACCEPT.toString());


        //check and set highest priority compression encoding if set
        //on the Accept-Encoding header
        parseAcceptEncodingHeader();
        boolean rc = isOutgoingMsgEncoded();

        //Check if the message has the appropriate type and size before attempting compression
        if (this.getHttpConfig().useAutoCompression() && !this.isCompressionCompliant()) {
            rc = false;
        }

        else if (msg.containsHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING) && !"identity".equalsIgnoreCase(msg.getHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING).asString())) {
            //Body has already been marked as compressed above the channel, do not attempt to compress
            rc = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Response already contains Content-Encoding: [" + msg.getHeader(HttpHeaderKeys.HDR_CONTENT_ENCODING).asString() + "]");
            }

        }

        else if (rc) {
            preferredEncoding = outgoingMsgEncoding.getName();
            if (!this.isSupportedEncoding() || !isCompressionAllowed()) {

                rc = false;
            }
        }

        else {

            // check private compression header
            preferredEncoding = msg.getHeader(HttpHeaderKeys.HDR_$WSZIP).asString();
            if (null != preferredEncoding) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Header requests compression: [" + preferredEncoding + "]");
                }

                msg.removeSpecialHeader(HttpHeaderKeys.HDR_$WSZIP);

                if (this.isSupportedEncoding() && isCompressionAllowed() && !this.unacceptedEncodings.contains(preferredEncoding)) {
                    rc = true;
                    setCompressionFlags();

                }

            }

            //if the private header didn't provide a valid compression
            //target, use the encodings from the accept-encoding header
            if (this.getHttpConfig().useAutoCompression() && !rc) {

                String serverPreferredEncoding = getHttpConfig().getPreferredCompressionAlgorithm().toLowerCase(Locale.ENGLISH);

                //if the compression element has a configured preferred compression
                //algorithm, check that the client accepts it and the server supports it.
                //If so, set this to be the compression algorithm.
                if (!"none".equalsIgnoreCase(serverPreferredEncoding) &&
                    (acceptableEncodings.containsKey(serverPreferredEncoding) || (bStarEncodingParsed && !this.unacceptedEncodings.contains(serverPreferredEncoding)))) {

                    this.preferredEncoding = serverPreferredEncoding;
                    if (this.isSupportedEncoding() && isCompressionAllowed()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Setting server preferred encoding");
                        }
                        rc = true;
                        setCompressionFlags();
                    }

                }

                //At this point, find the compression algorithm by finding the first
                //algorithm that is both supported by the client and server by iterating
                //through the sorted list of compression algorithms specified by the
                //Accept-Encoding header. This returns the first match or gzip in the case
                //that gzip is tied with other algorithms for the highest quality value.
                if (!rc) {

                    float gZipQV = 0F;
                    boolean checkedGZipCompliance = false;

                    //check if GZIP (preferred encoding) was provided
                    if (acceptableEncodings.containsKey(ContentEncodingValues.GZIP.getName())) {
                        gZipQV = acceptableEncodings.get(ContentEncodingValues.GZIP.getName());
                    } else {
                        checkedGZipCompliance = true;
                    }

                    for (String encoding : acceptableEncodings.keySet()) {
                        //if gzip has the same qv and we have yet to evaluate gzip,
                        //prioritize gzip over any other encoding.
                        if (acceptableEncodings.get(encoding) == gZipQV && !checkedGZipCompliance) {
                            preferredEncoding = ContentEncodingValues.GZIP.getName();
                            checkedGZipCompliance = true;
                            if (this.isSupportedEncoding() && isCompressionAllowed()) {
                                rc = true;
                                setCompressionFlags();
                                break;
                            }
                        }

                        preferredEncoding = encoding;
                        if (this.isSupportedEncoding() && isCompressionAllowed()) {
                            rc = true;
                            setCompressionFlags();
                            break;
                        }
                    }
                    //If there aren't any explicit matches of acceptable encodings,
                    //check if the '*' character was set as acceptable. If so, default
                    //to gzip encoding. If not allowed, try deflate. If neither are allowed,
                    //disable further attempts.
                    if (bStarEncodingParsed) {
                        if (!this.unacceptedEncodings.contains(ContentEncodingValues.GZIP.getName())) {
                            preferredEncoding = ContentEncodingValues.GZIP.getName();
                            rc = true;
                            setCompressionFlags();
                        } else if (!this.unacceptedEncodings.contains(ContentEncodingValues.DEFLATE.getName())) {

                            preferredEncoding = ContentEncodingValues.DEFLATE.getName();
                            rc = true;
                            setCompressionFlags();
                        }

                    }
                }
            }

        }

        if (!rc) {
            //compression not allowed, disable further attempts
            this.setGZipEncoded(false);
            this.setXGZipEncoded(false);
            this.setZlibEncoded(false);
            setOutgoingMsgEncoding(ContentEncodingValues.IDENTITY);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(tc, "Outgoing Encoding: [" + this.outgoingMsgEncoding + "]");
        }

        return rc;
    }

    private boolean isCompressionAllowed() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "isCompressionAllowed");
        }

        boolean isAllowed = Boolean.FALSE;

// TODO: get from attribute value of ACCEPT ENCODING
//        if (getRequest().getHeader(HttpHeaderKeys.HDR_ACCEPT_ENCODING).asString() == null) {
//            //If no accept-encoding field is present in a request, the server MAY assume
//            //that the client will accept any content coding.
//            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
//                Tr.exit(tc, "isCompressionAllowed(1)", Boolean.TRUE);
//            }
//            return true;
//        }

        if (acceptableEncodings.containsKey(this.preferredEncoding)) {
            //found encoding, verify if value is non-zero
            rc = (acceptableEncodings.get(this.preferredEncoding) > 0f);
        }

        else if (ContentEncodingValues.GZIP.getName().equals(preferredEncoding)) {
            //gzip and x-gzip are functionally the same
            if (acceptableEncodings.containsKey(ContentEncodingValues.XGZIP.getName())) {
                rc = (acceptableEncodings.get(ContentEncodingValues.XGZIP.getName()) > 0f);
            }
        }

        else if (ContentEncodingValues.IDENTITY.getName().equals(preferredEncoding)) {
            //Identity is always acceptable unless specifically set to 0. Since it
            //wasn't found in acceptableEncodings, return true

            rc = true;
        }

        else {
            //The special symbol "*" in an Accept-Encoding field matches any available
            //content-coding not explicitly listed in the header field.

            rc = this.bStarEncodingParsed;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "isCompressionAllowed(2): " + rc);
        }
        return rc;
    }

}
