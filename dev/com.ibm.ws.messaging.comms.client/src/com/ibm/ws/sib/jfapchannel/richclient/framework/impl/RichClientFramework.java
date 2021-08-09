/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.jfapchannel.richclient.framework.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.channelfw.ChainData;
import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.serialization.DeserializationObjectInputStream;
import com.ibm.ws.sib.jfapchannel.JFapChannelConstants;
import com.ibm.ws.sib.jfapchannel.framework.Framework;
import com.ibm.ws.sib.jfapchannel.framework.FrameworkException;
import com.ibm.ws.sib.jfapchannel.framework.NetworkTransportFactory;
import com.ibm.ws.sib.jfapchannel.richclient.impl.octracker.JFapOutboundChannelDefinitionImpl;
import com.ibm.ws.sib.utils.RuntimeInfo;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.channelfw.ChannelFramework;

/**
 * This class is the channel framework implementation of the JFap transport framework. All the
 * services that are provided by the framework are retrieved directly from the WAS channel
 * framework. If this class is being used then the code using it must be running inside a WAS client
 * or server container using an IBM JRE. As such, this class fully depends on the use of
 * WsByteBuffers and all aspects of the channel framework.
 * 
 * @author Gareth Matthews
 */
public class RichClientFramework extends Framework
{
    /** Trace */
    private static final TraceComponent tc = SibTr.register(RichClientFramework.class,
                                                            JFapChannelConstants.MSG_GROUP,
                                                            JFapChannelConstants.MSG_BUNDLE);
    /** NLS */
    private static final TraceNLS nls = TraceNLS.getTraceNLS(JFapChannelConstants.MSG_BUNDLE);

    /** Class name for FFDC's */
    private static final String CLASS_NAME = RichClientFramework.class.getName();

    private static ArrayList<CFEndPoint> endPointsList = new ArrayList<CFEndPoint>();//PM72835

