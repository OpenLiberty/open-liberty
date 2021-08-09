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
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//All methods are denied
@WebServlet(name = "StaticAnnotationPure1", urlPatterns = { "/StaticAnnotationPure1" })
@ServletSecurity(@HttpConstraint(EmptyRoleSemantic.DENY))
public class StaticAnnotationPure1 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationPure1() {
        super("StaticAnnotationPure1");
    }

}
