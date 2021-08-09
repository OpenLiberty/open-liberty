/*******************************************************************************
 * Copyright (c) 1997, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.persistent.filemgr;

/**
 * Thrown to indicate that some component of an ODG does not exist.
 *
 */
public class FileManagerException extends RuntimeException {

    private static final long serialVersionUID = -7544446508978881541L;
    
    /**
     * Constructs an NoSuchObjectException with the specified
     * detail message.
     */
    public FileManagerException(String message) {
        super(message);
    }

}


