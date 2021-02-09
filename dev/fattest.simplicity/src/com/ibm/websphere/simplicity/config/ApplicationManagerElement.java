/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
public class ApplicationManagerElement extends ConfigElement {

    private Boolean autoExpand;
    private String startTimeout;
    private String stopTimeout;
    private Boolean useJandex;

    public Boolean getAutoExpand() {
        return autoExpand;
    }

    @XmlAttribute
    public void setAutoExpand(Boolean autoExpand) {
        this.autoExpand = autoExpand;
    }

    public String getStartTimeout() {
        return startTimeout;
    }

    @XmlAttribute
    public void setStartTimeout(String startTimeout) {
        this.startTimeout = startTimeout;
    }

    public String getStopTimeout() {
        return stopTimeout;
    }

    @XmlAttribute
    public void setStopTimeout(String stopTimeout) {
        this.stopTimeout = stopTimeout;
    }

    public Boolean getUseJandex() {
        return useJandex;
    }

    @XmlAttribute
    public void setUseJandex(Boolean useJandex) {
        this.useJandex = useJandex;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ApplicationManagerElement [");
        if (autoExpand != null) {
            builder.append("autoExpand=");
            builder.append(autoExpand);
            builder.append(", ");
        }
        if (startTimeout != null) {
            builder.append("startTimeout=");
            builder.append(startTimeout);
            builder.append(", ");
        }
        if (stopTimeout != null) {
            builder.append("stopTimeout=");
            builder.append(stopTimeout);
            builder.append(", ");
        }
        if (useJandex != null) {
            builder.append("useJandex=");
            builder.append(useJandex);
        }
        builder.append("]");
        return builder.toString();
    }

}
