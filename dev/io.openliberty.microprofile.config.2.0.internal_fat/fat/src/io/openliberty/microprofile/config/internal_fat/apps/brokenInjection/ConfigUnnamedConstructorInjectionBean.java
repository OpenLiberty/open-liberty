/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.brokenInjection;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class ConfigUnnamedConstructorInjectionBean {

    private String SIMPLE_KEY4 = null;

    //this will fail
    @Inject
    public ConfigUnnamedConstructorInjectionBean(@ConfigProperty String SIMPLE_KEY4) {
        this.SIMPLE_KEY4 = SIMPLE_KEY4;
    }

    public String getSIMPLE_KEY4() {
        return SIMPLE_KEY4;
    }

}
