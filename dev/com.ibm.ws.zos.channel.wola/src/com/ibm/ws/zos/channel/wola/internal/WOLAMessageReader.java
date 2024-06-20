/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.wola.internal;

import java.nio.ByteBuffer;

import com.ibm.ws.zos.channel.local.LocalCommReadCompletedCallback;
import com.ibm.ws.zos.channel.local.LocalCommServiceContext;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessage;
import com.ibm.ws.zos.channel.wola.internal.msg.WolaMessageParseException;

/**
 * Reads, parses, and dispatches inbound WOLA messages.
 *
 * There's one WOLAMessageReader per WOLAConnectionLink.
 *
 * This guy will keep reading and parsing and dispatching WOLA messages
 * until it is cancelled (due to the connection closing) or until it suffers
 * a read error (in which case it will close the connection itself).
 *
 * Inbound requests are send to WolaRequestDispatcher for dispatching
 * into the EJB container. Inbound responses are sent to WolaOutboundRequestService,
 * which posts the Future who's waiting on that particular response.
 *
 */
public class WOLAMessageReader implements LocalCommReadCompletedCallback {

    /**
     * A reference to the WOLAConnectionLink that's associated with the messages read
     * by this WOLAMessageReader.
     */
    private final WolaConnLink wolaConnectionLink;

    /**
     * CTOR.
     *
     * @param wolaConnectionLink - The connection link associated with the messages read by
     *                               this guy.
     */
    protected WOLAMessageReader(WolaConnLink wolaConnectionLink) {
        this.wolaConnectionLink = wolaConnectionLink;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void ready(LocalCommServiceContext context, ByteBuffer data) {
        parseAndDispatch(context, new WOLAMessageParser(null, data));
    }

    /**
     * Note: if the exception parm is null, it means our callback is being
     * cancelled because the connection is closing. In that case, this method
     * does nothing. Otherwise, it initiates a close of the connection.
     *
     * {@inheritDoc}
     */
    @Override
    public void error(LocalCommServiceContext context, Exception e) {
        // TODO: There was an error while reading data on this connection.
        //       Should close the connection?  Try to read again?  Try to read
        //       X more times before giving up?

        if (e != null) {
            // The read failed for some reason.  Close the connection.
            wolaConnectionLink.close(wolaConnectionLink.getVirtualConnection(), e);
        }
    }

    /**
     * Parse and dispatch the WOLA message from the given raw message data.
     *
     * @param context
     * @param wolaMessageParser - contains the data we just read (plus any incomplete message
     *                              data from a previous read).
     */
    protected void parseAndDispatch(LocalCommServiceContext context, WOLAMessageParser wolaMessageParser) {

        try {

            parseAndDispatchInternal(context, wolaMessageParser);

        } catch (WolaMessageParseException wmpe) {
            // The WOLA message was bad.
            // Call into error(), which will close the connection.
            error(context, wmpe);
        }
    }

    /**
     * parseAndDispatch simply wraps a try-catch around this method.
     *
     * @param context
     * @param wolaMessageParser
     * @throws WolaMessageParseException
     */
    private void parseAndDispatchInternal(LocalCommServiceContext context, WOLAMessageParser wolaMessageParser) throws WolaMessageParseException {

        // Parse a complete WOLA message (if possible) from the given data (in WOLAMessageParser).
        //
        // Note: WOLAMessageParser was provided not only the data we just read but also any leftover
        // data from a previous incomplete read (see the callback object defined in this method).
        //
        // Any partial message data (i.e. incomplete messages or additional full messages) is contained
        // in the leftover ByteBuffer.

        WolaMessage wolaMessage = wolaMessageParser.parseMessage();
        final WOLAMessageLeftovers leftovers = wolaMessageParser.getLeftovers();

        // Set up the next callback to include the leftover data.
        WolaReadCompletedCallback callback = new WolaReadCompletedCallback(leftovers);

        // If we parsed a full message, dispatch it and set up the next action (another parse or read).
        if (wolaMessage != null) {
            if (leftovers == null) {
                context.asyncRead(callback); // Start the next read
                dispatch(wolaMessage, context, false); // Dispatch the current message
            } else {
                dispatch(wolaMessage, context, true);
                parseAndDispatchInternal(context, new WOLAMessageParser(leftovers.getByteBuffers()));
            }
        } else {
            // Either there's leftover data or we didn't read anything (no message to dispatch).
            // Read again, possibly synchronously (Note: this is a recursive call. In a very unlikely
            // scenario, this could cause stack overflow).
            context.read(callback);
        }
    }

    /**
     * Dispatch the given WOLA message.
     *
     * If it's a request, send it to WolaRequestDispatcher.
     *
     * If it's a response, send it to WolaOutboundRequestService, which will
     * post the appropriate Future waiting for the response.
     *
     * @param wolaMessage       The complete WOLA message
     * @param context           The local comm service context which was used to read the WOLA message.
     * @param moreDataToProcess A boolean flag indicating there is more data to process after this message
     *                              has been processed. If there is more data to process, we don't want to
     *                              release the disconnect lock on the context.
     */
    protected void dispatch(WolaMessage wolaMessage, LocalCommServiceContext context, boolean moreDataToProcess) {

        if (wolaMessage.isRequest()) {

            // Release the disconnect lock PRIOR to dispatching the request
            if (moreDataToProcess == false) {
                context.releaseDisconnectLock();
            }

            WOLARequestDispatcher wrd = new WOLARequestDispatcher(wolaConnectionLink, wolaMessage).setWolaRequestInterceptors(WOLAChannelFactoryProvider.getInstance().getWolaRequestInterceptors()); // TODO: inject ref instead of static call

            // If we can use this thread to dispatch work, do it.  Otherwise, queue to the
            // executor.
            if (context.isDispatchThread()) {
                wrd.run();
            } else {
                WOLAChannelFactoryProvider.getInstance().getExecutorService().execute(wrd);
            }
        } else {

            wolaConnectionLink.getOutboundRequestService().postResponse(wolaMessage);

            // Release the disconnect lock AFTER posting the waiting request thread.
            if (moreDataToProcess == false) {
                context.releaseDisconnectLock();
            }
        }
    }

    /**
     * Callback object for reads. This guy gets called by the underlying localcomm
     * channel when a read/asyncRead request is satisfied.
     *
     * Note: the callback may be called synchronously under a read() if deata is immediately
     * available.
     *
     */
    private class WolaReadCompletedCallback implements LocalCommReadCompletedCallback {

        /**
         * Incomplete WolaMessage data leftover from a previous partial read.
         */
        private final WOLAMessageLeftovers leftoversFromPreviousRead;

        /**
         * CTOR.
         */
        public WolaReadCompletedCallback(WOLAMessageLeftovers leftovers) {
            this.leftoversFromPreviousRead = leftovers;
        }

        @Override
        public void ready(LocalCommServiceContext context, ByteBuffer data) {
            // Call right back into parseAndDispatch, passing in the leftover
            // data from the previous round of parsing.
            parseAndDispatch(context, new WOLAMessageParser(leftoversFromPreviousRead, data));
        }

        @Override
        public void error(LocalCommServiceContext context, Exception e) {
            // Defer to the containing class's impl.
            WOLAMessageReader.this.error(context, e);
        }
    };

}
