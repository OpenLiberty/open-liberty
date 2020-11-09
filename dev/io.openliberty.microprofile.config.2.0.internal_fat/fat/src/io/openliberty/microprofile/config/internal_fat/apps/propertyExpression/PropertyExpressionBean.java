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
package io.openliberty.microprofile.config.internal_fat.apps.propertyExpression;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.Assert;

@RequestScoped
public class PropertyExpressionBean {

    @Inject
    @ConfigProperty(name = "keyFromVariableInServerXML")
    String key1;

    @Inject
    @ConfigProperty(name = "keyFromAppPropertyInServerXML")
    String key2;

    /**
     * keyFromVariableInServerXML is defined as a variable in the server.xml file as "${value1DefinedInTwoPlaces}".
     *
     * value1DefinedInTwoPlaces is defined in the bootstrap.properties as "value1a" and microprofile-config.properties as "value1b".
     *
     * Since bootstrap.properties has a higher ordinal than microprofile-config.properties (400>100), value1DefinedInTwoPlaces="value1a".
     *
     * Hence, by Property Expression, `key1` should have the value of "value1a".
     */
    public void checkVariable() throws Exception {
        Assert.assertEquals("value1a", key1);
    }

    /**
     * keyFromAppPropertyInServerXML is defined as an appProperty in the server.xml file as "${value2DefinedInBootstrapProperties}"
     *
     * value2DefinedInBootstrapProperties is defined in the bootstrap.properties file as "value2".
     *
     * Hence, by Property Expression, `key2` should have the value of "value2".
     */
    public void checkAppProperty() throws Exception {
        Assert.assertEquals("value2", key2);
    }

}
