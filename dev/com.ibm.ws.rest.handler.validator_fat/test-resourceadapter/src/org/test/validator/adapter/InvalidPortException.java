/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.test.validator.adapter;

import java.beans.PropertyDescriptor;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.resource.spi.InvalidPropertyException;

/**
 * Custom exception class provided by the resource adapter to ensure that validator output can be
 * created from exception types that are not defined by the spec.
 */
public class InvalidPortException extends InvalidPropertyException {
    private static final long serialVersionUID = 1L;

    InvalidPortException(String message, String errorCode) {
        super(message, errorCode);
        try {
            PropertyDescriptor portNumberProp = AccessController.doPrivileged((PrivilegedExceptionAction<PropertyDescriptor>) () -> {
                return new PropertyDescriptor("PortNumber", ManagedConnectionFactoryImpl.class);
            });
            setInvalidPropertyDescriptors(new PropertyDescriptor[] { portNumberProp });
        } catch (PrivilegedActionException x) {
            x.printStackTrace();
        }
    }
}
