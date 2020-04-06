/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.audit;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.ws.common.internal.encoder.Base64Coder;

/**
 * OAuth audit handler that writes the audit entries to local file in XML
 * format. This audit handler may be configured into the component by suppling
 * the classname
 * <code>com.ibm.oauth.core.api.audit.XMLFileOAuthAuditHandler</code> for the
 * configuration property: <code>oauth20.audithandler.classname</code>. If you
 * choose to use this audit handler class your configuration provider must also
 * provide a value for the configuration property:
 * <code>xmlFileAuditHandler.filename</code> with the filename of the audit file
 * to write to. If your application environment supports more than one instance
 * of the component, each component instance should have a unique filename. <br>
 * <br>
 * Audit records will be contained within an XML document with this basic
 * structure: <br>
 * 
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;entries xmlns="http://www.ibm.com/oauth/audit" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.ibm.com/oauth/audit oauthAudit.xsd">
 * &lt;/entries>
 * </pre>
 * 
 * <br>
 * Here is an example of an audit record from successfully issuing and
 * authorization code: <br>
 * 
 * <pre>
 * &lt;entry timestamp="2011-11-03T04:16:58Z">
 *   &lt;attributes>
 *      &lt;attribute name="request_type" type="urn:ibm:names:oauth:request">
 *         &lt;value>authorization&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="username" type="urn:ibm:names:oauth:request">
 *         &lt;value>shane&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="client_id" type="urn:ibm:names:query:param">
 *         &lt;value>key&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="redirect_uri" type="urn:ibm:names:query:param">
 *         &lt;value>https://localhost:9443/oauthclient/redirect.jsp&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="response_type" type="urn:ibm:names:query:param">
 *         &lt;value>code&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="state" type="urn:ibm:names:query:param">
 *         &lt;value>lAtc9WmB9wrESC2z27Hc&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="scope" type="urn:ibm:names:oauth:request">
 *         &lt;value>scope1&lt;/value>
 *         &lt;value>scope2&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="code" type="urn:ibm:names:oauth:response:attribute">
 *         &lt;value>sU0mD7McHQZlvYgNIPzUunMibuQ3kd&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="authorization_code_id" type="urn:ibm:names:oauth:response:metadata">
 *         &lt;value>sU0mD7McHQZlvYgNIPzUunMibuQ3kd&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="state_id" type="urn:ibm:names:oauth:state">
 *         &lt;value>3871ac2c-ad92-4ba4-82a0-c9ad0e4ceb0b&lt;/value>
 *      &lt;/attribute>
 *      &lt;attribute name="state" type="urn:ibm:names:oauth:response:attribute">
 *         &lt;value>lAtc9WmB9wrESC2z27Hc&lt;/value>
 *      &lt;/attribute>
 *   &lt;/attributes>
 * &lt;/entry>
 * </pre>
 * 
 * <br>
 * Here is an example of an audit record from an error at the token endpoint
 * when the client secret was wrong: <br>
 * 
 * <pre>
 * &lt;entry timestamp="2011-11-03T04:17:46Z">
 *    &lt;error type="invalid_client">
 *       &lt;message>&lt;![CDATA[An invalid client secret was presented for client: key]]>&lt;/message>
 *    &lt;/error>
 *    &lt;attributes>
 *       &lt;attribute name="request_type" type="urn:ibm:names:oauth:request">
 *          &lt;value>access_token&lt;/value>
 *       &lt;/attribute>
 *       &lt;attribute name="client_id" type="urn:ibm:names:oauth:param">
 *          &lt;value>key&lt;/value>
 *       &lt;/attribute>
 *       &lt;attribute name="client_secret" type="urn:ibm:names:body:param">
 *          &lt;value>secret2&lt;/value>
 *       &lt;/attribute>
 *       &lt;attribute name="redirect_uri" type="urn:ibm:names:body:param">
 *          &lt;value>https://localhost:9443/oauthclient/redirect.jsp&lt;/value>
 *       &lt;/attribute>
 *       &lt;attribute name="grant_type" type="urn:ibm:names:body:param">
 *          &lt;value>authorization_code&lt;/value>
 *       &lt;/attribute>
 *       &lt;attribute name="code" type="urn:ibm:names:body:param">
 *          &lt;value>453addb9lEk6lBXqvrSWM3EPZKPj2C&lt;/value>
 *       &lt;/attribute>
 *       &lt;attribute name="client_id" type="urn:ibm:names:body:param">
 *          &lt;value>key&lt;/value>
 *       &lt;/attribute>
 *    &lt;/attributes>
 * &lt;/entry>
 * </pre>
 */
public class XMLFileOAuthAuditHandler implements OAuthAuditHandler {
    final static String CLASS = XMLFileOAuthAuditHandler.class.getName();
    final static String CLOSING_TAG = "</entries>";

    final static Logger _log = Logger.getLogger(CLASS);

    /**
     * A configuration attribute name which must be found in the component
     * configuration and point to the string filename that will be used to write
     * audit records.
     */
    public static final String FILENAME = "xmlFileAuditHandler.filename";

