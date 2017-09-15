/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.wlp.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * wlp-ra.xml ibmui:groups element
 */
@Trivial
public class WlpIbmuiGroups {
    @XmlAttribute(name = "scope")
    private String scope;
    @XmlElement(name = "group")
    private final List<IbmuiGroup> groups = new LinkedList<IbmuiGroup>();

    public String getScope() {
        return scope;
    }

    public List<IbmuiGroup> getGroups() {
        return groups;
    }

    public String getGroupNLSKey(String groupName) {
        if (!groups.isEmpty()) {
            for (IbmuiGroup group : groups)
                if (group.name.equals(groupName))
                    return group.nlsKey;
        }

        return null;
    }

    public String getGroupOrder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < groups.size(); ++i) {
            sb.append(groups.get(i).getName());

            if (i + 1 != groups.size())
                sb.append(',');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        if (scope != null)
            sb.append("scope='").append(scope).append("' ");
        if (!groups.isEmpty()) {
            sb.append("groups=[");
            for (int i = 0; i < groups.size(); ++i) {
                sb.append(groups.get(i));

                if (i + 1 != groups.size())
                    sb.append(", ");
            }
            sb.append("] ");
        }
        sb.append('}');
        return sb.toString();
    }

    public static class IbmuiGroup {
        @XmlAttribute(name = "name")
        private String name;
        @XmlAttribute(name = "nlsKey")
        private String nlsKey;

        public String getName() {
            return name;
        }

        public String getNLSKey() {
            return nlsKey;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
            sb.append("name='").append(name).append("' ");
            sb.append("nlsKey='").append(nlsKey).append("' ");
            sb.append('}');
            return sb.toString();
        }
    }
}
