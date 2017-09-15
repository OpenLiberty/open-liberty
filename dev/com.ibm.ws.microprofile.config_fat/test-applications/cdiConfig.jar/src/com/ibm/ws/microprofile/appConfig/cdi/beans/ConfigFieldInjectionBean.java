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
package com.ibm.ws.microprofile.appConfig.cdi.beans;

import java.net.URL;
import java.util.Optional;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.ws.microprofile.appConfig.cdi.test.Animal;
import com.ibm.ws.microprofile.appConfig.cdi.test.Dog;
import com.ibm.ws.microprofile.appConfig.cdi.test.MyObject;
import com.ibm.ws.microprofile.appConfig.cdi.test.Parent;
import com.ibm.ws.microprofile.appConfig.cdi.test.Pizza;

public class ConfigFieldInjectionBean {

    @Inject
    Config config;

    @Inject
    Config config2;

    @Inject
    @ConfigProperty(name = "GENERIC_OBJECT_KEY")
    Instance<MyObject> GENERIC_INSTANCE_KEY;

    @Inject
    @ConfigProperty(name = "GENERIC_OBJECT_KEY")
    Provider<MyObject> GENERIC_PROVIDER_KEY;

    @Inject
    @ConfigProperty(name = "SIMPLE_KEY2")
    String SIMPLE_KEY2;

    @Inject
    @ConfigProperty(name = "SIMPLE_KEY3")
    String key3;

    @Inject
    @ConfigProperty(name = "PARENT_KEY")
    Parent PARENT_KEY;

    @Inject
    @ConfigProperty(name = "DISCOVERED_KEY", defaultValue = "NULL")
    String DISCOVERED_KEY;

    @Inject
    @ConfigProperty(name = "NULL_KEY", defaultValue = "DEFAULT_VALUE")
    String NULL_WITH_DEFAULT_KEY;

    @Inject
    @ConfigProperty(name = "DOG_KEY")
    Dog DOG_KEY;

    @Inject
    @ConfigProperty(name = "DOG_KEY")
    Animal ANIMAL_KEY;

    @Inject
    @ConfigProperty(name = "URL_KEY")
    Optional<URL> OPTIONAL_KEY;

    @Inject
    @ConfigProperty(name = "NULL_KEY")
    Optional<URL> OPTIONAL_NULL_KEY;

    @Inject
    @ConfigProperty(name = "MISSING_KEY")
    Optional<URL> OPTIONAL_MISSING_KEY;

    @Inject
    @ConfigProperty(name = "SYS_PROP", defaultValue = "NULL")
    String SYS_PROP;

    /**
     * This PIZZA_NEW_KEY does not exist in the configure sources. The default value "" will be null, which will result to a null.
     */
    @Inject
    @ConfigProperty(name = "PIZZA_NEW_KEY", defaultValue = "")
    Pizza PIZZA_MISSING_PROP;

    /**
     * The PIZZA_KEY does exist in the config source but it will result to null value on purpose. Even though the default value is good but it will not be used.
     */
    @Inject
    @ConfigProperty(name = "PIZZA_KEY", defaultValue = "chicken;12")
    Pizza PIZZA_EXISTING_PROP;

    /**
     * This is the sanity test to check PIZZA_GOOD_KEY will be resolved to a good Pizza. The default value is not used.
     */
    @Inject
    @ConfigProperty(name = "PIZZA_GOOD_KEY", defaultValue = "")
    Pizza PIZZA_GOOD_PROP;

    public String getSIMPLE_KEY1() {
        return config.getValue("SIMPLE_KEY1", String.class);
    }

    /**
     * @return the gENERIC_OBJECT_KEY
     */
    public MyObject getGENERIC_INSTANCE_KEY() {
        return GENERIC_INSTANCE_KEY.get();
    }

    public MyObject getGENERIC_PROVIDER_KEY() {
        return GENERIC_PROVIDER_KEY.get();
    }

    /**
     * @return the sIMPLE_KEY2
     */
    public String getSIMPLE_KEY2() {
        return SIMPLE_KEY2;
    }

    /**
     * @return the key3
     */
    public String getSIMPLE_KEY3() {
        return key3;
    }

    /**
     * @return the uRL_KEY
     */
    public Parent getPARENT_KEY() {
        return PARENT_KEY;
    }

    public String getDISCOVERED_KEY() {
        return DISCOVERED_KEY;
    }

    public String getNULL_WITH_DEFAULT_KEY() {
        return NULL_WITH_DEFAULT_KEY;
    }

    public Dog getDOG_KEY() {
        return DOG_KEY;
    }

    public Animal getANIMAL_KEY() {
        return ANIMAL_KEY;
    }

    public String getSYS_PROP() {
        return SYS_PROP;
    }

    public URL getOPTIONAL_KEY() {
        return OPTIONAL_KEY.orElse(null);
    }

    public URL getOPTIONAL_NULL_KEY() {
        return OPTIONAL_NULL_KEY.orElse(null);
    }

    public URL getOPTIONAL_MISSING_KEY() {
        return OPTIONAL_MISSING_KEY.orElse(null);
    }

    public Pizza getPIZZA_KEY() {
        return PIZZA_EXISTING_PROP;
    }

    public Pizza getPIZZA_MISSING_KEY() {
        return PIZZA_MISSING_PROP;
    }

    public Pizza getPIZZA_GOOD_KEY() {
        return PIZZA_GOOD_PROP;
    }

}
