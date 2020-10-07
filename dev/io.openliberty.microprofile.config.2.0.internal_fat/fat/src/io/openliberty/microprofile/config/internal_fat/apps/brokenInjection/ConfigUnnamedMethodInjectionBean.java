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

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class ConfigUnnamedMethodInjectionBean {

    private Config config = null;
    private String SIMPLE_KEY6;

    @Inject
    public ConfigUnnamedMethodInjectionBean(Config config) {
        this.config = config;
    }

    public ConfigUnnamedMethodInjectionBean() {
    }

    //this will fail

    @Inject
    public void setSimpleKey6(@ConfigProperty String SIMPLE_KEY6) {
        this.SIMPLE_KEY6 = SIMPLE_KEY6;
    }

    public String getSIMPLE_KEY5() {
        return config.getValue("SIMPLE_KEY5", String.class);
    }

    public String getSIMPLE_KEY6() {
        return SIMPLE_KEY6;
    }

}
