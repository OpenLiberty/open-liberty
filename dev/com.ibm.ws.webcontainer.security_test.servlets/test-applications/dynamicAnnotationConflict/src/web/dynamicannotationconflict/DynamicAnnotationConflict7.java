/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.dynamicannotationconflict;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;

import web.common.BaseServlet;

/**
 * Dynamic Annotation Conflict Servlet
 */
//Servlet already defined in web.xml, but use constraint from here
@WebServlet("/DynamicAnnotationConflict7")
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Manager" }))
public class DynamicAnnotationConflict7 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public DynamicAnnotationConflict7() {
        super("DynamicAnnotationConflict7");
    }

}
