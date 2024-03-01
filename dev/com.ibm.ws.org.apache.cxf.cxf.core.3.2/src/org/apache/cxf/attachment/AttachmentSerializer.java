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

package org.apache.cxf.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;  // Liberty Change
import java.util.logging.Logger;  // Liberty Change

import javax.activation.DataHandler;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.common.logging.LogUtils;  // Liberty Change


public class AttachmentSerializer {
    // http://tools.ietf.org/html/rfc2387
    private static final String DEFAULT_MULTIPART_TYPE = "multipart/related";

    private String contentTransferEncoding = "binary";

    private static final Logger LOG = LogUtils.getL7dLogger(AttachmentSerializer.class);  // Liberty Change

    private Message message;
    private String bodyBoundary;
    private OutputStream out;
    private String encoding;

    private String multipartType;
    private Map<String, List<String>> rootHeaders = Collections.emptyMap();
    private boolean xop = true;
    private boolean writeOptionalTypeParameters = true;	
    // Liberty Change Start 
    private static boolean supportSeparateAction = false;  

    static {

        String unescapedAction = System.getProperty("cxf.support.unescaped.action");
        if (LOG.isLoggable(Level.FINE)) {
           LOG.log(Level.FINE, "cxf.support.unescaped.action property is set to " + unescapedAction);
	}

        if (unescapedAction != null 
            && unescapedAction.trim().length() > 0
            && unescapedAction.trim().equalsIgnoreCase("true")) {
            supportSeparateAction = true;
        }
    }
	// Liberty Change End

    public AttachmentSerializer(Message messageParam) {
        message = messageParam;
    }

    public AttachmentSerializer(Message messageParam,
                                String multipartType,
                                boolean writeOptionalTypeParameters,
                                Map<String, List<String>> headers) {
        message = messageParam;
        this.multipartType = multipartType;
        this.writeOptionalTypeParameters = writeOptionalTypeParameters;
        this.rootHeaders = headers;
    }

    /**
     * Serialize the beginning of the attachment which includes the MIME
     * beginning and headers for the root message.
     */
    public void writeProlog() throws IOException {
        // Create boundary for body
        bodyBoundary = AttachmentUtil.getUniqueBoundaryValue();

        String bodyCt = (String) message.get(Message.CONTENT_TYPE);
        String bodyCtParams = null;
        String bodyCtParamsEscaped = null;
        // split the bodyCt to its head that is the type and its properties so that we
        // can insert the values at the right places based on the soap version and the mtom option
        // bodyCt will be of the form
        // soap11 -> text/xml
        // soap12 -> application/soap+xml; action="urn:ihe:iti:2007:RetrieveDocumentSet"

        if (bodyCt.indexOf(';') != -1) {
            int pos = bodyCt.indexOf(';');
            // get everything from the semi-colon
            bodyCtParams = bodyCt.substring(pos);
            bodyCtParamsEscaped = escapeQuotes(bodyCtParams);
            // keep the type/subtype part in bodyCt
            bodyCt = bodyCt.substring(0, pos);
        }
        // Set transport mime type
        String requestMimeType = multipartType == null ? DEFAULT_MULTIPART_TYPE : multipartType;

        StringBuilder ct = new StringBuilder(32);
        ct.append(requestMimeType);

        // having xop set to true implies multipart/related, but just in case...
        boolean xopOrMultipartRelated = xop
            || DEFAULT_MULTIPART_TYPE.equalsIgnoreCase(requestMimeType)
            || DEFAULT_MULTIPART_TYPE.startsWith(requestMimeType);

        // type is a required parameter for multipart/related only
        if (xopOrMultipartRelated
            && requestMimeType.indexOf("type=") == -1) {
            if (xop) {
                ct.append("; type=\"application/xop+xml\"");
            } else {
                ct.append("; type=\"").append(bodyCt).append('"');
            }
        }

        // boundary
        ct.append("; boundary=\"")
            .append(bodyBoundary)
            .append('"');

        String rootContentId = getHeaderValue("Content-ID", AttachmentUtil.BODY_ATTACHMENT_ID);

        // 'start' is a required parameter for XOP/MTOM, clearly defined
        // for simpler multipart/related payloads but is not needed for
        // multipart/mixed, multipart/form-data
        if (xopOrMultipartRelated) {
            ct.append("; start=\"<")
                .append(checkAngleBrackets(rootContentId))
                .append(">\"");
        }

        // start-info is a required parameter for XOP/MTOM, may be needed for
        // other WS cases but is redundant in simpler multipart/related cases
        // the parameters need to be included within the start-info's value in the escaped form

        if (writeOptionalTypeParameters || xop) {
            ct.append("; start-info=\"")
                .append(bodyCt);
	    // Liberty Change Start
            if (supportSeparateAction) {
               if (LOG.isLoggable(Level.FINE)) {
                  LOG.log(Level.FINE, "Format Content-Type using separate action attribute");
	       }
               if (bodyCtParams != null) {
                   ct.append("\"").append(bodyCtParams);
               }
	    }
	    else { // Liberty Change End
               if (bodyCtParamsEscaped != null) {
                   ct.append(bodyCtParamsEscaped);
               }
               ct.append('"');
	    }
        }

        message.put(Message.CONTENT_TYPE, ct.toString());


        // 2. write headers
        out = message.getContent(OutputStream.class);
        encoding = (String) message.get(Message.ENCODING);
        if (encoding == null) {
            encoding = StandardCharsets.UTF_8.name();
        }
        StringWriter writer = new StringWriter();
        writer.write("\r\n");
        writer.write("--");
        writer.write(bodyBoundary);

        StringBuilder mimeBodyCt = new StringBuilder();
        String bodyType = getHeaderValue("Content-Type", null);
        if (bodyType == null) {
            mimeBodyCt.append(xop ? "application/xop+xml" : bodyCt)
                .append("; charset=").append(encoding);
            if (xop) {
                mimeBodyCt.append("; type=\"").append(bodyCt);
                if (bodyCtParamsEscaped != null) {
                    mimeBodyCt.append(bodyCtParamsEscaped);
                }
                mimeBodyCt.append('"');
            } else if (bodyCtParams != null) {
                mimeBodyCt.append(bodyCtParams);
            }
        } else {
            mimeBodyCt.append(bodyType);
        }

        writeHeaders(mimeBodyCt.toString(), rootContentId, rootHeaders, writer);
        out.write(writer.getBuffer().toString().getBytes(encoding));
    }

