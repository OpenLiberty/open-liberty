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

import static com.ibm.ws.lars.testutils.BasicChecks.populateResource;
import static com.ibm.ws.lars.testutils.BasicChecks.simpleUpload;
import static com.ibm.ws.repository.common.enums.State.PUBLISHED;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.FileRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.AttachmentLinkType;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.DisplayPolicy;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.common.utils.internal.HashUtils;
import com.ibm.ws.repository.connections.RepositoryConnection;
import com.ibm.ws.repository.connections.RepositoryConnectionList;
import com.ibm.ws.repository.exceptions.RepositoryBackendException;
import com.ibm.ws.repository.exceptions.RepositoryBackendIOException;
import com.ibm.ws.repository.exceptions.RepositoryBackendRequestFailureException;
import com.ibm.ws.repository.exceptions.RepositoryException;
import com.ibm.ws.repository.exceptions.RepositoryResourceCreationException;
import com.ibm.ws.repository.exceptions.RepositoryResourceException;
import com.ibm.ws.repository.resources.AttachmentResource;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.internal.EsaResourceImpl;
import com.ibm.ws.repository.resources.internal.IfixResourceImpl;
import com.ibm.ws.repository.resources.internal.ProductResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl;
import com.ibm.ws.repository.resources.internal.RepositoryResourceImpl.AttachmentResourceImpl;
import com.ibm.ws.repository.resources.internal.ResourceFactory;
import com.ibm.ws.repository.resources.internal.SampleResourceImpl;
import com.ibm.ws.repository.resources.internal.UpdateType;
import com.ibm.ws.repository.resources.writeable.EsaResourceWritable;
import com.ibm.ws.repository.resources.writeable.RepositoryResourceWritable;
import com.ibm.ws.repository.strategies.writeable.AddThenDeleteStrategy;
import com.ibm.ws.repository.strategies.writeable.AddThenHideOldStrategy;
import com.ibm.ws.repository.strategies.writeable.UpdateInPlaceStrategy;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.WlpInformation;

public class ResourceTest {

    private static final String PROVIDER_NAME = "Test Provider";
    private static final Logger logger = Logger.getLogger(ResourceTest.class.getName());

    private final static File resourceDir = new File("lib/LibertyFATTestFiles");

    private TestResource _testRes;

    @Rule
    public final RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryConnection repoConnection = fixture.getAdminConnection();

    //Used for multi-repo tests
    @Rule
    public final FileRepositoryFixture fixture2 = FileRepositoryFixture.createFixture(new File("repo2"));

    @Before
    public void beforeTest() throws IOException {
        _testRes = new TestResource(repoConnection);
    }

    private SampleResourceImpl createSampleResource() {
        SampleResourceImpl sampleRes = new SampleResourceImpl(repoConnection);
        sampleRes.setType(ResourceType.PRODUCTSAMPLE);
        populateResource(sampleRes);
        return sampleRes;
    }

    private void uploadResource(RepositoryResourceWritable res) throws RepositoryBackendException, RepositoryResourceException {
        res.uploadToMassive(new AddThenDeleteStrategy());
    }

    /**
     * This test checks that a resource can be added to massive and that
     * the asset inside the resource is equivalent before and after the
     * upload (equivalent doesn't compare things like ID, uploadedOn etc).
     * It also makes sure that the number of resources in massive inceases
     * by one once we have done the add.
     */
    @Test
    public void testCreateResource() throws SecurityException, NoSuchFieldException, RepositoryException, URISyntaxException {

        populateResource(_testRes);
        Asset beforeAss = _testRes.getAsset();
        int numAssets = new RepositoryConnectionList(repoConnection).getAllResources().size();
        _testRes.uploadToMassive(new UpdateInPlaceStrategy());
        Asset afterAss = _testRes.getAsset();
        assertTrue("The asset was changed after uploading before=<" + beforeAss
                   + "> after=<" + afterAss + ">", beforeAss.equivalent(afterAss));
        assertNull(
                   "The original asset was updated with an ID when only the returned one should have an id ",
                   beforeAss.get_id());
        assertNotNull("An ID was not assigned to the asset after uploading ",
                      afterAss.get_id());
        int newNumAssets = new RepositoryConnectionList(repoConnection).getAllResources().size();
        assertEquals("Unexpected number of assets found after adding an asset",
                     numAssets + 1, newNumAssets);
    }

    /**
     * This test will check that the creation of resources from the
     * MassiveResource.Type enum creates a resource of the correct type
     *
     * @throws RepositoryBackendIOException
     */
    @Test
    public void testCreateType() throws RepositoryBackendIOException {
        // Null would fail an instanceof check so this checks for null as well
        assertTrue(ResourceFactory.getInstance().createResource(ResourceType.ADDON, repoConnection, null) instanceof ProductResourceImpl);
        assertTrue(ResourceFactory.getInstance().createResource(ResourceType.FEATURE, repoConnection, null) instanceof EsaResourceImpl);
        assertTrue(ResourceFactory.getInstance().createResource(ResourceType.IFIX, repoConnection, null) instanceof IfixResourceImpl);
        assertTrue(ResourceFactory.getInstance().createResource(ResourceType.INSTALL, repoConnection, null) instanceof ProductResourceImpl);
        assertTrue(ResourceFactory.getInstance().createResource(ResourceType.OPENSOURCE, repoConnection, null) instanceof SampleResourceImpl);
        assertTrue(ResourceFactory.getInstance().createResource(ResourceType.PRODUCTSAMPLE, repoConnection, null) instanceof SampleResourceImpl);
    }

    /**
     * This method checks that we can upload then read back an asset from massive and
     * that the resource read from massive is equivalent to the one we uploaded
     */
    @Test
    public void testGetAssetBackFromMassive() throws RepositoryException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException {
        SampleResourceImpl sampleRes = createSampleResource();
        uploadResource(sampleRes);
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertTrue("Resource was different from the one uploaded", sampleRes.equivalent(readBack));
    }

