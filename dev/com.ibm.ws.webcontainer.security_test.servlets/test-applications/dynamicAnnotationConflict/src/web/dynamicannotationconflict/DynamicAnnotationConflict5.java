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

import javax.annotation.security.RunAs;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;

import web.common.BaseServlet;

/**
 * Dynamic Annotation Conflict Servlet
 */
//RunAs conflict in web.xml and servlet
@RunAs("Employee")
@WebServlet(name = "DynamicAnnotationConflict5", urlPatterns = { "/DynamicAnnotationConflict5" })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Manager" }))
public class DynamicAnnotationConflict5 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public DynamicAnnotationConflict5() {
        super("DynamicAnnotationConflict5");
    }

}
