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
package com.ibm.ws.jaxrs.fat.paramconverter;

import java.util.ArrayList;

// this tests a custom Collection with a non public constructor
public class TestStringList extends ArrayList<String> {

    private static final long serialVersionUID = 2345673566345656425L;

    public static TestStringList newInstance() {
        return new TestStringList();
    }

    private TestStringList() {
        super();
    }

}
