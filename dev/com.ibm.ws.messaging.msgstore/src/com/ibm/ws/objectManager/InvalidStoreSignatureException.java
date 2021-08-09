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
 * Thrown when a file contains an ObjectStorean attempt is made to store an invalid object.
 * 
 * @param ObjectStore throwing the exception.
 * @param String the signature found.
 * @param String the signature expected.
 */
public final class InvalidStoreSignatureException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 5386326610925319343L;

    protected InvalidStoreSignatureException(ObjectStore source,
                                             String signatureFound,
                                             String signatureExpected)
    {
        super(source,
              InvalidStoreSignatureException.class,
              new Object[] { source.getName(),
                            signatureFound, signatureExpected });
    } // InvalidStoreSignatureException(). 
} // class InvalidStoreSignatureException.
