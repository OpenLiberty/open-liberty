/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;

import java.net.InetAddress;

/**
 * <p>
 * This interface represents the Via general-header.
 * ViaHeaders indicates the path taken by the Request
 * so far. This prevents Request looping and ensures
 * Responses take the same path as the Requests,
 * which assists in firewall traversal and other unusual
 * routing situations.
 * </p><p>
 * <B>Requests</B>
 * </p><p>
 * The client originating the Request must insert into
 * the Request a ViaHeader containing its host name
 * or network address and, if not the default port number, the
 * port number at which it wishes to receive Responses.
 * (Note that this port number can differ from the UDP source
 * port number of the Request.) A fully-qualified domain
 * name is recommended. Each subsequent proxy server that sends
 * the Request onwards must add its own additional ViaHeader
 * before any existing ViaHeaders. A proxy that receives a redirection
 * Response and then searches recursively, must use the
 * same ViaHeaders as on the original proxied Request.
 * </p><p>
 * A proxy should check the top-most ViaHeader to ensure that it
 * contains the sender's correct network address, as seen from
 * that proxy. If the sender's address is incorrect, the proxy
 * must add an additional received attribute.
 * </p><p>
 * A host behind a network address translator (NAT) or
 * firewall may not be able to insert a network address into
 * the ViaHeader that can be reached by the next hop beyond
 * the NAT. Use of the received attribute allows SIP Requests
 * to traverse NAT's which only modify the source IP address.
 * NAT's which modify port numbers, called Network Address
 * Port Translator's (NAPT) will not properly pass SIP when
 * transported on UDP, in which case an application layer
 * gateway is required. When run over TCP, SIP stands a better
 * chance of traversing NAT's, since its behavior is similar
 * to HTTP in this case (but of course on different ports).
 * </p><p>
 * A proxy sending a Request to a multicast address must
 * add the maddr parameter to its ViaHeader, and should add the
 * ttl parameter. If a server receives a Request which
 * contained an maddr parameter in the topmost ViaHeader, it
 * should send the Response to the multicast address
 * listed in the maddr parameter.
 * </p><p>
 * If a proxy server receives a Request which contains
 * its own address in a ViaHeader, it must respond with a
 * LOOP_DETECTED Response.
 * </p><p>
 * A proxy server must not forward a Request to a
 * multicast group which already appears in any of the ViaHeaders.
 * This prevents a malfunctioning proxy server from causing
 * loops. Also, it cannot be guaranteed that a proxy server
 * can always detect that the address returned by a location
 * service refers to a host listed in the ViaHeader list, as a
 * single host may have aliases or several network interfaces.
 * </p><p>
 * <B>Receiver-tagged Via Headers</B>
 * </p><p>
 * Normally, every host that sends or forwards a Request
 * adds a ViaHeader indicating the path traversed. However, it
 * is possible that Network Address Translators (NATs) changes
 * the source address and port of the Request
 * (e.g., from net-10 to a globally routable address), in which
 * case the Via header field cannot be relied on to route replies.
 * To prevent this, a proxy should check the top-most ViaHeader to
 * ensure that it contains the sender's correct network
 * address, as seen from that proxy. If the sender's address
 * is incorrect, the proxy must add a received parameter to the
 * ViaHeader inserted by the previous hop. Such a modified ViaHeader
 * is known as a receiver-tagged ViaHeader.
 * </p><p>
 * <B>Responses</B>
 * </p><p>
 * ViaHeaders in Responses are processed by a proxy or
 * UAC according to the following rules:
 * </p>
 * <LI>
 * The first ViaHeader should indicate the proxy or
 * client processing this Response. If it does not,
 * discard the Response. Otherwise, remove this ViaHeader.
 * </LI>
 * <LI>
 * If there is no second ViaHeader, this Response is
 * destined for this client. Otherwise, the processing depends
 * on whether the ViaHeader contains a maddr parameter or is
 * a receiver-tagged field.
 * </LI>
 * <LI>
 * If the second ViaHeader contains a maddr parameter, send
 * the Response to the multicast address listed there,
 * using the port indicated in "sent-by", or port 5060 if none
 * is present. The Response should be sent using the TTL
 * indicated in the ttl parameter, or with a TTL of 1 if that
 * parameter is not present. For robustness, Responses
 * must be sent to the address indicated in the maddr parameter
 * even if it is not a multicast address.
 * </LI>
 * <LI>
 * If the second ViaHeader does not contain a maddr parameter and
 * is a receiver-tagged ViaHeader , send the Response to
 * the address in the received parameter, using the port indicated
 * in the port value, or using port 5060 if none is present.
 * </LI>
 * <LI>
 * If neither of the previous cases apply, send the Response
 * to the address indicated by the host value in the second ViaHeader.
 * </LI>
 * <p>
 * <B>User Agent and Redirect Servers</B>
 * </p><p>
 * A UAS or redirect server sends a Response based on
 * one of the following rules:
 * </p>
 * <LI>
 * If the first ViaHeader in the Request contains a
 * maddr parameter, send the Response to the multicast
 * address listed there, using the port indicated, or port
 * 5060 if none is present. The Response should be sent
 * using the TTL indicated in the ttl parameter, or with a TTL
 * of 1 if that parameter is not present. For robustness,
 * Responses must be sent to the address indicated in the
 * maddr parameter even if it is not a multicast address.
 * </LI>
 * <LI>
 * If the address in the first ViaHeader differs from the source
 * address of the packet, send the Response to the actual packet
 * source address, similar to the treatment for receiver-tagged
 * ViaHeaders.
 * </LI>
 * <LI>
 * If neither of these conditions is true, send the Response
 * to the address contained in the host value. If the Request
 * was sent using TCP, use the existing TCP connection if available.
 * </LI>
 * <p>
 * <B>ViaHeader Parameters</B>
 * </p><p>
 * The defaults for protocol-name and transport are "SIP" and "UDP",
 * respectively. The maddr parameter, designating the multicast
 * address, and the ttl parameter, designating the time-to-live (TTL)
 * value, are included only if the Request was sent via multicast.
 * The received parameter is added only for receiver-added ViaHeaders
 * For reasons of privacy, a client or proxy may wish to hide its ViaHeader
 * information by encrypting it. The hidden parameter is included if this
 * ViaHeader was hidden by the upstream proxy. Note that privacy of the
 * proxy relies on the cooperation of the next hop, as the next-hop proxy
 * will, by necessity, know the IP address and port number of the source
 * host.
 * </p><p>
 * The branch parameter is included by every forking proxy.  The token
 * must be unique for each distinct Request generated when a proxy
 * forks. CANCEL Requests must have the same branch value as the
 * corresponding forked Request. When a Response arrives at the proxy it
 * can use the branch value to figure out which branch the Response
 * corresponds to. A proxy which generates a single Request (non-
 * forking) may also insert the branch parameter. The identifier has
 * to be unique only within a set of isomorphic Requests.
 * </p>
 *
 * @version 1.0
 *
 */
