/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.anno.test.data;

import javax.annotation.Resource;
import javax.annotation.Resource.AuthenticationType;

import com.ibm.ws.anno.test.data.sub.SubBase;

/**
 *
 */
@Resource(name = "/B", authenticationType = AuthenticationType.APPLICATION)
public class BClass extends SubBase implements CIntf {
    static {
        int i = 0;
    }

    public BClass() {

    }

    @Override
    public void publicMethod() {}

    @Override
    public Integer publicMethod(int n) {
        return null;
    }

    private void privateMethod() {}

    @Override
    @Resource
    public Integer n() {
        // TODO Auto-generated method stub
        return null;
    }
}
