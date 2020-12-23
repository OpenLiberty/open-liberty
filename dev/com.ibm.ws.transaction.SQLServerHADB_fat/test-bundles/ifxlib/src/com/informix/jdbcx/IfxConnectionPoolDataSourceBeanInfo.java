/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.informix.jdbcx;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

/**
 * This class defines the properties that will be set for this DataSource, the WAS
 * JDBCDriverService introspects this class and then sets these properties through the
 * setters in the ifxConnectionPoolDataSource class. The JDBCDriverService class pulls the
 * vendor properties from the server.xml. In the SQL Server and test containers case, those
 * properties are ultimately retrieved from the environment.
 *
 */
public class IfxConnectionPoolDataSourceBeanInfo extends SimpleBeanInfo {
    private final static Class myClass = IfxConnectionPoolDataSource.class;

    //
    // Note that here we create an array of 5 PropertyDescriptor
    // objects. The constructor takes just the name of the property,
    // and the bean class that implements it.
    //
    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        try {
            PropertyDescriptor password = new PropertyDescriptor("password", myClass);
            PropertyDescriptor selectMethod = new PropertyDescriptor("selectMethod", myClass);
            PropertyDescriptor user = new PropertyDescriptor("user", myClass);
            PropertyDescriptor serverName = new PropertyDescriptor("serverName", myClass);
            PropertyDescriptor URL = new PropertyDescriptor("URL", myClass);
            PropertyDescriptor[] list = { password, selectMethod, user, serverName, URL };
            return list;
        } catch (IntrospectionException iexErr) {
            throw new Error(iexErr.toString());
        }
    }
}
