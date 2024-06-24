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

import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.microprofile.openapi20.internal.services.MergeProcessor;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;

@Component(service = MergeProcessor.class, configurationPolicy = ConfigurationPolicy.IGNORE)
public class OpenAPI31MergeProcessor implements MergeProcessor {

    @Override
    public OpenAPIProvider mergeDocuments(List<OpenAPIProvider> documents) {
        // TODO: implement merging
        return documents.get(0);
    }

}
