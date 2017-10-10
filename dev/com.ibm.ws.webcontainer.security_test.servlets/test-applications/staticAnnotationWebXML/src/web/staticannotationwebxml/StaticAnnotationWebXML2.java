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

package web.staticannotationwebxml;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//Exact URL pattern for annotations - /StaticAnnotationWebXML2
// Because we have conflict with url in web.xml, we ignore the security annotation
@WebServlet(name = "StaticAnnotationWebXML2", urlPatterns = { "/StaticAnnotationWebXML2" })
@ServletSecurity(@HttpConstraint(EmptyRoleSemantic.DENY))
public class StaticAnnotationWebXML2 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationWebXML2() {
        super("StaticAnnotationWebXML2");
    }

}
