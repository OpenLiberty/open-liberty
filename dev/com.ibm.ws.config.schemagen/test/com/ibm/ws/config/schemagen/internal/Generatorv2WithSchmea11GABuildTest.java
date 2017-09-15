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

import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class Generatorv2WithSchmea11GABuildTest extends Generatorv2WithSchema11Test {
  private static File version = new File("build/wlp/lib/versions/WebSphereApplicationServer.properties");
  @BeforeClass
  public static void setup() throws Exception {
    version.getParentFile().mkdirs();
    Properties props = new Properties();
    props.setProperty("com.ibm.websphere.productVersion", "17.0.0.1");
    props.store(new FileWriter(version), null);
    setup(new String[] {"build/server.xsd", "-outputVersion=2", "-schemaVersion=1.1"});
  }
  
  @AfterClass
  public static void tearDown() {
    version.delete();
  }

  @Test
  public void checkBetaTagOnOCD() throws XPathExpressionException {
    Object obj = xp.evaluate("/schema/complexType[@name='serverType']//element[@name='betaElement']", root, XPathConstants.NODE);
    assertNull("betaElement should not be in schema", obj);
  }

  @Test
  public void checkBetaTagOnAD() throws XPathExpressionException {
    assertNull("betaElement should not be in schema", xp.evaluate("/schema/complexType[@name='test']/attribute[@name='beta']", root, XPathConstants.NODE));
  }
}
