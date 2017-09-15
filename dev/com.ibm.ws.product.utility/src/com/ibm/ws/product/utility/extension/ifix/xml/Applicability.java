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

/**
 * Representation of the &lt;applicability&gt; XML element in an iFix XML file.
 */
public class Applicability {

    @XmlElement(name = "offering")
    private List<Offering> offerings;

    public Applicability() {
        //blank constructor required for jaxb to work
    }

    public Applicability(List<Offering> offeringList) {
        offerings = offeringList;
    }

    /**
     * @return the offerings in this applicability element or <code>null</code> if there aren't any
     */
    public List<Offering> getOfferings() {
        return offerings;
    }
}
