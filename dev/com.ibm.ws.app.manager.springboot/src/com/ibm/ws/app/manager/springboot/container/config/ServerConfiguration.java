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
package com.ibm.ws.app.manager.springboot.container.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
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
 * Represents a server configuration document for the WAS 8.5 Liberty Profile.
 */
@XmlRootElement(name = "server")
public class ServerConfiguration implements Cloneable {

    private String description;

    @XmlElement(name = "httpEndpoint")
    private ConfigElementList<HttpEndpoint> httpEndpoints;

    @XmlElement(name = "virtualHost")
    private ConfigElementList<VirtualHost> virtualHosts;

    @XmlElement(name = "httpSession")
    private HttpSession httpSession;

    @XmlElement(name = "connectionManager")
    private ConfigElementList<ConnectionManager> connManagers;

    @XmlElement(name = "logging")
    private Logging logging;

    @XmlElement(name = "config")
    private ConfigMonitorElement config;

    @XmlElement(name = "webContainer")
    private WebContainerElement webContainer;

    @XmlElement(name = "ssl")
    private ConfigElementList<SSLConfig> ssls;

    @XmlElement(name = "keyStore")
    private ConfigElementList<KeyStore> keyStores;

    @XmlElement(name = "jspEngine")
    private JspEngineElement jspEngine;

    @XmlElement(name = "authData")
    private ConfigElementList<AuthData> authDataElements;

    @XmlElement(name = "variable")
    private ConfigElementList<Variable> variables;

    @XmlAnyAttribute
    private Map<QName, Object> unknownAttributes;

    @XmlAnyElement
    private List<Element> unknownElements;

    public ServerConfiguration() {
        this.description = "Generation date: " + new Date();
    }

    public void addConnectionManager(ConnectionManager connManager) {
        if (connManagers == null)
            connManagers = new ConfigElementList<ConnectionManager>();
        connManagers.add(connManager);
    }

    public boolean removeConnectionManagerById(String id) {
        if (connManagers == null)
            return false;

        for (ConnectionManager connManager : connManagers)
            if (connManager.getId().equals(id))
                return connManagers.remove(connManager);
        return false;
    }

