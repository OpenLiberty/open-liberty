/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javaee.dd.commonbnd;

import java.util.List;

import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;

/**
 * Represents the refBindingsGroup type from the ibm-common-bnd XSD.
 */
public interface RefBindingsGroup {

    @DDElement(name = "ejb-ref")
    @DDXMIElement(name = "ejbRefBindings")
    List<EJBRef> getEJBRefs();

    @DDElement(name = "resource-ref")
    @DDXMIElement(name = "resRefBindings")
    List<ResourceRef> getResourceRefs();

    @DDElement(name = "resource-env-ref")
    @DDXMIElement(name = "resourceEnvRefBindings")
    List<ResourceEnvRef> getResourceEnvRefs();

    @DDElement(name = "message-destination-ref")
    @DDXMIElement(name = "messageDestinationRefBindings")
    List<MessageDestinationRef> getMessageDestinationRefs();

    @DDElement(name = "data-source")
    // No XMI metadata: this element was not supported prior to EE 6.
    List<DataSource> getDataSources();

    @DDElement(name = "env-entry")
    // No XMI metadata: this element was not supported prior to EE 6.
    List<EnvEntry> getEnvEntries();
}
