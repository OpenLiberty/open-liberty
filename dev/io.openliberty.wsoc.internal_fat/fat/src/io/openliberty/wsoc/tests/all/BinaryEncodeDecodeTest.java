/*******************************************************************************
 * NOTICES AND INFORMATION FOR Open Liberty
 * Copyright 2014 IBM Corporation and others
 * This product includes software developed at
 * The Open Liberty Project (https://openliberty.io/).
 *******************************************************************************/
package io.openliberty.wsoc.tests.all;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;

import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.util.wsoc.WsocTestContext;
import io.openliberty.wsoc.util.wsoc.WsocTestRunner;
import io.openliberty.wsoc.common.Constants;
import io.openliberty.wsoc.endpoints.client.basic.BinaryEncodeDecodeClientEP;

/**
 * Test class for WebSocket Binary decoder and encoder
 * 
 * @author Rashmi Hunt
 */
public class BinaryEncodeDecodeTest {

    public static int DEFAULT_TIMEOUT = Constants.getDefaultTimeout();
    private static final Logger LOG = Logger.getLogger(BinaryEncodeDecodeTest.class.getName());

    private WsocTest wsocTest = null;

    public BinaryEncodeDecodeTest(WsocTest test) {
        this.wsocTest = test;
    }

    @SuppressWarnings("deprecation")
    protected void runEchoTest(Object tep, String resource, Object[] data, Object[] expectedData) throws Exception {

        WsocTestContext testdata = wsocTest.runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), data.length, DEFAULT_TIMEOUT);

        testdata.reThrowException();

        //actual response from ServerEndpoint, PathParamServerEP
        Object[] actualData = testdata.getMessage().toArray();
        LOG.log(Level.INFO, "actualData " + actualData);
        LOG.log(Level.INFO, "actualData 1st param " + actualData[0]);
        Assert.assertEquals(expectedData, actualData);
    }

    /*
     * 
     */
    protected void runEchoTestSingle(Object tep, String resource, Object data, Object expectedData) throws Exception {
        WsocTestContext testdata = wsocTest.runWsocTest(tep, resource, WsocTestRunner.getDefaultConfig(), 1, DEFAULT_TIMEOUT);

        testdata.reThrowException();

        //actual response from ServerEndpoint, PathParamServerEP
        Object actualData = testdata.getMessage().toArray();
        LOG.log(Level.INFO, "actualData" + actualData);
        Assert.assertEquals(expectedData, actualData);
    }

    /*
     * 
     */
    public void testAnnotatedBinaryDecoderSuccess() throws Exception {
        String str = "Binary Encoder Decoder";
        ByteBuffer buffer = ByteBuffer.wrap(str.getBytes());
        String[] expected = { "Result is " + "Binary Encoder Decoder" };

        ByteBuffer[] originalBuf = { buffer };
        runEchoTest(new BinaryEncodeDecodeClientEP.ByteBufferTest(originalBuf), "/basic/BinaryDecodeEncode", originalBuf, expected);
    }

    /*
     * this tests binary decoder and encoder inheritance
     */
    public void testAnnotatedBinaryDecoderExtendSuccess() throws Exception {
        String str = "Binary Encoder Decoder";
        ByteBuffer buffer = ByteBuffer.wrap(str.getBytes());
        String[] expected = { "Result is " + "Binary Encoder Decoder" };

        ByteBuffer[] originalBuf = { buffer };
        runEchoTest(new BinaryEncodeDecodeClientEP.ByteBufferTest(originalBuf), "/basic/BinaryDecodeEncodeExtend", originalBuf, expected);
    }

    /*
     * TODO [rashmi] commented this test case for now as jetty doesn't have a decoder to send InputStream from client side. Tal kto Bill
     * 
     * @Test
     * public void testAnnotatedBinaryStreamDecoder() throws Exception {
     * String str = "BinaryStream Decoder";
     * InputStream input = new ByteArrayInputStream(Charset.forName("UTF-8").encode(str).array());
     * String output = "Result is " + "BinaryStream Decoder";
     * 
     * runEchoTestSingle(new BinaryEncodeDecodeEP.BinaryStreamTest(input), "/basic/defaults", input, output);
     * }
     */
}