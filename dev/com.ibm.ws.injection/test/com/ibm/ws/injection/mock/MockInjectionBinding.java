/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.mock;

import java.lang.reflect.Member;

import javax.annotation.Resource;

import com.ibm.wsspi.injectionengine.ComponentNameSpaceConfiguration;
import com.ibm.wsspi.injectionengine.InjectionBinding;
import com.ibm.wsspi.injectionengine.InjectionException;
import com.ibm.wsspi.injectionengine.InjectionTargetContext;

/**
 *
 */
@Resource
public class MockInjectionBinding extends InjectionBinding<Resource> {

    private final Object ivReturnObj;

    /**
     * @param annotation
     * @param nameSpaceConfig
     */
    public MockInjectionBinding(Object returnObj) {
        super(MockInjectionBinding.class.getAnnotation(Resource.class), new ComponentNameSpaceConfiguration(null, null));
        ivReturnObj = returnObj;
    }

    /** {@inheritDoc} */
    @Override
    public void merge(Resource annotation, Class<?> instanceClass, Member member) throws InjectionException {
        throw new UnsupportedOperationException("Not Implemented");
    }

    @Override
    public void mergeSaved(InjectionBinding<Resource> binding) {
        // Nothing.
    }

    /** {@inheritDoc} */
    @Override
    protected Object getInjectionObjectInstance(Object targetObject,
                                                InjectionTargetContext targetContext) throws Exception {
        if (ivReturnObj == null)
            throw new NullPointerException("Expected Test Exception");

        return ivReturnObj;
    }

}
