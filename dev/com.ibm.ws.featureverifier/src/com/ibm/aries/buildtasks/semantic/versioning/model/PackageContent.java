/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.aries.buildtasks.semantic.versioning.model;

import java.util.HashMap;
import java.util.Map;

import com.ibm.aries.buildtasks.semantic.versioning.model.decls.ClassDeclaration;

public class PackageContent {
    private final PkgInfo pkgInfo;
    private final Map<String, ClassDeclaration> classes = new HashMap<String, ClassDeclaration>();
    private final Map<String, String> xsds = new HashMap<String, String>();

    PackageContent(PkgInfo p) {
        pkgInfo = p;
    }

    public void addClass(String className, ClassDeclaration cd) {
        classes.put(className, cd);
    }

    public void addXsd(String className, String xsdHash) {
        xsds.put(className, xsdHash);
    }

    public Map<String, ClassDeclaration> getClasses() {
        return classes;
    }

    public Map<String, String> getXsds() {
        return xsds;
    }

    public PkgInfo getPackage() {
        return pkgInfo;
    }

    @Override
    public String toString() {
        return "" + pkgInfo + " xsds:" + xsds.size() + " classes:" + classes.size();
    }
}