/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.channelfw;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChannelFrameworkException;

/**
 * The methods of this interface describe what is required to communicate
 * with a particular endpoint server represented by an inbound chain.
 */
public interface CFEndPoint extends Serializable {

    /**
     * Access the name of the inbound chain that the outbound chain described
     * by this definition can communicate with.
     * 
     * @return name of the associated inbound chain
     */
    String getName();

    /**
     * Access the address used to connect to the endpoint inbound chain.
     * 
     * @return address of the endpoint
     */
    InetAddress getAddress();

    /**
     * Access the port used to connect to the endpoint inbound chain.
     * 
     * @return port of the endpoint
     */
    int getPort();

    /**
     * Access the list of virtual hosts associated with this CFEndPoint.
     * If none are associated, an empty (non-null) list is returned.
     * These host names match EndPointInfo definitions.
     * 
     * @return List<String>
     */
    List<String> getVirtualHosts();

    /**
     * Access the ordered list of outbound channel definitions that make
     * up this outbound chain.
     * 
     * @return ordered list of outbound channel definitions
     */
    List<OutboundChannelDefinition> getOutboundChannelDefs();

    /**
     * Access the interface Class used to communicate with this outbound chain.
     * 
     * @return Class used to communicate with this outbound chain.
     */
    Class<?> getChannelAccessor();

    /**
     * Create the outbound channels and chain associated with this CFEndPoint
     * in the Channel Framework. This can be done before calling getOutboundVCFactory(..)
     * so that properties of the channels or chain can be modified before the
     * chain is started. The following call to getOutboundVCFactory() will start the
     * the updated chain.
     * 
     * @return ChainData object representing what was added to the Channel Framework,
     *         including the names of the channels and chains that can be used with
     *         the Channel Framework APIs to make property changes.
     * @throws ChannelFrameworkException
     */
    ChainData createOutboundChain() throws ChannelFrameworkException;

    /**
     * Get the virtual connection factory needed to create connection to
     * this end point.
     * 
     * @return virtual connection, or null if a problem occurs
     */
    VirtualConnectionFactory getOutboundVCFactory();

    /**
     * Check whether there is a SSL Channel in this chain or not.
     * 
     * @return true if enabled, false otherwise
     */
    boolean isSSLEnabled();

    /**
     * This method indicates if the CFEndPoint is for local use only. For example,
     * an inbound chain which has an in process connector channel should be associated
     * with a CFEndPoint that is not distributed to clients. In this case, this
     * method will return true. Internally, a CFEndPoint is determined by be local if
     * the connector channel's factory implements the LocalChannelFactory interface.
     * 
     * @return true for local only, false otherwise
     */
    boolean isLocal();

    /**
     * If there is an SSL Channel, add these properties to the channel.
     * 
     * @param sslProps properties desired in the SSL channel of the VCF
     * @param overwriteExisting indicator that existing properties should be overwritten or not
     * @return virtual connection factory for this endpoint
     * @throws IllegalStateException if the SSL channel isn't represented in this endpoint
     */
    VirtualConnectionFactory getOutboundVCFactory(Map<Object, Object> sslProps, boolean overwriteExisting) throws IllegalStateException;

    /**
     * This method will serialize the end point object into a String, based on
     * a specific XML definition.
     * 
     * @return UTF-8 encoded String
     * @throws NotSerializableException
     */
    String serializeToXML() throws NotSerializableException;

}
