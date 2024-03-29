/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
public class EJBTimerServiceElement extends ConfigElement {

    private Long lateTimerThreshold;
    private Long nonPersistentRetryInterval;
    private Integer nonPersistentMaxRetries;
    private String missedPersistentTimerAction;
    private String nonPersistentContextServiceRef;

    public Long getLateTimerThreshold() {
        return lateTimerThreshold;
    }

    @XmlAttribute(name = "lateTimerThreshold")
    public void setLateTimerThreshold(Long lateTimerThreshold) {
        this.lateTimerThreshold = lateTimerThreshold;
    }

    public Long getNonPersistentRetryInterval() {
        return nonPersistentRetryInterval;
    }

    @XmlAttribute(name = "nonPersistentRetryInterval")
    public void setNonPersistentRetryInterval(Long nonPersistentRetryInterval) {
        this.nonPersistentRetryInterval = nonPersistentRetryInterval;
    }

    public Integer getNonPersistentMaxRetries() {
        return nonPersistentMaxRetries;
    }

    @XmlAttribute(name = "nonPersistentMaxRetries")
    public void setNonPersistentMaxRetries(Integer nonPersistentMaxRetries) {
        this.nonPersistentMaxRetries = nonPersistentMaxRetries;
    }

    public String getMissedPersistentTimerAction() {
        return missedPersistentTimerAction;
    }

    @XmlAttribute(name = "missedPersistentTimerAction")
    public void setMissedPersistentTimerAction(String missedPersistentTimerAction) {
        this.missedPersistentTimerAction = missedPersistentTimerAction;
    }

    public String getNonPersistentContextServiceRef() {
        return nonPersistentContextServiceRef;
    }

    @XmlAttribute(name = "nonPersistentContextServiceRef")
    public void setNonPersistentContextServiceRef(String contextServiceRef) {
        this.nonPersistentContextServiceRef = contextServiceRef;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("EJBTimerServiceElement {");

        if (lateTimerThreshold != null) {
            buf.append("lateTimerThreshold=\"" + lateTimerThreshold + "\" ");
        }
        if (nonPersistentRetryInterval != null) {
            buf.append("nonPersistentRetryInterval=\"" + nonPersistentRetryInterval + "\" ");
        }
        if (nonPersistentMaxRetries != null) {
            buf.append("nonPersistentMaxRetries=\"" + nonPersistentMaxRetries + "\" ");
        }
        if (missedPersistentTimerAction != null) {
            buf.append("missedPersistentTimerAction=\"" + missedPersistentTimerAction + "\" ");
        }
        if (nonPersistentContextServiceRef != null) {
            buf.append("contextServiceRef=\"" + nonPersistentContextServiceRef + "\" ");
        }

        buf.append("}");
        return buf.toString();
    }
}