public interface ViaHeader extends ParametersHeader
{
    
    /**
     * Sets whether ViaHeader is hidden or not
     * @param <var>hidden</var> whether ViaHeader is hidden or not
     */
    public void setHidden(boolean hidden);
    
    /**
     * Returns boolean value indicating if ViaHeader has port
     * @return boolean value indicating if ViaHeader has port
     */
    public boolean hasPort();
    
    /**
     * Gets port of ViaHeader
     * (Returns negative int if port does not exist)
     * @return port of ViaHeader
     */
    public int getPort();
    
    /**
     * Removes TTL from ViaHeader (if it exists)
     */
    public void removeTTL();
    
    /**
     * Gets boolean value to indicate if ViaHeader
     * has TTL
     * @return boolean value to indicate if ViaHeader
     * has TTL
     */
    public boolean hasTTL();
    
    /**
     * Gets MAddr of ViaHeader
     * (Returns null if MAddr does not exist)
     * @return MAddr of ViaHeader
     */
    public String getMAddr();
    
    /**
     * Removes received from ViaHeader (if it exists)
     */
    public void removeReceived();
    
    /**
     * Removes branch from ViaHeader (if it exists)
     */
    public void removeBranch();
    
    /**
     * Sets port of ViaHeader
     * @param <var>port</var> port
     * @throws SipParseException if port is not accepted by implementation
     */
    public void setPort(int port)
                 throws SipParseException;
    
    /**
     * Sets MAddr of ViaHeader
     * @param <var>mAddr</var> MAddr
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(InetAddress mAddr)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Removes port from ViaHeader (if it exists)
     */
    public void removePort();
    
    /**
     * Sets received of ViaHeader
     * @param <var>received</var> received
     * @throws IllegalArgumentException if received is null
     * @throws SipParseException if received is not accepted by implementation
     */
    public void setReceived(String received)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets protocol version of ViaHeader
     * @return protocol version of ViaHeader
     */
    public String getProtocolVersion();
    
