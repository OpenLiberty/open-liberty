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
package com.ibm.ws.runtime.metadata;

/**
 * Internal. This class is used for internal communication between the metadata service and {@link MetaDataImpl}, which is extended by containers.
 */
public class MetaDataSecrets {
    /**
     * Initializes the metadata id.
     */
    public static void setID(MetaDataImpl metaData, int id) {
        metaData.id = id;
    }

    /**
     * The id set by {@link #setID}, or -1 if never called.
     */
    public static int getID(MetaDataImpl metaData) {
        return metaData.id;
    }
}