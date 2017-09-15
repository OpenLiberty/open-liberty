/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * A shared library (which is a set of reusable class files)
 * 
 * @author Tim Burns
 * 
 */
public class Library extends ConfigElement {

    private String filesetRef;
    private Fileset nestedFileset;

    private String fileRef;
    private File nestedFile;

    private String folderRef;
    private Folder nestedFolder;

    private String apiTypeVisibility;

    /**
     * @return the ID of the fileset that this library uses
     */
    public String getFilesetRef() {
        return this.filesetRef;
    }

    /**
     * @param filesetRef the ID of the fileset that this library uses
     */
    @XmlAttribute
    public void setFilesetRef(String filesetRef) {
        this.filesetRef = ConfigElement.getValue(filesetRef);
    }

    /**
     * @return the ID of the file that this library uses
     */
    public String getFileRef() {
        return this.fileRef;
    }

    /**
     * @param fileRef the ID of the file that this library uses
     */
    @XmlAttribute
    public void setFileRef(String fileRef) {
        this.fileRef = ConfigElement.getValue(fileRef);
    }

    /**
     * @return the ID of the folder that this library uses
     */
    public String getFolderRef() {
        return this.folderRef;
    }

    /**
     * @param folderRef the ID of the folder that this library uses
     */
    @XmlAttribute
    public void setFolderRef(String folderRef) {
        this.folderRef = ConfigElement.getValue(folderRef);
    }

    @XmlTransient
    public void setFileset(Fileset fileset) {
        if (fileset != null) {
            this.setFilesetRef(fileset.getId());
        }
    }

    @XmlElement(name = "fileset")
    public void setNestedFileset(Fileset fileset) {
        this.nestedFileset = fileset;
    }

    @XmlTransient
    public void setFile(File file) {
        if (file != null) {
            this.setFileRef(file.getId());
        }
    }

    @XmlElement(name = "file")
    public void setNestedFile(File file) {
        this.nestedFile = file;
    }

    @XmlTransient
    public void setFolder(Folder folder) {
        if (folder != null) {
            this.setFolderRef(folder.getId());
        }
    }

    @XmlElement(name = "folder")
    public void setNestedFolder(Folder folder) {
        this.nestedFolder = folder;
    }

    public Fileset getNestedFileset() {
        return this.nestedFileset;
    }

    public File getNestedFile() {
        return this.nestedFile;
    }

    public Folder getNestedFolder() {
        return this.nestedFolder;
    }

    @XmlAttribute(name = "apiTypeVisibility")
    public void setApiTypeVisibility(String apiTypes) {
        this.apiTypeVisibility = apiTypes;
    }

    public String getApiTypeVisibility() {
        return this.apiTypeVisibility;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Library{");
        buf.append("id=\"" + (getId() == null ? "" : getId()) + "\" ");
        buf.append("filesetRef=\"" + (filesetRef == null ? "" : filesetRef) + "\" ");
        buf.append("fileRef=\"" + (fileRef == null ? "" : fileRef) + "\" ");
        buf.append("folderRef=\"" + (folderRef == null ? "" : folderRef) + "\" ");
        buf.append("apiTypeVisibility=\"" + (apiTypeVisibility == null ? "" : apiTypeVisibility) + "\" ");
        if (nestedFileset != null)
            buf.append(nestedFileset.toString());
        if (nestedFile != null)
            buf.append(nestedFile.toString());
        if (nestedFolder != null)
            buf.append(nestedFolder.toString());
        buf.append("}");

        return buf.toString();
    }
}
