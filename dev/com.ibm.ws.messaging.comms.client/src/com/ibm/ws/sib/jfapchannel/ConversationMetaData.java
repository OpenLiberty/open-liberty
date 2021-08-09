/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel;

import java.net.InetAddress;

import javax.net.ssl.SSLSession;

/**
 * Meta information about a conversation.
 */
public interface ConversationMetaData
{
   /**
    * The chain name that conversation has been established over.
    * @return Chain name.
    */
   String getChainName();
   
   /**
    * True if the conversation passed through a SSL channel.  False otherwise.
    * @return True iff the conversation passed through an SSL channel.
    */
   boolean containsSSLChannel();
   
   /**
    * True if the conversation passed through a HTTP tunnel channel.  False otherwise.
    * @return True iff the conversation passed through a HTTP tunnel channel.
    */
   boolean containsHTTPTunnelChannel();
   
   /**
    * True if the conversation is inbound.  I.e. the conversation was established by our peer.
    * False if the conversation is outbound (ie. we established the conversation with our peer).
    * @return True iff the conversation is inbound
    */
   boolean isInbound();

   /**
    * @return True if the conversation is over a "trusted" connection.  An example of such
    * a connection would be the z/OS cross memory channel.
    */
   boolean isTrusted();                                                 // F224759.1
   
   /**
    * @return Network address of the peer to which this connection is connected.
    * The value returned might be:<ul>
    * <li>The network address.</li>
    * <li>A network address - but not the address of the peer (in the
    * eventuality that network address translation or other obfuscation has
    * taken place).</li>
    * <li>null - in the case where a network address doesn't make any sense.
    * For example, where the underlying communications medium is not a network.</li>
    * </ul>
    */
   InetAddress getRemoteAddress();                                       // F206161.5
   
   /**
    * @return Returns the port number of the remote connection. If it is not known, 0 is returned.
    */
   int getRemotePort();
   
   /**
    * @return Returns the SSL Session associated with the connection (or null if this is
    *         not an SSL connection).
    */
   SSLSession getSSLSession();
}
