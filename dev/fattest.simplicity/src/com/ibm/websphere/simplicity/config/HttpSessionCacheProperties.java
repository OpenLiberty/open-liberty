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

public class HttpSessionCacheProperties extends ConfigElement {
    // attributes
    private String hazelcast_config_location;
    private String hazelcast_instance_name;

    public String getHazelcastConfigLocation() {
        return hazelcast_config_location;
    }

    public String getHazelcastInstanceName() {
        return hazelcast_instance_name;
    }

    @XmlAttribute(name = "hazelcast.config.location")
    public void setHazelcastConfigLocation(String value) {
        hazelcast_config_location = value;
    }

    @XmlAttribute(name = "hazelcast.instance.name")
    public void setHazelcastInstanceName(String value) {
        hazelcast_instance_name = value;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(getClass().getSimpleName()).append("{");
        if (hazelcast_config_location != null)
            buf.append("hazelcast.config.location=\"" + hazelcast_config_location + "\" ");
        if (hazelcast_instance_name != null)
            buf.append("hazelcast.instance.name=\"" + hazelcast_instance_name + "\" ");

        buf.append("}");
        return buf.toString();
    }
}