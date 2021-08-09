/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.repository.transport.client.test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.json.stream.JsonGenerationException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.FileRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.LooseFileRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.SingleFileRepositoryFixture;
import com.ibm.ws.lars.testutils.fixtures.ZipRepositoryFixture;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.StateAction;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;
import com.ibm.ws.repository.transport.client.RestClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.ClientFailureException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.AppliesToFilterInfo;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentInfo;
import com.ibm.ws.repository.transport.model.AttachmentSummary;
import com.ibm.ws.repository.transport.model.FilterVersion;
import com.ibm.ws.repository.transport.model.WlpInformation;

public abstract class RepositoryClientTest {

    @Rule
    public RepositoryFixture fixture = null;

    protected RepositoryReadableClient _client;
    protected RepositoryWriteableClient _writeableClient; // Used for creating the repo and tidying up
    protected RepositoryReadableClient _unauthenticatedClient;

    @Before
    public void setup() {
        _client = fixture.getAdminClient();
        _writeableClient = fixture.getWriteableClient();
        _unauthenticatedClient = fixture.getUserClient();
    }

    public static class DirectoryRepositoryClientTest extends RepositoryClientTest {
        public DirectoryRepositoryClientTest() {
            fixture = FileRepositoryFixture.createFixture(new File("testFileRepo"));
        }
    }

    public static class ZipRepositoryClientTest extends RepositoryClientTest {
        public ZipRepositoryClientTest() {
            fixture = ZipRepositoryFixture.createFixture(new File("testZipRepo.zip"));
        }
    }

    public static class LooseFileRepositoryClientTest extends RepositoryClientTest {
        public LooseFileRepositoryClientTest() {
            fixture = LooseFileRepositoryFixture.createFixture(new File("testLooseRepo"));
        }
    }

    public static class SingleFileRepositoryClientTest extends RepositoryClientTest {
        public SingleFileRepositoryClientTest() {
            fixture = SingleFileRepositoryFixture.createFixture(new File("testSingleFileRepo"));
        }
    }

    public static class RestRepositoryClientTest extends RepositoryClientTest {
        public RestRepositoryClientTest() {
            fixture = FatUtils.getRestFixture();
        }
    }

    /**
     * This method will test adding an asset and then deleting it
     *
     * @throws IOException
     */
    @Test
    public void testAddAndDeleteAsset() throws Exception {
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        assertNotNull("The ID should of been set by massive",
                      createdAsset.get_id());
        assertEquals(
                     "The returned asset name should match the supplied one",
                     newAsset.getName(), createdAsset.getName());
    }

    /**
     * This test makes sure that we can get assets from Massive
     *
     * @throws IOException
     */
    @Test
    public void testGetAllAssets() throws IOException, RequestFailureException {
        _client.getAllAssets();
        /*
         * We don't have any consistent test data in there at the moment, if we
         * get this far call it a pass!
         */
    }

    /**
     * This method will test adding an asset and then getting it
     *
     * @throws IOException
     */
    @Test
    public void testGetAsset() throws Exception {
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        Asset gotAsset = _client.getAsset(createdAsset.get_id());
        assertEquals(
                     "The ID should be the same of the got asset as the one created originally",
                     createdAsset.get_id(), gotAsset.get_id());
        assertEquals(
                     "The returned asset name should match the supplied one",
                     newAsset.getName(), gotAsset.getName());
    }

    /**
     * This tests that custom attributes are created in Massive
     */
    @Test
    public void testCustomAttributes() throws Exception {
        Asset newAsset = createTestAsset();

        // Add custom attributes
        WlpInformation wlpInfo = new WlpInformation();
        String appliesTo = "Test applies to";
        wlpInfo.setAppliesTo(appliesTo);
        newAsset.setWlpInformation(wlpInfo);
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        WlpInformation createdWlpInfo = createdAsset.getWlpInformation();
        assertNotNull("The created asset should contain wlp infromation",
                      createdWlpInfo);
        assertEquals(
                     "Fields set on the wlp information should match in the returned object",
                     appliesTo, createdWlpInfo.getAppliesTo());
    }

    @Test
    public void testAttachmentEquivalent() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        Attachment createdAttachment1 = null;
        Attachment createdAttachment2 = null;
        Attachment createdAttachment3 = null;
        Attachment createdAttachment4 = null;
        Attachment createdAttachment5 = null;
        Attachment createdAttachment6 = null;
        Attachment createdAttachment7 = null;
        Attachment createdAttachment8 = null;
        Attachment createdAttachment9 = null;
        Attachment createdAttachment10 = null;

