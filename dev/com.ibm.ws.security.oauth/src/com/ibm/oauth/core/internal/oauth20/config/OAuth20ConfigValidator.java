/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.oauth.core.internal.oauth20.config;

import com.ibm.oauth.core.api.error.OAuthException;

public interface OAuth20ConfigValidator {

    public void validate() throws OAuthException;

    public OAuth20ConfigProvider getConfigProvider() throws OAuthException;

}
