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
package com.ibm.ws.jca.utils.xml.metatype;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.Utils;
import com.ibm.ws.jca.utils.Utils.ConstructType;

/**
 * Metatype Designate
 */
@Trivial
public class MetatypeDesignate {
    private String pid;
    private String factoryPid;
    private MetatypeObject object;
    private Object matchingRaXmlObject;
    private ConstructType childOcdType;

    public String getDesignateID() {
        if (pid != null)
            return pid;

        if (factoryPid != null)
            return factoryPid;

        return null;
    }

    public ConstructType getChildOcdType() {
        return childOcdType;
    }

    public Object getMatchingRaXmlObject() {
        return matchingRaXmlObject;
    }

    public void setInternalInformation(ConstructType type, Object matchingRaXmlObject) {
        this.matchingRaXmlObject = matchingRaXmlObject;
        childOcdType = type;
    }

    @XmlAttribute(name = "pid")
    public void setPid(String pid) {
        this.pid = pid;
    }

    @XmlAttribute(name = "factoryPid")
    public void setFactoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
    }

    @XmlElement(name = "Object")
    public void setObject(MetatypeObject object) {
        this.object = object;
    }

    public String getPid() {
        return this.pid;
    }

    public String getFactoryPid() {
        return this.factoryPid;
    }

    public MetatypeObject getObject() {
        return this.object;
    }

    @Override
    public String toString() {
        if (pid != null)
            return "MetatypeDesignate{pid='" + pid + "'}";
        else if (factoryPid != null)
            return "MetatypeDesignate{factoryPid='" + factoryPid + "'}";
        else
            return "MetatypeDesignate{}";
    }

    public String toMetatypeString(int padSpaces) {
        String buffer = Utils.getSpaceBufferString(padSpaces);
        String subBuffer = Utils.getSpaceBufferString(padSpaces + 1);
        StringBuilder sb = new StringBuilder(buffer).append("<Designate ");

        if (pid != null)
            sb.append("pid=\"").append(pid).append("\" ");
        if (factoryPid != null)
            sb.append("factoryPid=\"").append(factoryPid).append("\" ");

        sb.append(">");
        sb.append(subBuffer).append(object.toMetatypeString(0));

        sb.append(buffer).append("</Designate>");
        return sb.toString();
    }
}
