/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package oidc.client.servlets;

import jakarta.servlet.annotation.WebServlet;
import oidc.client.base.servlets.BaseUnsecuredServlet;

@WebServlet("/UnsecuredServlet")
public class UnsecuredServlet extends BaseUnsecuredServlet {

    private static final long serialVersionUID = 1L;

}
