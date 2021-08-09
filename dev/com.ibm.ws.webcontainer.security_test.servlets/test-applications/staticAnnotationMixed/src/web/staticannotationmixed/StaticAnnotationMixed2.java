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

package web.staticannotationmixed;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//WILD URL pattern for annotations so multiple url patterns can match- /StaticAnnotationMixed4/*
// We have two tests to verify that annotation is used and not used
// Since the url is not matched, the annotations will create a security constraint where all methods require "AllAuthenticated" user and the POST method will Deny all.
// Note, since the web.xml has one url security constraint for /StaticAnnotationMixed4/a, but no POST security constraints, the security annotation security constraint (Deny all) will apply
@WebServlet(name = "StaticAnnotationMixed2", urlPatterns = { "/StaticAnnotationMixed2/*" })
@ServletSecurity(value = @HttpConstraint(rolesAllowed = "AllAuthenticated"), httpMethodConstraints = { @HttpMethodConstraint(value = "POST",
                                                                                                                             emptyRoleSemantic = EmptyRoleSemantic.DENY) })
public class StaticAnnotationMixed2 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationMixed2() {
        super("StaticAnnotationMixed2");
    }

}
