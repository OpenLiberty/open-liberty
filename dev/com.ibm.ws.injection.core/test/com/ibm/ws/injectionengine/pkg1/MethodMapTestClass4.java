/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.injectionengine.pkg1;

import com.ibm.ws.injectionengine.pkg2.MethodMapTestClass3;

public class MethodMapTestClass4 extends MethodMapTestClass3 {
    @SuppressWarnings("all")
    @Override
    void pkg1OverridePkg1() { /* empty */}

    @SuppressWarnings("all")
    void pkg1NotOverridePkg2OverridePkg2() { /* empty */}

    @Override
    public void pkg1OverridePkg1AndPublicPkg2() { /* empty */}

    public void pkg1OverridePublicPkg2OverridePkg2() { /* empty */}
}
