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
package io.openliberty.microprofile.config.internal_fat.apps.converter;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class DuplicateConvertersBean {

    @Inject
    @ConfigProperty(name = "DUPLICATE_CONVERTERS_KEY_1")
    MyTypeWithMultipleConverters DUPLICATE_CONVERTERS_KEY_1;

    @Inject
    @ConfigProperty(name = "DUPLICATE_CONVERTERS_KEY_2")
    MyTypeWithMultipleConverters DUPLICATE_CONVERTERS_KEY_2;

    @Inject
    @ConfigProperty(name = "DUPLICATE_CONVERTERS_KEY_3")
    MyTypeWithMultipleConverters DUPLICATE_CONVERTERS_KEY_3;

    public MyTypeWithMultipleConverters getDUPLICATE_CONVERTERS_KEY_1() {
        return DUPLICATE_CONVERTERS_KEY_1;
    }

    public MyTypeWithMultipleConverters getDUPLICATE_CONVERTERS_KEY_2() {
        return DUPLICATE_CONVERTERS_KEY_2;
    }

    public MyTypeWithMultipleConverters getDUPLICATE_CONVERTERS_KEY_3() {
        return DUPLICATE_CONVERTERS_KEY_3;
    }

}
