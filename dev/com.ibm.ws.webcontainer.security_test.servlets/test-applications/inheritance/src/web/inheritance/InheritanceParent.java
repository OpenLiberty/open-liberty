/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.inheritance;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import web.common.BaseServlet;

@WebServlet(name = "InheritanceParent", urlPatterns = { "/InheritanceParent" })
@ServletSecurity(@HttpConstraint(rolesAllowed = "Manager", transportGuarantee = TransportGuarantee.CONFIDENTIAL))
public class InheritanceParent extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public InheritanceParent() {
        super("InheritanceParent");
    }

    public InheritanceParent(String servletName) {
        super(servletName);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        updateServletName("InheritanceParent - service");
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        updateServletName("InheritanceParent - doGet");
        handleRequest(req, resp);
    }

    //@Override
    public void doCustom(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        updateServletName("InheritanceParent - doCustom");
        handleRequest(req, resp);
    }

}
