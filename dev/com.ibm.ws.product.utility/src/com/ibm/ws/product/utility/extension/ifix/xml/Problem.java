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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Representation of the &lt;problem&gt; XML element in an iFix XML file.
 */
public class Problem {

    public static List<Problem> fromNodeList(NodeList nl) {
        List<Problem> problems = new ArrayList<Problem>();
        for (int i = 0; i < nl.getLength(); i++) {
            Node n = nl.item(i);
            if (n.getNodeName().equals("problem")) {
                String id = n.getAttributes().getNamedItem("id") == null ? null : n.getAttributes().getNamedItem("id").getNodeValue();
                String displayId = n.getAttributes().getNamedItem("displayId") == null ? null : n.getAttributes().getNamedItem("displayId").getNodeValue();
                String description = n.getAttributes().getNamedItem("description") == null ? null : n.getAttributes().getNamedItem("description").getNodeValue();

                problems.add(new Problem(id, displayId, description));
            }
        }

        return problems.size() > 0 ? problems : null;
    }

    private String displayId;
    private String description;
    private String id;

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
    public void setId(String id) {
        this.id = id;
    }

}
