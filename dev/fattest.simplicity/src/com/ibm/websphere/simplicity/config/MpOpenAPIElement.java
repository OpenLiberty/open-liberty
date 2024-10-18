/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import componenttest.topology.impl.LibertyServer;

public class MpOpenAPIElement extends ConfigElement {

    private String docPath;
    private String uiPath;
    private String openAPIVersion;

    @XmlElement(name = "includeApplication")
    protected List<String> includedApplications;

    @XmlElement(name = "excludeApplication")
    protected List<String> excludedApplications;

    @XmlElement(name = "includeModule")
    protected List<String> includedModules;

    @XmlElement(name = "excludeModule")
    protected List<String> excludedModules;

    protected MpOpenAPIInfoElement info;

    /**
     * @return the docPath
     */
    public String getDocPath() {
        return docPath;
    }

    /**
     * @param docPath the docPath to set
     */
    @XmlAttribute(name = "docPath")
    public void setDocPath(String docPath) {
        this.docPath = docPath;
    }

    /**
     * @return the uiPath
     */
    public String getUiPath() {
        return uiPath;
    }

    /**
     * @param uiPath the uiPath to set
     */
    @XmlAttribute(name = "uiPath")
    public void setUiPath(String uiPath) {
        this.uiPath = uiPath;
    }

    /**
     * @return a list of applications to be included.
     */
    public List<String> getIncludedApplications() {
        return (includedApplications == null) ? (includedApplications = new ArrayList<String>()) : includedApplications;
    }

    /**
     * @return a list of applications to be excluded.
     */

    public List<String> getExcludedApplications() {
        return (excludedApplications == null) ? (excludedApplications = new ArrayList<String>()) : excludedApplications;
    }

    /**
     * @return a list of modules to be included
     */
    public List<String> getIncludedModules() {
        return (includedModules == null) ? (includedModules = new ArrayList<String>()) : includedModules;
    }

    /**
     * @return a list of modules to be excluded
     */
    public List<String> getExcludedModules() {
        return (excludedModules == null) ? (excludedModules = new ArrayList<String>()) : excludedModules;
    }

    @XmlElement
    public MpOpenAPIInfoElement getInfo() {
        return info;
    }

    public void setInfo(MpOpenAPIInfoElement info) {
        this.info = info;
    }

    public String getOpenApiVersion() {
        return openAPIVersion;
    }

    @XmlAttribute(name = "openAPIVersion")
    public void setOpenApiVersion(String openApiVersion) {
        this.openAPIVersion = openApiVersion;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("MpOpenAPIElement [");
        sb.append("docPath=").append(docPath);
        sb.append(", uiPath=").append(uiPath);
        sb.append(", includeApplication=").append("[" + String.join(",", getIncludedApplications()) + "]");
        sb.append(", excludeApplication=").append("[" + String.join(",", getExcludedApplications()) + "]");
        sb.append(", includeModule=").append("[" + String.join(",", getIncludedModules()) + "]");
        sb.append(", excludeModule=").append("[" + String.join(",", getExcludedModules()) + "]");
        sb.append(", openAPIVersion=").append(openAPIVersion);
        sb.append("]");

        return sb.toString();
    }

    public static class MpOpenAPIElementBuilder {

        private final static Logger LOG = Logger.getLogger("MpOpenAPIElementBuilder");

        public static MpOpenAPIElementBuilder cloneBuilderFromServer(LibertyServer server) throws CloneNotSupportedException, Exception {
            return new MpOpenAPIElementBuilder(server);
        }

        public static MpOpenAPIElementBuilder cloneBuilderFromServerResetAppsAndModules(LibertyServer server) throws CloneNotSupportedException, Exception {
            MpOpenAPIElementBuilder builder = new MpOpenAPIElementBuilder(server);
            nullSafeClear(builder.element.excludedApplications);
            nullSafeClear(builder.element.includedApplications);
            nullSafeClear(builder.element.excludedModules);
            nullSafeClear(builder.element.excludedModules);
            return builder;
        }

        private final MpOpenAPIElement element;
        private final LibertyServer server;
        private final ServerConfiguration serverConfig;

        private MpOpenAPIElementBuilder(LibertyServer server) throws CloneNotSupportedException, Exception {
            this.server = server;
            this.serverConfig = server.getServerConfiguration().clone();
            this.element = serverConfig.getMpOpenAPIElement();
        }

        public void buildAndPushToServer() throws Exception {
            LOG.info("Pushing new server configuration: " + serverConfig.toString());
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(serverConfig);
            if (server.isStarted()) {
                server.waitForStringInLogUsingMark("CWWKG0017I"); //The server configuration was successfully updated
                //Setting a low timeout because this only appears if OpenAPI trace is enabled (and enabling trace breaks debuggers)
                server.waitForStringInLogUsingMark("Finished creating OpenAPI provider", 500);
            }
        }

        public MpOpenAPIElement build() throws Exception {
            return element;
        }

        public void buildAndOverwrite(MpOpenAPIElement other) {
            other.docPath = element.docPath;
            other.uiPath = element.uiPath;
            other.includedApplications = element.getIncludedApplications();
            other.excludedApplications = element.getExcludedApplications();
            other.includedModules = element.getIncludedModules();
            other.excludedModules = element.getExcludedModules();
        }

        public MpOpenAPIElementBuilder setDocPath(String docPath) {
            element.docPath = docPath;
            return this;
        }

        public MpOpenAPIElementBuilder setUiPath(String uiPath) {
            element.uiPath = uiPath;
            return this;
        }

        public MpOpenAPIElementBuilder addIncludedApplicaiton(String application) {
            element.getIncludedApplications().add(application);
            return this;
        }

        public MpOpenAPIElementBuilder addIncludedApplicaiton(List<String> applications) {
            element.getIncludedApplications().addAll(applications);
            return this;
        }

        public MpOpenAPIElementBuilder addIncludedModule(String module) {
            element.getIncludedModules().add(module);
            return this;
        }

        public MpOpenAPIElementBuilder addIncludedModule(List<String> module) {
            element.getIncludedModules().addAll(module);
            return this;
        }

        public MpOpenAPIElementBuilder addExcludedApplicaiton(String application) {
            element.getExcludedApplications().add(application);
            return this;
        }

        public MpOpenAPIElementBuilder addExcludedApplicaiton(List<String> applications) {
            element.getExcludedApplications().addAll(applications);
            return this;
        }

        public MpOpenAPIElementBuilder addExcludedModule(String module) {
            element.getExcludedModules().add(module);
            return this;
        }

        public MpOpenAPIElementBuilder addExcludedModule(List<String> module) {
            element.getExcludedModules().addAll(module);
            return this;
        }

        private static void nullSafeClear(Collection<?> c) {
            if (c != null) {
                c.clear();
            }
        }
    }

}
