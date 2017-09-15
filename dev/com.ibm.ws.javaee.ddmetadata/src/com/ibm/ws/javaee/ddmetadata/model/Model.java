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
package com.ibm.ws.javaee.ddmetadata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The model for a single XML document, which correlates the XML elements and
 * attributes to Java interfaces and their accessor methods.
 */
public class Model {
    public static class Namespace {
        public static class Version {
            public final String string;
            public final int constant;

            public Version(String string, int constant) {
                this.string = string;
                this.constant = constant;
            }
        }

        public final String namespace;
        public final List<Version> versions = new ArrayList<Version>();

        public Namespace(String namespace) {
            this.namespace = namespace;
        }
    }

    public final ModelElement root;
    public final String adapterImplClassName;
    public final String parserImplClassName;
    public final List<Namespace> namespaces = new ArrayList<Namespace>();
    public String xmiNamespace;
    public int xmiVersion;

    /**
     * The name of the type of the primary deployment descriptor that is
     * referenced by XMI documents.
     */
    public String xmiPrimaryDDTypeName;

    /**
     * The versions returned by {@link #xmiPrimaryDDTypeName} that determine
     * whether the XMI format should be used.
     */
    public List<String> xmiPrimaryDDVersions = Collections.emptyList();

    /**
     * The name of the XMI reference element for this document.
     */
    public String xmiRefElementName;

    public Model(ModelElement root, String adapterImplClassName, String parserImplClassName) {
        this.root = root;
        this.adapterImplClassName = adapterImplClassName;
        this.parserImplClassName = parserImplClassName;
    }

    public ModelInterfaceType getRootType() {
        return (ModelInterfaceType) root.method.getType();
    }
}
