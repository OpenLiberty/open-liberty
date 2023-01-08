/*******************************************************************************
 * Copyright (c) 2012,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jca.adapter;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import jakarta.resource.ResourceException;
import jakarta.resource.cci.IndexedRecord;

@SuppressWarnings("rawtypes")
public class BVTRecord extends ArrayList implements IndexedRecord, InvocationHandler {
    private static final long serialVersionUID = 7207884346248363094L;
    private String description = "the results";
    private String name = "results";
    private ResultSet resultSet;

    // Empty IndexedRecord
    BVTRecord() {}

    // InvocationHandler for jakarta.resource.cci.ResultSet
    BVTRecord(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    /** {@inheritDoc} */
    @Override
    public String getRecordName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public String getRecordShortDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object target = ResultSet.class.equals(method.getDeclaringClass()) ? resultSet : this;
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException x) {
            Throwable cause = x.getCause();
            if (cause instanceof SQLException)
                cause = new ResourceException(cause);
            throw cause;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setRecordName(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public void setRecordShortDescription(String description) {
        this.description = description;
    }
}
