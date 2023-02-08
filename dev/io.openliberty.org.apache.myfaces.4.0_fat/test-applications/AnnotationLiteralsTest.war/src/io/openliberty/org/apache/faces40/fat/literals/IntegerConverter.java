/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.faces40.fat.literals;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.ConverterException;
import jakarta.faces.convert.FacesConverter;

/**
 * This class forces CDI to create a @FacesConverter bean for testing
 */
@FacesConverter(value = "integerConverter", forClass = Integer.class, managed = true)
public class IntegerConverter implements Converter<Integer> {

    @Override
    public String getAsString(FacesContext context, UIComponent component, Integer value) throws ConverterException {
        return value.toString();
    }

    @Override
    public Integer getAsObject(FacesContext context, UIComponent component, String entity) throws ConverterException {
        return Integer.valueOf(entity);
    }

}
