/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.myfaces41.fat.uuidconverter.beans;

import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.UUID;

import jakarta.enterprise.context.SessionScoped;

/* 
 * Using this bean with the UUIDConverer results in a ClassCastException
*/
@SessionScoped
@Named("failBean")
public class FailBean implements Serializable {

    String id = "default";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    
}
