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

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

public class Bundles {
    @XmlElement(name = "bundle")
    private List<BundleFile> files;

    public Bundles() {
        //no-op
        //default constructor required by jaxb
    }

    public Bundles(List<BundleFile> files) {
        this.files = files;
    }

    public List<BundleFile> getBundleFiles() {
        return this.files;
    }
}
