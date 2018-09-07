/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transport.http2_test.headers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;

import com.ibm.ws.http.channel.h2internal.exceptions.CompressionException;
import com.ibm.ws.http.channel.h2internal.hpack.DynamicTable;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderField;
import com.ibm.ws.http.channel.h2internal.hpack.H2HeaderTable;
import com.ibm.ws.http.channel.h2internal.hpack.H2Headers;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants;
import com.ibm.ws.http.channel.h2internal.hpack.HpackConstants.LiteralIndexType;
import com.ibm.ws.http.channel.h2internal.hpack.HpackUtils;
import com.ibm.ws.http.channel.h2internal.hpack.IntegerRepresentation;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;

import test.common.SharedOutputManager;

/**
 *
 */
public class HeaderTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule rule = outputMgr;

    @Rule
    public TestName name = new TestName();

    @After
    public void tearDown() {
        //outputMgr.copySystemStreams();
    }

    @Test
    public void testDynamicTable() {

        //New dynamic table entries are meant to be at the lowest table index. If an eviction needs to occur
        //entries are removed from the tail (the largest index belong to the oldest entry).

        DynamicTable table = new DynamicTable(100);
        H2HeaderField header = new H2HeaderField("test-header", "size of 53"); //First Header
        H2HeaderField header2 = new H2HeaderField("t", "2"); //Second Header
        H2HeaderField header3 = new H2HeaderField("testEvict", "true"); //Third Header
        H2HeaderField header4 = new H2HeaderField("This is a very large header", "to test that everything gets evicted properly."); //Fourth Header

        table.addDynamicEntry(header);
        verifyDynamicTableEntry(table, 47, 1, "test-header");

        //Add second header
        table.addDynamicEntry(header2);
        verifyDynamicTableEntry(table, 13, 2, "t");

        //Adding header3 should cause eviction
        table.addDynamicEntry(header3);
        verifyDynamicTableEntry(table, 21, 2, "testEvict");

        //Adding header 4 should cause total eviction
        table.addDynamicEntry(header4);
        verifyDynamicTableEntry(table, 100, 0, null);

        //Test eviction by changing table size to 50. Header 1 and 2 will be re-added to the table.
        //The table should only contain header 2 after eviction.
        table.addDynamicEntry(header);
        table.addDynamicEntry(header2);
        table.updateDynamicTableSize(50);
        verifyDynamicTableEntry(table, 16, 1, "t");

        //Test eviction by changing table size to 0.
        table.updateDynamicTableSize(0);
        verifyDynamicTableEntry(table, 0, 0, null);

    }

    @Test
    public void integerRepresentationTests() {

        WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(5);

        ByteArrayOutputStream encodedIntegers = new ByteArrayOutputStream();
        byte[] expectedArray = new byte[] { (byte) 0x0A, (byte) 0x1F, (byte) 0x9A, (byte) 0x0A, (byte) 0x2A };

        //Encode '10' with 5-bit prefix. Expected result xxx01010
        try {
            IntegerRepresentation.encode(10, (byte) 0x00, 5);
            encodedIntegers.write(IntegerRepresentation.encode(10, (byte) 0x00, 5));
            //Encode '1337' with 5-bit prefix. Expected result xxx11111, 10011010, 00001010
            encodedIntegers.write(IntegerRepresentation.encode(1337, (byte) 0x00, 5));
            //Encode '42' on 8 bits. Expected result 00101010
            encodedIntegers.write(IntegerRepresentation.encode(42, (byte) 0x00, 8));

            encodedIntegers.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertArrayEquals(expectedArray, encodedIntegers.toByteArray());

        //Test Decode
        buffer.put(expectedArray);
        buffer.flip();

        Assert.assertTrue("Integer not decoded correctly.", IntegerRepresentation.decode(buffer, 5) == 10);
        Assert.assertTrue("Integer not decoded correctly.", IntegerRepresentation.decode(buffer, 5) == 1337);
        Assert.assertTrue("Integer not decoded correctly.", IntegerRepresentation.decode(buffer, 8) == 42);

        buffer.release();
        buffer = null;
    }

    @Test
    public void literalHeaderFieldWithIndexingTest() {
        //Brand new header tables
        H2HeaderTable readTable = new H2HeaderTable();
        H2HeaderTable writeTable = new H2HeaderTable();

        WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(26);
        byte[] expected = HpackUtils.hexStringToByteArray("400a637573746f6d2d6b65790d637573746f6d2d686561646572");
        H2HeaderField expectedHeader = new H2HeaderField("custom-key", "custom-header");

        verifyHeaderEncodeDecode(buffer, readTable, writeTable, expectedHeader, LiteralIndexType.INDEX, expected);

        //Dynamic table is expected to have: [1] (s=55) custom-key: custom-header | Table size: 55
        //This should be the very first entry and only entry to the dynamic entry. Entry should be
        //located at index 62 (Initial static table size + 1).

        Assert.assertTrue("Dynamic read table size is incorrect", readTable.getDynamicTableUsedAddressSpace() == 55);
        Assert.assertTrue("H2HeaderTable getHeaderEntry(index) did not return the expected H2HeaderField from read table",
                          expectedHeader.equals(readTable.getHeaderEntry(HpackConstants.STATIC_TABLE_SIZE + 1)));

        Assert.assertTrue("Dynamic write table size is incorrect", writeTable.getDynamicTableUsedAddressSpace() == 55);
        Assert.assertTrue("H2HeaderTable getHeaderEntry(index) did not return the expected H2HeaderField from write table",
                          expectedHeader.equals(writeTable.getHeaderEntry(HpackConstants.STATIC_TABLE_SIZE + 1)));
    }

    @Test
    public void literalHeaderFieldWithoutIndexingTest() {

        //Brand new header tables
        H2HeaderTable readTable = new H2HeaderTable();
        H2HeaderTable writeTable = new H2HeaderTable();
        WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(28);
        byte[] expected = HpackUtils.hexStringToByteArray("040c2f73616d706c652f70617468");
        H2HeaderField expectedHeader = new H2HeaderField(":path", "/sample/path");

        verifyHeaderEncodeDecode(buffer, readTable, writeTable, expectedHeader, LiteralIndexType.NOINDEXING, expected);
        //Dynamic table is expected to be empty

        Assert.assertTrue("Dynamic table size is incorrect", readTable.getDynamicTableUsedAddressSpace() == 0);
        Assert.assertTrue("Dynamic table size is incorrect", writeTable.getDynamicTableUsedAddressSpace() == 0);

    }

    @Test
    public void literalHeaderFieldNeverIndexTest() {

        //Brand new header tables
        H2HeaderTable readTable = new H2HeaderTable();
        H2HeaderTable writeTable = new H2HeaderTable();
        WsByteBuffer buffer = ChannelFrameworkFactory.getBufferManager().allocate(30);
        byte[] expected = HpackUtils.hexStringToByteArray("100870617373776f726406736563726574");
        H2HeaderField expectedHeader = new H2HeaderField("password", "secret");
        verifyHeaderEncodeDecode(buffer, readTable, writeTable, expectedHeader, LiteralIndexType.NEVERINDEX, expected);

        //Dynamic table is expected to be empty

        Assert.assertTrue("Dynamic table size is incorrect", readTable.getDynamicTableUsedAddressSpace() == 0);
        Assert.assertTrue("Dynamic table size is incorrect", writeTable.getDynamicTableUsedAddressSpace() == 0);

    }

    @Test
    public void testRequestsWithoutHuffmanCoding() {
        //Brand new header table - Testing Encoder
        H2HeaderTable readTable = new H2HeaderTable();
        WsByteBuffer buffer = null;
        H2HeaderField[] expectedHeaders = null;
        LiteralIndexType type = LiteralIndexType.INDEX;
        boolean huffman = false;
        byte[] received = null;

        //First Request
        received = HpackUtils.hexStringToByteArray("828684410f7777772e6578616d706c652e636f6d");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(20);
        buffer.put(received);
        buffer.flip();
        expectedHeaders = new H2HeaderField[] {
                                                new H2HeaderField(":method", "GET"),
                                                new H2HeaderField(":scheme", "http"),
                                                new H2HeaderField(":path", "/"),
                                                new H2HeaderField(":authority", "www.example.com")
        };
        this.testDecodeHeaders(readTable, expectedHeaders, buffer, type, huffman);

        //Table should have [  1] (s =  57) :authority: www.example.com Table size:  57
        Assert.assertTrue("Table size does not match expected size. " + readTable.printDynamicTable(), readTable.getDynamicTableUsedAddressSpace() == 57);

        //Second Request
        received = HpackUtils.hexStringToByteArray("828684be58086e6f2d6361636865");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(14);
        buffer.put(received);
        buffer.flip();
        expectedHeaders = new H2HeaderField[] {
                                                new H2HeaderField(":method", "GET"),
                                                new H2HeaderField(":scheme", "http"),
                                                new H2HeaderField(":path", "/"),
                                                new H2HeaderField(":authority", "www.example.com"),
                                                new H2HeaderField("cache-control", "no-cache")
        };

        this.testDecodeHeaders(readTable, expectedHeaders, buffer, type, huffman);

        //Table should have read:
        //  [  1] (s =  53) cache-control: no-cache
        //  [  2] (s =  57) :authority: www.example.com
        //        Table size: 110
        Assert.assertTrue("Table size does not match expected size. " + readTable.printDynamicTable(), readTable.getDynamicTableUsedAddressSpace() == 110);

        //Third Request
        received = HpackUtils.hexStringToByteArray("828785bf400a637573746f6d2d6b65790c637573746f6d2d76616c7565");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(29);
        buffer.put(received);
        buffer.flip();
        expectedHeaders = new H2HeaderField[] {
                                                new H2HeaderField(":method", "GET"),
                                                new H2HeaderField(":scheme", "https"),
                                                new H2HeaderField(":path", "/index.html"),
                                                new H2HeaderField(":authority", "www.example.com"),
                                                new H2HeaderField("custom-key", "custom-value")
        };

        this.testDecodeHeaders(readTable, expectedHeaders, buffer, type, huffman);

        //Table Should read
        //[  1] (s =  54) custom-key: custom-value
        //[  2] (s =  53) cache-control: no-cache
        //[  3] (s =  57) :authority: www.example.com
        //      Table size: 164
        Assert.assertTrue("Table size does not match expected size. " + readTable.printDynamicTable(), readTable.getDynamicTableUsedAddressSpace() == 164);

    }

    /**
     * This test decodes the same headers as the testRequestsWithoutHuffmanCoding() but
     * uses Huffman encoding for the literal values.
     */
    @Test
    public void testRequestsWithHuffmanCoding() {
        //Brand new header table - Testing Encoder
        H2HeaderTable readTable = new H2HeaderTable();
        WsByteBuffer buffer = null;
        H2HeaderField[] expectedHeaders = null;
        LiteralIndexType type = LiteralIndexType.INDEX;
        boolean huffman = true;
        byte[] received = null;

        //First Request
        received = HpackUtils.hexStringToByteArray("828684418cf1e3c2e5f23a6ba0ab90f4ff");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(17);
        buffer.put(received);
        buffer.flip();
        expectedHeaders = new H2HeaderField[] {
                                                new H2HeaderField(":method", "GET"),
                                                new H2HeaderField(":scheme", "http"),
                                                new H2HeaderField(":path", "/"),
                                                new H2HeaderField(":authority", "www.example.com")
        };
        this.testDecodeHeaders(readTable, expectedHeaders, buffer, type, huffman);

        //Table should have [  1] (s =  57) :authority: www.example.com Table size:  57
        Assert.assertTrue("Table size does not match expected size. " + readTable.printDynamicTable(), readTable.getDynamicTableUsedAddressSpace() == 57);

        //Second Request
        received = HpackUtils.hexStringToByteArray("828684be5886a8eb10649cbf");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(12);
        buffer.put(received);
        buffer.flip();
        expectedHeaders = new H2HeaderField[] {
                                                new H2HeaderField(":method", "GET"),
                                                new H2HeaderField(":scheme", "http"),
                                                new H2HeaderField(":path", "/"),
                                                new H2HeaderField(":authority", "www.example.com"),
                                                new H2HeaderField("cache-control", "no-cache")
        };

        this.testDecodeHeaders(readTable, expectedHeaders, buffer, type, huffman);

        //Table should have read:
        //  [  1] (s =  53) cache-control: no-cache
        //  [  2] (s =  57) :authority: www.example.com
        //        Table size: 110
        Assert.assertTrue("Table size does not match expected size. " + readTable.printDynamicTable(), readTable.getDynamicTableUsedAddressSpace() == 110);

        //Third Request
        received = HpackUtils.hexStringToByteArray("828785bf408825a849e95ba97d7f8925a849e95bb8e8b4bf");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(24);
        buffer.put(received);
        buffer.flip();
        expectedHeaders = new H2HeaderField[] {
                                                new H2HeaderField(":method", "GET"),
                                                new H2HeaderField(":scheme", "https"),
                                                new H2HeaderField(":path", "/index.html"),
                                                new H2HeaderField(":authority", "www.example.com"),
                                                new H2HeaderField("custom-key", "custom-value")
        };

        this.testDecodeHeaders(readTable, expectedHeaders, buffer, type, huffman);

        //Table Should read
        //[  1] (s =  54) custom-key: custom-value
        //[  2] (s =  53) cache-control: no-cache
        //[  3] (s =  57) :authority: www.example.com
        //      Table size: 164
        Assert.assertTrue("Table size does not match expected size. " + readTable.printDynamicTable(), readTable.getDynamicTableUsedAddressSpace() == 164);

    }

    /**
     * Tests several consecutive header lists, corresponding to HTTP responses,
     * on the same connection. The HTTP/2 SETTING_HEADER_TABLE_SIZE is set to
     * the value of 256, causing some evictions.
     */
    @Test
    public void testResponsesWithoutHuffmanCoding() {
        //Brand new header table - Testing Encoder
        H2HeaderTable writeTable = new H2HeaderTable(256);
        WsByteBuffer buffer = null;
        H2HeaderField[] headers = null;
        LiteralIndexType type = LiteralIndexType.INDEX;
        boolean huffman = false;
        byte[] expected = null;

        //First Response
        expected = HpackUtils.hexStringToByteArray("4803333032580770726976617465611d"
                                                   + "4d6f6e2c203231204f63742032303133"
                                                   + "2032303a31333a323120474d546e1768"
                                                   + "747470733a2f2f7777772e6578616d70"
                                                   + "6c652e636f6d");

        buffer = ChannelFrameworkFactory.getBufferManager().allocate(70);
        headers = new H2HeaderField[] {
                                        new H2HeaderField(":status", "302"),
                                        new H2HeaderField("cache-control", "private"),
                                        new H2HeaderField("date", "Mon, 21 Oct 2013 20:13:21 GMT"),
                                        new H2HeaderField("location", "https://www.example.com")
        };
        this.testEncodeHeaders(writeTable, headers, buffer, type, huffman, expected);

        //Table should have
        //        [  1] (s =  63) location: https://www.example.com
        //        [  2] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT
        //        [  3] (s =  52) cache-control: private
        //        [  4] (s =  42) :status: 302
        //              Table size: 222
        Assert.assertTrue("Table size does not match expected size. " + writeTable.printDynamicTable(), writeTable.getDynamicTableUsedAddressSpace() == 222);

        //Second Request: The (":status", "302) header field is evicted from the dynamic table
        //to free space to allow adding the (":status", "307") header field;
        expected = HpackUtils.hexStringToByteArray("4803333037c1c0bf");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(8);
        headers = new H2HeaderField[] {
                                        new H2HeaderField(":status", "307"),
                                        new H2HeaderField("cache-control", "private"),
                                        new H2HeaderField("date", "Mon, 21 Oct 2013 20:13:21 GMT"),
                                        new H2HeaderField("location", "https://www.example.com"),
        };

        this.testEncodeHeaders(writeTable, headers, buffer, type, huffman, expected);

        //Table should have read:
        //        [  1] (s =  42) :status: 307
        //        [  2] (s =  63) location: https://www.example.com
        //        [  3] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT
        //        [  4] (s =  52) cache-control: private
        //              Table size: 222
        Assert.assertTrue("Table size does not match expected size. " + writeTable.printDynamicTable(), writeTable.getDynamicTableUsedAddressSpace() == 222);

        //Third Response: Several header fields are evicted from the dynamic table during the
        //processing of this header list
        expected = HpackUtils.hexStringToByteArray("88c1611d4d6f6e2c203231204f637420"
                                                   + "323031332032303a31333a323220474d"
                                                   + "54c05a04677a69707738666f6f3d4153"
                                                   + "444a4b48514b425a584f5157454f5049"
                                                   + "5541585157454f49553b206d61782d61"
                                                   + "67653d333630303b2076657273696f6e"
                                                   + "3d31");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(98);
        headers = new H2HeaderField[] {
                                        new H2HeaderField(":status", "200"),
                                        new H2HeaderField("cache-control", "private"),
                                        new H2HeaderField("date", "Mon, 21 Oct 2013 20:13:22 GMT"),
                                        new H2HeaderField("location", "https://www.example.com"),
                                        new H2HeaderField("content-encoding", "gzip"),
                                        new H2HeaderField("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1")
        };

        this.testEncodeHeaders(writeTable, headers, buffer, type, huffman, expected);

        //Table Should read
        //        [  1] (s =  98) set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1
        //        [  2] (s =  52) content-encoding: gzip
        //        [  3] (s =  65) date: Mon, 21 Oct 2013 20:13:22 GMT
        //            Table size: 215
        Assert.assertTrue("Table size does not match expected size. " + writeTable.printDynamicTable(), writeTable.getDynamicTableUsedAddressSpace() == 215);

    }

    /**
     * Tests same conditions as testResponsesWithoutHuffmanCoding but uses
     * Huffman encoding for the literal values. The HTTP/2 setting parameter
     * SETTING_HEADER_TABLE_SIZE is set to the value of 256 octets, causing some
     * evictions. Eviction uses the length of the decoded literal value, therefore
     * the same evictions occur as in the aforementioned test.
     */
    @Test
    public void testResponsesWithHuffmanCoding() {
        //Brand new header table - Testing Encoder
        H2HeaderTable writeTable = new H2HeaderTable(256);
        WsByteBuffer buffer = null;
        H2HeaderField[] headers = null;
        LiteralIndexType type = LiteralIndexType.INDEX;
        boolean huffman = true;
        byte[] expected = null;

        //First Response
        expected = HpackUtils.hexStringToByteArray("488264025885aec3771a4b6196d07abe"
                                                   + "941054d444a8200595040b8166e082a6"
                                                   + "2d1bff6e919d29ad171863c78f0b97c8"
                                                   + "e9ae82ae43d3");

        buffer = ChannelFrameworkFactory.getBufferManager().allocate(54);
        headers = new H2HeaderField[] {
                                        new H2HeaderField(":status", "302"),
                                        new H2HeaderField("cache-control", "private"),
                                        new H2HeaderField("date", "Mon, 21 Oct 2013 20:13:21 GMT"),
                                        new H2HeaderField("location", "https://www.example.com")
        };
        this.testEncodeHeaders(writeTable, headers, buffer, type, huffman, expected);

        //Table should have
        //        [  1] (s =  63) location: https://www.example.com
        //        [  2] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT
        //        [  3] (s =  52) cache-control: private
        //        [  4] (s =  42) :status: 302
        //              Table size: 222
        Assert.assertTrue("Table size does not match expected size. " + writeTable.printDynamicTable(), writeTable.getDynamicTableUsedAddressSpace() == 222);

        //Second Request: The (":status", "302) header field is evicted from the dynamic table
        //to free space to allow adding the (":status", "307") header field;
        expected = HpackUtils.hexStringToByteArray("4883640effc1c0bf");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(8);
        headers = new H2HeaderField[] {
                                        new H2HeaderField(":status", "307"),
                                        new H2HeaderField("cache-control", "private"),
                                        new H2HeaderField("date", "Mon, 21 Oct 2013 20:13:21 GMT"),
                                        new H2HeaderField("location", "https://www.example.com"),
        };

        this.testEncodeHeaders(writeTable, headers, buffer, type, huffman, expected);

        //Table should have read:
        //        [  1] (s =  42) :status: 307
        //        [  2] (s =  63) location: https://www.example.com
        //        [  3] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT
        //        [  4] (s =  52) cache-control: private
        //              Table size: 222
        Assert.assertTrue("Table size does not match expected size. " + writeTable.printDynamicTable(), writeTable.getDynamicTableUsedAddressSpace() == 222);

        //Third Response: Several header fields are evicted from the dynamic table during the
        //processing of this header list
        expected = HpackUtils.hexStringToByteArray("88c16196d07abe941054d444a8200595"
                                                   + "040b8166e084a62d1bffc05a839bd9ab"
                                                   + "77ad94e7821dd7f2e6c7b335dfdfcd5b"
                                                   + "3960d5af27087f3672c1ab270fb5291f"
                                                   + "9587316065c003ed4ee5b1063d5007");
        buffer = ChannelFrameworkFactory.getBufferManager().allocate(79);
        headers = new H2HeaderField[] {
                                        new H2HeaderField(":status", "200"),
                                        new H2HeaderField("cache-control", "private"),
                                        new H2HeaderField("date", "Mon, 21 Oct 2013 20:13:22 GMT"),
                                        new H2HeaderField("location", "https://www.example.com"),
                                        new H2HeaderField("content-encoding", "gzip"),
                                        new H2HeaderField("set-cookie", "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1")
        };

        this.testEncodeHeaders(writeTable, headers, buffer, type, huffman, expected);

        //Table Should read
        //        [  1] (s =  98) set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1
        //        [  2] (s =  52) content-encoding: gzip
        //        [  3] (s =  65) date: Mon, 21 Oct 2013 20:13:22 GMT
        //            Table size: 215
        Assert.assertTrue("Table size does not match expected size. " + writeTable.printDynamicTable(), writeTable.getDynamicTableUsedAddressSpace() == 215);

    }

    private void verifyDynamicTableEntry(DynamicTable table, int expectedFreeSpace, int expectedEntriesAmount, String expectedHeaderName) {

        Assert.assertTrue("Table was not updated with correct size.", table.freeSpace() == expectedFreeSpace);
        Assert.assertTrue("Amount of table entries was not updated correctly", table.amountOfEntries() == expectedEntriesAmount);
        if (expectedHeaderName != null) {
            Assert.assertTrue("New table entry was not set correctly in table.", table.get(0).getName().equals(expectedHeaderName));
        }
    }

    private void verifyHeaderEncodeDecode(WsByteBuffer buffer, H2HeaderTable readTable, H2HeaderTable writeTable, H2HeaderField expectedHeader,
                                          LiteralIndexType indexingType, byte[] expected) {

        H2HeaderField decodedHeader = null;
        //Encode header without using Huffman encoding and index it into the table
        try {
            buffer.put(H2Headers.encodeHeader(writeTable, expectedHeader.getName(), expectedHeader.getValue(), indexingType, false));
        } catch (CompressionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        buffer.flip();
        boolean result = true;
        for (byte b : expected) {
            if (buffer.get() != b) {
                result = false;
                break;
            }
        }
        Assert.assertTrue("Header encoding was not done correctly.", result);

        //Test Decode process - Literal not indexed
        buffer.flip();
        try {
            decodedHeader = H2Headers.decodeHeader(buffer, readTable);
        } catch (CompressionException e) {
            e.printStackTrace();
        }

        Assert.assertTrue("decodeHeader did not complete correctly.", expectedHeader.equals(decodedHeader));

    }

    private void testDecodeHeaders(H2HeaderTable table, H2HeaderField[] expectedHeaders, WsByteBuffer buffer,
                                   LiteralIndexType type, boolean huffman) {

        ArrayList<H2HeaderField> headers = new ArrayList<H2HeaderField>();
        //Try decoding all provided headers into the buffer.
        try {
            while (buffer.hasRemaining()) {

                headers.add(H2Headers.decodeHeader(buffer, table));

            }
        } catch (CompressionException e) {

            e.printStackTrace();
        }

        //Verify correct decoding of H2Headers against expected value
        boolean result = true;

        for (int i = 0; i < headers.size(); i++) {
            if (!headers.get(i).equals(expectedHeaders[i])) {
                result = false;
                break;
            }
        }
        Assert.assertTrue("Request was not decoded correctly.", result);

        //Release buffer in preparation for next request if any.
        buffer.release();
        buffer = null;
    }

    private void testEncodeHeaders(H2HeaderTable table, H2HeaderField[] headers, WsByteBuffer buffer,
                                   LiteralIndexType type, boolean huffman, byte[] expected) {

        //Try encoding all provided headers into the buffer.
        try {
            for (H2HeaderField header : headers) {
                buffer.put(H2Headers.encodeHeader(table, header.getName(), header.getValue(), type, huffman));

            }
        } catch (CompressionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Revert buffer position to start and evaluate
        buffer.flip();
        //Verify correct encoding of H2Header against expected value
        boolean result = true;

        for (byte b : expected) {
            if (buffer.get() != b) {
                result = false;
                break;
            }
        }
        Assert.assertTrue("Request was not encoded correctly.", result);

        //Release buffer in preparation for next request if any.
        buffer.release();
        buffer = null;
    }

}
