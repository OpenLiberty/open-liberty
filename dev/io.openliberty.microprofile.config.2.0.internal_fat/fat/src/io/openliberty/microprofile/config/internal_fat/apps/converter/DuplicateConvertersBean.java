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
package io.openliberty.microprofile.config.internal_fat.apps.converter;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.openliberty.microprofile.config.internal_fat.Config20Tests;

@RequestScoped
public class DuplicateConvertersBean {

    @Inject
    @ConfigProperty(name = Config20Tests.DUPLICATE_CONVERTERS_KEY_1)
    MyTypeWithMultipleConverters value1;

    @Inject
    @ConfigProperty(name = Config20Tests.DUPLICATE_CONVERTERS_KEY_2)
    MyTypeWithMultipleConverters value2;

    @Inject
    @ConfigProperty(name = Config20Tests.DUPLICATE_CONVERTERS_KEY_3)
    MyTypeWithMultipleConverters value3;

    public MyTypeWithMultipleConverters getValue1() {
        return value1;
    }

    public MyTypeWithMultipleConverters getValue2() {
        return value2;
    }

    public MyTypeWithMultipleConverters getValue3() {
        return value3;
    }

}
