/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.LocalChannelFactory;
import com.ibm.wsspi.channelfw.SSLChannelFactory;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.channelfw.exception.ChannelFrameworkException;

/**
 * This object represents information about a how to communicate with
 * an inbound chain.
 */
public class CFEndPointImpl implements CFEndPoint, Serializable {

    /** Trace tool. */
    private static final TraceComponent tc = Tr.register(CFEndPointImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /** Serialization ID string */
    private static final long serialVersionUID = -4050856186139408312L;

    /** Name of the inbound chain / endpoint associated with the end point. */
    private String name = null;
    /** Array of vhosts the end point is listening on. */
    private List<String> vhostArray = null;
    /** Port number the end point is listening on. */
    private int port = 0;
    /** Address the end point is listening on. */
    private InetAddress address = null;
    /**
     * Array of outbound channel definitions used to build chain to talk to this
     * end point.
     */
    private LinkedList<OutboundChannelDefinition> outboundChannelDefs = null;
    /**
     * Class of the interface used to communicate with the first outbound channel.
     */
    private Class<?> channelAccessor = null;
    /**
     * If any channel factory is a SSLChannelFactory, this is the index into the
     * list
     */
    private int sslChannelIndex = -1;
    /** Index of the local channel factory if found */
    private int localChannelIndex = -1;
    /** VCF used to communicate with this end point. */
    private transient VirtualConnectionFactory vcf = null;
    /**
     * Reference to channel framework for use in creating VCF when it is asked
     * for.
     */
    private transient final ChannelFrameworkImpl framework = ChannelFrameworkImpl.getRef();
    /** Outbound chain config. */
    private transient ChainData outboundChainData = null;

    /**
     * Constructor
     * 
     * @param chainData
     * @throws ChannelFrameworkException
     */
    public CFEndPointImpl(ChainDataImpl chainData) throws ChannelFrameworkException {
        try {
            this.name = chainData.getName();
            this.outboundChannelDefs = new LinkedList<OutboundChannelDefinition>();

            // Assign the host and port from the chain properties (which originated in
            // the TCP channel)
            Map<Object, Object> chainProperties = chainData.getPropertyBag();
            String addressString = null;
            String portString = null;
            if (chainProperties != null) {
                addressString = (String) chainProperties.get(ChannelFrameworkConstants.HOST_NAME);
                portString = (String) chainProperties.get(ChannelFrameworkConstants.PORT);
                String listenPortString = (String) chainProperties.get(ChannelFrameworkConstants.LISTENING_PORT);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Resolving tcp properties: host=" + addressString + ", port=" + portString + ", listenPort=" + listenPortString);
                }
                assignAddress(addressString);
                assignPort(portString, listenPortString);
            } else {
                // Did not find required propreties.
                throw new IllegalArgumentException("No properties found in device side channel.  No CFEndPoint created.");
            }

            ChannelData[] channelList = chainData.getChannelList();
            ChannelFactory inboundFactory = null;
            Map<Object, Object> copyMap = null;
            Map<Object, Object> channelMap = null;
            for (int i = 0; i < channelList.length; i++) {
                // Extract the ChannelFactory for each channel in the list.
                inboundFactory = this.framework.getChannelFactoryInternal(channelList[i].getFactoryType(), false);

                // Make a copy / pseudo clone of the property bag so the channel factory
                // can't modify it.
                copyMap = new HashMap<Object, Object>();
                channelMap = channelList[i].getPropertyBag();
                if (channelMap != null) {
                    for (Entry<Object, Object> entry : channelMap.entrySet()) {
                        copyMap.put(entry.getKey(), entry.getValue());
                    }
                }

                // This is a temporary hack that should be replaced in a future release.
                // The http tunnel channel
                // needs access to the host and port of this endpoint in order to create
                // the MD5 URI to pass to the
                // web server plugins. In the future ... (1) this for loop should handle
                // the connector side channel
                // first and save the host and port and (2)
                // getOutboundChannelDefinition(..) should take a reference
                // to this CFEndPoint enabling the ChannelFactory to call getAddress()
                // and getPort(). For now,
                // since it is late in the cycle and interfaces can't change, this hack
                // will get it done.
                copyMap.put(ChannelFrameworkConstants.CHAIN_DATA_KEY, chainData);

                OutboundChannelDefinition def = inboundFactory.getOutboundChannelDefinition(copyMap);
                if (null != def) {
                    this.outboundChannelDefs.addFirst(def);
                }
            }

            // Assign the channelAccessor class based on the application side channel
            // definition.
            determineChannelAccessor();
            // Assign the isSSLEnabled flag based on the outbound channel definitions.
            determineIsSSLEnabled();
            // Assign the isLocal flag based on the outbound channel definitions.
            determineIsLocal();
            // Identify the vhost.
            this.vhostArray = this.framework.getVhost(addressString, portString);
            if (null == this.vhostArray) {
                // api says this should be non-null
                this.vhostArray = new ArrayList<String>(0);
            }
        } catch (Throwable t) {
            // no FFDC required
            throw new ChannelFrameworkException(t);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "constructor: " + this);
        }
    }

    /**
     * Identify the channel accessor class based on the application side outbound
     * channel definition array.
     * 
     * @throws ChannelFrameworkException
     * @throws ChannelFactoryException
     */
    private void determineChannelAccessor() throws ChannelFrameworkException, ChannelFactoryException {
        // Extract the factory class from the application side channel definition.
        OutboundChannelDefinition first = this.outboundChannelDefs.getFirst();
        Class<?> factoryClass = first.getOutboundFactory();
        if (factoryClass == null) {
            throw new ChannelFrameworkException("No factory class associated with outbound channel " + first);
        }
        // Get an instance of the channel factory from the framework.
        ChannelFactory outboundFactory = framework.getChannelFactoryInternal(factoryClass, false);
        if (outboundFactory == null) {
            throw new ChannelFrameworkException("No channel factory could be found for factory class " + factoryClass);
        }
        // Get the channel accessor from the factory instance.
        this.channelAccessor = outboundFactory.getApplicationInterface();
    }

    /**
     * Identify if one of the outbound channel definitions has a factory that
     * implements
     * the SSLChannelFactory.
     */
    private void determineIsSSLEnabled() {
        // Loop through each channel in the outbound chain represented by this
        // endpoint
        int i = 0;
        for (OutboundChannelDefinition def : this.outboundChannelDefs) {
            // Look for the SSL channel factory.
            if (SSLChannelFactory.class.isAssignableFrom(def.getOutboundFactory())) {
                // Found SSL channel factory.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found SSLChannelFactory interface: " + def.getOutboundFactory());
                }
                this.sslChannelIndex = i;
                break;
            }
            i++;
        }
    }

    /**
     * Identify if one of the outbound channel definitions has a factory that
     * implements
     * the LocalChannelFactory.
     */
    private void determineIsLocal() {
        // Loop through each channel in the outbound chain represented by this
        // endpoint
        int i = 0;
        for (OutboundChannelDefinition def : this.outboundChannelDefs) {
            if (LocalChannelFactory.class.isAssignableFrom(def.getOutboundFactory())) {
                // Found Local channel factory.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found LocalChannelFactory interface: " + def.getOutboundFactory());
                }
                this.localChannelIndex = i;
                break;
            }
            i++;
        }
    }

    /**
     * Assign the value of address based on the String parameter.
     * 
     * @param addressString
     * @throws ChannelFrameworkException
     * @throws UnknownHostException
     */
    private void assignAddress(String addressString) throws ChannelFrameworkException, UnknownHostException {
        if (addressString == null) {
            // No address found in properties. No CFEndPoint can be created.
            throw new ChannelFrameworkException("No address available in properties.");
        }
        if ("*".equals(addressString)) {
            // TODO WAS used the node name
            this.address = InetAddress.getLocalHost();
        } else {
            this.address = InetAddress.getByName(addressString);
        }
    }

    /**
     * Assign the value of port based on the parameters. If the first parameter is
     * null, the second/backup
     * will be used.
     * 
     * @param portString
     * @param listenPortString
     * @throws ChannelFrameworkException
     * @throws NumberFormatException
     *             - format of port string is not valid
     */
    private void assignPort(String portString, String listenPortString) throws ChannelFrameworkException, NumberFormatException {
        if (portString != null) {
            // Found a port string. Convert it to an int.
            port = Integer.parseInt(portString);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Port set from regular tcp port: " + port);
            }
        } else {
            // Port String not found so try the backup listening port.
            if (listenPortString != null) {
                // Found a listening port string. Convert it to an int.
                port = Integer.parseInt(listenPortString);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Port set from tcp listening port: " + port);
                }
            } else {
                // Listening port not found either.
                throw new ChannelFrameworkException("Port not available in TCP channel properties.");
            }
        }
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#getName()
     */
    @Override
    public String getName() {
        return this.name;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#getAddress()
     */
    @Override
    public InetAddress getAddress() {
        return this.address;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#getPort()
     */
    @Override
    public int getPort() {
        return this.port;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#getVirtualHosts()
     */
    @Override
    public List<String> getVirtualHosts() {
        if (null == this.vhostArray) {
            // nothing here but the api says it returns a non-null empty list
            this.vhostArray = new ArrayList<String>(0);
        }
        return this.vhostArray;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#getOutboundChannelDefs()
     */
    @Override
    public List<OutboundChannelDefinition> getOutboundChannelDefs() {
        return this.outboundChannelDefs;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#getChannelAccessor()
     */
    @Override
    public Class<?> getChannelAccessor() {
        return this.channelAccessor;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#isSSLEnabled()
     */
    @Override
    public boolean isSSLEnabled() {
        return -1 != this.sslChannelIndex;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#isLocal()
     */
    @Override
    public boolean isLocal() {
        return -1 != this.localChannelIndex;
    }

    /**
     * Set a reference to the outbound chain data. This is called by the framework
     * once the chain is built.
     * 
     * @param inputChainData
     */
    protected void setOutboundChainData(ChainData inputChainData) {
        this.outboundChainData = inputChainData;
    }

    /**
     * Fetch the reference to the outbound chain data.
     * 
     * @return ChainData
     */
    protected ChainData getOutboundChainData() {
        return this.outboundChainData;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#createOutboundChain()
     */
    @Override
    public synchronized ChainData createOutboundChain() throws ChannelFrameworkException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "createOutboundChain");
        }

        // Check to see if the chain has already been created.
        if (outboundChainData == null) {
            this.outboundChainData = framework.createOutboundChain(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "createOutboundChain");
        }
        return this.outboundChainData;
    }

    /*
     * @see
     * com.ibm.websphere.channelfw.CFEndPoint#getOutboundVCFactory(java.util.Map,
     * boolean)
     */
    @Override
    public VirtualConnectionFactory getOutboundVCFactory(Map<Object, Object> sslProps, boolean overwriteExisting) throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getOutboundVCFactory(ssl)");
        }

        // Ensure the SSL channel factory exists in the outbound channel
        // definitions.
        if (-1 == this.sslChannelIndex) {
            throw new IllegalStateException("The SSL channel factory does not exist in this CFEndPoint.");
        }

        // Create a new OutboundChannelDefintion with new properties.
        OutboundChannelDefinition existingDef = this.outboundChannelDefs.get(this.sslChannelIndex);
        OutboundChannelDefinition newChannelDef = new OutboundChannelDefinitionImpl(existingDef, sslProps, overwriteExisting);
        // Overlay the new definition into the array managed by this endpoint.
        this.outboundChannelDefs.add(this.sslChannelIndex, newChannelDef);
        // Now that the properties are folded in, call the basic implementation.
        VirtualConnectionFactory localVCF = getOutboundVCFactory();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getOutboundVCFactory(ssl)");
        }
        return localVCF;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#getOutboundVCFactory()
     */
    @Override
    public VirtualConnectionFactory getOutboundVCFactory() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "getOutboundVCFactory");
        }

        // Handle case where existing vcf has been destroyed.
        if (vcf != null) {
            OutboundVirtualConnectionFactoryImpl outboundVCF = (OutboundVirtualConnectionFactoryImpl) vcf;
            if (outboundVCF.getRefCount() == 0) {
                // VCF has been destroyed.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found destroyed vcf, nulling it out to build another");
                }
                vcf = null;
            }
        }

        // Check to see if a vcf already exists.
        if (vcf == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Existing vcf not available.  Build one.");
            }
            // Determine if the outbound chain already exists.
            if (outboundChainData != null) {
                // The outbound chain has already been created. Use its name.
                try {
                    vcf = framework.getOutboundVCFactory(outboundChainData.getName());
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught exception while getting VCFactory: " + e);
                    }
                    vcf = null;
                }
            } else {
                // The outbound chain data does NOT exist.
                try {
                    // This method will end up calling setOutboundVCFactory to assign
                    // a valid value to vcf.
                    // This method will also call setOutboundChainData.
                    this.framework.prepareEndPoint(this);
                } catch (ChannelFrameworkException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Caught exception while preparing CFEndPoint: " + e);
                    }
                    this.vcf = null;
                }
            }
        } else {
            OutboundVirtualConnectionFactoryImpl outboundVCF = (OutboundVirtualConnectionFactoryImpl) vcf;
            outboundVCF.incrementRefCount();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Found an existing VCF, updating the ref count to " + outboundVCF.getRefCount());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "getOutboundVCFactory");
        }
        return vcf;
    }

    /**
     * This method is called from the framework once it determines a VCF that will
     * work to communicate to this endpoint.
     * 
     * @param inputVCF
     */
    public void setOutboundVCFactory(VirtualConnectionFactory inputVCF) {
        this.vcf = inputVCF;
    }

    /*
     * @see com.ibm.websphere.channelfw.CFEndPoint#serializeToXML()
     */
    @Override
    public String serializeToXML() throws NotSerializableException {
        return CFEndPointSerializer.serialize(this);
    }

    /*
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("CFEndPoint@").append(hashCode());
        sb.append(' ').append(this.name);
        if (vhostArray == null || vhostArray.isEmpty()) {
            sb.append(",[null]");
        } else {
            sb.append(",[");
            for (String vhost : this.vhostArray) {
                sb.append(vhost);
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
        }
        sb.append(',').append(this.port);
        sb.append(',').append(this.address);
        sb.append(',').append(this.channelAccessor.getName());
        if (-1 != this.sslChannelIndex) {
            sb.append(",ssl=").append(this.sslChannelIndex);
        }
        if (-1 != this.localChannelIndex) {
            sb.append(",local=").append(this.localChannelIndex);
        }
        if (this.outboundChannelDefs != null && !this.outboundChannelDefs.isEmpty()) {
            sb.append(",[");
            for (OutboundChannelDefinition def : this.outboundChannelDefs) {
                sb.append(OutboundChannelDefinitionImpl.toString(def));
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
        }
        return sb.toString();
    }

}
