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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.validation.Configuration;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.ValidationException;
import javax.validation.ValidationProviderResolver;
import javax.validation.ValidatorFactory;
import javax.validation.executable.ExecutableValidator;

import org.apache.cxf.common.logging.LogUtils;

import com.ibm.ws.jaxrs20.beanvalidation.component.BeanValidationService;

public class BeanValidationProvider {
    private static final Logger LOG = LogUtils.getL7dLogger(BeanValidationProvider.class);

    private final ValidatorFactory factory;

    public BeanValidationProvider() {
        try {
            factory = BeanValidationService.instance().getDefaultValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
    }

    public BeanValidationProvider(ValidationConfiguration cfg) {
        try {
            Configuration<?> factoryCfg = Validation.byDefaultProvider().configure();
            initFactoryConfig(factoryCfg, cfg);
            factory = factoryCfg.buildValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
    }

    public BeanValidationProvider(ValidatorFactory factory) {
        if (factory == null) {
            throw new NullPointerException("Factory is null");
        }
        this.factory = factory;
    }

    public BeanValidationProvider(ValidationProviderResolver resolver) {
        this(resolver, (Class) null);
    }

    public <T extends Configuration<T>> BeanValidationProvider(
                                                               ValidationProviderResolver resolver,
                                                               Class<javax.validation.spi.ValidationProvider<T>> providerType) {
        this(resolver, providerType, null);
    }

    public <T extends Configuration<T>> BeanValidationProvider(
                                                               ValidationProviderResolver resolver,
                                                               Class<javax.validation.spi.ValidationProvider<T>> providerType,
                                                               ValidationConfiguration cfg) {
        try {
            Configuration<?> factoryCfg = providerType != null
                            ? Validation.byProvider(providerType).providerResolver(resolver).configure()
                            : Validation.byDefaultProvider().providerResolver(resolver).configure();
            initFactoryConfig(factoryCfg, cfg);
            factory = factoryCfg.buildValidatorFactory();
        } catch (final ValidationException ex) {
            LOG.severe("Bean Validation provider can not be found, no validation will be performed");
            throw ex;
        }
    }

    private static void initFactoryConfig(Configuration<?> factoryCfg, ValidationConfiguration cfg) {
        if (cfg != null) {
            factoryCfg.parameterNameProvider(cfg.getParameterNameProvider());
            factoryCfg.messageInterpolator(cfg.getMessageInterpolator());
            factoryCfg.traversableResolver(cfg.getTraversableResolver());
            factoryCfg.constraintValidatorFactory(cfg.getConstraintValidatorFactory());
            for (Map.Entry<String, String> entry : cfg.getProperties().entrySet()) {
                factoryCfg.addProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    public <T> void validateParameters(final T instance, final Method method, final Object[] arguments) {

        if (BeanValidationService.instance() != null && !BeanValidationService.instance().isMethodConstrained(method)) {
            return;
        }
        final ExecutableValidator methodValidator = getExecutableValidator();
        final Set<ConstraintViolation<T>> violations = methodValidator.validateParameters(instance,
                                                                                          method, arguments);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    public <T> void validateReturnValue(final T instance, final Method method, final Object returnValue) {

        if (BeanValidationService.instance() != null && !BeanValidationService.instance().isMethodConstrained(method)) {
            return;
        }
        final ExecutableValidator methodValidator = getExecutableValidator();
        final Set<ConstraintViolation<T>> violations = methodValidator.validateReturnValue(instance,
                                                                                           method, returnValue);

        if (!violations.isEmpty()) {
            throw new ResponseConstraintViolationException(violations);
        }
    }

    public <T> void validateReturnValue(final T bean) {
        final Set<ConstraintViolation<T>> violations = doValidateBean(bean);
        if (!violations.isEmpty()) {
            throw new ResponseConstraintViolationException(violations);
        }
    }

    public <T> void validateBean(final T bean) {
        final Set<ConstraintViolation<T>> violations = doValidateBean(bean);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private <T> Set<ConstraintViolation<T>> doValidateBean(final T bean) {
        return factory.getValidator().validate(bean);
    }

    private ExecutableValidator getExecutableValidator() {

        return factory.getValidator().forExecutables();
    }
}
