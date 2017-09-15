/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.validation;

import javax.validation.Configuration;
import javax.validation.ParameterNameProvider;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;

public class BeanValidationProviderEx extends BeanValidationProvider {

    public BeanValidationProviderEx() {
        super();
    }

    public BeanValidationProviderEx(ParameterNameProvider parameterNameProvider) {
        super(new ValidationConfiguration(parameterNameProvider));
    }

    public BeanValidationProviderEx(ValidationConfiguration cfg) {
        super(cfg);
    }

    public BeanValidationProviderEx(ValidatorFactory factory) {
        super(factory);
    }

    public BeanValidationProviderEx(ValidationProviderResolver resolver) {
        super(resolver);
    }

    public <T extends Configuration<T>> BeanValidationProviderEx(
                                                                 ValidationProviderResolver resolver,
                                                                 Class<javax.validation.spi.ValidationProvider<T>> providerType) {
        super(resolver, providerType);
    }

    public <T extends Configuration<T>> BeanValidationProviderEx(
                                                                 ValidationProviderResolver resolver,
                                                                 Class<javax.validation.spi.ValidationProvider<T>> providerType,
                                                                 ValidationConfiguration cfg) {
        super(resolver, providerType, cfg);
    }

}
