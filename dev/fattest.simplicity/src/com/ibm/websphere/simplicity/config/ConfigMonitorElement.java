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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * <p>
 * This represents an XML element for the server configuration monitor such as:
 * </p>
 * <code>&lt;config monitorInterval="500ms" updateTrigger="polled"/&gt;</code>
 */
public class ConfigMonitorElement extends ConfigElement {

    private String updateTrigger;

    private String monitorInterval;

    /**
     * @return the updateTrigger
     */
    public String getUpdateTrigger() {
        return updateTrigger;
    }

    /**
     * @param updateTrigger the updateTrigger to set
     */
    @XmlAttribute(name = "updateTrigger")
    public void setUpdateTrigger(String updateTrigger) {
        this.updateTrigger = updateTrigger;
    }

    /**
     * @return the monitorInterval
     */
    public String getMonitorInterval() {
        return monitorInterval;
    }

    /**
     * @param monitorInterval the monitorInterval to set
     */
    @XmlAttribute(name = "monitorInterval")
    public void setMonitorInterval(String monitorInterval) {
        this.monitorInterval = monitorInterval;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("ConfigMonitorElement{");
        if (updateTrigger != null)
            buf.append("updateTrigger=\"" + updateTrigger + "\" ");
        if (monitorInterval != null)
            buf.append("monitorInterval=\"" + monitorInterval + "\" ");
        buf.append("}");

        return buf.toString();
    }
}
