/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.injectionengine;

/**
 * This exception is thrown when the injection engine detects that the user
 * configuration of an injection is incorrect or invalid.
 */
public class InjectionConfigurationException extends InjectionException {

    private static final long serialVersionUID = -6795675132453992281L;

    public InjectionConfigurationException()
    {
        // intentionally left blank.
    }

    public InjectionConfigurationException(String message)
    {
        super(message);
    }

    public InjectionConfigurationException(Throwable throwable)
    {
        super(throwable);
    }

    public InjectionConfigurationException(String detailMessage, Throwable throwable)
    {
        super(detailMessage, throwable);
    }
}
