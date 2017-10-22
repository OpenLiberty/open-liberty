/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.lumberjack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.wsspi.ssl.SSLSupport;

/**
 * Client that builds on top of lumber jack client to implement
 * logmet related changes.
 */
public class MTLumberjackClient extends LumberjackClient {

    /* Constants for the authorization frame */
    private final String MT_PROTOCOL_VERSION = "2";
    private final String TENANT_FRAME_TYPE = "T";

    public MTLumberjackClient(String sslConfig, SSLSupport sslSupport) throws SSLException {
        super(sslConfig, sslSupport);
    }

    public boolean authorize(String tenantId, @Sensitive String tenantPassword) throws IOException {
        //Prepare the auth frame
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] tenantIdBytes = tenantId.getBytes(UTF_8);
        byte[] tenantPasswordBytes = tenantPassword.getBytes(UTF_8);

        byte tenantIdLength = (byte) tenantIdBytes.length;
        byte tenantPasswordLength = (byte) tenantPasswordBytes.length;

        output.write(MT_PROTOCOL_VERSION.getBytes(UTF_8));//Protocol version 2
        output.write(TENANT_FRAME_TYPE.getBytes(UTF_8)); //Frame type 'T'
        output.write(ByteBuffer.allocate(1).put(tenantIdLength).array());//1 byte id length
        output.write(tenantIdBytes);
        output.write(ByteBuffer.allocate(1).put(tenantPasswordLength).array());//1 byte password length
        output.write(tenantPasswordBytes);

        //Write the auth frame to the wire
        writeFrame(output.toByteArray());

        //Check the ack to see if the user is authorized
        byte[] buffer = new byte[6];
        int bytesReceived = 0;

        while ((bytesReceived = bytesReceived + in.read(buffer, bytesReceived, buffer.length - bytesReceived)) != -1) {
            if (bytesReceived == 6) {
                String response = new String(Arrays.copyOfRange(buffer, 0, 1), UTF_8);
                String frameType = new String(Arrays.copyOfRange(buffer, 1, 2), UTF_8);
                if (frameType.equals("A")) {
                    if (response.equals("0"))
                        return false;
                    else
                        return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

//    public static void main(String[] args) {
//
//        /**
//         * ALCH_TENANT_ID --> 4039392b-08bc-4eb1-8e1a-c81cfcddb44f
//         * stack_id --> liberty
//         * instance_id --> eval.liberty.data
//         * file --> /var/log/syslog
//         * host --> LibertyContainer
//         * type --> linux_syslog
//         * line --> test message
//         */
//
//        Map<String, String> testEvent = new HashMap<String, String>();
//        testEvent.put("ALCH_TENANT_ID", "4039392b-08bc-4eb1-8e1a-c81cfcddb44f");
//        testEvent.put("stack_id", "liberty");
//        testEvent.put("instance_id", "eval.liberty.data");
//        testEvent.put("file", "/var/log/syslog");
//        testEvent.put("host", "LibertyContainer");
//        testEvent.put("type", "linux_syslog");
//        testEvent.put("line", "test message");
//
//        try {
//            SSLHelper sslHelper;
//            sslHelper = new SSLHelper();
//            MTLumberJackClient client = new MTLumberJackClient(sslHelper);
//
//            client.connect("logs.opvis.bluemix.net", 9091);
//            boolean isAuthorized = client.authorize("4039392b-08bc-4eb1-8e1a-c81cfcddb44f", "BZNj0TYT0cSi");
//            System.out.println("Tenant is authorized --> " + isAuthorized);
//
//            List<Map<String, String>> events = new ArrayList<>();
//            events.add(testEvent);
//            client.sendEvents(events);
//            client.readAckFrame();
//        } catch (KeyManagementException e) {
//            e.printStackTrace();
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (UnknownHostException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}