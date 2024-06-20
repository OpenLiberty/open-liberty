/*******************************************************************************
 * Copyright (c) 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.sm.smfview;

//------------------------------------------------------------------------------
/** UnsupportedVersionException */
public class UnsupportedVersionException extends Throwable {

    /**
     * 
     */
    private static final long serialVersionUID = -3414294222177115891L;

    /**
     * Creates new UnsupportedVersionException.
     * 
     * @param aClassName        Name of class causing exception.
     * @param aSupportedVersion Version the class supports.
     * @param aRequestedVersion Version requested from the SmfStream.
     */
    public UnsupportedVersionException(
                                       String aClassName,
                                       int aSupportedVersion,
                                       int aRequestedVersion) {

        super("UnsupportedVersionException: " + aClassName
              + " SupportedVersion: " + aSupportedVersion
              + ", RequestedVersion: " + aRequestedVersion);

    } // UnsupportedVersionException(...)

} // UnsupportedVersionException