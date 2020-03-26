/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.tai;

import java.util.Map;

import com.ibm.wsspi.security.tai.TrustAssociationInterceptor;

public interface TAIService {

    public boolean isInvokeForUnprotectedURI();

    public boolean isFailOverToAppAuthType();

    public boolean isInvokeForFormLogin();

    public Map<String, TrustAssociationInterceptor> getTais(boolean invokeBeforeSSO);

    public boolean isDisableLtpaCookie(String taiId);
}
