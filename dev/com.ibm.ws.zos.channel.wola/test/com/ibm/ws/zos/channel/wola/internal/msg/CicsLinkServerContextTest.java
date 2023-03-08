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
package com.ibm.ws.zos.channel.wola.internal.msg;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;
import com.ibm.ws.zos.channel.wola.internal.TestWolaJcaRequestInfoImpl;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 *
 */
public class CicsLinkServerContextTest {

    /**
     * Verify the CicsLinkServerContext looks right.
     *
     * The data marshalled in the raw buffer should match the data in the WolaJcaRequestInfo.
     */
    @Test
    public void verifyCicsLinkServerContext() {

        WolaJcaRequestInfo wolaJcaRequestInfo = new TestWolaJcaRequestInfoImpl();
        CicsLinkServerContext cicsLinkServerContext = new CicsLinkServerContext(wolaJcaRequestInfo);
        ByteBuffer raw = ByteBuffer.wrap(cicsLinkServerContext.getBytes());

        assertEquals(ByteBuffer.wrap(CicsLinkServerContext.EyeCatcher).getLong(), raw.getLong(WolaMessageContext.EyeCatcherOffset));
        assertEquals(WolaMessageContextId.CicsLinkServerContextId.nativeValue, raw.getInt(WolaMessageContext.ContextIdOffset));

        // Note: the contextLen doesn't include the context header
        assertEquals(CicsLinkServerContext.ContextSize - WolaMessageContext.HeaderSize, raw.getInt(WolaMessageContext.ContextLenOffset));

        assertEquals(wolaJcaRequestInfo.getConnectionWaitTimeout(), raw.getInt(CicsLinkServerContext.ConnectionWaitTimeoutOffset));
        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskTranID(), CicsLinkServerContext.LinkTaskTranIDLength),
                     getEbcdicString(raw, CicsLinkServerContext.LinkTaskTranIDOffset, CicsLinkServerContext.LinkTaskTranIDLength));

        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskReqContID(), CicsLinkServerContext.LinkTaskReqContIDLength),
                     getEbcdicString(raw, CicsLinkServerContext.LinkTaskReqContIDOffset, CicsLinkServerContext.LinkTaskReqContIDLength));
        assertEquals(wolaJcaRequestInfo.getLinkTaskReqContType(), raw.getInt(CicsLinkServerContext.LinkTaskReqContTypeOffset));

        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskRspContID(), CicsLinkServerContext.LinkTaskRspContIDLength),
                     getEbcdicString(raw, CicsLinkServerContext.LinkTaskRspContIDOffset, CicsLinkServerContext.LinkTaskRspContIDLength));
        assertEquals(wolaJcaRequestInfo.getLinkTaskRspContType(), raw.getInt(CicsLinkServerContext.LinkTaskRspContTypeOffset));

        assertEquals(CodepageUtils.padRight(wolaJcaRequestInfo.getLinkTaskChanID(), CicsLinkServerContext.LinkTaskChanIDLength),
                     getEbcdicString(raw, CicsLinkServerContext.LinkTaskChanIDOffset, CicsLinkServerContext.LinkTaskChanIDLength));
        assertEquals(wolaJcaRequestInfo.getLinkTaskChanType(), raw.getInt(CicsLinkServerContext.LinkTaskChanTypeOffset));

        assertEquals(wolaJcaRequestInfo.getUseCICSContainer(), raw.getInt(CicsLinkServerContext.UseCICSContainerOffset));
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
