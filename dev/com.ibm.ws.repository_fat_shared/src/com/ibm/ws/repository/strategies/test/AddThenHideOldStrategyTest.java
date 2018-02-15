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
package com.ibm.ws.repository.strategies.test;

import static com.ibm.ws.lars.testutils.BasicChecks.populateResource;
import static com.ibm.ws.lars.testutils.ReflectionTricks.reflectiveCallNoPrimitives;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.exceptions.RepositoryResourceUpdateException;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.internal.ToolResourceImpl;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.ProductResourceWritable;
import com.ibm.ws.repository.resources.writeable.ToolResourceWritable;
import com.ibm.ws.repository.resources.writeable.WebDisplayable;
import com.ibm.ws.repository.strategies.writeable.AddNewStrategy;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;
import com.ibm.ws.repository.strategies.writeable.AddThenHideOldStrategy;
import com.ibm.ws.repository.strategies.writeable.UploadStrategy;

public class AddThenHideOldStrategyTest extends StrategyTestBaseClass {

    private static final String APPLIES_TO_8555 = "com.ibm.websphere.appserver; productEdition=\"BASE,BASE_ILAN,DEVELOPERS,EXPRESS,ND,zOS\"; productVersion=8.5.5.5";
    private static final String APPLIES_TO_8559 = "com.ibm.websphere.appserver; productEdition=\"BASE,BASE_ILAN,DEVELOPERS,EXPRESS,ND,zOS\"; productVersion=8.5.5.9";
    private static final String FEATURE_NAME = "dummy feature";
    private static final String PRODUCT_NAME = "dummy product";
    final String APPLIES_TO_JAN_BETA = "com.ibm.websphere.appserver; productVersion=2016.1.0.0";
    final String APPLIES_TO_FEB_BETA = "com.ibm.websphere.appserver; productVersion=2016.2.0.0";

