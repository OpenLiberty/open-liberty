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
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class MPConfigBeanWithApplicationScope {

    @Inject
    @ConfigProperty(name = "app_scope_key", defaultValue = "annoValue")
    String testKey;

    public void appScopeDefaultValueTest() {
        check("defaultValue");
    }

    public void appScopeEnvValueTest() {
        check("envValue");
    }

    public void appScopeEnvValueChangeTest() {
        check("envValueChange");
    }

    public void appScopeServerValueTest() {
        check("serverValue");
    }

    public void appScopeAnnoValueTest() {
        check("annoValue");
    }

    private void check(String expected) {
        assertEquals("Wrong value for test key.", expected, testKey);
    }
}
