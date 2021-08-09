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
public class LogicalNode implements XPathLogicalNode {

    private String operator = null;
    private Object leftChild = null;
    private Object rightChild = null;
    boolean inRepos = true;

    @Override
    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String getOperator() {
        return operator;
    }

    @Override
    public void setLeftChild(Object leftChild) {
        this.leftChild = leftChild;
    }

    @Override
    public Object getLeftChild() {
        return leftChild;
    }

    @Override
    public void setRightChild(Object rightChild) {
        this.rightChild = rightChild;
    }

    @Override
    public Object getRightChild() {
        return rightChild;
    }

    @Override
    public short getNodeType() {
        return XPathNode.NODE_LOGICAL;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator getPropertyNodes(HashMap attNodeMap) {
        HashMap nodeMap = attNodeMap;

        if (nodeMap == null) {
            nodeMap = new HashMap();
        }
        ((XPathNode) leftChild).getPropertyNodes(nodeMap);
        return ((XPathNode) rightChild).getPropertyNodes(nodeMap);
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
        s = s.append(leftChild.toString() + " " + operator + " " + rightChild.toString());
        return s.toString();
    }
}