    /**
     * Checks that we equivalent method says that resources are equivalent that
     * have different fields that we are not interested in. This is achieved by creating
     * a resource (which has fields we care about in) then uploading the resource.
     * We then get the resource back from massive which will now contain fields
     * that we don't care about and should not effect the equivalent method.
     *
     * We then make an update to a field we do care about. We then ensure that this
     * resource is not equivalent to the one we read back from massive. We then
     * upload this resource and re-check this hasn't changed the fact that this resource
     * should not be equivalent to the one we read back from massive.
     *
     * We then read back the resource from massive again and at this point the two
     * resources should be equivalent again.
     */
    @Test
    public void testResourceEquivalent() throws URISyntaxException, RepositoryException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.US);
        uploadResource(sampleRes);

        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertTrue("The resource uploaded is not equivalent to the one extracted from massive",
                   sampleRes.equivalent(readBack));

        sampleRes.moveToState(PUBLISHED);
        assertTrue("The resource is not equivalent after being published",
                   sampleRes.equivalent(readBack));

        sampleRes.setDescription("new description");
        assertFalse("The resource has been updated and shouldn't be equivalent to the one obtained from massive",
                    sampleRes.equivalent(readBack));

        uploadResource(sampleRes);

        assertFalse("The resource has been updated and shouldn't be equivalent to the one obtained from massive",
                    sampleRes.equivalent(readBack));

        readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertTrue("The resource uploaded is not equivalent to the one extracted from massive",
                   sampleRes.equivalent(readBack));
    }

    /**
     * Tests that we can upload a resource into massive and that the find matching resource returns a
     * matching one when it should
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    public void testFindMatchingResource() throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, RepositoryException, IOException, URISyntaxException {
        // Add a resource
        SampleResourceImpl sampleRes = createSampleResource();

        List<RepositoryResourceImpl> resFromMassive = sampleRes.findMatchingResource();
        assertTrue("A matching resource was found when there should not have been one",
                   resFromMassive.isEmpty());

        uploadResource(sampleRes);

        resFromMassive = sampleRes.findMatchingResource();

        assertEquals("No matching resource was found when we expected to find one",
                     1, resFromMassive.size());
        assertTrue("The matching resource was not as expected", sampleRes.equivalent(resFromMassive.get(0)));
    }

    /**
     * Tests that we can upload a resource, then using the same resource object
     * we can update the fields in the resource and update it in massive.
     * Check that the resource we get back is equivalent to the updated resource we
     * uploaded.
     */
    @Test
    public void testUpdateResource() throws IOException, RepositoryException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException {
        assumeThat(fixture.isUpdateSupported(), is(true));

        SampleResourceImpl sampleRes = createSampleResource();
        uploadResource(sampleRes);

        sampleRes.setDescription("Updated description");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        // Read the resource back from massive
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);

        assertTrue("The updated resource was not the same as the one found in massive",
                   sampleRes.equivalent(readBack));

    }

    /**
     * This tests is similiar to testUpdateResource but this time we update fields
     * inside the wlpinfo inner fields.
     */
    @Test
    public void testUpdateWlpInfoInResource() throws IOException, RepositoryException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException {
        assumeThat(fixture.isUpdateSupported(), is(true));

        SampleResourceImpl sampleRes = createSampleResource();
        sampleRes.setAppliesTo("old applies to");
        uploadResource(sampleRes);

        sampleRes.setAppliesTo("new applies to");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        // Read the resource back from massive
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);

        assertTrue("The updated resource was not the same as the one found in massive",
                   sampleRes.equivalent(readBack));

    }

    /**
     * This test is like testUpdateWlpInfoInResource except that we create a brand new
     * resource object to update with instead of re-using the resource object we used for
     * doing the initial add. This checks that we don't hit any partial-update problems
     */
    @Test
    public void testUpdateWlpInfo() throws IOException, RepositoryException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException {
        assumeThat(fixture.isUpdateSupported(), is(true));

        SampleResourceImpl res1 = new SampleResourceImpl(repoConnection);
        res1.setType(ResourceType.OPENSOURCE);
        res1.setAppliesTo("testAppliesTo1");
        populateResource(res1);

        uploadResource(res1);
        String id1 = res1.getId();

        SampleResourceImpl res2 = new SampleResourceImpl(repoConnection);
        populateResource(res2);
        res2.setType(ResourceType.OPENSOURCE);
        res2.setDescription("desc changed");
        res2.setAppliesTo("testAppliesTo1");

        res2.uploadToMassive(new UpdateInPlaceStrategy());

        // Read the resource back from massive
        String id2 = res2.getId();

        assertEquals("This was an update, the ids should be the same", id1, id2);
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id1);

        assertFalse("The updated resource was the same as the one found in massive",
                    res1.equivalent(readBack));
        assertTrue("The updated resource was not the same as the one found in massive",
                   res2.equivalent(readBack));

    }

    /**
     * This test tests the refreshFromMassive call. A resource is uploaded and read back.
     * The resource is then updated, we check that the updated resource is not equivalent to
     * the updated resource. We then refresh the resource we read back - this should make
     * the resource equivalent to the updated one.
     */
    @Test
    public void testRefreshData() throws IOException, RepositoryException, SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, URISyntaxException {
        assumeThat(fixture.isUpdateSupported(), is(true));

        // Add a resource
        SampleResourceImpl sampleRes = createSampleResource();
        uploadResource(sampleRes);

        // Read the resource back from massive
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);

        // Update resource
        sampleRes.setDescription("Updated description");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        // Check that the original resource has not been updated
        assertFalse("The resource found in massive was equal to the pre-updated resource " +
                    " when it should have been updated",
                    sampleRes.equivalent(readBack));

        // refresh to get latest data
        readBack.refreshFromMassive();

        // Now confirm the 2 resources match
        assertTrue("The resource found in massive was not the same as the updated resource " +
                   " when it should have been updated",
                   sampleRes.equivalent(readBack));
    }

    /**
     * Tests
     * - What happens when we delete something that doesn't exist
     * - Add resource
     * - delete resource
     * - ensure the resource is no longer in massive
     *
     * @throws URISyntaxException
     */
    @Test
    public void testDelete() throws IOException, RepositoryException, URISyntaxException {
        populateResource(_testRes);
        uploadResource(_testRes);
        String id = _testRes.getId();
        _testRes.delete();
        try {
            repoConnection.getResource(id);
            fail("Should not of been able to get an asset that did not exist");
        } catch (RepositoryException e) {
            // pass
        }
    }

    /**
     * Add an attachment and ensure we can get an input stream to it. The attachment
     * is added to the resource before the resource is uploaded
     *
     * @throws URISyntaxException
     */
    @Test
    public void testAddAttachmentBeforeUploading() throws IOException, RepositoryException, URISyntaxException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment);
        uploadResource(sampleRes);
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);
        assertEquals("We only expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No attachment was found", readBack.getAttachment(attachment.getName()));
    }

    /**
     * Add an attachment and ensure we can get an input stream to it. The resource is created
     * is created then the attachment is added to the resource and re-uploaded to massive, this
     * should update the asset in massive to include the attachment. We make sure we can get
     * an input stream to the attachment and that there is exactly one attachment associated
     * with the resource.
     *
     * @throws URISyntaxException
     */
    @Test
    public void testAddAttachmentAfterUploading() throws IOException, RepositoryException, URISyntaxException {
        SampleResourceImpl sampleRes = createSampleResource();
        uploadResource(sampleRes);
        File attachment = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment);
        sampleRes.uploadToMassive(new AddThenDeleteStrategy());
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);
        assertEquals("We only expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment was found", readBack.getAttachment(attachment.getName()));
    }

    @Test
    public void testWeCanHostAttachmentsOutsideMassive() throws Exception {
        SampleResourceImpl sampleRes = createSampleResource();

        String testFile = "testfile.txt";

        String sampleUrl = fixture.getHostedFileRoot() + "/testfile.txt";

        sampleRes.addAttachment(new File(resourceDir, testFile),
                                AttachmentType.CONTENT, testFile, sampleUrl, null);
        uploadResource(sampleRes);
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);
        assertEquals("We expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment was found", readBack.getAttachment(testFile));
        AttachmentResource att = readBack.getAttachment(testFile);
        InputStream is = att.getInputStream();
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        char[] buffer = new char[1024];
        StringBuilder sb = new StringBuilder();
        while ((isr.read(buffer)) != -1) {
            sb.append(buffer);
        }
        assertTrue(sb.toString().contains("This is a test file"));
        assertEquals("The link type should default to direct", AttachmentLinkType.DIRECT, ((AttachmentResourceImpl) att).getLinkType());
    }

    /**
     * This test checks that we the updateRequired methods return the correct values.
     * A resource is created, we confirm that updateRequired returns ADD at this point.
     * We then upload the resource into massive and add an attachment. At this point
     * the asset itself should not need updating (returns NOTHING), but the attachment
     * itself should return ADD when asked if an update is required.
     * Next we modify the resource to use a different attachment (using the same attachment
     * name as the original name). This should cause the asset to say that NOTHING needs updating
     * but the attachment should say that an UPDATE is required.
     * Lastly we upload the resource and ensure that, with no further changes to the resource,
     * that both the asset and the attachment say that nothing needs updating
     */
    @Test
    public void testUpdateAttachmentRequired() throws RepositoryException, IOException, URISyntaxException {
        SampleResourceImpl sampleRes = createSampleResource();
        List<RepositoryResourceImpl> matching = sampleRes.findMatchingResource();
        assertEquals("Attachment had been uploaded, isUpdateRequired should have returned ADD",
                     UpdateType.ADD, sampleRes.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);
        File attachment = new File(resourceDir, "TestAttachment.txt");
        matching = sampleRes.findMatchingResource();
        assertEquals("Nothing has changed on the resource so isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));

        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "documentationAttachment");
        matching = sampleRes.findMatchingResource();

        AttachmentResourceImpl sampleAtt;

        // The resource should not need updating after an attachment is added
        assertEquals("Attachment was added, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need adding
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("documentationAttachment");
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleAtt.updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new AddThenDeleteStrategy());

        sampleRes.addContent(attachment);
        matching = sampleRes.findMatchingResource();

        // The resource will need updating when content is added
        assertEquals("Content was added so the file size should have changed and isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleRes.updateRequired(getFirst(matching)));
        // ... and the attachment does need adding
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("TestAttachment.txt");
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleAtt.updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new AddThenDeleteStrategy());

        attachment = new File(resourceDir, "TestAttachment2.txt");
        sampleRes.addAttachment(attachment, AttachmentType.CONTENT, "TestAttachment.txt");
        matching = sampleRes.findMatchingResource();
        // The resource should not need updating
        assertEquals("Attachment was changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need updating
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("TestAttachment.txt");
        assertEquals("Attachment was added, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleAtt.updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new AddThenDeleteStrategy());

        matching = sampleRes.findMatchingResource();
        assertEquals("nothing has changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("TestAttachment.txt");
        assertEquals("nothing has changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleAtt.updateRequired(getFirst(matching)));
    }

    private RepositoryResourceImpl getFirst(List<RepositoryResourceImpl> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

    @Test
    public void testUpdateURLInAttachment() throws RepositoryException, IOException, URISyntaxException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment = new File(resourceDir, "TestAttachment.txt");

        AttachmentResourceImpl sampleAtt;

        // No url specified yet
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "useDefaultURL");
        List<RepositoryResourceImpl> matching = sampleRes.findMatchingResource();
        assertEquals("Asset has not been uploaded, isUpdateRequired should have returned ADD",
                     UpdateType.ADD, sampleRes.updateRequired(getFirst(matching)));
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("useDefaultURL");
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleAtt.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Still no url specified yet - so this should match - we aren't changing the attachment at all
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "useDefaultURL");
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset has not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("useDefaultURL");
        assertEquals("Attachment has not changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleAtt.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Now create a new attachment and use the internal URL, should add a new attachment
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "specifyURL");
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("specifyURL");
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleAtt.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Now use an internal URL, should add a new attachment
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "specifyURL", "http://blah", AttachmentLinkType.DIRECT);
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("specifyURL");
        assertEquals("Attachment URL was set to external, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleAtt.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        // Now change the URL, this should cause an update on attachment
        sampleRes.addAttachment(attachment, AttachmentType.DOCUMENTATION, "specifyURL", "http://differentblah", AttachmentLinkType.DIRECT);
        matching = sampleRes.findMatchingResource();
        assertEquals("Asset not changed, isUpdateRequired should have returned NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("specifyURL");
        assertEquals("Attachment URL was changed, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleAtt.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);
    }

    /**
     * Check CRC of attachment, upload it then verify CRC matches the input stream
     * from massive for the asset
     */
    @Test
    public void testCRC() throws Exception {
        File attachment = new File(resourceDir, "TestAttachment.txt");
        Method getCRCMethod = RepositoryResourceImpl.class.getDeclaredMethod("getCRC", InputStream.class);
        getCRCMethod.setAccessible(true);
        long localFileCRC = (Long) getCRCMethod.invoke(null, new FileInputStream(attachment));

        SampleResourceImpl sampleRes = createSampleResource();
        sampleRes.addContent(attachment);
        uploadResource(sampleRes);
        long massiveFileCRC = sampleRes.getAttachment(attachment.getName()).getCRC();

        assertEquals("CRC of file before adding to massive is different from the one in massive",
                     localFileCRC, massiveFileCRC);
    }

    @Test
    public void testFeaturedWeight() throws URISyntaxException, RepositoryBackendException, RepositoryResourceException {
        SampleResourceImpl sampleRes = createSampleResource();
        uploadResource(sampleRes);

        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertNull("The resource should not have a featuredWeight", readBack.getFeaturedWeight());

        sampleRes.setFeaturedWeight("5");
        uploadResource(sampleRes);
        readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertEquals("The resource should not have a featuredWeight", "5", readBack.getFeaturedWeight());

        sampleRes.setFeaturedWeight("4");
        uploadResource(sampleRes);
        readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertEquals("The resource should not have a featuredWeight", "4", readBack.getFeaturedWeight());

        sampleRes.setFeaturedWeight(null);
        uploadResource(sampleRes);
        readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertNull("The resource should not have a featuredWeight", readBack.getFeaturedWeight());
    }

    /**
     * Checks that the resource should get updated if the CRC changes
     *
     * @throws URISyntaxException
     * @throws RepositoryException
     */
    @Test
    public void testCRCChangeCausesUpdate() throws URISyntaxException, RepositoryException {
        SampleResourceImpl sampleRes = createSampleResource();
        List<RepositoryResourceImpl> matching = sampleRes.findMatchingResource();
        assertEquals("Attachment had been uploaded, isUpdateRequired should have returned ADD",
                     UpdateType.ADD, sampleRes.updateRequired(getFirst(matching)));
        uploadResource(sampleRes);

        AttachmentResourceImpl sampleAtt;

        File attachment1 = new File(resourceDir, "crc1.txt");
        sampleRes.addAttachment(attachment1, AttachmentType.DOCUMENTATION, "documentationAttachment");
        matching = sampleRes.findMatchingResource();
        // The resource should not need updating after an attachment is added
        assertEquals("Attachment was added, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need adding
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("documentationAttachment");
        assertEquals("Attachment was added, isUpdateRequired should have returned type ADD",
                     UpdateType.ADD, sampleAtt.updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());

        File attachment2 = new File(resourceDir, "crc2.txt");
        sampleRes.addAttachment(attachment2, AttachmentType.DOCUMENTATION, "documentationAttachment");
        matching = sampleRes.findMatchingResource();
        // The resource should not need updating
        assertEquals("Attachment was changed, isUpdateRequired should have returned type NOTHING",
                     UpdateType.NOTHING, sampleRes.updateRequired(getFirst(matching)));
        // ... but the attachment does need updating
        sampleAtt = (AttachmentResourceImpl) sampleRes.getAttachment("documentationAttachment");
        assertEquals("Attachment was added, isUpdateRequired should have returned type UPDATE",
                     UpdateType.UPDATE, sampleAtt.updateRequired(getFirst(matching)));
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());
    }

    /**
     * This test uploads an attachment, then re-uploads a different attachment under the same
     * name (causing an update). This test ensures that the information in AttachmentInfo (in this
     * case we use CRC) is updated correctly when the attachment is updated.
     *
     * @throws Exception
     */
    @Test
    public void testUpdateAttachmentInfo() throws Exception {
        Method getCRCMethod = RepositoryResourceImpl.class.getDeclaredMethod("getCRC", InputStream.class);
        getCRCMethod.setAccessible(true);

        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        File attachment2 = new File(resourceDir, "TestAttachment2.txt");
        long localFileCRC1 = (Long) getCRCMethod.invoke(null, new FileInputStream(attachment1));
        long localFileCRC2 = (Long) getCRCMethod.invoke(null, new FileInputStream(attachment2));
        logger.log(Level.INFO, "local1 = " + localFileCRC1);
        logger.log(Level.INFO, "local2 = " + localFileCRC2);

        SampleResourceImpl sampleRes = createSampleResource();
        sampleRes.addContent(attachment1);
        uploadResource(sampleRes);
        long massiveFileCRC = sampleRes.getAttachment(attachment1.getName()).getCRC();

        assertEquals("CRC of file before adding to massive is different from the one in massive",
                     localFileCRC1, massiveFileCRC);

        // Specify that the "TestAttachment2.txt" should be stored under the name "TestAttachment.txt". This
        // will mean the attachment gets updated, rather than a new attachment added
        sampleRes.addAttachment(attachment2, AttachmentType.CONTENT, "TestAttachment.txt");
        sampleRes.uploadToMassive(new UpdateInPlaceStrategy());
        massiveFileCRC = sampleRes.getAttachment(attachment1.getName()).getCRC();

        assertEquals("CRC of file before adding to massive is different from the one in massive",
                     localFileCRC2, massiveFileCRC);
    }

    /**
     * This method checks that the provider name is set correctly, and doesn't use a default
     */
    @Test
    public void testDifferentProvider() throws RepositoryException, URISyntaxException {
        SampleResourceImpl sampleRes = createSampleResource();
        sampleRes.setProviderName(PROVIDER_NAME);
        uploadResource(sampleRes);
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        logger.log(Level.INFO, "Readback " + readBack);
        assertTrue("The asset read back from Massive didn't have the expected provider",
                   readBack.toString().contains(PROVIDER_NAME));
        assertEquals("The asset read back from Massive didn't have the expected provider",
                     sampleRes.getProviderName(), readBack.getProviderName());
    }

    /**
     * This tests that we can't refreshFromMassive after a resource has been deleted
     */
    @Test
    public void testRefreshAfterDelete() throws URISyntaxException, RepositoryException {
        populateResource(_testRes);
        uploadResource(_testRes);
        _testRes.delete();
        try {
            _testRes.refreshFromMassive();
            fail("We should not be able to refresh an asset from massive after it has been deleted");
        } catch (RepositoryBackendRequestFailureException e) {
            // expected
            assertTrue("Unexpected exception caught " + e, e.getMessage().contains(_testRes.getId()));
        }
    }

    /**
     * This method checks that the size value on an attachment is equal to the
     * size of the attachment payload
     */
    @Test
    public void testGetSize() throws URISyntaxException, RepositoryException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment);
        uploadResource(sampleRes);
        long size = sampleRes.getAttachment("TestAttachment.txt").getSize();
        assertEquals(size, attachment.length());
    }

    /**
     * This test checks we can upload 2 attachments in one asset. We check that the attachment
     * count is 2 and that we can get an InputStream to both attachments
     *
     */
    @Test
    public void testAddMultipleAttachments() throws URISyntaxException, RepositoryException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.ENGLISH);
        uploadResource(sampleRes);
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);
        assertEquals("We expected 2 attachment", 2, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment1 was found",
                      readBack.getAttachment(attachment1.getName()).getInputStream());
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.ENGLISH));
    }

    /**
     * Tests that we can upload 2 attachments then get a snapshot of the resource from massive.
     * We then make sure that snapshot is equivalent to the resource we uploaded. We then update
     * one of the attachments then upload the resource again. We get a 2nd snapshot of the resource
     * from massive. We ensure that the two snapshots are not equivalent, that the one we uploaded
     * is equivalent to the 2nd snapshot but not equivalent to the first snapshot.
     */
    @Test
    public void testAddThenUpdateAttachment() throws URISyntaxException, RepositoryException, SecurityException, NoSuchMethodException, IllegalArgumentException, FileNotFoundException, IllegalAccessException, InvocationTargetException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.US);
        uploadResource(sampleRes);

        String id = sampleRes.getId();
        RepositoryResourceImpl readBack1 = (RepositoryResourceImpl) repoConnection.getResource(id);
        assertTrue("Resource should be equivalent", sampleRes.equivalent(readBack1));

        attachment1 = new File(resourceDir, "TestAttachment2.txt");
        sampleRes.addAttachment(attachment1, AttachmentType.CONTENT, "TestAttachment.txt");
        uploadResource(sampleRes);

        RepositoryResourceImpl readBack2 = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertFalse("Resource should not be equivalent", readBack1.equivalent(readBack2));
        assertFalse("Resource should not be equivalent", sampleRes.equivalent(readBack1));
        assertTrue("Resource should be equivalent", sampleRes.equivalent(readBack2));

    }

    /**
     * Tests that an attachment can be added to massive, and that we can then delete
     * the attachment. We then confirm that we resource has the correct number of
     * attachments and ensure that we can still get an input stream to the remaining
     * attachment, and that we can't get an input stream to the deleted attachment.
     * We then re-add the attachment to ensure there are no problems with adding
     * an attachment back after deleting it.
     */
    @Test
    public void testAddThenDeleteThenReadAttachment() throws URISyntaxException, RepositoryException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addLicense(attachment2, Locale.ENGLISH);
        uploadResource(sampleRes);

        String id = sampleRes.getId();

        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);
        assertEquals("We expected 2 attachment", 2, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment1 was found",
                      readBack.getAttachment(attachment1.getName()));
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.ENGLISH));

        ((AttachmentResourceImpl) sampleRes.getAttachment("TestAttachment.txt")).deleteNow();

        readBack = (RepositoryResourceImpl) repoConnection.getResource(id);
        assertEquals("We expected 1 attachment", 1, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.ENGLISH));

        AttachmentResource at = readBack.getAttachment(attachment1.getName());
        assertNull(at);

        // Lets add it back to ensure there is no problems re-adding a deleted attachment
        sampleRes.addContent(attachment1);
        uploadResource(sampleRes);

        readBack = (RepositoryResourceImpl) repoConnection.getResource(sampleRes.getId());
        assertEquals("We expected 2 attachment", 2, readBack.getAttachmentCount());
        assertNotNull("No input stream to the attachment1 was found",
                      readBack.getAttachment(attachment1.getName()));
        assertNotNull("No input stream to the attachment2 was found",
                      readBack.getLicense(Locale.US));
    }

    /**
     * Tests that the attachment can be downloaded and the file sizes are the same
     */
    @Test
    public void testDownloadFile() throws RepositoryException, URISyntaxException, IOException {
        SampleResourceImpl sampleRes = createSampleResource();
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        sampleRes.addContent(attachment1);
        uploadResource(sampleRes);
        String id = sampleRes.getId();
        RepositoryResourceImpl readBack = (RepositoryResourceImpl) repoConnection.getResource(id);
        AttachmentResourceImpl at = (AttachmentResourceImpl) readBack.getAttachment("TestAttachment.txt");
        Logger.getLogger(this.getClass().getName()).log(Level.INFO, at.toString());
        File download = new File(resourceDir, "download.file");
        if (download.exists()) {
            download.delete();
        }
        download.createNewFile();
        download.deleteOnExit();
        at.downloadToFile(download);
        assertEquals(at.getSize(), download.length());
    }

    /**
     * CAUTION: The test name is important for this test.
     * When a sample resource is created it is given the name of the test that created it, so that
     * if any resource are left behind we know where it was created. This test uses the name of
     * the resource to locate a resource it uploads so if you change the method name you must
     * also change the check
     * if (res.getName().endsWith("testGetAllAssetsDoesntGetAttachments")) {
     * to match the new methiod name
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetAllAssetsDoesntGetAttachments() throws Exception {
        SampleResourceImpl sampleRes = createSampleResource();
        sampleRes.setName("Test sample for testGetAllAssetsDoesntGetAttachments");
        File attachment1 = new File(resourceDir, "TestAttachment.txt");
        File attachment2 = new File(resourceDir, "license_enus.txt");
        sampleRes.addContent(attachment1);
        sampleRes.addLicense(attachment2, Locale.US);
        uploadResource(sampleRes);

        Collection<? extends RepositoryResource> allResources = new RepositoryConnectionList(repoConnection).getAllResources();

        RepositoryResourceImpl readBack = null;
        for (RepositoryResource res : allResources) {
            logger.log(Level.INFO, "Found res " + res.getName());
            if (res.getName().endsWith("testGetAllAssetsDoesntGetAttachments")) {
                readBack = (RepositoryResourceImpl) res;
                break;
            }
        }

        assertNotNull("Wasn't able to find the resource that was just uploaded. Uh oh", readBack);

        Field f = RepositoryResourceImpl.class.getDeclaredField("_attachments");
        f.setAccessible(true);
        Object o = f.get(readBack);
        HashMap<String, AttachmentResource> attachments = (HashMap<String, AttachmentResource>) o;
        assertTrue(attachments.isEmpty());

        assertEquals("There should be 2 attachments (lazily attached) when we try and get them", 2,
                     readBack.getAttachmentCount());

    }

    /**
     * Test to ensure we can read the attachment of type DOCUMENTATION from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthDOCUMENTATION() throws Exception {
        simpleUnauthBASE(AttachmentType.DOCUMENTATION, false);
    }

    /**
     * Test to ensure we can read the attachment of type DOCUMENTATION from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthLicenseDOCUMENTATION() throws Exception {
        simpleUnauthBASE(AttachmentType.DOCUMENTATION, true);
    }

    /**
     * Test to ensure we can read the attachment of type CONTENT from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthCONTENT() throws Exception {
        simpleUnauthBASE(AttachmentType.CONTENT, false);
    }

    /**
     * Test to ensure we can read the attachment of type CONTENT from an asset without
     * authentication.
     *
     * @throws Exception
     */
    @Test
    public void testSimpleUnauthLicenseCONTENT() throws Exception {
        simpleUnauthBASE(AttachmentType.CONTENT, true);
    }

    /**
     * Test to make sure that we always set the asset type when we create it
     */
    @Test
    public void testTypeSetOnCreation() {
        for (ResourceType type : ResourceType.values()) {
            RepositoryResourceImpl resource = ResourceFactory.getInstance().createResource(type, repoConnection, null);
            assertEquals("The type should be set", type, resource.getType());
        }
    }

    /**
     * Method to add an asset with an attachment of the specified type and ensure it can be read back
     * without authentication.
     *
     * @param type
     * @throws RepositoryException
     */
    public void simpleUnauthBASE(AttachmentType type, boolean licensed) throws RepositoryException {
        // create provider
        String prov = "IBM";

        // create asset
        logger.log(Level.INFO, "Creating asset with attachment of type " + type);
        RepositoryResourceImpl res = new IfixResourceImpl(repoConnection);
        res.addAttachment(new File(resourceDir, "TestAttachment.txt"), type, "TestAttachment.txt");
        res.setProviderName(prov);
        res.setName("Test ifix resource " + createDateString());
        if (licensed) {
            res.addLicense(new File(resourceDir, "license_enus.txt"), Locale.ENGLISH);
        }

        // upload asset and publish it
        res.uploadToMassive(new UpdateInPlaceStrategy());
        String id = res.getId();
        res.moveToState(PUBLISHED);

        // Get resource without authentication
        IfixResourceImpl unauthIfix;
        try {
            unauthIfix = (IfixResourceImpl) fixture.getUserConnection().getResource(id); //UNAUTH
            // expected
        } catch (RepositoryBackendException e) {
            fail("RepositoryIOException thrown: " + e);
            return;
        }

        // check we can get an input stream to all
        Collection<AttachmentResource> attResources = unauthIfix.getAttachments();
        for (AttachmentResource attResource : attResources) {
            attResource.getInputStream();
        }
    }

    /**
     * Create two samples with the same name and type but different providers to ensure that
     * separate test items are created (name, provider and type are used to work out whether
     * to do an add or replace) BUT with the same generated vanityRelativeURL (as provider is
     * not part of the generated vanityURL). Call getAllResourcesWithVanityRelativeURL and
     * ensure that both are returned.
     */
    @Test
    public void testGetAllResourcesWithVanityRelativeURL() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {

        SampleResourceImpl sampleRes1 = createSampleResource();
        sampleRes1.setName("testGetAllResourcesWithVanityRelativeURL");
        String p1 = "Provider1";
        sampleRes1.setProviderName(p1);
        uploadResource(sampleRes1);

        SampleResourceImpl sampleRes2 = createSampleResource();
        sampleRes2.setName("testGetAllResourcesWithVanityRelativeURL");
        String p2 = "Provider2";
        sampleRes2.setProviderName(p2);
        uploadResource(sampleRes2);

        String vanityRelativeURL = "samples-testGetAllResourcesWithVanityRelativeURL";
        Collection<RepositoryResourceImpl> collection = new RepositoryConnectionList(repoConnection).getAllResourcesWithVanityRelativeURL(vanityRelativeURL);

        assertEquals("There should be 2 resources returned", 2,
                     collection.size());
    }

    @Test
    public void testSimpleGetAllFromMultipleRepos() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {

        RepositoryConnection repo2 = fixture2.getWritableConnection();

        // Creates a collection with just one repo
        RepositoryConnectionList repo1Only = new RepositoryConnectionList(repoConnection);
        RepositoryConnectionList repo2Only = new RepositoryConnectionList(repo2);

        // Creates a collection with both repos in (for reading from). Get repo1 from repoConnection
        RepositoryConnectionList bothRepos = new RepositoryConnectionList(repoConnection);
        bothRepos.add(repo2);

        // Add an asset to repo1
        SampleResourceImpl sampleRes1 = createSampleResource();
        sampleRes1.setName("samp1");
        simpleUpload(sampleRes1);

        // Add an asset to repo2
        SampleResourceImpl sampleRes2 = createSampleResource();
        sampleRes2.setName("samp2");
        // Override default so asset goes to repo2
        sampleRes2.setRepositoryConnection(repo2);
        simpleUpload(sampleRes2);

        // Should only get one asset from repo1
        assertEquals("Should only be 1 asset in repo1", 1, repo1Only.getAllResources().size());

        // Should only get one asset from repo2
        assertEquals("Should only be 1 asset in repo2", 1, repo2Only.getAllResources().size());

        // Should get two assets when using both repos
        assertEquals("Should get two assets when using both repos", 2, bothRepos.getAllResources().size());
    }

    @Test
    public void testGetAllFromMultipleReposWithDupes() throws URISyntaxException, RepositoryResourceException, RepositoryBackendException {

        RepositoryConnection repo2 = fixture2.getWritableConnection();

        // Creates a collection with just one repo in
        RepositoryConnectionList repo1Only = new RepositoryConnectionList(repoConnection);
        RepositoryConnectionList repo2Only = new RepositoryConnectionList(repo2);

        // Creates a collection with both repos in (for reading from). Get repo1 from _loginInfo
        RepositoryConnectionList bothRepos = new RepositoryConnectionList(repoConnection);
        bothRepos.add(repo2);

        // Add an asset to repo1
        SampleResourceImpl sampleRes1 = createSampleResource();
        sampleRes1.setName("samp1");
        simpleUpload(sampleRes1);

        // Add an asset to repo1
        SampleResourceImpl sampleRes2 = createSampleResource();
        sampleRes2.setName("samp2");
        simpleUpload(sampleRes2);

        // Add the same asset again to repo2. Override default so asset goes to repo2
        sampleRes2.setRepositoryConnection(repo2);
        simpleUpload(sampleRes2);

        // Should only get one asset from repo1
        assertEquals("Should be 2 assets in repo1", 2, repo1Only.getAllResources().size());

        // Should only get one asset from repo2
        assertEquals("Should only be 1 asset in repo2", 1, repo2Only.getAllResources().size());

        // Should get two assets when using both repos
        assertEquals("Should get two assets when using both repos", 2, bothRepos.getAllResources().size());
    }

    @Test
    public void testWriteJson() throws IOException, RepositoryResourceException {
        RepositoryResourceImpl _testObject = createSampleResource();
        _testObject.setName("wibble");
        _testObject.setLicenseId("lic id");
        _testObject.setDescription("test desc");;
        _testObject.setState(State.DRAFT);

        File jsonFile = File.createTempFile("wibble", "json");
        FileOutputStream fos = new FileOutputStream(jsonFile);
        _testObject.writeDiskRepoJSONToStream(fos);
        logger.log(Level.INFO, "Wrote to " + jsonFile.getAbsolutePath());
    }

    @Test
    public void testGetAppliesToMinimumVersions() throws URISyntaxException {
        SampleResourceImpl sampleResource = createSampleResource();
        sampleResource.setAppliesTo("a;productVersion=8.5.5.6,b;productVersion=\"1.0.0.0\"");
        Set<String> expected = new HashSet<String>();
        expected.add("8.5.5.6");
        expected.add("1.0.0.0");
        assertEquals(expected, sampleResource.getAppliesToMinimumVersions());
        sampleResource.setAppliesTo(null);
        assertEquals(new HashSet<String>(), sampleResource.getAppliesToMinimumVersions());
        sampleResource.setAppliesTo("a");
        assertEquals(new HashSet<String>(), sampleResource.getAppliesToMinimumVersions());
    }

    @Test
    public void testUpdateGeneratedFieldsAndCheckEditionsWithValidEdition() throws RepositoryResourceCreationException, NoSuchFieldException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        executeUpdateGeneratedFieldsTest("BASE", true, true);
    }

    @Test
    public void testUpdateGeneratedFieldsAndCheckEditionsWithInvalidEdition() throws RepositoryResourceCreationException, NoSuchFieldException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        executeUpdateGeneratedFieldsTest("invalid", true, false);
    }

    @Test
    public void testUpdateGeneratedFieldsAndDontCheckEditionsWithValidEdition() throws RepositoryResourceCreationException, NoSuchFieldException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        executeUpdateGeneratedFieldsTest("BASE", false, true);
    }

    @Test
    public void testUpdateGeneratedFieldsAndDontCheckEditionsWithInvalidEdition() throws RepositoryResourceCreationException, NoSuchFieldException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        executeUpdateGeneratedFieldsTest("invalid", false, true);
    }

    @Test
    public void testMainAttachmentSHA256() throws IOException, RepositoryException {
        File contentFile = new File(resourceDir, "TestAttachment.txt");

        String fileHash = HashUtils.getFileSHA256String(contentFile);

        RepositoryResourceImpl mr = new EsaResourceImpl(null);
        mr.addContent(contentFile);

        String contentHash = mr.getMainAttachmentSHA256();

        assertEquals("Main attachment SHA256 hash should be the same as the hash of the file added", fileHash, contentHash);
    }

    /**
     * Run multi-threaded upload of multiple resources
     *
     * @throws RepositoryResourceException
     * @throws RepositoryBackendException
     */
    @Test
    public void testMultiThreadedUploadAndLocking() throws RepositoryBackendException {
        final int VANITY_URLS = 3;
        final int RESOURCES_PER_VANITY_URL = 10;
        List<EsaResourceImpl> list = createRandomizedListOfResources(VANITY_URLS, RESOURCES_PER_VANITY_URL);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (EsaResourceImpl esa : list) {
            Runnable worker = new WorkerThread(esa);
            executor.execute(worker);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Finished all threads");

        int countVisible = 0;
        int countHidden = 0;
        final String highestVersion = "8.5.5." + RESOURCES_PER_VANITY_URL;
        Collection<? extends RepositoryResource> resources = assertResourceCount(30);
        for (RepositoryResource result : resources) {
            EsaResourceWritable esa = (EsaResourceWritable) result;
            if (esa.getAppliesTo().contains(highestVersion)) {
                assertEquals(highestVersion + " should be visible", DisplayPolicy.VISIBLE, esa.getWebDisplayPolicy());
                countVisible++;
            } else {
                assertEquals("other than " + highestVersion + " should be hidden", DisplayPolicy.HIDDEN, esa.getWebDisplayPolicy());
                countHidden++;
            }
        }
        assertEquals("Wrong number of recources visible: ", VANITY_URLS, countVisible);
        assertEquals("Wrong number of recources hidden: ", (VANITY_URLS * RESOURCES_PER_VANITY_URL) - VANITY_URLS, countHidden);
    }

    @Test
    public void testMavenCoords() throws RepositoryResourceException, RepositoryBackendException {
        SampleResourceImpl sample = createSampleResource();
        String sampleCoords = "com.example.mygroup:my-artifact:0.0.1";

        sample.setMavenCoordinates(sampleCoords);
        assertThat(sample.getMavenCoordinates(), is(sampleCoords));

        uploadResource(sample);
        RepositoryResource res = fixture.getAdminConnection().getResource(sample.getId());
        assertThat(res.getMavenCoordinates(), is(sampleCoords));
    }

    /**
     * ------------------------------------------------------------------------------------------------
     * HELPER METHODS
     * ------------------------------------------------------------------------------------------------
     *
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */

    public void executeUpdateGeneratedFieldsTest(String edition, boolean checkEditions,
                                                 boolean expectSuccess) throws NoSuchFieldException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        RepositoryResourceImpl res = new SampleResourceImpl(null);
        Asset ass = new Asset();
        WlpInformation wlpInfo = new WlpInformation();
        wlpInfo.setAppliesTo("com.ibm.fish; productEditions=\"" + edition + "\"");
        ass.setWlpInformation(wlpInfo);
        Field f = RepositoryResourceImpl.class.getDeclaredField("_asset");
        f.setAccessible(true);
        f.set(res, ass);
        try {
            res.updateGeneratedFields(checkEditions);
            if (!expectSuccess) {
                fail("We should have had an exception thrown when we attempted to updateGeneratedFields as the edition is not valid: ");
            }
        } catch (RepositoryResourceCreationException e) {
            if (expectSuccess) {
                fail("We should not have had an exception thrown when we attempted to updateGeneratedFields as the edition is valid " + e);
            }
        }
    }

    private class TestResource extends RepositoryResourceImpl {

        public TestResource(RepositoryConnection loginInfo) {
            super(loginInfo, null);
            setType(ResourceType.PRODUCTSAMPLE);
        }

        @Override
        public Asset getAsset() {
            return _asset;
        }

    }

    /**
     * Creates a string saying when something is created. This can be used to ensure that
     * a newly created asset will not be a match for a prior one to ensure that an add not
     * an update occurs
     *
     * @return
     */
    private String createDateString() {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        String createdString = " created at "
                               + dateFormater.format(Calendar.getInstance().getTime());
        return createdString;
    }

    /**
     * Inner class for multi-threaded testing
     */
    public class WorkerThread implements Runnable {

        private final EsaResourceImpl esa;

        public WorkerThread(EsaResourceImpl esa) {
            this.esa = esa;
        }

        @Override
        public void run() {
            System.out.println(Thread.currentThread().getName() + " Start. resource = " + esa.getName());
            processCommand();
            System.out.println(Thread.currentThread().getName() + " End.");
        }

        private void processCommand() {
            try {
                esa.uploadToMassive(new AddThenHideOldStrategy(State.PUBLISHED, State.PUBLISHED));
            } catch (RepositoryBackendException e) {
                System.out.println("RepositoryBackendException thrown");
                e.printStackTrace();
            } catch (RepositoryResourceException e) {
                System.out.println("RepositoryResourceException thrown");
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a list containing a specified number of features in a random order
     *
     * @param noOfVanityUrls - the number of different vanityUrl features to create
     * @param numberOfResourcesPerVanityUrl - how many of each of them
     * @return
     */
    private List<EsaResourceImpl> createRandomizedListOfResources(int noOfVanityUrls, int numberOfItemsPerVanityUrl) {
        final String APPLIES_TO_STEM = "com.ibm.websphere.appserver; productEdition=\"BASE,BASE_ILAN,DEVELOPERS,EXPRESS,ND,zOS\"; productVersion=8.5.5.";
        final String FEATURE_NAME_STEM = "dummyEsa";
        final String VANITY_URL_STEM = "vanityUrl";

        System.out.println("START Creating list");
        List<EsaResourceImpl> list = new ArrayList<EsaResourceImpl>();

        for (int i = 1; i < noOfVanityUrls + 1; i++) {
            for (int j = 1; j < numberOfItemsPerVanityUrl + 1; j++) {
                EsaResourceImpl esa = new EsaResourceImpl(repoConnection);
                String featureName = FEATURE_NAME_STEM + i + "-v" + j;
                String providesFeature = "Feature" + i;

                esa.setVisibility(Visibility.PUBLIC);
                esa.setWebDisplayPolicy(DisplayPolicy.VISIBLE);
                esa.setProviderName("IBM");
                esa.setName(featureName);
                esa.setAppliesTo(APPLIES_TO_STEM + j);
                esa.setProvideFeature(providesFeature);
                esa.setVanityURL(VANITY_URL_STEM + i);
                esa.setDescription(featureName + " on " + providesFeature);
                list.add(esa);
                System.out.println("CREATING v855" + j + ", vanityUrl=" + VANITY_URL_STEM + i + ", feature=" + providesFeature + ", name=" + esa.getName());
            }
        }

        Collections.shuffle(list);
        System.out.println("END Creating list");
        return list;
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

}
