/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.application.loose;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents a single entry in a virtual archive XML document.
 */
public class VirtualEntry {

    private String targetInArchive;

    /**
     * No-argument constructor (required by JAXB)
     */
    public VirtualEntry() {
        super(); // required by JAXB
    }

    /**
     * Primary constructor for virtual entries
     * 
     * @param targetInArchive the virtual location of this entry in the parent archive
     */
    public VirtualEntry(String targetInArchive) {
        this.setTargetInArchive(targetInArchive);
    }

    /**
     * @return the virtual location of this entry in the parent archive
     */
    public String getTargetInArchive() {
        return targetInArchive;
    }

    /**
     * Defines the virtual location of this entry in the parent archive
     * 
     * @param targetInArchive the virtual location of this entry in the parent archive
     */
    @XmlAttribute
    public void setTargetInArchive(String targetInArchive) {
        this.targetInArchive = targetInArchive;
    }

}
