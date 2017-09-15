/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.metadata;

/**
 * This exception is thrown to indicate a severe error has occurred
 * during metadata processing
 */

public class MetaDataException
                extends RuntimeException
{
    private static final long serialVersionUID = 6561900330766430495L;

    /**
     * Create a new <code>MetaDataException</code> instance. <p>
     */

    public MetaDataException(String message) {
        super(message);
    } // MetaDataException

    public MetaDataException(String message, Throwable cause) {
        super(message, cause);
    } // MetaDataException

    public MetaDataException(Throwable cause) {
        super(cause);
    } // MetaDataException

} // MetaDataException
