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
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/InheritanceChild3" })
@ServletSecurity
public class InheritanceChild3 extends InheritanceParent {
    private static final long serialVersionUID = 1L;

    public InheritanceChild3() {
        super("InheritanceChild3 - fall back to Default");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        updateServletName("InheritanceChild3 - doPost");
        handleRequest(req, resp);
    }

    @Override
    public void doCustom(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        updateServletName("InheritanceChild3 - doCustom");
        handleRequest(req, resp);
    }

}