    @Test
    public void testProductUploadHiding() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        final String PRODUCT_VERSION_8555 = "8.5.5.5";
        final String PRODUCT_VERSION_8559 = "8.5.5.9";
        ProductResourceWritable prod = new ProductResourceImpl(repoConnection);
        prod.setProviderName("IBM");
        prod.setName(PRODUCT_NAME);
        prod.setType(ResourceType.INSTALL);
        prod.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        prod.setProductVersion(PRODUCT_VERSION_8559);
        prod.setDescription("8559");
        prod.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        prod.setProductVersion(PRODUCT_VERSION_8555);
        prod.setDescription("8555");
        prod.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> resources = assertResourceCount(2);
        for (RepositoryResource result : resources) {
            ProductResourceWritable res = (ProductResourceWritable) result;
            if (res.getDescription().equals("8559")) {
                assertEquals("The original resource should now be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
            } else if (res.getDescription().equals("8555")) {
                assertEquals("The new resource should be hidden", DisplayPolicy.HIDDEN, res.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testFeatureUploadHiding() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setDescription("8559");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        esa.setAppliesTo(APPLIES_TO_8555);
        esa.setDescription("8555");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> resources = assertResourceCount(2);
        for (RepositoryResource result : resources) {
            EsaResourceWritable res = (EsaResourceWritable) result;
            if (res.getDescription().equals("8559")) {
                assertEquals("The 8559 resource should now be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
            } else if (res.getDescription().equals("8555")) {
                assertEquals("The 8555 resource should be hidden", DisplayPolicy.HIDDEN, res.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testFeatureUploadHidingReplaceWithSame() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setDescription("8559");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setDescription("8559");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> resources = assertResourceCount(1);
        for (RepositoryResource result : resources) {
            EsaResourceWritable res = (EsaResourceWritable) result;
            assertEquals("The 8559 resource should still be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
        }

        // check no duplicate items were created
        assertResourceCountWithDupes(1);
    }

    @Test
    public void testFeatureUploadHidingReplaceHiddenWithSame() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        esa.setAppliesTo(APPLIES_TO_8555);
        esa.setDescription("8555");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setDescription("8559");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(2);

        esa.setAppliesTo(APPLIES_TO_8555);
        esa.setDescription("8555-replacement");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> resources = assertResourceCount(2);
        for (RepositoryResource result : resources) {
            EsaResourceWritable res = (EsaResourceWritable) result;
            if (res.getDescription().equals("8555")) {
                fail("The '8555' resource should have been replaced by the '8555-replacement' reource");
            } else if (res.getDescription().equals("8555-replacement")) {
                assertEquals("The '8555-replacement' resource should now be hidden", DisplayPolicy.HIDDEN, res.getWebDisplayPolicy());
            } else if (res.getDescription().equals("8559")) {
                assertEquals("The 8559 resource should be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }

        // check no duplicate items were created
        assertResourceCountWithDupes(2);
    }

    @Test
    public void testFeatureUploadHidingPartialAppliesTo() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        final String APPLIES_TO_PRODUCT = "com.ibm.websphere.appserver"; // no version specified
        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setAppliesTo(APPLIES_TO_PRODUCT);
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        esa.setVersion("1.0.0");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        esa.setVersion("2.0.0");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> resources2 = assertResourceCount(2);
        for (RepositoryResource res : resources2) {
            if (res.getVersion().equals("1.0.0")) {
                assertEquals("The original resource should now be visible", DisplayPolicy.HIDDEN, ((WebDisplayable) res).getWebDisplayPolicy());
            } else if (res.getVersion().equals("2.0.0")) {
                assertEquals("The new resource should be hidden", DisplayPolicy.VISIBLE, ((WebDisplayable) res).getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testFeatureUploadHidingBasedOnVersion() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setName(FEATURE_NAME);
        esa.setProviderName("IBM");

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        esa.setVersion("2.0.0");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        esa.setVersion("1.0.0");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(2);
        for (RepositoryResource result : results) {
            EsaResourceWritable res = (EsaResourceWritable) result;
            if (res.getVersion().equals("1.0.0")) {
                assertEquals("The v1 resource should be hidden", DisplayPolicy.HIDDEN, res.getWebDisplayPolicy());
            } else if (res.getVersion().equals("2.0.0")) {
                assertEquals("The v2 resource should be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testFeatureUploadHidingBasedOnBadVersion() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setName(FEATURE_NAME);
        esa.setProviderName("IBM");

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        esa.setVersion("Bad version 1");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        // With the appliesTo the same and INVALID versions the upload should use the newer one
        esa.setVersion("Bad version 2");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(2);
        for (RepositoryResource result : results) {
            EsaResourceWritable res = (EsaResourceWritable) result;
            if (res.getVersion().equals("Bad version 1")) {
                assertEquals("The 'Bad version 1' resource should be hidden", DisplayPolicy.HIDDEN, res.getWebDisplayPolicy());
            } else if (res.getVersion().equals("Bad version 2")) {
                assertEquals("The 'Bad version 2' resource should be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testFeatureUploadHidingVersionShouldNotOverrideAppliesTo() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setName(FEATURE_NAME);
        esa.setProviderName("IBM");

        esa.setAppliesTo(APPLIES_TO_8555);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        esa.setVersion("2.0.0");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setVersion("1.0.0");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(2);
        for (RepositoryResource result : results) {
            EsaResourceWritable res = (EsaResourceWritable) result;
            if (res.getVersion().equals("1.0.0")) {
                assertEquals("The v1/8559 resource should be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
            } else if (res.getVersion().equals("2.0.0")) {
                assertEquals("The v2/8555 resource should be hidden", DisplayPolicy.HIDDEN, res.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testFeatureUploadHidingErrorWhenDuplicates() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setName(FEATURE_NAME);
        esa.setProviderName("IBM");
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCountWithDupes(1);

        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCountWithDupes(2);

        try {
            esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));
        } catch (RepositoryResourceUpdateException e) {
            // when we add these two dups and then do an addThenHideOld we expect the
            // super.uploadAsset(resource, matchingResources) call to AddThenDelete to tidy up the
            // matching resource
            fail("AddThenHideOldStrategy was expected to handle this.");
        }

        // should be only one resource left as the AddThenDelete should have removed the two matching ones
        assertResourceCountWithDupes(1);
    }

    @Test
    public void testFeatureUploadHidingAppliesToWithPlus() throws RepositoryException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

        final String APPLIES_TO_8555_PLUS = "com.ibm.websphere.appserver; productVersion=8.5.5.5+";
        final String APPLIES_TO_8559_PLUS = "com.ibm.websphere.appserver; productVersion=8.5.5.9+";
        final String APPLIES_TO_8559 = "com.ibm.websphere.appserver; productVersion=8.5.5.9";

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);

        esa.setAppliesTo(APPLIES_TO_8559_PLUS);
        esa.setDescription("8559+");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));
        assertResourceCount(1);

        esa.setAppliesTo(APPLIES_TO_8555_PLUS);
        esa.setDescription("8555+");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));
        Collection<? extends RepositoryResource> resources1 = assertResourceCount(2);
        for (RepositoryResource res : resources1) {
            if (res.getDescription().equals("8559+")) {
                assertEquals("The 8559+ resource should now be visible", DisplayPolicy.VISIBLE, ((WebDisplayable) res).getWebDisplayPolicy());
            } else if (res.getDescription().equals("8555+")) {
                assertEquals("The 8555+ resource should now be hidden", DisplayPolicy.HIDDEN, ((WebDisplayable) res).getWebDisplayPolicy());
            }
        }

        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setDescription("8559");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));
        Collection<? extends RepositoryResource> resources2 = assertResourceCount(3);
        for (RepositoryResource res : resources2) {
            if (res.getDescription().equals("8559+")) {
                assertEquals("The 8559+ resource should now be visible", DisplayPolicy.VISIBLE, ((WebDisplayable) res).getWebDisplayPolicy());
            } else if (res.getDescription().equals("8555+")) {
                assertEquals("The 8555+ resource should now be hidden", DisplayPolicy.HIDDEN, ((WebDisplayable) res).getWebDisplayPolicy());
            } else if (res.getDescription().equals("8559")) {
                assertEquals("The 8559 resource should now be hidden", DisplayPolicy.HIDDEN, ((WebDisplayable) res).getWebDisplayPolicy());
            }
        }
    }

    @Test
    public void testFeatureUploadHidingEqualAppliesToDiffVersions() throws RepositoryException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        esa.setVersion("2.0.0");
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));

        esa.setVersion("1.0.0");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(2);
        for (RepositoryResource res : results) {
            EsaResourceWritable loopEsa = (EsaResourceWritable) res;
            if (loopEsa.getWebDisplayPolicy().equals(DisplayPolicy.VISIBLE)) {
                assertEquals("The visible feature had wrong version:", "2.0.0", loopEsa.getVersion());
            }
        }
    }

    @Test
    public void testFeatureUploadHidingBlankAppliesTo() throws RepositoryException {

        EsaResourceWritable esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));

        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(1);
        for (RepositoryResource res : results) {
            EsaResourceWritable loopEsa = (EsaResourceWritable) res;
            if (loopEsa.getWebDisplayPolicy().equals(DisplayPolicy.VISIBLE)) {
                assertEquals("The visible feature had wrong appliesTo:", null, loopEsa.getAppliesTo());
            }
        }
    }

    @Test
    public void testFeatureUploadHidingTwoBetas() throws RepositoryException {

        EsaResourceImpl esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setAppliesTo(APPLIES_TO_FEB_BETA);
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));

        esa.setAppliesTo(APPLIES_TO_JAN_BETA);
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(2);
        for (RepositoryResource res : results) {
            EsaResourceWritable loopEsa = (EsaResourceWritable) res;
            if (loopEsa.getWebDisplayPolicy().equals(DisplayPolicy.VISIBLE)) {
                assertEquals("The visible feature had wrong appliesTo:", APPLIES_TO_FEB_BETA, loopEsa.getAppliesTo());
            }
        }
    }

    @Test
    public void testFeatureUploadHidingBetaAndNonBeta() throws RepositoryException {

        final String APPLIES_TO_JAN_BETA = "com.ibm.websphere.appserver; productVersion=2016.1.0.0";

        EsaResourceImpl esa = new EsaResourceImpl(repoConnection);
        esa.setProviderName("IBM");
        esa.setAppliesTo(APPLIES_TO_8559);
        esa.setName(FEATURE_NAME);
        esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        esa.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));

        esa.setAppliesTo(APPLIES_TO_JAN_BETA);
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(2);
        for (RepositoryResource res : results) {
            EsaResourceWritable loopEsa = (EsaResourceWritable) res;
            if (loopEsa.getWebDisplayPolicy().equals(DisplayPolicy.VISIBLE)) {
                assertEquals("The visible feature had wrong appliesTo:", APPLIES_TO_8559, loopEsa.getAppliesTo());
            }
        }
    }

    @Test
    public void testFeatureUploadHidingHiddenBetaResource() throws RepositoryBackendException, RepositoryResourceException {

        EsaResourceImpl esa = new EsaResourceImpl(repoConnection);
        esa.setVisibility(Visibility.PUBLIC);
        esa.setProviderName("IBM");
        esa.setName(FEATURE_NAME);
        esa.setAppliesTo(APPLIES_TO_FEB_BETA);
        esa.setVanityURL("foourl");
        esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(1);
        for (RepositoryResource res : results) {
            EsaResourceWritable loopEsa = (EsaResourceWritable) res;
            if (loopEsa.getName().equals(FEATURE_NAME)) {
                assertEquals("Feature had wrong visibility:", Visibility.PUBLIC, loopEsa.getVisibility());
                assertEquals("Feature had wrong webDisplayPolicy:", DisplayPolicy.VISIBLE, loopEsa.getWebDisplayPolicy());
            } else {
                fail("Unexpected asset found with name of " + loopEsa.getName());
            }
        }
    }

    @Test
    public void testToolUploadHiding() throws RepositoryException {

        final String TOOL_NAME = "testToolUploadHiding dummy feature";

        ToolResourceWritable tool = new ToolResourceImpl(repoConnection);
        tool.setDescription("Desc1");
        tool.setProviderName("IBM");
        tool.setName(TOOL_NAME);
        tool.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        tool.uploadToMassive(new AddNewStrategy(State.PUBLISHED, State.PUBLISHED));

        tool.setDescription("Desc2");
        tool.setVersion("version 2");
        tool.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));

        Collection<? extends RepositoryResource> results = assertResourceCount(1);
        for (RepositoryResource res : results) {
            ToolResourceWritable loopTool = (ToolResourceWritable) res;
            if (loopTool.getWebDisplayPolicy().equals(DisplayPolicy.VISIBLE)) {
                assertEquals("The visible tool had wrong description:", "Desc2", loopTool.getDescription());
            }
        }
    }

    @Test
    public void testAddingToRepoUsingReplaceExistingStrategy() throws RepositoryBackendException, RepositoryResourceException, URISyntaxException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException {

        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.setAppliesTo(APPLIES_TO_8555);
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());

        resource.setDescription("New");
        resource.setVersion("Version 2");
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());

