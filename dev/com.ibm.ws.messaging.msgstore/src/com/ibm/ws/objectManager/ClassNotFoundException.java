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
 * Thrown when the object manager tries to read a class that it cannot load.
 */
public final class ClassNotFoundException
                extends ObjectManagerException {

    private static final long serialVersionUID = 4500305405705255053L;

    /**
     * ClassNotFoundException.
     * 
     * @param Class of static which throws this ClassNotFoundException.
     * @param java.lang.ClassNotFoundException which was caught.
     */
    protected ClassNotFoundException(Class sourceClass,
                                     java.lang.ClassNotFoundException classNotFoundException)
    {
        super(sourceClass,
              ClassNotFoundException.class,
              classNotFoundException,
              classNotFoundException);
    } // ClassNotFoundException().

    /**
     * ClassNotFoundException.
     * 
     * @param Object which is throwing this ClassNotFoundIOException.
     * @param java.io.ClassNotFoundException which was caught.
     */
    protected ClassNotFoundException(Object source,
                                     java.lang.ClassNotFoundException classNotFoundException)
    {
        super(source,
              ClassNotFoundException.class,
              classNotFoundException,
              classNotFoundException);
    } // ClassNotFoundException(). 
} // End of class ClassNotFoundException.
