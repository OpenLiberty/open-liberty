/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.servlet.filesystem;

/**
 * class to assist in building verification for artifact api.
 */
public class FileSystem {

    public static FileSystem root(
        String nameAsEntry, String pathAsEntry,
        String resourceAsEntry,
        boolean hasData, String data, long size,
        String physicalPath,
        int uriCount, String[] uris,
        FileSystem... children) {

        return new FileSystem( FsType.ROOT,
                       "/", "/",
                       nameAsEntry, pathAsEntry,
                       null, physicalPath,
                       uriCount, uris,
                       hasData, data, size,
                       children);
    }

    public static FileSystem dir(
        String name, String path,
        String resource, String physicalPath,
        int uriCount, String[] uris,
        FileSystem... children) {

        return new FileSystem( FsType.DIR,
                       name, path,
                       null, null,
                       resource, physicalPath,
                       uriCount, uris,
                       DOES_NOT_HAVE_DATA, null, 0,
                       children);
    }

    public static FileSystem File(
        String name, String path,
        boolean hasData, String data, long size,
        String resource, String physicalPath) {

        return new FileSystem( FsType.FILE,
                       name, path,
                       null, null,
                       resource, physicalPath,
                       -1, null,
                       hasData, data, size);
    }

    //

    public String getDebugPath() {
        String debugPath = getPath();

        FileSystem rootNode = this;
        while ( rootNode != null ) {
            while ( !rootNode.isRoot() ) {
                rootNode = rootNode.getParent();
            }

            if ( rootNode.getPathAsEntry() != null ) {
                debugPath = rootNode.getPathAsEntry() + "#" + debugPath;
            } else {
                debugPath = "#" + debugPath;
            }

            rootNode = rootNode.getParent();
        }

        return debugPath;
    }

    //

    private FileSystem parent;

    public FileSystem getParent() {
        return parent;
    }

    private void setParent(FileSystem parent) {
        this.parent = parent;
    }

    //

    private final FileSystem[] children;

    public FileSystem getChildByName(String targetChildName) {
        if ( children == null ) {
            return null;
        }

        for ( FileSystem nextChild : children ) {
            String nextChildName;
            if ( nextChild.isRoot() ) {
                nextChildName = nextChild.getNameAsEntry();
            } else {
                nextChildName = nextChild.getName();
            }

            if ( nextChildName.equals(targetChildName) ) {
                return nextChild;
            }
        }

        return null;
    }

    public FileSystem[] getChildren() {
        return children;
    }

    //

    private final FsType type;

    public FsType getType() {
        return type;
    }

    public boolean isRoot() {
        return getType().isRoot();
    }

    public boolean isDir() {
        return getType().isDir();
    }

    public boolean isFile() {
        return getType().isFile();
    }

    public boolean isContainer() {
        return ( isRoot() || isDir() );
    }

    //

    private final String name;
    private final String path;

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    //

    private final String nameAsEntry;
    private final String pathAsEntry;

    public String getNameAsEntry() {
        return nameAsEntry;
    }

    public String getPathAsEntry() {
        return pathAsEntry;
    }

    //

    private final String resource;
    private final String physicalPath;

    public String getResource() {
        return resource;
    }

    public String getPhysicalPath() {
        return physicalPath;
    }

    //

    private final int urlCount;
    private final String[] urls;

    public int getUrlCount() {
        return urlCount;
    }

    public String[] getURLs() {
        return urls;
    }

    //

    private final boolean hasData;
    private final String data;
    private final long size;

    public boolean hasData() {
        return hasData;
    }

    public String getData() {
        return data;
    }

    public long getSize() {
        return size;
    }

    //

    public static enum FsType {
        ROOT, DIR, FILE;

        public boolean isRoot() {
            return ( this == ROOT );
        }

        public boolean isDir() {
            return ( this == DIR );
        }

        public boolean isFile() {
            return ( this == FILE );
        }
    }

    public static final boolean DOES_HAVE_DATA = true;
    public static final boolean DOES_NOT_HAVE_DATA = false;

    private FileSystem( FsType type,
                String name, String path,
                String nameAsEntry, String pathAsEntry,
                String resource, String physicalPath,
                int urlCount, String[] urls,
                boolean hasData, String data, long size, FileSystem... children) {

        this.parent = null;

        this.type = type;

        this.name = name;
        this.path = path;

        this.nameAsEntry = nameAsEntry;
        this.pathAsEntry = pathAsEntry;

        this.resource = resource;
        this.physicalPath = physicalPath;

        this.urlCount = urlCount;
        this.urls = urls;

        this.hasData = hasData;
        this.data = data;
        this.size = size;

        this.children = children;
        if ( this.children != null ) {
            for (FileSystem nextChild : children ) {
                nextChild.setParent(this);
            }
        }
    }
}
