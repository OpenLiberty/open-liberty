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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.repository.common.enums.ResourceType;
import com.ibm.ws.repository.common.enums.State;
import com.ibm.ws.repository.transport.client.JSONAssetConverter;
import com.ibm.ws.repository.transport.model.Asset;
import com.ibm.ws.repository.transport.model.Attachment;
import com.ibm.ws.repository.transport.model.Feedback;
import com.ibm.ws.repository.transport.model.Provider;

public class JSONAssetConverterTest {

    private final String ID = "theId";
    private Calendar CREATED_DATE;
    private final State STATE = State.DRAFT;
    private final ResourceType TYPE = ResourceType.IFIX;
    private final int TWEETS = Integer.MAX_VALUE;
    private final String ATTACHMENT1_NAME = "name1";
    private final String ATTACHMENT2_NAME = "name2";

    /**
     * This method will set the data values to appropriate things for the test
     * object
     */
    @Before
    public void setupTestData() {
        CREATED_DATE = Calendar.getInstance();
    }

    /**
     * This test will write a list of assets to a stream that is fed back into
     * the read asset to make sure that it both the write and read work ok.
     *
     * @throws IOException
     */
    @Test
    public void testAssetListConversion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call, need to wrap object in an array as we
        // only have write single object and want to read array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write('[');
        JSONAssetConverter.writeValue(outputStream, asset);
        outputStream.write(']');
        List<Asset> readInAssets = JSONAssetConverter.readValues(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("We only serialized one asset so should have one back", 1,
                     readInAssets.size());
        Asset readInAsset = readInAssets.get(0);
        testAsset(readInAsset);
    }

    /**
     * Test to make sure a single asset can be read in from a stream
     *
     * @throws Exception
     */
    @Test
    public void testAssetConverstion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONAssetConverter.writeValue(outputStream, asset);
        Asset readInAsset = JSONAssetConverter.readValue(new ByteArrayInputStream(outputStream.toByteArray()));
        testAsset(readInAsset);
    }

    /**
     * Test to ensure we're serializing dates correctly
     * <p>
     * We had an issue where we were using the local time zone to write and parse dates which gave us inconsistent results over the DST boundary.
     * <p>
     * If we're still doing that, this test will fail in the UK timezone.
     *
     * @throws Exception
     */
    @Test
    public void testDateConversion() throws Exception {
        // Both T1 and T2 are 00:30 in TZ Europe/London due to DST change
        Calendar t1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        t1.set(2014, Calendar.OCTOBER, 25, 23, 30, 0);
        Calendar t2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        t2.set(2014, Calendar.OCTOBER, 26, 0, 30, 0);

        Calendar local = Calendar.getInstance();
        Asset asset = createTestAsset();

        local.setTimeInMillis(t1.getTimeInMillis());
        asset.setCreatedOn(local);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONAssetConverter.writeValue(outputStream, asset);
        Asset readInAsset = JSONAssetConverter.readValue(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("Writing and reading a date should produce the same date", asset.getCreatedOn(), readInAsset.getCreatedOn());

        local.setTimeInMillis(t2.getTimeInMillis());
        asset.setCreatedOn(local);

        outputStream = new ByteArrayOutputStream();
        JSONAssetConverter.writeValue(outputStream, asset);
        readInAsset = JSONAssetConverter.readValue(new ByteArrayInputStream(outputStream.toByteArray()));
        assertEquals("Writing and reading a date should produce the same date", asset.getCreatedOn(), readInAsset.getCreatedOn());
    }

    /**
     * This test will write an Asset to a stream that is fed back into the read
     * generic type to make sure that it both the write and read work ok.
     *
     * @throws IOException
     */
    @Test
    public void testGenericListConversion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call, need to wrap object in an array as we
        // only have write single object and want to read array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write('[');
        JSONAssetConverter.writeValue(outputStream, asset);
        outputStream.write(']');

        // Use the generic method
        List<Asset> readInAssets = JSONAssetConverter.readValues(
                                                                 new ByteArrayInputStream(outputStream.toByteArray()),
                                                                 Asset.class);
        assertEquals("We only serialized one asset so should have one back", 1,
                     readInAssets.size());
        Asset readInAsset = readInAssets.get(0);
        testAsset(readInAsset);
    }

    /**
     * Test to make sure a single generic type can be read in from a stream
     *
     * @throws Exception
     */
    @Test
    public void testGenericConverstion() throws Exception {
        Asset asset = createTestAsset();

        // Pipe write output to read call
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JSONAssetConverter.writeValue(outputStream, asset);

        // Use the generic method
        Asset readInAsset = JSONAssetConverter.readValue(
                                                         new ByteArrayInputStream(outputStream.toByteArray()),
                                                         Asset.class);
        testAsset(readInAsset);

    }

    /**
     * This tests that you can convert an asset with a provider to make sure the Url field
     * encodes / decodes correctly. (Originally Url, then URI, now String).
     *
     * @throws Exception
     */
    @Test
    public void testProviderConversion() throws Exception {
        Provider testProvider = new Provider();
        testProvider.setUrl("http://www.ibm.com");

        String writtenProvider = JSONAssetConverter.writeValueAsString(testProvider);
        assertTrue("The written provider should contain the supplied URL", writtenProvider.contains("www.ibm.com"));
        Provider readInProvider = JSONAssetConverter.readValue(new ByteArrayInputStream(writtenProvider.getBytes()), Provider.class);
        assertEquals("Read in provider should be equal to the original", testProvider, readInProvider);
    }

    /**
     * Makes sure the values are set to the test values in the supplied asset
     *
     * @param asset
     *            The asset to test
     */
    private void testAsset(Asset asset) {
        // Test all the values we set
        assertEquals("Write and read should produce the same string value", ID,
                     asset.get_id());
        assertEquals("Write and read should produce the same Calendar value",
                     CREATED_DATE, asset.getCreatedOn());
        assertEquals(
                     "Write and read should produce the same simple enum value",
                     STATE, asset.getState());
        assertEquals(
                     "Write and read should produce the same complex enum value",
                     TYPE, asset.getType());
        Feedback readInFeedback = asset.getFeedback();
        assertNotNull("Nested object should of written and read",
                      readInFeedback);
        assertEquals("Write and read should produce the same int value",
                     TWEETS, readInFeedback.getTweets());
        List<Attachment> readInAttachments = asset.getAttachments();
        assertNull("The asset (obtained from a get all), should not have any attachments until they are read",
                   readInAttachments);

        // Also make sure that lists and objects that weren't set are still null
        assertNull("Strings that weren't set should be null",
                   asset.getDescription());
        assertNull("Objects that weren't set should be null",
                   asset.getCreatedBy());
    }

    /**
     * Creates a test asset with various sub-objects and different types set
     *
     * @return The asset
     */
    private Asset createTestAsset() {
        Asset asset = new Asset();

        // Set a few test fields, don't do all of them but a representative
        // sample
        // String
        asset.set_id(ID);

        // Calendar
        asset.setCreatedOn(CREATED_DATE);

        // Single object
        Feedback feedback = new Feedback();
        asset.setFeedback(feedback);

        // Simple enum (uses name())
        asset.setState(STATE);

        // Complex enum uses value
        asset.setType(TYPE);

        // int
        feedback.setTweets(TWEETS);

        // List of objects
        Attachment attachment1 = new Attachment();
        attachment1.setName(ATTACHMENT1_NAME);
        asset.addAttachement(attachment1);
        Attachment attachment2 = new Attachment();
        attachment2.setName(ATTACHMENT2_NAME);
        asset.addAttachement(attachment2);
        return asset;
    }

}
