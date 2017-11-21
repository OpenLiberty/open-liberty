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

package com.ibm.websphere.simplicity.config.wim;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.simplicity.config.ConfigElement;
import com.ibm.websphere.simplicity.config.ConfigElementList;

/**
 * Configuration for the following nested elements:
 *
 * <ul>
 * <li>ldapRegistry --> failoverServers</li>
 * </ul>
 */
public class FailoverServers extends ConfigElement {

    private String name;
    private ConfigElementList<Server> servers;

    public FailoverServers() {}

    public FailoverServers(String name, String[][] servers) {
        this.name = name;

        for (String[] server : servers) {
            if (server.length == 2) {
                String host = server[0];
                String port = server[1];

                new Server(host, port);
            }
        }
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the server
     */
    public ConfigElementList<Server> getServers() {
        return (servers == null) ? (servers = new ConfigElementList<Server>()) : servers;
    }

    /**
     * @param name the name to set
     */
    @XmlAttribute(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param server the server to set
     */
    @XmlElement(name = "server")
    public void setServers(ConfigElementList<Server> servers) {
        this.servers = servers;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (name != null) {
            sb.append("name=\"").append(name).append("\" ");;
        }
        if (servers != null) {
            sb.append("servers=\"").append(servers).append("\" ");;
        }

        sb.append("}");

        return sb.toString();
    }
}