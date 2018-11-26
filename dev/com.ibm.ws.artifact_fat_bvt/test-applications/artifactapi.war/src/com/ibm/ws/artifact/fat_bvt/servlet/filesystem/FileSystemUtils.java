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

import java.util.ArrayList;

/**
 *
 */
public class FileSystemUtils {
    /**
     * add all the stuff in overlay, ontop of base, to make a new Fs to be returned.
     * 
     * @param base
     * @param overlay
     * @return
     */
    public static FileSystem merge(FileSystem node, FileSystem overlay) {
        FileSystem newNode = null;
        if (node.isRoot() ) {
            ArrayList<FileSystem> children = null;
            if (node.getChildren() != null) {
                children = new ArrayList<FileSystem>();
                //add the base set children, overriding where needed.
                for (FileSystem child : node.getChildren()) {
                    String name = child.isRoot() ? child.getNameAsEntry() : child.getName();
                    FileSystem nodeFromOverlay = overlay.getChildByName(name);
                    if (nodeFromOverlay == null) {
                        //overlay didnt know about this node, so clone the base one.
                        children.add(cloneNode(child));
                    } else {
                        //overlay did know about this node, so merge downward.
                        children.add(merge(child, nodeFromOverlay));
                    }
                }
            }
            if (overlay.getChildren() != null) {
                if (children == null) {
                    children = new ArrayList<FileSystem>();
                }
                //add in stuff that was only in overlay.
                for (FileSystem child : overlay.getChildren()) {
                    String name = child.isRoot() ? child.getNameAsEntry() : child.getName();
                    FileSystem nodeFromBase = node.getChildByName(name);
                    if (nodeFromBase == null) {
                        children.add(cloneNode(child));
                    }
                }
            }
            if (children != null) {
                FileSystem[] fs = children.toArray(new FileSystem[] {});
                newNode = FileSystem.root(node.getNameAsEntry(), node.getPathAsEntry(), node.getResource(), node.hasData(), node.getData(), node.getSize(),
                                  node.getPhysicalPath(),
                                  node.getUrlCount(), node.getURLs(),
                                  fs);
            } else {
                newNode = FileSystem.root(node.getNameAsEntry(), node.getPathAsEntry(), node.getResource(), node.hasData(), node.getData(), node.getSize(),
                                  node.getPhysicalPath(),
                                  node.getUrlCount(), node.getURLs()
                                  );
            }
        } else if (node.isDir() ) {
            ArrayList<FileSystem> children = null;
            if (node.getChildren() != null) {
                children = new ArrayList<FileSystem>();
                //add the base set children, overriding where needed.
                for (FileSystem child : node.getChildren()) {
                    String name = child.isRoot() ? child.getNameAsEntry() : child.getName();
                    FileSystem nodeFromOverlay = overlay.getChildByName(name);
                    if (nodeFromOverlay == null) {
                        //overlay didnt know about this node, so clone the base one.
                        children.add(cloneNode(child));
                    } else {
                        //overlay did know about this node, so merge downward.
                        children.add(merge(child, nodeFromOverlay));
                    }
                }
            }
            if (overlay.getChildren() != null) {
                if (children == null) {
                    children = new ArrayList<FileSystem>();
                }
                //add in stuff that was only in overlay.
                for (FileSystem child : overlay.getChildren()) {
                    String name = child.isRoot() ? child.getNameAsEntry() : child.getName();
                    FileSystem nodeFromBase = node.getChildByName(name);
                    if (nodeFromBase == null) {
                        children.add(cloneNode(child));
                    }
                }
            }
            if (children != null) {
                FileSystem[] fs = children.toArray(new FileSystem[] {});
                newNode = FileSystem.dir(node.getName(), node.getPath(), node.getResource(), node.getPhysicalPath(), node.getUrlCount(), node.getURLs(), fs);
            } else {
                newNode = FileSystem.dir(node.getName(), node.getPath(), node.getResource(), node.getPhysicalPath(), node.getUrlCount(), node.getURLs());
            }
        } else if (node.isFile() ) {
            newNode = FileSystem.File(node.getName(), node.getPath(), node.hasData(), node.getData(), node.getSize(), node.getResource(), node.getPhysicalPath());
        }
        return newNode;
    }

    public static FileSystem cloneNode(FileSystem node) {
        FileSystem newNode = null;
        if (node.isRoot() ) {
            if (node.getChildren() != null) {
                ArrayList<FileSystem> children = new ArrayList<FileSystem>();
                for (FileSystem child : node.getChildren()) {
                    children.add(cloneNode(child));
                }
                FileSystem[] fs = children.toArray(new FileSystem[] {});
                newNode = FileSystem.root( node.getNameAsEntry(), node.getPathAsEntry(), node.getResource(), node.hasData(), node.getData(), node.getSize(),
                                   node.getPhysicalPath(), node.getUrlCount(), node.getURLs(), fs );
            } else {
                newNode = FileSystem.root( node.getNameAsEntry(), node.getPathAsEntry(), node.getResource(), node.hasData(), node.getData(), node.getSize(),
                                   node.getPhysicalPath(), node.getUrlCount(), node.getURLs() );
            }
        } else if (node.isDir() ) {
            if (node.getChildren() != null) {
                ArrayList<FileSystem> children = new ArrayList<FileSystem>();
                for (FileSystem child : node.getChildren()) {
                    children.add(cloneNode(child));
                }
                FileSystem[] fs = children.toArray(new FileSystem[] {});
                newNode = FileSystem.dir(node.getName(), node.getPath(), node.getResource(), node.getPhysicalPath(), node.getUrlCount(), node.getURLs(), fs);
            } else {
                newNode = FileSystem.dir(node.getName(), node.getPath(), node.getResource(), node.getPhysicalPath(), node.getUrlCount(), node.getURLs());
            }
        } else if (node.isFile() ) {
            newNode = FileSystem.File(node.getName(), node.getPath(), node.hasData(), node.getData(), node.getSize(), node.getResource(), node.getPhysicalPath());
        }
        return newNode;
    }
}
