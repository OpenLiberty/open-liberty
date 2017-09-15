/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.archaius.impl.fat.tests;

import org.eclipse.microprofile.config.Config;

import com.ibm.ws.microprofile.appConfig.test.utils.AppConfigTestApp;
import com.ibm.ws.microprofile.config.fat.tests.AbstractConfigApiTest;

/**
 *
 */
public abstract class AbstractAppConfigTestApp implements AppConfigTestApp {

    public String ffdc;

    public String getResult(Config config) {
        CHelper ch = new CHelper(this, config);
        String result = "";//ch.toString();
        if (checkProps(ch)) {
            result = this.addPassed(result);
        } else {
            result = this.addFfdc(result, ch);
        }
        return result;
    }

    /**
     * @param result
     * @param ch
     * @return
     */
    public String addFfdc(String result, CHelper ch) {
        return this.ffdc = ffdc + " PROBLEM: " + this.getClass().getSimpleName() + " " + ch;
    }

    /**
     * This is the default simplest checker for this ear that is often overridden
     *
     * @param config
     * @return
     */
    protected boolean checkProps(Config c) {
        return checkProps(new CHelper(c));
    }

    /**
     * This is the default simplest checker for this ear that is often overridden
     *
     * @param config
     * @return
     */
    public boolean checkProps(CHelper ch) {
        return ch.contains("defaultSources.jar.meta-inf.config.properties", "jarPropertiesDefaultValue");
    }

    /**
     * @param result
     * @return
     */
    protected String addPassed(String result) {
        return result + "\n" + this.getClass().getSimpleName() + ": " + AbstractConfigApiTest.PASSED;
    }

}
