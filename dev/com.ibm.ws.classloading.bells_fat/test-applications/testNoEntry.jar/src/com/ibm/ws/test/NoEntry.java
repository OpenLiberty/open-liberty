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

package com.ibm.ws.test;

import com.ibm.ws.classloading.exporting.test.TestInterface;

public class NoEntry implements TestInterface {

    @Override
    public String isThere(String name) {
        return name + " is there";
    }

    @Override
    public String hasProperties(String name) {
        return name + " has properties " + null;
    }

}
