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

package com.ibm.ws.repository.test;

import static com.ibm.ws.lars.testutils.BasicChecks.checkCopyFields;
import static com.ibm.ws.lars.testutils.BasicChecks.populateResource;
import static com.ibm.ws.lars.testutils.BasicChecks.simpleUpload;
import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallAnyTypes;
import static com.ibm.ws.lars.testutils.matchers.ResourceByIdMatcher.hasId;
import static com.ibm.ws.repository.common.enums.State.PUBLISHED;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.FileRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.DownloadPolicy;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.ProductDefinition;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.connections.SimpleProductDefinition;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.EsaResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.MatchResult;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.WritableResourceFactory;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;
import com.ibm.ws.repository.strategies.writeable.UpdateInPlaceStrategy;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;

public class EsaResourceTest {

    @Rule
    public final RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryConnection repoConnection = fixture.getAdminConnection();

    // Used for multi-repo tests
    @Rule
    public final FileRepositoryFixture fixture2 = FileRepositoryFixture.createFixture(new File("repo2"));
    @Rule
    public final FileRepositoryFixture fixture3 = FileRepositoryFixture.createFixture(new File("repo3"));

    protected EsaResourceImpl createTestObject(RestRepositoryConnection loginInfo) throws IOException {
        return new EsaResourceImpl(repoConnection);
    }

    @Test
    public void testIsDownloadable() {
        assertEquals("ESAs should only be downloadable by installer",
                     DownloadPolicy.INSTALLER, new EsaResourceImpl(repoConnection).getDownloadPolicy());
    }

    /**
     * Test to make sure when you set a short name there is also a lower case one set
     */
    @Test
    public void testLowerCaseShortName() {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(repoConnection);
        String shortName = "ShortNameValue";
        esa.setShortName(shortName);
        assertEquals("The lower case version should have been set", shortName.toLowerCase(), esa.getLowerCaseShortName());
        esa.setShortName(null);
        assertNull("The lower case version should have been unset", esa.getLowerCaseShortName());
    }

    @Test
    public void testCopyFields() throws IllegalArgumentException, IllegalAccessException, InstantiationException, NoSuchMethodException, SecurityException, InvocationTargetException, IOException {
        checkCopyFields(new EsaResourceImpl(repoConnection), new EsaResourceImpl(repoConnection));
    }

    /**
     * Repo1 has 3 features, esa1, esa2 and esa3
     * Repo2 has 2 features, esa2 and esa4
     * Repo3 has 3 features, esa2, esa3 and esa4
     *
     * @throws NoRepoAvailableException
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws URISyntaxException
     */
    @Test
    public void testReadEsasFromMultipleRepositories() throws RepositoryResourceException, RepositoryBackendException, URISyntaxException {

        RepositoryConnection repo2 = fixture2.getWritableConnection();
        RepositoryConnection repo3 = fixture3.getWritableConnection();

        // Creates a collection with just repo2 in (for uploading)
        RepositoryConnectionList repo1Only = new RepositoryConnectionList(repoConnection);
        RepositoryConnectionList repo2Only = new RepositoryConnectionList(repo2);
        RepositoryConnectionList repo3Only = new RepositoryConnectionList(repo3);

        // Creates a collection with both repos in (for reading from). Get repo1 from _loginInfo
        RepositoryConnectionList allRepos = new RepositoryConnectionList(repoConnection);
        allRepos.add(repo2);
        allRepos.add(repo3);

        // Add an asset to repo1
        EsaResourceImpl esaRes1 = createEsaResource("esa1");
        esaRes1.setName("esa1");
        simpleUpload(esaRes1);

        // Add an asset to repo1
        EsaResourceImpl esaRes2 = createEsaResource("esa2");
        simpleUpload(esaRes2);

        // Add an asset to repo1
        EsaResourceImpl esaRes3 = createEsaResource("esa3");
        simpleUpload(esaRes3);

        // Add esa2 again to repo2.
        esaRes2.setRepositoryConnection(repo2);
        simpleUpload(esaRes2);

        // Add esa4 to repo 2
        EsaResourceImpl esaRes4 = createEsaResource("esa4");
        esaRes4.setRepositoryConnection(repo2);
        simpleUpload(esaRes4);

        // Add esa2 and esa3 to repo 3.
        esaRes2.setRepositoryConnection(repo3);
        simpleUpload(esaRes2);
        esaRes3.setRepositoryConnection(repo3);
        simpleUpload(esaRes3);

        // Create a new esa resource using the same provideFeature (instead of reusing the
        // same esa like we have done above). So Esa5
        EsaResourceImpl esaRes4dupe = createEsaResource("esa4");
        esaRes4dupe.setRepositoryConnection(repo3);
        simpleUpload(esaRes4dupe);

        // Should only get one asset from repo1
        assertEquals("Should be 3 assets in repo1", 3, repo1Only.getAllResources().size());

        // Should get one asset from repo2
        assertEquals("Should be 2 assets in repo2", 2, repo2Only.getAllResources().size());

        // Should get two assets from repo3
        assertEquals("Should be 3 assets in repo3", 3, repo3Only.getAllResources().size());

        // Should get two assets when using both repos
        assertEquals("Should get two assets when using both repos", 4, allRepos.getAllResources().size());
    }