        Collection<? extends RepositoryResource> allResources = assertResourceCount(2);
        for (RepositoryResource result : allResources) {
            EsaResourceWritable res = (EsaResourceWritable) result;
            if (res.getDescription().equals("Original")) {
                assertEquals("The original resource should now be hidden", DisplayPolicy.HIDDEN, res.getWebDisplayPolicy());
            } else if (res.getDescription().equals("New")) {
                assertEquals("The new resource should be visible", DisplayPolicy.VISIBLE, res.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    /**
     * The website treats a null web display policy as though it was visible so add then hide should hide it.
     *
     * @throws URISyntaxException
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     */
    @Test
    public void testHidingNullDisplayPolicy() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {

        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(null);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());
        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());

        Collection<? extends RepositoryResource> allResources = assertResourceCount(2);
        for (RepositoryResource result : allResources) {
            EsaResourceWritable esa = (EsaResourceWritable) result;
            if (esa.getDescription().equals("Original")) {
                assertEquals("The original resource should now be hidden", DisplayPolicy.HIDDEN, esa.getWebDisplayPolicy());
            } else if (esa.getDescription().equals("New")) {
                assertNull("The new resource should still be visible due to null display policy", esa.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    /**
     * Test if you have a draft asset with the same vanity URL then it is ignored
     */
    @Test
    public void testDraftAssetsIgnored() throws RepositoryBackendException, RepositoryResourceException, URISyntaxException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, InstantiationException {

        EsaResourceImpl draft = new EsaResourceImpl(repoConnection);
        populateResource(draft);
        draft.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        draft.setDescription("Draft");
        draft.setProviderName("IBM");
        draft.uploadToMassive(new AddNewStrategy());

        EsaResourceImpl published = new EsaResourceImpl(repoConnection);
        published.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        published.setDescription("Published");
        published.setVersion("version 2");
        published.setProviderName("IBM");
        published.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));

        assertEquals("this resource should have gone into the published state", State.PUBLISHED, published.getState());
        EsaResourceImpl newResource = new EsaResourceImpl(repoConnection);
        newResource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        newResource.setDescription("New");
        newResource.setVersion("version 3");
        newResource.setProviderName("IBM");
        // This shouldn't throw an exception for the test to pass
        newResource.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, newResource.getState());

        Collection<? extends RepositoryResource> allResources = assertResourceCount(3);
        for (RepositoryResource result : allResources) {
            EsaResourceWritable esa = (EsaResourceWritable) result;
            if (esa.getDescription().equals("Draft")) {
                assertEquals("The draft resource should remain visible", DisplayPolicy.VISIBLE, esa.getWebDisplayPolicy());
            } else if (esa.getDescription().equals("Published")) {
                assertEquals("The original resource should now be hidden", DisplayPolicy.HIDDEN, esa.getWebDisplayPolicy());
            } else if (esa.getDescription().equals("New")) {
                assertEquals("The new resource should now be visible", DisplayPolicy.VISIBLE, esa.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test: " + esa);
            }
        }
    }

    @Test
    public void testOverwritingExisting() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {

        EsaResourceImpl resource1 = new EsaResourceImpl(repoConnection);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setDescription("Original");
        resource1.uploadToMassive(new AddNewStrategy());
        EsaResourceImpl resource2 = new EsaResourceImpl(repoConnection);
        resource2.setDescription("New");
        populateResource(resource2);
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.uploadToMassive(new AddThenHideOldStrategy());
        Collection<? extends RepositoryResource> allResources = repoConnection.getAllResourcesWithDupes();
        assertEquals("There should be 1 resources in the repo", 1, allResources.size());
        assertEquals("There resource should be the second one added", resource2, allResources.iterator().next());
        assertEquals("The feature web display policy should be visible", DisplayPolicy.VISIBLE,
                     reflectiveCallNoPrimitives(allResources.iterator().next(), "getWebDisplayPolicy", (Object[]) null));
    }

    @Test
    public void testDontHideWhenAddingHidden() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {

        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());

        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.setWebDisplayPolicy(DisplayPolicy.HIDDEN);
        resource.uploadToMassive(new AddThenHideOldStrategy());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (RepositoryResource res : allResources) {
            if (res.getDescription().equals("Original")) {
                assertEquals("The original resource should be visibile still", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("New")) {
                assertEquals("The new resource should be hidden", DisplayPolicy.HIDDEN, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testDontHideWhenAddingDraft() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {

        EsaResourceImpl resource = new EsaResourceImpl(repoConnection);
        populateResource(resource);
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.setDescription("Original");
        resource.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource.getState());

        resource.setDescription("New");
        resource.setVersion("version 2");
        resource.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource.uploadToMassive(new AddThenHideOldStrategy(null, State.DRAFT));
        assertEquals("this resource should have gone into the draft state", State.DRAFT, resource.getState());

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("There should be 2 resources in the repo", 2, allResources.size());
        for (RepositoryResource res : allResources) {
            if (res.getDescription().equals("Original")) {
                assertEquals("The original resource should be visibile still", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else if (res.getDescription().equals("New")) {
                assertEquals("The new resource should be visible", DisplayPolicy.VISIBLE, reflectiveCallNoPrimitives(res, "getWebDisplayPolicy", (Object[]) null));
            } else {
                fail("Unexpected resource found in the test");
            }
        }
    }

    @Test
    public void testDeleteAssetAfterCacheSetup() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {

        EsaResourceImpl resource1 = new EsaResourceImpl(repoConnection);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setDescription("Original");
        resource1.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource1.getState());

        // Do an update to get the cache loaded
        EsaResourceImpl resource2 = new EsaResourceImpl(repoConnection);
        populateResource(resource2);
        resource2.setDescription("New");
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.uploadToMassive(new AddThenHideOldStrategy());
        assertResourceCount(1);

        // Now update this using the add then delete so that the one in the cache will be deleted
        EsaResourceImpl resource3 = new EsaResourceImpl(repoConnection);
        populateResource(resource3);
        resource3.setDescription("Even Newer");
        resource3.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource3.uploadToMassive(new AddThenDeleteStrategy());
        assertResourceCount(1);

        // Now add a new one which doesn't match but has the same vanity URL, we should discover that resource2 has been deleted and hide resource3
        EsaResourceImpl resource4 = new EsaResourceImpl(repoConnection);
        populateResource(resource4);
        resource4.setVersion("2");
        resource4.setDescription("Final resource");
        resource4.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource4.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource4.getState());

        Collection<? extends RepositoryResource> allResources = assertResourceCount(2);
        for (RepositoryResource res : allResources) {
            EsaResourceWritable esa = (EsaResourceWritable) res;

            if (esa.getDescription().equals("Even Newer")) {
                assertEquals("The 'Even newer' resource should now be hidden", DisplayPolicy.HIDDEN, esa.getWebDisplayPolicy());
            } else if (esa.getDescription().equals("Final resource")) {
                assertEquals("The 'Final resource' resource should be visible", DisplayPolicy.VISIBLE, esa.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test " + esa);
            }
        }
    }

    /**
     * Having two visible assets with the same vanity URL means that repository
     * is in an inconsistent state. There is some logic to look for this inconsistency
     * in the AddThenHideStrategy. This test is testing the checking logic. It sets
     * up the repository in a dodgy state by uploading two visible assets with the
     * same URL (using AddNew), and then tests that an error is thrown by the checking
     * logic when you try to add a third asset using AddThenHideOld
     *
     */
    @Test
    public void testDuplicateVisibleVanityURLs() throws RepositoryBackendException, RepositoryResourceException {

        // Need to set name/appliesTo for the the assets to ensure they are treated as different
        // and hence the second asset will get uploaded (instead of ignored)
        // Also set the vanity URL to be the same for both.
        EsaResourceImpl resource1 = new EsaResourceImpl(repoConnection);
        populateResource(resource1);
        resource1.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource1.setProviderName("IBM");
        resource1.setName("name1");
        resource1.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.0");
        resource1.setVanityURL("foourl");
        resource1.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource1.getState());

        EsaResourceImpl resource2 = new EsaResourceImpl(repoConnection);
        populateResource(resource2);
        resource2.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource2.setProviderName("IBM");
        resource2.setName("name2");
        resource2.setVanityURL("foourl");
        resource2.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.1");
        resource2.uploadToMassive(new AddNewStrategy(null, State.PUBLISHED));
        assertEquals("this resource should have gone into the published state", State.PUBLISHED, resource2.getState());

        // Should now have 2 visible assets with the URL 'foourl'. Uploading another
        // asset with the same URL (using AddThenHideOld), should trigger the checks for
        // duplicate vanity URLs and hence an error.
        EsaResourceImpl resource3 = new EsaResourceImpl(repoConnection);
        populateResource(resource3);
        resource3.setProviderName("IBM");
        resource3.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
        resource3.setName("name3");
        resource3.setVanityURL("foourl");
        resource3.setAppliesTo("com.ibm.ws.wlp; productVersion=8.5.5.2");
        resource3.uploadToMassive(new AddThenHideOldStrategy(null, State.PUBLISHED));

        Collection<? extends RepositoryResource> allResources = assertResourceCount(3);
        for (RepositoryResource res : allResources) {
            EsaResourceWritable esa = (EsaResourceWritable) res;

            if (esa.getName().equals("name1")) {
                assertEquals("Esa 'name1' should now be hidden", DisplayPolicy.HIDDEN, esa.getWebDisplayPolicy());
            } else if (esa.getName().equals("name2")) {
                assertEquals("Esa 'name2' should now be hidden", DisplayPolicy.HIDDEN, esa.getWebDisplayPolicy());
            } else if (esa.getName().equals("name3")) {
                assertEquals("Esa 'name3' should now be visible", DisplayPolicy.VISIBLE, esa.getWebDisplayPolicy());
            } else {
                fail("Unexpected resource found in the test: " + esa);
            }
        }

    }

    @Override
    protected UploadStrategy createStrategy(State ifMatching, State ifNoMatching) {
        return new AddThenHideOldStrategy(ifMatching, ifNoMatching);
    }

    /**
     * Convenience method for both getting all the resources and checking that the correct number are available
     *
     * @param expected - the expected number of resources in the repository
     * @return Collection<? extends RepositoryResource>
     * @throws RepositoryBackendException
     */
    private Collection<? extends RepositoryResource> assertResourceCount(int expected) throws RepositoryBackendException {
        Collection<? extends RepositoryResource> countList = new RepositoryConnectionList(repoConnection).getAllResources();
        assertEquals("Wrong number of resources returned: ", expected, countList.size());
        return countList;
    }

    /**
     * Convenience method for both getting all the resources and checking that the correct number are available.
     * Duplicate resources are returned if they exist.
     *
     * @param expected - the expected number of resources in the repository including duplicates
     * @return Collection<? extends RepositoryResource>
     * @throws RepositoryBackendException
     */
    private Collection<? extends RepositoryResource> assertResourceCountWithDupes(int expected) throws RepositoryBackendException {
        Collection<? extends RepositoryResource> countList = new RepositoryConnectionList(repoConnection).getAllResourcesWithDupes();
        assertEquals("Wrong number of resources returned: ", expected, countList.size());
        return countList;
    }
}
