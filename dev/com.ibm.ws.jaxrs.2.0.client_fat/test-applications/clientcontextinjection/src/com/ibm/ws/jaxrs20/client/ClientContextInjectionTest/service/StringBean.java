/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.client.ClientContextInjectionTest.service;

public class StringBean
{
    private String header;

    public String get()
    {
        return this.header;
    }

    public void set(String header) {
        this.header = header;
    }

    @Override
    public String toString()
    {
        return "StringBean. To get a value, use rather #get() method.";
    }

    public StringBean(String header)
    {
        this.header = header;
    }
}