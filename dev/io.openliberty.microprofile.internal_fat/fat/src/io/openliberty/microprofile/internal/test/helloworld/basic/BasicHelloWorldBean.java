/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.internal.test.helloworld.basic;

import javax.enterprise.context.ApplicationScoped;

import io.openliberty.microprofile.internal.test.helloworld.HelloWorldBean;

@ApplicationScoped
public class BasicHelloWorldBean implements HelloWorldBean {

    public static final String MESSAGE = "Hello World!";

    @Override
    public String getMessage() {
        return MESSAGE;
    }
}
