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
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/InheritanceChild4" })
@ServletSecurity(@HttpConstraint(rolesAllowed = "Employee"))
public class InheritanceChild4 extends InheritanceParent {
    private static final long serialVersionUID = 1L;

    public InheritanceChild4() {
        super("InheritanceChild4 - fall back to Default");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        updateServletName("InheritanceChild4 - doPost");
        handleRequest(req, resp);
    }

    @Override
    public void doCustom(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        updateServletName("InheritanceChild4 - doCustom");
        handleRequest(req, resp);
    }

}
