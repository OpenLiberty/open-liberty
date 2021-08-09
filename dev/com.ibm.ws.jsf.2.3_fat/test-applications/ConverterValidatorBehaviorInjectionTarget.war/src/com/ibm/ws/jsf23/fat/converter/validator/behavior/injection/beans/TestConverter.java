/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf23.fat.converter.validator.behavior.injection.beans;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 * Converter that supports Injection.
 *
 * Added use of generics to this Converter to test MYFACES-4311.
 */
@FacesConverter(value = "testConverter", managed = true)
public class TestConverter implements Converter<String> {

    @Inject
    private TestCDIBean testBean;

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.convert.Converter#getAsObject(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
     */
    @Override
    public String getAsObject(FacesContext context, UIComponent component, String value) throws ConverterException {
        int index = value.indexOf(testBean.getWorld());
        if (index != -1) {
            value = value.substring(0, index) + testBean.getEarth();
        }
        return value;
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.faces.convert.Converter#getAsString(javax.faces.context.FacesContext, javax.faces.component.UIComponent, java.lang.String)
     */
    @Override
    public String getAsString(FacesContext context, UIComponent component, String value) throws ConverterException {
        if (value == null) {
            return "";
        }
        return value;
    }

}
