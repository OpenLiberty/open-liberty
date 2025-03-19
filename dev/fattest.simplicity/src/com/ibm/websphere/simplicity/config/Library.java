/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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

    private String apiTypeVisibility;
    private String description;
    private String fileRef;
    private String filesetRef;
    private String folderRef;
    private String pathRef;
    private String name;

    @XmlElement(name = "file")
    private ConfigElementList<File> files;

    @XmlElement(name = "fileset")
    private ConfigElementList<Fileset> filesets;

    @XmlElement(name = "folder")
    private ConfigElementList<Folder> folders;

    @XmlElement(name = "path")
    private ConfigElementList<Path> paths;

    @XmlAttribute(name = "apiTypeVisibility")
    public void setApiTypeVisibility(String apiTypeVisibility) {
        this.apiTypeVisibility = ConfigElement.getValue(apiTypeVisibility);
    }

    public String getApiTypeVisibility() {
        return this.apiTypeVisibility;
    }

    @XmlAttribute(name = "description")
    public void setDescription(String description) {
        this.description = ConfigElement.getValue(description);
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * @param fileRef the ID of the file that this library uses
     */
    @XmlAttribute(name = "fileRef")
    public void setFileRef(String fileRef) {
        this.fileRef = ConfigElement.getValue(fileRef);
    }

    /**
     * Convenience method for setting fileRef from a file
     */
    @XmlTransient
    public void setFile(File file) {
        if (file != null) {
            this.setFileRef(file.getId());
        }
    }

    /**
     * @return the ID of the file that this library uses
     */
    public String getFileRef() {
        return this.fileRef;
    }

    /**
     * @param filesetRef the ID of the fileset that this library uses
     */
    @XmlAttribute(name = "filesetRef")
    public void setFilesetRef(String filesetRef) {
        this.filesetRef = ConfigElement.getValue(filesetRef);
    }

    /**
     * Convenience method for setting filesetRef from a fileset
     */
    @XmlTransient
    public void setFileset(Fileset fileset) {
        if (fileset != null) {
            this.setFilesetRef(fileset.getId());
        }
    }

    /**
     * @return the ID of the fileset that this library uses
     */
    public String getFilesetRef() {
        return this.filesetRef;
    }

    /**
     * @param folderRef the ID of the folder that this library uses
     */
    @XmlAttribute(name = "folderRef")
    public void setFolderRef(String folderRef) {
        this.folderRef = ConfigElement.getValue(folderRef);
    }

    /**
     * Convenience method for setting folderRef from a folder
     */
    @XmlTransient
    public void setFolder(Folder folder) {
        if (folder != null) {
            this.setFolderRef(folder.getId());
        }
    }

    /**
     * @param pathRef the ID of the path that this library uses
     */
    @XmlAttribute(name = "pathRef")
    public void setPathRef(String pathRef) {
        this.pathRef = ConfigElement.getValue(pathRef);
    }

    /**
     * Convenience method for setting pathRef from a path
     */
    @XmlTransient
    public void setPath(Path path) {
        if (path != null) {
            this.setPathRef(path.getId());
        }
    }

    /**
     * @return the ID of the folder that this library uses
     */
    public String getFolderRef() {
        return this.folderRef;
    }

    /**
     * @return the ID of the path that this library uses
     */
    public String getPathRef() {
        return this.pathRef;
    }

    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = ConfigElement.getValue(name);
    }

    public String getName() {
        return this.name;
    }

    public ConfigElementList<File> getFiles() {
        if (this.files == null) {
            this.files = new ConfigElementList<File>();
        }
        return this.files;
    }

    public ConfigElementList<Fileset> getFilesets() {
        if (this.filesets == null) {
            this.filesets = new ConfigElementList<Fileset>();
        }
        return this.filesets;
    }

    public ConfigElementList<Folder> getFolders() {
        if (this.folders == null) {
            this.folders = new ConfigElementList<Folder>();
        }
        return this.folders;
    }

    public ConfigElementList<Path> getPaths() {
        if (this.paths == null) {
            this.paths = new ConfigElementList<Path>();
        }
        return this.paths;
    }

    /**
     * Legacy method for returning a single nested file.
     *
     * @deprecated library may contain more than one nested file; use {@link #getFiles}.
     *
     * @return     the nested file
     */
    @Deprecated
    public File getNestedFile() {
        if (files != null && files.size() > 0) {
            if (files.size() == 1) {
                return files.get(0);
            }
            throw new IllegalStateException("library contains more than one nested file");
        }
        return null;
    }

    /**
     * Legacy method for setting a single nested file.
     *
     * @deprecated     library may contain more than one nested file; use {@link #getFiles}.
     *
     * @param      the nested file
     */
    @Deprecated
    @XmlTransient
    public void setNestedFile(File file) {
        if (files != null && files.size() > 1) {
            throw new IllegalStateException("library contains more than one nested file");
        }
        if (file == null) {
            files = null;
        } else {
            if (files == null) {
                files = new ConfigElementList<File>();
            } else {
                files.clear();
            }
            files.add(file);
        }
    }

    /**
     * Legacy method for returning a single nested fileset.
     *
     * @deprecated library may contain more than one nested fileset; use {@link #getFilesets}
     *
     * @return     the nested fileset
     */
    @Deprecated
    public Fileset getNestedFileset() {
        if (filesets != null && filesets.size() > 0) {
            if (filesets.size() == 1) {
                return filesets.get(0);
            }
            throw new IllegalStateException("library contains more than one nested fileset");
        }
        return null;
    }

    /**
     * Legacy method for setting a single nested fileset.
     *
     * @deprecated     library may contain more than one nested fileset; use {@link #getFiles}.
     *
     * @param      the nested fileset
     */
    @Deprecated
    @XmlTransient
    public void setNestedFileset(Fileset fileset) {
        if (filesets != null && filesets.size() > 1) {
            throw new IllegalStateException("library contains more than one nested fileset");
        }
        if (fileset == null) {
            filesets = null;
        } else {
            if (filesets == null) {
                filesets = new ConfigElementList<Fileset>();
            } else {
                filesets.clear();
            }
            filesets.add(fileset);
        }
    }

    /**
     * Legacy method for returning a single nested folder.
     *
     * @deprecated library may contain more than one nested folder; use {@link #getFolders}
     *
     * @return     the nested folder
     */
    @Deprecated
    @XmlTransient
    public Folder getNestedFolder() {
        if (folders != null && folders.size() > 0) {
            if (folders.size() == 1) {
                return folders.get(0);
            }
            throw new IllegalStateException("library contains more than one nested folder");
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("Library{");
        if (getId() != null)
            buf.append("id=\"" + getId() + "\" ");
        if (apiTypeVisibility != null)
            buf.append("apiTypeVisibility=\"" + apiTypeVisibility + "\" ");
        if (description != null)
            buf.append("description=\"" + description + "\" ");
        if (fileRef != null)
            buf.append("fileRef=\"" + fileRef + "\" ");
        if (filesetRef != null)
            buf.append("filesetRef=\"" + filesetRef + "\" ");
        if (folderRef != null)
            buf.append("folderRef=\"" + folderRef + "\" ");
        if (name != null)
            buf.append("name=\"" + name + "\" ");
        if (files != null)
            buf.append(files).append(' ');
        if (filesets != null)
            buf.append(filesets).append(' ');
        if (folders != null)
            buf.append(folders).append(' ');
        buf.append("}");

        return buf.toString();
    }
}
