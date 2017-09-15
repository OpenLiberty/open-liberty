/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.xpath.mapping.datatype;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class PropertyNode implements XPathPropertyNode {

    private String operator = null;
    private String name = null;
    private Object value = null;
    private boolean inRepos = true;

    @Override
    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setValue(Object value) {

        this.value = value;
    }

    @Override
    public Object getValue() {
        return value;
    }

    @Override
    public short getNodeType() {
        return XPathNode.NODE_PROPERTY;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator getPropertyNodes(HashMap attNodeMap) {
        HashMap nodeMap = attNodeMap;

        if (nodeMap == null) {
            nodeMap = new HashMap();
        }
        nodeMap.put(Integer.valueOf(this.hashCode()), this);
        return nodeMap.values().iterator();
    }

    @Override
    public void setPropertyLocation(boolean inRepos) {
        this.inRepos = inRepos;
    }

    @Override
    public boolean isPropertyInRepository() {
        return inRepos;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer();
        s = s.append(name + " " + operator + " " + value.toString());
        return s.toString();
    }
}