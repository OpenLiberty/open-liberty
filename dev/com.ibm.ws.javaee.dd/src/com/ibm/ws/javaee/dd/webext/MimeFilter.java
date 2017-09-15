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

import com.ibm.ws.javaee.ddmetadata.annotation.DDAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.DDAttributeType;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;

/**
 * Represents &lt;mime-filter>.
 */
public interface MimeFilter {

    @DDAttribute(name = "target", type = DDAttributeType.String)
    @DDXMIAttribute(name = "target")
    String getTarget();

    @DDAttribute(name = "mime-type", type = DDAttributeType.String)
    @DDXMIAttribute(name = "type")
    String getMimeType();
}
