/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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

public class UpdatedFile {
    @XmlAttribute
    private String id;
    @XmlAttribute
    private long size;
    @XmlAttribute
    private String hash;
    @XmlAttribute
    private String date;

    public UpdatedFile() {
        //required blank constructor for jaxb
    }

    public UpdatedFile(String id, long size, String date, String hash) {
        this.id = id;
        this.size = size;
        this.date = date;
        this.hash = hash;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @return the hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * @return the date
     */
    public String getDate() {
        return date;
    }

}
