/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/

package com.ibm.ws.cdi.impl.weld.validation;

import java.util.Collection;

import org.jboss.weld.bootstrap.BeanDeployment;
import org.jboss.weld.bootstrap.Validator;
import org.jboss.weld.exceptions.DefinitionException;
import org.jboss.weld.logging.MessageCallback;
import org.jboss.weld.manager.BeanManagerImpl;

import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.Interceptor;
import jakarta.enterprise.inject.spi.Producer;

public abstract class LibertyDelegatingValidator extends Validator {

    protected final Validator delegate;

    public LibertyDelegatingValidator(Validator delegate) {
        super(null, null);
        this.delegate = delegate;
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void validateInjectionPoint(InjectionPoint ij, BeanManagerImpl beanManager) {
        delegate.validateInjectionPoint(ij, beanManager);
    }

    @Override
    public void validateInjectionPointForDefinitionErrors(InjectionPoint ij, Bean<?> bean, BeanManagerImpl beanManager) {
        delegate.validateInjectionPointForDefinitionErrors(ij, bean, beanManager);
    }

    @Override
    public void validateMetadataInjectionPoint(InjectionPoint ij, Bean<?> bean, MessageCallback<DefinitionException> messageCallback) {
        delegate.validateMetadataInjectionPoint(ij, bean, messageCallback);
    }

    @Override
    public void validateEventMetadataInjectionPoint(InjectionPoint ip) {
        delegate.validateEventMetadataInjectionPoint(ip);
    }

    @Override
    public void validateInjectionPointForDeploymentProblems(InjectionPoint ij, Bean<?> bean, BeanManagerImpl beanManager) {
        delegate.validateInjectionPointForDeploymentProblems(ij, bean, beanManager);
    }

    @Override
    public void validateProducers(Collection<Producer<?>> producers, BeanManagerImpl beanManager) {
        delegate.validateProducers(producers, beanManager);
    }

    @Override
    public void validateProducer(Producer<?> producer, BeanManagerImpl beanManager) {
        delegate.validateProducer(producer, beanManager);
    }

    @Override
    public void validateInjectionPointPassivationCapable(InjectionPoint ij, Bean<?> resolvedBean, BeanManagerImpl beanManager) {
        delegate.validateInjectionPointPassivationCapable(ij, resolvedBean, beanManager);
    }

    @Override
    public void validateInterceptorDecoratorInjectionPointPassivationCapable(InjectionPoint ij, Bean<?> resolvedBean, BeanManagerImpl beanManager, Bean<?> bean) {
        delegate.validateInterceptorDecoratorInjectionPointPassivationCapable(ij, resolvedBean, beanManager, bean);
    }

    @Override
    public void validateDeployment(BeanManagerImpl manager, BeanDeployment deployment) {
        delegate.validateDeployment(manager, deployment);
    }

    @Override
    public void validateSpecialization(BeanManagerImpl manager) {
        delegate.validateSpecialization(manager);
    }

    @Override
    public void validateBeans(Collection<? extends Bean<?>> beans, BeanManagerImpl manager) {
        delegate.validateBeans(beans, manager);
    }

    @Override
    public void validateInterceptors(Collection<? extends Interceptor<?>> interceptors, BeanManagerImpl manager) {
        delegate.validateInterceptors(interceptors, manager);
    }

    @Override
    public void validateDecorators(Collection<? extends Decorator<?>> decorators, BeanManagerImpl manager) {
        delegate.validateDecorators(decorators, manager);
    }

    @Override
    public void validateBeanNames(BeanManagerImpl beanManager) {
        delegate.validateBeanNames(beanManager);
    }

    @Override
    public void cleanup() {
        delegate.cleanup();
    }

    @Override
    public boolean isResolved(Bean<?> bean) {
        return delegate.isResolved(bean);
    }

    @Override
    public void clearResolved() {
        delegate.clearResolved();
    }

}
