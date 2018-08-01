/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.duplicateInServerXML.web;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RequestScoped
public class DuplicateInServerXMLBean {

    @Inject
    @ConfigProperty(name = "serverXMLKey1")
    String orderVariable1;

    @Inject
    @ConfigProperty(name = "serverXMLKey2")
    String orderVariable2;

    @Inject
    @ConfigProperty(name = "serverXMLKey3")
    String orderVariable3;

    // The duplicateAppPropertiesTest tests the behaviour, defined by mp config 1.3
    // metatype.xml, where property elements will be merged across an application
    // element such that the last defined property is used.
    //
    // The server.xml has,
    //
    // <application location="duplicateInServerXMLApp.war">
    //    <appProperties>
    //       <property name="serverXMLKey1" value="valueinAppProperties1"/>
    //       <property name="serverXMLKey2" value="valueinAppProperties2a"/>
    //    </appProperties>
    //    <appProperties>
    //       <property name="serverXMLKey2" value="valueinAppProperties2b"/>
    //       <property name="serverXMLKey3" value="valueinAppProperties3"/>
    //    </appProperties>
    // </application>
    //
    // In this test each of serverXMLKey1, serverXMLKey2 and serverXMLKey3 will be
    // retrieved from the config with the valueinAppProperties2b being used for
    // serverXMLKey2.
    //
    public void duplicateAppPropertiesTest() throws Exception {
        assertEquals("valueinAppProperties1", orderVariable1);
        assertEquals("valueinAppProperties2b", orderVariable2);
        assertEquals("valueinAppProperties3", orderVariable3);
    }
}
