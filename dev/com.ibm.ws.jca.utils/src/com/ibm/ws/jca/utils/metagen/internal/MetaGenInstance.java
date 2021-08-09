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
package com.ibm.ws.jca.utils.metagen.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.Utils;
import com.ibm.ws.jca.utils.xml.metatype.Metatype;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;

/**
 * Holds one instances of the metatype generator...
 * An instance is defined as a combination of one ra.xml, wlp-ra.xml,
 * and the generated metatype.
 */
@Trivial
public class MetaGenInstance {
    public final XmlFileSet xmlFileSet = new XmlFileSet();
    public final String adapterName;
    public final Metatype metatype;
    private boolean isProcessed = false;
    private String moduleName;

    public MetaGenInstance(String adapterName, String rarFilePath) throws IOException {
        metatype = new Metatype();
        this.adapterName = adapterName;

        final File _rarFile = new File(rarFilePath);

        if (!_rarFile.getName().endsWith(".rar"))
            throw new IOException("The file " + _rarFile.getName() + " uses an unexpected file type. The expected file type is " + ".rar");

        if (!Utils.doesFileExistPrivileged(_rarFile))
            throw new FileNotFoundException(_rarFile.getAbsolutePath());

        xmlFileSet.rarFile = _rarFile;
    }

    public MetaGenInstance(String adapterName, String raXmlFilePath, String wlpRaXmlFilePath) throws IOException {
        metatype = new Metatype();
        this.adapterName = adapterName;

        File _raXmlFile = new File(raXmlFilePath);

        if (!_raXmlFile.getName().endsWith(InternalConstants.RA_XML_FILE_NAME))
            throw new IOException("Expecting " + InternalConstants.RA_XML_FILE_NAME + " but found " + _raXmlFile.getName());

        xmlFileSet.raXmlFile = _raXmlFile;

        if (wlpRaXmlFilePath == null)
            return;

        File _wlpRaXmlFile = new File(wlpRaXmlFilePath);
        if (!_wlpRaXmlFile.getName().endsWith(InternalConstants.WLP_RA_XML_FILE_NAME))
            throw new IOException("Expecting " + InternalConstants.WLP_RA_XML_FILE_NAME + " but found " + _wlpRaXmlFile.getName());

        xmlFileSet.wlpRaXmlFile = _wlpRaXmlFile;
    }

    public MetaGenInstance(String adapterName, RaConnector mergedConnector, String moduleName) throws IOException {
        metatype = new Metatype();
        this.adapterName = adapterName;
        xmlFileSet.parsedXml = mergedConnector;
        this.moduleName = moduleName;
    }

    public void markAsProcessed() {
        isProcessed = true;
    }

    public boolean isProcessed() {
        return isProcessed;
    }

    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MetaGenInstance{");
        sb.append("adapterName='").append(adapterName);
        sb.append("' isProcessed='").append(isProcessed);
        sb.append("' xmlFileSet=").append(xmlFileSet);
        sb.append('}');
        return sb.toString();
    }
}
