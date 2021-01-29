/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.TypeWithBadConverter;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.TypeWithNoConverter;
import io.openliberty.microprofile.config.internal_fat.apps.brokenInjection.converters.TypeWithValidConverter;

@RequestScoped
public class BadConfigPropertyBean {

    @Inject
    @ConfigProperty(name = "nonExistantKey") // This Config Property is not defined, and hence will throw an Exception
    String nonExistantKey1; // This Type has a built in Converter

    @Inject
    @ConfigProperty(name = "nonExistingKeyWithCustomConverter") // This Config Property is not defined, and hence will throw an Exception
    TypeWithValidConverter nonExistantKey2; // This Type has a custom Converter registered

    @Inject
    @ConfigProperty(name = "noConverterKey") // This Config Property is defined
    TypeWithNoConverter noConverterProp; // This Type does not have a Converter registered, and hence will throw an Exception

    @Inject
    @ConfigProperty(name = "badConverterKey") // This Config Property is defined
    TypeWithBadConverter badConverterProp; // This Type has a bad custom Converter registered, which will throw an Exception

}
