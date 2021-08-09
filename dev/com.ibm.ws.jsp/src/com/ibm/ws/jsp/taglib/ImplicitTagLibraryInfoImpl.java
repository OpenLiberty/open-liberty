/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;

import com.ibm.wsspi.jsp.resource.JspInputSource;

public class ImplicitTagLibraryInfoImpl extends TagLibraryInfoImpl {
    public ImplicitTagLibraryInfoImpl(String directoryName, JspInputSource inputSource) {
        super("", directoryName, "webinf", inputSource);
        this.shortname = directoryName;
        if (shortname.startsWith("/WEB-INF/")) {
            shortname = shortname.substring(9);
        }
        if (shortname.endsWith("/")) {
            shortname = shortname.substring(0, shortname.lastIndexOf('/')-1);
        }
        shortname = shortname.replaceAll("/", "-");
        this.tlibversion = "1.0";
        this.jspversion = "2.0";
    }
}
