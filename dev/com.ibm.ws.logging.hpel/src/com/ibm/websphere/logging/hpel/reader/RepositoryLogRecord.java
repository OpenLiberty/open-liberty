/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.logging.hpel.reader;

import java.util.Map;

/**
 * An individual record in the HPEL repository.
 * <p>
 * Note that both logging requests and tracing requests are represented as
 * RepositoryLogRecords. The java.util.logging package makes no distinction
 * between logging and tracing, and does not differentiate between requests
 * that require localization and those that do not. The RepositoryLogRecord
 * separately keeps track of which requests were successfully localized, and
 * where specified also keeps track of which requests are localizable.
 * <p>
 * Many of the RepositoryLogRecord's fields are modeled after identically
 * named fields in the java.util.logging.LogRecord class:
 * <ul>
 * <li><code>getLevel</code></li>
 * <li><code>getLoggerName</code></li>
 * <li><code>getMillis</code></li>
 * <li><code>getParameters</code></li>
 * <li><code>getResourceBundleName</code></li>
 * <li><code>getThreadID</code></li>
 * <li><code>getSourceClassName</code></li>
 * <li><code>getSourceMethodName</code></li>
 * <li><code>getThreadID</code></li>
 * </ul>
 * <p>
 * The RepositoryLogRecord also keeps track of additional information as
 * explained in the method detail.
 * <p>
 * <strong>Fields related to re-localization:</strong>
 * <p>
 * The RepositoryLogRecord makes re-localization (retranslating a log to
 * another language using an alternate locale's resource bundles) of messages
 * possible by storing the raw message. The localizable field helps improve
 * message formatting efficiency by providing a hint to formatters about which
 * requests localization is required for.
 * <p>
 * The message locale field is also helpful for re-localization cases as it
 * contains the locale name used during the runtime localization of the
 * localized message, which can be used to verify if the formatted message was
 * already localized in the target locale.
 * <p>
 * Note that for needs not related to re-localization, the formatted message
 * is the most useful representation of the message, and mimics the
 * java.util.logging.LogRecord's {@link java.util.logging.LogRecord#getMessage()} behavior.
 * 
 * @ibm-api
 */
public interface RepositoryLogRecord extends RepositoryLogRecordHeader {
    // Names for WsLogRecord items now in extensions hash
    public static final String PTHREADID = "thread";
    public static final String COMPONENT = "component";
    public static final String CORRELATIONID = "UOW";
    public static final String ORBREQUESTID = "ORBRequestId";
    public static final String ORGANIZATION = "org";
    public static final String PRODUCT = "prod";

    // Field names for localization attribute
    public static final int DEFAULT_LOCALIZATION = 0;
    public static final int REQUIRES_LOCALIZATION = 1;
    public static final int REQUIRES_NO_LOCALIZATION = 2;

    /**
     * Returns the name of the logger that created this record.
     * 
     * @return the loggerName attribute value.
     * @see java.util.logging.LogRecord#getLoggerName()
     */
    public String getLoggerName();

    /**
     * Returns the IBM Message ID of the record's message.
     * <p>
     * The IBM Message ID is the unique identifier of this message across IBM
     * software products. This message ID is the same regardless of what
     * locale the message is rendered in.
     * <p>
     * For non-message records, and for other messages that do not begin with a
     * message ID this method returns <code>null</code>.
     * 
     * @return the message ID or <code>null</code> if there is no message ID.
     */
    public String getMessageID();

    /**
     * Returns the raw message text as it was supplied in the call to the logger.
     * 
     * @return the raw message.
     */
    public String getRawMessage();

    /**
     * Returns the formatted version of the record with any place holder
     * parameters substituted with values.
     * <p>
     * In cases where the localized message is not null, the formatted message
     * is the localized message with any place holder values filled in with
     * corresponding parameter values.
     * <p>
     * In cases where the localized message is null, the formatted message
     * is the raw message with any place holder values filled in with
     * corresponding parameter values.
     * <p>
     * This is the same behavior as found in {@link java.util.logging.LogRecord#getMessage()}
     * 
     * @return the fully localized and formatted message.
     * @see #getRawMessage()
     * @see #getLocalizedMessage()
     * @see java.util.logging.LogRecord#getMessage()
     */
    public String getFormattedMessage();

