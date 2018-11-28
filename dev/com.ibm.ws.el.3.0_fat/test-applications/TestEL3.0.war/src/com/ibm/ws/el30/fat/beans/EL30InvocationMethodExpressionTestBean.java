/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.el30.fat.beans;

/**
 * This is a simple bean to test invocation of method expressions
 */
public class EL30InvocationMethodExpressionTestBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private String parent;
    private final Child myChild;

    public EL30InvocationMethodExpressionTestBean() {
        myChild = new Child();
        parent = null;
    }

    public void setParentName(String parent) {
        this.parent = parent;
    }

    public String getParentName() {
        return parent;
    }

    public Child getChild() {
        return myChild;
    }

    @Override
    public String toString() {
        return "toString method of object with current parent name " + parent;
    }

    /**
     * Child class created to know the child of the parent
     */
    public class Child {

        private String childName;

        public Child() {
            this.childName = null;
        }

        public void setChildName(String name) {
            this.childName = name;
        }

        public String getChildName() {
            return childName;
        }
    }
}
