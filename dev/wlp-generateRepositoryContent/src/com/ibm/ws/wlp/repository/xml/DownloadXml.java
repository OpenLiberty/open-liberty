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
package com.ibm.ws.wlp.repository.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "downloads")
public class DownloadXml {

    private List<DownloadItem> downloadItems;

    /**
     * @return the downloadItems
     */
    @XmlElement(name = "download")
    public List<DownloadItem> getDownloadItems() {
        if (this.downloadItems == null) {
            this.downloadItems = new ArrayList<DownloadItem>();
        }
        return this.downloadItems;
    }

}
