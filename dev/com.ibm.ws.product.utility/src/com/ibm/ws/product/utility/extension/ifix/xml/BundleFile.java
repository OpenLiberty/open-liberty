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

public class BundleFile extends UpdatedFile {

    @XmlAttribute
    private String symbolicName;
    @XmlAttribute
    private String version;
    @XmlAttribute
    private boolean isBaseBundle;

    public BundleFile() {
        //required blank constructor for jaxb
    }

    public BundleFile(String id, long size, String date, String hash, String symbolicName, String version, boolean isBaseBundle) {
        super(id, size, date, hash);
        this.symbolicName = symbolicName;
        this.version = version;
        this.isBaseBundle = isBaseBundle;
    }

    public String getSymbolicName() {
        return this.symbolicName;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isBaseBundle() {
        return this.isBaseBundle;
    }
}
