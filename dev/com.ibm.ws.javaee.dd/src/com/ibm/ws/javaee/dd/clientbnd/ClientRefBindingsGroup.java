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
package com.ibm.ws.javaee.dd.clientbnd;

import java.util.List;

import com.ibm.ws.javaee.dd.commonbnd.DataSource;
import com.ibm.ws.javaee.dd.commonbnd.EJBRef;
import com.ibm.ws.javaee.dd.commonbnd.EnvEntry;
import com.ibm.ws.javaee.dd.commonbnd.MessageDestinationRef;
import com.ibm.ws.javaee.dd.commonbnd.ResourceEnvRef;
import com.ibm.ws.javaee.dd.commonbnd.ResourceRef;
import com.ibm.ws.javaee.ddmetadata.annotation.DDElement;
import com.ibm.ws.javaee.ddmetadata.annotation.DDXMIElement;

/**
 * Represents the refBindingsGroup type from the ibm-common-bnd XSD.
 */
public interface ClientRefBindingsGroup extends com.ibm.ws.javaee.dd.commonbnd.RefBindingsGroup {

    @Override
    @DDElement(name = "ejb-ref")
    @DDXMIElement(name = "ejbRefs")
    List<EJBRef> getEJBRefs();

    @Override
    @DDElement(name = "resource-ref")
    @DDXMIElement(name = "resourceRefs")
    List<ResourceRef> getResourceRefs();

    @Override
    @DDElement(name = "resource-env-ref")
    @DDXMIElement(name = "resourceEnvRefBindings")
    List<ResourceEnvRef> getResourceEnvRefs();

    @Override
    @DDElement(name = "message-destination-ref")
    @DDXMIElement(name = "messageDestinationRefs")
    List<MessageDestinationRef> getMessageDestinationRefs();

    @Override
    @DDElement(name = "data-source")
    // No XMI metadata: this element was not supported prior to EE 6.
    List<DataSource> getDataSources();

    @Override
    @DDElement(name = "env-entry")
    // No XMI metadata: this element was not supported prior to EE 6.
    List<EnvEntry> getEnvEntries();
}
