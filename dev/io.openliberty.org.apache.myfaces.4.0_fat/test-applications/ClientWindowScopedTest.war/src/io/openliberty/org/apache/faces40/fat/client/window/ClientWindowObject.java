/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.client.window;

import java.io.Serializable;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.faces.lifecycle.ClientWindowScoped;
import jakarta.inject.Named;

@Named(value = "clientWindowObject")
@ClientWindowScoped
public class ClientWindowObject implements Serializable {

    private static final long serialVersionUID = 1L;

    @PostConstruct
    public void init() {
        System.out.println("FACE40CWO:INIT:" + this.hashCode());
    }

    @PreDestroy
    public void remove() {
        System.out.println("FACE40CWO:REMOVE:" + this.hashCode());
    }

    public String getId() {
        return "" + this.hashCode();
    }
}
