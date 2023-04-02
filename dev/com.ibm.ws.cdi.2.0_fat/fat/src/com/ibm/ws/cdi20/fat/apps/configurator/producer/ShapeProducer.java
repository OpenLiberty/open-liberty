/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.cdi20.fat.apps.configurator.producer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class ShapeProducer {

    @Produces
    @ThreeDimensional
    public Dodecahedron producer(ParameterInjectedBean p) {
        return new Dodecahedron(p);
    }

    @Produces
    @Default
    @Dependent
    public ParameterInjectedBean produceParamOne() {
        return new ParameterInjectedBean(Default.class);
    }
}