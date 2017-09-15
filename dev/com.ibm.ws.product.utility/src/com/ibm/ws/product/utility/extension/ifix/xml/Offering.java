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

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Representation of the &lt;offering&gt; XML element in an iFix XML file.
 */
public class Offering {

    private String id;

    private String tolerance;

    public Offering() {
        //required empty constructor for jaxb
    }

    public Offering(String id, String tolerance) {
        this.id = id;
        this.tolerance = tolerance;
    }

    public String getId() {
        return this.id;
    }

    @XmlAttribute
    public void setId(String id) {
        this.id = id;
    }

    public String getTolerance() {
        return this.tolerance;
    }

    @XmlAttribute
    public void setTolerance(String tolerance) {
        this.tolerance = tolerance;
    }
}
