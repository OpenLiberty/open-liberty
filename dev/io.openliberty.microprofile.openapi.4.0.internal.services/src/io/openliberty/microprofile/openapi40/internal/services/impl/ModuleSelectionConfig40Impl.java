/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.services.impl;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.microprofile.openapi20.internal.ModuleSelectionConfigImpl;
import io.openliberty.microprofile.openapi20.internal.services.ModuleSelectionConfig;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, service = ModuleSelectionConfig.class)
public class ModuleSelectionConfig40Impl extends ModuleSelectionConfigImpl {

    @Override
    protected InclusionMode getDefaultInclusion() {
        return InclusionMode.ALL;
    }

}
