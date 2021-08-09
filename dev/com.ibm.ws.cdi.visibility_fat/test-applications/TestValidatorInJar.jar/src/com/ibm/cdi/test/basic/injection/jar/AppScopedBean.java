/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.cdi.test.basic.injection.jar;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AppScopedBean {

    public final static String MSG = "App Scoped Hello World";

    public String getMsg() {
        return MSG;
    }
}
