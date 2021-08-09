/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.internal;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.DataFormatHelper;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.logging.LogRecordExt;

/**
 * The WsLogRecord extends the java.util.logging.LogRecord to add the extra
 * fields that are contained in a WebSphere Application Server log event.
 * <p>
 * Instances of this class can be created only by the WsLogger class. This class
 * is not made public to user code. From a users perspective, once a WsLogRecord
 * is created, it belongs to the logging framework and user code MUST NOT update
 * any attributes of the LogRecord.
 * <p>
 * NOTE: BaseTraceService creates a subclass of WsLogRecord.
 * <b> Serialization notes:</b>
 * <ul>
 * <li>The WsLogRecord class, although serializable must NOT be sent over the
 * wire to a non-WebSphere process since WsLogRecord will not be in the
 * classpath on the receiving process. This is documented.
 * </ul>
 */
public class WsLogRecord extends LogRecord implements java.io.Serializable, LogRecordExt {

    // Don't forget to update this value if a change is made to this class
    // which changes the serialized fields. See the discussion on Object
    // Serialization in the JDK documentation for more information.
    // From Version 1.2.
    private static final long serialVersionUID = 8979064390839459362L;

    private static final String emptyString = "";

    /**
     * Component name
     */
    private String ivComponent = emptyString;

    /**
     * Correlation Id - set by runtime
     */
    private String ivCorrelationId = emptyString;

    /**
     * Extensions
     */
    private final Map<String, String> ivExtensions = new HashMap<String, String>();

    /**
     * A String containing the message as formatted by Tr or JRAS in the
     * MessageEvent If non-null this will be used instead of attempting to
     * localize the message
     */
    private String ivFormattedMessage = null;

    private String ivMessageLocale = null;

    /**
     * Organization name
     */
    private String ivOrganization = emptyString;

    /**
     * Process ID - set by runtime
     */
    private String ivProcessId = emptyString;

    /**
     * Process name - set by runtime
     */
    private String ivProcessName = emptyString;

    /**
     * Product name
     */
    private String ivProduct = emptyString;

    /**
     * Raw Data
     */
    private byte[] ivRawData = null;

    /**
     * Cached StackTrace
     */
    private String ivStackTrace;

    /**
     * Thread name - set by runtime;
     */
    private String ivThreadName;

    /**
     * WebSphere AppServer version - set by runtime
     */
    private String ivVersion = emptyString;

    private String ivSourceClassName;

    private String ivSourceMethodName;

    public static final int DEFAULT_LOCALIZATION = 0;
    public static final int REQUIRES_LOCALIZATION = 1;
    public static final int REQUIRES_NO_LOCALIZATION = 2;

    private int localizable;

    /**
     * The class that issued the Logger request that created this LogRecord.
     * The class is used to find the ResourceBundle for the message contained
     * in this LogRecord.
     */
    private transient Class<?> ivTraceClass;

    /**
     * Construct a WsLogRecord with the given level and message values.
     * <p>
     * 
     * @param level
     *            the logging Level value. Must NOT be null
     * @param msg
     *            the message. May be a text message, pre-localized message or a
     *            message key (in a resource bundle).
     * @param org
     *            the organization that wrote the application using the logger.
     *            Null is tolerated.
     * @param product
     *            the name of the product using the logger. Null is tolerated.
     * @param component
     *            the component using the logger. Null is tolerated.
     */
    protected WsLogRecord(Level level, String msg) {
        super(level, msg);
    }

    public void addExtension(String name, String value) {
        this.ivExtensions.put(name, value);
    }

    public void addExtensions(Map<String, String> extensions) {
        this.ivExtensions.putAll(extensions);
    }

    /**
     * Return the value of the component attribute.
     * 
     * @return component name (may be null)
     */
    public String getComponent() {
        return this.ivComponent;
    }

    /**
     * Return the value of the correlation Id attribute.
     * 
     * @return correlation id (may be null)
     */
    public String getCorrelationId() {
        return this.ivCorrelationId;
    }