    final static String TEMPLATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + System.getProperty("line.separator")
            + "<entries xmlns=\"http://www.ibm.com/oauth/audit\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.ibm.com/oauth/audit oauthAudit.xsd\">"
            + System.getProperty("line.separator") + CLOSING_TAG;

    private boolean _initialized = false;
    private RandomAccessFile _raf = null;
    private long _pos = 0;
    private Document _document;
    private LSSerializer _serializer;

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.oauth.core.api.audit.OAuthAuditHandler#init(com.ibm.oauth.core
     * .api.config.OAuthComponentConfiguration)
     */
    public void init(OAuthComponentConfiguration config) {
        String filename = config.getConfigPropertyValue(FILENAME);
        if (filename != null) {
            File actualFile = new File(filename);
            if (!actualFile.exists()) {
                // create the audit file from template if it doesn't already
                // exist
                try {
                    createFile(actualFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    _log
                            .warning("Fail to create XML audit file, audit handler disabled.");
                    return;
                }
            }
            try {
                _raf = new RandomAccessFile(actualFile, "rw");
                _pos = positionClosingTag();
                _raf.seek(_pos);
            } catch (IOException e) {
                e.printStackTrace();
                _log
                        .warning("Fail to seek the closing tag in XML audit file, audit handler disabled.");
                return;
            }
            try {
                initXMLSerializer();
            } catch (Exception e) {
                e.printStackTrace();
                _log
                        .warning("Fail to initialize XML serializer, audit handler disabled.");
                return;
            }

            _initialized = true;
        } else {
            _log.warning(FILENAME + " config is null, audit handler disabled");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.ibm.oauth.core.api.audit.OAuthAuditHandler#writeEntry(com.ibm.oauth
     * .core.api.audit.OAuthAuditEntry)
     */
    public void writeEntry(OAuthAuditEntry entry) throws OAuth20Exception {
        if (_initialized) {
            String xmlFrag = _serializer.writeToString(entry.toXML(_document))
                    + "\n";
            synchronized (this) {
                try {
                    byte[] data = Base64Coder.getBytes(xmlFrag);
                    _raf.write(data);
                    _raf.write(Base64Coder.getBytes(CLOSING_TAG));
                    _pos = _pos + data.length;
                    // move to the position for next entry
                    _raf.seek(_pos);
                } catch (IOException e) {
                    _log.log(Level.SEVERE, "Fail to write audit entry", e);
                }
            }
        }
    }

    private void initXMLSerializer() throws Exception {
        DOMImplementationRegistry reg = DOMImplementationRegistry.newInstance();
        DOMImplementationLS impl = (DOMImplementationLS) reg
                .getDOMImplementation("LS");
        LSParser parser = impl.createLSParser(
                DOMImplementationLS.MODE_SYNCHRONOUS,
                "http://www.w3.org/2001/XMLSchema");
        LSInput input = impl.createLSInput();
        ByteArrayInputStream is = new ByteArrayInputStream(Base64Coder.getBytes(TEMPLATE));
        input.setByteStream(is);
        _document = parser.parse(input);
        _serializer = impl.createLSSerializer();
        _serializer.getDomConfig().setParameter("format-pretty-print", true);
        _serializer.getDomConfig().setParameter("xml-declaration", false);
    }

    private void createFile(File actualFile) throws IOException {
        OutputStream os = new BufferedOutputStream(new FileOutputStream(
                actualFile));
        ByteArrayInputStream is = new ByteArrayInputStream(Base64Coder.getBytes(TEMPLATE));

        byte[] buf = new byte[1024];
        int count = 0;
        while ((count = is.read(buf)) != -1) {
            os.write(buf, 0, count);
        }
        is.close();
        os.close();
    }

    private long positionClosingTag() throws IOException {
        byte[] closingTag = Base64Coder.getBytes(CLOSING_TAG);
        long pos = _raf.length();
        int trunk = 4096;
        byte[] content = null;
        do {
            long start = pos - trunk < 0 ? 0 : pos - trunk;
            int len = (int) (pos - start);
            int remain = 0;
            if (content != null) {
                remain = content.length > closingTag.length ? closingTag.length
                        : content.length;
            }
            // read next trunk
            _raf.seek(start);
            byte[] temp = new byte[len + remain];
            int count = 0;
            while (count < len) {
                count += _raf.read(temp, count, len - count);
            }
            // add the remaining from previous loop
            if (remain > 0) {
                System.arraycopy(content, 0, temp, len, remain);
            }
            content = temp;
            len += remain;
            // seek the closing tag
            for (int i = len; i >= closingTag.length; i--) {
                for (int j = 1; j <= closingTag.length; j++) {
                    if (content[i - j] != closingTag[closingTag.length - j]) {
                        break;
                    }
                    if (j == closingTag.length) {
                        return start + i - closingTag.length;
                    }
                }
            }

            pos = start;
        } while (pos > 0);

        return _raf.length();
    }

}
