/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 */
public class mpMetricsElement {

    private Boolean authentication;

    @XmlElement(name = "webModuleDoc")
    private ConfigElementList<WebModuleDocElement> webModuleDocs;

    public Boolean getAuthentication() {
        return authentication;
    }

    @XmlAttribute(name = "authentication")
    public void setAuthentication(Boolean authentication) {
        this.authentication = authentication;
    }

    public ConfigElementList<WebModuleDocElement> getWebModuleDocs() {
        if (this.webModuleDocs == null) {
            this.webModuleDocs = new ConfigElementList<WebModuleDocElement>();
        }
        return this.webModuleDocs;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("mpMetricsElement [");
        sb.append("authentication=").append(authentication);
        sb.append(", webModuleDocs=[");
        if (webModuleDocs != null) {
            for (WebModuleDocElement webModuleDoc : webModuleDocs) {
                sb.append(webModuleDoc.toString()).append(", ");
            }
        }
        sb.append("]]");
        return sb.toString();
    }

    @XmlType(name = "mpMetricsWebModuleDoc")
    public static class WebModuleDocElement extends ConfigElement {

        private Boolean authentication;

        public Boolean getAuthentication() {
            return authentication;
        }

        @XmlAttribute(name = "authentication")
        public void setAuthentication(Boolean authentication) {
            this.authentication = authentication;
        }

        @Override
        public String toString() {
            return "WebModuleDoc [authentication=" + authentication + "]";
        }
    }
}