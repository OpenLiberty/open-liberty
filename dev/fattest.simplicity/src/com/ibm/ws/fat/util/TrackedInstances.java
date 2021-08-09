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
package com.ibm.ws.fat.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Caches the output of /com.ibm.ws.cdi_fat/test-applications/basicEjbInWar.war/resources/showInstances.jsp
 */
public class TrackedInstances {

    private final List<String> constructed;
    private final List<String> destroyed;
    private final List<String> living;
    private final List<String> dead;

    /**
     * Parses the output of /com.ibm.ws.cdi_fat/test-applications/basicEjbInWar.war/resources/showInstances.jsp
     * 
     * @param document the HTML returned from a request to showInstances.jsp
     */
    public TrackedInstances(Document document) {
        List<String> constructed = new ArrayList<String>();
        List<String> destroyed = new ArrayList<String>();
        List<String> living = new ArrayList<String>();
        List<String> dead = new ArrayList<String>();
        if (document != null) {
            NodeList orderedLists = document.getElementsByTagName("ol");
            if (orderedLists != null) {
                int numOrderedLists = orderedLists.getLength();
                for (int olIndex = 0; olIndex < numOrderedLists; olIndex++) {
                    Element orderedList = (Element) orderedLists.item(olIndex);
                    if (orderedList == null) {
                        continue;
                    }
                    NodeList spans = orderedList.getElementsByTagName("span");
                    if (spans != null && spans.getLength() > 0) {
                        Element span = (Element) spans.item(0);
                        if (span == null) {
                            continue;
                        }
                        String spanText = getText(span);
                        List<String> activeList = null;
                        if ("Constructed Instances:".equals(spanText)) {
                            activeList = constructed;
                        } else if ("Destroyed Instances:".equals(spanText)) {
                            activeList = destroyed;
                        } else if ("Living Instances:".equals(spanText)) {
                            activeList = living;
                        } else if ("Dead Instances:".equals(spanText)) {
                            activeList = dead;
                        }
                        NodeList listItems = orderedList.getElementsByTagName("li");
                        if (activeList == null || listItems == null) {
                            continue;
                        }
                        int numItems = listItems.getLength();
                        for (int liIndex = 0; liIndex < numItems; liIndex++) {
                            Element listItem = (Element) listItems.item(liIndex);
                            if (listItem == null) {
                                continue;
                            }
                            String listItemText = getText(listItem);
                            if (listItemText == null) {
                                continue;
                            }
                            activeList.add(listItemText);
                        }
                    }
                }
            }
        }
        this.constructed = Collections.unmodifiableList(constructed);
        this.destroyed = Collections.unmodifiableList(destroyed);
        this.living = Collections.unmodifiableList(living);
        this.dead = Collections.unmodifiableList(dead);
    }

    private static String getText(Node node) {
        NodeList children = node.getChildNodes();
        for (int k = 0; k < children.getLength(); k++) {
            Node child = children.item(k);
            if (child.getNodeType() == Node.TEXT_NODE) {
                return child.getNodeValue();
            }
        }
        return null;
    }

    /**
     * @return the constructed
     */
    public List<String> getConstructed() {
        return this.constructed;
    }

    /**
     * @return the destroyed
     */
    public List<String> getDestroyed() {
        return this.destroyed;
    }

    /**
     * @return the living
     */
    public List<String> getLiving() {
        return this.living;
    }

    /**
     * @return the dead
     */
    public List<String> getDead() {
        return this.dead;
    }

}
