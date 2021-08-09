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
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is a representation of an iFix XML file on disk
 */
public class IFixInfo implements MetadataOutput {

    public static IFixInfo fromDocument(Document doc) {
        if (doc == null)
            return null;

        Element e = doc.getDocumentElement();
        e.normalize();
        if (!"fix".equals(e.getNodeName()))
            return null;

        IFixInfo ifi = new IFixInfo(e.getAttribute("id"), e.getAttribute("version"));
        ifi.applicability = Applicability.fromNodeList(e.getElementsByTagName("applicability"));
        ifi.information = Information.fromNodeList(e.getElementsByTagName("information"));
        ifi.properties = Property.fromNodeList(e.getElementsByTagName("properties"));
        ifi.resolves = Resolves.fromNodeList(e.getElementsByTagName("resolves"));
        ifi.updates = Updates.fromNodeList(e.getElementsByTagName("updates"));

        return ifi;
    }

    private final String aparIdPrefix = "com.ibm.ws.apar.";

    //XmlAttribute - defined on setter below
    private String id;

    private final String version;

    private Applicability applicability;

    private final Categories categories = new Categories();//unused for now but we need it in xml as a self-closing element

    private Information information;

    private List<Property> properties;

    private Resolves resolves;

    private Updates updates;

    public IFixInfo(String id, String version) {
        this.id = id;
        this.version = version;
    }

    /**
     *
     * @param setId
     * @param setVersion
     * @param aparList
     * @param fixDescription
     * @param offerings
     * @param properties
     * @param includedJars
     */
    public IFixInfo(String setId, String setVersion, Set<String> aparList,
                    String fixDescription, ArrayList<Offering> offerings, List<Property> properties,
                    Set<UpdatedFile> updatedFiles) {
        //set id
        id = setId;
        //set version
        version = setVersion;
        //set applicability
        applicability = new Applicability(offerings);
        information = new Information(setId, setVersion, fixDescription);
        this.properties = properties;

        //create resolves list
        ArrayList<Problem> problems = new ArrayList<Problem>();
        if (aparList != null) {
            for (String apar : aparList) {
                String aparId = apar.toString();
                Problem myProblem = new Problem(aparIdPrefix + aparId, aparId, aparId);
                problems.add(myProblem);
            }
        }
        resolves = new Resolves(problems);

        updates = new Updates(updatedFiles);
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the resolves element from the XML or <code>null</code> if there isn't one
     */
    public Resolves getResolves() {
        return resolves;
    }

    /**
     * @return the updates from the XML or <code>null</code> if there isn't one
     */
    public Updates getUpdates() {
        return updates;
    }

    /**
     * @return the applicability element from the XML or <code>null</code> if there isn't one
     */
    public Applicability getApplicability() {
        return applicability;
    }

    /**
     * @return the properties element from the XML or <code>null</code> if there isn't one
     */
    public List<Property> getProperties() {
        return properties;
    }
}
