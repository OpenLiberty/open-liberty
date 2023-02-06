/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.org.apache.faces40.fat.programmaticfacelettests;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.faces.annotation.View;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
public class TestFieldAndMethodViewDeclarations {

    @Inject
    MethodFacelet testMethod;

    @Produces
    @ApplicationScoped
    @View("/FieldView.xhtml")
    FieldFacelet testField;

    @Produces
    @ApplicationScoped
    @Named
    @View("/MethodView.xhtml")
    public MethodFacelet produceBean() {
        return new MethodFacelet();
    }

    public TestFieldAndMethodViewDeclarations() {
        testField = new FieldFacelet();
    }
}
