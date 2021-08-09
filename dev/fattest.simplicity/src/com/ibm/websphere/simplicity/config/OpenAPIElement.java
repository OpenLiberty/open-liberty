/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

public class OpenAPIElement extends ConfigElement {

    private String publicURL;
    private String customization;
    private Boolean enablePrivateURL;
    private Boolean validation;
    private Boolean disableFileMonitor;

    @XmlElement(name = "webModuleDoc")
    private ConfigElementList<WebModuleDocElement> webModuleDocs;

    public String getPublicURL() {
        return publicURL;
    }

    @XmlAttribute(name = "publicURL")
    public void setPublicURL(String publicURL) {
        this.publicURL = publicURL;
    }

    public String getCustomization() {
        return customization;
    }

    @XmlAttribute(name = "customization")
    public void setCustomization(String customization) {
        this.customization = customization;
    }

    public Boolean getEnablePrivateURL() {
        return enablePrivateURL;
    }

    @XmlAttribute(name = "enablePrivateURL")
    public void setEnablePrivateURL(Boolean enablePrivateURL) {
        this.enablePrivateURL = enablePrivateURL;
    }

    public Boolean getValidation() {
        return validation;
    }

    @XmlAttribute(name = "validation")
    public void setValidation(Boolean validation) {
        this.validation = validation;
    }

    public Boolean getDisableFileMonitor() {
        return disableFileMonitor;
    }

    @XmlAttribute(name = "disableFileMonitor")
    public void setDisableFileMonitor(Boolean disableFileMonitor) {
        this.disableFileMonitor = disableFileMonitor;
    }

    public ConfigElementList<WebModuleDocElement> getWebModuleDocs() {
        if (this.webModuleDocs == null) {
            this.webModuleDocs = new ConfigElementList<WebModuleDocElement>();
        }
        return this.webModuleDocs;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("OpenAPIElement [");
        sb.append("publicURL=").append(publicURL);
        sb.append(", customization=").append(customization);
        sb.append(", enablePrivateURL=").append(enablePrivateURL);
        sb.append(", validation=").append(validation);
        sb.append(", disableFileMonitor=").append(disableFileMonitor);
        sb.append(", webModuleDocs=[");
        if (webModuleDocs != null) {
            for (WebModuleDocElement webModuleDoc : webModuleDocs) {
                sb.append(webModuleDoc.toString()).append(", ");
            }
        }
        sb.append("]]");
        return sb.toString();
    }

    @XmlType(name = "OpenAPIWebModuleDoc")
    public static class WebModuleDocElement extends ConfigElement {

        private String contextRoot;
        private Boolean enabled;
        private Boolean isPublic;

        public String getContextRoot() {
            return contextRoot;
        }

        @XmlAttribute(name = "contextRoot")
        public void setContextRoot(String contextRoot) {
            this.contextRoot = contextRoot;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        @XmlAttribute(name = "enabled")
        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Boolean getIsPublic() {
            return isPublic;
        }

        @XmlAttribute(name = "public")
        public void setIsPublic(Boolean isPublic) {
            this.isPublic = isPublic;
        }

        @Override
        public String toString() {
            return "WebModuleDoc [contextRoot=" + contextRoot + ", enabled=" + enabled + ", public=" + isPublic + "]";
        }
    }
}
