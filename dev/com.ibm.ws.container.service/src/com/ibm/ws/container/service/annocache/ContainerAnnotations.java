/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.container.service.annocache;

/**
 * Annotations data for a non-module container.
 */

// Used by:
//
// com.ibm.ws.app.manager.war/src/com/ibm/ws/app/manager/ear/internal/EARDeployedAppInfo.java
// -- used to discover when jar files have EJB annotations

/**
 * Annotations data for a single container.
 *
 * Container annotations
 *
 * Currently used to discover application JAR files which have EJB annotations.
 */
public interface ContainerAnnotations extends Annotations {
    /**
     * Answer the entry prefix of the single child class source of the
     * container annotations.  Answer null if the class source has no
     * entry prefix.
     * 
     * @return The entry prefix of the class source of the container
     *     annotations.
     */
    String getEntryPrefix();
    
    /**
     * Set the entry prefix of the single class source of the container
     * annotations.  This must be done before obtaining results from
     * the container annotations.
     *
     * @param entryPrefix The entry prefix of the class source of the
     *     container annotations. 
     */
    void setEntryPrefix(String entryPrefix);
}
