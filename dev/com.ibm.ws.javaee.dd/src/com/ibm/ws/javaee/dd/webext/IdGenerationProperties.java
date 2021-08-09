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
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;

/**
 * Represents &lt;id-generation-properties>.
 */
@LibertyNotInUse
public interface IdGenerationProperties {

    boolean isSetUseURI();

    @DDAttribute(name = "use-uri", type = DDAttributeType.Boolean)
    boolean isUseURI();

    boolean isSetAlternateName();

    @DDAttribute(name = "alternate-name", type = DDAttributeType.String)
    String getAlternateName();

    boolean isSetUsePathInfos();

    @DDAttribute(name = "use-path-infos", type = DDAttributeType.Boolean)
    boolean isUsePathInfos();

    @DDElement(name = "cache-variable")
    List<CacheVariable> getCacheVariables();
}
