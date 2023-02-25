/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package oidc.client.tokenMinValidity90s.servlets;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import oidc.client.base.servlets.BaseOpenIdConfig;

@Named
@Dependent
public class OpenIdConfig extends BaseOpenIdConfig {

    // override and/or create new get methods

}
