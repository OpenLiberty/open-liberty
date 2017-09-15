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


import java.io.File;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;

public abstract class Generatorv1TestBase extends GeneratorTestBase {
  // This is a helpful utility method that will generate a schema for an arbitary install using the latest schema generator.
  public static void main(String[] args) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    setInstallDir(new File("/Users/nottinga/Documents/liberty/runtimes/85x/8550gavanilla"));
    BundleRepositoryRegistry.addBundleRepository(Utils.getInstallDir().getAbsolutePath(), ExtensionConstants.CORE_EXTENSION);
    new Generator().createSchema(new String[] {"build/server.xsd"});
  }
  
  // TODO different between v1 and v2.
  @Test
  public void checkEnumeration() throws XPathExpressionException {
    assertXPath("The onError attribute of the config element is expected to be a restriction of the String type.",
        "complexType[@name='test']/attribute[@name='onError']/simpleType/restriction/@base",
        "xsd:string");
    assertXPath("The onError attribute of the config element should have a WARN option.",
        "complexType[@name='test']/attribute[@name='onError']/simpleType/restriction/enumeration[1]/@value",
        "WARN");
    assertXPath("The onError attribute of the config element should have a FAIL option.",
        "complexType[@name='test']/attribute[@name='onError']/simpleType/restriction/enumeration[2]/@value",
        "FAIL");
    assertXPath("The onError attribute of the config element should have an IGNORE option.",
        "complexType[@name='test']/attribute[@name='onError']/simpleType/restriction/enumeration[3]/@value",
        "IGNORE");
  }
  
  @Test
  public void checkOnErrorType() throws XPathExpressionException {
    assertXPath("The attribute be based on a restriction on xsd:string",
        "complexType[@name='test']/attribute[@name='anError']/simpleType/restriction/@base",
        "xsd:string");
    assertXPath("The onError attribute of the config element should have a WARN option.",
        "complexType[@name='test']/attribute[@name='onError']/simpleType/restriction/enumeration[1]/@value",
        "WARN");
    assertXPath("The onError attribute of the config element should have a FAIL option.",
        "complexType[@name='test']/attribute[@name='onError']/simpleType/restriction/enumeration[2]/@value",
        "FAIL");
    assertXPath("The onError attribute of the config element should have an IGNORE option.",
        "complexType[@name='test']/attribute[@name='onError']/simpleType/restriction/enumeration[3]/@value",
        "IGNORE");
  }
  
  @Test
  public void checkOnConflictType() throws XPathExpressionException {
    assertXPath("The attribute be based on a restriction on xsd:string",
        "complexType[@name='includeType']/attribute[@name='onConflict']/simpleType/restriction/@base",
        "xsd:string");
    assertXPath("The onConflict attribute of the config element should have a MERGE option.",
        "complexType[@name='includeType']/attribute[@name='onConflict']/simpleType/restriction/enumeration[1]/@value",
        "MERGE");
    assertXPath("The onConflict attribute of the config element should have a REPLACE option.",
        "complexType[@name='includeType']/attribute[@name='onConflict']/simpleType/restriction/enumeration[2]/@value",
        "REPLACE");
    assertXPath("The onConflict attribute of the config element should have a IGNORE option.",
        "complexType[@name='includeType']/attribute[@name='onConflict']/simpleType/restriction/enumeration[3]/@value",
        "IGNORE");
   
  }
}