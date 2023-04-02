/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.dependentintojax;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;

@Dependent
public class DependentBean {

    public String getMsg() {
        return "";
    }

    @PreDestroy
    public void destroy() {
        new Throwable("Diag - PreDestroy").printStackTrace();
        MyResource.registerPreDestroy("");
    }
}