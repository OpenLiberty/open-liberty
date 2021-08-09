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

package web.staticannotationpure;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;

import web.common.BaseServlet;

//All methods (ex. POST) require Manager, GET is unprotected, CUSTOM requires Employee
@WebServlet(name = "StaticAnnotationPure5", urlPatterns = { "/staticAnnotation/StaticAnnotationPure5" })
@ServletSecurity(value = @HttpConstraint(rolesAllowed = "Manager"), httpMethodConstraints = { @HttpMethodConstraint("GET"),
                                                                                              @HttpMethodConstraint(value = "CUSTOM", rolesAllowed = "Employee") })
public class StaticAnnotationPure5 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationPure5() {
        super("StaticAnnotationPure5");
    }

}