    public String getExtension(String name) {
        return this.ivExtensions.get(name);
    }

    public Map<String, String> getExtensions() {
        return this.ivExtensions;
    }

    /**
     * @return String
     */
    public String getFormattedMessage() {
        return this.ivFormattedMessage;
    }

    /**
     * Query the message locale information. Returns null if not set.
     * 
     * @return String
     */
    public String getMessageLocale() {
        return this.ivMessageLocale;
    }

    /**
     * Return the value of the organization attribute.
     * 
     * @return organization (may be null)
     */
    public String getOrganization() {
        return this.ivOrganization;
    }

    /**
     * Return the value of the ProcessId attribute.
     * 
     * @return process Id (may be null)
     */
    public String getProcessId() {
        return this.ivProcessId;
    }

    /**
     * Return the value of the ProcessName attribute.
     * 
     * @return process name (may be null)
     */
    public String getProcessName() {
        return this.ivProcessName;
    }

    /**
     * Return the value of the product attribute.
     * 
     * @return product (may be null)
     */
    public String getProduct() {
        return this.ivProduct;
    }

    /**
     * Return the value of the raw data attribute.
     * 
     * @return product (may be null)
     */
    public byte[] getRawData() {
        return this.ivRawData;
    }

    /**
     * Return the value of the stack trace attribute.
     * 
     * @return stack trace (may be null)
     */
    public String getStackTrace() {
        return this.ivStackTrace;
    }

    /**
     * @return String
     */
    public String getReporterOrSourceThreadName() {
        return this.ivThreadName;
    }

    /**
     * Return the value of the version attribute.
     * 
     * @return version name (may be null)
     */
    public String getVersion() {
        return this.ivVersion;
    }

    /**
     * Sets the value of the component attribute.
     * 
     * @param component
     *            The component to set
     */
    public void setComponent(String component) {
        if (component == null)
            return;
        this.ivComponent = component;
    }

    /**
     * Set the correlation id attribute.
     * <p>
     * 
     * @param correlationId
     *            the correlation id value. Null is tolerated
     */
    public void setCorrelationId(String correlationId) {
        if (correlationId == null)
            return;
        this.ivCorrelationId = correlationId;
    }

    /**
     * Sets the formattedMessage.
     * 
     * @param formattedMessage
     *            The formattedMessage to set
     */
    public void setFormattedMessage(String formattedMessage) {
        this.ivFormattedMessage = formattedMessage;
    }

    /**
     * Set the message locale information to the input value.
     * 
     * @param locale
     */
    public void setMessageLocale(String locale) {
        this.ivMessageLocale = locale;
    }

    /**
     * Sets the value of the organization attribute.
     * 
     * @param organization
     *            The organization to set
     */
    public void setOrganization(String organization) {
        if (organization == null)
            return;
        this.ivOrganization = organization;
    }

    /**
     * Set the version attribute.
     * <p>
     * 
     * @param id
     *            the process id. Null is tolerated.
     */
    public void setProcessId(String id) {
        if (id == null)
            return;
        this.ivProcessId = id;
    }

    /**
     * Set the Process Name attribute.
     * <p>
     * 
     * @param name
     *            the process name. Null is tolerated.
     */
    public void setProcessName(String name) {
        if (name == null)
            return;
        this.ivProcessName = name;
    }

    /**
     * Sets the value of the product attribute.
     * 
     * @param product
     *            The product to set
     */
    public void setProduct(String product) {
        if (product == null)
            return;
        this.ivProduct = product;
    }

    /**
     * Sets the value of the raw data attribute.
     * 
     * @param rawData
     */
    public void setRawData(byte[] rawData) {
        if (rawData == null)
            return;
        this.ivRawData = rawData;
    }

    /**
     * Sets the threadName.
     * 
     * @param threadName
     *            The threadName to set
     */
    public void setSourceThreadName(String threadName) {
        this.ivThreadName = threadName;
    }

    /**
     * Sets the stackTrace.
     * 
     * @param stackTrace
     *            The stackTrace to set
     */
    public void setStackTrace(String stackTrace) {
        this.ivStackTrace = stackTrace;
    }

