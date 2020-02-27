/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.acme.internal;

import java.util.Collection;

/**
 * Certificate signing request options to send to the ACME CA server.
 */
public class CSROptions {

	private final Collection<String> domains;
	private String country = null;
	private String locality = null;
	private String organization = null;
	private String organizationalUnit = null;
	private String state = null;
	private Long validForMs = null;

	/**
	 * Construct a new {@link CSROptions} instance for the specified domains.
	 * 
	 * @param domains
	 *            The domains to request the certificate for.
	 */
	public CSROptions(Collection<String> domains) {

		if (domains == null || domains.isEmpty()) {
			throw new IllegalArgumentException("There must be a valid domain for the CSR request.");
		}

		this.domains = domains;
	}

	/**
	 * @return the country
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @param country
	 *            the country to set
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @return the locality
	 */
	public String getLocality() {
		return locality;
	}

	/**
	 * @param locality
	 *            the locality to set
	 */
	public void setLocality(String locality) {
		this.locality = locality;
	}

	/**
	 * @return the organization
	 */
	public String getOrganization() {
		return organization;
	}

	/**
	 * @param organization
	 *            the organization to set
	 */
	public void setOrganization(String organization) {
		this.organization = organization;
	}

	/**
	 * @return the organizationalUnit
	 */
	public String getOrganizationalUnit() {
		return organizationalUnit;
	}

	/**
	 * @param organizationalUnit
	 *            the organizationalUnit to set
	 */
	public void setOrganizationalUnit(String organizationalUnit) {
		this.organizationalUnit = organizationalUnit;
	}

	/**
	 * @return the state
	 */
	public String getState() {
		return state;
	}

	/**
	 * @param state
	 *            the state to set
	 */
	public void setState(String state) {
		this.state = state;
	}

	/**
	 * @return the validForMs
	 */
	public Long getValidForMs() {
		return validForMs;
	}

	/**
	 * @param validForMs
	 *            the validForMs to set
	 */
	public void setValidForMs(Long validForMs) {
		this.validForMs = validForMs;
	}

	/**
	 * @return the domains
	 */
	public Collection<String> getDomains() {
		return domains;
	}
}
