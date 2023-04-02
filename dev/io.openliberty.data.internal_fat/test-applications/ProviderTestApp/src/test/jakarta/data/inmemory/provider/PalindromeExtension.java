/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.inmemory.provider;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;

import jakarta.data.exceptions.MappingException;
import jakarta.data.repository.DataRepository;
import jakarta.data.repository.Repository;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanAttributes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A fake Jakarta Data provider extension that only produces a single repository class,
 * which is because it doesn't have a real implementation and is only for tests
 * that register a Jakarta Data provider as a CDI extension.
 */
public class PalindromeExtension implements Extension {

    private final ArrayList<Bean<?>> repositoryBeans = new ArrayList<>();

    private final HashSet<AnnotatedType<?>> repositoryTypes = new HashSet<>();

    @Trivial
    public <T> void annotatedRepository(@Observes @WithAnnotations(Repository.class) ProcessAnnotatedType<T> event) {
        AnnotatedType<T> type = event.getAnnotatedType();

        Repository repository = type.getAnnotation(Repository.class);
        String provider = repository.provider();
        if (Repository.ANY_PROVIDER.equals(provider) || "Palindrome".equalsIgnoreCase(provider)) {
            System.out.println("Palindrome CDI Extension: adding repository " + repository.toString() + ' ' + type.getJavaClass().getName());
            repositoryTypes.add(type);
        } else {
            System.out.println("Palindrome CDI Extension: ignore repository " + repository.toString() + ' ' + type.getJavaClass().getName());
        }
    }

    public void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanMgr) {
        for (AnnotatedType<?> repositoryType : repositoryTypes) {
            Class<?> repositoryInterface = repositoryType.getJavaClass();

            Class<?> entityClass = null;
            for (Type interfaceType : repositoryInterface.getGenericInterfaces()) {
                if (interfaceType instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) interfaceType;
                    if (parameterizedType.getRawType().getTypeName().startsWith(DataRepository.class.getPackageName())) {
                        Type typeParams[] = parameterizedType.getActualTypeArguments();
                        if (typeParams.length == 2 && typeParams[0] instanceof Class) {
                            entityClass = (Class<?>) typeParams[0];
                            break;
                        }
                    }
                }
            }

            if (entityClass == null)
                throw new MappingException("Did not find the entity class for " + repositoryInterface);

            PalindromicEntity entityAnno = entityClass.getAnnotation(PalindromicEntity.class);

            if (entityAnno == null) {
                Repository repository = repositoryType.getAnnotation(Repository.class);
                if (!Repository.ANY_PROVIDER.equals(repository.provider()))
                    throw new MappingException("The Palindrom mock Jakarta Data provider cannot provide the " +
                                               repositoryType.getJavaClass().getName() + " repository because the repository's " +
                                               entityClass.getName() + " entity class is not annotated with " + PalindromicEntity.class.getName());
            } else {
                BeanAttributes<?> attrs = beanMgr.createBeanAttributes(repositoryType);
                Bean<?> bean = beanMgr.createBean(attrs, repositoryInterface, new PalindromeRepositoryProducer.Factory<>());
                repositoryBeans.add(bean);
            }
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event, BeanManager beanMgr) {
        for (Bean<?> bean : repositoryBeans) {
            event.addBean(bean);
        }
    }
}
