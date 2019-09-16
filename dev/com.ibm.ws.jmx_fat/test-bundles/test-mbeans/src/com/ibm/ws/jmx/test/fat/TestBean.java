/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.test.fat;

import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

public class TestBean implements DynamicMBean {

    private final String name;

    protected TestBean(String name) {
        System.setProperty("com.ibm.ws.jmx.test.fat", "true");
        this.name = name;
    }

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service properties
     */
    protected void activate(Map<String, Object> properties) {
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param reason int representation of reason the component is stopping
     */
    protected void deactivate(int reason) {
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Object getAttribute(String arg0) throws AttributeNotFoundException, MBeanException, ReflectionException {
        return toString();
    }

    @Override
    public AttributeList getAttributes(String[] arg0) {
        return null;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(name, "test bean", null, null, null, null);
    }

    @Override
    public Object invoke(String arg0, Object[] arg1, String[] arg2) throws MBeanException, ReflectionException {
        return null;
    }

    @Override
    public void setAttribute(Attribute arg0) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {

    }

    @Override
    public AttributeList setAttributes(AttributeList arg0) {
        return null;
    }
}
