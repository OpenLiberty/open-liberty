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

import javax.enterprise.context.ApplicationScoped;
import javax.faces.component.behavior.ClientBehaviorBase;
import javax.faces.component.behavior.FacesBehavior;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Simple ApplicationScoped bean to test injection of FacesConverter, FacesValidator and FacesBehavior objects
 */
@Named("jsfArtifactsInjectionBean")
@ApplicationScoped
public class JSFArtifactsInjectionBean {

    @Inject
    @FacesConverter(value = "testConverter", managed = true)
    private Converter<?> converter;

    @Inject
    @FacesValidator(value = "testValidator", managed = true)
    private Validator<?> validator;

    @Inject
    @FacesBehavior(value = "testBehavior", managed = true)
    private ClientBehaviorBase behavior;

    public Converter<?> getConverter() {
        return converter;
    }

    public Validator<?> getValidator() {
        return validator;
    }

    public ClientBehaviorBase getBehavior() {
        return behavior;
    }
}
