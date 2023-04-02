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

package web.staticannotationmixed;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;

import web.common.BaseServlet;

//Exact URL pattern for annotations - /StaticAnnotationMixedFragment1
// Because we have conflict with url in web.xml, we ignore the security annotation
@WebServlet(name = "StaticAnnotationMixedFragment1", urlPatterns = { "/StaticAnnotationMixedFragment1" })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "AllAuthenticated" }))
public class StaticAnnotationMixedFragment1 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationMixedFragment1() {
        super("StaticAnnotationMixedFragment1");
    }

}
