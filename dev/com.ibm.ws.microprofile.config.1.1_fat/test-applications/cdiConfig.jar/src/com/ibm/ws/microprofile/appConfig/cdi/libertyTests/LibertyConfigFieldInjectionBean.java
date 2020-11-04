/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.cdi.libertyTests;

import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.ws.microprofile.appConfig.cdi.test.Animal;
import com.ibm.ws.microprofile.appConfig.cdi.test.Parent;
import com.ibm.ws.microprofile.appConfig.cdi.test.Pizza;

public class LibertyConfigFieldInjectionBean {

    @Inject
    @ConfigProperty(name = "PARENT_KEY")
    Parent PARENT_KEY;

    @Inject
    @ConfigProperty(name = "DOG_KEY")
    Animal ANIMAL_KEY;

    /**
     * The PIZZA_KEY does exist in the config source but it will result to null value on purpose. Even though the default value is good but it will not be used.
     *
     * TODO: The intended behaviour for this for mpConfig > 1.4 is still in discussion: https://github.com/eclipse/microprofile-config/issues/608.
     *
     * Currently, mpConfig > 1.4 is throwing a "DeploymentException: SRCFG02004: Required property PIZZA_KEY not found".
     */
    @Inject
    @ConfigProperty(name = "PIZZA_KEY", defaultValue = "chicken;12")
    Pizza PIZZA_EXISTING_PROP;

    /**
     * This PIZZA_NEW_KEY does not exist in the configure sources. The default value "" will be null, which will result to a null.
     *
     * From mpConfig > 1.4 an "Empty string as the default value will be ignored, which is same as not setting the default value"
     *
     * Since the property would not be defined, this would cause a `NoSuchElementException`.
     */
    @Inject
    @ConfigProperty(name = "PIZZA_NEW_KEY", defaultValue = "")
    Pizza PIZZA_MISSING_PROP;

    public LibertyConfigFieldInjectionBean() {
        System.out.println("xtor: ConfigFieldInjectionBean()");
    }

    public Parent getPARENT_KEY() {
        return PARENT_KEY;
    }

    public Animal getANIMAL_KEY() {
        return ANIMAL_KEY;
    }

    public Pizza getPIZZA_KEY() {
        return PIZZA_EXISTING_PROP;
    }

    public Pizza getPIZZA_MISSING_KEY() {
        return PIZZA_MISSING_PROP;
    }

}
