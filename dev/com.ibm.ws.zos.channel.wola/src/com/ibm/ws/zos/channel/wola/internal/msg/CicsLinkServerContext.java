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

import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;
import com.ibm.ws.zos.channel.wola.internal.ByteBufferVector;
import com.ibm.ws.zos.channel.wola.internal.natv.CodepageUtils;

/**
 * A WolaMessage context for carrying along a bunch of extra info needed by
 * the Link Server to invoke the CICS service.
 *
 */
public class CicsLinkServerContext extends WolaMessageContext {

    /**
     * Eyecatcher.
     */
    public static final byte[] EyeCatcher = CodepageUtils.getEbcdicBytesPadded("BBOACLSC", 8);

    /**
     * Field offsets.
     */
    public static final int ConnectionWaitTimeoutOffset = 0x10; // Immediately follows header
    public static final int LinkTaskTranIDOffset = 0x14;
    public static final int LinkTaskReqContIDOffset = 0x18;
    public static final int LinkTaskReqContTypeOffset = 0x28;
    public static final int LinkTaskRspContIDOffset = 0x2c;
    public static final int LinkTaskRspContTypeOffset = 0x3c;
    public static final int LinkTaskChanIDOffset = 0x40;
    public static final int LinkTaskChanTypeOffset = 0x50;
    public static final int UseCICSContainerOffset = 0x54;

    /**
     * Field lengths (for padded fields).
     */
    public static final int LinkTaskTranIDLength = 4;
    public static final int LinkTaskReqContIDLength = 16;
    public static final int LinkTaskRspContIDLength = 16;
    public static final int LinkTaskChanIDLength = 16;

    /**
     * The total size of the context, including the header.
     */
    public static final int ContextSize = 0x58;

    public final byte[] rawBytes;

    /**
     * CTOR.
     */
    public CicsLinkServerContext(WolaJcaRequestInfo wolaJcaRequestInfo) {
        super(CicsLinkServerContext.EyeCatcher, WolaMessageContextId.CicsLinkServerContextId);

        ByteBufferVector rawBytes = new ByteBufferVector(new byte[ContextSize]);
        fillHeader(rawBytes);
        rawBytes.putInt(ConnectionWaitTimeoutOffset,
                        wolaJcaRequestInfo.getConnectionWaitTimeout()).putStringFieldPadded(LinkTaskTranIDOffset, LinkTaskTranIDLength, wolaJcaRequestInfo.getLinkTaskTranID(),
                                                                                            CodepageUtils.EBCDIC).putStringFieldPadded(LinkTaskReqContIDOffset,
                                                                                                                                       LinkTaskReqContIDLength,
                                                                                                                                       wolaJcaRequestInfo.getLinkTaskReqContID(),
                                                                                                                                       CodepageUtils.EBCDIC).putInt(LinkTaskReqContTypeOffset,
                                                                                                                                                                    wolaJcaRequestInfo.getLinkTaskReqContType()).putStringFieldPadded(LinkTaskRspContIDOffset,
                                                                                                                                                                                                                                      LinkTaskRspContIDLength,
                                                                                                                                                                                                                                      wolaJcaRequestInfo.getLinkTaskRspContID(),
                                                                                                                                                                                                                                      CodepageUtils.EBCDIC).putInt(LinkTaskRspContTypeOffset,
                                                                                                                                                                                                                                                                   wolaJcaRequestInfo.getLinkTaskRspContType()).putStringFieldPadded(LinkTaskChanIDOffset,
                                                                                                                                                                                                                                                                                                                                     LinkTaskChanIDLength,
                                                                                                                                                                                                                                                                                                                                     wolaJcaRequestInfo.getLinkTaskChanID(),
                                                                                                                                                                                                                                                                                                                                     CodepageUtils.EBCDIC).putInt(LinkTaskChanTypeOffset,
                                                                                                                                                                                                                                                                                                                                                                  wolaJcaRequestInfo.getLinkTaskChanType()).putInt(UseCICSContainerOffset,
                                                                                                                                                                                                                                                                                                                                                                                                                   wolaJcaRequestInfo.getUseCICSContainer());

        this.rawBytes = rawBytes.toByteArray();
    }

    /** {@inheritDoc} */
    @Override
    byte[] getBytes() {
        return rawBytes;
    }

}
