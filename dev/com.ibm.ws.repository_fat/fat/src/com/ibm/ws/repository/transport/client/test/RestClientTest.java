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

package com.ibm.ws.repository.transport.client.test;

import static com.ibm.ws.lars.testutils.matchers.AssetByIdMatcher.hasId;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Rule;
import org.junit.Test;

import com.ibm.ws.lars.testutils.FatUtils;
import com.ibm.ws.lars.testutils.fixtures.RepositoryFixture;
import com.ibm.ws.repository.common.enums.AttachmentType;
import com.ibm.ws.repository.common.enums.FilterableAttribute;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.common.enums.StateAction;
import com.ibm.ws.repository.common.enums.Visibility;
import com.ibm.ws.repository.connections.RestRepositoryConnection;
import com.ibm.ws.repository.transport.client.ClientLoginInfo;
import com.ibm.ws.repository.transport.client.RepositoryReadableClient;
import com.ibm.ws.repository.transport.client.RepositoryWriteableClient;
import com.ibm.ws.repository.transport.client.RestClient;
import com.ibm.ws.repository.transport.exceptions.BadVersionException;
import com.ibm.ws.repository.transport.exceptions.ClientFailureException;
import com.ibm.ws.repository.transport.exceptions.RequestFailureException;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.AttachmentSummary;

/**
 * Tests for the {@link RestClient} object.
 */
public class RestClientTest {

    private static final Logger logger = Logger.getLogger(RestClientTest.class.getName());
    private static final File resourcesDir = new File("lib/LibertyFATTestFiles");

    @Rule
    public RepositoryFixture fixture = FatUtils.getRestFixture();

    private final RepositoryReadableClient _client = fixture.getAdminClient();
    private final RepositoryWriteableClient _writeableClient = fixture.getWriteableClient();
    private final RepositoryReadableClient _unauthenticatedClient = fixture.getUserClient();

    /**
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
     * This should not throw an exception, the repo should be available
     *
     * @throws IOException
     * @throws RequestFailureException
     */
    @Test
    public void testCheckRepositoryStatus() throws IOException, RequestFailureException {
        _unauthenticatedClient.checkRepositoryStatus();
    }

    /**
     * Test for {@link RestClient#getAllAssetsMetadata()}
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Test
    public void testGetAllAssetsMetadata() throws IOException, RequestFailureException {
        assertTrue(_client instanceof RestClient);

        Map<String, List<String>> headers = ((RestClient) _client).getAllAssetsMetadata();

        // We currently don't require any specific headers so just check we have some headers
        assertFalse("There should be some headers returned", headers.isEmpty());
    }

    /**
     * Tests adding an attachment that is stored on an http server
     *
     * @throws Exception
     */
    @Test
    public void testDownloadFromHttp() throws Exception {

        Asset newAsset = createTestAsset();
        Asset createdAsset = _writeableClient.addAsset(newAsset);
        Attachment createdAttachment = null;

        String attachmentUrl = fixture.getHostedFileRoot() + "/testfile.txt";

        AttachmentSummary attachmentSummary = new MockAttachmentSummary(new File(resourcesDir, "TestAttachment.txt"), "TestAttachment.txt", AttachmentType.CONTENT, 0, attachmentUrl);

        createdAttachment = _writeableClient.addAttachment(createdAsset.get_id(), attachmentSummary);
        assertEquals("The attachment asset ID should match the asset ID",
                     createdAsset.get_id(), createdAttachment.getAssetId());
        assertEquals("The URL should have been the one supplied: " + createdAttachment.toString(),
                     attachmentUrl,
                     createdAttachment.getUrl());

        InputStream attachmentContentStream = _client.getAttachment(createdAsset, createdAttachment);
        BufferedReader attachmentContentReader = new BufferedReader(new InputStreamReader(attachmentContentStream));
        assertEquals("The content in the attachment should be the testfile content",
                     "This is a test file",
                     attachmentContentReader.readLine());
    }

    /**
     * Test to make sure that if you don't have permission to upload then you get a {@link RequestFailureException} with the right response code and message in it.
     *
     * @throws BadVersionException
     * @throws IOException
     * @throws ClientFailureException
     * @throws SecurityException
     */
    @Test
    public void testFailToUpload() throws IOException, RequestFailureException, BadVersionException, SecurityException, ClientFailureException {
        Asset newAsset = createTestAsset();
        RepositoryWriteableClient client = (RepositoryWriteableClient) fixture.getUserClient();
        try {
            client.addAsset(newAsset);
            fail("Should have thrown a RequestFailureException");
        } catch (RequestFailureException e) {
            assertThat("The response code should match", e.getResponseCode(), anyOf(is(401), is(403)));
            assertNotNull("The error message should not be null", e.getErrorMessage());
        }
    }

