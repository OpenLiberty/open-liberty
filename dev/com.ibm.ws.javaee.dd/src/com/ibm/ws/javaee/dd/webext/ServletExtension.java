/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.webext;

import java.util.List;

import com.ibm.ws.javaee.dd.commonext.GlobalTransaction;
import com.ibm.ws.javaee.dd.commonext.LocalTransaction;
import com.ibm.ws.javaee.dd.web.common.Servlet;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIRefElement;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;servlet>.
 */
@LibertyNotInUse
@DDIdAttribute
public interface ServletExtension {

    @DDAttribute(name = "name", type = DDAttributeType.String)
    @DDXMIRefElement(name = "extendedServlet", referentType = Servlet.class, getter = "getServletName")
    String getName();

    @DDElement(name = "local-transaction")
    @DDXMIElement(name = "localTransaction")
    LocalTransaction getLocalTransaction();

    @DDElement(name = "global-transaction")
    @DDXMIElement(name = "globalTransaction")
    GlobalTransaction getGlobalTransaction();

    @DDElement(name = "web-global-transaction")
    @DDXMIElement(name = "webGlobalTransaction")
    WebGlobalTransaction getWebGlobalTransaction();

    @DDElement(name = "markup-language")
    @DDXMIElement(name = "markupLanguages")
    List<MarkupLanguage> getMarkupLanguages();
}
