/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.ddmetadata.generator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class ModelClassGenerator {
    private static String[] splitClassName(String className) {
        int index = className.lastIndexOf('.');
        return new String[] { className.substring(0, index), className.substring(index + 1) };
    }

    protected final File destdir;
    private final String packageName;
    final String simpleName;

    ModelClassGenerator(File destdir, String className) {
        this.destdir = destdir;
        String[] split = splitClassName(className);
        packageName = split[0];
        simpleName = split[1];
    }

    ModelClassGenerator(File destdir, String packageName, String simpleName) {
        this.destdir = destdir;
        this.packageName = packageName;
        this.simpleName = simpleName;
    }

    PrintWriter open() {
        File packageDir = new File(destdir, packageName.replace('.', '/'));

        if (!packageDir.mkdirs() && !packageDir.isDirectory()) {
            throw new IllegalStateException("Unable to create directory: " + packageDir);
        }

        File classFile = new File(packageDir, simpleName + ".java");
        System.out.println("Generating " + classFile);
        try {
            PrintWriter out = new PrintWriter(classFile, "UTF-8");
            out.println("// NOTE: This is a generated file. Do not edit it directly.");
            writePackageAnnotations(out);
            out.append("package ").append(packageName).append(";").println();
            out.println();
            return out;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected void writePackageAnnotations(PrintWriter out) {}
}