    /**
     * Gets boolean value to indicate if ViaHeader
     * has received
     * @return boolean value to indicate if ViaHeader
     * has received
     */
    public boolean hasReceived();
    
    /**
     * Sets protocol version of ViaHeader
     * @param <var>protocolVersion</var> protocol version
     * @throws IllegalArgumentException if protocolVersion is null
     * @throws SipParseException if protocolVersion is not accepted by implementation
     */
    public void setProtocolVersion(String protocolVersion)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets boolean value to indicate if ViaHeader
     * has branch
     * @return boolean value to indicate if ViaHeader
     * has branch
     */
    public boolean hasBranch();
    
    /**
     * Gets transport of ViaHeader
     * @return transport of ViaHeader
     */
    public String getTransport();
    
    /**
     * Returns boolean value indicating if ViaHeader is hidden
     * @return boolean value indicating if ViaHeader is hidden
     */
    public boolean isHidden();
    
    /**
     * Sets transport of ViaHeader
     * @param <var>transport</var> transport
     * @throws IllegalArgumentException if transport is null
     * @throws SipParseException if transport is not accepted by implementation
     */
    public void setTransport(String transport)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets MAddr of ViaHeader
     * @param <var>mAddr</var> MAddr
     * @throws IllegalArgumentException if mAddr is null
     * @throws SipParseException if mAddr is not accepted by implementation
     */
    public void setMAddr(String mAddr)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets host of ViaHeader
     * @return host of ViaHeader
     */
    public String getHost();
    
    /**
     * Removes MAddr from ViaHeader (if it exists)
     */
    public void removeMAddr();
    
    /**
     * Sets host of ViaHeader
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(String host)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets boolean value to indicate if ViaHeader
     * has MAddr
     * @return boolean value to indicate if ViaHeader
     * has MAddr
     */
    public boolean hasMAddr();
    
    /**
     * Sets host of ViaHeader
     * @param <var>host</var> host
     * @throws IllegalArgumentException if host is null
     * @throws SipParseException if host is not accepted by implementation
     */
    public void setHost(InetAddress host)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets received of ViaHeader
     * @param <var>received</var> received
     * @throws IllegalArgumentException if received is null
     * @throws SipParseException if received is not accepted by implementation
     */
    public void setReceived(InetAddress received)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets comment of ViaHeader
     * (Returns null if comment does not exist)
     * @return comment of ViaHeader
     */
    public String getComment();
    
    /**
     * Gets received of ViaHeader
     * (Returns null if received does not exist)
     * @return received of ViaHeader
     */
    public String getReceived();
    
    /**
     * Gets boolean value to indicate if ViaHeader
     * has comment
     * @return boolean value to indicate if ViaHeader
     * has comment
     */
    public boolean hasComment();
    
    /**
     * Sets branch of ViaHeader
     * @param <var>branch</var> branch
     * @throws IllegalArgumentException if branch is null
     * @throws SipParseException if branch is not accepted by implementation
     */
    public void setBranch(String branch)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Sets the rport parameter of this Via header.
     * This just sets the attribute name with no attribute value.
     */
    public void setRPort();
    
    /**
     * Gets the rport parameter of this Via header.
     * @return the rport parameter value, or -1 if no rport value
     */
    public int getRPort();
    
    /**
     * Sets comment of ViaHeader
     * @param <var>comment</var> comment
     * @throws IllegalArgumentException if comment is null
     * @throws SipParseException if comment is not accepted by implementation
     */
    public void setComment(String comment)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets branch of ViaHeader
     * (Returns null if branch does not exist)
     * @return branch of ViaHeader
     */
    public String getBranch();
    
    /**
     * Removes comment from ViaHeader (if it exists)
     */
    public void removeComment();
    
    /**
     * Sets TTL of ViaHeader
     * @param <var>ttl</var> TTL
     * @throws SipParseException if ttl is not accepted by implementation
     */
    public void setTTL(int ttl)
                 throws SipParseException;
    
    /**
     * Gets TTL of ViaHeader
     * (Returns negative int if TTL does not exist)
     * @return TTL of ViaHeader
     */
    public int getTTL();
    
    /**
     * TCP constant for ViaHeader
     */
    public final static String TCP = "TCP";
    
    /**
     * UDP constant for ViaHeader
     */
    public final static String UDP = "UDP";
    
    ////////////////////////////////////////////////////////////////
    
    /**
     * Name of ViaHeader
     */
    public final static String name = "Via";
}
