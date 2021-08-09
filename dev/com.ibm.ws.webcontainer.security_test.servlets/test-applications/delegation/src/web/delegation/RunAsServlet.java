/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.delegation;

import java.io.IOException;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;

import web.common.BaseServlet;

/**
 * RunAs Servlet
 */
public class RunAsServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public RunAsServlet() {
        super("RunAsServlet");
    }

    @Override
    protected void performTask(HttpServletRequest req, HttpServletResponse resp, StringBuffer sb) throws ServletException, IOException {
        Subject runAsSubject = null;
        try {
            runAsSubject = WSSubject.getRunAsSubject();
        } catch (WSSecurityException e) {
            sb.append("</br>Error getting RunAs subject: " + e.getMessage() + "\n</br>");
        }
        sb.append("</br>RunAs subject: " + runAsSubject + "\n</br>");
    }
}
