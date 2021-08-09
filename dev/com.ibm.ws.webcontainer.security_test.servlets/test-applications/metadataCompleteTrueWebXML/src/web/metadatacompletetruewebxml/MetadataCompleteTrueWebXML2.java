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

package web.metadatacompletetruewebxml;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;

import web.common.BaseServlet;

//Because metadata-complete=true in web.xml, static annotations here are ignored
@WebServlet(name = "MetadataCompleteTrueWebXML2", urlPatterns = { "/MetadataCompleteTrueWebXML2" })
@ServletSecurity(@HttpConstraint(rolesAllowed = { "Manager" }))
public class MetadataCompleteTrueWebXML2 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public MetadataCompleteTrueWebXML2() {
        super("MetadataCompleteTrueWebXML2");
    }

}
