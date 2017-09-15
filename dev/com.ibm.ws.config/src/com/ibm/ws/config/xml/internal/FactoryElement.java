/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal;

import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.xml.internal.MetaTypeRegistry.RegistryEntry;

/**
 *
 */
public class FactoryElement extends MetaTypeElement {

    private final String id;

    public FactoryElement(SimpleElement element, int index, RegistryEntry entry) {
        super(element, entry.getPid());

        if (element.isUsingNonDefaultId()) {
            // This ID was specified in server.xml, so use it.
            this.id = element.getId();
        } else {
            // Either the ID is null, or we assigned a default one.

            // Try using the metatype default
            String idAttribute = entry.getDefaultId();

            // If it's still null (no metatype default), generate a new one
            if (idAttribute == null) {
                idAttribute = (index < 1) ? "default-0" : "default-" + index;
            }

            this.id = idAttribute;
        }

    }

    /**
     * @param elements
     * @throws ConfigMergeException
     */
    public FactoryElement(List<SimpleElement> elements, String pid, String id) throws ConfigMergeException {
        super(elements.get(0).getNodeName(), pid);
        this.mergeBehavior = elements.get(0).mergeBehavior;
        setDocumentLocation(elements.get(0).getDocumentLocation());
        this.id = id;
        merge(elements);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.internal.ConfigElement#isSingleton()
     */
    @Override
    @Trivial
    public boolean isSingleton() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.internal.ConfigElement#isFactory()
     */
    @Override
    @Trivial
    public boolean isFactory() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.internal.ConfigElement#getId()
     */
    @Override
    @Trivial
    public String getId() {
        return this.id;
    }

    @Override
    @Trivial
    public String getNodeName() {
        return this.pid;
    }

    @Override
    @Trivial
    public ConfigID getConfigID() {

        if (getParent() == null) {
            return new ConfigID(this.pid, getId());
        } else {
            // Attribute name is unique under a parent, pid may not be, so distinguish using the attr name
            // For example, <AD id="fooRef" ibm:reference="com.ibm.ws.foobar"/>
            //              <AD id="barRef" ibm:reference="com.ibm.ws.foobar"/>
            return new ConfigID(getParent().getConfigID(), this.pid, getId(), childAttributeName);
        }
    }
}
