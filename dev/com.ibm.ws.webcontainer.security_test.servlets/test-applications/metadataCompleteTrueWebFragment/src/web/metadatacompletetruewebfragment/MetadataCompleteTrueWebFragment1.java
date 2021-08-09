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

package web.metadatacompletetruewebfragment;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import web.common.BaseServlet;

import javax.servlet.annotation.WebServlet;

//Because metadata-complete=true in web-fragment.xml, static annotations here are ignored
@WebServlet(name = "MetadataCompleteTrueWebFragment1", urlPatterns = { "/MetadataCompleteTrueWebFragment1" })
@ServletSecurity(@HttpConstraint(EmptyRoleSemantic.DENY))
public class MetadataCompleteTrueWebFragment1 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public MetadataCompleteTrueWebFragment1() {
        super("MetadataCompleteTrueWebFragment1");
    }

}
