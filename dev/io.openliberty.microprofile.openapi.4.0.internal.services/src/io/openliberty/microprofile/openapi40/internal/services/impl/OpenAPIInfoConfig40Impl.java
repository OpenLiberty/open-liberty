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

import java.util.Dictionary;
import java.util.Optional;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.openapi20.internal.OpenAPIInfoConfigImpl;
import io.openliberty.microprofile.openapi20.internal.services.OpenAPIInfoConfig;

@Component(configurationPid = "io.openliberty.microprofile.openapi", service = OpenAPIInfoConfig.class)
public class OpenAPIInfoConfig40Impl extends OpenAPIInfoConfigImpl {

    @Override
    protected Optional<Info> parseInfoProperties(Dictionary<String, Object> infoProperties) {
        Optional<Info> result = super.parseInfoProperties(infoProperties);

        result.ifPresent(info -> {
            // Add fields which are new in OpenAPI 3.1
            info.setSummary((String) infoProperties.get(SUMMARY_KEY));

            String licenseIdentifier = (String) infoProperties.get(LICENSE_IDENTIFIER_KEY);
            if (licenseIdentifier != null) {
                License license = info.getLicense();
                if (license == null) {
                    license = OASFactory.createLicense();
                    info.setLicense(license);
                }
                license.setIdentifier(licenseIdentifier);
            }
        });

        return result;
    }

}
