/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.container.osgi.internal;

import java.util.Collections;
import java.util.List;

import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.javaee.dd.common.JNDIEnvironmentRef;
import com.ibm.ws.javaee.dd.common.PersistenceUnitRef;
import com.ibm.ws.jpa.management.JPAPUnitProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessor;
import com.ibm.wsspi.injectionengine.InjectionProcessorProvider;

@Component(service = InjectionProcessorProvider.class)
public class JPAPUnitProcessorProvider extends InjectionProcessorProvider<PersistenceUnit, PersistenceUnits> {
    private static final List<Class<? extends JNDIEnvironmentRef>> REF_CLASSES =
                    Collections.<Class<? extends JNDIEnvironmentRef>> singletonList(PersistenceUnitRef.class);

    /** {@inheritDoc} */
    @Override
    public InjectionProcessor<PersistenceUnit, PersistenceUnits> createInjectionProcessor() {
        return new JPAPUnitProcessor();
    }

    /** {@inheritDoc} */
    @Override
    public Class<PersistenceUnit> getAnnotationClass() {
        return PersistenceUnit.class;
    }

    /** {@inheritDoc} */
    @Override
    public Class<PersistenceUnits> getAnnotationsClass() {
        return PersistenceUnits.class;
    }

    /** {@inheritDoc} */
    @Override
    public List<Class<? extends JNDIEnvironmentRef>> getJNDIEnvironmentRefClasses() {
        return REF_CLASSES;
    }
}
