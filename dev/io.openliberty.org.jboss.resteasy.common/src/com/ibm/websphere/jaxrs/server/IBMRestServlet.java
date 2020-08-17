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
package com.ibm.websphere.jaxrs.server;

import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class IBMRestServlet extends HttpServlet30Dispatcher {
    private static final long serialVersionUID = -7916305366621576524L;

}
