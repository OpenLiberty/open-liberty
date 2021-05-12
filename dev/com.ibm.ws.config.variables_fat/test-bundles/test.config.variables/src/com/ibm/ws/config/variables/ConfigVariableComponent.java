/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.variables;

import java.util.Collection;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.config.xml.LibertyVariable;

/**
 *
 */
@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public class ConfigVariableComponent implements ServerXMLVariables {

    private ConfigVariables vars;

    @Activate
    protected void activate(ComponentContext ctx) {
        System.out.println("Activating test component ConfigVariableComponent");
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {

    }

    @Reference
    protected void setVariables(ConfigVariables cv) {
        this.vars = cv;
    }

    @Override
    public Map<String, String> getServerXMLVariables() {
        return vars.getUserDefinedVariables();
    }

    @Override
    public Map<String, String> getServerXMLVariableDefaultValues() {
        return vars.getUserDefinedVariableDefaults();
    }

    @Override
    public Collection<LibertyVariable> getLibertyVariables() {
        return vars.getAllLibertyVariables();
    }

}
