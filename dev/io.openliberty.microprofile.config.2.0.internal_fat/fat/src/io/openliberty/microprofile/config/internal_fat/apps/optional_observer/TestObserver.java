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
package io.openliberty.microprofile.config.internal_fat.apps.optional_observer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TestObserver {

    @Inject
    @ConfigProperty(defaultValue = "hello")
    String property;

    private static final void observerMethod1(@Observes @Initialized(ApplicationScoped.class) final Object obj,
                                              @ConfigProperty(name = "DOESNOTEXIST") final Optional<String> optionalProperty) {
        assertNotNull(optionalProperty);
        assertFalse(optionalProperty.isPresent());
        System.out.println("observerMethod1 run");
    }

    private static final void observerMethod2(@Observes @Initialized(ApplicationScoped.class) final Object obj,
                                              @ConfigProperty(name = "DOESNOTEXIST", defaultValue = "OHYESITDOES") final String defaultValue) {
        assertEquals("OHYESITDOES", defaultValue);
        System.out.println("observerMethod2 run");
    }

    private static final void observerMethod3(@Observes @Initialized(ApplicationScoped.class) final Object obj,
                                              @ConfigProperty(name = "DOESNOTEXIST", defaultValue = "OHYESITDOES") final Instance<String> instance) {
        assertEquals("OHYESITDOES", instance.get());
        System.out.println("observerMethod3 run");
    }

    public String getProperty() {
        return property;
    }
}
