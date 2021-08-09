/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install;

/**
 * This class provides APIs for an Exception caused by a Cancelled Installation.
 */
public class CancelException extends InstallException {

    private static final long serialVersionUID = -487184439522887816L;

    /**
     * This method creates a CancelException
     *
     * @param message Exception message
     * @param rc Return code
     */
    public CancelException(String message, int rc) {
        super(message, rc);
    }

}
