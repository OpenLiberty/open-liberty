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

/*************************************************************************
 * HashtableInterface.  This interface is used during initialization of the
 *      hashtable if the hashtable is being initialized from disk and the
 *      hashtable was not properly closed.  Applications that extend the 
 *      HashtableOnDisk or logically extend it via embedding may need to
 *      do their own cleanup also.   Part of recovery is to iterate the
 *      hashtable, passing each key and object to this interface.
 *
 * Note.  This is really old.  I wonder if we should be using the 
 * HashtableAction instead.
 *      
 *************************************************************************/
public interface HashtableInitInterface
{
    void initialize(Object key, Object value);
}
