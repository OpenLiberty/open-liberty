/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20;

/**
 * Exception indicates that configurations for the endpoint is incorrect, the configuration may be from annotation or deployment plans.
 */
public class InvalidMetaDataException extends Exception {

    public InvalidMetaDataException(String msg) {
        super(msg);
    }
}
