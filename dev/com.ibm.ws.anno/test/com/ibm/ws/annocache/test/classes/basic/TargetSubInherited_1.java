/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.test.classes.basic;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;

@Resource(name = "/B", authenticationType = AuthenticationType.APPLICATION)
public class TargetSubInherited_1 extends TargetInherited implements TestInterface {
    static {
        @SuppressWarnings("unused")
        int i = 0;
    }

    public TargetSubInherited_1() {
    	// EMPTY
    }

    //

    @Override
    public void publicMethod() {
    	// EMPTY
    }

    @Override
    public Integer publicMethod(int n) {
        return null;
    }

    @Override
    @Resource
    public Integer n() {
        return null;
    }
    
    @SuppressWarnings("unused")
    private void privateMethod() {
    	// EMPTY
    }
}
