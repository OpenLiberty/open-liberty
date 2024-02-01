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
package mpapp1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class ApplicationScopedOnCheckpointBean {

    @Inject
    @ConfigProperty(name = "early_access_app_scope_key", defaultValue = "annoValue")
    String testKey;

    @Inject
    @ConfigProperty(name = "early_access_optional_app_scope_key")
    Optional<String> earlyAccessOptionalTestKey;

    @Inject
    @ConfigProperty(name = "early_access_provider_optional_app_scope_key")
    Provider<Optional<String>> earlyAccessProviderOptionalTestKey;

    public void appScopedValueTest() {
        // The value of test_key was not updated here because the property was accessed during the checkpoint.
        // A warning CWWKC0651W is logged to let the user know that the updated value of the property might not be used during restore.
        check("defaultValue");
    }

    //@Initialized(ApplicationScoped.class) is seen before any request. Accessing the bean in the early startup code of application.
    public void observeInit(@Observes @Initialized(ApplicationScoped.class) Object event) {
        System.out.println(getClass() + ": " + "Initializing application context");
        check("defaultValue");
        checkOptionalValue(false);
        checkProviderOptionalValue(false);
    }

    public void appScopeEarlyAccessNoDefaultEnvValueTest() {
        // The value of earlyAccessOptionalTestKey was not updated here because the property was accessed during the checkpoint.
        // A warning CWWKC0651W is logged to let the user know that the updated value of the property might not be used during restore.
        checkOptionalValue(false);
    }

    public void appScopeEarlyAccessNoDefaultServerValueTest() {
        // The value of earlyAccessOptionalTestKey was not updated here because the property was accessed during the checkpoint.
        // A warning CWWKC0651W is logged to let the user know that the updated value of the property might not be used during restore.
        checkOptionalValue(false);
    }

    public void appScopeEarlyAccessNoDefaultProviderEnvValueTest() {
        checkProviderOptionalValue(true, "providerEnvValue");
    }

    public void appScopeEarlyAccessNoDefaultProviderServerValueTest() {
        checkProviderOptionalValue(true, "providerServerValue");
    }

    private void check(String expected) {
        assertEquals("Wrong value for test key.", expected, testKey);
    }

    private void checkOptionalValue(boolean present) {
        if (present) {
            assertTrue("Value of optional test key should be present.", earlyAccessOptionalTestKey.isPresent());
        } else {
            assertFalse("Value of optional test key should not be present.", earlyAccessOptionalTestKey.isPresent());
        }
    }

    private void checkProviderOptionalValue(boolean present, String... expected) {
        if (present) {
            assertTrue("Value of provider optional test key should be present.", earlyAccessProviderOptionalTestKey.get().isPresent());
            // Even though the provider value gets updated on restore, the warning message CWWKC0651W will be logged.
            assertEquals("Wrong value of provider optional test key.", expected[0], earlyAccessProviderOptionalTestKey.get().get());
        } else {
            assertFalse("Value of provider optional test key should not be present.", earlyAccessProviderOptionalTestKey.get().isPresent());
        }

    }
}
