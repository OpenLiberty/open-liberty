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
 * Thrown when the object manager tries to read a class with an unknown signature.
 */
public final class ObjectSignatureNotFoundException
                extends ObjectManagerException
{
    private static final long serialVersionUID = -6889437473733066245L;

    /**
     * ObjectSignatureNotFoundException.
     * 
     * @param Class of static which throws this ObjectSignatureNotFoundException.
     * @param int the unrecognised object signature.
     */
    protected ObjectSignatureNotFoundException(Class sourceClass,
                                               int objectSignature)
    {
        super(sourceClass,
              ObjectSignatureNotFoundException.class,
              new Integer(objectSignature));

    } // ObjectSignatureNotFoundException().

} // class ObjectSignatureNotFoundException.
