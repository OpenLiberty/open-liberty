/*******************************************************************************
 * Copyright (c) 2006, 2021 IBM Corporation and others.
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

package com.ibm.ws.ejbcontainer.injection.mix.ejb;

import javax.annotation.Resource;

public class SuperOfSuperSuperEnvInject {
    @Resource(name = "superSuperMyNumber")
    protected int myNumber = 1;

    public int getSuperSuperMyNumber() {
        return myNumber;
    }

    private String superSuperPrivateString = "No, I won't";

    @SuppressWarnings("unused")
    @Resource
    private void setSuperSuperPrivateString(String superSuperPrivateString) {
        this.superSuperPrivateString = superSuperPrivateString;
    }

    public String getSuperSuperPrivateString() {
        return this.superSuperPrivateString;
    }

}
