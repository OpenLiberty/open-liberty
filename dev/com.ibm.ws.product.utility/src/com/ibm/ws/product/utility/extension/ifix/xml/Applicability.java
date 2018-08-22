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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NodeList;

/**
 * Representation of the &lt;applicability&gt; XML element in an iFix XML file.
 */
public class Applicability {

    public static Applicability fromNodeList(NodeList nl) {
        List<Offering> offerings = new ArrayList<Offering>();

        for (int i = 0; i < nl.getLength(); i++) //Applicability Elements
            for (int j = 0; j < nl.item(i).getChildNodes().getLength(); j++) //Offering Elements
                if (nl.item(i).getChildNodes().item(j).getNodeName().equals("offering"))
                    offerings.add(Offering.fromNode(nl.item(i).getChildNodes().item(j)));

        return new Applicability(offerings);
    }

    private final List<Offering> offerings;

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
