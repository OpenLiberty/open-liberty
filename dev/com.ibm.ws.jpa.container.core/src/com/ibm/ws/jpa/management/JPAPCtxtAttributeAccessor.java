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
package com.ibm.ws.jpa.management;

import javax.persistence.PersistenceContext;

/**
 * Accessor for annotation attributes that were added after JPA 2.0.
 */
public class JPAPCtxtAttributeAccessor {
    /**
     * Returns true if PersistenceContext.serialization() is
     * SynchronizationType.UNSYNCHRONIZED.
     */
    public boolean isUnsynchronized(PersistenceContext pCtxt) {
        return false;
    }
}
