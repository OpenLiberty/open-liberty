/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package com.ibm.ws.app.manager.module;

/**
 * A helper class for setting an application start/stop operation result to
 * an error with a translated message.
 */
@SuppressWarnings("serial")
public class DeployedAppInfoFailure extends Exception {
    public DeployedAppInfoFailure(String translatedMessage, Throwable cause) {
        super(translatedMessage, cause);
    }

    // Override to remove the class name.
    @Override
    public String toString() {
        return getMessage();
    }
}
