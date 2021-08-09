/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.htod;

/**************************************************************************
 * HashtableAction.  This defines a callback interface for use in iterating
 *       the hashtable.  To use, implement the interface and pass it to
 *       HashtableOnDisk.iterateKeys or HashtableOnDisk.iterateObjects.  The
 *       is iterated and this callback is invoked for each object found.
 *
 * @param entry This is the HashtableEntry for the object found in the current
 *       iteration.
 *************************************************************************/
public interface HashtableAction
{
    public boolean execute(HashtableEntry entry) throws Exception;
}

