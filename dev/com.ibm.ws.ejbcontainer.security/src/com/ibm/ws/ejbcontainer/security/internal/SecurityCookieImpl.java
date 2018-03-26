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
    private final Subject adjustedInvokedSubject;
    private final Subject adjustedReceivedSubject;

    SecurityCookieImpl(Subject invokedSubject, Subject receivedSubject) {
        this.invokedSubject = this.adjustedInvokedSubject =invokedSubject;
        this.receivedSubject = this.adjustedReceivedSubject = receivedSubject;
    }

    SecurityCookieImpl(Subject invokedSubject, Subject receivedSubject, Subject adjustedInvokedSubject, Subject adjustedReceivedSubject) {
        this.invokedSubject = invokedSubject;
        this.receivedSubject = receivedSubject;
        this.adjustedInvokedSubject = adjustedInvokedSubject;
        this.adjustedReceivedSubject = adjustedReceivedSubject;
    }

    public Subject getInvokedSubject() {
        return invokedSubject;
    }

    public Subject getReceivedSubject() {
        return receivedSubject;
    }

    public Subject getAdjustedInvokedSubject() {
        return adjustedInvokedSubject;
    }

    public Subject getAdjustedReceivedSubject() {
        return adjustedReceivedSubject;
    }
}
