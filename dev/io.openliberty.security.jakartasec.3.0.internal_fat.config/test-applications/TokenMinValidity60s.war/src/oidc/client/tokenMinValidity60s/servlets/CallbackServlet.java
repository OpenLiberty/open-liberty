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
package oidc.client.tokenMinValidity60s.servlets;

import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseCallbackServlet;

@WebServlet("/Callback")
public class CallbackServlet extends BaseCallbackServlet {

    private static final long serialVersionUID = -417476984908088827L;

}
