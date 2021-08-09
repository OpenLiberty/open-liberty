package com.ibm.ws.objectManager;

/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * Thrown when an attempt is made to use a logFile that is already being used by another program.
 * 
 * @param ObjectStore throwing the exception.
 * @param String naming the file backing the store file that is locked.
 */
public final class StoreFileInUseException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 8495819717883794122L;

    protected StoreFileInUseException(ObjectStore source,
                                      String storeName)
    {
        super(source,
              StoreFileInUseException.class,
              new Object[] { source, storeName });
    } // StoreFileInUseException().
} // class StoreFileInUseException.
