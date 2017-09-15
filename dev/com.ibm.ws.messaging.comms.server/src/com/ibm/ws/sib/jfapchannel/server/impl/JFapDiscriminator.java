/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.server.impl;

import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jfap.inbound.channel.CommsServerServiceFacade;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.Discriminator;
import com.ibm.wsspi.channelfw.VirtualConnection;

/**
 * Discriminator used to identify if an transmission can be interpreted by the JFAP
 * channel. This boils down to looking at the first few bytes to see if we recognise
 * the eyecatcher.
 * 
 * @author prestona
 */
public class JFapDiscriminator implements Discriminator {
    private static final TraceComponent tc = SibTr.register(JFapDiscriminator.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);

    private static byte EYECATCHER[] = { (byte) 0xBE, (byte) 0xEF };

    static {
        if (tc.isDebugEnabled())
            SibTr.debug(tc, "@(#) SIB/ws/code/sib.jfapchannel.server.impl/src/com/ibm/ws/sib/jfapchannel/impl/JFapDiscriminator.java, SIB.comms, WASX.SIB, aa1225.01 1.16");
    }

    private Channel channel = null;

    /**
     * Creates a new discriminator for a JFAP channel
     * 
     * @param channel
     *            The JFAP channel the discriminator will be discriminating on behalf of.
     */
    public JFapDiscriminator(Channel channel) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", channel);
        this.channel = channel;
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Look at the data and determine if we can process it or not. If there are enough bytes of data
     * to look for an eyecatcher, we can give a definitive answer - otherwise the best we can do is
     * say no or maybe.
     */
    public int discriminate(VirtualConnection vc, Object discrimData) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "discriminate", new Object[]
            {
             vc, discrimData
            });

        int answer;
        // begin D218910
        WsByteBuffer[] dataArray = (WsByteBuffer[]) discrimData;

        // begin D234423
        if (dataArray == null) {
            if (tc.isDebugEnabled())
                SibTr.debug(this, tc, "timeout prior to discrimination.");
            answer = MAYBE;
        } else {
            WsByteBuffer data = null;
            int originalPosition = 0;
            int originalLimit = 0;
            switch (dataArray.length) {
                case 0:
                    if (tc.isDebugEnabled())
                        SibTr.debug(this, tc, "discriminate", "MAYBE");
                    if (tc.isEntryEnabled())
                        SibTr.exit(this, tc, "discriminate");
                    return MAYBE;
                case 1:
                    originalPosition = dataArray[0].position();
                    originalLimit = dataArray[0].limit();
                    data = dataArray[0].flip();
                    break;
                default:
                    int totalAmount = 0;
                    for (int i = 0; i < dataArray.length; ++i) {
                        totalAmount += dataArray[i].position();
                    }
                    data = CommsServerServiceFacade.getBufferPoolManager().allocate(totalAmount);
                    for (int i = 0; i < dataArray.length; ++i) {
                        originalPosition = dataArray[i].position();
                        originalLimit = dataArray[i].limit();
                        dataArray[i].flip();
                        data.put(dataArray[i]);
                        dataArray[i].position(originalPosition);
                        dataArray[i].limit(originalLimit);
                    }
                    data.flip();
                    break;
            }
            // end D218910

            if (tc.isDebugEnabled()) {
                byte[] debugData = null;
                int start = 0;

                if (data.hasArray()) {
                    debugData = data.array();
                    start = data.arrayOffset() + data.position();;
                } else {
                    debugData = new byte[32];
                    int pos = data.position();
                    data.get(debugData);
                    data.position(pos);
                    start = 0;
                }

                SibTr.bytes(this, tc, debugData, start, 32, "Discrimination Data");
            }

            if (data.remaining() < EYECATCHER.length) {
                // Not enought data to tell for sure - but maybe enough to eliminate ourself
                answer = MAYBE;
                for (int i = 0; (data.remaining() > 0) && (answer == MAYBE); ++i) {
                    if (data.get() != EYECATCHER[i])
                        answer = NO;
                }
            } else {
                // Enough data to be able to decide if this is ours or not.
                answer = YES;
                for (int i = 0; (i < EYECATCHER.length) && (answer == YES); ++i) {
                    if (data.get() != EYECATCHER[i])
                        answer = NO;
                }
            }

            data.position(originalPosition); // D218910
            data.limit(originalLimit); // D218910
        }
        // end D234423

        if (tc.isDebugEnabled()) {
            switch (answer) {
                case (YES):
                    SibTr.debug(this, tc, "discriminate", "YES");
                    break;
                case (NO):
                    SibTr.debug(this, tc, "discriminate", "NO");
                    break;
                case (MAYBE):
                    SibTr.debug(this, tc, "discriminate", "MAYBE");
                    break;
            }
        }
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "discriminate");
        return answer;
    }

    /**
     * Returns the data type this discriminator is able to discriminate for.
     * This is always WsByteBuffer.
     */
    public Class getDiscriminatoryDataType() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDiscriminatorDataType");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDiscriminatorDataType");
        return com.ibm.wsspi.bytebuffer.WsByteBuffer.class; // F188491
    }

    /**
     * Returns the channel this discriminator discriminates on behalf of.
     */
    public Channel getChannel() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getChannel");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getChannel", channel);
        return channel;
    }

    /**
     * Get the properties for this discriminator (none)
     */
    public Map getProperties() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getProperties");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getProperties");
        return null;
    }

    /**
     * Get the weighting to use for this discriminator
     */
    public int getWeight() {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "getWeight");
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "getWeight");
        // TODO: this probably isn't a good value.
        return 0;
    }

    // begin F188491
    public void cleanUpState(VirtualConnection vc) {
        if (tc.isEntryEnabled())
            SibTr.entry(this, tc, "cleanUpState", vc);
        if (tc.isEntryEnabled())
            SibTr.exit(this, tc, "cleanUpState");
    }
    // end F188491
}
