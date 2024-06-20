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
package com.ibm.ws.zos.channel.wola.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;
import com.ibm.ws.zos.channel.wola.internal.msg.CicsLinkServerContext;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageContext;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageContextArea;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageContextId;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaServiceNameContext;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 *
 */
public class WolaOutboundRequestTest {

    /**
     *
     */
    @Test
    public void testBuildMessage() throws Exception {

        String serviceName = "myService";
        byte[] appData = new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 };
        WolaJcaRequestInfo wolaJcaRequestInfo = new TestWolaJcaRequestInfoImpl();

        WolaMessage wolaMessage = new WolaOutboundRequest().buildMessage(serviceName, appData, wolaJcaRequestInfo);

        assertEquals(appData.length, wolaMessage.getDataArea().length);
        assertEquals(serviceName, wolaMessage.getServiceName());

        ByteBuffer raw = wolaMessage.toByteBuffer();

        // The total message size is equal to the WolaMessage header + the context area header + the length
        // of the service name context + the length of the cics context + the length of the data area.
        int serviceNameContextLen = WolaServiceNameContext.NameOffset + serviceName.length();
        int cicsLinkServerContextLen = CicsLinkServerContext.ContextSize;
        int contextAreaLen = WolaMessageContextArea.HeaderSize + serviceNameContextLen + cicsLinkServerContextLen;
        int expectedTotalMessageSize = WolaMessage.HeaderSize + contextAreaLen + appData.length;

        assertEquals(expectedTotalMessageSize, raw.remaining());
        assertEquals(expectedTotalMessageSize, raw.getInt(WolaMessage.TotalMessageSizeOffset));

        // The context area should sit right after the message header
        assertEquals(WolaMessage.HeaderSize, raw.getInt(WolaMessage.ContextAreaOffsetOffset));
        assertEquals(contextAreaLen, raw.getInt(WolaMessage.ContextAreaLengthOffset));

        // Verify the context area data looks right.
        assertEquals(WolaMessageContextArea.EyeCatcher, raw.getLong(WolaMessage.HeaderSize + WolaMessageContextArea.EyeCatcherOffset));
        assertEquals(2, raw.getInt(WolaMessage.HeaderSize + WolaMessageContextArea.NumContextsOffset)); // num contexts

        // Verify the serviceName context looks right.
        // The contexts sit just after the WolaMessageContextArea header
        int serviceNameContextOffset = WolaMessage.HeaderSize + WolaMessageContextArea.HeaderSize;
        assertEquals(ByteBuffer.wrap(WolaServiceNameContext.EyeCatcher).getLong(), raw.getLong(serviceNameContextOffset + WolaMessageContext.EyeCatcherOffset));
        assertEquals(WolaMessageContextId.BBOASNC_Identifier.nativeValue, raw.getInt(serviceNameContextOffset + WolaMessageContext.ContextIdOffset));

        // Note: the contextLen doesn't include the context header
        assertEquals(serviceNameContextLen - WolaMessageContext.HeaderSize, raw.getInt(serviceNameContextOffset + WolaMessageContext.ContextLenOffset));
        assertEquals(serviceName.length(), raw.getShort(serviceNameContextOffset + WolaServiceNameContext.NameLengthOffset));

        byte[] serviceNameBytes = new byte[serviceName.length()];
        ((ByteBuffer) raw.duplicate().position(serviceNameContextOffset + WolaServiceNameContext.NameOffset)).get(serviceNameBytes);
        assertEquals(serviceName, new String(serviceNameBytes, CodepageUtils.EBCDIC));

        verifyCicsLinkServerContext(wolaJcaRequestInfo, raw, serviceNameContextOffset + serviceNameContextLen);

        // These fields from the WolaMessage header are populated by the wolaJcaRequestInfo.
        assertEquals(wolaJcaRequestInfo.getMvsUserID(), wolaMessage.getMvsUserId());
        assertEquals(wolaJcaRequestInfo.getConnectionID(), raw.getInt(WolaMessage.JcaConnectionIdManagedConnectionIdOffset));

        // The app data should sit after the context area.
        assertEquals(WolaMessage.HeaderSize + contextAreaLen, raw.getInt(WolaMessage.DataAreaOffsetOffset));
        assertEquals(appData.length, raw.getInt(WolaMessage.DataAreaLengthOffset));

