/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package mpapp1;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ApplicationScopedOnCheckpointBean {

    @Inject
    @ConfigProperty(name = "early_access_app_scope_key", defaultValue = "annoValue")
    String testKey;

    public void applicationScopedValueTest() {
        // The value of test_key was not updated here because the property was accessed during the checkpoint.
        // A warning CWWKC0651W is thrown to let the user know that the updated value of the property might not be used during restore.
        check("defaultValue");
    }

    //@Initialized(ApplicationScoped.class) is seen before any request. Accessing the bean in the early startup code of application.
    public void observeInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        System.out.println(getClass() + ": " + "Initializing application context");
        check("defaultValue");
    }

    private void check(String expected) {
        assertEquals("Wrong value for test key.", expected, testKey);
    }
}
