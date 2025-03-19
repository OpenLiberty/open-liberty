/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.el60.fat.beanelresolver.beans;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * A SimpleBeanInfo to define the read and write methods for TestBean.
 */
public class TestBeanBeanInfo extends SimpleBeanInfo {

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        PropertyDescriptor testPropertyDescriptor = null;
        try {
            // PropertyDescriptor(String propertyName, Class<?> beanClass, String readMethodName, String writeMethodName)
            testPropertyDescriptor = new PropertyDescriptor("test", TestBean.class, "returnTest", "writeTest");
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }

        PropertyDescriptor[] descriptors = { testPropertyDescriptor };

        return descriptors;
    }
}