    /**
     * Set a throwable associated with the log event.
     * 
     * @param throwable
     *            a throwable
     */
    @Override
    public void setThrown(Throwable thrown) {
        if (thrown != null) {
            super.setThrown(thrown);
            this.ivStackTrace = DataFormatHelper.throwableToString(thrown);
        }
    }

    /**
     * Set the version attribute.
     * <p>
     * 
     * @param version
     *            the WAS version. Null is tolerated
     */
    public void setVersion(String version) {
        if (version == null)
            return;
        this.ivVersion = version;
    }

    // setSourceClassName, getSourceClassName, getSourceMethodName, and
    // getSourceMethodName are
    // provided to override the default behaviour which is to try to infer the
    // caller...which performs
    // badly and doesn't identify the right caller in our framework.

    @Override
    public void setSourceClassName(String sourceClassName) {
        this.ivSourceClassName = sourceClassName;
    }

    /**
     * @see java.util.logging.LogRecord#getSourceClassName()
     */
    @Override
    public String getSourceClassName() {
        return this.ivSourceClassName;
    }

    @Override
    public void setSourceMethodName(String sourceMethodName) {
        this.ivSourceMethodName = sourceMethodName;
    }

    /**
     * @see java.util.logging.LogRecord#getSourceMethodName()
     */
    @Override
    public String getSourceMethodName() {
        return this.ivSourceMethodName;
    }

    /**
     * Trace class setter.
     * 
     * @param The class that issued the Logger request that created this LogRecord.
     */
    public void setTraceClass(Class<?> traceClazz) {
        ivTraceClass = traceClazz;
    }

    @Override
    public ResourceBundle getResourceBundle() {
        ResourceBundle rb = super.getResourceBundle();
        String rbBame = getResourceBundleName();
        if (rb == null && ivTraceClass != null && rbBame != null) {
            // The best odds for finding the resource bundle are with using the
            // classloader that loaded the associated class to begin with. Start
            // there.
            try {
                rb = TraceNLSResolver.getInstance().getResourceBundle(ivTraceClass, rbBame, Locale.getDefault());
                super.setResourceBundle(rb);
            } catch (MissingResourceException ex) {
                // no FFDC required
            }
        }
        return rb;
    }

    /**
     * @{inheritDoc
     */
    @Override
    public String getFormattedMessage(Locale locale) {

        ResourceBundle bundle = null;

        if (locale == Locale.ENGLISH && ivTraceClass != null) {
            bundle = TraceNLS.getBaseResourceBundle(ivTraceClass, getResourceBundleName());
        }

        return TraceNLSResolver.getInstance().getMessage(ivTraceClass,
                                                         bundle,
                                                         getResourceBundleName(),
                                                         getMessage(),
                                                         getParameters(),
                                                         getMessage() + " not found in resource bundle " + getResourceBundleName(),
                                                         true, // Yes, format please.
                                                         locale,
                                                         false); // No, don't be quiet about it.
    }

    /**
     * Static method constructs a WsLogRecord object using the given parameters.
     * This bridges Tr-based trace and Logger based trace
     */
    public static WsLogRecord createWsLogRecord(TraceComponent tc, Level level, String msg, Object[] msgParms) {
        WsLogRecord retMe = new WsLogRecord(level, msg);

        retMe.setLoggerName(tc.getName());
        retMe.setParameters(msgParms);
        retMe.setTraceClass(tc.getTraceClass());
        retMe.setResourceBundleName(tc.getResourceBundleName());

        if (level.intValue() >= Level.INFO.intValue()) {
            retMe.setLocalizable(REQUIRES_LOCALIZATION);
        }
        else {
            retMe.setLocalizable(REQUIRES_NO_LOCALIZATION);
        }
        return retMe;
    }

    /**
     * @return the localizable
     */
    public int getLocalizable() {
        return localizable;
    }

    /**
     * @param localizable the localizable to set
     */
    public void setLocalizable(int localizable) {
        this.localizable = localizable;
    }

}
