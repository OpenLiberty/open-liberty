/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jakartaee9.internal.apps.jakartaee9.web.cdi;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class BasicCDIBean {

    public static final String MSG = "Hello world";

    public String sayHi() {
        return MSG;
    }

}