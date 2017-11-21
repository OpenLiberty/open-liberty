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
//RunAs conflict in servlet
@RunAs("Employee")
@WebServlet(name = "DynamicAnnotationConflict4", urlPatterns = { "/DynamicAnnotationConflict4" })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Manager" }))
public class DynamicAnnotationConflict4 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public DynamicAnnotationConflict4() {
        super("DynamicAnnotationConflict4");
    }

}
