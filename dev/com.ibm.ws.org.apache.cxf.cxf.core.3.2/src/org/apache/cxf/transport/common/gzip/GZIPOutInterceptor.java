/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.common.gzip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.io.AbstractThresholdOutputStream;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor that compresses outgoing messages using gzip and sets the
 * HTTP Content-Encoding header appropriately. An instance of this class should
 * be added as an out interceptor on clients that need to talk to a service that
 * accepts gzip-encoded requests or on a service that wants to be able to return
 * compressed responses. In server mode, the interceptor only compresses
 * responses if the client indicated (via an Accept-Encoding header on the
 * request) that it can understand them. To handle gzip-encoded input messages,
 * see {@link GZIPInInterceptor}. This interceptor supports a compression
 * {@link #threshold} (default 1kB) - messages smaller than this threshold will
 * not be compressed. To force compression of all messages, set the threshold to
 * 0. This class was originally based on one of the CXF samples
 * (configuration_interceptor).
 */
public class GZIPOutInterceptor extends AbstractPhaseInterceptor<Message> {

    /**
     * Enum giving the possible values for whether we should gzip a particular
     * message.
     */
    public enum UseGzip {
        NO, YES, FORCE
    }

    /**
     * regular expression that matches any encoding with a
     * q-value of 0 (or 0.0, 0.00, etc.).
     */
    public static final Pattern ZERO_Q = Pattern.compile(";\\s*q=0(?:\\.0+)?$");

    /**
     * regular expression which can split encodings
     */
    public static final Pattern ENCODINGS = Pattern.compile("[,\\s]*,\\s*");

    /**
     * Key under which we store the original output stream on the message, for
     * use by the ending interceptor.
     */
    public static final String ORIGINAL_OUTPUT_STREAM_KEY = GZIPOutInterceptor.class.getName()
                                                            + ".originalOutputStream";

    /**
     * Key under which we store an indication of whether compression is
     * permitted or required, for use by the ending interceptor.
     */
    public static final String USE_GZIP_KEY = GZIPOutInterceptor.class.getName() + ".useGzip";

    /**
     * Key under which we store the name which should be used for the
     * content-encoding of the outgoing message. Typically "gzip" but may be
     * "x-gzip" if we are processing a response message and this is the name
     * given by the client in Accept-Encoding.
     */
    public static final String GZIP_ENCODING_KEY = GZIPOutInterceptor.class.getName() + ".gzipEncoding";

    public static final String SOAP_JMS_CONTENTENCODING = "SOAPJMS_contentEncoding";

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(GZIPOutInterceptor.class);
    private static final Logger LOG = LogUtils.getL7dLogger(GZIPOutInterceptor.class);


    /**
     * Compression threshold in bytes - messages smaller than this will not be
     * compressed.
     */
    private int threshold = 1024;
    private boolean force;
    private Set<String> supportedPayloadContentTypes;
    
    public GZIPOutInterceptor() {
        super(Phase.PREPARE_SEND);
        addAfter(MessageSenderInterceptor.class.getName());
    }
    public GZIPOutInterceptor(int threshold) {
        super(Phase.PREPARE_SEND);
        addAfter(MessageSenderInterceptor.class.getName());
        this.threshold = threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public int getThreshold() {
        return threshold;
    }

    public void handleMessage(Message message) {
        UseGzip use = gzipPermitted(message);
        if (use != UseGzip.NO) {
            // remember the original output stream, we will write compressed
            // data to this later
            OutputStream os = message.getContent(OutputStream.class);
            if (os == null) {
                return;
            }
            message.put(ORIGINAL_OUTPUT_STREAM_KEY, os);
            message.put(USE_GZIP_KEY, use);

            // new stream to cache the message
            GZipThresholdOutputStream cs
                = new GZipThresholdOutputStream(threshold,
                                                os,
                                                use == UseGzip.FORCE,
                                                message);
            message.setContent(OutputStream.class, cs);
        }
    }

    /**
     * Checks whether we can, cannot or must use gzip compression on this output
     * message. Gzip is always permitted if the message is a client request. If
     * the message is a server response we check the Accept-Encoding header of
     * the corresponding request message - with no Accept-Encoding we assume
     * that gzip is not permitted. For the full gory details, see <a
     * href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.3">section
     * 14.3 of RFC 2616</a> (HTTP 1.1).
     *
     * @param message the outgoing message.
     * @return whether to attempt gzip compression for this message.
     * @throws Fault if the Accept-Encoding header does not allow any encoding
     *                 that we can support (identity, gzip or x-gzip).
     */
    public UseGzip gzipPermitted(Message message) {
        UseGzip permitted = UseGzip.NO;
        if (supportedPayloadContentTypes != null && message.containsKey(Message.CONTENT_TYPE)
            && !supportedPayloadContentTypes.contains(message.get(Message.CONTENT_TYPE))) {
            return permitted;
        }
        if (MessageUtils.isRequestor(message)) {
            LOG.fine("Requestor role, so gzip enabled");
            Object o = message.getContextualProperty(USE_GZIP_KEY);
            if (o instanceof UseGzip) {
                permitted = (UseGzip)o;
            } else if (o instanceof String) {
                permitted = UseGzip.valueOf((String)o);
            } else {
                permitted = force ? UseGzip.YES : UseGzip.NO;
            }
            message.put(GZIP_ENCODING_KEY, "gzip");
            addHeader(message, "Accept-Encoding", "gzip;q=1.0, identity; q=0.5, *;q=0");
        } else {
            LOG.fine("Response role, checking accept-encoding");
            Exchange exchange = message.getExchange();
            Message request = exchange.getInMessage();
            Map<String, List<String>> requestHeaders = CastUtils.cast((Map<?, ?>)request
                .get(Message.PROTOCOL_HEADERS));
            if (requestHeaders != null) {
                List<String> acceptEncodingHeader = CastUtils.cast(HttpHeaderHelper
                    .getHeader(requestHeaders, HttpHeaderHelper.ACCEPT_ENCODING));
                List<String> jmsEncodingHeader = CastUtils.cast(requestHeaders.get(SOAP_JMS_CONTENTENCODING));
                if (jmsEncodingHeader != null && jmsEncodingHeader.contains("gzip")) {
                    permitted = UseGzip.YES;
                    message.put(GZIP_ENCODING_KEY, "gzip");
                }
                if (acceptEncodingHeader != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Accept-Encoding header: " + acceptEncodingHeader);
                    }
                    // Accept-Encoding is a comma separated list of entries, so
                    // we split it into its component parts and build two
                    // lists, one with all the "q=0" encodings and the other
                    // with the rest (no q, or q=<non-zero>).
                    List<String> zeros = new ArrayList<>(3);
                    List<String> nonZeros = new ArrayList<>(3);

                    for (String headerLine : acceptEncodingHeader) {
                        String[] encodings = ENCODINGS.split(headerLine.trim());

                        for (String enc : encodings) {
                            Matcher m = ZERO_Q.matcher(enc);
                            if (m.find()) {
                                zeros.add(enc.substring(0, m.start()));
                            } else if (enc.indexOf(';') >= 0) {
                                nonZeros.add(enc.substring(0, enc.indexOf(';')));
                            } else {
                                nonZeros.add(enc);
                            }
                        }
                    }

                    // identity encoding is permitted if (a) it is not
                    // specifically disabled by an identity;q=0 and (b) if
                    // there is a *;q=0 then there is also an explicit
                    // identity[;q=<non-zero>]
                    //
                    // [x-]gzip is permitted if (a) there is an explicit
                    // [x-]gzip[;q=<non-zero>], or (b) there is a
                    // *[;q=<non-zero>] and no [x-]gzip;q=0 to disable it.
                    boolean identityEnabled = !zeros.contains("identity")
                                              && (!zeros.contains("*") || nonZeros.contains("identity"));
                    boolean gzipEnabled = nonZeros.contains("gzip")
                                          || (nonZeros.contains("*") && !zeros.contains("gzip"));
                    boolean xGzipEnabled = nonZeros.contains("x-gzip")
                                           || (nonZeros.contains("*") && !zeros.contains("x-gzip"));

                    if (identityEnabled && !gzipEnabled && !xGzipEnabled) {
                        permitted = UseGzip.NO;
                    } else if (identityEnabled && gzipEnabled) {
                        permitted = UseGzip.YES;
                        message.put(GZIP_ENCODING_KEY, "gzip");
                    } else if (identityEnabled && xGzipEnabled) {
                        permitted = UseGzip.YES;
                        message.put(GZIP_ENCODING_KEY, "x-gzip");
                    } else if (!identityEnabled && gzipEnabled) {
                        permitted = UseGzip.FORCE;
                        message.put(GZIP_ENCODING_KEY, "gzip");
                    } else if (!identityEnabled && xGzipEnabled) {
                        permitted = UseGzip.FORCE;
                        message.put(GZIP_ENCODING_KEY, "x-gzip");
                    } else {
                        throw new Fault(new org.apache.cxf.common.i18n.Message("NO_SUPPORTED_ENCODING",
                                                                               BUNDLE));
                    }
                } else {
                    LOG.fine("No accept-encoding header");
                }
            }
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("gzip permitted: " + permitted);
        }
        return permitted;
    }

    static class GZipThresholdOutputStream extends AbstractThresholdOutputStream {
        Message message;

        GZipThresholdOutputStream(int t, OutputStream orig,
                                         boolean force, Message msg) {
            super(t);
            super.wrappedStream = orig;
            message = msg;
            if (force) {
                setupGZip();
            }
        }

        private void setupGZip() {

        }

        @Override
        public void thresholdNotReached() {
            //nothing
            LOG.fine("Message is smaller than compression threshold, not compressing.");
        }

        @Override
        public void thresholdReached() throws IOException {
            LOG.fine("Compressing message.");
            // Set the Content-Encoding HTTP header
            String enc = (String)message.get(GZIP_ENCODING_KEY);
            addHeader(message, "Content-Encoding", enc);
            // if this is a response message, add the Vary header
            if (!Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE))) {
                addHeader(message, "Vary", "Accept-Encoding");
            }

            // gzip the result
            GZIPOutputStream zipOutput = new GZIPOutputStream(wrappedStream);
            wrappedStream = zipOutput;
        }
    }

    /**
     * Adds a value to a header. If the given header name is not currently
     * set in the message, an entry is created with the given single value.
     * If the header is already set, the value is appended to the first
     * element of the list, following a comma.
     *
     * @param message the message
     * @param name the header to set
     * @param value the value to add
     */
    private static void addHeader(Message message, String name, String value) {
        Map<String, List<String>> protocolHeaders = CastUtils.cast((Map<?, ?>)message
            .get(Message.PROTOCOL_HEADERS));
        if (protocolHeaders == null) {
            protocolHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            message.put(Message.PROTOCOL_HEADERS, protocolHeaders);
        }
        List<String> header = CastUtils.cast((List<?>)protocolHeaders.get(name));
        if (header == null) {
            header = new ArrayList<>();
            protocolHeaders.put(name, header);
        }
        if (header.isEmpty()) {
            header.add(value);
        } else {
            header.set(0, header.get(0) + "," + value);
        }
    }
    public void setForce(boolean force) {
        this.force = force;
    }
    public Set<String> getSupportedPayloadContentTypes() {
        return supportedPayloadContentTypes;
    }
    public void setSupportedPayloadContentTypes(Set<String> supportedPayloadContentTypes) {
        this.supportedPayloadContentTypes = supportedPayloadContentTypes;
    }

}
