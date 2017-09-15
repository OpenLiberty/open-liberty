/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.managedobject;

/**
 * DefaultManagedObjectService is a marker interface that provides a mechanism to obtain
 * a reference to the default implementation of the {@link ManagedObjectService). <p>
 *
 * This is useful for implementations of ManagedObjectService that would like to delegate
 * method calls to the default implementation (for example, when the CDI provided
 * ManagedObjectService does not need to manage a particular object).
 */
public interface DefaultManagedObjectService extends ManagedObjectService {
    // No additional methods are provided
}
