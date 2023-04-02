/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.internal.test.helloworld.config;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.openliberty.microprofile.internal.test.helloworld.HelloWorldBean;

@ApplicationScoped
public class ConfiguredHelloWorldBean implements HelloWorldBean {

    @Inject
    @ConfigProperty(name = "message")
    private String message;

    @Override
    public String getMessage() {
        return message;
    }
}
