/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedobject;

import java.io.Serializable;

/**
 * The context for creating a managed object.
 */
public interface ManagedObjectContext extends Serializable
{
    /**
     * Return context data about an object to be created. The available types
     * vary depending on the factory used to create the ManagedObject.
     *
     * @param klass the data type
     * @return the context data, or null if the data type is unrecognized
     */
    <T> T getContextData(Class<T> klass);

    /**
     * Release any resources associated with this state. This method must not
     * throw exceptions. If cleanup of the state results in an exception, it
     * must be ignored.
     */

    void release();
}
