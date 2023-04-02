/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.wsspi.adaptable.module;

/**
 * Provides the ability for an object implementing the interface to convert itself to another type of object.
 */
public interface Adaptable {
    /**
     * Adapt this object into an object of the passed type, if possible.
     * 
     * @param <T> The type to adapt to & return.
     * @param adaptTarget The type to adapt to.
     * @return instance of type <T> if successful, null otherwise.
     */
    <T> T adapt(Class<T> adaptTarget) throws UnableToAdaptException;
}
