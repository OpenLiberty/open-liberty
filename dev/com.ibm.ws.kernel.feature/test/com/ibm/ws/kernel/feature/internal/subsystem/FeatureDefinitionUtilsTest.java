/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.subsystem;

import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;
import test.utils.SharedConstants;

import com.ibm.ws.kernel.feature.AppForceRestart;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ImmutableAttributes;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ProvisioningDetails;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.ws.kernel.provisioning.VersionUtility;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * 
 */
public class FeatureDefinitionUtilsTest {

    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=audit=enabled:featureManager=all=enabled");
    static WsLocationAdmin locSvc;

    @Rule
    public TestRule outputRule = outputMgr;

    @Rule
    public TestName testName = new TestName();

    @Test
    public void testSimpleReadManifestWriteCache() throws IOException {
        File featureFile = new File(SharedConstants.TEST_DATA_DIR, "com.ibm.websphere.supersededA.mf");

        ProvisioningDetails details = new ProvisioningDetails(featureFile, null);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", featureFile, details);

        // --- This is the test feature (reading from a file) ---
        // Manifest-Version: 1.0
        // Subsystem-SymbolicName: com.ibm.websphere.supersededA;visibility:=public; superseded=true; superseded-by="appSecurity-2.0,[servlet-3.0]"
        // Subsystem-Version: 1.0.0
        // Subsystem-Type: osgi.subsystem.feature
        // Subsystem-Content: notexistA;location:="lib/notexistA"
        // IBM-Feature-Version: 2

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);

        FeatureDefinitionUtils.writeAttributes(iAttr, details, writer);
        writer.flush();
        String result = out.toString();

        // -- Result string should look something like this: 
        // com.ibm.websphere.supersededA=../com.ibm.ws.kernel.feature_test/build/unittest/test data/com.ibm.websphere.supersededA.mf;1364156286000;300;;;2;PUBLIC;NEVER;1.0.0;0000

        String[] lines = result.split(FeatureDefinitionUtils.NL);

        Assert.assertEquals("Output should be only one line: " + result, 1, lines.length);
        Assert.assertFalse("Output should not contain AutoFeature indicator: " + result, result.contains("-C:"));
        Assert.assertFalse("Output should not contain API Services indicator: " + result, result.contains("-V:"));
        Assert.assertFalse("Output should not contain API Packages indicator: " + result, result.contains("-A:"));
        Assert.assertFalse("Output should not contain SPI Packages indicator: " + result, result.contains("-S:"));

        int pos = result.indexOf('=');
        Assert.assertTrue("Output should contain an =, result: " + result, pos > 0);
        Assert.assertEquals("String before the = should be the symbolic name", iAttr.symbolicName, result.substring(0, pos));

        String[] parts = FeatureDefinitionUtils.splitPattern.split(result.substring(pos + 1).trim());
        Assert.assertEquals("There should be 10 parts in the value", 10, parts.length);
        Assert.assertEquals("parts[0] should contain the absolute file path", featureFile.getAbsolutePath(), parts[0]);

        // Test parse long: lastModified and length
        Assert.assertEquals("parts[1]  should contain the file modified time", featureFile.lastModified(), FeatureDefinitionUtils.getLongValue(parts[1], -1));
        Assert.assertEquals("parts[2]  should contain the file length", featureFile.length(), FeatureDefinitionUtils.getLongValue(parts[2], -1));

        Assert.assertTrue("parts[3] should be empty (no short name)", parts[3].isEmpty());

        Assert.assertEquals("parts[4]  should contain the feature version", 2, FeatureDefinitionUtils.getIntegerValue(parts[4], 0));
        Assert.assertSame("parts[5] should contain the visibility string", Visibility.PUBLIC, Visibility.fromString(parts[5]));
        Assert.assertSame("parts[6] should contain the app restart header value", AppForceRestart.NEVER, AppForceRestart.fromString(parts[6]));
        Assert.assertSame("parts[7] should resolve to the shared version constant", VersionUtility.VERSION_1_0, VersionUtility.stringToVersion(parts[7]));
        Assert.assertEquals("parts[8] should contain empty flags (no API/SPI, not autofeature)", "00000", parts[8]);

