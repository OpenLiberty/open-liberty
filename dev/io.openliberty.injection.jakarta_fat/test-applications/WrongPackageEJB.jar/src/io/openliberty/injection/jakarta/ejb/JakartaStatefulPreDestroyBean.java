/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.injection.jakarta.ejb;

import jakarta.ejb.EJBException;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

/**
 * Stateful bean using jakarta package annotations, except for
 * an incorrect use of javax.annotation.PreDestroy.
 */
@Stateful
public class JakartaStatefulPreDestroyBean {

    @javax.annotation.PreDestroy
    private void postConstruct() {
        throw new EJBException("javax.annotation.PreDestroy should not be called");
    }

    @Remove
    public void remove() {
        // bean is removed after method completes
    }
}
