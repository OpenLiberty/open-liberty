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
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;

import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class AttachmentSerializer {
    // http://tools.ietf.org/html/rfc2387
    private static final String DEFAULT_MULTIPART_TYPE = "multipart/related";
    
    private Message message;
    private String bodyBoundary;
    private OutputStream out;
    private String encoding;
    
    private String multipartType;
    private Map<String, List<String>> rootHeaders = Collections.emptyMap();
    private boolean xop = true;
    private boolean writeOptionalTypeParameters = true;
    
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
        bodyCt = bodyCt.replaceAll("\"", "\\\"");
        
        // The bodyCt string is used enclosed within "", so if it contains the character ", it
        // should be adjusted, like in the following case:
        //   application/soap+xml; action="urn:ihe:iti:2007:RetrieveDocumentSet"
        // The attribute action is added in SoapActionOutInterceptor, when SOAP 1.2 is used
        // The string has to be changed in:
        //   application/soap+xml"; action="urn:ihe:iti:2007:RetrieveDocumentSet
        // so when it is enclosed within "", the result must be:
        //   "application/soap+xml"; action="urn:ihe:iti:2007:RetrieveDocumentSet"
        // instead of 
        //   "application/soap+xml; action="urn:ihe:iti:2007:RetrieveDocumentSet""
        // that is wrong because when used it produces:
        //   type="application/soap+xml; action="urn:ihe:iti:2007:RetrieveDocumentSet""
        if ((bodyCt.indexOf('"') != -1) && (bodyCt.indexOf(';') != -1)) {
            int pos = bodyCt.indexOf(';');
            StringBuilder st = new StringBuilder(bodyCt.substring(0 , pos));
            st.append("\"").append(bodyCt.substring(pos, bodyCt.length() - 1));
            bodyCt = st.toString();
        }        
        
        // Set transport mime type
        String requestMimeType = multipartType == null ? DEFAULT_MULTIPART_TYPE : multipartType;
        
        StringBuilder ct = new StringBuilder();
        ct.append(requestMimeType);
        
        // having xop set to true implies multipart/related, but just in case...
        boolean xopOrMultipartRelated = xop 
            || DEFAULT_MULTIPART_TYPE.equalsIgnoreCase(requestMimeType)
            || DEFAULT_MULTIPART_TYPE.startsWith(requestMimeType);
        
        // type is a required parameter for multipart/related only
        if (xopOrMultipartRelated
            && requestMimeType.indexOf("type=") == -1) {
            ct.append("; ");
            if (xop) {
                ct.append("type=\"application/xop+xml\"");
            } else {
                ct.append("type=\"").append(bodyCt).append("\"");
            }    
        }
        
        // boundary
        ct.append("; ")
            .append("boundary=\"")
            .append(bodyBoundary)
            .append("\"");
            
        String rootContentId = getHeaderValue("Content-ID", AttachmentUtil.BODY_ATTACHMENT_ID);
        
        // 'start' is a required parameter for XOP/MTOM, clearly defined
        // for simpler multipart/related payloads but is not needed for
        // multipart/mixed, multipart/form-data
        if (xopOrMultipartRelated) {
            ct.append("; ")
                .append("start=\"<")
                .append(checkAngleBrackets(rootContentId))
                .append(">\"");
        }
        
        // start-info is a required parameter for XOP/MTOM, may be needed for
        // other WS cases but is redundant in simpler multipart/related cases
        if (writeOptionalTypeParameters || xop) {
            ct.append("; ")
                .append("start-info=\"")
                .append(bodyCt)
                .append("\"");
        }
        
        
        message.put(Message.CONTENT_TYPE, ct.toString());

        
        // 2. write headers
        out = message.getContent(OutputStream.class);
        encoding = (String) message.get(Message.ENCODING);
        if (encoding == null) {
            encoding = "UTF-8";
        }
        StringWriter writer = new StringWriter();
        writer.write("\r\n");
        writer.write("--");
        writer.write(bodyBoundary);
        
        StringBuilder mimeBodyCt = new StringBuilder();
        String bodyType = getHeaderValue("Content-Type", null);
        if (bodyType == null) {
            mimeBodyCt.append((xop ? "application/xop+xml" : bodyCt) + "; charset=")
                .append(encoding)
                .append("; type=\"")
                .append(bodyCt)
                .append("\";");
        } else {
            mimeBodyCt.append(bodyType);
        }
        
        writeHeaders(mimeBodyCt.toString(), rootContentId, rootHeaders, writer);
        out.write(writer.getBuffer().toString().getBytes(encoding));
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
    
    private static void writeHeaders(String contentType, String attachmentId, 
                                     Map<String, List<String>> headers, Writer writer) throws IOException {
        writer.write("\r\n");
        writer.write("Content-Type: ");
        writer.write(contentType);
        writer.write("\r\n");

        writer.write("Content-Transfer-Encoding: binary\r\n");

        if (attachmentId != null) {
            attachmentId = checkAngleBrackets(attachmentId);
            writer.write("Content-ID: <");
            writer.write(URLDecoder.decode(attachmentId, "UTF-8"));
            writer.write(">\r\n");
        }
        // headers like Content-Disposition need to be serialized
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String name = entry.getKey();
            if ("Content-Type".equalsIgnoreCase(name) || "Content-ID".equalsIgnoreCase(name)
                || "Content-Transfer-Encoding".equalsIgnoreCase(name)) {
                continue;
            }
            writer.write(name + ": ");
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
                writer.write("\r\n");
                writer.write("--");
                writer.write(bodyBoundary);
                
                Map<String, List<String>> headers = null;
                Iterator<String> it = a.getHeaderNames();
                if (it.hasNext()) {
                    headers = new LinkedHashMap<String, List<String>>();
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
                handler.writeTo(out);
            }
        }
        StringWriter writer = new StringWriter();                
        writer.write("\r\n");
        writer.write("--");
        writer.write(bodyBoundary);
        writer.write("--");
        out.write(writer.getBuffer().toString().getBytes(encoding));
        out.flush();
    }

    public boolean isXop() {
        return xop;
    }

    public void setXop(boolean xop) {
        this.xop = xop;
    }

}
