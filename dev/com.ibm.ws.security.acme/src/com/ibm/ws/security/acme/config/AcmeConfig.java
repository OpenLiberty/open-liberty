/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.config;

import java.util.Set;

public interface AcmeConfig {

	public String getDirectoryURI();

	public String getDomain();

	public int getValidFor();

	public String getCountry();

	public String getLocality();

	public String getState();

	public String getOrganization();

	public String getOrganizationalUnit();

	public int getChallengeRetries();

	public int getChallengeRetryWait();

	public int getOrderRetries();

	public int getOrderRetryWait();

	public String getAccountKeyFile();

	public Set<String> getAccountContact();

	public boolean getAcceptTermsOfService();

	public String getDomainKeyFile();

}
