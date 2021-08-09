package com.ibm.ws.sib.msgstore.persistence;
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

import com.ibm.ws.sib.msgstore.PersistenceException;

/**
 * A unique key generator, probably backed by a persistent mechanism as we need
 * unique keys across server restarts. The values returned are most likely in
 * sequence, but that is not guaranteed. 
 * 
 * @author kschloss
 * @author pradine
 */
public interface UniqueKeyGenerator
{
    /**
     * 
     * @return The unique name of the generator.
     */
    public String getName();

    /**
     * 
     * @return The size of a range of unique id's.
     */
    public long getRange();

    /**
     * Returns a unique numeric value across the entire life of the message store.
     * At least until the <code>long</code> value wraps around.
     * 
     * @return a value which is unique across the entire life of the
     * message store
     * 
     */
    public long getUniqueValue() throws PersistenceException;

    /**
     * Returns a unique value that is unique only to the current instance of the
     * <code>UniqueKeyGenerator</code>
     * 
     * @return A value which is unique to the current instance of the unique
     * key generator
     */
    public long getPerInstanceUniqueValue();
}
