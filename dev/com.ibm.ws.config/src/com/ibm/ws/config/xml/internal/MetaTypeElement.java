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

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public abstract class MetaTypeElement extends ConfigElement {

    protected final String pid;

    public abstract boolean isSingleton();

    public abstract boolean isFactory();

    /**
     * @param element
     */
    public MetaTypeElement(SimpleElement element, String pid) {
        super(element);
        this.pid = pid;
    }

    /**
     * @param nodeName
     */
    public MetaTypeElement(String nodeName, String pid) {
        super(nodeName);
        this.pid = pid;
    }

    @Override
    @Trivial
    public String getFullId() {
        return getConfigID().toString();
    }

    @Override
    @Trivial
    public boolean isSimple() {
        return false;
    }

}
