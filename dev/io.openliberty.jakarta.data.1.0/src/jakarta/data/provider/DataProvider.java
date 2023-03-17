/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package jakarta.data.provider;

import java.lang.annotation.Annotation;
import java.util.Set;

import jakarta.data.exceptions.MappingException;

/**
 * To propose in Jakarta Data.
 */
public interface DataProvider {
    <R> R getRepository(Class<R> repositoryInterface) throws MappingException;

    String name();

    void repositoryBeanDisposed(Object repository);

    Set<Class<? extends Annotation>> supportedEntityAnnotations();
}
