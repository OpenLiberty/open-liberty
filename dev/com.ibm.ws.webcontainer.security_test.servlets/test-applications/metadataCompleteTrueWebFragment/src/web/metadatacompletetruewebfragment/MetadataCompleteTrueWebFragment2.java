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

//Because metadata-complete=true in web-fragment.xml, static annotations here should be ignored but are NOT since this servlet
//class file is NOT included in the web fragment jar file
@WebServlet(name = "MetadataCompleteTrueWebFragment2", urlPatterns = { "/MetadataCompleteTrueWebFragment2" })
@ServletSecurity(@HttpConstraint(EmptyRoleSemantic.DENY))
public class MetadataCompleteTrueWebFragment2 extends BaseServlet {
    private static final long serialVersionUID = 1L;

    public MetadataCompleteTrueWebFragment2() {
        super("MetadataCompleteTrueWebFragment2");
    }

}
