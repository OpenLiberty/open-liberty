/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin;

import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.sib.utils.SIBUuid8;

/**
 * Defines an MQ queue localization. This is an MQ queue that is being used as
 * the queue message point.
 */
public interface MQLocalizationDefinition extends BaseLocalizationDefinition {

    /**
     * Returns the channel name to be used to connect to this MQ queue.
     * 
     * @return the channel name to be used to connect to this MQ queue.
     */
    public String getMQChannelName();

    /**
     * Sets the channel name to be used to connect to this MQ queue.
     * 
     * @param mqChannelName
     */
    public void setMQChannelName(String mqChannelName);

    /**
     * Returns the name of the QueueManager to use to connect to this MQ queue.
     * 
     * @return the name of the QueueManager to use to connect to this MQ queue.
     */
    public String getMQQueueManagerName();

    /**
     * Sets the name of the QueueManager to use to connect to this MQ queue.
     * 
     * @param mqQueueManagerName
     */
    public void setMQQueueManagerName(String mqQueueManagerName);

    /**
     * Returns the virtual queue manager name
     * 
     * @return the virtual queue manager name
     */
    public String getVirtualQueueManagerName();

    /**
     * Sets the name of the virtual queue manager name.
     * 
     * @param virtualQueueManagerName
     */
    public void setVirtualQueueManagerName(String virtualQueueManagerName);

    /**
     * Returns the queue name of this MQ queue.
     * 
     * @return the queue name of this MQ queue.
     */
    public String getMQQueueName();

    /**
     * Set the name of the TransportChain to be used in order to obtain the
     * correct SSLSocketFactory to be used when creating SSL sockets to the MQ
     * Server. May be null if SSL is not in use.
     * 
     * @param transportChainName
     */
    public void setTransportChainName(String transportChainName);

    /**
     * Returns the name of the Transport Chain to be used to obtain the correct
     * SSLSocketFactory. May be null if SSL is not in use.
     * 
     * @return the name of the Transport Chain to be used.
     */
    public String getTransportChainName();

    /**
     * Set the Authentication Alias to be used when connecting to MQ.
     * 
     * @param authAlias
     */
    public void setAuthenticationAlias(String authAlias);

    /**
     * Returns the name of the Transport Chain to be used to obtain the correct
     * SSLSocketFactory. May be null if SSL is not in use.
     * 
     * @return the name of the Transport Chain to be used.
     */
    public String getAuthenticationAlias();

    /**
     * Set whether the user identifiers set within inbound MQ messages shout be
     * trusted for authorization purposes.
     * 
     * @param trust
     *            true if the MQ message identifiers should be trusted.
     */
    public void setTrustMessageUserIdentifiers(boolean trust);

    /**
     * Returns Get whether the user identifiers set within inbound MQ messages
     * shout be trusted for authorization purposes.
     * 
     * @return true if the MQ message identifiers should be trusted.
     */
    public boolean getTrustMessageUserIdentifiers();

    /**
     * Set whether the RFH2 header should be set within outbound MQ messages.
     * 
     * @param enableRFH2Header
     *            true if the RFH2 header should be set.
     */
    public void setEnableRFH2Header(boolean enableRFH2Header);

    /**
     * Returns Get whether the RFH2 header should be set within outbound MQ
     * messages.
     * 
     * @return true if the RFH2 header should be set.
     */
    public boolean getEnableRFH2Header();

    /**
     * Sets the queue name of this MQ queue.
     * 
     * @param mqQueueName
     */
    public void setMQQueueName(String mqQueueName);

    /**
     * Returns the name of the MQServer that is supporting this MQ queue.
     * 
     * @return the name of the MQServer that is supporting this MQ queue.
     */
    public String getMQServerName();

    /**
     * Set the name of the MQServer that is supporting this MQ queue.
     * 
     * @param mqServerName
     */
    public void setMQServerName(String mqServerName);

    /**
     * Returns the port used to connect to the MQServer.
     * 
     * @return the port used to connect to the MQServer.
     */
    public int getMQServerPort();

    /**
     * Sets the port used to connect to the MQServer.
     * 
     * @param mqServerPort
     */
    public void setMQServerPort(int mqServerPort);

    /**
     * Returns the UUID of the MQServer Bus Member.
     * 
     * @return the UUID of the MQServer Bus Member.
     */
    public SIBUuid8 getMQServerBusMemberUuid();

