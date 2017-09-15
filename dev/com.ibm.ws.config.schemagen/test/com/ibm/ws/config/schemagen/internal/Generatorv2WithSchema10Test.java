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
package com.ibm.ws.config.schemagen.internal;

import javax.xml.xpath.XPathExpressionException;

import org.junit.BeforeClass;
import org.junit.Test;

public class Generatorv2WithSchema10Test extends Generatorv2TestBase {
  @BeforeClass
  public static void setup() throws Exception {
    setup(new String[] {"build/server.xsd", "-outputVersion=2", "-schemaVersion=1.0"});
  }

  @Test
  public void testSchema10Any() throws XPathExpressionException {
    assertXPath("There should be an xsd:any with an attribute of processContent='skip'",
                "complexType[@name='testAny']/choice/any/@processContents",
                "skip");
    assertXPath("There should be an xsd:any with an attribute of maxOccurs='unbounded'",
                "complexType[@name='testAny']/choice/any/@maxOccurs",
                "unbounded");
    assertXPath("There should not be a pid elememnt", "complexType[@name='testAny']/choice/element[@name='pid']", null);
  }
}
