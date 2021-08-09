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
package com.ibm.ws.ejbcontainer.osgi.internal;

/**
 * Thrown when an error occurs while starting or stopping a module.
 */
@SuppressWarnings("serial")
public class EJBRuntimeException extends Exception {
    EJBRuntimeException(Throwable t) {
        super(t);
    }
}
