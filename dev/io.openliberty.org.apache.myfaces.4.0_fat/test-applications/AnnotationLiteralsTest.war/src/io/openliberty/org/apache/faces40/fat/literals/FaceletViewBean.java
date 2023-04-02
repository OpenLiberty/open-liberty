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
package io.openliberty.org.apache.faces40.fat.literals;

import java.io.IOException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.annotation.View;
import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.facelets.Facelet;

/**
 * This class forces CDI to create a @View bean literal for testing
 */
@View("/fake.xhtml")
@ApplicationScoped
public class FaceletViewBean extends Facelet {

    @Override
    public void apply(FacesContext facesContext, UIComponent root) throws IOException {
        return; //Do nothing
    }

}
