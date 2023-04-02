/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.restfulWS30.cdi30.fat.complex;

import javax.annotation.PreDestroy;

import jakarta.enterprise.context.Dependent;

@Dependent
public class SimpleBean {

    String _response;

    public String getResponse() {
        return _response;
    }

    public String getMessage() {
        _response = "Hello from SimpleBean";
        return _response;
    }

    @PreDestroy
    public void destruct() {
        System.out.println(this + " Pre Destroy called on " + this);
    }
}