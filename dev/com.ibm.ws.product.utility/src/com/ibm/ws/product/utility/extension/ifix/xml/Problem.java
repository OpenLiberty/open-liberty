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
 * Representation of the &lt;problem&gt; XML element in an iFix XML file.
 */
public class Problem {

    private String displayId;
    private String description;
    private String id;

    public Problem() {
        //needed as Jaxb needs a blank constructor
    }

    public Problem(String setId, String setDisplayId, String setDescription) {
        id = setId;
        displayId = setDisplayId;
        description = setDescription;
    }

    /**
     * @return the displayId
     */
    public String getDisplayId() {
        return displayId;
    }

    /**
     * @param displayId the displayId to set
     */
    @XmlAttribute
    public void setDisplayId(String displayId) {
        this.displayId = displayId;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    @XmlAttribute
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    @XmlAttribute
    public void setId(String id) {
        this.id = id;
    }

}
