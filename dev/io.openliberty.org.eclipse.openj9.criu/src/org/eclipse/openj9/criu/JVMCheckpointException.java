/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.openj9.criu;

public final class JVMCheckpointException extends JVMCRIUException {
    private static final long serialVersionUID = 3729891178366898261L;

    public JVMCheckpointException(String message, int errorCode) {
        super(message, errorCode);
    }
}