    @Test
    public void testLifecycleFromDraft() throws Exception {
        Asset asset = createTestAsset();
        asset = _writeableClient.addAsset(asset);

        assertEquals("Wrong initial state", State.DRAFT, asset.getState());
        String assetId = asset.get_id();

        try {
            _writeableClient.updateState(assetId, StateAction.CANCEL);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        try {
            _writeableClient.updateState(assetId, StateAction.NEED_MORE_INFO);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        try {
            _writeableClient.updateState(assetId, StateAction.APPROVE);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        _writeableClient.updateState(assetId, StateAction.PUBLISH);
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.AWAITING_APPROVAL, asset.getState());

    }

    @Test
    public void testLifecycleFromAwaitingApproval() throws Exception {
        Asset asset = createTestAsset();
        asset = _writeableClient.addAsset(asset);
        String assetId = asset.get_id();
        _writeableClient.updateState(assetId, StateAction.PUBLISH);
        asset = _client.getAsset(assetId);
        assertEquals("Wrong initial state", State.AWAITING_APPROVAL, asset.getState());

        _writeableClient.updateState(assetId, StateAction.CANCEL);
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());

        _writeableClient.updateState(assetId, StateAction.PUBLISH);
        _writeableClient.updateState(assetId, StateAction.NEED_MORE_INFO);
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        _writeableClient.updateState(assetId, StateAction.PUBLISH);
        _writeableClient.updateState(assetId, StateAction.APPROVE);
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.PUBLISHED, asset.getState());
    }

    @Test
    public void testLifecycleFromNeedMoreInfo() throws Exception {
        Asset asset = createTestAsset();
        asset = _writeableClient.addAsset(asset);
        String assetId = asset.get_id();
        _writeableClient.updateState(assetId, StateAction.PUBLISH);
        _writeableClient.updateState(assetId, StateAction.NEED_MORE_INFO);
        asset = _client.getAsset(assetId);
        assertEquals("Wrong initial state", State.NEED_MORE_INFO, asset.getState());

        try {
            _writeableClient.updateState(assetId, StateAction.APPROVE);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        try {
            _writeableClient.updateState(assetId, StateAction.CANCEL);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        try {
            _writeableClient.updateState(assetId, StateAction.NEED_MORE_INFO);
            fail("Expected a request failure");
        } catch (RequestFailureException e) {
            // expected
        }
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.NEED_MORE_INFO, asset.getState());

        _writeableClient.updateState(assetId, StateAction.PUBLISH);
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.AWAITING_APPROVAL, asset.getState());
    }

    @Test
    public void testLifecycleFromPublished() throws Exception {
        Asset asset = createTestAsset();
        asset = _writeableClient.addAsset(asset);
        String assetId = asset.get_id();
        _writeableClient.updateState(assetId, StateAction.PUBLISH);
        _writeableClient.updateState(assetId, StateAction.APPROVE);
        asset = _client.getAsset(assetId);
        assertEquals("Wrong initial state", State.PUBLISHED, asset.getState());

        // Only the unpublish action can be done from the published state
        for (StateAction action : StateAction.values()) {
            try {
                if (action != StateAction.UNPUBLISH) {
                    _writeableClient.updateState(assetId, action);
                    fail("Expected a request failure");
                }
            } catch (RequestFailureException e) {
                // expected
                assertEquals("Expected an exception saying this was invalid but had: " + e.getErrorMessage(), e.getErrorMessage(), "Invalid action " + action.getValue()
                                                                                                                                   + " performed on the asset with state published");
            }
            asset = _client.getAsset(assetId);
            assertEquals("Unexpected state transition", State.PUBLISHED, asset.getState());
        }

        // Upublish back to draft state
        _writeableClient.updateState(assetId, StateAction.UNPUBLISH);
        asset = _client.getAsset(assetId);
        assertEquals("Unexpected state transition", State.DRAFT, asset.getState());
    }

    /**
     * Test to make sure that the state can be updated on an asset
     *
     * @throws IOException
     */
    @Test
    public void testStateUpdate() throws Exception {
        Asset asset = createTestAsset();
        asset = _writeableClient.addAsset(asset);
        _writeableClient.updateState(asset.get_id(), StateAction.PUBLISH);
        Asset updatedAsset = _client.getAsset(asset.get_id());
        assertEquals("The asset state should've been updated",
                     State.AWAITING_APPROVAL, updatedAsset.getState());
    }

