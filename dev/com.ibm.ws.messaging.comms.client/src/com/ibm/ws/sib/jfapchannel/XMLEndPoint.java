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
package com.ibm.ws.sib.jfapchannel;

import java.net.InetAddress;

/**
 * The interface for an endpoint that was created from XML information given back from a TRM 
 * handshake. This is needed in the portly client where an XML TRM handshake is performed to work
 * out the endpoint of the correct ME.
 * 
 * @author Adrian Preston
 */
public abstract class XMLEndPoint
{
   /** Enum of the possible chain types this endpoint could represent */
   public static enum ChainTypeEnumeration { HTTP, HTTPS, SSL, TCP, UNKNOWN };
   
   /**
    * @return Returns the address this endpoint is connecting to.
    */
   public abstract InetAddress getAddress();
   
   /**
    * @return Returns the port this endpoint is connecting to.
    */
   public abstract int getPort();
   
   /**
    * @return Returns the chain type enumeration.
    */
   public abstract ChainTypeEnumeration getType();
   
   /**
    * @see java.lang.Object#equals(java.lang.Object)
    */
   public final boolean equals(Object o)
   {
      boolean result;
      
      if (o instanceof XMLEndPoint)
      {
         XMLEndPoint ep = (XMLEndPoint)o;
         result = getAddress().equals(ep.getAddress()) &&
                  (getPort() == ep.getPort()) &&
                  (getType() == ep.getType());
      }
      else
      {
         result = false;
      }
      return result;
   }

   /**
    * @see java.lang.Object#hashCode()
    */
   public final int hashCode()
   {
      return getAddress().hashCode() ^ 
             getPort() ^
             getType().hashCode();
   }
   
   // These are methods that are present on a real CFEndPoint but are not needed for us
   
//   public final String getName() {throw new RuntimeException();}
//   public final void setName(String name) {throw new RuntimeException();}
//   public final String getVhost() {throw new RuntimeException();}
//   public final OutboundChannelDefinition[] getOutboundChannelDefs() {throw new RuntimeException();}
//   public final Class getChannelAccessor() {throw new RuntimeException();}
//   public final WSChainData createOutboundChain() throws ChannelException, ChainException {throw new RuntimeException();}
//   public final WSVirtualConnectionFactory getOutboundVCFactory() {throw new RuntimeException();}
//   public final void setOutboundVCFactory(WSVirtualConnectionFactory vcf) {throw new RuntimeException();}
//   public final boolean isSSLEnabled() {throw new RuntimeException();}
//   public final boolean isLocal() {throw new RuntimeException();}
//   public final WSVirtualConnectionFactory getOutboundVCFactory(Map sslProps, boolean overwriteExisting) throws IllegalArgumentException {throw new RuntimeException();}
//   public final String serializeToXML() throws NotSerializableException {throw new RuntimeException();}
}
