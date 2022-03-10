/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.messaging.lifecycle;

public class LifecycleError extends Error {

    public LifecycleError(String message, Throwable cause) {
	super(message, cause);
    }

    public LifecycleError(String message) {
	super(message);
    }
}