    /**
     * Returns the localized version of the raw message if localization was
     * attempted and successful.
     * <p>
     * In cases where the raw message is localizable, and localization
     * is successful, the localized message is the value looked up from the
     * associated resource bundle in the associated locale using the raw
     * message as the key.
     * <p>
     * In cases where the raw message is localizable, and localization fails,
     * the localized message is null.
     * <p>
     * In cases where the raw message is not localizable, the localized
     * message is null.
     * <p>
     * Note that, in contrast to the formatted message, the localized message
     * does NOT have its place holder values (if any) filled in with parameter
     * values.
     * 
     * @return the localized message or <code>null</code> if no localization
     *         is necessary or localization failed.
     * @see #getMessageLocale()
     */
    public String getLocalizedMessage();

    /**
     * Returns the locale used for message localization.
     * <p>
     * In cases where the raw message is localizable, and localization is
     * successful, the locale is the locale used by the runtime for
     * localization.
     * <p>
     * In cases where the raw message is localizable, and localization fails,
     * the locale is null.
     * <p>
     * In cases where the raw message is not localizable, the locale is null.
     * 
     * @return the message locale or <code>null</code> if localization was
     *         not required for this record or failed at runtime.
     */
    public String getMessageLocale();

    /**
     * Returns the resource bundle name associated with this record.
     * <p>
     * Resource bundles typically contain the locale specific version of the
     * raw message.
     * 
     * @return the resource bundle name or <code>null</code> if message
     *         does not require translation.
     * @see java.util.logging.LogRecord#getResourceBundleName()
     */
    public String getResourceBundleName();

    /**
     * Returns values for the positional parameters in the record's message.
     * 
     * @return the parameters.
     * @see java.util.logging.LogRecord#getParameters()
     */
    public Object[] getParameters();

    /**
     * Returns the sequence index of the message as generated by the logger.
     * 
     * @return the sequence index.
     */
    public long getSequence();

    /**
     * Returns the name of the class that made the call to the logger.
     * 
     * Note that this may be the name of the source class supplied in the call
     * to the logger, or may be an inferred source class name, and is not
     * guaranteed to be accurate.
     * 
     * @return the source class name.
     * @see java.util.logging.LogRecord#getSourceClassName()
     */
    public String getSourceClassName();

    /**
     * Returns the name of the method that made the call to the logger.
     * 
     * Note that this may be the name of the source method supplied in the call
     * to the logger, or may be an inferred source method name, and is not
     * guaranteed to be accurate.
     * 
     * @return the source method name.
     * @see java.util.logging.LogRecord#getSourceMethodName()
     */
    public String getSourceMethodName();

    /**
     * Returns the stack trace if this record was generated due to an
     * exception.
     * 
     * The stack trace is only computed for records where a throwable was
     * supplied by the caller explicitly.
     * 
     * @return the stack trace or <code>null</code> if message does not
     *         have a stack trace associated with it.
     */
    public String getStackTrace();

    /**
     * Returns the entire set of extended values associated with this record.
     * <p>
     * Extensions may be added to a record by the logger via the
     * Logger.properties extension mechanism, or may be added to the
     * record by the logging runtime which uses extensions for other fields
     * (see extension constants).
     * <p>
     * Extensions from the HPEL {@link com.ibm.websphere.logging.hpel.LogRecordContext} are also added to the record by the HPEL handlers.
     * 
     * @return the extensions.
     */
    public Map<String, String> getExtensions();

    /**
     * Returns an extended value associated with this record.
     * <p>
     * This is effectively returning <code>getExtensions().get(name)</code>
     * 
     * @param name the extension's name.
     * @return the specified extension.
     */
    public String getExtension(String name);

    /**
     * Returns an indicator of whether or not this record is localizable.
     * <p>
     * This field is generally set via the Logger.properties file using the
     * minimum_localization_level attribute. The Logger.properties file is an
     * extension to the java.util.logging API.
     * <p>
     * The indicator will be one of the following values:
     * <p>DEFAULT_LOCALIZATION where no information was supplied by the logger
     * as to the localizability of this record.</p>
     * <p>REQUIRES_LOCALIZATION where the record is localizable.</p>
     * <p>REQUIRES_NO_LOCALIZATION where the record is not localizable.</p>
     * 
     * @return the localizable attribute value.
     */
    public int getLocalizable();

    /**
     * Returns any raw data associated with this record.
     * 
     * @return the rawData attribute value.
     */
    public byte[] getRawData();

}
