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

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

/**
 * Represents a client configuration document for the WAS 8.5 Liberty Profile.
 */
@XmlRootElement(name = "client")
public class ClientConfiguration implements Cloneable {

    private String description;
    @XmlElement(name = "featureManager")
    private FeatureManager featureManager;

    @XmlElement(name = "activationSpec")
    private ConfigElementList<ActivationSpec> activationSpecs;

    @XmlElement(name = "adminObject")
    private ConfigElementList<AdminObject> adminObjects;

    @XmlElement(name = "basicRegistry")
    private ConfigElementList<BasicRegistry> basicRegistries;

    @XmlElement(name = "application")
    private ConfigElementList<Application> applications;

    @XmlElement(name = "library")
    private ConfigElementList<Library> libraries;

    @XmlElement(name = "fileset")
    private ConfigElementList<Fileset> filesets;

    @XmlElement(name = "logging")
    private Logging logging;

    @XmlElement(name = "include")
    private ConfigElementList<IncludeElement> includeElements;

    @XmlElement(name = "executor")
    private ExecutorElement executor;

    @XmlElement(name = "config")
    private ConfigMonitorElement config;

    @XmlElement(name = "keyStore")
    private KeyStore keyStore;

    @XmlElement(name = "variable")
    private ConfigElementList<Variable> variables;

    @XmlElement(name = "classloading")
    private ClassloadingElement classLoading;

    @XmlAnyAttribute
    private Map<QName, Object> unknownAttributes;

    @XmlAnyElement
    private List<Element> unknownElements;

    public ClientConfiguration() {
        this.description = "Generation date: " + new Date();
    }

    public ConfigElementList<ActivationSpec> getActivationSpecs() {
        if (this.activationSpecs == null)
            this.activationSpecs = new ConfigElementList<ActivationSpec>();
        return this.activationSpecs;
    }

    public ConfigElementList<AdminObject> getAdminObjects() {
        if (this.adminObjects == null)
            this.adminObjects = new ConfigElementList<AdminObject>();
        return this.adminObjects;
    }

    /**
     * Retrieves a description of this configuration.
     * 
     * @return a description of this configuration
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Sets the description of this configuration
     * 
     * @param description the description of this configuration
     */
    @XmlAttribute
    public void setDescription(String description) {
        this.description = ConfigElement.getValue(description);
    }

    /**
     * @return the featureManager for this configuration
     */
    public FeatureManager getFeatureManager() {
        if (this.featureManager == null) {
            this.featureManager = new FeatureManager();
        }
        return this.featureManager;
    }

    public ConfigElementList<BasicRegistry> getBasicRegistries() {
        if (this.basicRegistries == null) {
            this.basicRegistries = new ConfigElementList<BasicRegistry>();
        }
        return this.basicRegistries;
    }

    /**
     * @return the KeyStore configuration for this client
     */
    public KeyStore getKeyStore() {
        if (this.keyStore == null) {
            this.keyStore = new KeyStore();
        }
        return this.keyStore;
    }

    /**
     * @return the includeElement
     */
    public ConfigElementList<IncludeElement> getIncludes() {
        if (this.includeElements == null)
            this.includeElements = new ConfigElementList<IncludeElement>();
        return this.includeElements;
    }

    /**
     * @return the ExecutorElement
     */
    public ExecutorElement getExecutor() {
        if (this.executor == null)
            this.executor = new ExecutorElement();
        return this.executor;
    }

    public void setExecutorElement(ExecutorElement exec) {
        this.executor = exec;
    }

    /**
     * @return the applicationMonitor
     */
    public ClassloadingElement getClassLoadingElement() {

        if (this.classLoading == null)
            this.classLoading = new ClassloadingElement();

        return this.classLoading;
    }

    /**
     * @return explicitly installed applications
     */
    public ConfigElementList<Application> getApplications() {
        if (this.applications == null) {
            this.applications = new ConfigElementList<Application>();
        }
        return this.applications;
    }

    /**
     * Removes all applications with a specific name
     * 
     * @param name
     *            the name of the applications to remove
     * @return the removed applications (no longer bound to the client
     *         configuration)
     */
    public ConfigElementList<Application> removeApplicationsByName(String name) {
        ConfigElementList<Application> installedApps = this.getApplications();
        ConfigElementList<Application> uninstalledApps = new ConfigElementList<Application>();
        for (Application app : installedApps) {
            if (name != null && name.equals(app.getName())) {
                uninstalledApps.add(app);
            }
        }
        installedApps.removeAll(uninstalledApps);
        return uninstalledApps;
    }

    /**
     * Adds an application to the current config, or updates an application with
     * a specific name if it already exists
     * 
     * @param name
     *            the name of the application
     * @param path
     *            the fully qualified path to the application archive on the
     *            liberty machine
     * @param type
     *            the type of the application (ear/war/etc)
     * @return the deployed application
     */
    public Application addApplication(String name, String path, String type) {
        ConfigElementList<Application> apps = this.getApplications();
        Application application = null;
        for (Application app : apps) {
            if (name != null && name.equals(app.getName())) {
                application = app;
            }
        }
        if (application == null) {
            application = new Application();
            apps.add(application);
        }
        application.setName(name);
        application.setId(name); // application names must be unique, just like element ID names (other config objects probably aren't sharing the app name)
        application.setType(type);
        application.setLocation(path); // assumes that archive has already been transferred; see FileSetup.java
        return application;
    }

