/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

package com.ibm.ws.test;

import com.ibm.ws.classloading.exporting.test.TestInterface;

public class ImplClassNotConstructible implements TestInterface {

    public ImplClassNotConstructible(String name) {
        // Ctor parm type must be none or Map<String,String>
    }

    @Override
    public String isThere(String name) {
        return name + " is there";
    }

}
