/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.cdi.annotations.fat.apps.abstractDecorator;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AbstractDecoratorTestBean implements AbstractDecoratorTestInterface {

    @Override
    public String test1() {
        return "test1";
    }

    @Override
    public String test2() {
        return "test2";
    }

    @Override
    public String test3() {
        return "test3";
    }

}
