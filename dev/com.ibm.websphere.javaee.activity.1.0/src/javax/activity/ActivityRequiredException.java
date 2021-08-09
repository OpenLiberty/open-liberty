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
package javax.activity;

import java.rmi.RemoteException;

/**
 * A mirror of the corresponding class that was provided in JDK 5-8, and removed in JDK 9+
 */
@SuppressWarnings("serial")
public class ActivityRequiredException extends RemoteException {

    public ActivityRequiredException() {}

    public ActivityRequiredException(String message) {
        super(message);
    }

    public ActivityRequiredException(String message, Throwable cause) {
        super(message, cause);
    }

    public ActivityRequiredException(Throwable cause) {
        super("", cause);
    }
}
