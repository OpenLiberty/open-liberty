/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

/**
 * Allows you to configure logging (group=kernal, component=com.ibm.ws.logging,
 * see resources/OSGI-INF/metatype/metatype.xml)
 *
 */
public class Logging extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_MAX_FILE_SIZE = "maxFileSize";
    private Integer maxFileSize;

    public final static String XML_ATTRIBUTE_NAME_MAX_FILES = "maxFiles";
    private Integer maxFiles;

    public final static String XML_ATTRIBUTE_NAME_CONSOLE_LOG_LEVEL = "consoleLogLevel";
    private String consoleLogLevel;

    public final static String XML_ATTRIBUTE_NAME_ = "messageFileName";
    private String messageFileName;

    public final static String XML_ATTRIBUTE_NAME_TRACE_FILE_NAME = "traceFileName";
    private String traceFileName;

    public final static String XML_ATTRIBUTE_NAME_TRACE_SPECIFICATION = "traceSpecification";
    private String traceSpecification;

    public final static String XML_ATTRIBUTE_NAME_TRACE_FORMAT = "traceFormat";
    private String traceFormat;

    public final static String XML_ATTRIBUTE_NAME_LOG_DIRECTORY = "logDirectory";
    private String logDirectory;

    public final static String XML_ATTRIBUTE_NAME_ISO_DATE_FORMAT = "isoDateFormat";
    private boolean isoDateFormat;

    /**
     * @return the configured log directory
     */
    public String getLogDirectory() {
        return this.logDirectory;
    }

    /**
     * @param logDirectory new log directory
     */
    public void setLogDirectory(String logDirectory) {
        this.logDirectory = ConfigElement.getValue(logDirectory);
    }

    /**
     * @return the configured trace specification
     */
    public String getTraceSpecification() {
        return this.traceSpecification;
    }

    /**
     * @param traceSpecification the traceSpecification to set
     */

    public void setTraceSpecification(String traceSpecification) {
        this.traceSpecification = ConfigElement.getValue(traceSpecification);
    }

    /**
     * @return the traceFileName
     */
    public String getTraceFileName() {
        return this.traceFileName;
    }

    /**
     * default="trace.log"
     *
     * @param traceFileName the traceFileName to set
     */

    public void setTraceFileName(String traceFileName) {
        this.traceFileName = ConfigElement.getValue(traceFileName);
    }

    /**
     * @return the max file size
     */
    public Integer getMaxFileSize() {
        return this.maxFileSize;
    }

    /**
     * default="20"
     *
     * @param maxFileSize the max file size to set
     */

    public void setMaxFileSize(Integer maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    /**
     * @return the max number of files
     */
    public Integer getMaxFiles() {
        return this.maxFiles;
    }

    /**
     * default="2"
     *
     * @param maxFiles the new value for maximum number of files
     */

    public void setMaxFiles(Integer maxFiles) {
        this.maxFiles = maxFiles;
    }

    /**
     * @return the traceFormat
     */
    public String getTraceFormat() {
        return this.traceFormat;
    }

    /**
     * default="BASIC"; options= "BASIC", "ENHANCED", "ADVANCED"
     *
     * @param traceFormat the traceFormat to set
     */

    public void setTraceFormat(String traceFormat) {
        this.traceFormat = ConfigElement.getValue(traceFormat);
    }

    /**
     * @return the messages file name
     */
    public String getMessageFileName() {
        return this.messageFileName;
    }

    /**
     * default="stdout"
     *
     * @param messageFileName the messageFileName to set
     */

    public void setMessageFileName(String messageFileName) {
        this.messageFileName = ConfigElement.getValue(messageFileName);
    }

    /**
     * default="AUDIT"; options= "INFO", "AUDIT", "WARNING", "ERROR"
     *
     * @param consoleLogLevel The level filter for messages sent to system out or system err.
     */

    public void setConsoleLogLevel(String consoleLogLevel) {
        this.consoleLogLevel = ConfigElement.getValue(consoleLogLevel);
    }

    /**
     * @return the console log level name
     */
    public String getConsoleLogLevel() {
        return this.consoleLogLevel;
    }

    /**
     * default="false"
     *
     * @param isoDateFormat A boolean to determine if the date and time in the Liberty logs should be in ISO-8601 format.
     */

    public void setIsoDateFormat(boolean isoDateFormat) {
        this.isoDateFormat = isoDateFormat;
    }

    /**
     * @return the boolean value set to use the ISO 8601 date format
     */
    public boolean getIsoDateFormat() {
        return this.isoDateFormat;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Logging{");
        if (maxFileSize != null)
            buf.append("maxFileSize=\"" + maxFileSize + "\" ");
        if (maxFiles != null)
            buf.append("maxFiles=\"" + maxFiles + "\" ");
        if (consoleLogLevel != null)
            buf.append("consoleLogLevel=\"" + consoleLogLevel + "\" ");
        if (messageFileName != null)
            buf.append("consoleFileName=\"" + messageFileName + "\" ");
        if (traceFileName != null)
            buf.append("traceFileName=\"" + traceFileName + "\" ");
        if (traceFormat != null)
            buf.append("traceFormat=\"" + traceFormat + "\" ");
        if (traceSpecification != null)
            buf.append("traceSpecification=\"" + traceSpecification + "\"");
        if (logDirectory != null)
            buf.append("logDirectory=\"" + logDirectory + "\"");

        buf.append("isoDateFormat=\"" + isoDateFormat + "\"");
        buf.append("}");
        return buf.toString();
    }
}
