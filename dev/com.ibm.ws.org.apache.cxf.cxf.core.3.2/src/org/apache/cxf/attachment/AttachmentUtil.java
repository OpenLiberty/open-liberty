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

// Liberty Change - https://github.com/apache/cxf/blob/ce2dfb8f84175df5fb1c2241b80b6342ca4eeb55/core/src/main/java/org/apache/cxf/attachment/AttachmentUtil.java
package org.apache.cxf.attachment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;  // Liberty Change
import java.util.logging.Level;   // Liberty Change

import javax.activation.CommandInfo;
import javax.activation.CommandMap;
import javax.activation.DataContentHandler;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.activation.MailcapCommandMap;
import javax.activation.URLDataSource;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.SystemPropertyAction; // Liberty Change
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.common.logging.LogUtils;  // Liberty Change

// Liberty Change - Backport https://github.com/apache/cxf/pull/960
// Liberty Changes - Could potentially be removed when updating to CXF 3.5.5
public final class AttachmentUtil {
    // Liberty Change - Start
    // The xop:include "href" attribute (https://www.w3.org/TR/xop10/#xop_href) may include
    // arbitrary URL which we should never follow (unless explicitly allowed).
    public static final String ATTACHMENT_XOP_FOLLOW_URLS_PROPERTY = "org.apache.cxf.attachment.xop.follow.urls";
    // Liberty Change - End

    public static final String BODY_ATTACHMENT_ID = "root.message@cxf.apache.org";

    private static volatile int counter;
    private static final String ATT_UUID = UUID.randomUUID().toString();

    private static final Random BOUND_RANDOM = new Random();
    private static final CommandMap DEFAULT_COMMAND_MAP = CommandMap.getDefaultCommandMap();
    private static final MailcapCommandMap COMMAND_MAP = new EnhancedMailcapCommandMap();
    private static final Logger LOG = LogUtils.getL7dLogger(AttachmentUtil.class);  // Liberty Change

    static final class EnhancedMailcapCommandMap extends MailcapCommandMap {
        @Override
        public synchronized DataContentHandler createDataContentHandler(
                String mimeType) {
            DataContentHandler dch = super.createDataContentHandler(mimeType);
            if (dch == null) {
	        if (LOG.isLoggable(Level.FINE)) { //Liberty Change Start
	           LOG.fine("createDataContentHandler using DEFAULT_COMMAND_MAP");
	        } //Liberty Change End
                dch = DEFAULT_COMMAND_MAP.createDataContentHandler(mimeType);
            }
            return dch;
        }

        @Override
        public DataContentHandler createDataContentHandler(String mimeType,
                DataSource ds) {
            DataContentHandler dch = super.createDataContentHandler(mimeType);
            if (dch == null) {
	        if (LOG.isLoggable(Level.FINE)) { //Liberty Change Start
	           LOG.fine("createDataContentHandler using DEFAULT_COMMAND_MAP for DataSource: " + (ds != null ? ds.getName() : "NULL"));
	        } //Liberty Change End
                dch = DEFAULT_COMMAND_MAP.createDataContentHandler(mimeType, ds);
            }
            return dch;
        }

