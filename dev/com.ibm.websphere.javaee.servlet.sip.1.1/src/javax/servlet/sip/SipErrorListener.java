/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package javax.servlet.sip;

import java.util.EventListener;

/**
 * Causes applications to be notified of various error conditions
 * occurring during regular SIP transaction processing.
 */
public interface SipErrorListener extends EventListener {
    /**
     * Invoked by the servlet container to notify an application that
     * no ACK was received for an INVITE transaction for which a final
     * response has been sent upstream.
     * 
     * <p>This method is invoked in UASs only, not in applications
     * that proxied the INVITE.
     * 
     * @param ee    the event that identifies the request
     */
    void noAckReceived(SipErrorEvent ee);

    /**
     * Invoked by the servlet container for applications acting as a UAS
     * when no PRACK was received for a previously sent reliable provisional
     * response. It is then up to the application to generate the 5xx
     * response reccommended by RFC 3262 for the INVITE transaction.
     * The original INVITE request as well as the unacknowledged reliable
     * response is available from the <code>SipErrorEvent</code> argument.
     * 
     * @param ee    the event that identifies the original request and
     *              the reliable provisional response
     */
    void noPrackReceived(SipErrorEvent ee);
}
