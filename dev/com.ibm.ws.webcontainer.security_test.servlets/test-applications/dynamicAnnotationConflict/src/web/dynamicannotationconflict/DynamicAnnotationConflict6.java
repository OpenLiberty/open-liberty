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
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

/**
 * Dynamic Annotation Conflict Servlet
 */
//Wildcard, so no exact match
@WebServlet(name = "web.DynamicAnnotationConflict6", urlPatterns = { "/DynamicAnnotationConflict6/b" })
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { "Manager" }), httpMethodConstraints = @HttpMethodConstraint(value = "POST", emptyRoleSemantic = EmptyRoleSemantic.DENY))
public class DynamicAnnotationConflict6 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public DynamicAnnotationConflict6() {
        super("DynamicAnnotationConflict6");
    }

}
