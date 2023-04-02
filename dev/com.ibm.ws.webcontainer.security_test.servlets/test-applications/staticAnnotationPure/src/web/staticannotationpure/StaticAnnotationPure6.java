/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package web.staticannotationpure;

import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//GET allows all roles, POST requires Manager, CUSTOM requires Employee and SSL
//Multiple URL patterns
@WebServlet(name = "StaticAnnotationPure6", urlPatterns = { "/StaticAnnotationPure6", "/staticAnnotation/StaticAnnotationPure6" })
@ServletSecurity(httpMethodConstraints = { @HttpMethodConstraint(value = "GET", rolesAllowed = "AllAuthenticated"),
                                           @HttpMethodConstraint(value = "POST", rolesAllowed = "Manager"),
                                           @HttpMethodConstraint(value = "CUSTOM", rolesAllowed = "Employee", transportGuarantee = TransportGuarantee.CONFIDENTIAL) })
public class StaticAnnotationPure6 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationPure6() {
        super("StaticAnnotationPure6");
    }

}