    private static String escapeQuotes(String s) {
        return s.indexOf('"') != 0 ? s.replace("\"", "\\\"") : s;
    }

    public void setContentTransferEncoding(String cte) {
        contentTransferEncoding = cte;
    }

    private String getHeaderValue(String name, String defaultValue) {
        List<String> value = rootHeaders.get(name);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.size(); i++) {
            sb.append(value.get(i));
            if (i + 1 < value.size()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    private void writeHeaders(String contentType, String attachmentId,
                                     Map<String, List<String>> headers, Writer writer) throws IOException {
        writer.write("\r\nContent-Type: ");
        writer.write(contentType);
        writer.write("\r\nContent-Transfer-Encoding: " + contentTransferEncoding + "\r\n");

        if (attachmentId != null) {
            attachmentId = checkAngleBrackets(attachmentId);
            writer.write("Content-ID: <");
            
            // 
            // RFC-2392 (https://datatracker.ietf.org/doc/html/rfc2392) says:
            // A "cid" URL is converted to the corresponding Content-ID message
            // header [MIME] by removing the "cid:" prefix, converting the % encoded
            // character to their equivalent US-ASCII characters, and enclosing the
            // remaining parts with an angle bracket pair, "<" and ">".  
            //
            if (attachmentId.startsWith("cid:")) {
                writer.write(decode(attachmentId.substring(4),
                    StandardCharsets.UTF_8));
            } else { 
                //
                // RFC-2392 (https://datatracker.ietf.org/doc/html/rfc2392) says:
                // 
                //   content-id = url-addr-spec
                //   url-addr-spec = addr-spec ; URL encoding of RFC 822 addr-spec
                // 
                // RFC-822 addr-spec (https://datatracker.ietf.org/doc/html/rfc822#appendix-D) says:
                //  
                //   addr-spec = local-part "@" domain ; global address
                //
                String[] address = attachmentId.split("@", 2);
                if (address.length == 2) {
                    // See please AttachmentUtil::createContentID, the domain part is URL encoded
                    final String decoded = tryDecode(address[1], StandardCharsets.UTF_8);
                    // If the domain part is encoded, decode it 
                    if (!decoded.equalsIgnoreCase(address[1])) {
                        writer.write(address[0] + "@" + decoded);
                    } else {
                        writer.write(attachmentId);
                    }
                } else {
                    writer.write(URLEncoder.encode(attachmentId, StandardCharsets.UTF_8.name()));
                }
            }
            writer.write(">\r\n");
        }
        // headers like Content-Disposition need to be serialized
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            if ("Content-Type".equalsIgnoreCase(name) || "Content-ID".equalsIgnoreCase(name)
                || "Content-Transfer-Encoding".equalsIgnoreCase(name)) {
                continue;
            }
            writer.write(name);
            writer.write(": ");
            List<String> values = entry.getValue();
            for (int i = 0; i < values.size(); i++) {
                writer.write(values.get(i));
                if (i + 1 < values.size()) {
                    writer.write(",");
                }
            }
            writer.write("\r\n");
        }

        writer.write("\r\n");
    }

    private static String checkAngleBrackets(String value) {
        if (value.charAt(0) == '<' && value.charAt(value.length() - 1) == '>') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Write the end of the body boundary and any attachments included.
     * @throws IOException
     */
    public void writeAttachments() throws IOException {
        if (message.getAttachments() != null) {
            for (Attachment a : message.getAttachments()) {
                StringWriter writer = new StringWriter();
                writer.write("\r\n--");
                writer.write(bodyBoundary);

                final Map<String, List<String>> headers;
                Iterator<String> it = a.getHeaderNames();
                if (it.hasNext()) {
                    headers = new LinkedHashMap<>();
                    while (it.hasNext()) {
                        String key = it.next();
                        headers.put(key, Collections.singletonList(a.getHeader(key)));
                    }
                } else {
                    headers = Collections.emptyMap();
                }


                DataHandler handler = a.getDataHandler();
                handler.setCommandMap(AttachmentUtil.getCommandMap());

                writeHeaders(handler.getContentType(), a.getId(),
                             headers, writer);
                out.write(writer.getBuffer().toString().getBytes(encoding));
                if ("base64".equals(contentTransferEncoding)) {
                    try (InputStream inputStream = handler.getInputStream()) {
                        encodeBase64(inputStream, out, IOUtils.DEFAULT_BUFFER_SIZE);
                    }
                } else {
                    handler.writeTo(out);
                }
            }
        }
        StringWriter writer = new StringWriter();
        writer.write("\r\n--");
        writer.write(bodyBoundary);
        writer.write("--");
        out.write(writer.getBuffer().toString().getBytes(encoding));
        out.flush();
    }

    private int encodeBase64(InputStream input, OutputStream output, int bufferSize) throws IOException {
        int avail = input.available();
        if (avail > 262143) {
            //must be divisible by 3
            avail = 262143;
        }
        if (avail > bufferSize) {
            bufferSize = avail;
        }
        final byte[] buffer = new byte[bufferSize];
        int n = input.read(buffer);
        int total = 0;
        while (-1 != n) {
            if (n == 0) {
                throw new IOException("0 bytes read in violation of InputStream.read(byte[])");
            }
            //make sure n is divisible by 3
            int left = n % 3;
            n -= left;
            if (n > 0) {
                Base64Utility.encodeAndStream(buffer, 0, n, output);
                total += n;
            }
            if (left != 0) {
                for (int x = 0; x < left; ++x) {
                    buffer[x] = buffer[n + x];
                }
                n = input.read(buffer, left, buffer.length - left);
                if (n == -1) {
                    // we've hit the end, but still have stuff left, write it out
                    Base64Utility.encodeAndStream(buffer, 0, left, output);
                    total += left;
                }
            } else {
                n = input.read(buffer);
            }
        }
        return total;
    }

    public boolean isXop() {
        return xop;
    }

    public void setXop(boolean xop) {
        this.xop = xop;
    }

    // URL decoder would also decode '+' but according to  RFC-2392 we need to convert
    // only the % encoded character to their equivalent US-ASCII characters. 
    private static String decode(String s, Charset charset) throws UnsupportedEncodingException {
	// Liberty Change Start
        String dString = URLDecoder.decode(s.replaceAll("([^%])[+]", "$1%2B"), charset.name());
        if (LOG.isLoggable(Level.FINEST)) {
           LOG.finest("decode: Original string:  " + s + ", charset: " + charset + 
			", Decoded string: " + dString);
	}
        return dString;
	// Liberty Change End
    }

    // Try to decode the string assuming the decoding may fail, the original string is going to
    // be returned in this case.
    private static String tryDecode(String s, Charset charset) {
        try { 
	    String dString = decode(s, charset); // Liberty Change Start
            if (LOG.isLoggable(Level.FINEST)) {
               LOG.finest("tryDecode: Original string:  " + s + ", charset: " + charset + 
			", Decoded string: " + dString);
	    }
            return dString;  // Liberty Change End
        } catch (IllegalArgumentException ex) {
            LOG.finest("tryDecode: IllegalArgumentException exception: " + ex); // Liberty Change
            return s;
        } catch (UnsupportedEncodingException ex) {
            LOG.finest("tryDecode: UnsupportedEncodingException exception: " + ex); // Liberty Change
            return s;
        }
    }
}
