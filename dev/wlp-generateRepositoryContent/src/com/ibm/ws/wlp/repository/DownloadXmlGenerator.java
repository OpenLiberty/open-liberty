/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.wlp.repository;

import java.io.File;

import org.apache.tools.ant.Task;

import com.ibm.ws.wlp.repository.xml.DownloadXml;

/**
 * This abstract class provides a base class for creating ANT tasks that writes content to the download XML file. It does not implement the {@link Task#execute()} but provides
 * utility methods for working with the download XML.
 */
public abstract class DownloadXmlGenerator extends Task {

    /** The file to save download xml information to */
    private File downloadXmlFile;

    private final XmlGenerator<DownloadXml> xmlGenerator;

    public DownloadXmlGenerator() {
        this.xmlGenerator = new XmlGenerator<DownloadXml>(DownloadXml.class, this);
    }

    /**
     * If the {@link #downloadXmlFile} property has been set then this will parse it and return the {@link DownloadXml} for the content of that file. If the file does not exist a
     * new {@link DownloadXml} will be returned. Will return <code>null</code> if the property is not set.
     * 
     * @return The {@link DownloadXml} or <code>null</code> if no file was supplied
     */
    protected DownloadXml parseDownloadXml() {
        return this.xmlGenerator.parseXml(this.downloadXmlFile);
    }

    /**
     * This will write the download XML to the {@link #downloadXmlFile}.
     * 
     * @param downloadXml The {@link DownloadXml} to write
     */
    protected void writeDownloadXml(DownloadXml downloadXml) {
        this.xmlGenerator.writeXml(downloadXml, this.downloadXmlFile);
    }

    public String getDownloadXmlFile() {
        return this.downloadXmlFile != null ? this.downloadXmlFile.getAbsolutePath() : null;
    }

    public void setDownloadXmlFile(String downloadXmlFilePath) {
        if (downloadXmlFilePath != null && !downloadXmlFilePath.isEmpty()) {
            this.downloadXmlFile = new File(downloadXmlFilePath);
        } else {
            this.downloadXmlFile = null;
        }
    }

}
