/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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

import com.ibm.websphere.simplicity.config.context.ClassloaderContext;
import com.ibm.websphere.simplicity.config.context.JEEMetadataContext;
import com.ibm.websphere.simplicity.config.context.SecurityContext;
import com.ibm.websphere.simplicity.config.context.SyncToOSThreadContext;
import com.ibm.websphere.simplicity.config.context.ZOSWLMContext;

/**
 * Represents the <contextService> element in server.xml
 */
public class ContextService extends ConfigElement {
    // attributes

    private String baseContextRef;
    private String jndiName;
    private String onError;

    // nested elements

    @XmlElement(name = "classloaderContext")
    private ConfigElementList<ClassloaderContext> classloaderContexts;

    @XmlElement(name = "jeeMetadataContext")
    private ConfigElementList<JEEMetadataContext> jeeMetadataContexts;

    @XmlElement(name = "securityContext")
    private ConfigElementList<SecurityContext> securityContexts;

    @XmlElement(name = "syncToOSThreadContext")
    private ConfigElementList<SyncToOSThreadContext> syncToOSThreadContexts;

    @XmlElement(name = "zosWLMContext")
    private ConfigElementList<ZOSWLMContext> zosWLMContexts;

    public String getBaseContextRef() {
        return baseContextRef;
    }

    public String getJndiName() {
        return jndiName;
    }

    public String getOnError() {
        return onError;
    }

    // only one nested <classloaderContext> is valid, but we must allow for testing invalid config, too
    public ConfigElementList<ClassloaderContext> getClassloaderContexts() {
        return classloaderContexts == null ? (classloaderContexts = new ConfigElementList<ClassloaderContext>()) : classloaderContexts;
    }

    // only one nested <jeeMetadataContext> is valid, but we must allow for testing invalid config, too
    public ConfigElementList<JEEMetadataContext> getJEEMetadataContexts() {
        return jeeMetadataContexts == null ? (jeeMetadataContexts = new ConfigElementList<JEEMetadataContext>()) : jeeMetadataContexts;
    }

    // only one nested <securityContext> is valid, but we must allow for testing invalid config, too
    public ConfigElementList<SecurityContext> getSecurityContexts() {
        return securityContexts == null ? (securityContexts = new ConfigElementList<SecurityContext>()) : securityContexts;
    }

    // only one nested <syncToOSThreadContext> is valid, but we must allow for testing invalid config, too
    public ConfigElementList<SyncToOSThreadContext> getSyncToOSThreadContexts() {
        return syncToOSThreadContexts == null ? (syncToOSThreadContexts = new ConfigElementList<SyncToOSThreadContext>()) : syncToOSThreadContexts;
    }

    // only one nested <zosWLMContext> is valid, but we must allow for testing invalid config, too
    public ConfigElementList<ZOSWLMContext> getZOSWLMContexts() {
        return zosWLMContexts == null ? (zosWLMContexts = new ConfigElementList<ZOSWLMContext>()) : zosWLMContexts;
    }

    @XmlAttribute
    public void setBaseContextRef(String baseContextRef) {
        this.baseContextRef = baseContextRef;
    }

    @XmlAttribute
    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    @XmlAttribute
    public void setOnError(String onError) {
        this.onError = onError;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append('{');
        if (getId() != null)
            buf.append("id=").append(getId()).append(' ');
        if (baseContextRef != null)
            buf.append("baseContextRef=").append(baseContextRef).append(' ');
        if (jndiName != null)
            buf.append("jndiName=").append(jndiName).append(' ');
        if (onError != null)
            buf.append("onError=").append(onError).append(' ');
        if (classloaderContexts != null)
            buf.append(classloaderContexts).append(' ');
        if (jeeMetadataContexts != null)
            buf.append(jeeMetadataContexts).append(' ');
        if (securityContexts != null)
            buf.append(securityContexts).append(' ');
        if (syncToOSThreadContexts != null)
            buf.append(syncToOSThreadContexts).append(' ');
        if (zosWLMContexts != null)
            buf.append(zosWLMContexts).append(' ');
        buf.append('}');
        return buf.toString();
    }
}