    /** Log class info on load */
    static
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc,
                        "@(#) SIB/ws/code/sib.jfapchannel.client.rich.impl/src/com/ibm/ws/sib/jfapchannel/framework/impl/RichClientFramework.java, SIB.comms, WASX.SIB, uu1215.01 1.8");
    }

    /** Our reference to the real WAS channel framework */
    private ChannelFramework framework = null;

    /**
     * Constructor. When this object is constructed the channel framework is initialised.
     */
    public RichClientFramework()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>");
        framework = ChannelFrameworkReference.getInstance();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "<init>");
    }

    /**
     * This method is our entry point into the Channel Framework implementation of the JFap Framework
     * interfaces.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#getNetworkTransportFactory()
     */
    @Override
    public NetworkTransportFactory getNetworkTransportFactory()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getNetworkTransportFactory");
        NetworkTransportFactory factory = new RichClientTransportFactory(framework);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getNetworkTransportFactory", factory);
        return factory;
    }

    /**
     * This method will retrieve the property bag on the named chain so that it can be determined
     * what properties have been set on it.
     * 
     * @param outboundTransportName The chain name.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#getOutboundConnectionProperties(java.lang.String)
     */
    @Override
    public Map getOutboundConnectionProperties(String outboundTransportName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getOutboundConnectionProperties",
                        outboundTransportName);

        ChainData chainData = framework.getChain(outboundTransportName);

        // Obtain properties for outbound channel
        ChannelData[] channelList = chainData.getChannelList();
        Map properties = null;
        if (channelList.length > 0)
        {
            properties = channelList[0].getPropertyBag();
        }
        else
        {
            throw new SIErrorException(
                            nls.getFormattedMessage("OUTCONNTRACKER_INTERNAL_SICJ0064",
                                                    null,
                                                    "OUTCONNTRACKER_INTERNAL_SICJ0064"));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getOutboundConnectionProperties", properties);
        return properties;
    }

    /**
     * This method will retrieve the property bag on the outbound chain that will be used by the
     * specified end point. This method is only valid for CFEndPoints.
     * 
     * @param ep The CFEndPoint
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#getOutboundConnectionProperties(java.lang.Object)
     */
    @Override
    public Map getOutboundConnectionProperties(Object ep)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getOutboundConnectionProperties", ep);

        Map properties = null;
        if (ep instanceof CFEndPoint)
        {
            OutboundChannelDefinition[] channelDefinitions = (OutboundChannelDefinition[]) ((CFEndPoint) ep).getOutboundChannelDefs().toArray();
            if (channelDefinitions.length < 1)
                throw new SIErrorException(nls.getFormattedMessage("OUTCONNTRACKER_INTERNAL_SICJ0064", null, "OUTCONNTRACKER_INTERNAL_SICJ0064"));
            properties = channelDefinitions[0].getOutboundChannelProperties();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getOutboundConnectionProperties", properties);
        return properties;
    }

    /**
     * This method is used to prepare the endpoint and its outbound channel chain for connection.
     * The outbound chain is modified to include any SSL properties that have been specified with the
     * SIB properties file as well as any TCP chains having their thread pool modified. This method
     * is only valid for CFEndPoints.
     * 
     * @param ep The CFEndPoint
     * 
     * @return a modified copy of the input object.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#prepareOutboundConnection(java.lang.Object)
     */
    @Override
    public Object prepareOutboundConnection(Object ep) throws FrameworkException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "prepareOutboundConnection", ep);

        final CFEndPoint originalEndPoint = (CFEndPoint) ep;

        //Attempt to clone the original end point to prevent us affecting other users.
        final CFEndPoint endPoint = cloneEndpoint(originalEndPoint);

        // If the endpoint relates to an SSL transport chain - then we need to add
        // our own SSL properties for things like local keystore and truststore
        // files.
        Map sslProps = null; // D232743 
        if (endPoint.isSSLEnabled())
        {
            // begin D223333
            // Obtain the class of the SSL Channel Factory.  This is used
            // to identify SSL channel instances by the subsequent code.
            Class sslChannelFactory = null;
            try
            {
                sslChannelFactory =
                                Class.forName(JFapChannelConstants.CLASS_SSL_CHANNEL_FACTORY);
            } catch (ClassNotFoundException cnfe)
            {
                FFDCFilter.processException(cnfe, CLASS_NAME + ".prepareEndPoint",
                                            JFapChannelConstants.RICHCLIENTFRAMEWORK_PREPAREEP_01, this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.exception(this, tc, cnfe);
                throw new SIErrorException(
                                nls.getFormattedMessage("CONNDATAGROUP_INTERNAL_SICJ0062",
                                                        null,
                                                        "CONNDATAGROUP_INTERNAL_SICJ0062"),
                                cnfe);
            }

            if (RuntimeInfo.isClientContainer() || RuntimeInfo.isThinClient() || RuntimeInfo.isFatClient())
            {
                // In the client environment, we take our SSL configuration
                // from the sib.client.ssl.properties file.                  
                sslProps = ChannelFrameworkReference.loadSSLProperties();
            }
            else
            {
                // In the server environment, we obtain the SSL configuration
                // from a chain with the same name.  E.g. if the endpoint relates
                // to a remote chain called "SecureMessaging", we look for a locally
                // defined chain called "SecureMessaging" to obtain our configuration.
                // Of course, there is no guarentee that this chain will be defined.
                String chainName = endPoint.getName();
                ChainData chainData = framework.getChain(chainName);

                if (chainData == null)
                {
                    // This application server does not have a suitably named
                    // chain.  Warn the user.
                    // Inserts are chain name and hostname
                    SibTr.warning(tc, "CONNDATAGROUP_BADSLLCHAINNAME_SICJ0066",
                                  new Object[] { endPoint.getName(), endPoint.getAddress().getHostAddress() });
                }
                else
                {
                    ChannelData sslChannel = null;
                    ChannelData[] channelArray = chainData.getChannelList();
                    for (int i = 0; (i < channelArray.length) && (sslChannel == null); ++i)
                    {
                        if (channelArray[i].getFactoryType().equals(sslChannelFactory))
                            sslChannel = channelArray[i];
                    }

                    if (sslChannel == null)
                    {
                        // Although we have located a suitably named chain, it does
                        // not contain an SSL channel instance from which we can obtain
                        // the properties we require.  Warn the user.
                        SibTr.warning(tc, "CONNDATAGROUP_NOTSSLCHAIN_SICJ0067",
                                      new Object[] { endPoint.getName(), endPoint.getAddress().getHostAddress() });
                    }
                    else
                    {
                        sslProps = sslChannel.getPropertyBag();
                    }
                }

            }

            // If the endpoint contains an SSL channel - then modifiy its properties by adding a
            // map containing local SSL configuration information.
            if (sslProps != null)
            {
                modifyEndpointChannelProperties(endPoint, sslChannelFactory, sslProps, true);
            }
        }

        // If the endpoint contains an TCP channel - then modifiy its properties by overriding
        // the threadpool it will use.
        Class<?> tcpChannelFactory = null;
        try
        {
            //Liberty COMMS todo
            // most of this class code may not be needed
            //final boolean targetThreadpoolExists = 
            //   ThreadPoolRepositoryManager.getThreadPoolRepository().getThreadPool(JFapChannelConstants.CLIENT_TCP_CHANNEL_THREADPOOL_NAME) != null;
            boolean targetThreadpoolExists = false;
            if (targetThreadpoolExists)
            {
                tcpChannelFactory = Class.forName(JFapChannelConstants.CLASS_TCP_CHANNEL_FACTORY);
                Map<String, String> tcpProps = new HashMap<String, String>();
                tcpProps.put("threadPoolName", JFapChannelConstants.CLIENT_TCP_CHANNEL_THREADPOOL_NAME);
                modifyEndpointChannelProperties(endPoint, tcpChannelFactory, tcpProps, false);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Cannot find threadpool: " + JFapChannelConstants.CLIENT_TCP_CHANNEL_THREADPOOL_NAME + " using default");
            }
        } catch (ClassNotFoundException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".prepareEndPoint",
                                        JFapChannelConstants.RICHCLIENTFRAMEWORK_PREPAREEP_02, this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Unable to load TCP Channel", e);

            throw new FrameworkException(e);
        }

        /* PM72835 - Start */
        final Iterator<CFEndPoint> itr = endPointsList.iterator();
        while (itr.hasNext()) {
            CFEndPoint tempEndPoint = itr.next();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Comparing the Endpoints from list , sent from trm ", new Object[] { tempEndPoint, endPoint });
            if (areEndPointsEqual(tempEndPoint, endPoint))
                return tempEndPoint;
        }
        /* PM72835 - End */

        //Romil Liberty change: commenting out this .. for now .. as the flow is outbound
        //initially only in-bound is worked
        /*
         * try
         * {
         * 
         * framework.prepareEndPoint(endPoint);
         * }
         * catch (ChannelFrameworkException e)
         * {
         * FFDCFilter.processException(e, CLASS_NAME + ".prepareEndPoint",
         * JFapChannelConstants.RICHCLIENTFRAMEWORK_PREPAREEP_03,
         * new Object[] { this, endPoint });
         * 
         * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Unable to prepare end point", e);
         * 
         * throw new FrameworkException(e);
         * }
         */
        endPointsList.add(endPoint); //PM72835
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "prepareOutboundConnection", endPoint);
        return endPoint;
    }

    /**
     * Modifies the properties associated with a specific channel in a channel framework endpoint.
     * This is a temporary work around until the Channel Framework implements similar functionality
     * itself. Most of the code for this method was taken from
     * com.ibm.ws.channel.framework.impl.CFEndPointImpl#getOutboundVCFactory(Map, boolean)
     * 
     * @param endPoint
     *            The endpoint to modify
     * @param channelFactoryClass
     *            The channel in the endpoint to modify
     * @param properties
     *            The new properties to associate with the channel.
     * @param overwriteExisting
     *            Should we overwrite existing properties?
     */
    private void modifyEndpointChannelProperties(CFEndPoint endPoint, Class<?> channelFactoryClass,
                                                 Map properties, boolean overwriteExisting)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "modifyEndpointChannelProperties",
                        new Object[]
                        {
                         endPoint,
                         channelFactoryClass,
                         properties,
                         "" + overwriteExisting
                        });

        // Romil libertry changes convert list to array
        OutboundChannelDefinition[] outboundChannelDefs = (OutboundChannelDefinition[]) endPoint.getOutboundChannelDefs().toArray();

        Class factoryClass = null;
        OutboundChannelDefinition existingChannelDef = null;
        int index = 0;
        boolean foundChannel = false;
        // Loop through each channel in the outbound chain represented by this endpoint
        for (index = 0; index < outboundChannelDefs.length; index++)
        {
            // Attempt to identify the channel factory we are interested in.
            existingChannelDef = outboundChannelDefs[index];
            factoryClass = outboundChannelDefs[index].getOutboundFactory();
            if (channelFactoryClass.isAssignableFrom(factoryClass))
            {
                foundChannel = true;
                break;
            }
        }

        if (foundChannel)
        {
            // Create a new OutboundChannelDefintion with new properties.
            OutboundChannelDefinition newChannelDef =
                            new JFapOutboundChannelDefinitionImpl(existingChannelDef,
                                            properties,
                                            overwriteExisting);

            // Overlay the new definition into the array managed by this endpoint. 
            outboundChannelDefs[index] = newChannelDef;
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Did not find desired channel");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "modifyEndpointChannelProperties");
    }

    /**
     * Retrieves the host address from the specified CFEndPoint.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#getHostAddress(java.lang.Object)
     */
    @Override
    public InetAddress getHostAddress(Object endPoint)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getHostAddress", endPoint);

        InetAddress address = null;
        if (endPoint instanceof CFEndPoint)
        {
            address = ((CFEndPoint) endPoint).getAddress();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getHostAddress", address);
        return address;
    }

    /**
     * Retrieves the host port from the specified CFEndPoint.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#getHostPort(java.lang.Object)
     */
    @Override
    public int getHostPort(Object endPoint)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getHostPort", endPoint);

        int port = 0;
        if (endPoint instanceof CFEndPoint)
        {
            port = ((CFEndPoint) endPoint).getPort();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getHostPort", Integer.valueOf(port));
        return port;
    }

    /**
     * Outputs a log file warning if the specified chain may not have been correctly defined because
     * of a missing properties file.
     * 
     * @param endPoint the end point to test and see if outputting a warning is appropriate.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#warnIfSSLAndPropertiesFileMissing(java.lang.Object)
     */
    @Override
    public void warnIfSSLAndPropertiesFileMissing(Object endPoint)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "warnIfSSLAndPropertiesFileMissing", endPoint);

        // Only output a warning if running in client
        if (!RuntimeInfo.isServer() && !RuntimeInfo.isClusteredServer())
        {
            // Only display a warning if the endpoing is SSL enabled
            if (((CFEndPoint) endPoint).isSSLEnabled())
            {
                // Test to see if the properties file exists.
                final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                Boolean propertiesFileExists = (Boolean)
                                AccessController.doPrivileged(
                                                new PrivilegedAction<Object>()
                                                {
                                                    @Override
                                                    public Object run()
                                                    {
                                                        URL url = classLoader.getResource(JFapChannelConstants.CLIENT_SSL_PROPERTIES_FILE);
                                                        return Boolean.valueOf(url != null);
                                                    }
                                                }
                                                );

                if (!propertiesFileExists.booleanValue())
                {
                    SibTr.warning(tc,
                                  SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                                  "NO_SSL_PROPERTIES_FILE_SICJ0012",
                                  new Object[] { JFapChannelConstants.CLIENT_SSL_PROPERTIES_FILE });
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "warnIfSSLAndPropertiesFileMissing");
    }

    /**
     * Outputs a log file warning if the specified chain may not have been correctly defined because
     * of a missing properties file.
     * 
     * @param chainName the chain name to test and see if outputting a warning is appropriate.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#warnIfSSLAndPropertiesFileMissing(java.lang.String)
     */
    @Override
    public void warnIfSSLAndPropertiesFileMissing(String chainName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "warnIfSSLAndPropertiesFileMissing", chainName);

        // Only output a warning if running in a client
        if (!RuntimeInfo.isServer() && !RuntimeInfo.isClusteredServer())
        {
            // Determine if the chain is SSL enabled
            boolean chainIsSSLEnabled = false;
            ChainData chainData = framework.getChain(chainName);
            if (chainData != null)
            {
                if (chainData.getType() == FlowType.OUTBOUND)
                {
                    ChannelData[] channelDataArray = chainData.getChannelList();
                    if (channelDataArray != null)
                    {
                        for (int i = 0; i < channelDataArray.length; ++i)
                        {
                            Class channelFactoryType = channelDataArray[i].getFactoryType();
                            chainIsSSLEnabled |=
                                            channelFactoryType.getName().equals(JFapChannelConstants.CLASS_SSL_CHANNEL_FACTORY);
                        }
                    }
                }
            }

            // If the chain is SSL enabled - and if no properties file was found when defining
            // the outbound SSL chains - output a suitable warning.
            if (chainIsSSLEnabled && ChannelFrameworkReference.isOutboundSSLChainDefinedWithoutProperties())
            {
                SibTr.warning(tc,
                              SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                              "NO_SSL_PROPERTIES_FILE_SICJ0012",
                              new Object[] { JFapChannelConstants.CLIENT_SSL_PROPERTIES_FILE });
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "warnIfSSLAndPropertiesFileMissing");
    }

    /**
     * The channel framework EP's don't have their own equals method - so one is implemented here by
     * comparing the various parts of the EP.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#areEndPointsEqual(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean areEndPointsEqual(Object ep1, Object ep2)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "areEndPointsEqual", new Object[] { ep1, ep2 });

        boolean isEqual = false;

        if (ep1 instanceof CFEndPoint && ep2 instanceof CFEndPoint)
        {
            CFEndPoint cfEp1 = (CFEndPoint) ep1;
            CFEndPoint cfEp2 = (CFEndPoint) ep2;

            // The CFW does not provide an equals method for its endpoints.
            // We need to manually equals the important bits up
            isEqual = isEqual(cfEp1.getAddress(), cfEp2.getAddress()) &&
                      isEqual(cfEp1.getName(), cfEp2.getName()) &&
                      cfEp1.getPort() == cfEp2.getPort() &&
                      cfEp1.isLocal() == cfEp2.isLocal() &&
                      cfEp1.isSSLEnabled() == cfEp2.isSSLEnabled();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "areEndPointsEqual", Boolean.valueOf(isEqual));
        return isEqual;
    }

    /**
     * The channel framework EP's don't have their own hashCode method - so one is implemented here
     * using the various parts of the EP.
     * 
     * @see com.ibm.ws.sib.jfapchannel.framework.Framework#getEndPointHashCode(java.lang.Object)
     */
    @Override
    public int getEndPointHashCode(Object ep)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getEndPointHashCode", ep);

        int hashCode = 0;
        if (ep instanceof CFEndPoint)
        {
            CFEndPoint cfEndPoint = (CFEndPoint) ep;
            if (cfEndPoint.getAddress() != null)
                hashCode = hashCode ^ cfEndPoint.getAddress().hashCode();
            if (cfEndPoint.getName() != null)
                hashCode = hashCode ^ cfEndPoint.getName().hashCode();
        }

        if (hashCode == 0)
            hashCode = hashCode ^ ep.hashCode();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getEndPointHashCode", Integer.valueOf(hashCode));
        return hashCode;
    }

    /**
     * Compares two objects. Returns false if one is null but the other isn't, returns true if both
     * are null, otherwise returns the result of their equals() method.
     * 
     * @param o1
     * @param o2
     * 
     * @return Returns true if o1 and o2 are equal.
     */
    private boolean isEqual(Object o1, Object o2)
    {
        // Both null - they are equal
        if (o1 == null && o2 == null)
        {
            return true;
        }
        // One is null and the other isn't - they are not equal
        if ((o1 == null && o2 != null) || (o1 != null && o2 == null))
        {
            return false;
        }

        // Otherwise fight it out amongst themselves
        return o1.equals(o2);
    }

    /**
     * Creates a copy of originalEndPoint.
     * This is to prevent us causing problems when we change it. As there is no exposed way to do this we will use serialization.
     * It may be a bit slow, but it is implementation safe as the implementation of CFEndPoint is designed to be serialized by WLM.
     * Plus we only need to do this when creating the original connection so it is not main line code.
     * 
     * @param originalEndPoint the CFEndPoint to be cloned
     * 
     * @return a cloned CFEndPoint or the original one if the clone failed.
     */
    private CFEndPoint cloneEndpoint(final CFEndPoint originalEndPoint)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cloneEndpoint", originalEndPoint);

        CFEndPoint endPoint;
        ByteArrayOutputStream baos = null;
        ObjectOutputStream out = null;
        ObjectInputStream in = null;

        try
        {
            baos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(baos);
            out.writeObject(originalEndPoint);
            out.flush();

            ClassLoader cl = AccessController.doPrivileged(new PrivilegedAction<ClassLoader>()
            {
                @Override
                public ClassLoader run() {
                    return Thread.currentThread().getContextClassLoader();
                }
            });
            in = new DeserializationObjectInputStream(new ByteArrayInputStream(baos.toByteArray()), cl);
            endPoint = (CFEndPoint) in.readObject();
        } catch (IOException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".cloneEndpoint", JFapChannelConstants.RICHCLIENTFRAMEWORK_CLONE_01, this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Caught IOException copying endpoint", e);

            //Use input parameter.
            endPoint = originalEndPoint;
        } catch (ClassNotFoundException e)
        {
            FFDCFilter.processException(e, CLASS_NAME + ".cloneEndpoint", JFapChannelConstants.RICHCLIENTFRAMEWORK_CLONE_02, this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Caught ClassNotFoundException copying endpoint", e);

            //Use input parameter.
            endPoint = originalEndPoint;
        } finally
        {
            //Tidy up resources.
            try
            {
                if (out != null)
                {
                    out.close();
                }
                if (in != null)
                {
                    in.close();
                }
            } catch (IOException e)
            {
                //No FFDC code needed.
                //Absorb any exceptions as we no longer care.
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cloneEndpoint", endPoint);
        return endPoint;
    }

}