        // Check for superseded
        Assert.assertTrue("Test for superseded should return true", details.isSuperseded());
        Assert.assertEquals("Test for superseded-by should return value from header", "appSecurity-2.0,[servlet-3.0]", details.getSupersededBy());

        // Check subsystem content
        Assert.assertEquals("Subsystem-Content (all) should contain one element", 1, details.getConstituents(null).size());
        Assert.assertEquals("Subsystem-Content (bundle) should contain one element", 1, details.getConstituents(SubsystemContentType.BUNDLE_TYPE).size());
        Assert.assertTrue("Subsystem-Content (boot.jar) should return no elements", details.getConstituents(SubsystemContentType.BOOT_JAR_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (jar) should contain one element", details.getConstituents(SubsystemContentType.JAR_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (file) should contain one element", details.getConstituents(SubsystemContentType.FILE_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (feature) should contain one element", details.getConstituents(SubsystemContentType.FEATURE_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (unknown) should contain one element", details.getConstituents(SubsystemContentType.UNKNOWN).isEmpty());
    }

    @Test(expected = FeatureManifestException.class)
    public void testMissingSymbolicName() throws IOException {
        // TODO: ADD EXPECTATION FOR ERROR MESSAGE CODE
        // outputMgr.expectError("CWWKFIXME");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        FeatureDefinitionUtils.loadAttributes("", null, details);
    }

    @Test(expected = FeatureManifestException.class)
    public void testInvalidSubsystemType() throws IOException {
        // TODO: ADD EXPECTATION FOR ERROR MESSAGE CODE
        // outputMgr.expectError("CWWKFIXME");

        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.dummy;visibility:=protected \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        FeatureDefinitionUtils.loadAttributes("", null, details);
    }

    @Test(expected = FeatureManifestException.class)
    public void testMissingSubsystemVersion() throws IOException {
        // TODO: ADD EXPECTATION FOR ERROR MESSAGE CODE
        // outputMgr.expectError("CWWKFIXME");

        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.dummy;visibility:=protected \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        FeatureDefinitionUtils.loadAttributes("", null, details);
    }

    @Test
    public void testDownlevelFeatureVersion() throws IOException {
        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.dummy;visibility:=protected \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 1 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", null, details);
        Assert.assertFalse("Attributes should indicate the feature version is not supported", iAttr.isSupportedFeatureVersion());
    }

    @Test
    public void testUnspecifiedFeatureVersion() throws IOException {
        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.dummy;visibility:=protected \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", null, details);
        Assert.assertEquals("IBM feature version should be 0 (unspecified)", 0, iAttr.featureVersion);
        Assert.assertTrue("Feature version should be supported", iAttr.isSupportedFeatureVersion());
    }

    @Test(expected = FeatureManifestException.class)
    public void testUnsupportedFeatureVersion() throws IOException {
        // We expect this error code to be in the output for unsupported feature version
        outputMgr.expectError("CWWKF0022E");

        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.dummy;visibility:=protected \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 3 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        FeatureDefinitionUtils.loadAttributes("", null, details);
    }

    @Test
    public void testIBMShortNameUnspecified() throws Exception {
        // IBM-ShortName not present
        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.good \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", null, details);

        Assert.assertEquals("symbolicName should return com.ibm.websphere.good", "com.ibm.websphere.good", iAttr.symbolicName);
        Assert.assertNull("shortName should be null: " + iAttr.shortName, iAttr.shortName);
        Assert.assertEquals("featureName should return symbolicName", iAttr.symbolicName, iAttr.featureName);
    }

    @Test
    public void testIBMShortNamePublic() throws Exception {
        // IBM-ShortName in mf file of a public feature
        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("IBM-ShortName: public-1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.public-1.0; visibility:=public \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", null, details);

        Assert.assertEquals("symbolicName should return com.ibm.websphere.public-1.0", "com.ibm.websphere.public-1.0", iAttr.symbolicName);
        Assert.assertEquals("shortName should return public-1.0", "public-1.0", iAttr.shortName);
        Assert.assertEquals("featureName should return shortName", iAttr.shortName, iAttr.featureName);
    }

    @Test
    public void testIBMShortNameHidden() throws Exception {
        // IBM-ShortName in mf file but it should be ignored because feature is not public
        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("IBM-ShortName: public-1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.not.public-1.0 \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", null, details);

        Assert.assertEquals("symbolicName should return com.ibm.websphere.public-1.0", "com.ibm.websphere.not.public-1.0", iAttr.symbolicName);
        Assert.assertNull("shortName should be null", iAttr.shortName);
        Assert.assertEquals("featureName should return symbolicName", iAttr.symbolicName, iAttr.featureName);
    }

    @Test
    public void testMismatchedSupersededFalse() throws IOException {
        // We expect this error code to be in the output for mismatched superseded flags
        outputMgr.expectError("CWWKF0020E");

        // Intentional extra trailing whitespace... 
        // Superseded is specified false, but we say it is superseded by something... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.supersededA;visibility:=public; superseded=false; superseded-by=\"appSecurity-2.0\" \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        FeatureDefinitionUtils.loadAttributes("", null, details);
        assertFalse("The method isSuperseded() should return false (ignored)", details.isSuperseded());
    }

    @Test
    public void testImmutableAttributeFlags() throws Exception {
        // Round-trip through the cache will convert to absolute path. We need the input file to 
        // have also been constructed from an absolute path so it will match.. 
        File featureFile = new File(SharedConstants.TEST_DATA_DIR, "com.ibm.websphere.supersededA.mf").getAbsoluteFile();

        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.supersededA; visibility:=protected \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.write("IBM-App-ForceRestart: install \n");
        writer.write("IBM-API-Package: javax.servlet.annotation;  type=\"spec\", \n");
        writer.write(" com.ibm.wsspi.webcontainer;  type=\"internal\" \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("custom", featureFile, details);
        Assert.assertFalse("AutoFeature flag should be false", iAttr.isAutoFeature);
        Assert.assertFalse("API-service flag should be false", iAttr.hasApiServices);
        Assert.assertTrue("API-package flag should be true", iAttr.hasApiPackages);
        Assert.assertFalse("SPI-package flag should be false", iAttr.hasSpiPackages);

        // Do nothing that would trigger more detailed fluff up of API/SPI here.. 
        // Want to make sure it gets into the cache anyway.. 

        // Reset the output buffer, let's flatten it for the cache.. 
        out.reset();
        writer = new PrintWriter(out, true);
        FeatureDefinitionUtils.writeAttributes(iAttr, details, writer);
        writer.flush();
        String result = out.toString();
        System.out.println(result);

        // So, now we have our shiny cache line, which should actually be two lines, kind of like this: 
        // com.ibm.websphere.supersededA=;xxxx;xxxx;custom;;2;PROTECTED;INSTALL;1.0.0;0010
        // -A:javax.servlet.annotation;  type="spec",javax.servlet.descriptor; type="spec",com.ibm.websphere.servlet.session;  type="ibm-api",com.ibm.wsspi.servlet.session;  type="ibm-api",com.ibm.wsspi.webcontainer;  type="internal"

        String[] lines = result.split(FeatureDefinitionUtils.NL);
        Assert.assertEquals("Output should be two lines:\n" + result, 2, lines.length);
        Assert.assertTrue("lines[0] should start with custom:\n" + lines[0], lines[0].startsWith("custom:"));
        Assert.assertTrue("lines[0] should end with ;;2;PROTECTED;INSTALL;1.0.0;00100;SERVER\n" + lines[0], lines[0].endsWith(";;2;PROTECTED;INSTALL;1.0.0;00100;SERVER"));
        Assert.assertTrue("lines[1] SHOULD startsWith API Packages indicator:\n" + lines[1], lines[1].startsWith("-A:"));

        // Now, let's read those lines back in, and make sure the resulting attributes survived
        ImmutableAttributes iAttr2 = FeatureDefinitionUtils.loadAttributes(lines[0], null);
        Assert.assertNotNull("Should return non-null attribtues", iAttr2);
        assertAttributesEqual(ImmutableAttributes.class, iAttr, iAttr2);
        Assert.assertEquals("Equals should show these as the same", iAttr, iAttr2);
        Assert.assertEquals("hashCode should be the same", iAttr.hashCode(), iAttr2.hashCode());

        // Let's just mess around with the flags now.. 
        String newLine = lines[0].replace("0010", "1101");
        System.out.println("Updated line: \n\t" + newLine);
        iAttr2 = FeatureDefinitionUtils.loadAttributes(newLine, null);
        Assert.assertNotNull("Should return non-null attribtues", iAttr2);
        // Equals/hashCode are only focused on symbolic name & version: these should still be equal.. 
        Assert.assertEquals("Equals should show these as the same", iAttr, iAttr2);
        Assert.assertEquals("hashCode should be the same", iAttr.hashCode(), iAttr2.hashCode());
        Assert.assertTrue("AutoFeature flag should be true", iAttr2.isAutoFeature);
        Assert.assertTrue("API-service flag should be true", iAttr2.hasApiServices);
        Assert.assertFalse("API-package flag should be false", iAttr2.hasApiPackages);
        Assert.assertTrue("SPI-package flag should be true", iAttr2.hasSpiPackages);
    }

    @Test
    public void testProvisioningDetailsExtraLines() throws Exception {
        // Round-trip through the cache will convert to absolute path. We need the input file to 
        // have also been constructed from an absolute path so it will match.. 
        File featureFile = new File(SharedConstants.TEST_DATA_DIR, "com.ibm.websphere.supersededA.mf").getAbsoluteFile();

        // Intentional extra trailing whitespace... 
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.supersededA; visibility:=public; singleton:=true \n");
        writer.write("IBM-ShortName: shortName\n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.1 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.write("IBM-App-ForceRestart: uninstall \n");
        // One of each magic header with extra lines .. we know the flags work from the other test, but
        // but need to verify the combined parse..
        writer.write("IBM-API-Package: javax.servlet.annotation;  type=\"spec\" \n");
        writer.write("IBM-SPI-Package: javax.servlet.descriptor; type=\"spec\" \n");
        writer.write("IBM-API-Service: org.apache.aries.blueprint.NamespaceHandler; service=service; service.ranking=0; osgi.service.blueprint.namespace=\"http://www.osgi.org/xmlns/blueprint/v1.0\" \n");
        writer.write("IBM-Provision-Capability: osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.blueprint-1.0))\", \n");
        writer.write(" osgi.identity; filter:=\"(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jpa-2.0))\" \n");
        writer.flush();

        // Lame: round-trip through to the cache lines we need: 
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("custom", featureFile, details);
        out.reset();
        writer = new PrintWriter(out, true);
        FeatureDefinitionUtils.writeAttributes(iAttr, details, writer);
        writer.flush();

        // Here it is (should we fail later..)
        String result = out.toString();
        System.out.println(result);

        String[] lines = result.split(FeatureDefinitionUtils.NL);
        Assert.assertEquals("Output should be 5 lines:\n" + result, 5, lines.length);
        Assert.assertTrue("lines[0] should end with ;shortName;2;PUBLIC;UNINSTALL;1.0.1;11111;SERVER\n" + lines[0],
                          lines[0].endsWith(";shortName;2;PUBLIC;UNINSTALL;1.0.1;11111;SERVER"));
        Assert.assertTrue("lines[1] SHOULD startsWith AutoFeature indicator:\n" + lines[1], lines[1].startsWith("-C:"));
        Assert.assertTrue("lines[2] SHOULD startsWith API Services indicator:\n" + lines[2], lines[2].startsWith("-V:"));
        Assert.assertTrue("lines[3] SHOULD startsWith API Packages indicator:\n" + lines[3], lines[3].startsWith("-A:"));
        Assert.assertTrue("lines[4] SHOULD startsWith SPI Packages indicator:\n" + lines[4], lines[4].startsWith("-S:"));

        // So, we know the cache line(s) came out right-ish. Now, what happens when we try to read them in?
        in = new ByteArrayInputStream(out.toByteArray());
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        // peel off the first line --> immutable attributes
        String readLine = reader.readLine();
        ImmutableAttributes newAttr = FeatureDefinitionUtils.loadAttributes(readLine, null);
        assertAttributesEqual(ImmutableAttributes.class, iAttr, newAttr);

        // Now, based on our attribute flags.. pull off the extra lines.. 
        ProvisioningDetails details2 = new ProvisioningDetails(reader, newAttr);
        // Some fields should be equal... 
        assertAttributesEqual(ProvisioningDetails.class, details, details2,
                              "apiPackages", "apiServices", "autoFeatureCapability", "spiPackages");
        // MOST fields should be null because we avoided touching them. 
        assertAttributesNull(ProvisioningDetails.class, details2, false,
                             "apiPackages", "apiServices", "autoFeatureCapability", "spiPackages", "iAttr");
    }

    @Test
    public void testInstallIsValidVisibilityDirective() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out, true);
        writer.write("Manifest-Version: 1.0 \n");
        writer.write("Subsystem-SymbolicName: com.ibm.websphere.install-1.0; visibility:=install \n");
        writer.write("Subsystem-Type: osgi.subsystem.feature \n");
        writer.write("Subsystem-Version: 1.0.0 \n");
        writer.write("IBM-Feature-Version: 2 \n");
        writer.flush();

        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        ProvisioningDetails details = new ProvisioningDetails(null, in);
        ImmutableAttributes iAttr = FeatureDefinitionUtils.loadAttributes("", null, details);

        Assert.assertEquals("symbolicName should return com.ibm.websphere.install-1.0", "com.ibm.websphere.install-1.0", iAttr.symbolicName);
        Assert.assertEquals("Visibility should return INSTALL", Visibility.INSTALL, iAttr.visibility);
    }

    void assertAttributesEqual(Class<?> c, Object o1, Object o2, String... fieldNames) throws Exception {
        List<Field> fields;
        if (fieldNames.length == 0) {
            fields = Arrays.asList(c.getDeclaredFields());
        } else {
            fields = new ArrayList<Field>();
            for (String name : fieldNames) {
                fields.add(c.getDeclaredField(name));
            }
        }

        for (Field f : fields) {
            f.setAccessible(true);
            Assert.assertEquals("Field " + f.getName() + " should be equal between the two objects", f.get(o1), f.get(o2));
        }
    }

    void assertAttributesNull(Class<?> c, Object o, boolean specifiedFieldsAreNull, String... fieldNames) throws Exception {
        List<Field> fields;
        if (fieldNames.length == 0 || !specifiedFieldsAreNull) {
            fields = Arrays.asList(c.getDeclaredFields());
        } else {
            fields = new ArrayList<Field>();
            for (String name : fieldNames) {
                fields.add(c.getDeclaredField(name));
            }
        }

        List<String> names = Arrays.asList(fieldNames);

        for (Field f : fields) {
            // If the names specified in the parameter list are the ones that should not be null, 
            // and this field is one of those.. skip it.
            if (!specifiedFieldsAreNull && names.contains(f.getName()))
                continue;

            // skip basic types, statics, and finals.
            // --> this is obviously not an exhaustive list.
            // types may have to be added when there are fields of those types.. 
            Class<?> type = f.getType();
            int modifiers = f.getModifiers();
            if (type == int.class || type == boolean.class
                || Modifier.isFinal(modifiers)
                || Modifier.isStatic(modifiers))
                continue;

            f.setAccessible(true);
            Assert.assertNull("Field " + f.getName() + " should have a null value", f.get(o));
        }
    }
}
