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
package com.ibm.ws.ejbcontainer.security.internal;

import javax.security.auth.Subject;

/**
 * A place to store the subject information at the beginning of the preinvoke.
 */
public class SecurityCookieImpl {
    private final Subject invokedSubject;
    private final Subject receivedSubject;

    SecurityCookieImpl(Subject invokedSubject, Subject receivedSubject) {
        this.invokedSubject = invokedSubject;
        this.receivedSubject = receivedSubject;
    }

    public Subject getInvokedSubject() {
        return invokedSubject;
    }

    public Subject getReceivedSubject() {
        return receivedSubject;
    }
}
