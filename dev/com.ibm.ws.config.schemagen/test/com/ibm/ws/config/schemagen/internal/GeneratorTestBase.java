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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.osgi.framework.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;
import com.ibm.ws.kernel.provisioning.ExtensionConstants;

public abstract class GeneratorTestBase {

  private static ResourceBundle messages = ResourceBundle.getBundle("OSGI-INF/l10n/metatype");
  protected static XPath xp;
  protected static Element root;

  public static void setup(String[] args) throws Exception {
    setInstallDir(new File("build/wlp"));
    BundleRepositoryRegistry.addBundleRepository(Utils.getInstallDir().getAbsolutePath(), ExtensionConstants.CORE_EXTENSION);
    File installDir = Utils.getInstallDir();
    File featureDir = new File(installDir, "lib/features");
    File platformDir = new File(installDir, "lib/platform");
    
    featureDir.mkdirs();
    platformDir.mkdirs();
    
    Manifest man = new Manifest();
    Attributes attribs = man.getMainAttributes();
    attribs.put(Attributes.Name.MANIFEST_VERSION, "1");
    attribs.putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
    attribs.putValue(Constants.BUNDLE_SYMBOLICNAME, "myjar");
    
    JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(new File(installDir,"lib/myjar.jar")), man);
    putFile(jarOut, "test", "OSGI-INF/metatype/metatype.xml");
    putFile(jarOut, "test", "OSGI-INF/l10n/metatype.properties");
    jarOut.close();
  
    copyConfigJar(new File(installDir, "lib"));
  
    man = new Manifest();
    attribs = man.getMainAttributes();
    attribs.put(Attributes.Name.MANIFEST_VERSION, "1");
    attribs.putValue("Subsystem-Content", "myjar, com.ibm.ws.config");
    attribs.putValue("Subsystem-SymbolicName", "myfeature");
    attribs.putValue("Subsystem-Type", "osgi.subsystem.feature");
    attribs.putValue("Subsystem-ManifestVersion", "1");
    attribs.putValue("Subsystem-Version", "1");
    FileOutputStream fOut = new FileOutputStream(new File(featureDir, "myfeature.mf"));
    man.write(fOut);
    fOut.close();
    
