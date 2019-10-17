/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile10.fat.tests.helloworld;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HelloWorldBean {

    public static final String MESSAGE = "Hello World!";

    public String getMessage() {
        return MESSAGE;
    }
}
