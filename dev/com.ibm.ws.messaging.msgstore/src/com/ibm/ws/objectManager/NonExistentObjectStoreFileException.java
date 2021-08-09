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
 * Thrown when an attempt is made to locate an ObjectStore file which should already exist.
 * 
 * @version @(#) 1/25/13
 * @author Andrew_Banks
 */
public final class NonExistentObjectStoreFileException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 1L;

    /**
     * @param objectStore that is unable to locate the file.
     * @param fileName the ObjectStore cannot find.
     */
    protected NonExistentObjectStoreFileException(ObjectStore objectStore,
                                                  String fileName) {
        super(objectStore,
              NonExistentObjectStoreFileException.class,
              new Object[] { objectStore,
                            null,
                            fileName });
    } // NonExistentObjectStoreFileException().

    /**
     * @param objectStore that is unable to locate the file.
     * @param exception caught by the ObjectStore.
     * @param fileName the ObjectStore cannot find.
     */
    protected NonExistentObjectStoreFileException(ObjectStore objectStore,
                                                  Exception exception,
                                                  String fileName) {
        super(objectStore,
              NonExistentObjectStoreFileException.class,
              exception, new Object[] { objectStore,
                                       exception,
                                       fileName });
    } // NonExistentObjectStoreFileException().
} // class NonExistentObjectStoreFileException.