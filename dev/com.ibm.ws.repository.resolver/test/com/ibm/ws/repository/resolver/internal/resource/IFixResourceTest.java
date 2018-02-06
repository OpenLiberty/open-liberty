/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.repository.resolver.internal.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;
import com.ibm.ws.product.utility.extension.ifix.xml.Offering;
import com.ibm.ws.product.utility.extension.ifix.xml.UpdatedFile;
import com.ibm.ws.repository.resolver.internal.namespace.InstallableEntityIdentityConstants;
import com.ibm.ws.repository.resolver.internal.namespace.ProductNamespace;
import com.ibm.ws.repository.resolver.internal.resource.IFixResource;
import com.ibm.ws.repository.resolver.internal.resource.RequirementImpl;
import com.ibm.ws.repository.resources.internal.IfixResourceImpl;

/**
 * Tests for {@link IFixResource}
 */
public class IFixResourceTest {

    private static final Set<String> aparIds = new HashSet<String>();

    /**
     * Sets a couple of apar IDs in the {@link #aparIds}, runs before every test so tests are free to change the collection.
     */
    @Before
    public void setupAparIds() {
        aparIds.clear();
        aparIds.add("PM00001");
        aparIds.add("PM00002");
    }

    /**
     * Test that an instance of {@link IFixResource} returns the
     * correct requirements on the product
     */
    @Test
    public void testRequirementsOnInstalledIFix() {
        Resource resource = IFixResource.createInstance(createIFixInfo());
        List<Requirement> requirements = resource.getRequirements(null);
        assertEquals("There should be a single requirement on the product", 1,
                     requirements.size());
        assertEquals(
                     "The requirements should be on the version of the product",
                     "(&(version>=8.5.5.0)(!(version>=8.5.5.1)))",
                     requirements.get(0).getDirectives()
                                     .get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
        assertEquals("The name should match the version range", "[8.5.5.0,8.5.5.1)", ((RequirementImpl) requirements.get(0)).getName());
    }

    /**
     * Test that an instance of {@link IFixResource} provides the
     * capabilities of the APARs it fixes
     */
    @Test
    public void testCapabilitiesOnInstalledIFix() {
        Resource resource = IFixResource.createInstance(createIFixInfo());
        testCapabilities(resource);
    }

    /**
     * Test to make sure that iFixes are sorted suitably
     */
    @Test
    public void testSorting() {
        // Create three iFixes, they should be sorted from latest to newest, all done on the updates element in the iFixInfo
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        UpdatedFile recentFile = new UpdatedFile(null, 0, dateFormat.format(new Date(900000000)), null);
        IFixInfo recentIFixInfo = createIFixInfoWithUpdates(Collections.singleton(recentFile), "recent");
        IFixResource recentIFixResource = IFixResource.createInstance(recentIFixInfo);

        // For the medium IFix info set two files, one with an old update date and one with a fairly recent one - it should pick the latest
        UpdatedFile mediumFile = new UpdatedFile(null, 0, dateFormat.format(new Date(100000000)), null);
        UpdatedFile reallyOldFile = new UpdatedFile(null, 0, dateFormat.format(new Date(700000000)), null);
        Set<UpdatedFile> mediumFiles = new HashSet<UpdatedFile>();
        mediumFiles.add(reallyOldFile);
        mediumFiles.add(mediumFile);
        IFixInfo mediumIFixInfo = createIFixInfoWithUpdates(mediumFiles, "medium");
        IFixResource mediumIFixResource = IFixResource.createInstance(mediumIFixInfo);

        UpdatedFile oldFile = new UpdatedFile(null, 0, dateFormat.format(new Date(400000000)), null);
        IFixInfo oldIFixInfo = createIFixInfoWithUpdates(Collections.singleton(oldFile), "old");
        IFixResource oldIFixResource = IFixResource.createInstance(oldIFixInfo);

        // Now add them to a sorted list
        List<IFixResource> iFixResourceList = new ArrayList<IFixResource>();
        iFixResourceList.add(mediumIFixResource);
        iFixResourceList.add(recentIFixResource);
        iFixResourceList.add(oldIFixResource);

        Collections.sort(iFixResourceList);

        assertEquals("The list should have been sorted so the most recent ifix is first", recentIFixResource, iFixResourceList.get(0));
        assertEquals("The list should have been sorted so the medium ifxis is second", mediumIFixResource, iFixResourceList.get(1));
        assertEquals("The list should have been sorted so the oldest ifix is last", oldIFixResource, iFixResourceList.get(2));
    }

    /**
     * This tests that an iFix created from an {@link IfixResourceImpl} will have a requirement on the product it applies to.
     * 
     * @throws InvalidSyntaxException
     */
    @Test
    public void testRequirementsOnMassiveIFix() throws InvalidSyntaxException {
        IfixResourceImpl iFixMassiveResource = new IfixResourceImpl(null);
        String productId = "foo";
        iFixMassiveResource.setAppliesTo(productId);
        iFixMassiveResource.setProvideFix(Collections.singleton("PM12345"));
        IFixResource testObject = IFixResource.createInstance(iFixMassiveResource);
        List<Requirement> requirements = testObject.getRequirements(null);

        assertEquals("There should be one requirement on the product", 1, requirements.size());
        RequirementImpl requirement = (RequirementImpl) requirements.get(0);
        assertEquals("The requirement should be for the product namespace", ProductNamespace.PRODUCT_NAMESPACE, requirement.getNamespace());
        assertEquals("The name should match the applies to", productId, requirement.getName());

        // Make sure the filter works
        String filterString = requirement.getDirectives().get(ProductNamespace.REQUIREMENT_FILTER_DIRECTIVE);
        Filter filter = FrameworkUtil.createFilter(filterString);
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, productId);
        assertTrue("The filter should match a valid product", filter.matches(attributes));

        attributes.put(ProductNamespace.CAPABILITY_PRODUCT_ID_ATTRIBUTE, "BAR");
        assertFalse("The filter should not match an invalid product", filter.matches(attributes));
    }