    public ConfigElementList<AuthData> getAuthDataElements() {
        if (this.authDataElements == null) {
            this.authDataElements = new ConfigElementList<AuthData>();
        }
        return this.authDataElements;
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
     * Retrieves the list of HttpEndpoints in this configuration
     *
     * @return the list of HttpEndpoints in this configuration
     */
    public ConfigElementList<HttpEndpoint> getHttpEndpoints() {
        if (this.httpEndpoints == null) {
            this.httpEndpoints = new ConfigElementList<HttpEndpoint>();
        }
        return this.httpEndpoints;
    }

    /**
     * Retrieves the list of VirtualHosts in this configuration
     *
     * @return the list of VirtualHosts in this configuration
     */
    public ConfigElementList<VirtualHost> getVirtualHosts() {
        if (this.virtualHosts == null) {
            this.virtualHosts = new ConfigElementList<>();
        }
        return this.virtualHosts;
    }

    /**
     * @return the HTTP session manager configuration for this server
     */
    public HttpSession getHttpSession() {
        if (this.httpSession == null) {
            this.httpSession = new HttpSession();
        }
        return this.httpSession;
    }

    /**
     * @return the WebContainer configuration for this server
     */
    public WebContainerElement getWebContainer() {
        if (this.webContainer == null) {
            this.webContainer = new WebContainerElement();
        }
        return this.webContainer;
    }

    /**
     * @return the ssl configurations for this server
     */
    public ConfigElementList<SSLConfig> getSsls() {
        if (this.ssls == null) {
            this.ssls = new ConfigElementList<SSLConfig>();
        }
        return this.ssls;
    }

    /**
     * @return the KeyStore configurations for this server
     */
    public ConfigElementList<KeyStore> getKeyStores() {
        if (this.keyStores == null) {
            this.keyStores = new ConfigElementList<KeyStore>();
        }
        return this.keyStores;
    }

    /**
     * @return the Jsp configuration for this server
     */
    public JspEngineElement getJspEngine() {
        if (this.jspEngine == null) {
            this.jspEngine = new JspEngineElement();
        }
        return this.jspEngine;
    }

    /**
     * @return the connection managers
     */
    public ConfigElementList<ConnectionManager> getConnectionManagers() {
        if (this.connManagers == null)
            this.connManagers = new ConfigElementList<ConnectionManager>();

        return this.connManagers;
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
     * @return all configured {@code <variable>} elements
     */
    public ConfigElementList<Variable> getVariables() {
        if (this.variables == null) {
            this.variables = new ConfigElementList<Variable>();
        }
        return this.variables;
    }

    private List<Field> getAllXmlElements() {
        List<Field> xmlElements = new ArrayList<Field>();
        for (Field field : getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(XmlElement.class))
                xmlElements.add(field);
        }
        return xmlElements;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ServerConfiguration clone() throws CloneNotSupportedException {
        ServerConfiguration clone = (ServerConfiguration) super.clone();

        for (Field field : getAllXmlElements()) {
            try {
                Object val = field.get(this);
                if (val instanceof ConfigElementList) {
                    field.set(clone, ((ConfigElementList<ConfigElement>) val).clone());
                } else if (val != null) {
                    field.set(clone, ((ConfigElement) val).clone());
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error on field: " + field);
            }
        }
        return clone;
    }

    @Override
    public String toString() {
        String nl = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer("ServerConfiguration" + nl);

        for (Field field : getAllXmlElements()) {
            try {
                buf.append(field.get(this).toString());
            } catch (Exception ignore) {
            }
        }
        return buf.toString();
    }

    @Override
    public boolean equals(Object otherConfig) {
        if (otherConfig == null)
            return false;
        if (!(otherConfig instanceof ServerConfiguration))
            return false;

        // Consider server configurations equal if their XmlElements match up
        for (Field field : getAllXmlElements()) {
            try {
                Object thisVal = field.get(this);
                Object otherVal = field.get(otherConfig);
                if (!(thisVal == null ? otherVal == null : thisVal.equals(otherVal)))
                    return false;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    /**
     * Calls modify() on elements in the configuration that implement the ModifiableConfigElement interface.
     *
     * @throws Exception when the update fails
     */
    public void updateDatabaseArtifacts() throws Exception {
        List<ModifiableConfigElement> mofiableElementList = new ArrayList<ModifiableConfigElement>();
        findModifiableConfigElements(this, mofiableElementList);

        for (ModifiableConfigElement element : mofiableElementList) {
            element.modify(this);
        }
    }

    /**
     * Finds all of the objects in the given config element that implement the
     * ModifiableConfigElement interface.
     *
     * @param element The config element to check.
     * @param modifiableConfigElements The list containing all modifiable elements.
     * @throws Exception
     */
    private void findModifiableConfigElements(Object element, List<ModifiableConfigElement> modifiableConfigElements) throws Exception {

        // If the current element implements ModifiableConfigElement add it to the list.
        if (element instanceof ModifiableConfigElement) {
            modifiableConfigElements.add((ModifiableConfigElement) element);
        }

        // Iterate over all of the elements.
        for (Field field : element.getClass().getDeclaredFields()) {
            if (!field.isAccessible())
                field.setAccessible(true);

            Object fieldValue = field.get(element);

            if (fieldValue != null) {
                if (fieldValue instanceof ConfigElement) {
                    findModifiableConfigElements(fieldValue, modifiableConfigElements);
                } else if (fieldValue instanceof ConfigElementList) {
                    for (ConfigElement e : (ConfigElementList<?>) fieldValue)
                        findModifiableConfigElements(e, modifiableConfigElements);
                }
            }
        }
    }

    /**
     * Removes an unknown element, by tag name. One might use this to remove the
     * configuration for a feature which is not part of the product, for example one
     * that is built and installed by a FAT bucket.
     *
     * @param tagName The tag name that should be removed.
     *
     * @return A list of the items that were removed.
     */
    public List<Element> removeUnknownElement(String tagName) {
        List<Element> removedElements = new LinkedList<Element>();
        Iterator<Element> i = unknownElements.iterator();
        while ((i != null) && (i.hasNext())) {
            Element e = i.next();
            if ((e != null) && (e.getTagName().equals(tagName))) {
                removedElements.add(e);
                i.remove();
            }
        }
        return removedElements;
    }

    /**
     * Adds elements previously removed from the unknown elements list. This was intended
     * to be used to add anything back to the configuration that was removed by the
     * removeUnknownElements() method.
     *
     * @param unknownElements The elements to add back to the configuration.
     */
    public void addUnknownElements(List<Element> unknownElements) {
        for (Element e : unknownElements) {
            this.unknownElements.add(e);
        }
    }
}