        // Add the attachment
        createdAttachment1 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);
        createdAttachment2 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);
        assertTrue(createdAttachment1.equivalent(createdAttachment2));
        assertFalse(createdAttachment1.equals(createdAttachment2));

        // When we supply a URL (same one each time) then the attachments should be equivalent
        createdAttachment3 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "htpp://url");
        createdAttachment4 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "htpp://url");
        assertTrue(createdAttachment3.equivalent(createdAttachment4));
        assertFalse(createdAttachment3.equals(createdAttachment4));

        // The URLs are different but if link type is null then it's treated as equivalent (as we may copy attachments
        // that are stored in massive and get different URL's for attachments we want to this of as equivalent)
        createdAttachment5 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "apple");
        createdAttachment5.setLinkType(null);
        createdAttachment6 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "pear");
        createdAttachment6.setLinkType(null);
        assertTrue(createdAttachment5.equivalent(createdAttachment6));
        assertFalse(createdAttachment5.equals(createdAttachment6));

        // This time one of the link types is non null so equiv should fail
        createdAttachment7 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "apple");
        createdAttachment8 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "pear");
        createdAttachment8.setLinkType(null);
        assertFalse(createdAttachment7.equivalent(createdAttachment8));
        assertFalse(createdAttachment7.equals(createdAttachment8));

        // Check with other link type being non null
        createdAttachment9 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "apple");
        createdAttachment9.setLinkType(null);
        createdAttachment10 = addAttachment(createdAsset.get_id(),
                                            "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "pear");
        assertFalse(createdAttachment9.equivalent(createdAttachment10));
        assertFalse(createdAttachment9.equals(createdAttachment10));
    }

    @Test
    public void testAddAttachmentWithInfo() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        Attachment createdAttachment = null;
        // Add the attachment
        final Attachment at = new Attachment();
        at.setWlpInformation(new AttachmentInfo());
        at.getWlpInformation().setCRC(2345);
        createdAttachment = addAttachment(createdAsset.get_id(),
                                          "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 2345);
        assertEquals("The attachment asset ID should match the asset ID",
                     createdAsset.get_id(), createdAttachment.getAssetId());
        assertNotNull("The URL should of been set by massive",
                      createdAttachment.getUrl());
        assertEquals("The CRC value in the AttachmentInfo didn't match", 2345,
                     createdAttachment.getWlpInformation().getCRC());
    }

    /**
     * Tests equivalence between two assets with the same attachment
     *
     * @throws Exception
     */
    @Test
    public void testAssetEquivalent() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset1 = _writeableClient.addAsset(newAsset);
        Asset createdAsset2 = _writeableClient.addAsset(newAsset);
        Attachment createdAttachment1 = null;
        Attachment createdAttachment2 = null;
        Attachment createdAttachment3 = null;
        Attachment createdAttachment4 = null;
        Attachment createdAttachment5 = null;
        Attachment createdAttachment6 = null;

        createdAttachment1 = addAttachment(createdAsset1.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "http://url");
        createdAttachment2 = addAttachment(createdAsset2.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "http://url");

        // add attachment to asset
        createdAsset1.addAttachement(createdAttachment1);
        createdAsset2.addAttachement(createdAttachment2);
        // Attachments are equivalent as they have the same url
        assertTrue(createdAsset1.equivalent(createdAsset2));
        assertFalse(createdAsset1.equals(createdAsset2));

        // Add the attachment to massive
        createdAttachment3 = addAttachment(createdAsset1.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);
        createdAttachment4 = addAttachment(createdAsset2.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);

        // add attachment to asset
        createdAsset1.addAttachement(createdAttachment3);
        createdAsset2.addAttachement(createdAttachment4);
        // Attachments are equivalent as both attachments have generated URLs, and when they are generated (linkType
        // is set to null) then we don't check the URLs
        assertTrue(createdAsset1.equivalent(createdAsset2));
        assertFalse(createdAsset1.equals(createdAsset2));

        // Add the attachment to massive
        createdAttachment5 = addAttachment(createdAsset1.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, "apple");
        createdAttachment6 = addAttachment(createdAsset2.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);
        // add attachment to asset
        createdAsset1.addAttachement(createdAttachment5);
        createdAsset2.addAttachement(createdAttachment6);
        // Attachments are no longer equivalent as as the URLs are different now (one is using a generated one and
        // other is hardcoded to use one)
        assertFalse(createdAsset1.equivalent(createdAsset2));
        assertFalse(createdAsset1.equals(createdAsset2));
    }

    /**
     * Tests inequivalence between two attachments with the same content but
     * of different attachment types.
     *
     * @throws Exception
     */
    @Test
    public void testAttachmentNotEquivalent() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        Attachment createdAttachment1 = null;
        Attachment createdAttachment2 = null;

        // Add the attachments to massive using different attachment types
        createdAttachment1 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, AttachmentType.CONTENT, null);
        createdAttachment2 = addAttachment(createdAsset.get_id(),
                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0, AttachmentType.DOCUMENTATION, null);

        // add attachment to asset
        createdAsset.addAttachement(createdAttachment1);
        createdAsset.addAttachement(createdAttachment2);
        assertFalse(createdAttachment1.equivalent(createdAttachment2)); // Type is different
        assertFalse(createdAttachment1.equals(createdAttachment2));
    }

    /**
     * Test that adding an attachment without an attachment type specified
     * causes an exception
     *
     * @throws Exception
     */
    @Test
    public void testAttachmentNoTypeSpecified() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);

        try {
            // Add the attachments to massive without specifying an Attachment.Type
            addAttachment(createdAsset.get_id(), "TestAttachment.txt",
                          new File(getResourcesDir(), "TestAttachment.txt"), 0, null);
        } catch (IllegalArgumentException iae) {
            // Expected
        } catch (Exception e) {
            fail("Unexpected Exception caught " + e);
        }
    }

    /**
     * Try adding an attachment after creating an asset
     *
     * @throws IOException
     */
    @Test
    public void testAddAttachment() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        String name = "TestAttachment.txt";
        Asset newAsset = createTestAsset();
        // For directory based repos the name should match the main attachment file name
        newAsset.setName(name);
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        Attachment createdAttachment = null;

        File attachmentFile = new File(getResourcesDir(), name);
        System.out.println("Adding attachment: " + attachmentFile);
        // Add the attachment
        createdAttachment = addAttachment(createdAsset.get_id(),
                                          "TestAttachment.txt", attachmentFile, 0);
        assertEquals("The attachment asset ID should match the asset ID",
                     createdAsset.get_id(), createdAttachment.getAssetId());
        assertNotNull("The URL should of been set by massive",
                      createdAttachment.getUrl());

        // Get the attachment content to make sure it was added ok
        InputStream attachmentContentStream = _client.getAttachment(createdAsset, createdAttachment);
        BufferedReader attachmentContentReader = new BufferedReader(new InputStreamReader(attachmentContentStream));
        assertEquals("The content in the attachment should be the same as what was uploaded",
                     "This is a test attachment",
                     attachmentContentReader.readLine());
        attachmentContentReader.close();
    }

    @Test
    public void testGetAllAssetsDoesntGetAttachments() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        addAttachment(createdAsset.get_id(),
                      "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);

        Collection<Asset> allAssets = _client.getAllAssets();
        for (Asset theAsset : allAssets) {
            List<Attachment> noAttachments = theAsset.getAttachments();
            assertNull("There should not be any attachments from a getAllAssets call", noAttachments);
        }
    }

    @Test
    public void testGetAssetGetsAttachments() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        Attachment createdAttachment = null;

        // Add the attachment
        createdAttachment = addAttachment(createdAsset.get_id(),
                                          "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);

        Asset ass = _client.getAsset(createdAsset.get_id());
        assertEquals("There should be 1 attachments from a getAsset call", 1,
                     ass.getAttachments().size());
        assertEquals("The attachment asset ID should match the asset ID",
                     createdAsset.get_id(), createdAttachment.getAssetId());

        Attachment readAttachment = ass.getAttachments().get(0);

        assertNotNull("The URL should of been set by massive",
                      readAttachment.getUrl());

        // Get the attachment content to make sure it was added ok
        InputStream attachmentContentStream = _client.getAttachment(createdAsset, readAttachment);
        BufferedReader attachmentContentReader = new BufferedReader(new InputStreamReader(attachmentContentStream));
        assertEquals(
                     "The content in the attachment should be the same as what was uploaded",
                     "This is a test attachment",
                     attachmentContentReader.readLine());

        attachmentContentReader.close();

        System.out.println("asset = " + ass.get_id());
        _writeableClient.updateState(ass.get_id(), StateAction.PUBLISH);
        _writeableClient.updateState(ass.get_id(), StateAction.APPROVE);
    }

    /**
     * Make sure we can delete an asset and all its attachments
     *
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     */
    @Test
    public void testDeleteAssetAndAttachments() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachments to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);

        // Add the attachments
        Attachment createdAttachment1 = addAttachment(createdAsset.get_id(), "TestAttachment.txt",
                                                      new File(getResourcesDir(), "TestAttachment.txt"), 0);
        Attachment createdAttachment2 = addAttachment(createdAsset.get_id(), "TestAttachment2.txt",
                                                      new File(getResourcesDir(), "TestAttachment.txt"), 0);

        // Now delete the lot and make sure we can't access them any more
        _writeableClient.deleteAssetAndAttachments(createdAsset.get_id());

        try {
            _client.getAttachment(createdAsset, createdAttachment1);
            fail("Attachment 1 should've been deleted");
        } catch (Exception e) {
            // pass
        }

        try {
            _client.getAttachment(createdAsset, createdAttachment2);
            fail("Attachment 2 should've been deleted");
        } catch (Exception e) {
            // pass
        }

        // And make sure the asset was deleted
        try {
            _client.getAsset(createdAsset.get_id());
            fail("Asset should've been deleted");
        } catch (Exception e) {
            // pass
        }
    }

    /**
     * Test to make sure you can update an asset
     *
     * @throws IOException
     */
    @Test
    public void testUpdate() throws Exception {
        assumeThat(fixture.isUpdateSupported(), is(true));

        Asset asset = createTestAsset();
        asset = _writeableClient.addAsset(asset);
        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        String updatedName = "updatedName";
        asset.setName(updatedName);
        Asset updatedAsset = _writeableClient.updateAsset(asset);
        assertEquals("The asset name should've been updated", updatedName,
                     updatedAsset.getName());
    }

    /**
     * Test to make sure you can update an attachment on an asset
     *
     * @throws IOException
     */
    @Test
    public void testUpdateAttachment() throws Exception {
        assumeThat(fixture.isAttachmentSupported(), is(true));
        // Add an asset that we'll add the attachment to
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        final Attachment createdAttachment = addAttachment(createdAsset.get_id(),
                                                           "TestAttachment.txt", new File(getResourcesDir(), "TestAttachment.txt"), 0);;

        // Get the attachment content to make sure it was added ok

        InputStream attachmentContentStream = null;
        BufferedReader attachmentContentReader = null;

        try {
            attachmentContentStream = _client.getAttachment(createdAsset, createdAttachment);
            attachmentContentReader = new BufferedReader(new InputStreamReader(attachmentContentStream));
            assertEquals(
                         "The content in the attachment should be the same as what was uploaded",
                         "This is a test attachment",
                         attachmentContentReader.readLine());
        } finally {
            if (attachmentContentStream != null) {
                attachmentContentStream.close();
            }
            if (attachmentContentReader != null) {
                attachmentContentReader.close();
            }
        }

        // Now update
        AttachmentSummary attachSummary = new MockAttachmentSummary(new File(getResourcesDir(), "TestAttachmentUpdated.txt"), "TestAttachment.txt", AttachmentType.CONTENT, 0, null);
        attachSummary.getAttachment().set_id(createdAttachment.get_id());

        Attachment updatedAttachment = _writeableClient.updateAttachment(createdAsset.get_id(), attachSummary);

        try {
            attachmentContentStream = _client.getAttachment(createdAsset, updatedAttachment);
            attachmentContentReader = new BufferedReader(new InputStreamReader(attachmentContentStream));
            assertEquals(
                         "The content in the attachment should be the same as the udpated file",
                         "This is an updated test attachment",
                         attachmentContentReader.readLine());
        } finally {
            if (attachmentContentStream != null) {
                attachmentContentStream.close();
            }
            if (attachmentContentReader != null) {
                attachmentContentReader.close();
            }
        }
    }

    /**
     * This test will make sure you can get an asset by it's {@link WlpInformation.ResourceType}.
     *
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonGenerationException
     */
    @Test
    public void testGetByType() throws Exception {
        int initialEsaCount = _client.getAssets(ResourceType.FEATURE).size();
        int initialAddonCount = _client.getAssets(ResourceType.ADDON).size();
        int initialTotalCount = _client.getAllAssets().size();

        // Add 3 assets with different types (including null)
        String esaAssetName = "ESA Asset";
        String addonAssetName = "Addon Asset";
        Asset asset = new Asset();
        asset.setName(esaAssetName);
        WlpInformation wlpInformation = new WlpInformation();
        asset.setType(ResourceType.FEATURE);
        asset.setWlpInformation(wlpInformation);
        _writeableClient.addAsset(asset);

        asset = new Asset();
        asset.setName(addonAssetName);
        wlpInformation = new WlpInformation();
        asset.setType(ResourceType.ADDON);
        asset.setWlpInformation(wlpInformation);
        _writeableClient.addAsset(asset);

        asset = new Asset();
        asset.setName(addonAssetName);
        _writeableClient.addAsset(asset);

        // Now make sure we get the right ones back when we call the method
        Collection<Asset> esas = _client.getAssets(ResourceType.FEATURE);
        assertEquals("Only one esa was added", initialEsaCount + 1, esas.size());
        boolean found = false;
        for (Asset esa : esas) {
            if (esaAssetName.equals(esa.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("The name of the esa should match the supplied value", found);

        Collection<Asset> addons = _client.getAssets(ResourceType.ADDON);
        assertEquals("Only one addon was added", initialAddonCount + 1, addons.size());
        found = false;
        for (Asset addon : addons) {
            if (addonAssetName.equals(addon.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("The name of the addon should match the supplied value", found);

        Collection<Asset> allAssets = _client.getAllAssets();
        assertEquals("Three assets were added in total", initialTotalCount + 3, allAssets.size());

        assertEquals("Passing in null should get all assets", initialTotalCount + 3, _client.getAssets(null).size());
    }

    /**
     * Add an asset and ensure it can be read back without being authenticated
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void testUnauthReadOne() throws Exception {

        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        _writeableClient.updateState(createdAsset.get_id(), StateAction.PUBLISH);
        _writeableClient.updateState(createdAsset.get_id(), StateAction.APPROVE);

        // We're doing lots of asserts so make sure it is definitely tidied if
        // one of these fails
        Asset gotAsset = _unauthenticatedClient.getAsset(createdAsset.get_id());
        assertEquals("The ID should be the same of the got asset as the one created originally",
                     createdAsset.get_id(), gotAsset.get_id());
        assertEquals("The returned asset name should match the supplied one",
                     newAsset.getName(), gotAsset.getName());
    }

    /**
     * Add an asset and ensure it can be read back without being authenticated
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @Test
    public void testUnauthReadAttachment() throws Exception {

        assumeThat(fixture.isAttachmentSupported(), is(true));
        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        final Attachment createdAttachment = new Attachment();
        createdAttachment.setType(AttachmentType.CONTENT);
        createdAttachment.setWlpInformation(new AttachmentInfo());
        createdAttachment.getWlpInformation().setCRC(0);

        // Add the attachment
        _writeableClient.addAttachment(createdAsset.get_id(),
                                       new AttachmentSummary() {

                                           @Override
                                           public String getName() {
                                               return "TestAttachment.txt";
                                           }

                                           @Override
                                           public File getFile() {
                                               return new File(getResourcesDir(), "TestAttachment.txt");
                                           }

                                           @Override
                                           public String getURL() {
                                               return null;
                                           }

                                           @Override
                                           public Attachment getAttachment() {
                                               return createdAttachment;
                                           }

                                           @Override
                                           public Locale getLocale() {
                                               return null;
                                           }
                                       });

        _writeableClient.updateState(createdAsset.get_id(), StateAction.PUBLISH);
        _writeableClient.updateState(createdAsset.get_id(), StateAction.APPROVE);

        Asset gotAsset = _unauthenticatedClient.getAsset(createdAsset.get_id());
        assertEquals("The ID should be the same of the got asset as the one created originally",
                     createdAsset.get_id(), gotAsset.get_id());
        assertEquals("The returned asset name should match the supplied one",
                     newAsset.getName(), gotAsset.getName());

        List<Attachment> attachments = gotAsset.getAttachments();
        for (Attachment att : attachments) {
            InputStream is = _unauthenticatedClient.getAttachment(gotAsset, att);
            assertNotNull("Unexpected null InputStream returned", is);
            is.close();
        }
    }

    /**
     * Test that if we don't recognize the content then we throw an exception for a single get by ID
     *
     * @throws RequestFailureException
     * @throws IOException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testVerificationSingleObject() throws IOException, RequestFailureException, SecurityException, ClientFailureException {
        Asset asset = new Asset();
        WlpInformation wlpInformation = new WlpInformation();
        String version = Float.toString(WlpInformation.MAX_VERSION + 1);
        wlpInformation.setWlpInformationVersion(version);
        asset.setWlpInformation(wlpInformation);
        try {
            Asset returnedAsset = _writeableClient.addAsset(asset);
            String assetId = returnedAsset.get_id();
            _client.getAsset(assetId);
            fail("Should have thrown a bad version exception");
        } catch (BadVersionException e) {
            // This is correct
            assertEquals("The version in the exception should match that set on the wlpInformation", version, e.getBadVersion());
            assertEquals("The min version in the exception should match that set on the wlpInformation", Float.toString(WlpInformation.MIN_VERSION), e.getMinVersion());
            assertEquals("The max version in the exception should match that set on the wlpInformation", Float.toString(WlpInformation.MAX_VERSION), e.getMaxVersion());
        }
    }

    /**
     * Test that if we don't recognize the content then we ignore it when doing a get all
     *
     * @throws RequestFailureException
     * @throws IOException
     * @throws BadVersionException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testVerificationMultipleObjects() throws IOException, RequestFailureException, BadVersionException, SecurityException, ClientFailureException {
        Asset invalidAsset = new Asset();
        invalidAsset.setName("Invalid");
        WlpInformation invalidWlpInformation = new WlpInformation();
        invalidWlpInformation.setWlpInformationVersion(Float.toString(WlpInformation.MAX_VERSION + 1));
        invalidAsset.setWlpInformation(invalidWlpInformation);

        Asset validAsset = new Asset();
        validAsset.setName("Valid");
        WlpInformation validWlpInformation = new WlpInformation();
        validWlpInformation.setWlpInformationVersion(Float.toString(WlpInformation.MIN_VERSION));
        validAsset.setWlpInformation(validWlpInformation);
        try {
            _writeableClient.addAsset(invalidAsset);
        } catch (BadVersionException e) {
            // This is expected as add tries to reload the asset and it is invalid
        }
        _writeableClient.addAsset(validAsset);
        Collection<Asset> allAssets = _client.getAllAssets();
        assertEquals("Expected to get just the single valid asset back" + allAssets, 1, allAssets.size());
        assertEquals("Expected to get just the valid asset back", "Valid", allAssets.iterator().next().getName());
    }

    @Test
    public void testSettingVisibilityToInstaller() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset asset = new Asset();
        WlpInformation wlpInformation = new WlpInformation();
        wlpInformation.setVisibility(Visibility.INSTALL);
        asset.setWlpInformation(wlpInformation);
        Asset returnedAsset = _writeableClient.addAsset(asset);
        assertEquals("Expected the visibility to be read back correctly", Visibility.INSTALL, returnedAsset.getWlpInformation().getVisibility());
    }

    /**
     * Test you can filter by multiple types
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringByTypes() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset feature = createTestAsset();
        feature.setType(ResourceType.FEATURE);
        feature = _writeableClient.addAsset(feature);

        Asset sample = createTestAsset();
        sample.setType(ResourceType.PRODUCTSAMPLE);
        sample = _writeableClient.addAsset(sample);

        Asset install = createTestAsset();
        install.setType(ResourceType.INSTALL);
        install = _writeableClient.addAsset(install);

        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.FEATURE);
        types.add(ResourceType.PRODUCTSAMPLE);
        Collection<Asset> assets = _client.getAssets(types, null, null, null);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature", assets.contains(feature));
        assertTrue("Should get back the feature", assets.contains(sample));
    }

    /**
     * Tests you can filter by min version
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringByMinVersion() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset assetWithRightVersion = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        FilterVersion minVersion = new FilterVersion();
        minVersion.setValue("8.5.5.4");
        filterInfo.setMinVersion(minVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightVersion.setWlpInformation(wlpInfo);
        assetWithRightVersion = _writeableClient.addAsset(assetWithRightVersion);

        Asset assetWithNoVersion = createTestAsset();
        assetWithNoVersion = _writeableClient.addAsset(assetWithNoVersion);

        Collection<Asset> assets = _client.getAssets(null, null, null, Collections.singleton("8.5.5.4"));
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithRightVersion));
    }

    /**
     * Tests you can filter by having no visibility
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringByVisibility() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset assetWithRightVisibility = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        wlpInfo.setVisibility(Visibility.PUBLIC);
        assetWithRightVisibility.setWlpInformation(wlpInfo);
        assetWithRightVisibility = _writeableClient.addAsset(assetWithRightVisibility);

        Asset assetWithWrongVisiblity = createTestAsset();
        WlpInformation wlpInfoWrongVisibility = new WlpInformation();
        wlpInfoWrongVisibility.setVisibility(Visibility.PRIVATE);
        assetWithWrongVisiblity.setWlpInformation(wlpInfoWrongVisibility);
        assetWithWrongVisiblity = _writeableClient.addAsset(assetWithWrongVisiblity);

        Collection<Asset> assets = _client.getAssets(null, null, Visibility.PUBLIC, null);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithRightVisibility));
    }

    /**
     * Tests you can filter by having two product IDs
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringByMultipleProductIds() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset assetWithRightProduct1 = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct1");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightProduct1.setWlpInformation(wlpInfo);
        assetWithRightProduct1 = _writeableClient.addAsset(assetWithRightProduct1);

        Asset assetWithRightProduct2 = createTestAsset();
        filterInfo.setProductId("correct2");
        assetWithRightProduct2.setWlpInformation(wlpInfo);
        assetWithRightProduct2 = _writeableClient.addAsset(assetWithRightProduct2);

        Asset assetWithIncorrectId = createTestAsset();
        filterInfo.setProductId("incorrect");
        assetWithIncorrectId.setWlpInformation(wlpInfo);
        assetWithIncorrectId = _writeableClient.addAsset(assetWithIncorrectId);

        Collection<String> rightProductIds = new HashSet<String>();
        rightProductIds.add("correct1");
        rightProductIds.add("correct2");
        Collection<Asset> assets = _client.getAssets(null, rightProductIds, null, null);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature with ID correct1", assets.contains(assetWithRightProduct1));
        assertTrue("Should get back the feature with ID correct2", assets.contains(assetWithRightProduct2));
    }

    /**
     * Tests you can filter by having two product IDs with different product versions
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringByMultipleProductIdsAndVersions() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset assetWithRightProduct1 = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct1");
        FilterVersion minVersion = new FilterVersion();
        minVersion.setValue("8.5.5.4");
        filterInfo.setMinVersion(minVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightProduct1.setWlpInformation(wlpInfo);
        assetWithRightProduct1 = _writeableClient.addAsset(assetWithRightProduct1);

        Asset assetWithRightProduct2 = createTestAsset();
        filterInfo.setProductId("correct2");
        minVersion.setValue("1.0.0.0");
        assetWithRightProduct2.setWlpInformation(wlpInfo);
        assetWithRightProduct2 = _writeableClient.addAsset(assetWithRightProduct2);

        Asset assetWithRightProduct2ButWrongVersion = createTestAsset();
        filterInfo.setProductId("correct2");
        minVersion.setValue("2.0.0.0");
        assetWithRightProduct2ButWrongVersion.setWlpInformation(wlpInfo);
        assetWithRightProduct2ButWrongVersion = _writeableClient.addAsset(assetWithRightProduct2ButWrongVersion);
        minVersion.setValue("1.0.0.0");

        Asset assetWithIncorrectId = createTestAsset();
        filterInfo.setProductId("incorrect");
        assetWithIncorrectId.setWlpInformation(wlpInfo);
        assetWithIncorrectId = _writeableClient.addAsset(assetWithIncorrectId);

        Collection<String> rightProductIds = new HashSet<String>();
        rightProductIds.add("correct1");
        rightProductIds.add("correct2");
        Collection<String> rightVersions = new HashSet<String>();
        rightVersions.add("1.0.0.0");
        rightVersions.add("8.5.5.4");
        Collection<Asset> assets = _client.getAssets(null, rightProductIds, null, rightVersions);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature with ID correct1", assets.contains(assetWithRightProduct1));
        assertTrue("Should get back the feature with ID correct2", assets.contains(assetWithRightProduct2));
    }

    /**
     * Tests you can filter by having different fields set
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringByMultipleFields() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset featureWithRightVisibilityAndVersion = createTestAsset();
        featureWithRightVisibilityAndVersion.setType(ResourceType.FEATURE);
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct");
        FilterVersion minVersion = new FilterVersion();
        minVersion.setValue("8.5.5.4");
        filterInfo.setMinVersion(minVersion);
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        wlpInfo.setVisibility(Visibility.PUBLIC);
        featureWithRightVisibilityAndVersion.setWlpInformation(wlpInfo);
        featureWithRightVisibilityAndVersion = _writeableClient.addAsset(featureWithRightVisibilityAndVersion);

        Asset featureWithWrongVisiblity = createTestAsset();
        featureWithWrongVisiblity.setType(ResourceType.FEATURE);
        wlpInfo.setVisibility(Visibility.PRIVATE);
        featureWithWrongVisiblity.setWlpInformation(wlpInfo);
        featureWithWrongVisiblity = _writeableClient.addAsset(featureWithWrongVisiblity);
        wlpInfo.setVisibility(Visibility.PUBLIC);

        Asset featureWithWrongVersion = createTestAsset();
        featureWithWrongVersion.setType(ResourceType.FEATURE);
        minVersion.setValue("8.5.5.0");
        featureWithWrongVersion.setWlpInformation(wlpInfo);
        featureWithWrongVersion = _writeableClient.addAsset(featureWithWrongVersion);
        minVersion.setValue("8.5.5.4");

        Asset featureWithWrongProduct = createTestAsset();
        featureWithWrongVersion.setType(ResourceType.FEATURE);
        filterInfo.setProductId("incorrect");
        featureWithWrongProduct.setWlpInformation(wlpInfo);
        featureWithWrongProduct = _writeableClient.addAsset(featureWithWrongProduct);

        Asset install = createTestAsset();
        install.setType(ResourceType.INSTALL);
        install = _writeableClient.addAsset(install);

        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.FEATURE);
        Collection<Asset> assets = _client.getAssets(types, Collections.singleton("correct"), Visibility.PUBLIC, Collections.singleton("8.5.5.4"));
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(featureWithRightVisibilityAndVersion));
    }

    /**
     * Tests that you can filter for an asset with no max version
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringForNoMaxVersion() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset assetWithMaxVersion = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        FilterVersion maxVersion = new FilterVersion();
        maxVersion.setValue("8.5.5.4");
        filterInfo.setMaxVersion(maxVersion);
        filterInfo.setHasMaxVersion("true");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithMaxVersion.setWlpInformation(wlpInfo);
        assetWithMaxVersion = _writeableClient.addAsset(assetWithMaxVersion);

        Asset assetWithNoMaxVersion = createTestAsset();
        filterInfo.setMaxVersion(null);
        filterInfo.setHasMaxVersion("false");
        assetWithNoMaxVersion.setWlpInformation(wlpInfo);
        assetWithNoMaxVersion = _writeableClient.addAsset(assetWithNoMaxVersion);

        Collection<Asset> assets = _client.getAssetsWithUnboundedMaxVersion(null, null, null);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithNoMaxVersion));
    }

    /**
     * Tests that you can filter for an asset with no max version
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringForNoMaxVersionWithProductId() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset assetWithMaxVersion = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct");
        FilterVersion maxVersion = new FilterVersion();
        maxVersion.setValue("8.5.5.4");
        filterInfo.setMaxVersion(maxVersion);
        filterInfo.setHasMaxVersion("true");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithMaxVersion.setWlpInformation(wlpInfo);
        assetWithMaxVersion = _writeableClient.addAsset(assetWithMaxVersion);

        Asset assetWithNoMaxVersion = createTestAsset();
        filterInfo.setMaxVersion(null);
        filterInfo.setHasMaxVersion("false");
        assetWithNoMaxVersion.setWlpInformation(wlpInfo);
        assetWithNoMaxVersion = _writeableClient.addAsset(assetWithNoMaxVersion);

        Asset assetWithNoMaxVersionWrongProduct = createTestAsset();
        filterInfo.setMaxVersion(null);
        filterInfo.setHasMaxVersion("false");
        filterInfo.setProductId("incorrect");
        assetWithNoMaxVersionWrongProduct.setWlpInformation(wlpInfo);
        assetWithNoMaxVersionWrongProduct = _writeableClient.addAsset(assetWithNoMaxVersionWrongProduct);

        Collection<Asset> assets = _client.getAssetsWithUnboundedMaxVersion(null, Collections.singleton("correct"), null);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature", assets.contains(assetWithNoMaxVersion));
    }

    /**
     * Tests you can filter by having two product IDs
     *
     * @throws IOException
     * @throws BadVersionException
     * @throws RequestFailureException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringByMultipleProductIdsAndNoMaxVersion() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset assetWithRightProduct1 = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct1");
        filterInfo.setHasMaxVersion("false");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        assetWithRightProduct1.setWlpInformation(wlpInfo);
        assetWithRightProduct1 = _writeableClient.addAsset(assetWithRightProduct1);

        Asset assetWithRightProduct2 = createTestAsset();
        filterInfo.setProductId("correct2");
        assetWithRightProduct2.setWlpInformation(wlpInfo);
        assetWithRightProduct2 = _writeableClient.addAsset(assetWithRightProduct2);

        Asset assetWithIncorrectId = createTestAsset();
        filterInfo.setProductId("incorrect");
        assetWithIncorrectId.setWlpInformation(wlpInfo);
        assetWithIncorrectId = _writeableClient.addAsset(assetWithIncorrectId);

        Collection<String> rightProductIds = new HashSet<String>();
        rightProductIds.add("correct1");
        rightProductIds.add("correct2");
        Collection<Asset> assets = _client.getAssetsWithUnboundedMaxVersion(null, rightProductIds, null);
        assertEquals("Two assets should be obtained", 2, assets.size());
        assertTrue("Should get back the feature with ID correct1", assets.contains(assetWithRightProduct1));
        assertTrue("Should get back the feature with ID correct2", assets.contains(assetWithRightProduct2));
    }

    /**
     * Tests that you can filter for an asset with no max version, type and visibility
     *
     * @throws RequestFailureException
     * @throws BadVersionException
     * @throws IOException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilteringForNoMaxVersionTypeAndVisibility() throws IOException, BadVersionException, RequestFailureException, SecurityException, ClientFailureException {
        Asset featureWithMaxVersion = createTestAsset();
        featureWithMaxVersion.setType(ResourceType.FEATURE);
        WlpInformation wlpInfo = new WlpInformation();
        wlpInfo.setVisibility(Visibility.PUBLIC);
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("correct");
        FilterVersion maxVersion = new FilterVersion();
        maxVersion.setValue("8.5.5.4");
        filterInfo.setMaxVersion(maxVersion);
        filterInfo.setHasMaxVersion("true");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        featureWithMaxVersion.setWlpInformation(wlpInfo);
        featureWithMaxVersion = _writeableClient.addAsset(featureWithMaxVersion);

        Asset featureWithNoMaxVersion = createTestAsset();
        featureWithNoMaxVersion.setType(ResourceType.FEATURE);
        filterInfo.setMaxVersion(null);
        filterInfo.setHasMaxVersion("false");
        featureWithNoMaxVersion.setWlpInformation(wlpInfo);
        featureWithNoMaxVersion = _writeableClient.addAsset(featureWithNoMaxVersion);

        Asset featureWithNoMaxVersionWrongVisibility = createTestAsset();
        featureWithNoMaxVersionWrongVisibility.setType(ResourceType.FEATURE);
        wlpInfo.setVisibility(Visibility.PRIVATE);
        featureWithNoMaxVersionWrongVisibility.setWlpInformation(wlpInfo);
        featureWithNoMaxVersionWrongVisibility = _writeableClient.addAsset(featureWithNoMaxVersionWrongVisibility);

        Asset assetWithNoMaxVersionWrongProduct = createTestAsset();
        filterInfo.setMaxVersion(null);
        filterInfo.setHasMaxVersion("false");
        filterInfo.setProductId("incorrect");
        assetWithNoMaxVersionWrongProduct.setWlpInformation(wlpInfo);
        assetWithNoMaxVersionWrongProduct = _writeableClient.addAsset(assetWithNoMaxVersionWrongProduct);

        Asset install = createTestAsset();
        install.setType(ResourceType.INSTALL);
        install = _writeableClient.addAsset(install);

        Collection<ResourceType> types = new HashSet<ResourceType>();
        types.add(ResourceType.FEATURE);
        Collection<Asset> assets = _client.getAssetsWithUnboundedMaxVersion(types, Collections.singleton("correct"), Visibility.PUBLIC);
        assertEquals("One asset should be obtained", 1, assets.size());
        assertTrue("Should get back the feature " + featureWithNoMaxVersion + " from " + assets, assets.contains(featureWithNoMaxVersion));
    }

    /**
     * Test for the {@link RestClient#getFilteredAssets(Map)} method.
     *
     * @throws RequestFailureException
     * @throws IOException
     * @throws BadVersionException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testGetFilteredAssets() throws IOException, RequestFailureException, BadVersionException, SecurityException, ClientFailureException {
        // Create an assets that has all of the filterable attributes set
        Asset filterableAsset = createTestAsset();
        filterableAsset.setType(ResourceType.FEATURE);
        WlpInformation wlpInfo = new WlpInformation();
        filterableAsset.setWlpInformation(wlpInfo);
        wlpInfo.setVisibility(Visibility.PUBLIC);
        wlpInfo.addProvideFeature("feature1");
        wlpInfo.setShortName("IsThisShort?");
        wlpInfo.setVanityRelativeURL("no_url");
        AppliesToFilterInfo filterInfo = new AppliesToFilterInfo();
        filterInfo.setProductId("a.product");
        FilterVersion filterVersion = new FilterVersion();
        filterVersion.setValue("1");
        filterInfo.setMinVersion(filterVersion);
        filterInfo.setHasMaxVersion("false");
        wlpInfo.setAppliesToFilterInfo(Collections.singleton(filterInfo));
        filterableAsset = _writeableClient.addAsset(filterableAsset);
        String assetId = filterableAsset.get_id();

        // Now try to filter on each attribute both in positive and negative way
        // TYPE
        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.TYPE, Collections.singleton(ResourceType.FEATURE.getValue()));
        Collection<Asset> filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.TYPE, Collections.singleton(ResourceType.ADDON.getValue()));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // PRODUCT ID
        filters.clear();
        // Try a collection of strings
        Collection<String> productIdValues = new HashSet<String>();
        productIdValues.add("a.product");
        productIdValues.add("another.product");
        filters.put(FilterableAttribute.PRODUCT_ID, productIdValues);
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.PRODUCT_ID, Collections.singleton("Wibble"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // VISIBILITY
        filters.clear();
        filters.put(FilterableAttribute.VISIBILITY, Collections.singleton(Visibility.PUBLIC.toString()));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.VISIBILITY, Collections.singleton(Visibility.INSTALL.toString()));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // PRODUCT MIN VERSION
        filters.clear();
        filters.put(FilterableAttribute.PRODUCT_MIN_VERSION, Collections.singleton("1"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.PRODUCT_MIN_VERSION, Collections.singleton("2"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // PRODUCT HAS MAX VERSION
        filters.clear();
        filters.put(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Collections.singleton("false"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.PRODUCT_HAS_MAX_VERSION, Collections.singleton("true"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // SYMBOLIC NAME
        filters.clear();
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("feature1"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("wibble"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // SHORT NAME
        filters.clear();
        filters.put(FilterableAttribute.SHORT_NAME, Collections.singleton("IsThisShort?"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.SHORT_NAME, Collections.singleton("Wibble"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // LOWER CASE SHORT NAME
        filters.clear();
        filters.put(FilterableAttribute.LOWER_CASE_SHORT_NAME, Collections.singleton("isthisshort?"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.LOWER_CASE_SHORT_NAME, Collections.singleton("Wibble"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // VANITY URL
        filters.clear();
        filters.put(FilterableAttribute.VANITY_URL, Collections.singleton("no_url"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.VANITY_URL, Collections.singleton("foo_url"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

        // Finally make sure passing in null is ok
        filters.clear();
        filters.put(FilterableAttribute.TYPE, null);
        assertFalse("Should get back some assets", _client.getFilteredAssets(filters).isEmpty());
        filters.put(FilterableAttribute.TYPE, Collections.<String> emptySet());
        assertFalse("Should get back some assets", _client.getFilteredAssets(filters).isEmpty());
    }

    /**
     * Test to make sure a filter value can include an &
     *
     * @throws RequestFailureException
     * @throws IOException
     * @throws BadVersionException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFilterWithAmpersand() throws IOException, RequestFailureException, BadVersionException, SecurityException, ClientFailureException {
        Asset filterableAsset = createTestAsset();
        WlpInformation wlpInfo = new WlpInformation();
        filterableAsset.setWlpInformation(wlpInfo);
        wlpInfo.addProvideFeature("feature&1");
        filterableAsset = _writeableClient.addAsset(filterableAsset);
        String assetId = filterableAsset.get_id();

        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("feature&1"));
        Collection<Asset> filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be a single asset filtered", 1, filteredAssets.size());
        assertEquals("We should get the right asset back", assetId, filteredAssets.iterator().next().get_id());
        filters.put(FilterableAttribute.SYMBOLIC_NAME, Collections.singleton("wibble"));
        filteredAssets = _client.getFilteredAssets(filters);
        assertEquals("There should be no assets filtered", 0, filteredAssets.size());

    }

    /**
     * Tests the find works without supplying any types
     *
     * @throws Exception
     */
    @Test
    public void testFindNoTypes() throws Exception {
        Asset asset1 = createTestAsset();
        asset1.setType(ResourceType.FEATURE);
        asset1.setDescription("keyword1");
        asset1 = _writeableClient.addAsset(asset1);
        String assetId1 = asset1.get_id();
        _writeableClient.updateState(assetId1, StateAction.PUBLISH);
        _writeableClient.updateState(assetId1, StateAction.APPROVE);

        Asset asset2 = createTestAsset();
        asset2.setType(ResourceType.FEATURE);
        asset2.setDescription("keyword2");
        asset2 = _writeableClient.addAsset(asset2);
        String assetId2 = asset2.get_id();
        _writeableClient.updateState(assetId2, StateAction.PUBLISH);
        _writeableClient.updateState(assetId2, StateAction.APPROVE);

        // Indexing...
        fixture.refreshTextIndex(assetId2);

        List<Asset> found = _unauthenticatedClient.findAssets("keyword1", null);
        assertEquals("Only one asset should be found", 1, found.size());
        assertEquals("The first asset should be found", assetId1, found.get(0).get_id());
    }

    /**
     * Tests the find works supplying a single type
     *
     * @throws Exception
     */
    @Test
    public void testFindOneType() throws Exception {
        Asset asset1 = createTestAsset();
        asset1.setType(ResourceType.FEATURE);
        asset1.setDescription("keyword1");
        asset1 = _writeableClient.addAsset(asset1);
        String assetId1 = asset1.get_id();
        _writeableClient.updateState(assetId1, StateAction.PUBLISH);
        _writeableClient.updateState(assetId1, StateAction.APPROVE);

        Asset asset2 = createTestAsset();
        asset2.setType(ResourceType.OPENSOURCE);
        asset2.setDescription("keyword1");
        asset2 = _writeableClient.addAsset(asset2);
        String assetId2 = asset2.get_id();
        _writeableClient.updateState(assetId2, StateAction.PUBLISH);
        _writeableClient.updateState(assetId2, StateAction.APPROVE);

        // Indexing...
        fixture.refreshTextIndex(assetId2);

        List<Asset> found = _unauthenticatedClient.findAssets("keyword1", Collections.singleton(ResourceType.FEATURE));
        assertEquals("Only one asset should be found", 1, found.size());
        assertEquals("The first asset should be found", assetId1, found.get(0).get_id());
    }

    /**
     * Tests the find works supplying two types
     *
     * @throws Exception
     */
    @Test
    public void testFindTwoTypes() throws Exception {
        Asset asset1 = createTestAsset();
        asset1.setType(ResourceType.FEATURE);
        asset1.setDescription("keyword1");
        asset1 = _writeableClient.addAsset(asset1);
        String assetId1 = asset1.get_id();
        _writeableClient.updateState(assetId1, StateAction.PUBLISH);
        _writeableClient.updateState(assetId1, StateAction.APPROVE);

        Asset asset2 = createTestAsset();
        asset2.setType(ResourceType.OPENSOURCE);
        asset2.setDescription("keyword1");
        asset2 = _writeableClient.addAsset(asset2);
        String assetId2 = asset2.get_id();
        _writeableClient.updateState(assetId2, StateAction.PUBLISH);
        _writeableClient.updateState(assetId2, StateAction.APPROVE);

        Asset asset3 = createTestAsset();
        asset3.setType(ResourceType.PRODUCTSAMPLE);
        asset3.setDescription("keyword1");
        asset3 = _writeableClient.addAsset(asset3);
        String assetId3 = asset3.get_id();
        _writeableClient.updateState(assetId3, StateAction.PUBLISH);
        _writeableClient.updateState(assetId3, StateAction.APPROVE);

        // Indexing...
        fixture.refreshTextIndex(assetId3);

        Collection<ResourceType> featureAndOpensource = new HashSet<ResourceType>();
        featureAndOpensource.add(ResourceType.FEATURE);
        featureAndOpensource.add(ResourceType.OPENSOURCE);
        List<Asset> found = _unauthenticatedClient.findAssets("keyword1", featureAndOpensource);
        assertEquals("Two assets should be found", 2, found.size());
        boolean found1 = false;
        boolean found2 = false;
        for (Asset asset : found) {
            if (!found1 && assetId1.equals(asset.get_id())) {
                found1 = true;
            } else if (!found2 && assetId2.equals(asset.get_id())) {
                found2 = true;
            } else {
                fail("Unexpected asset found: " + asset);
            }
        }
    }

    @Test
    public void testRepositoryStatusPass() throws Exception {

        // create and write an asset to the repository before the check as the file will have been deleted
        Asset asset1 = createTestAsset();
        asset1.setType(ResourceType.FEATURE);
        asset1.setDescription("keyword1");
        asset1 = _writeableClient.addAsset(asset1);

        _client.checkRepositoryStatus();
    }

    @Test
    public void testAssetIdsMaintained() throws Exception {
        // Some repository clients generate the IDs for assets
        // Check that creating several assets and then deleting one doesn't change the IDs of the others

        Asset asset1 = _writeableClient.addAsset(createTestAsset());
        Asset asset2 = _writeableClient.addAsset(createTestAsset());
        Asset asset3 = _writeableClient.addAsset(createTestAsset());

        _writeableClient.deleteAssetAndAttachments(asset2.get_id());

        assertThat(_client.getAsset(asset1.get_id()), equalTo(asset1));
        assertThat(_client.getAsset(asset3.get_id()), equalTo(asset3));
        try {
            _client.getAsset(asset2.get_id());
            fail("Exception not thrown retrieving deleted asset");
        } catch (Exception e) {
            // expected
        }
    }

    protected Attachment addAttachment(String id, final String name,
                                       final File f, int crc) throws IOException, BadVersionException, RequestFailureException {

        // default to adding CONTENT attachment and not supplying a URL
        return addAttachment(id, name, f, crc, AttachmentType.CONTENT, null);
    }

    protected Attachment addAttachment(String id, final String name, final File f, int crc, String url) throws IOException, BadVersionException, RequestFailureException {

        // default to adding CONTENT attachment
        return addAttachment(id, name, f, crc, AttachmentType.CONTENT, url);
    }

    protected Attachment addAttachment(String id, String name, File f, int crc, AttachmentType type,
                                       String url) throws IOException, BadVersionException, RequestFailureException {
        System.out.println("Adding attachment:");
        System.out.println("Asset id: " + id);
        System.out.println("File: " + f.getAbsolutePath());
        System.out.println("Type: " + type);
        System.out.println("URL: " + url);

        AttachmentSummary summary = new MockAttachmentSummary(f, name, type, crc, url);
        Attachment newAt = _writeableClient.addAttachment(id, summary);

        System.out.println("Attachment added:");
        System.out.println(newAt.toString());
        return newAt;
    }

    /*
     * Creates a test asset with a timestamp in its name
     *
     * @return The asset
     */
    protected Asset createTestAsset() {
        Asset newAsset = new Asset();
        String assetName = "Test Asset" + createDateString();
        newAsset.setName(assetName);
        return newAsset;
    }

    /**
     * Creates a string saying when something is created.
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
     * @return The location where the resources to use in this test are
     */
    protected File getResourcesDir() {
        return new File("lib/LibertyFATTestFiles");
    }

}
