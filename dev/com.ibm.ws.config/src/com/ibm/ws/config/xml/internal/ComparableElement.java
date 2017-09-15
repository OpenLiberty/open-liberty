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
public class ComparableElement extends SimpleElement {

    private final String id;
    private final String pid;

    public ComparableElement(SimpleElement element, int index, String pid, String defaultId) {
        super(element);

        // Replace the ID attribute if it's null or was generated
        String idAttribute = element.getId();
        if ((idAttribute == null) || (idAttribute.startsWith("default"))) {
            if (defaultId == null) {
                idAttribute = (index < 1) ? "default-0" : "default-" + index;
                this.usingDefaultId = true;
            } else {
                idAttribute = defaultId;
            }
        }
        this.id = idAttribute;
        this.pid = pid;
    }

    /*
     * This furthers the confusion about what might be returned from this method.
     * child-first processing in ConfigEvaluator apparently needs to deal with both 
     * the original xml element node name or the pid being returned.
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.internal.ConfigElement#getNodeName()
     */
    @Override
    public String getNodeName() {
        return this.pid == null ? super.getNodeName() : pid;
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
}
