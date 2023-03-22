/*******************************************************************************
 * Copyright (c) 2022,2023 IBM Corporation and others.
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

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import jakarta.data.exceptions.MappingException;
import jakarta.data.provider.DataProvider;
import jakarta.data.repository.DataRepository;

/**
 * A fake Jakarta Data provider that only produces a single repository class,
 * which is because it doesn't have a real implementation and is only for tests
 * that obtain a DataProvider from the ServiceLoader.
 */
public class PalindromeProvider implements DataProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <R> R getRepository(Class<R> repositoryInterface) throws MappingException {
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
        if (entityAnno == null)
            throw new MappingException("Did not find entity annotation on " + entityClass);

        return (R) new PalindromeRepository();
    }

    @Override
    public void repositoryBeanDisposed(Object repository) {
    }

    @Override
    public String name() {
        return "Palindrome Data Provider";
    }

    @Override
    public Set<Class<? extends Annotation>> supportedEntityAnnotations() {
        return Set.of(PalindromicEntity.class);
    }
}
