/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.openj9.criu;

/**
 * A CRIU exception representing a failed CRIU operation.
 */
public final class SystemCheckpointException extends JVMCRIUException {
    private static final long serialVersionUID = 1262214147293662586L;

    /**
     * Creates a SystemCheckpointException with the specified message and error code.
     *
     * @param message the message
     * @param errorCode the error code
     */
    public SystemCheckpointException(String message, int errorCode) {
        super(message, errorCode);
    }
}