    /**
     * Sets the SIBUuid8 of the MQServer Bus Member.
     * 
     * @param mqServerBusMemberUuid
     */
    public void setMQServerBusMemberUuid(SIBUuid8 mqServerBusMemberUuid);

    /**
     * Set the Reliability to use for inbound persistent MQ Messages.
     * 
     * @param persistentReliability
     *            the Reliability to be used.
     */
    public void setInboundPersistentReliability(Reliability persistentReliability);

    /**
     * Returns the Reliability to used for inbound persistent MQ Messages.
     * 
     * @return the Reliability to be used for inbound persistent MQ Messages.
     */
    public Reliability getInboundPersistentReliability();

    /**
     * Set the Reliability to use for inbound non-persistent MQ Messages.
     * 
     * @param nonPersistentReliability
     *            the Reliability to be used.
     */
    public void setInboundNonPersistentReliability(Reliability nonPersistentReliability);

    /**
     * Returns the Reliability to used for inbound non-persistent MQ Messages.
     * 
     * @return the Reliability to be used for inbound non-persistent MQ Messages.
     */
    public Reliability getInboundNonPersistentReliability();

    /**
     * Returns the name of the host where the MQ queue manager or MQ queue sharing
     * group is running.
     * 
     * @return name of the host where the MQ queue manager or MQ queue sharing
     *         group is running.
     */
    public String getMQHostName();

    /**
     * Sets the host name instance variable to the supplied value. Does not write
     * this value into the configuration.
     * 
     * @param hostName
     *            the new host name.
     */
    public void setMQHostName(String hostName);

    /**
     * Returns the type of transport mode to use.
     * 
     * @return TransportMode The type of transport to use.
     */
    public TransportMode getTransportMode();

    /**
     * Sets the type of transport mode to use.
     * 
     * @param Type of transport mode to use.
     */
    public void setTransportMode(TransportMode mode);

    /**
     * Returns SSL type
     * 
     * @return SSL type
     */
    public SSLType getSSLType();

    /**
     * Sets SSL type
     * 
     * @param SSLType type
     */
    public void setSSLType(SSLType type);

    /**
     * Returns SSL specific endpoint
     * 
     * @return String SSL endpoint
     */
    public String getSSLSpecificEndpoint();

    /**
     * Sets SSL specific endpoint
     * 
     * @param String SSL specific endpoint
     */
    public void setSSLSpecificEndpoint(String endpoint);

    /**
     * Returns SSL CRL
     * 
     * @return String SSL CRL
     */
    public String getSSLCRL();

    /**
     * Sets SSL CRL
     * 
     * @param String SSL CRL
     */
    public void setSSLCRL(String crl);

    /**
     * Returns SSL reset count
     * 
     * @return int SSL reset count
     */
    public int getSSLResetCount();

    /**
     * Sets SSL reset count
     * 
     * @param int SSL reset count
     */
    public void setSSLResetCount(int count);

    /**
     * Returns SSL peer
     * 
     * @return String SSL peer
     */
    public String getSSLPeer();

    /**
     * Sets SSL peer
     * 
     * @param String SSL peer
     */
    public void setSSLPeer(String peer);

    /**
     * Returns send exit
     * 
     * @return String send exit
     */
    public String getSendExit();

    /**
     * Sets send exit
     * 
     * @param String send exit
     */
    public void setSendExit(String sendExit);

    /**
     * Returns send exit init data
     * 
     * @return String send exit init data
     */
    public String getSendExitInitData();

    /**
     * Sets send exit init data
     * 
     * @param String send exit init data
     */
    public void setSendExitInitData(String sendExitInitData);

    /**
     * Returns receive exit
     * 
     * @return String receive exit
     */
    public String getReceiveExit();

    /**
     * Sets receive exit
     * 
     * @param String receive exit
     */
    public void setReceiveExit(String receiveExit);

    /**
     * Returns receive exit init data
     * 
     * @return String receive exit init data
     */
    public String getReceiveExitInitData();

    /**
     * Sets receive exit init data
     * 
     * @param String receive exit init data
     */
    public void setReceiveExitInitData(String receiveExitInitData);

    /**
     * Returns security exit
     * 
     * @return String security exit
     */
    public String getSecurityExit();

    /**
     * Sets security exit
     * 
     * @param String security exit
     */
    public void setSecurityExit(String securityExit);

    /**
     * Returns security exit init data
     * 
     * @return String security exit init data
     */
    public String getSecurityExitInitData();

    /**
     * Sets security exit init data
     * 
     * @param String security exit init data
     */
    public void setSecurityExitInitData(String securityExitInitData);

}
