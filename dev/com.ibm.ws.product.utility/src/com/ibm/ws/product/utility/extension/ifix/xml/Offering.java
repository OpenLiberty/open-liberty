/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility.extension.ifix.xml;

import org.w3c.dom.Node;

/**
 * Representation of the &lt;offering&gt; XML element in an iFix XML file.
 */
public class Offering {

    public static Offering fromNode(Node n) {
        String id = n.getAttributes().getNamedItem("id") == null ? null : n.getAttributes().getNamedItem("id").getNodeValue();
        String tolerance = n.getAttributes().getNamedItem("tolerance") == null ? null : n.getAttributes().getNamedItem("tolerance").getNodeValue();
        return new Offering(id, tolerance);
    }

    private String id;

    private String tolerance;

    public Offering(String id, String tolerance) {
        this.id = id;
        this.tolerance = tolerance;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTolerance() {
        return this.tolerance;
    }

    public void setTolerance(String tolerance) {
        this.tolerance = tolerance;
    }
}
