/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.urn.component;

import java.io.IOException;

import jakarta.faces.component.FacesComponent;
import jakarta.faces.component.UIInput;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ResponseWriter;

/**
 * A simple custom component that converts the value passed in to lowercase.
 */
@FacesComponent(createTag = true) // Use default namespace jakarta.faces.component
public class TestComponent extends UIInput {

    @Override
    public void encodeBegin(FacesContext context) throws IOException {

        String value = (String) getAttributes().get("value");

        if (value != null) {
            ResponseWriter writer = context.getResponseWriter();
            writer.write(value.toLowerCase());
        }
    }

}
