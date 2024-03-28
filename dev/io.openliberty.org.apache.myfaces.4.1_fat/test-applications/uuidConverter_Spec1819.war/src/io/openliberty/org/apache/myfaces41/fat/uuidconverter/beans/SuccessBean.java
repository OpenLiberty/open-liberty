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

@SessionScoped
@Named("successBean")
public class SuccessBean implements Serializable {

    UUID id = UUID.randomUUID();

    public UUID getId() {
        System.out.println("GETID: " + id);
        return id;
    }

    public void setId(UUID id) {
        System.out.println("SETID: " + id.getClass() + " " + id);
        this.id = id;
    }

    
}
