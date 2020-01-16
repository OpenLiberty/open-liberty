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
package com.ibm.ws.wsat.threadedserver.server;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.tx.jta.ut.util.AbstractTestServlet;

@WebServlet({ "/ParticipantCheckServlet" })
public class ParticipantCheckServlet extends AbstractTestServlet {

	private static final long serialVersionUID = 1L;

	protected String get(HttpServletRequest request) throws ServletException, IOException {

		return Integer.toString(XAResourceImpl.committedCount());
	}
}
