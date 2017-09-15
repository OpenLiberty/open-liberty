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
 * Represents a single directory entry in a virtual archive XML document.
 */
public class DirectoryEntry extends FileEntry {
    protected String excludes;

    /**
     * No-argument constructor (required by JAXB)
     */
    public DirectoryEntry() {
        super(); // required by JAXB
    }

    /**
     * Primary constructor for directory entries
     * 
     * @param sourceOnDisk the physical location of this entry in the file system
     * @param targetInArchive the virtual location of this entry in the parent archive
     */
    public DirectoryEntry(String sourceOnDisk, String targetInArchive) {
        super(sourceOnDisk, targetInArchive);
    }

    /**
     * Supplementary constructor for directory entries
     * 
     * @param sourceOnDisk the physical location of this entry in the file system
     * @param excludes entries to exclude from <code>sourceOnDisk</code> in the virtual archive
     * @param targetInArchive the virtual location of this entry in the parent archive
     */
    public DirectoryEntry(String sourceOnDisk, String excludes, String targetInArchive) {
        super(sourceOnDisk, targetInArchive);
        this.excludes = excludes;
    }

    /**
     * @return entries to exclude from <code>sourceOnDisk</code> in the virtual archive
     */
    public String getExcludes() {
        return excludes;
    }

    /**
     * Defines an exclusion filter for <code>sourceOnDisk</code>
     * 
     * @param excludes entries to exclude from <code>sourceOnDisk</code> in the virtual archive
     */
    @XmlAttribute
    public void setExcludes(String excludes) {
        this.excludes = excludes;
    }

}
