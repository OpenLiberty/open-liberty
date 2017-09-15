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
package com.ibm.ws.jndi.internal;

import java.util.LinkedList;

/**
 * This class represents a special leaf node in the JNDI Context that represents
 * entries corresponding to services registered with the property osgi.jndi.service.
 * name. This node is created by the JNDIServiceBinder when adding such services to
 * the registry
 * 
 */
public class AutoBindNode {

    /**
     * This field represents the latest entry in the AutoBindNode. If its null it means
     * that the AutoBindNode is empty and can be removed.
     */
    private volatile Object single;

    /**
     * This field contains entries in the AutoBindNode. If its null it means that there is
     * only one or less entries in the AutoBindNode.
     */
    private LinkedList<Object> multiple;

    AutoBindNode(Object single) {
        this.single = single;
    }

    /**
     * This method is used to add an entry to the AutoBindNode
     * Note that the node can contain multiple entries. This method
     * is not thread safe and should be externally synchronized such that
     * other modifications do not happen concurrently on another thread.
     * 
     * @param entry The entry to add
     */
    void addLastEntry(Object entry) {
        if (multiple == null) {
            if (!single.equals(entry)) {
                multiple = new LinkedList<Object>();
                multiple.addLast(single);
                multiple.addLast(entry);
            }
        } else {
            if (!multiple.contains(entry)) {
                multiple.addLast(entry);
            }
        }
        single = entry;
    }

    /**
     * This method is used to remove an entry from the AutoBindNode.
     * This method is not thread safe and should be externally synchronized
     * such that other modifications do not happen concurrently on another
     * thread.
     * 
     * @param entry The entry to remove
     * @return boolean representing whether the AutoBindNode is empty and
     *         can be removed
     */
    boolean removeEntry(Object entry) {
        if (multiple == null) {
            if (entry.equals(single)) {
                single = null;
                return true;
            }
        } else {
            multiple.remove(entry);
            if (single.equals(entry)) {
                single = multiple.peekLast();
            }
            if (multiple.size() == 1) {
                multiple = null;
            }
        }
        return false;
    }

    /**
     * Gets the last added entry. This method need not be
     * 
     * @return the last added entry.
     */
    Object getLastEntry() {
        return single;
    }
}
