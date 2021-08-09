/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin;

import java.io.Serializable;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class ConfigID implements Serializable {

    private static final long serialVersionUID = -7188381474767207297L;

    private final String pid;
    private final String id;

    private final ConfigID parent;

    private final String childAttribute;

    public ConfigID(String pid) {
        this(null, pid, null, null);
    }

    public ConfigID(String pid, String id) {
        this(null, pid, id, null);
    }

    /**
     * @param parent
     * @param nodeName
     * @param id2
     */
    public ConfigID(ConfigID parent, String nodeName, String id2) {
        this(parent, nodeName, id2, null);
    }

    public ConfigID(ConfigID parent, String nodeName, String id, String childAttribute) {
        this.pid = nodeName;
        this.id = id;
        this.parent = parent;
        this.childAttribute = childAttribute;
    }

    public String getPid() {
        return pid;
    }

    public String getId() {
        return id;
    }

    /**
     * Translate config.id back into an ConfigID object
     * 
     * @param property
     * @return
     */
    public static ConfigID fromProperty(String property) {
        //TODO Perhaps there is a clever regex that could handle this
        String[] parents = property.split("//");

        ConfigID id = null;
        for (String parentString : parents) {
            id = constructId(id, parentString);
        }

        return id;
    }

    /**
     * @param parent2
     * @param parentString
     * @return
     */
    private static ConfigID constructId(ConfigID parent2, String parentString) {
        String childAttribute = parseChildAttribute(parentString);
        String pid = parsePid(parentString);
        String id = parseId(parentString);

        return new ConfigID(parent2, pid, id, childAttribute);
    }

    /**
     * @param parentString
     * @return
     */
    private static String parseId(String parentString) {
        int idx = parentString.indexOf('[');
        if (idx == -1)
            return null;

        return parentString.substring(idx + 1, parentString.indexOf(']'));
    }

    /**
     * @param parentString
     * @return
     */
    private static String parsePid(String parentString) {
        int idx = parentString.indexOf('(');
        if (idx == -1)
            idx = parentString.indexOf('[');
        if (idx == -1)
            return parentString;
        else
            return parentString.substring(0, idx);
    }

    /**
     * @param parentString
     * @return
     */
    private static String parseChildAttribute(String parentString) {
        int idx = parentString.indexOf('(');
        if (idx != -1) {
            return parentString.substring(idx + 1, parentString.indexOf(')'));
        }
        return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((pid == null) ? 0 : pid.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
        result = prime * result + ((childAttribute == null) ? 0 : childAttribute.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ConfigID)) {
            return false;
        }
        ConfigID other = (ConfigID) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (pid == null) {
            if (other.pid != null) {
                return false;
            }
        } else if (!pid.equals(other.pid)) {
            return false;
        }

        if (parent == null) {
            if (other.parent != null) {
                return false;
            }
        } else if (!parent.equals(other.parent)) {
            return false;
        }

        if (childAttribute == null) {
            if (other.childAttribute != null) {
                return false;
            }
        } else if (!childAttribute.equals(other.childAttribute)) {
            return false;
        }

        return true;
    }

    @Override
    @Trivial
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (this.parent != null) {
            buffer.append(parent.toString());
            buffer.append("//");
        }
        buffer.append(pid);

        if (childAttribute != null) {
            buffer.append('(').append(childAttribute).append(')');
        }
        if (id != null) {
            buffer.append('[').append(id).append(']');
        }

        return buffer.toString();
    }

    /**
     * @return
     */
    public ConfigID getParent() {
        return this.parent;
    }

    public String getChildAttribute() {
        return this.childAttribute;
    }

}
