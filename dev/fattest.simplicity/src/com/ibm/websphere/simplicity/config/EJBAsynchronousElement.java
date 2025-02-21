/*******************************************************************************
 * Copyright (c) 2012, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class EJBAsynchronousElement extends ConfigElement {

    private String contextServiceRef;
    private String unclaimedRemoteResultTimeout;
    private Integer maxUnclaimedRemoteResults;

    public String getContextServiceRef() {
        return contextServiceRef;
    }

    @XmlAttribute(name = "contextServiceRef")
    public void setContextServiceRef(String contextServiceRef) {
        this.contextServiceRef = contextServiceRef;
    }

    public String getUnclaimedRemoteResultTimeout() {
        return unclaimedRemoteResultTimeout;
    }

    @XmlAttribute(name = "unclaimedRemoteResultTimeout")
    public void setUnclaimedRemoteResultTimeout(String unclaimedRemoteResultTimeout) {
        this.unclaimedRemoteResultTimeout = unclaimedRemoteResultTimeout;
    }

    public Integer getMaxUnclaimedRemoteResults() {
        return maxUnclaimedRemoteResults;
    }

    @XmlAttribute(name = "maxUnclaimedRemoteResults")
    public void setMaxUnclaimedRemoteResults(Integer maxUnclaimedRemoteResults) {
        this.maxUnclaimedRemoteResults = maxUnclaimedRemoteResults;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("EJBAsynchronousElement {");

        if (contextServiceRef != null)
            buf.append("contextServiceRef=\"" + contextServiceRef + "\" ");
        if (unclaimedRemoteResultTimeout != null)
            buf.append("unclaimedRemoteResultTimeout=\"").append(unclaimedRemoteResultTimeout).append("\" ");
        if (maxUnclaimedRemoteResults != null)
            buf.append("maxUnclaimedRemoteResults=\"").append(maxUnclaimedRemoteResults).append("\" ");

        buf.append("}");
        return buf.toString();
    }
}