    /**
     * This tests that an iFix created from an {@link IfixResourceImpl} will have a capability for each of the fixes it fixes.
     */
    @Test
    public void testCapabilitiesOnMassiveIFix() {
        IfixResourceImpl massiveIfix = new IfixResourceImpl(null);
        String productId = "foo";
        massiveIfix.setAppliesTo(productId);
        massiveIfix.setProvideFix(aparIds);
        IFixResource testObject = IFixResource.createInstance(massiveIfix);
        testCapabilities(testObject);
    }

    /**
     * Test for {@link IFixResource#convertVersion(org.osgi.framework.Version)}.
     */
    @Test
    public void testConvertVersion() {
        Version converted = IFixResource.convertVersion(new Version("1.0.3005"));
        assertEquals("The 3 part version should have converted to a four part one", new Version("1.0.3.5"), converted);

        // Try it with 0 as the fourth digit
        converted = IFixResource.convertVersion(new Version("1.0.3000"));
        assertEquals("The 3 part version should have converted to a four part one", new Version("1.0.3.0"), converted);
    }

    /**
     * Util method to make sure that there is a capability for each of the {@link #aparIds}
     * 
     * @param resource
     */
    private void testCapabilities(Resource resource) {
        List<Capability> capabilities = resource.getCapabilities(null);
        assertEquals("There should be one capability for each APAR",
                     aparIds.size(), capabilities.size());

        for (Capability capability : capabilities) {
            // Remove to guard against duplicates
            String capabilitySymbolicName = (String) capability.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
            assertTrue(
                       "The ID of the APAR in this capability is not one of the apars fixed by this iFix: " + capabilitySymbolicName,
                       aparIds.remove(capabilitySymbolicName));
            String type = (String) capability.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
            assertEquals("The type of the capability should be iFix", InstallableEntityIdentityConstants.TYPE_IFIX, type);
        }
    }

    /**
     * This method will create an ifix info with the supplied updates object and a single requirement with the given name
     * 
     * @param set
     * @param name
     * @return
     */
    private IFixInfo createIFixInfoWithUpdates(Set<UpdatedFile> updatedFiles, String name) {
        return new IFixInfo(null, null, Collections.singleton(name), null, null, null, updatedFiles);
    }

    /**
     * This method will create an iFix info object with a couple of APARs (as
     * defined in {@link #aparIds}) being fixed and applicable to version
     * 8.5.5000.
     * 
     * @return The IFixInfo ready for tests
     */
    private IFixInfo createIFixInfo() {
        final ArrayList<Offering> offerings = new ArrayList<Offering>();
        Offering offering = new Offering();
        offering.setTolerance("[8.5.5000,8.5.5001)");
        offerings.add(offering);

        return new IFixInfo(null, null, aparIds, null, offerings, null, null);
    }

}
