/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents a single {@code <ejbTiming>} child element inside {@code <requestTiming>}.
 *
 * <pre>
 * &lt;requestTiming ...&gt;
 *   &lt;ejbTiming requestType="async" appName="MyApp" moduleName="MyModule"
 *              beanName="MyBean" methodName="myMethod"
 *              slowRequestThreshold="10s" hungRequestThreshold="60s"/&gt;
 * &lt;/requestTiming&gt;
 * </pre>
 */
public class EJBTimingElement extends ConfigElement {

    private String requestType;
    private String appName;
    private String moduleName;
    private String beanName;
    private String methodName;
    private String slowRequestThreshold;
    private String hungRequestThreshold;
    private Boolean enableThreadDumps;
    private Boolean interruptHungRequests;

    public String getRequestType() {
        return requestType;
    }

    @XmlAttribute(name = "requestType")
    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getAppName() {
        return appName;
    }

    @XmlAttribute(name = "appName")
    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getModuleName() {
        return moduleName;
    }

    @XmlAttribute(name = "moduleName")
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getBeanName() {
        return beanName;
    }

    @XmlAttribute(name = "beanName")
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getMethodName() {
        return methodName;
    }

    @XmlAttribute(name = "methodName")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getSlowRequestThreshold() {
        return slowRequestThreshold;
    }

    @XmlAttribute(name = "slowRequestThreshold")
    public void setSlowRequestThreshold(String slowRequestThreshold) {
        this.slowRequestThreshold = slowRequestThreshold;
    }

    public String getHungRequestThreshold() {
        return hungRequestThreshold;
    }

    @XmlAttribute(name = "hungRequestThreshold")
    public void setHungRequestThreshold(String hungRequestThreshold) {
        this.hungRequestThreshold = hungRequestThreshold;
    }

    public Boolean getEnableThreadDumps() {
        return enableThreadDumps;
    }

    @XmlAttribute(name = "enableThreadDumps")
    public void setEnableThreadDumps(Boolean enableThreadDumps) {
        this.enableThreadDumps = enableThreadDumps;
    }

    public Boolean getInterruptHungRequests() {
        return interruptHungRequests;
    }

    @XmlAttribute(name = "interruptHungRequests")
    public void setInterruptHungRequests(Boolean interruptHungRequests) {
        this.interruptHungRequests = interruptHungRequests;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("EJBTimingElement {");
        if (requestType != null)
            sb.append("requestType=\"").append(requestType).append("\" ");
        if (appName != null)
            sb.append("appName=\"").append(appName).append("\" ");
        if (moduleName != null)
            sb.append("moduleName=\"").append(moduleName).append("\" ");
        if (beanName != null)
            sb.append("beanName=\"").append(beanName).append("\" ");
        if (methodName != null)
            sb.append("methodName=\"").append(methodName).append("\" ");
        if (slowRequestThreshold != null)
            sb.append("slowRequestThreshold=\"").append(slowRequestThreshold).append("\" ");
        if (hungRequestThreshold != null)
            sb.append("hungRequestThreshold=\"").append(hungRequestThreshold).append("\" ");
        if (enableThreadDumps != null)
            sb.append("enableThreadDumps=\"").append(enableThreadDumps).append("\" ");
        if (interruptHungRequests != null)
            sb.append("interruptHungRequests=\"").append(interruptHungRequests).append("\" ");
        sb.append("}");
        return sb.toString();
    }
}