    /**
     * This test sees if the {@link EsaResourceImpl#getMatchingEsas(String, RepositoryConnectionList)} method works.
     *
     * @throws URISyntaxException
     * @throws RepositoryBackendException
     * @throws RepositoryResourceException
     */
    @Test
    public void testFilteringEsas() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {
        // Add ESAs that would match on the 3 different attributes (and one that doesn't) and make sure we get back the right one when we filter by that attribute
        String filterString = "wibble";
        EsaResourceImpl bySymbolicName = createEsaResource(filterString);
        bySymbolicName.uploadToMassive(new UpdateInPlaceStrategy());

        EsaResourceImpl byShortName = createEsaResource("foo");
        byShortName.setShortName(filterString);
        byShortName.uploadToMassive(new UpdateInPlaceStrategy());

        EsaResourceImpl byLowerCaseShortName = createEsaResource("bar");
        String byLowerCaseFilterString = "wobble";
        byLowerCaseShortName.setShortName(byLowerCaseFilterString.toUpperCase());
        byLowerCaseShortName.uploadToMassive(new UpdateInPlaceStrategy());

        EsaResourceImpl noMatch = createEsaResource("baz");
        noMatch.uploadToMassive(new UpdateInPlaceStrategy());

        testMatch(FilterableAttribute.SYMBOLIC_NAME, filterString, bySymbolicName);
        testMatch(FilterableAttribute.SHORT_NAME, filterString, byShortName);
        testMatch(FilterableAttribute.LOWER_CASE_SHORT_NAME, byLowerCaseFilterString, byLowerCaseShortName);
    }

    @Test
    public void testMatches() throws Throwable {
        String appliesTo = "com.ibm.websphere.appserver; productVersion=8.5.5.1; productInstallType=Archive";
        EsaResourceImpl resource = new EsaResourceImpl(null);
        resource.setAppliesTo(appliesTo);
        reflectiveCallAnyTypes(resource, "updateGeneratedFields", new Class[] { boolean.class }, new Object[] { Boolean.TRUE });

        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        assertEquals("Matcher should have returned MATCHED", MatchResult.MATCHED, resource.matches(def));

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.2", "Archive", null, "BASE");
        assertEquals("Matcher should have returned INVALID_VERSION", MatchResult.INVALID_VERSION, resource.matches(def));

        def = new SimpleProductDefinition("wibble", "8.5.5.1", "Archive", null, "BASE");
        assertEquals("Matcher should have returned NOT_APPLICABLE", MatchResult.NOT_APPLICABLE, resource.matches(def));

        // Should match as our asset has no edition set
        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "Invalid");
        assertEquals("Matcher should have returned INVALID_EDITION", MatchResult.MATCHED, resource.matches(def));

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Invalid", null, "BASE");
        assertEquals("Matcher should have returned INVALID_INSTALL_TYPE", MatchResult.INVALID_INSTALL_TYPE, resource.matches(def));

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        assertEquals("Matcher should have returned MATCHED", MatchResult.MATCHED, resource.matches(def));

        appliesTo = "com.ibm.websphere.appserver; productVersion=2014.3.0.0; productInstallType=Archive";
        resource.setAppliesTo(appliesTo);
        reflectiveCallAnyTypes(resource, "updateGeneratedFields", new Class[] { boolean.class }, new Object[] { Boolean.TRUE });

