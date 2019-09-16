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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ImmutableAttributes;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureDefinitionUtils.ProvisioningDetails;
import com.ibm.ws.kernel.feature.provisioning.SubsystemContentType;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

import junit.framework.Assert;
import test.common.SharedOutputManager;
import test.utils.SharedConstants;

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
    public void testSimpleReadManifestWriteCache() throws Exception {
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
        DataOutputStream writer = new DataOutputStream(out);

        FeatureRepository.writeFeatureAttributes(iAttr, details, writer);
        writer.flush();

        DataInputStream in = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        ImmutableAttributes iAttr2 = FeatureRepository.loadFeatureAttributes(in);
        ProvisioningDetails details2 = FeatureRepository.loadProvisioningDetails(in, iAttr2);

        assertAttributesEqual(ImmutableAttributes.class, iAttr, iAttr2);

        // Check subsystem content
        Assert.assertEquals("Subsystem-Content (all) should contain one element", 1, details2.getConstituents(null).size());
        Assert.assertEquals("Subsystem-Content (bundle) should contain one element", 1, details2.getConstituents(SubsystemContentType.BUNDLE_TYPE).size());
        Assert.assertTrue("Subsystem-Content (boot.jar) should return no elements", details2.getConstituents(SubsystemContentType.BOOT_JAR_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (jar) should contain one element", details2.getConstituents(SubsystemContentType.JAR_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (file) should contain one element", details2.getConstituents(SubsystemContentType.FILE_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (feature) should contain one element", details2.getConstituents(SubsystemContentType.FEATURE_TYPE).isEmpty());
        Assert.assertTrue("Subsystem-Content (unknown) should contain one element", details2.getConstituents(SubsystemContentType.UNKNOWN).isEmpty());
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
        DataOutputStream dataWriter = new DataOutputStream(out);
        FeatureRepository.writeFeatureAttributes(iAttr, details, dataWriter);
        writer.flush();

        // Now, let's read those lines back in, and make sure the resulting attributes survived
        DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(out.toByteArray()));
        ImmutableAttributes iAttr2 = FeatureRepository.loadFeatureAttributes(dataIn);
        Assert.assertNotNull("Should return non-null attribtues", iAttr2);
        assertAttributesEqual(ImmutableAttributes.class, iAttr, iAttr2);
        Assert.assertEquals("Equals should show these as the same", iAttr, iAttr2);
        Assert.assertEquals("hashCode should be the same", iAttr.hashCode(), iAttr2.hashCode());

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
        DataOutputStream dataWriter = new DataOutputStream(out);
        FeatureRepository.writeFeatureAttributes(iAttr, details, dataWriter);
        writer.flush();

        // Now, what happens when we try to read them in?
        in = new ByteArrayInputStream(out.toByteArray());
        DataInputStream dataReader = new DataInputStream(in);

        // peel off the first line --> immutable attributes
        ImmutableAttributes newAttr = FeatureRepository.loadFeatureAttributes(dataReader);
        assertAttributesEqual(ImmutableAttributes.class, iAttr, newAttr);

        // Now, based on our attribute flags.. pull off the extra lines..
        ProvisioningDetails details2 = FeatureRepository.loadProvisioningDetails(dataReader, newAttr);
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
