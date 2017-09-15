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

public class Generatorv1WithSchema10Test extends Generatorv1TestBase {
  @BeforeClass
  public static void setup() throws Exception {
    setup(new String[] {"build/server.xsd", "-outputVersion=1", "-schemaVersion=1.0"});
  }

  @Test
  public void testSchema10Any() throws XPathExpressionException {
    assertXPath("There should not be an xsd:any", "complexType[@name='testAny']/choice/any", null);
    assertXPath("There should be a pid element", "complexType[@name='testAny']/choice/element[@name='pid']/@type", "other");
  }
}
