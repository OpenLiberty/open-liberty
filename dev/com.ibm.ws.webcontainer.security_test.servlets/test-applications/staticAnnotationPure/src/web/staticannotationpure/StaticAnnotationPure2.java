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
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//All methods are unprotected, but requires SSL
@WebServlet("/StaticAnnotationPure2")
@ServletSecurity(@HttpConstraint(transportGuarantee = TransportGuarantee.CONFIDENTIAL))
public class StaticAnnotationPure2 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public StaticAnnotationPure2() {
        super("StaticAnnotationPure2");
    }

}