        byte[] appDataBytes = new byte[appData.length];
        ((ByteBuffer) raw.duplicate().position(raw.getInt(WolaMessage.DataAreaOffsetOffset))).get(appDataBytes);
        assertArrayEquals(appData, appDataBytes);
    }

    /**
     * Verify the CicsLinkServerContext looks right.
     *
     * The data marshalled in the raw buffer should match the data in the wolaJcaRequestInfo.
     */
    private void verifyCicsLinkServerContext(WolaJcaRequestInfo wolaJcaRequestInfo, ByteBuffer raw, int cicsLinkServerContextOffset) {

        assertEquals(ByteBuffer.wrap(CicsLinkServerContext.EyeCatcher).getLong(), raw.getLong(cicsLinkServerContextOffset + WolaMessageContext.EyeCatcherOffset));
        assertEquals(WolaMessageContextId.CicsLinkServerContextId.nativeValue, raw.getInt(cicsLinkServerContextOffset + WolaMessageContext.ContextIdOffset));

        // Note: the contextLen doesn't include the context header
        assertEquals(CicsLinkServerContext.ContextSize - WolaMessageContext.HeaderSize, raw.getInt(cicsLinkServerContextOffset + WolaMessageContext.ContextLenOffset));

        assertEquals(wolaJcaRequestInfo.getConnectionWaitTimeout(), raw.getInt(cicsLinkServerContextOffset + CicsLinkServerContext.ConnectionWaitTimeoutOffset));
        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskTranID(), CicsLinkServerContext.LinkTaskTranIDLength),
                     getEbcdicString(raw, cicsLinkServerContextOffset + CicsLinkServerContext.LinkTaskTranIDOffset, CicsLinkServerContext.LinkTaskTranIDLength));

        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskReqContID(), CicsLinkServerContext.LinkTaskReqContIDLength),
                     getEbcdicString(raw, cicsLinkServerContextOffset + CicsLinkServerContext.LinkTaskReqContIDOffset, CicsLinkServerContext.LinkTaskReqContIDLength));
        assertEquals(wolaJcaRequestInfo.getLinkTaskReqContType(), raw.getInt(cicsLinkServerContextOffset + CicsLinkServerContext.LinkTaskReqContTypeOffset));

        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskRspContID(), CicsLinkServerContext.LinkTaskRspContIDLength),
                     getEbcdicString(raw, cicsLinkServerContextOffset + CicsLinkServerContext.LinkTaskRspContIDOffset, CicsLinkServerContext.LinkTaskRspContIDLength));
        assertEquals(wolaJcaRequestInfo.getLinkTaskRspContType(), raw.getInt(cicsLinkServerContextOffset + CicsLinkServerContext.LinkTaskRspContTypeOffset));

        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskChanID(), CicsLinkServerContext.LinkTaskChanIDLength),
                     getEbcdicString(raw, cicsLinkServerContextOffset + CicsLinkServerContext.LinkTaskChanIDOffset, CicsLinkServerContext.LinkTaskChanIDLength));
        assertEquals(wolaJcaRequestInfo.getLinkTaskChanType(), raw.getInt(cicsLinkServerContextOffset + CicsLinkServerContext.LinkTaskChanTypeOffset));

        assertEquals(wolaJcaRequestInfo.getUseCICSContainer(), raw.getInt(cicsLinkServerContextOffset + CicsLinkServerContext.UseCICSContainerOffset));

    }

    /**
     * Read a string from the buffer.
     */
    private String getEbcdicString(ByteBuffer raw, int index, int maxLen) {
        byte[] bytes = new byte[maxLen];
        ((ByteBuffer) raw.duplicate().position(index)).get(bytes);

        try {
            return truncateAtNull(new String(bytes, CodepageUtils.EBCDIC));

        } catch (UnsupportedEncodingException uee) {
            throw new IllegalArgumentException(uee);
        }
    }

    /**
     * @return the given String truncated at the first null byte in the String.
     *         E.g. "abc\0def" would return "abc".
     */
    private String truncateAtNull(String s) {
        int firstNull = s.indexOf(0);
        return (firstNull < 0) ? s : s.substring(0, firstNull);
    }

}
