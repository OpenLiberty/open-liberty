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

public abstract class Generatorv2TestBase extends GeneratorTestBase {

  // This is a helpful utility method that will generate a schema for an arbitary install using the latest schema generator.
  public static void main(String[] args) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    setInstallDir(new File("/Users/nottinga/Documents/liberty/runtimes/85x/8550gavanilla"));
    BundleRepositoryRegistry.addBundleRepository(Utils.getInstallDir().getAbsolutePath(), ExtensionConstants.CORE_EXTENSION);
    new Generator().createSchema(new String[] {"build/server.xsd"});
  }
  
  @Test
  public void checkEnumeration() throws XPathExpressionException {
    assertXPath("The attribute should be a union with a variableType member",
        "complexType[@name='test']/attribute[@name='onError']//union/@memberTypes",
        "variableType");
    assertXPath("The onError attribute of the config element is expected to be a restriction of the String type.",
        "complexType[@name='test']/attribute[@name='onError']//union/simpleType/restriction/@base",
        "xsd:string");
    assertXPath("The onError attribute of the config element should have a WARN option.",
        "complexType[@name='test']/attribute[@name='onError']//union/simpleType/restriction/enumeration[1]/@value",
        "WARN");
    assertXPath("The onError attribute of the config element should have a FAIL option.",
        "complexType[@name='test']/attribute[@name='onError']//union/simpleType/restriction/enumeration[2]/@value",
        "FAIL");
    assertXPath("The onError attribute of the config element should have an IGNORE option.",
        "complexType[@name='test']/attribute[@name='onError']//union/simpleType/restriction/enumeration[3]/@value",
        "IGNORE");
  }
  
  @Test
  public void checkStringWithMinAndMax() throws XPathExpressionException {
    assertXPath("A String attribute with a min and a max should be a restriction on type.",
        "complexType[@name='test']/attribute[@name='minMaxString']/@use",
        "required");
    assertXPath("A String attribute with a min and a max should be a restriction on type.",
        "complexType[@name='test']/attribute[@name='minMaxString']//restriction/@base",
        "xsd:string");
    assertXPath("A String attribute with a min and a max should have minLength length set.",
        "complexType[@name='test']/attribute[@name='minMaxString']//restriction/minLength/@value",
        "2");
    assertXPath("A String attribute with a min and a max should have maxLength length set.",
        "complexType[@name='test']/attribute[@name='minMaxString']//restriction/maxLength/@value",
        "4");
  }
  
  @Test
  public void checkStringWithMin() throws XPathExpressionException {
    assertXPath("A String attribute with a min and a max should be a restriction on type.",
        "complexType[@name='test']/attribute[@name='minString']/@use",
        "required");
    assertXPath("A String attribute with a min and a max should be a restriction on type.",
        "complexType[@name='test']/attribute[@name='minString']//restriction/@base",
        "xsd:string");
    assertXPath("A String attribute with a min should have minLength length set.",
        "complexType[@name='test']/attribute[@name='minString']//restriction/minLength/@value",
        "2");
    assertXPath("A String attribute with a min should not have maxLength length set.",
        "complexType[@name='test']/attribute[@name='minString']//restriction/maxLength",
        null);
  }

  @Test
  public void checkStringWithMax() throws XPathExpressionException {
    assertXPath("A String attribute with a min and a max should be a restriction on type.",
        "complexType[@name='test']/attribute[@name='maxString']/@use",
        "required");
    assertXPath("A String attribute with a min and a max should be a restriction on type.",
        "complexType[@name='test']/attribute[@name='maxString']//restriction/@base",
        "xsd:string");
    assertXPath("A String attribute with a max should not have minLength length set.",
        "complexType[@name='test']/attribute[@name='maxString']//restriction/minLength",
        null);
    assertXPath("A String attribute with a max should have maxLength length set.",
        "complexType[@name='test']/attribute[@name='maxString']//restriction/maxLength/@value",
        "4");
  }

  @Test
  public void checkIntWithMinAndMax() throws XPathExpressionException {
    assertXPath("The attribute should be a union with a variableType member",
        "complexType[@name='test']/attribute[@name='intMinMax']/simpleType/union/@memberTypes",
        "variableType");
    assertXPath("The attribute be based on a restriction on xsd:int",
        "complexType[@name='test']/attribute[@name='intMinMax']/simpleType/union/simpleType/restriction/@base",
        "xsd:int");
    assertXPath("The attribute should have minInclusive set to 2",
        "complexType[@name='test']/attribute[@name='intMinMax']/simpleType/union/simpleType/restriction/minInclusive/@value",
        "2");
    assertXPath("The attribute should have maxInclusive set to 4",
        "complexType[@name='test']/attribute[@name='intMinMax']/simpleType/union/simpleType/restriction/maxInclusive/@value",
        "4");
  }
  
  @Test
  public void checkIntWithMin() throws XPathExpressionException {
    assertXPath("The attribute should be a union with a variableType member",
        "complexType[@name='test']/attribute[@name='intMin']/simpleType/union/@memberTypes",
        "variableType");
    assertXPath("The attribute be based on a restriction on xsd:int",
        "complexType[@name='test']/attribute[@name='intMin']/simpleType/union/simpleType/restriction/@base",
        "xsd:int");
    assertXPath("The attribute should have minInclusive set to 2",
        "complexType[@name='test']/attribute[@name='intMin']/simpleType/union/simpleType/restriction/minInclusive/@value",
        "2");
    assertXPath("The attribute should not have maxInclusive set",
        "complexType[@name='test']/attribute[@name='intMin']/simpleType/union/simpleType/restriction/maxInclusive",
        null);
  }

  @Test
  public void checkIntWithMax() throws XPathExpressionException {
    assertXPath("The attribute should be a union with a variableType member",
        "complexType[@name='test']/attribute[@name='intMax']/simpleType/union/@memberTypes",
        "variableType");
    assertXPath("The attribute be based on a restriction on xsd:int",
        "complexType[@name='test']/attribute[@name='intMax']/simpleType/union/simpleType/restriction/@base",
        "xsd:int");
    assertXPath("The attribute should have minInclusive set to 2",
        "complexType[@name='test']/attribute[@name='intMax']/simpleType/union/simpleType/restriction/minInclusive",
        null);
    assertXPath("The attribute should not have maxInclusive set",
        "complexType[@name='test']/attribute[@name='intMax']/simpleType/union/simpleType/restriction/maxInclusive/@value",
        "4");
  }

  @Test
  public void checkOnErrorType() throws XPathExpressionException {
    assertXPath("The attribute should be a union with a variableType member",
        "complexType[@name='test']/attribute[@name='anError']//union/@memberTypes",
        "variableType");
    assertXPath("The attribute be based on a restriction on xsd:string",
        "complexType[@name='test']/attribute[@name='anError']//union/simpleType/restriction/@base",
        "xsd:string");
    assertXPath("The onError attribute of the config element should have a WARN option.",
        "complexType[@name='test']/attribute[@name='onError']//union/simpleType/restriction/enumeration[1]/@value",
        "WARN");
    assertXPath("The onError attribute of the config element should have a FAIL option.",
        "complexType[@name='test']/attribute[@name='onError']//union/simpleType/restriction/enumeration[2]/@value",
        "FAIL");
    assertXPath("The onError attribute of the config element should have an IGNORE option.",
        "complexType[@name='test']/attribute[@name='onError']//union/simpleType/restriction/enumeration[3]/@value",
        "IGNORE");
  }

  @Test
  public void checkOnConflictType() throws XPathExpressionException {
    assertXPath("The attribute should be a union with a variableType member",
        "complexType[@name='includeType']/attribute[@name='onConflict']//union/@memberTypes",
        "variableType");
    assertXPath("The attribute be based on a restriction on xsd:string",
        "complexType[@name='includeType']/attribute[@name='onConflict']//union/simpleType/restriction/@base",
        "xsd:string");
    assertXPath("The onConflict attribute of the config element should have a MERGE option.",
        "complexType[@name='includeType']/attribute[@name='onConflict']//union/simpleType/restriction/enumeration[1]/@value",
        "MERGE");
    assertXPath("The onConflict attribute of the config element should have a REPLACE option.",
        "complexType[@name='includeType']/attribute[@name='onConflict']//union/simpleType/restriction/enumeration[2]/@value",
        "REPLACE");
    assertXPath("The onConflict attribute of the config element should have an IGNORE option.",
        "complexType[@name='includeType']/attribute[@name='onConflict']//union/simpleType/restriction/enumeration[3]/@value",
        "IGNORE");
  
  }
}