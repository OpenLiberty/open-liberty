/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package beanvalidation11;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.validation.ParameterNameProvider;

/**
 * Simple implementation of a ParameterNameProvider that provides modified
 * parameter names for testing purposes.
 */
public class CustomParameterNameProvider implements ParameterNameProvider {

    @Override
    public List<String> getParameterNames(Constructor<?> arg0) {
        return getParameterNames(arg0.getParameterTypes());
    }

    @Override
    public List<String> getParameterNames(Method arg0) {
        return getParameterNames(arg0.getParameterTypes());
    }

    private List<String> getParameterNames(final Class<?>[] classes) {
        final List<String> list = new ArrayList<String>(classes.length);

        for (int i = 0; i < classes.length; i++) {
            list.add(classes[i].getSimpleName() + "_" + i);
        }

        return list;
    }

}
