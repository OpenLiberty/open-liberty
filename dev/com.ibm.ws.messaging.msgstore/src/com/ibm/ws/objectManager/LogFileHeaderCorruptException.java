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
 * Thrown when a logFile header is read and found to be corrput.
 * 
 * @param FileLogHeader that is corrupt.
 */
public final class LogFileHeaderCorruptException
                extends ObjectManagerException
{
    private static final long serialVersionUID = 7513134746065751699L;

    protected LogFileHeaderCorruptException(FileLogHeader source)
    {
        super(source,
              LogFileHeaderCorruptException.class);
    } // LogFileHeaderCorruptException().
} // class LogFileHeaderCorruptException.
