/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.openapi40.internal.impl;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.microprofile.openapi20.internal.merge.MergeProcessorImpl;
import io.openliberty.microprofile.openapi20.internal.merge.NameProcessor.DocumentNameProcessor;
import io.openliberty.microprofile.openapi20.internal.merge.NameType;
import io.openliberty.microprofile.openapi20.internal.services.MergeProcessor;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIProvider;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class OpenAPI31MergeProcessor extends MergeProcessorImpl implements MergeProcessor {

    @Override
    protected MergeProcessorInstance createMergeProcessorInstance(List<OpenAPIProvider> documents) {
        return new MergeProcessorInstance31(documents);
    }

    protected class MergeProcessorInstance31 extends MergeProcessorInstance {

        public MergeProcessorInstance31(List<OpenAPIProvider> providers) {
            super(providers);
        }

        @Override
        protected void renameClashingComponents(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
            super.renameClashingComponents(document, documentNameProcessor);

            Components components = document.getComponents();
            if (components != null) {
                components.setPathItems(renameComponents(NameType.PATH_ITEMS, components.getPathItems(), documentNameProcessor));
            }
        }

        @Override
        protected void renameClashingWebhooks(OpenAPI document, DocumentNameProcessor documentNameProcessor) {
            Map<String, PathItem> webhooks = document.getWebhooks();
            if (webhooks != null) {
                document.setWebhooks(renameComponents(NameType.WEBHOOKS, webhooks, documentNameProcessor));
            }
        }
    }

}
