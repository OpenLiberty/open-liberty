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

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class MPConfigBean {

    @Inject
    @ConfigProperty(name = "req_scope_key", defaultValue = "annoValue")
    String testKey;

    public void defaultValueTest() {
        check("defaultValue");
    }

    public void envValueTest() {
        check("envValue");
    }

    public void envValueChangeTest() {
        check("envValueChange");
    }

    public void serverValueTest() {
        check("serverValue");
    }

    public void annoValueTest() {
        check("annoValue");
    }

    private void check(String expected) {
        assertEquals("Wrong value for test key.", expected, testKey);
    }
}
