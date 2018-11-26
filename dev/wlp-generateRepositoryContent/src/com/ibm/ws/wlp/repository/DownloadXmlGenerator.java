/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
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