    new Generator().createSchema(args);
    xp = XPathFactory.newInstance().newXPath();
    Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("build", "server.xsd"));
    root = doc.getDocumentElement();
  }

  static void copyConfigJar(File rootDir) {
    String properlyLibertyInstall = System.getenv("WLP_INSTALL_DIR");
    
    if (properlyLibertyInstall == null) {
      properlyLibertyInstall = "../build.image/wlp";
    }
    
    File dir = new File(properlyLibertyInstall, "lib");
    File[] jars = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith("com.ibm.ws.config_") && name.endsWith(".jar");
      }
    });
    
    if (jars != null) {
      for (File f : jars) {
        byte[] buffer = new byte[4096];
        
        try {
          InputStream in = new FileInputStream(f);
          OutputStream out = new FileOutputStream(new File(rootDir, f.getName()));
          int len;
          while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
          }
          in.close();
          out.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
        
      }
    }
  }

  static void putFile(JarOutputStream jarOut, String dir, String file)
      throws IOException {
        jarOut.putNextEntry(new ZipEntry(file));
        FileInputStream fIn = new FileInputStream(new File(dir, file));
        byte[] bytes = new byte[4096];
        int len;
        while ((len = fIn.read(bytes)) != -1) {
          jarOut.write(bytes, 0, len);
        }
        fIn.close();
      }

  protected static void setInstallDir(File file) throws SecurityException,
      NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Utils.setInstallDir(file);
        Field installDirField = SchemaWriter.class.getDeclaredField("installDir");
        installDirField.setAccessible(true);
        installDirField.set(null, file);
        System.setProperty("wlp.install.dir", file.getAbsolutePath());
      }

  protected void assertXPath(String failureMessage, String xpath, String expectation)
      throws XPathExpressionException {
        QName type = expectation == null ? XPathConstants.NODE : XPathConstants.STRING;
        Object val = xp.evaluate("/schema/" + xpath, root, type);
        
        assertEquals(failureMessage, expectation, val);
      }

  protected void assertOptionalDuration(String id, String type)
      throws XPathExpressionException {
        
        String stem = "complexType[@name='test']/attribute[@name='" + id + "']";
        
        assertXPath("The attribute's use is not optional.", 
            stem + "/@use", 
            "optional");
        assertXPath("The default is not 500.", 
            stem + "/@default", 
            "500");
        assertXPath("The type is not duration.", 
            stem + "/@type", 
            type);
      }

  @Test
  public void checkServerType() throws XPathExpressionException {
    assertXPath("Couldn't find the config child of the server element",
                "complexType[@name='serverType']//element[@name='test']/@type",
                "test");
    assertXPath("Couldn't find the standard include child of the server element",
                "complexType[@name='serverType']//element[@name='include']/@type",
                "includeType");
    assertXPath("Couldn't find the standard variable child of the server element",
                "complexType[@name='serverType']//element[@name='variable']/@type",
                "variableDefinitionType");
    assertXPath("Couldn't find the description attribute of the server element",
                "complexType[@name='serverType']//attribute[@name='description']/@type",
                "xsd:string");
  }

  @Test
  public void checkRootServerElement() throws XPathExpressionException {
    assertXPath("Couldn't find the root server element",
        "element[@name='server']/@type",
        "serverType");
  }
  
  @Test
  public void checkRootClientElement() throws XPathExpressionException {
	  assertXPath("Couldn't find the root client element", 
	  	"element[@name='client']/@type",
	  	"serverType");
  }

  public void checkExcludesChildren() throws XPathExpressionException {
    assertXPath("The attribute should be required.",
        "complexType[@name='extendsTest']/attribute[@name='required']/@use",
        "required");
  
    assertXPath("A noRefPidRef attribute should not have been generated of type pidType.",
                "complexType[@name='extendsTest']/attribute[@name='pid']",
                null);
  }

  @Test
  public void checkIncludeType() throws XPathExpressionException {
    assertXPath("Couldn't find the standard include child of the server element",
                "complexType[@name='includeType']//attribute[@name='optional']/@type",
                "xsd:boolean");
    assertXPath("Couldn't find the standard include child of the server element",
        "complexType[@name='includeType']//attribute[@name='location']/@type",
        "location");
    assertXPath("Couldn't find the location type",
                "simpleType[@name='location']/union/@memberTypes",
                "xsd:string variableType");
  }

  @Test
  public void checkNLSForType() throws XPathExpressionException {
    String description = messages.getString("config.desc");
    String label = messages.getString("config.name");
    
    assertXPath("The description is not present", 
                "complexType[@name='test']/annotation/documentation", 
                description);
    assertXPath("The description is not present", 
                "complexType[@name='test']/annotation/appinfo/label", 
                label);
  }

  @Test
  public void checkOptionalDuration() throws XPathExpressionException {
    assertOptionalDuration("duration", "duration");
  
    String description = messages.getString("config.monitorInterval.desc");
    String label = messages.getString("config.monitorInterval.name");
    assertXPath("The description is not correct.", 
        "complexType[@name='test']/attribute[@name='duration']/annotation/documentation", 
        description + "Specify a positive integer followed by a unit of time, which can be hours (h), minutes (m), seconds (s), or milliseconds (ms). For example, specify 500 milliseconds as 500ms. You can include multiple values in a single entry. For example, 1s500ms is equivalent to 1.5 seconds.");
    assertXPath("The label is not correct.", 
        "complexType[@name='test']/attribute[@name='duration']/annotation/appinfo/label", 
        label);
  }

  @Test
  public void checkOptionalDurationSeconds() throws XPathExpressionException {
    assertOptionalDuration("duration_s", "secondDuration");
  }

  @Test
  public void checkOptionalDurationMinutes() throws XPathExpressionException {
    assertOptionalDuration("duration_m", "minuteDuration");
  }

  @Test
  public void checkOptionalDurationHours() throws XPathExpressionException {
    assertOptionalDuration("duration_h", "hourDuration");
  }

  @Test
  public void checkOptionalDurationMillis() throws XPathExpressionException {
    assertOptionalDuration("duration_ms", "duration");
  }

  @Test
  public void checkVariable() throws XPathExpressionException {
    assertXPath("The variable element should be set for the onError attribute of the config element.",
                "complexType[@name='test']/attribute[@name='onError']/annotation/appinfo/variable",
                "onError");
    assertXPath("A variable has been unexpectidly set for the monitorInterval attribute of the config element.", 
        "complexType[@name='test']/attribute[@name='monitorInterval']/annotation/appinfo/variable", 
        null);
  }

  @Test
  public void checkInternalAttributesAreNotInSchema()
      throws XPathExpressionException {
        assertXPath("The config.target attribute escaped into the schema", 
            "complexType[@name='test']/attribute[@name='config.target']", 
            null);
      }

  @Test
  public void checkLocationType() throws XPathExpressionException {
    assertXPath("The location attribute is not generated as a location",
                "complexType[@name='test']/attribute[@name='location']/@type",
                "location");
  }

  @Test
  public void checkFileLocationType() throws XPathExpressionException {
    assertXPath("The locationFile attribute is not generated as a file location",
                "complexType[@name='test']/attribute[@name='locationFile']/@type",
                "fileLocation");
  }

  @Test
  public void checkDirLocationType() throws XPathExpressionException {
    assertXPath("The locationDir attribute is not generated as a dir location",
                "complexType[@name='test']/attribute[@name='locationDir']/@type",
                "dirLocation");
  }

  @Test
  public void checkURLLocationType() throws XPathExpressionException {
    assertXPath("The locationURL attribute is not generated as a url location",
                "complexType[@name='test']/attribute[@name='locationURL']/@type",
                "urlLocation");
  }

  @Test
  public void checkPasswordType() throws XPathExpressionException {
    assertXPath("The password attribute is not generated as a password",
                "complexType[@name='test']/attribute[@name='password']/@type",
                "password");
  }

  @Test
  public void checkPasswordHashType() throws XPathExpressionException {
    assertXPath("The passwordHash attribute is not generated as a password hash",
                "complexType[@name='test']/attribute[@name='passwordHash']/@type",
                "passwordHash");
  }

  @Test
  public void checkPidType() throws XPathExpressionException {
    assertXPath("The pid element has not been generated as a child of type other",
        "complexType[@name='test']//element[@name='pid']/@type",
        "other");
  
    assertXPath("A pidRef attribute should have been generated of type pidType.",
                "complexType[@name='test']/attribute[@name='pidRef']/@type",
                "pidType");
    assertXPath("A pidRef attribute should have been generated of type pidType.",
                "complexType[@name='test']/attribute[@name='pidRef']/annotation/appinfo/reference",
                "other");
  
    // Checks that a reference to an OCD that doesn't have ibm:alias doesn't end up with a ref but is an element.
  
    assertXPath("The noRefPid element has not been generated as a child of type other",
        "complexType[@name='test']//element[@name='noRefPid']/@type",
        "notTopLevel");
  
    assertXPath("A noRefPidRef attribute should not have been generated of type pidType.",
                "complexType[@name='test']/attribute[@name='noRefPid']",
                null);
  }

  @Test
  public void checkUnqiue() throws XPathExpressionException {
    assertXPath("A attribute with a unique appinfo should have been generated.",
        "complexType[@name='test']/attribute[@name='unique']/annotation/appinfo/unique",
        "jndiName");
  }

  @Test
  public void checkRequired() throws XPathExpressionException {
    assertXPath("The attribute should be required.",
        "complexType[@name='test']/attribute[@name='required']/@use",
        "required");
  }

  @Test
  public void checkCardinality() throws XPathExpressionException {
    assertXPath("An attribute with cardinality greater than one should be an element.",
        "complexType[@name='test']//element[@name='cardinality']/@type",
        "xsd:string");
  }

  @Test
  public void checkInt() throws XPathExpressionException {
    assertXPath("An int attribute should be of type intType",
        "complexType[@name='test']/attribute[@name='int']/@type",
        "intType");
    assertXPath("intType shoudl be a union of xsd:int and variableType",
        "simpleType[@name='intType']/union/@memberTypes",
        "xsd:int variableType");
  }

  // @Test added in subclasses
  public void testSchema11Any() throws XPathExpressionException {
    assertXPath("There should be an xsd:any with an attribute of processContent='skip'",
    	        "complexType[@name='testAny']/choice/any/@processContents",
                "skip");
    assertXPath("There should be an xsd:any with an attribute of maxOccurs='1'",
                "complexType[@name='testAny']/choice/any/@maxOccurs",
                "1");
    assertXPath("There should be a pid element", "complexType[@name='testAny']/choice/element[@name='pid']/@type", "other");
  }

  @Test
  public void checkAnyNoChildrenType() throws XPathExpressionException {
	assertXPath("There should be an xsd:any with an attribute of processContent='skip'",
	            "complexType[@name='testAnyNoChildren']/sequence/any/@processContents",
	            "skip");
	assertXPath("There should be an xsd:any with an attribute of maxOccurs='1'",
                "complexType[@name='testAnyNoChildren']/sequence/any/@maxOccurs",
                "1");
  }

  @Test
  public void checkTokenType() throws XPathExpressionException {
    assertXPath("The attribute should of type tokenType",
        "complexType[@name='test']/attribute[@name='token']/@type",
        "tokenType");
    assertXPath("The attribute should of type tokenType",
        "simpleType[@name='tokenType']/union/@memberTypes",
        "xsd:token variableType");
  }

}
