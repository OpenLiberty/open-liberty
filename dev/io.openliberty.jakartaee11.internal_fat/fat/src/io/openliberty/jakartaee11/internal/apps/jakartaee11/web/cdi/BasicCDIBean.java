/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee11.internal.apps.jakartaee11.web.cdi;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class BasicCDIBean {

    public static final String MSG = "Hello world";

    public String sayHi() {
        return MSG;
    }

}
