/*
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
*/
package com.ibm.jbatch.jsl.model;

import java.util.List;

/**
 *
 */
public abstract class Listeners {

    /**
     * Gets the value of the listenerList property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the listenerList property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     *
     * <pre>
     * getListenerList().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Listener }
     *
     *
     */
    abstract public List<? extends Listener> getListenerList();

}