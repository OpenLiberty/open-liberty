/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.internal.exceptions;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.acme.AcmeCaException;

/**
 * Exception that is thrown when a certificate renew request occurs too
 * soon after the prior request.
 */
@Trivial
public class CertificateRenewRequestBlockedException extends AcmeCaException {

	private static final long serialVersionUID = -2381844611991560528L;
	
	/*
	 * Time left until the renew request is allowed, expressed in milliseconds
	 */
	private long timeLeftMs = -1;

	/**
	 * Constructs a new exception with the specified detail message. The cause
	 * is not initialized, and may subsequently be initialized by a call to
	 * initCause.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 */
	public CertificateRenewRequestBlockedException(String message) {
		super(message);
	}
	
	/**
	 * Constructs a new exception with the specified detail message. The cause
	 * is not initialized, and may subsequently be initialized by a call to
	 * initCause.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later
	 *            retrieval by the {@link #getMessage()} method.
	 * @param timeLeft
	 *            amount of time in milliseconds before the next renew request
	 *            is allowed
	 */
	public CertificateRenewRequestBlockedException(String message, long timeLeft) {
		super(message);
		timeLeftMs = timeLeft;
	}

	/**
	 * Constructs a new exception with the specified detail message and cause.
	 * <p/>
	 * Note that the detail message associated with cause is not automatically
	 * incorporated in this exception's detail message.
	 * 
	 * @param message
	 *            the detail message (which is saved for later retrieval by the
	 *            {@link #getMessage()} method).
	 * @param cause
	 *            the cause (which is saved for later retrieval by the
	 *            {@link #getCause()} method). (A null value is permitted, and
	 *            indicates that the cause is nonexistent or unknown.)
	 */
	public CertificateRenewRequestBlockedException(String message, Throwable cause) {
		super(message, cause);
	}
	
	/**
	 * Set the amount of time left in milliseconds before the next renewal request is allowed.
	 * 
	 * @param timeLeft
	 *            amount of time in milliseconds before the next renew request is allowed
	 */
	public void setTimeLeftForBlackout(long timeLeft) {
		timeLeftMs = timeLeft;
	}

	/**
	 * Get the amount of time left in milliseconds before the next renewal request is allowed.
	 * 
	 * @return The amount of time in milliseconds before the next renew request is allowed
	 */
	public long getTimeLeftForBlackout() {
		return timeLeftMs;
	}
}
