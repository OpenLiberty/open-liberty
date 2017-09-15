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

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDIdAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;markup-language>.
 */
@LibertyNotInUse
@DDIdAttribute
public interface MarkupLanguage {

    @DDAttribute(name = "name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "name")
    String getName();

    @DDAttribute(name = "mime-type", type = DDAttributeType.String)
    @DDXMIAttribute(name = "mimeType")
    String getMimeType();

    @DDAttribute(name = "error-page", type = DDAttributeType.String)
    @DDXMIAttribute(name = "errorPage")
    String getErrorPage();

    @DDAttribute(name = "default-page", type = DDAttributeType.String)
    @DDXMIAttribute(name = "defaultPage")
    String getDefaultPage();

    @DDElement(name = "page")
    @DDXMIElement(name = "pages")
    List<Page> getPages();
}
