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