        @Override
        public synchronized CommandInfo[] getAllCommands(String mimeType) {

            CommandInfo[] commands = super.getAllCommands(mimeType);
            CommandInfo[] defaultCommands = DEFAULT_COMMAND_MAP.getAllCommands(mimeType);
            List<CommandInfo> cmdList = new ArrayList<>(Arrays.asList(commands));

            // Add CommandInfo which does not exist in current command map.
            for (CommandInfo defCmdInfo : defaultCommands) {
                String defCmdName = defCmdInfo.getCommandName();
                boolean cmdNameExist = false;
                for (CommandInfo cmdInfo : commands) {
	            if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	               LOG.finest("getAllCommands: processing cmd: " + cmdInfo.getCommandName());
	            } //Liberty Change End
                    if (cmdInfo.getCommandName().equals(defCmdName)) {
                        cmdNameExist = true;
	                if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	                   LOG.finest("getAllCommands: Found command " + defCmdName);
	                } //Liberty Change End
                        break;
                    }
                }
                if (!cmdNameExist) {
	            if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	               LOG.finest("getAllCommands: Cmd does not exist, using default: " + defCmdName);
	            } //Liberty Change End
                    cmdList.add(defCmdInfo);
                }
            }

            CommandInfo[] allCommandArray = new CommandInfo[0];
            return cmdList.toArray(allCommandArray);
        }

        @Override
        public synchronized CommandInfo getCommand(String mimeType, String cmdName) {

            CommandInfo cmdInfo = super.getCommand(mimeType, cmdName);
            if (cmdInfo == null) {
	        if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	           LOG.finest("super getCommand returned null, so using default");
	        } //Liberty Change End
                cmdInfo = DEFAULT_COMMAND_MAP.getCommand(mimeType, cmdName);
            }
            return cmdInfo;
        }

        /**
         * Merge current mime types and default mime types.
         */
        @Override
        public synchronized String[] getMimeTypes() {
            String[] mimeTypes = super.getMimeTypes();
            String[] defMimeTypes = DEFAULT_COMMAND_MAP.getMimeTypes();
            Set<String> mimeTypeSet = new HashSet<>();
            mimeTypeSet.addAll(Arrays.asList(mimeTypes));
            mimeTypeSet.addAll(Arrays.asList(defMimeTypes));
            String[] mimeArray = new String[0];
            return mimeTypeSet.toArray(mimeArray);
        }
    }



    private AttachmentUtil() {

    }

    static {
        COMMAND_MAP.addMailcap("image/*;;x-java-content-handler="
                               + ImageDataContentHandler.class.getName());
    }

    public static CommandMap getCommandMap() {
        return COMMAND_MAP;
    }

    public static boolean isMtomEnabled(Message message) {
	// Liberty Change Start
        boolean mtomEnabled = false;	
        // return MessageUtils.getContextualBoolean(message, Message.MTOM_ENABLED, false);
        mtomEnabled = MessageUtils.getContextualBoolean(message, Message.MTOM_ENABLED, false);
	if (LOG.isLoggable(Level.FINE)) { //Liberty Change Start
	   LOG.fine("MTOM enabled: " + mtomEnabled);
	}
	return mtomEnabled;
        //Liberty Change End
    }

    public static void setStreamedAttachmentProperties(Message message, CachedOutputStream bos)
        throws IOException {
        Object directory = message.getContextualProperty(AttachmentDeserializer.ATTACHMENT_DIRECTORY);
	if (LOG.isLoggable(Level.FINEST)) {  //Liberty Change Start
	   LOG.finest("setStreamedAttachmentProperties: Attachment directory: " + directory);
	} //Liberty Change End
        if (directory != null) {
            if (directory instanceof File) {
                bos.setOutputDir((File)directory);
            } else {
                bos.setOutputDir(new File((String)directory));
            }
        }

        Object threshold = message.getContextualProperty(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD);
	if (LOG.isLoggable(Level.FINE)) {  //Liberty Change Start
	   LOG.fine("setStreamedAttachmentProperties: Attachment memory threshold: " + threshold);
	} //Liberty Change End
        if (threshold != null) {
            if (threshold instanceof Long) {
                bos.setThreshold((Long)threshold);
            } else {
                bos.setThreshold(Long.parseLong((String)threshold));
            }
        } else {
            bos.setThreshold(AttachmentDeserializer.THRESHOLD);
        }

        Object maxSize = message.getContextualProperty(AttachmentDeserializer.ATTACHMENT_MAX_SIZE);
	if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	   LOG.finest("setStreamedAttachmentProperties: Attachment maxSize: " + maxSize);
	} //Liberty Change End
        if (maxSize != null) {
            if (maxSize instanceof Long) {
                bos.setMaxSize((Long) maxSize);
            } else {
                bos.setMaxSize(Long.parseLong((String)maxSize));
            }
        }
    }

    public static String createContentID(String ns) throws UnsupportedEncodingException {
        // tend to change
        String cid = "cxf.apache.org";

        String name = ATT_UUID + "-" + String.valueOf(++counter);
        if (ns != null && (ns.length() > 0)) {
            try {
                URI uri = new URI(ns);
                String host = uri.getHost();
                if (host != null) {
                    cid = host;
                } else {
                    cid = ns;
                }
            } catch (Exception e) {
	        if (LOG.isLoggable(Level.FINEST)) {  //Liberty Change Start
	           LOG.finest("createContentID caught exception: "  + e + " setting cid to ns: " + ns);
	        } //Liberty Change End
                cid = ns;
            }
        }

	String ret_cid = URLEncoder.encode(name, StandardCharsets.UTF_8.name()) + "@"
            + URLEncoder.encode(cid, StandardCharsets.UTF_8.name());

	return ret_cid;
    }

    public static String getUniqueBoundaryValue() {
        //generate a random UUID.
        //we don't need the cryptographically secure random uuid that
        //UUID.randomUUID() will produce.  Thus, use a faster
        //pseudo-random thing
        long leastSigBits = 0;
        long mostSigBits = 0;
        synchronized (BOUND_RANDOM) {
            mostSigBits = BOUND_RANDOM.nextLong();
            leastSigBits = BOUND_RANDOM.nextLong();
        }

        mostSigBits &= 0xFFFFFFFFFFFF0FFFL;  //clear version
        mostSigBits |= 0x0000000000004000L;  //set version

        leastSigBits &= 0x3FFFFFFFFFFFFFFFL; //clear the variant
        leastSigBits |= 0x8000000000000000L; //set to IETF variant

        UUID result = new UUID(mostSigBits, leastSigBits);

	if (LOG.isLoggable(Level.FINEST)) {  //Liberty Change Start
	   LOG.finest("getUniqueBoundaryValue returning : " + "uuid:" + result.toString());
	} //Liberty Change End

        return "uuid:" + result.toString();
    }

    public static String getAttachmentPartHeader(Attachment att) {

        StringBuilder buffer = new StringBuilder(200);
        buffer.append(HttpHeaderHelper.getHeaderKey(HttpHeaderHelper.CONTENT_TYPE) + ": "
                + att.getDataHandler().getContentType() + ";\r\n");
        if (att.isXOP()) {
            buffer.append("Content-Transfer-Encoding: binary\r\n");
        }
        String id = att.getId();
        if (id.charAt(0) == '<') {
            id = id.substring(1, id.length() - 1);
        }
        buffer.append("Content-ID: <" + id + ">\r\n\r\n");
        return buffer.toString();
    }

    public static Map<String, DataHandler> getDHMap(final Collection<Attachment> attachments) {
        Map<String, DataHandler> dataHandlers = null;
        if (attachments != null) {
            if (attachments instanceof LazyAttachmentCollection) {
                dataHandlers = ((LazyAttachmentCollection)attachments).createDataHandlerMap();
            } else {
                dataHandlers = new DHMap(attachments);
            }
        }

	if (LOG.isLoggable(Level.FINEST)) {  //Liberty Change Start
	   LOG.finest("getDHMap: dataHandlers" + dataHandlers);
	} //Liberty Change End

        return dataHandlers == null ? new LinkedHashMap<String, DataHandler>() : dataHandlers;
    }

    static class DHMap extends AbstractMap<String, DataHandler> {
        final Collection<Attachment> list;
        DHMap(Collection<Attachment> l) {
            list = l;
        }
        public Set<Map.Entry<String, DataHandler>> entrySet() {
            return new AbstractSet<Map.Entry<String, DataHandler>>() {
                @Override
                public Iterator<Map.Entry<String, DataHandler>> iterator() {
                    final Iterator<Attachment> it = list.iterator();
                    return new Iterator<Map.Entry<String, DataHandler>>() {
                        public boolean hasNext() {
                            return it.hasNext();
                        }
                        public Map.Entry<String, DataHandler> next() {
                            final Attachment a = it.next();
                            return new Map.Entry<String, DataHandler>() {
                                @Override
                                public String getKey() {
                                    return a.getId();
                                }

                                @Override
                                public DataHandler getValue() {
                                    return a.getDataHandler();
                                }

                                @Override
                                public DataHandler setValue(DataHandler value) {
                                    return null;
                                }
                            };
                        }
                        public void remove() {
                            it.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return list.size();
                }
            };
        }
        public DataHandler put(String key, DataHandler value) {
            Iterator<Attachment> i = list.iterator();
            DataHandler ret = null;
            while (i.hasNext()) {
                Attachment a = i.next();
                if (a.getId().equals(key)) {
                    i.remove();
                    ret = a.getDataHandler();
                    break;
                }
            }
            list.add(new AttachmentImpl(key, value));
            return ret;
        }
    }

    public static String cleanContentId(String id) {

        if (id != null) {
            if (id.startsWith("<")) {
                // strip <>
                id = id.substring(1, id.length() - 1);
            }
            // strip cid:
            if (id.startsWith("cid:")) {
                id = id.substring(4);
            }
            // urldecode. Is this bad even without cid:? What does decode do with malformed %-signs, anyhow?
            try {
                id = URLDecoder.decode(id, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
	        if (LOG.isLoggable(Level.FINEST)) {  //Liberty Change Start
	           LOG.finest("cleanContentId ignoring UnsupportedEncodingException: " + e);
	        } //Liberty Change End
                //ignore, keep id as is
            }
        }
        if (id == null) {
            //no Content-ID, set cxf default ID
            id = "root.message@cxf.apache.org";
        }
        return id;
    }

    static String getHeaderValue(List<String> v) {
        if (v != null && !v.isEmpty()) {
            return v.get(0);
        }
        return null;
    }
    static String getHeaderValue(List<String> v, String delim) {
        if (v != null && !v.isEmpty()) {
            StringBuilder b = new StringBuilder();
            for (String s : v) {
                if (b.length() > 0) {
                    b.append(delim);
                }
                b.append(s);
            }
            return b.toString();
        }
        return null;
    }
    static String getHeader(Map<String, List<String>> headers, String h) {
        return getHeaderValue(headers.get(h));
    }
    static String getHeader(Map<String, List<String>> headers, String h, String delim) {
        return getHeaderValue(headers.get(h), delim);
    }

    public static Attachment createAttachment(InputStream stream, Map<String, List<String>> headers)
        throws IOException {

        String id = cleanContentId(getHeader(headers, "Content-ID"));

        AttachmentImpl att = new AttachmentImpl(id);

        final String ct = getHeader(headers, "Content-Type");
        String cd = getHeader(headers, "Content-Disposition");
        String fileName = getContentDispositionFileName(cd);

	if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	   LOG.finest("createAttachment: Content-ID: " + id + ", Content-Type: " + 
		       ct + ", Content-Disposition: " + cd + ", filename: " + fileName);
	} //Liberty Change End

        String encoding = null;

        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            String name = e.getKey();
	    if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	       LOG.finest("createAttachment processing header: " + name);
	    } //Liberty Change End
            if (name.equalsIgnoreCase("Content-Transfer-Encoding")) {
                encoding = getHeader(headers, name);
                if ("binary".equalsIgnoreCase(encoding)) {
                    att.setXOP(true);
                }
            }
            att.setHeader(name, getHeaderValue(e.getValue()));
        }
        if (encoding == null) {
            encoding = "binary";
        }
        InputStream ins =  decode(stream, encoding);
        if (ins != stream) {
            headers.remove("Content-Transfer-Encoding");
        }
        DataSource source = new AttachmentDataSource(ct, ins);
        if (!StringUtils.isEmpty(fileName)) {
            ((AttachmentDataSource)source).setName(fileName);
        }
        att.setDataHandler(new DataHandler(source));
        return att;
    }

    static String getContentDispositionFileName(String cd) {
        if (StringUtils.isEmpty(cd)) {
	    if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	       LOG.finest("getContentDispositionFileName returning NULL");
	    } //Liberty Change End
            return null;
        }
        ContentDisposition c = new ContentDisposition(cd);
        String s = c.getParameter("filename");
        if (s == null) {
            s = c.getParameter("name");
        }
        return s;
    }

    public static InputStream decode(InputStream in, String encoding) throws IOException {
        if (encoding == null) {
            return in;
        }
        encoding = encoding.toLowerCase();

        // some encodings are just pass-throughs, with no real decoding.
        if ("binary".equals(encoding)
            || "7bit".equals(encoding)
            || "8bit".equals(encoding)) {
            return in;
        } else if ("base64".equals(encoding)) {
            return new Base64DecoderStream(in);
        } else if ("quoted-printable".equals(encoding)) {
            return new QuotedPrintableDecoderStream(in);
        } else {
            throw new IOException("Unknown encoding " + encoding);
        }
    }
    public static boolean isTypeSupported(String contentType, List<String> types) {

        if (contentType == null) {
            return false;
        }
        contentType = contentType.toLowerCase();
        for (String s : types) {
            if (contentType.indexOf(s) != -1) {
                return true;
            }
        }

        return false;
    }

    public static Attachment createMtomAttachment(boolean isXop, String mimeType, String elementNS,
                                                 byte[] data, int offset, int length, int threshold) {
        if (!isXop || length <= threshold) {
            return null;
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        ByteDataSource source = new ByteDataSource(data, offset, length);
        source.setContentType(mimeType);
        DataHandler handler = new DataHandler(source);

        String id;
        try {
            id = AttachmentUtil.createContentID(elementNS);
        } catch (UnsupportedEncodingException e) {
            throw new Fault(e);
        }
        AttachmentImpl att = new AttachmentImpl(id, handler);
        att.setXOP(isXop);
        return att;
    }

    public static Attachment createMtomAttachmentFromDH(
        boolean isXop, DataHandler handler, String elementNS, int threshold) {

        if (!isXop) {
            return null;
        }

        // The following is just wrong. Even if the DataHandler has a stream, we should still
        // apply the threshold.
        try {
            DataSource ds = handler.getDataSource();
            if (ds instanceof FileDataSource) {
                FileDataSource fds = (FileDataSource)ds;
                File file = fds.getFile();
                if (file.length() < threshold) {
	            if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	               LOG.finest("createMtomAttachmentFromDH: file.length is < threshold: " + file.length());
	            } //Liberty Change End
                    return null;
                }
            } else if (ds.getClass().getName().endsWith("ObjectDataSource")) {
                Object o = handler.getContent();
                if (o instanceof String
                    && ((String)o).length() < threshold) {
	            if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	               LOG.finest("createMtomAttachmentFromDH: handler size is < threshold: " + ((String)o).length());
	            } //Liberty Change End
                    return null;
                } else if (o instanceof byte[] && ((byte[])o).length < threshold) {
	            if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	               LOG.finest("createMtomAttachmentFromDH: byte size is < threshold: " +  ((byte[])o).length);
	            } //Liberty Change End
                    return null;
                }
            }
        } catch (IOException e1) {
	     if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	         LOG.finest("Ignoring IOException: "  +e1);
	     } //Liberty Change End
        //   ignore, just do the normal attachment thing
        }

        String id;
        try {
            id = AttachmentUtil.createContentID(elementNS);
        } catch (UnsupportedEncodingException e) {
            throw new Fault(e);
        }
        AttachmentImpl att = new AttachmentImpl(id, handler);
        if (!StringUtils.isEmpty(handler.getName())) {
            //set Content-Disposition attachment header if filename isn't null
            String file = handler.getName();
            File f = new File(file);
            if (f.exists() && f.isFile()) {
                file = f.getName();
            }
            att.setHeader("Content-Disposition", "attachment;name=\"" + file + "\"");
        }
        att.setXOP(isXop);
        return att;
    }

    public static DataSource getAttachmentDataSource(String contentId, Collection<Attachment> atts) {
        // Liberty Change - Start
        //
        // RFC-2392 (https://datatracker.ietf.org/doc/html/rfc2392) says:
        //
        // A "cid" URL is converted to the corresponding Content-ID message
        // header [MIME] by removing the "cid:" prefix, converting the % encoded
        // character to their equivalent US-ASCII characters, and enclosing the
        // remaining parts with an angle bracket pair, "<" and ">".
        //
        // Liberty Change - End

        if (contentId.startsWith("cid:")) {
            try {
                contentId = URLDecoder.decode(contentId.substring(4), StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException ue) {
	        if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	           LOG.finest("UnsupportedEncodingException occurred: "  + ue);
	        } //Liberty Change End
                contentId = contentId.substring(4);
            }
            // Liberty Change - Start
            // href attribute information item: MUST be a valid URI per the cid: URI scheme (RFC 2392),
            // for example:
            //
            //   <xop:Include xmlns:xop='http://www.w3.org/2004/08/xop/include' href='cid:http://example.org/me.png'/>
            //
            // See please https://www.w3.org/TR/xop10/
            //
            if (contentId.indexOf("://") == -1) {
                return loadDataSource(contentId, atts);
            } else {
                try {
                    final boolean followUrls = Boolean.valueOf(SystemPropertyAction
                        .getProperty(ATTACHMENT_XOP_FOLLOW_URLS_PROPERTY, "false"));
	            if (LOG.isLoggable(Level.FINEST)) { //Liberty Change Start
	                LOG.finest("org.apache.cxf.attachment.xop.follow.urls property is set to: "  + followUrls);
	            } //Liberty Change End
                    if (followUrls) {
                        return new URLDataSource(new URL(contentId));
                    } else {
                        return loadDataSource(contentId, atts);
                    }
                } catch (MalformedURLException e) {
                    throw new Fault(e);
                }
            }
        } else {
            return loadDataSource(contentId, atts);
        }
        // Liberty Change - End
    }

    private static DataSource loadDataSource(String contentId, Collection<Attachment> atts) {
        return new LazyDataSource(contentId, atts);
    }

}
