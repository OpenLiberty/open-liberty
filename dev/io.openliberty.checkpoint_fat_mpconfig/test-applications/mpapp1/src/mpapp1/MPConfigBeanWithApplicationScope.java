/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MPConfigBeanWithApplicationScope {

    @Inject
    @ConfigProperty(name = "app_scope_key", defaultValue = "annoValue")
    String testKey;

    @Inject
    @ConfigProperty(name = "optional_app_scope_key")
    Optional<String> optionalTestKey;

    @Inject
    @ConfigProperty(name = "provider_app_scope_key")
    Provider<String> providerTestKey;

    public void appScopeDefaultValueTest() {
        check("defaultValue");
    }

    public void appScopeEnvValueTest() {
        check("envValue");
    }

    public void appScopeServerValueTest() {
        check("serverValue");
    }

    public void appScopeAnnoValueTest() {
        check("annoValue");
    }

    public void varDirValueTest() {
        check("varDirValue");
    }

    public void appScopeNoDefaultEnvValueTest() {
        checkOptionalKey("optionalEnvValue");
    }

    public void appScopeNoDefaultServerValueTest() {
        checkOptionalKey("optionalServerValue");
    }

    public void appScopeProviderEnvValueTest() {
        checkProviderKey("providerEnvValue");
    }

    private void check(String expected) {
        assertEquals("Wrong value for test key.", expected, testKey);
    }

    private void checkOptionalKey(String expected) {
        assertEquals("Wrong value for optional test key.", expected, optionalTestKey.get());
    }

    private void checkProviderKey(String expected) {
        assertEquals("Wrong value for optional test key.", expected, providerTestKey.get());
    }
}