    /**
     * Test to make sure that the state can be updated on an asset to the
     * published state
     *
     * @throws IOException
     */
    @Test
    public void testPublishAsset() throws Exception {
        Asset asset = createTestAsset();
        asset = _writeableClient.addAsset(asset);

        // You need two calls to update the state to get to published
        _writeableClient.updateState(asset.get_id(), StateAction.PUBLISH);
        _writeableClient.updateState(asset.get_id(), StateAction.APPROVE);
        Asset updatedAsset = _client.getAsset(asset.get_id());

        assertEquals("The asset state should've been updated",
                     State.PUBLISHED, updatedAsset.getState());
    }

    /**
     * Add an asset and ensure it can be read back without being authenticated
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testUnauthGetAll() throws Exception {

        // create 2 published assets
        Asset newAsset1 = createTestAsset();
        Asset createdAsset1 = _writeableClient.addAsset(newAsset1);
        _writeableClient.updateState(createdAsset1.get_id(), StateAction.PUBLISH);
        _writeableClient.updateState(createdAsset1.get_id(), StateAction.APPROVE);
        logger.log(Level.INFO, "Created asset id=" + createdAsset1.get_id());

        Asset newAsset2 = createTestAsset();
        Asset createdAsset2 = _writeableClient.addAsset(newAsset2);
        _writeableClient.updateState(createdAsset2.get_id(), StateAction.PUBLISH);
        _writeableClient.updateState(createdAsset2.get_id(), StateAction.APPROVE);
        logger.log(Level.INFO, "Created asset id=" + createdAsset2.get_id());

        Collection<Asset> assetList = _unauthenticatedClient.getAllAssets();

        assertThat("Wrong list of assets found", assetList, containsInAnyOrder(hasId(createdAsset1), hasId(createdAsset2)));
    }

    /**
     * Test to make sure if you have a valid charset in the content type then it is returned.
     */
    @Test
    public void testGetCharsetValidCharset() {
        String utf16 = "utf-16";
        String contentType = "text/html; charset=" + utf16;
        String charset = RestClient.getCharset(contentType);
        assertEquals("We should have got the charset we entered", utf16, charset);
    }

    /**
     * Test to make sure if you have a valid charset in the content type then it is returned even if there are more properties
     */
    @Test
    public void testGetCharsetValidCharsetAndMultipleProperties() {
        String utf16 = "utf-16";
        String contentType = "text/html; charset=" + utf16 + " ; foo=bar";
        String charset = RestClient.getCharset(contentType);
        assertEquals("We should have got the charset we entered", utf16, charset);
    }

    /**
     * Test to make sure if you don't have a charset the default is used
     */
    @Test
    public void testGetCharsetDefault() {
        String contentType = "text/html";
        String charset = RestClient.getCharset(contentType);
        assertEquals("We should have got the default charset", "UTF-8", charset);
    }

    /**
     * Test to make sure if you have a invalid charset in the content type then the default is returned.
     */
    @Test
    public void testGetCharsetInvalidCharset() {
        String contentType = "text/html; charset=wibble";
        String charset = RestClient.getCharset(contentType);
        assertEquals("We should have got the default charset", "UTF-8", charset);
    }

    /**
     * Try to filter on visibility private and install, this isn't valid as the values are stored in two different objects.
     * <p>
     * This is only a problem for REST clients because there is no way to express filter with the available query syntax.
     * <p>
     * The desired query is <code>wlpInformation.visibility=PRIVATE OR wlpInformation2.visibility=INSTALL</code>
     *
     * @throws RequestFailureException
     * @throws IOException
     */
    @Test
    public void testInvalidFilter() throws IOException, RequestFailureException {
        Map<FilterableAttribute, Collection<String>> filters = new HashMap<FilterableAttribute, Collection<String>>();
        Collection<String> visibilityValues = new HashSet<String>();
        visibilityValues.add(Visibility.PRIVATE.toString());
        visibilityValues.add(Visibility.INSTALL.toString());
        filters.put(FilterableAttribute.VISIBILITY, visibilityValues);
        try {
            _client.getFilteredAssets(filters);
            fail("Should not be able to filter on private and install visibilities at the same time");
        } catch (IllegalArgumentException e) {

        }
    }

    @Test
    public void testRepositoryStatusNoRepoFound() throws IOException, RequestFailureException {

        RestRepositoryConnection connection = (RestRepositoryConnection) fixture.getAdminConnection();
        String realLocation = connection.getRepositoryUrl();
        String invalidUrl = realLocation.substring(0, realLocation.indexOf("/ma")) + "/invalid";
        RestClient notFound = new RestClient(new ClientLoginInfo("user", "password", "12345", invalidUrl));
        try {
            notFound.checkRepositoryStatus();
            fail("An exception should have been thrown as the repo should not be reachable");
        } catch (RequestFailureException e) {
            // Expect a request failure exception
        }
    }

}
