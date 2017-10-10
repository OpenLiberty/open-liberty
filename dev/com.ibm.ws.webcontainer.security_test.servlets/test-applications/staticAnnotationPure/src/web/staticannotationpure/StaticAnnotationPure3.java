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

import javax.annotation.security.RunAs;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//All methods (ex. CUSTOM) are denied access, GET requires Manager, POST requires Employee and SSL, runas Employee
@RunAs("Employee")
@WebServlet("/StaticAnnotationPure3")
@ServletSecurity(value = @HttpConstraint(EmptyRoleSemantic.DENY), httpMethodConstraints = {
                                                                                            @HttpMethodConstraint(value = "GET", rolesAllowed = "Manager",
                                                                                                                  transportGuarantee = TransportGuarantee.NONE),
                                                                                            @HttpMethodConstraint(value = "POST", rolesAllowed = "Employee",
                                                                                                                  transportGuarantee = TransportGuarantee.CONFIDENTIAL) })
public class StaticAnnotationPure3 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationPure3() {
        super("StaticAnnotationPure3");
    }

}
