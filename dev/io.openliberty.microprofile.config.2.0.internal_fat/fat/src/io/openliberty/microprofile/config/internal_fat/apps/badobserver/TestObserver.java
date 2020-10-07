/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.badObserver;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TestObserver {

    // Should throw a deployment exception
    private static final void observerMethod(@Observes @Initialized(ApplicationScoped.class) final Object obj,
                                             @ConfigProperty(name = "DOESNOTEXIST") final String doesnotexist) {
        throw new RuntimeException("This method should not have been called");
    }
}
