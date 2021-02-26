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
package com.ibm.ws.security.acme.internal;

/**
 * This class represents one entry in the AcmeHistory file.
 *
 */
public final class AcmeHistoryEntry {
	private final String date;
	private final String serial;
	private final String directoryURI;
	private final String accountURI;
	private final String expirationDate;
	private final String smallSpaceDelim = "         ";
	
	/**
	 * 
	 * @param date The date the certificate was created.
	 * @param serial The serial number of the certificate.
	 * @param directoryURI The ACME directory URI.
	 * @param accountURI The account location.
	 * @param expirationDate The certificate expiration date.
	 */
	public AcmeHistoryEntry(String date, String serial, String directoryURI, String accountURI, String expirationDate) {
		this.date=date;
		this.serial=serial;
		this.directoryURI = directoryURI;
		this.accountURI = accountURI;
		this.expirationDate = expirationDate;
	}
	
	/**
	 * 
	 * @return The date the certificate was created.
	 */
	protected String getDate() {
		return date;
	}
	
	/**
	 * 
	 * @return The certificate serial number.
	 */
	protected String getSerial() {
		return serial;
	}
	
	/**
	 * 
	 * @return The ACME directory URI.
	 */
	protected String getDirectoryURI() {
		return directoryURI;
	}
	
	/**
	 * 
	 * @return The account location.
	 */
	protected String getAccountURI() {
		return accountURI;
	}
	
	/**
	 * 
	 * @return The date the certificate expires.
	 */
	protected String getExpirationDate() {
		return expirationDate;
	}
	
	/**
	 * 
	 * @return The date, serial, directoryURI, accountURI, and expiration date in a string.
	 */
	public String toString() {
		return date + smallSpaceDelim + serial + smallSpaceDelim + directoryURI + smallSpaceDelim + accountURI + smallSpaceDelim + expirationDate;
	}
}