        def = new SimpleProductDefinition("com.ibm.websphere.appserver", "2014.3.0.0", "Archive", null, "BASE");
        assertEquals("Matcher should have returned MATCHED", MatchResult.MATCHED, resource.matches(def));

    }

    @Test
    public void testGetAppliesToVersion() {
        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        EsaResourceImpl mr = new EsaResourceImpl(null);
        mr.setAppliesTo("com.ibm.websphere.appserver; productVersion=2013.13.13; productEditions=\"Core,BASE\",com.ibm.tinypinkelf; productVersion=8.5.5.0; productEditions=\"Base\"");
        assertEquals("Should have got version 2013.13.13 back", "2013.13.13", mr.getAppliesToVersions(Collections.singleton(def)));
    }

    @Test
    public void testGetAppliesToVersionWithNoEdition() {
        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        EsaResourceImpl mr = new EsaResourceImpl(null);
        mr.setAppliesTo("com.ibm.websphere.appserver; productVersion=2013.13.13,com.ibm.tinypinkelf; productVersion=8.5.5.0; productEditions=\"Base\"");
        assertEquals("Should have got version 2013.13.13 back", "2013.13.13", mr.getAppliesToVersions(Collections.singleton(def)));
    }

    @Test
    public void testGetAppliesToWithNoVersion() {
        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        EsaResourceImpl mr = new EsaResourceImpl(null);
        mr.setAppliesTo("com.ibm.websphere.appserver; productEditions=\"Core,BASE\",com.ibm.tinypinkelf; productVersion=8.5.5.0; productEditions=\"Base\"");
        assertNull("Should have got a null version back", mr.getAppliesToVersions(Collections.singleton(def)));
    }

    @Test
    public void testGetAppliesToWithNoEditionOrVersion() {
        ProductDefinition def = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        EsaResourceImpl mr = new EsaResourceImpl(null);
        mr.setAppliesTo("com.ibm.websphere.appserver,com.ibm.tinypinkelf; productVersion=8.5.5.0; productEditions=\"Base\"");
        assertNull("Should have got a null version back", mr.getAppliesToVersions(Collections.singleton(def)));
    }

    /**
     * Checks that the appliesToFilter isn't used and that two esas do match when they have matching appliesTo's.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithFakeAppliesTo() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResourceImpl esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\", anotherProduct");
        Asset ass = (Asset) reflectiveCallAnyTypes(esa1, "getAsset", null, null);
        AppliesToFilterInfo atfi = new AppliesToFilterInfo();
        atfi.setEditions(Collections.singletonList("Random non matching edition"));
        ass.getWlpInformation().setAppliesToFilterInfo(Collections.singleton(atfi));

        EsaResourceImpl esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\", anotherProduct");

        assertEquals("Matching data should match", esa1.createMatchingData(), esa2.createMatchingData());
    }

    /**
     * We'll ceate an appliesToFilter that matches but that shouldn't be used, instead a new appliesToFilter
     * should be created, and that should not match
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithNonMatchingESAs() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResourceImpl esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\"");
        Asset ass1 = (Asset) reflectiveCallAnyTypes(esa1, "getAsset", null, null);
        AppliesToFilterInfo atfi1 = new AppliesToFilterInfo();
        atfi1.setEditions(Collections.singletonList("Fake Match"));
        ass1.getWlpInformation().setAppliesToFilterInfo(Collections.singleton(atfi1));

        EsaResourceImpl esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"ND\"");
        Asset ass2 = (Asset) reflectiveCallAnyTypes(esa1, "getAsset", null, null);
        AppliesToFilterInfo atfi2 = new AppliesToFilterInfo();
        atfi2.setEditions(Collections.singletonList("Fake Match"));
        ass2.getWlpInformation().setAppliesToFilterInfo(Collections.singleton(atfi2));

        assertFalse("Matching data should match", esa1.createMatchingData().equals(esa2.createMatchingData()));
    }

    /**
     * This checks that the order of entries in the appliesTo doesn't matter.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithDifferentOrderProducts() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResourceImpl esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\";productVersion=8.5.5.0, anotherProduct");

        EsaResourceImpl esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo("anotherProduct, com.ibm.websphere.appserver;productEditions=\"Base\";productVersion=8.5.5.0");

        assertEquals("Matching data should match", esa1.createMatchingData(), esa2.createMatchingData());
    }

    /**
     * This checks that the order of the fields within an entry in the appliesTo doesn't matter.
     *
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    @Test
    public void testMatchWithRegeneratedAppliesToWithDifferentOrderWithinAProduct() throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        EsaResourceImpl esa1 = new EsaResourceImpl(null);
        esa1.setAppliesTo("com.ibm.websphere.appserver;productEditions=\"Base\"; productVersion=8.5.5.0, anotherProduct");

        EsaResourceImpl esa2 = new EsaResourceImpl(null);
        esa2.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.0;productEditions=\"Base\", anotherProduct");

        assertEquals("Matching data should match", esa1.createMatchingData(), esa2.createMatchingData());
    }

    @Test
    public void testGetAppliesToWithMultipleDefinitions1() {
        ProductDefinition def1 = new SimpleProductDefinition("com.ibm.websphere.appserver", null, "Archive", null, "BASE");
        ProductDefinition def2 = new SimpleProductDefinition("com.ibm.websphere.appserver.zos", null, "Archive", null, "BASE");
        EsaResourceImpl mr = new EsaResourceImpl(null);
        mr.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.1; productEditions=\"BASE\"");
        ArrayList<ProductDefinition> defs = new ArrayList<ProductDefinition>();
        defs.add(def1);
        defs.add(def2);
        assertEquals("Should have got version 8.5.5.1 back", "8.5.5.1", mr.getAppliesToVersions(defs));
    }

    @Test
    public void testGetAppliesToWithMultipleDefinitions2() {
        ProductDefinition def1 = new SimpleProductDefinition("com.ibm.websphere.appserver", null, "Archive", null, "BASE");
        ProductDefinition def2 = new SimpleProductDefinition("com.ibm.websphere.appserver.zos", null, "Archive", null, "BASE");
        EsaResourceImpl mr = new EsaResourceImpl(null);
        mr.setAppliesTo("com.ibm.websphere.appserver.zos; productVersion=8.5.5.2; productEditions=\"BASE\"");
        ArrayList<ProductDefinition> defs = new ArrayList<ProductDefinition>();
        defs.add(def1);
        defs.add(def2);
        assertEquals("Should have got version 8.5.5.2 back", "8.5.5.2", mr.getAppliesToVersions(defs));
    }

    @Test
    public void testGetAppliesToWithMultipleDefinitions3() {
        ProductDefinition def1 = new SimpleProductDefinition("com.ibm.websphere.appserver", null, "Archive", null, "BASE");
        ProductDefinition def2 = new SimpleProductDefinition("com.ibm.websphere.appserver.zos", null, "Archive", null, "BASE");
        EsaResourceImpl mr = new EsaResourceImpl(null);
        mr.setAppliesTo("com.ibm.websphere.appserver.zos; productVersion=8.5.5.2; productEditions=\"BASE\", com.ibm.websphere.appserver; productVersion=8.5.5.1; productEditions=\"BASE\"");
        ArrayList<ProductDefinition> defs = new ArrayList<ProductDefinition>();
        defs.add(def1);
        defs.add(def2);
        assertEquals("Should have got version 8.5.5.2 back", "8.5.5.2", mr.getAppliesToVersions(defs));
    }

    /**
     * Test the various get methods work when an applies to range is used.
     *
     * @throws Exception
     */
    @Test
    public void testGetAppliesToRange() throws Exception {
        EsaResourceWritable esa = WritableResourceFactory.createEsa(repoConnection);
        esa.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.2+");
        esa.setName("Name to find");
        esa.setProvideFeature("com.ibm.ws.test.feature");
        esa.setVisibility(Visibility.PUBLIC);
        esa.setProviderName("IBM");
        esa.uploadToMassive(new AddNewStrategy(PUBLISHED, PUBLISHED));
        fixture.refreshTextIndex(esa.getId());

        // Run the test against all of the methods that get ESAs
        runMatchTest(esa, new RunMatchClosure() {
            @Override
            public Collection<EsaResource> runMatch(ProductDefinition definition) throws Exception {
                return new RepositoryConnectionList(repoConnection).getMatchingEsas(definition);
            }
        });

        runMatchTest(esa, new RunMatchClosure() {
            @Override
            public Collection<EsaResource> runMatch(ProductDefinition definition) throws Exception {
                return new RepositoryConnectionList(repoConnection).getMatchingEsas(definition, Visibility.PUBLIC);
            }
        });

        runMatchTest(esa, new RunMatchClosure() {
            @Override
            public Collection<EsaResource> runMatch(ProductDefinition definition) throws Exception {
                RepositoryConnectionList unauthenticatedLoginInfo = new RepositoryConnectionList(fixture.getUserConnection());
                return unauthenticatedLoginInfo.findMatchingEsas("Name to find", definition, Visibility.PUBLIC);
            }
        });
    }

    /**
     * Tests the matching logic for ESAs, because we do some pre-filtering on the server
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testFindMatchingResource() throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, RepositoryException, IOException, URISyntaxException {

        EsaResourceImpl original = (EsaResourceImpl) WritableResourceFactory.createEsa(repoConnection);
        original.setProvideFeature("com.example.testA");
        original.setName("Test A");
        original.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.4");
        original.uploadToMassive(new AddNewStrategy(PUBLISHED, PUBLISHED));

        EsaResourceWritable differentName = WritableResourceFactory.createEsa(repoConnection);
        differentName.setProvideFeature("com.example.testB");
        differentName.setName("Test B");
        differentName.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.4");
        differentName.uploadToMassive(new AddNewStrategy(PUBLISHED, PUBLISHED));

        EsaResourceWritable differentProductVersion = WritableResourceFactory.createEsa(repoConnection);
        differentProductVersion.setProvideFeature("com.example.testA");
        differentProductVersion.setName("Test A");
        differentProductVersion.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.5");
        differentProductVersion.uploadToMassive(new AddNewStrategy(PUBLISHED, PUBLISHED));

        EsaResourceWritable shouldMatch = WritableResourceFactory.createEsa(repoConnection);
        shouldMatch.setProvideFeature("com.example.testA");
        shouldMatch.setName("Test A");
        shouldMatch.setAppliesTo("com.ibm.websphere.appserver; productVersion=8.5.5.4");
        shouldMatch.uploadToMassive(new AddNewStrategy(PUBLISHED, PUBLISHED));

        // getPotentiallyMatchingResources should find things that match type and symbolic name
        Collection<? extends RepositoryResource> potentialMatches = callGetPotentiallyMatchingResources(original);
        assertThat(potentialMatches, containsInAnyOrder(hasId(original), hasId(differentProductVersion), hasId(shouldMatch)));

        List<RepositoryResourceImpl> matches = original.findMatchingResource();
        assertThat(matches, containsInAnyOrder(hasId(original), hasId(shouldMatch)));
    }

    private static interface RunMatchClosure {
        Collection<EsaResource> runMatch(ProductDefinition definition) throws Exception;
    }

    private void runMatchTest(EsaResource resourceToFind, RunMatchClosure methodToTest) throws Exception {
        ProductDefinition exactProduct = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.2", "Archive", null, "BASE");
        ProductDefinition higherProduct = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.3", "Archive", null, "BASE");
        ProductDefinition lowerProduct = new SimpleProductDefinition("com.ibm.websphere.appserver", "8.5.5.1", "Archive", null, "BASE");
        ProductDefinition betaProduct = new SimpleProductDefinition("com.ibm.websphere.appserver", "2015.1.0.0", "Archive", null, "EARLY_ACCESS");
        Collection<EsaResource> matches = methodToTest.runMatch(exactProduct);
        assertEquals(1, matches.size());
        assertTrue(matches.contains(resourceToFind));

        matches = methodToTest.runMatch(higherProduct);
        assertEquals(1, matches.size());
        assertTrue(matches.contains(resourceToFind));

        matches = methodToTest.runMatch(betaProduct);
        assertEquals(1, matches.size());
        assertTrue(matches.contains(resourceToFind));

        matches = methodToTest.runMatch(lowerProduct);
        assertEquals(0, matches.size());
    }

    /**
     * Tests that you can run the {@link EsaResourceImpl#getMatchingEsas(FilterableAttribute, String, RepositoryConnectionList)} method correctly and get the expected result
     *
     * @param attribute
     * @param filterString
     * @param expectedResource
     * @throws RepositoryBackendException
     */
    private void testMatch(FilterableAttribute attribute, String filterString, EsaResource expectedResource) throws RepositoryBackendException {
        Collection<EsaResource> matched = repoConnection.getMatchingEsas(attribute, filterString);
        assertEquals("There should only be one match", 1, matched.size());
        assertTrue("The match should be the right resource", matched.contains(expectedResource));
    }

    private EsaResourceImpl createEsaResource(String provideFeature) throws URISyntaxException {
        EsaResourceImpl esaRes = new EsaResourceImpl(repoConnection);
        populateResource(esaRes);
        esaRes.setProvideFeature(provideFeature);
        esaRes.setName(provideFeature);
        return esaRes;
    }

    @SuppressWarnings("unchecked")
    private Collection<? extends RepositoryResource> callGetPotentiallyMatchingResources(RepositoryResourceImpl resource) {
        try {
            Method m = RepositoryResourceImpl.class.getDeclaredMethod("getPotentiallyMatchingResources");
            m.setAccessible(true);
            return (Collection<RepositoryResource>) m.invoke(resource);
        } catch (Exception e) {
            throw new RuntimeException("Exception calling getPotentiallyMatchingResources", e);
        }
    }

}
