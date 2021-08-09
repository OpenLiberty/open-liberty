/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.exceptions;

/**
 * An exception to indicate that a state transition attempted on a Massive
 * Asset was not a valid transition for an asset in that state ie between
 * draft and published without going through awwaiting_approval.
 *
 */
public class RepositoryResourceValidationException extends RepositoryResourceException {

    private static final long serialVersionUID = -517149002743240874L;

    public RepositoryResourceValidationException(String message, String resourceId) {
        super(message, resourceId);
    }

    public RepositoryResourceValidationException(String message, String resourceId, Throwable cause) {
        super(message, resourceId, cause);
    }

}
