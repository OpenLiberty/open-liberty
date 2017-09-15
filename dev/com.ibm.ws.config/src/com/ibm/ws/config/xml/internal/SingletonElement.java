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

/**
 *
 */
public class SingletonElement extends MetaTypeElement {

    /**
     * @param nodeName
     */
    public SingletonElement(String nodeName, String pid) {
        super(nodeName, pid);
    }

    /**
     * @param configElement
     * @param factoryPid
     */
    public SingletonElement(SimpleElement configElement, String pid) {
        super(configElement, pid);
    }

    /**
     * @param elements
     * @throws ConfigMergeException
     */
    public SingletonElement(List<SimpleElement> elements, String pid) throws ConfigMergeException {
        super(elements.get(0).getNodeName(), pid);
        this.mergeBehavior = elements.get(0).mergeBehavior;
        setDocumentLocation(elements.get(0).getDocumentLocation());
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
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.xml.internal.ConfigElement#isFactory()
     */
    @Override
    @Trivial
    public boolean isFactory() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.config.xml.internal.ConfigElement#getId()
     */
    @Override
    @Trivial
    public String getId() {
        return null;
    }

    @Override
    public ConfigID getConfigID() {
        return new ConfigID(this.pid, null);
    }

}
