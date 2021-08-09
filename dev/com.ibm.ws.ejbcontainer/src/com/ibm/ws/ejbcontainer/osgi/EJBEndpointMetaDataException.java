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
package com.ibm.ws.ejbcontainer.osgi;

import java.io.NotSerializableException;
import java.io.ObjectOutputStream;

/**
 * Thrown when an exception occurs while initializing the metadata for an
 * EJBEndpoint.
 */
@SuppressWarnings("serial")
public class EJBEndpointMetaDataException extends Exception {
    public EJBEndpointMetaDataException(Throwable cause) {
        super(cause);
    }

    private void writeObject(ObjectOutputStream out) throws NotSerializableException {
        throw new NotSerializableException();
    }
}
