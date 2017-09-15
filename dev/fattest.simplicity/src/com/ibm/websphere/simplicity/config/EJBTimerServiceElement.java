/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("EJBTimerServiceElement {");

        if (nonPersistentRetryInterval != null) {
            buf.append("nonPersistentRetryInterval=\"" + nonPersistentRetryInterval + "\" ");
        }
        if (nonPersistentMaxRetries != null) {
            buf.append("nonPersistentMaxRetries=\"" + nonPersistentMaxRetries + "\" ");
        }

        buf.append("}");
        return buf.toString();
    }
}