    /**
     * @return gets all configured shared libraries
     */
    public ConfigElementList<Library> getLibraries() {
        if (this.libraries == null) {
            this.libraries = new ConfigElementList<Library>();
        }
        return this.libraries;
    }

    /**
     * @return gets all configured file sets
     */
    public ConfigElementList<Fileset> getFilesets() {
        if (this.filesets == null) {
            this.filesets = new ConfigElementList<Fileset>();
        }
        return this.filesets;
    }

    /**
     * @return get fileset by id
     */
    public Fileset getFilesetById(String id) {
        if (this.filesets != null)
            for (Fileset fileset : this.filesets)
                if (fileset.getId().equals(id))
                    return fileset;

        return null;
    }

    /**
     * @return gets logging configuration
     */
    public Logging getLogging() {
        if (this.logging == null) {
            this.logging = new Logging();
        }
        return this.logging;
    }

    /**
     * @return the config
     */
    public ConfigMonitorElement getConfig() {
        if (this.config == null) {
            this.config = new ConfigMonitorElement();
        }
        return config;
    }

    /**
     * @return all configured <variable> elements
     */
    public ConfigElementList<Variable> getVariables() {
        if (this.variables == null) {
            this.variables = new ConfigElementList<Variable>();
        }
        return this.variables;
    }

    @Override
    public ClientConfiguration clone() throws CloneNotSupportedException {
        ClientConfiguration clone = (ClientConfiguration) super.clone();

        // clone activationSpec
        if (this.activationSpecs != null) {
            clone.activationSpecs = new ConfigElementList<ActivationSpec>();
            for (ActivationSpec activationSpec : this.activationSpecs)
                clone.activationSpecs.add((ActivationSpec) activationSpec.clone());
        }

        // clone adminObjects
        if (this.adminObjects != null) {
            clone.adminObjects = new ConfigElementList<AdminObject>();
            for (AdminObject adminObject : this.adminObjects)
                clone.adminObjects.add((AdminObject) adminObject.clone());
        }

        // clone includeElements
        if (this.includeElements != null) {
            clone.includeElements = new ConfigElementList<IncludeElement>();
            for (IncludeElement include : this.includeElements)
                clone.includeElements.add((IncludeElement) include.clone());
        }

        // clone featureManager - must explicitly clone due to TreeSet in FeatureManager
        if (this.featureManager != null)
            clone.featureManager = this.featureManager.clone();

        // clone httpEndpoints
        if (this.basicRegistries != null) {
            clone.basicRegistries = new ConfigElementList<BasicRegistry>();
            for (BasicRegistry basicRegistry : basicRegistries)
                clone.basicRegistries.add(basicRegistry.clone());
        }

        // clone applications
        if (this.applications != null) {
            clone.applications = new ConfigElementList<Application>();
            for (Application app : this.applications)
                clone.applications.add(app.clone());
        }

        // clone libraries
        if (this.libraries != null) {
            clone.libraries = new ConfigElementList<Library>();
            for (Library lib : this.libraries)
                clone.libraries.add((Library) lib.clone());
        }

        // clone filesets
        if (this.filesets != null) {
            clone.filesets = new ConfigElementList<Fileset>();
            for (Fileset fileset : this.filesets)
                clone.filesets.add((Fileset) fileset.clone());
        }

        // clone variables
        if (this.variables != null) {
            clone.variables = new ConfigElementList<Variable>();
            for (Variable variable : this.variables)
                clone.variables.add((Variable) variable.clone());
        }

        return clone;
    }

    @Override
    public String toString() {
        String nl = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer("ClientConfiguration{" + this.description + "}" + nl);

        if (featureManager != null)
            buf.append(featureManager.toString() + nl);
        if (activationSpecs != null)
            for (ActivationSpec activationSpec : activationSpecs)
                buf.append(activationSpec.toString()).append(nl);
        if (adminObjects != null)
            for (AdminObject adminObject : adminObjects)
                buf.append(adminObject.toString()).append(nl);
        if (applications != null)
            for (Application app : applications)
                buf.append(app.toString() + nl);
        if (libraries != null)
            for (Library lib : libraries)
                buf.append(lib.toString() + nl);
        if (filesets != null)
            for (Fileset fileset : filesets)
                buf.append(fileset.toString() + nl);
        if (logging != null)
            buf.append(logging.toString() + nl);
        if (includeElements != null)
            for (IncludeElement include : includeElements)
                buf.append(include.toString() + nl);
        if (executor != null)
            buf.append(executor.toString() + nl);
        if (config != null)
            buf.append(config.toString() + nl);
        if (variables != null)
            for (Variable variable : variables)
                buf.append(variable.toString() + nl);
        if (keyStore != null)
            buf.append(keyStore.toString() + nl);

        return buf.toString();
    }
}
