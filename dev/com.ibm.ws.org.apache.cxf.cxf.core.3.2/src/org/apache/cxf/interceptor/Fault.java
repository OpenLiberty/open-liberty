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

package org.apache.cxf.interceptor;

import java.net.HttpURLConnection;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.i18n.UncheckedException;
import org.apache.cxf.helpers.DOMUtils;

/**
 * A Fault that occurs during invocation processing.
 */
public class Fault extends UncheckedException {
    public static final QName FAULT_CODE_CLIENT = new QName("http://cxf.apache.org/faultcode", "client");
    public static final QName FAULT_CODE_SERVER = new QName("http://cxf.apache.org/faultcode", "server");
    public static final String STACKTRACE_NAMESPACE = "http://cxf.apache.org/fault";
    public static final String STACKTRACE = "stackTrace";
    private static final int DEFAULT_HTTP_RESPONSE_CODE = HttpURLConnection.HTTP_INTERNAL_ERROR;
    private static final long serialVersionUID = -1583932965031558864L;

    private Element detail;
    private String message; // Liberty change: messageString field renamed as message. message overide super.message field
    private QName code;
    private String lang;
    /**
     * response http header status code
     */
    private int statusCode = DEFAULT_HTTP_RESPONSE_CODE;

    public Fault(Message message, Throwable throwable) {
        super(message, throwable);
        this.message = message.toString();
        code = FAULT_CODE_SERVER;
    }

    public Fault(Message message) {
        super(message);
        this.message = message.toString();
        code = FAULT_CODE_SERVER;
    }

    public Fault(String message, Logger log) {
        this(new Message(message, log));
    }
    public Fault(String message, ResourceBundle b) {
        this(new Message(message, b));
    }
    public Fault(String message, Logger log, Throwable t) {
        this(new Message(message, log), t);
    }
    public Fault(String message, ResourceBundle b, Throwable t) {
        this(new Message(message, b), t);
    }
    public Fault(String message, Logger log, Throwable t, Object ... params) {
        this(new Message(message, log, params), t);
    }
    public Fault(String message, ResourceBundle b, Throwable t, Object ... params) {
        this(new Message(message, b, params), t);
    }

    public Fault(Throwable t) {
        super(t);
        // Liberty change:
        // if (message != null) {  Liberty change: line removed
        //   message = message.toString(); Liberty change: line removed
        if (super.getMessage() != null) {// Liberty change: line added
            message = super.getMessage();// Liberty change: line added
        } else {
            // message = t == null ? null : t.getMessage(); // Liberty change: line removed
            message = getMessage(t); // Liberty change: line added
        }
        code = FAULT_CODE_SERVER;
    }

    public Fault(Message message, Throwable throwable, QName fc) {
        super(message, throwable);
        this.message = message.toString();
        code = fc;
    }

    public Fault(Message message, QName fc) {
        super(message);
        this.message = message.toString();
        code = fc;
    }

    public Fault(Throwable t, QName fc) {
        super(t);
        /* if (message != null) {  Liberty change: if block removed and replaced
             message = message.toString();
         } else {
             message = t == null ? null : t.getMessage(); Liberty change: end */
        if (super.getMessage() != null) { // Liberty change: if block replaced with the code below
            message = super.getMessage();
        } else {
            message = getMessage(t);  // Liberty change: end
        }
        code = fc;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public QName getFaultCode() {
        return code;
    }

    public Fault setFaultCode(QName c) {
        code = c;
        return this;
    }

    /**
     * Returns the detail node.
     * @return the detail node.
     */
    public Element getDetail() {
        return detail;
    }

    /**
     * Sets a details <code>Node</code> on this fault.
     *
     * @param details the detail node.
     */
    public void setDetail(Element details) {
        detail = details;
    }

    /**
     * Indicates whether this fault has a detail message.
     *
     * @return <code>true</code> if this fault has a detail message;
     *         <code>false</code> otherwise.
     */
    public boolean hasDetails() {
        return this.detail != null;
    }

    /**
     * Returns the detail node. If no detail node has been set, an empty
     * <code>&lt;detail&gt;</code> is created.
     *
     * @return the detail node.
     */
    public Element getOrCreateDetail() {
        if (detail == null) {
            detail = DOMUtils.getEmptyDocument().createElement("detail");
        }
        return detail;
    }

    /**
     * Returns http header status code.
     * @return status code.
     */
    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * Set http header status code on this fault.
     *
     * @param statusCode
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    // Liberty change: getMessage method below is added 
    /**
     * Extracts the effective message value from the specified exception object
     * @param t
     * @return
     */
    private static String getMessage(Throwable t) {
        return t == null ? null : t.getMessage() != null ? t.getMessage() : t.toString();
    }

    public void setLang(String convertedLang) {
        lang = convertedLang;
    }

    public String getLang() {
        return lang;
    }
}
