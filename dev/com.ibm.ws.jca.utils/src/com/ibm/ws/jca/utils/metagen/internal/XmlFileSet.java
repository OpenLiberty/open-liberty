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

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpRaConnector;

/**
 * Used to hold RAR, ra.xml, wlp-ra.xml, and so on files for
 * a generator instance.
 */
@Trivial
public class XmlFileSet {
    public File rarFile;
    public File raXmlFile;
    public File wlpRaXmlFile;
    public RaConnector parsedXml;
    public WlpRaConnector parsedWlpXml;
    public String rarRaXmlFilePath;
    public String rarWlpRaXmlFilePath;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("XmlFileSet{");

        if (rarFile != null)
            sb.append("rarFile='").append(rarFile.getAbsolutePath()).append("' ");
        if (raXmlFile != null)
            sb.append("raXmlFile='").append(raXmlFile.getAbsolutePath()).append("' ");
        if (wlpRaXmlFile != null)
            sb.append("wlpRaXmlFile='").append(wlpRaXmlFile.getAbsolutePath()).append("' ");
        if (rarRaXmlFilePath != null)
            sb.append("rarRaXmlFilePath='").append(rarRaXmlFilePath).append("' ");
        if (rarWlpRaXmlFilePath != null)
            sb.append("wlpRaXmlFilePath='").append(rarWlpRaXmlFilePath).append("'");

        sb.append('}');
        return sb.toString();
    }
}
