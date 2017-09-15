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
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIAttribute;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;servlet-cache-config>.
 */
@LibertyNotInUse
public interface ServletCacheConfig {

    @DDAttribute(name = "properties-group-name", type = DDAttributeType.String)
    @DDXMIAttribute(name = "propertiesGroupName")
    String getPropertiesGroupName();

    @DDAttribute(name = "name", elementName = "servlet", type = DDAttributeType.String)
    List<String> getServletNames();

    boolean isSetTimeout();

    @DDAttribute(name = "value", elementName = "timeout", type = DDAttributeType.Int)
    @DDXMIAttribute(name = "timeout")
    int getTimeout();

    boolean isSetPriority();

    @DDAttribute(name = "value", elementName = "priority", type = DDAttributeType.Int)
    @DDXMIAttribute(name = "priority")
    int getPriority();

    boolean isSetInvalidateOnly();

    @DDAttribute(name = "value", elementName = "invalidate-only", type = DDAttributeType.Boolean)
    @DDXMIAttribute(name = "invalidateOnly")
    boolean isInvalidateOnly();

    @DDAttribute(name = "name", elementName = "external-cache-group", type = DDAttributeType.String)
    @DDXMIAttribute(name = "externalCacheGroups")
    List<String> getExternalCacheGroupNames();

    @DDAttribute(name = "class", elementName = "id-generator", type = DDAttributeType.String)
    @DDXMIAttribute(name = "idGenerator")
    String getIdGenerator();

    @DDAttribute(name = "class", elementName = "metadata-generator", type = DDAttributeType.String)
    @DDXMIAttribute(name = "metadataGenerator")
    String getMetadataGenerator();

    @DDElement(name = "id-generation-properties")
    IdGenerationProperties getIdGenerationProperties();

}
