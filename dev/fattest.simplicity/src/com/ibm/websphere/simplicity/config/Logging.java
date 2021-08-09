/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Allows you to configure logging (group=kernal, component=com.ibm.ws.logging,
 * see resources/OSGI-INF/metatype/metatype.xml)
 *
 * @author Tim Burns
 *
 */
public class Logging extends ConfigElement {

    private Integer maxFileSize;
    private Integer maxFiles;
    private String consoleLogLevel;
    private String messageFileName;
    private String traceFileName;
    private String traceSpecification;
    private String traceFormat;
    private String logDirectory;
    private boolean isoDateFormat;
    private String jsonFields;
    private String consoleFormat;
    private String jsonAccessLogFields;

    /**
     * @return the configured log directory
     */
    public String getLogDirectory() {
        return this.logDirectory;
    }

    /**
     * @param logDirectory new log directory
     */
    @XmlAttribute(name = "logDirectory")
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
    @XmlAttribute(name = "traceSpecification")
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
    @XmlAttribute(name = "traceFileName")
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
    @XmlAttribute(name = "maxFileSize")
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
     * @param maxCiles the new value for maximum number of files
     */
    @XmlAttribute(name = "maxFiles")
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
    @XmlAttribute(name = "traceFormat")
    public void setTraceFormat(String traceFormat) {
        this.traceFormat = ConfigElement.getValue(traceFormat);
    }

    /**
     * default="";
     *
     * @param jsonFields the jsonFields to set
     */
    @XmlAttribute(name = "jsonFieldMappings")
    public void setjsonFields(String jsonFields) {
        this.jsonFields = ConfigElement.getValue(jsonFields);
    }

    /**
     * @return the message fields
     */
    public String getjsonFields() {
        return this.jsonFields;
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
     * @param consoleFileName the consoleFileName to set
     */
    @XmlAttribute(name = "messageFileName")
    public void setMessageFileName(String messageFileName) {
        this.messageFileName = ConfigElement.getValue(messageFileName);
    }

    /**
     * default="AUDIT"; options= "INFO", "AUDIT", "WARNING", "ERROR"
     *
     * @param consoleLogLevel The level filter for messages sent to system out or system err.
     */
    @XmlAttribute(name = "consoleLogLevel")
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
    @XmlAttribute(name = "isoDateFormat")
    public void setIsoDateFormat(boolean isoDateFormat) {
        this.isoDateFormat = isoDateFormat;
    }

    /**
     * @return the boolean value set to use the ISO 8601 date format
     */
    public boolean getIsoDateFormat() {
        return this.isoDateFormat;
    }

    /**
     * default="dev"; options= "dev", "simple", "json"
     *
     * @param consoleFormat the consoleFormat to set
     */
    @XmlAttribute(name = "consoleFormat")
    public void setConsoleFormat(String consoleFormat) {
        this.consoleFormat = ConfigElement.getValue(consoleFormat);
    }

    /**
     * @return the consoleFormat
     */
    public String getConsoleFormat() {
        return this.consoleFormat;
    }

    /**
     * default="default"; options="logFormat"
     *
     * @param jsonAccessLogFields the value of jsonAccessLogFields configuration to set
     */
    @XmlAttribute(name = "jsonAccessLogFields")
    public void setJsonAccessLogFields(String jsonAccessLogFields) {
        this.jsonAccessLogFields = ConfigElement.getValue(jsonAccessLogFields);
    }

    /**
     * @return the value of the jsonAccessLogFields configuration attribute
     */
    public String getJsonAccessLogFields() {
        return this.jsonAccessLogFields;
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
