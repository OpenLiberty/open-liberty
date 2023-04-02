/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.mongo;

/**
 * Interface to allow Mongo components to communicate that some sort of change has occurred and that any existing connections should be invalidated.
 */
public interface MongoChangeListener {

    /**
     * Fired when a change occurs that requires existing DB connections to be invalidated.
     */
    void changeOccurred();

}
