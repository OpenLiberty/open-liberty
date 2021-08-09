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

/**
 * Information used to create the WAS dev content
 */
@XmlRootElement(name = "entries")
public class WasDevContentXml {

    private List<SampleWasDevItem> sampleItems;

    /**
     * @return the downloadItems
     */
    @XmlElement(name = "entry")
    public List<SampleWasDevItem> getDownloadItems() {
        if (this.sampleItems == null) {
            this.sampleItems = new ArrayList<SampleWasDevItem>();
        }
        return this.sampleItems;
    }

}
