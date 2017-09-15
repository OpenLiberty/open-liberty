/*******************************************************************************
 * Copyright (c) 2015, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer;

import com.ibm.ws.managedobject.ManagedObjectContext;

/**
 * A factory for creating client references for a specific EJB.
 */
public interface EJBReferenceFactory {
    /**
     * Creates a client reference to an EJB. For stateful EJBs, this will also
     * create a backing instance that must be removed via {@link EJBReference#remote}.
     *
     * @param context the managed object context
     * @return the client reference
     */
    EJBReference create(ManagedObjectContext context);
}
