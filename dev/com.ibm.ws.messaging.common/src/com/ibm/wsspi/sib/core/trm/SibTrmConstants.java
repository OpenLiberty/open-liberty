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

package com.ibm.wsspi.sib.core.trm;

/**
 * Constants used to create the map of properties passed on <code>SICoreConnectionFactory.createConnection</code>
 * when the <code>SICoreConnectionFactory</code> is obtained with a <code>FactoryType</code>
 * of <code>TRM_CONNECTION</code>.
 */

public interface SibTrmConstants {

    /**
     * The key value for the required <code>busName</code> property. If a property with this
     * key does not exist then an exception will be thrown. The <code>String</code> value of
     * this property must be set to the name of the required bus.
     */

    public static final String BUSNAME = "busName";

    /**
     * The key value for the optional <code>targetTransportChain</code> property. The <code>String</code>
     * value of this property should be set to the name of transport chain over which the client
     * wants to connect to the bus if remote connection over a network is required.
     * <p>
     * The following transport chains are preconfigured:
     * <ul>
     * <li><code>InboundBasicMessaging</code> no security
     * <li><code>InboundSecureMessaging</code> uses SSL
     * </ul>
     */

    public static final String TARGET_TRANSPORT_CHAIN = "targetTransportChain";

      // Preconfigured values for TARGET_TRANSPORT_CHAIN

      /** Value for <code>InboundBasicMessaging</code> transport chain */
      public static final String TARGET_TRANSPORT_CHAIN_BASIC = "InboundBasicMessaging";

      /** Value for <code>InboundSecureMessaging</code> transport chain */
      public static final String TARGET_TRANSPORT_CHAIN_SECURE = "InboundSecureMessaging";

      /** Default value for target transport chain */
      // removed under d313487: the default value depends on security usage

    /**
     * Bootstrap transport chain values, the following bootstrap transport chains are preconfigured:
     * <p>
     * <ul>
     * <li><code>BootstrapBasicMessaging</code> no security
     * <li><code>BootstrapSecureMessaging</code> uses SSL
     * <li><code>BootstrapTunneledMessaging</code> uses HTTP
     * <li><code>BootstrapTunneledSecureMessaging</code> uses HTTP/SSL
     * </ul>
     */

     /** Value for <code>BootstrapBasicMesaging</code> bootstrap protocol */
     public static final String BOOTSTRAP_TRANSPORT_CHAIN_BASIC = "BootstrapBasicMessaging";

     /** Value for <code>BootstrapSecureMessaging</code> bootstrap protocol */
     public static final String BOOTSTRAP_TRANSPORT_CHAIN_SECURE = "BootstrapSecureMessaging";

     /** Value for <code>BootstrapTunneledMessaging</code> bootstrap protocol */
     public static final String BOOTSTRAP_TRANSPORT_CHAIN_TUNNELED = "BootstrapTunneledMessaging";

     /** Value for <code>BootstrapTunneledSecureMessaging</code> bootstrap protocol */
     public static final String BOOTSTRAP_TRANSPORT_CHAIN_TUNNELED_SECURE = "BootstrapTunneledSecureMessaging";

     /** Default value for bootstrap protocol */
     // removed under d313487: the default value depends on security usage

    /**
     * The key value for the optional <code>providerEndpoints</code> property. The <code>String</code>
     * value of this property is a comma separated list of host:port:bootstrap transport chain triplets. Each
     * triplet represents the TCP/IP hostname, TCP/IP port number and a bootstrap transport chain of a server
     * which can act as a 'bootstrap' server for the client. The bootstrap server will find
     * a suitable messaging engine for the client to
     * connect to in the bus and if necessary redirect the client to the selected messaging
     * engine.
     * This property is ignored if the client is directly connected to the bus when the client
     * is in the same process as a selected messaging engine.
     * <p>
     * Triplets need not be complete, if the bootstrap transport chain is not specified for a triplet then
     * then the default value BootstrapBasicMessaging will be used. If the port is not
     * specified for a triplet then a default port will be used.
     * <p>
     * For example:
     * <p>
     * <code>host1:6789:BootstrapBasicMessaging,host2:7777,host3</code>
     * <p>
     * If no "providerEndPoints" property is set then a default triplet will be used.
     */

    
    public static final String PROVIDER_ENDPOINTS = "remoteServerAddress";

      // Default provider endpoint values

      /** Default bootstrap server host */
      public static final String PROVIDER_ENDPOINTS_LOCALHOST = "localhost";

      /** Default bootstrap server ports */
      public static final String PROVIDER_ENDPOINTS_PORT_BASIC      = "7276";
      public static final String PROVIDER_ENDPOINTS_PORT_SECURE     = "7286"; // d313487

      /** Default bootstrap server transport chain */
      // removed under d313487: the default value depends on security usage

    /**
     * The key for the optional <code>targetGroup</code> property. The <code>String</code>
     * value of this property is the name of a target group to which the selected messaging
     * engine should belong.
     */
      
      //Liberty COMMS change
      public static final String TARGET_TRANSPORT_TYPE = "targetTransport";
      
      //Acceptable values for TARGET_TRANSPORT_TYPE
      public static final String TARGET_TRANSPORT_BINDING = "BINDING";
      public static final String TARGET_TRANSPORT_CLIENT = "CLIENT";
      public static final String TARGET_TRANSPORT_BINDING_CLIENT = "BINDING_THEN_CLIENT";
      public static final String TARGET_TRANSPORT_CLIENT_BINDING = "CLIENT_THEN_BINDING";
      public static final String TARGET_TRANSPORT_DEFAULT = TARGET_TRANSPORT_BINDING_CLIENT;
      

    public static final String TARGET_GROUP = "targetGroup";

    /**
     * The key for the optional <code>targetType</code> property. The <code>String</code>
     * value of this property is the type of the <code>targetGroup</code> property value
     * name.
     * <p>
     * Valid values for this property are:
     * <ul>
     * <li><code>BusMember</code> means <code>targetGroup</code> represents a bus member name.
     * A bus member is the name of a unclustered application server in the format node.server or
     * the name of a cluster.
     * <li><code>Custom</code> means <code>targetGroup</code> represents the name of a user
     * specified custom group. Messaging engines may be configured to join a user specified custom
     * group when they start.
     * <li>ME<code>ME</code> means <code>targetGroup</code> represents the name of a messaging engine.
     * </ul>
     * The default value for this property is <code>BusMember</code>.
     */

    public static final String TARGET_TYPE = "targetType";

      // Acceptable values for TARGET_TYPE

      /** Value for <code>BusMember</code> target type */
      public static final String TARGET_TYPE_BUSMEMBER = "BusMember";

      /** Value for <code>Custom</code> target type */
      public static final String TARGET_TYPE_CUSTOM = "Custom";

      /** Value for <code>ME</code> target type */
      public static final String TARGET_TYPE_ME = "ME";

      // The following values should not be normally be used

      /** Value for <code>Destination</code> target type - internal use only */
      public static final String TARGET_TYPE_DESTINATION = "Destination";

      /** Value for <code>MEUUid</code> target type  - internal use only */
      public static final String TARGET_TYPE_MEUUID = "MEUuid";

      /** Default value for target type */
      public static final String TARGET_TYPE_DEFAULT = TARGET_TYPE_BUSMEMBER;

    /**
     * The key value for the optional <code>targetSignificance</code> property. The
     * <code>String</code> value for this property sets whether a messaging engine
     * in the <code>targetGroup</code> is required or preferred.
     * Valid values for this property are:
     *
     * <ul>
     * <li><code>Required</code> means a messaging engine in the specified <code>targetGroup</code> is required.
     * <li><code>Preferred</code> means a messaging engine in the specified <code>targetGroup</code> is preferred but not required.
     * </ul>
     *
     * The default value for this property is <code>Preferred</code>.
     */

    public static final String TARGET_SIGNIFICANCE = "targetSignificance";

      // Acceptable values for TARGET_SIGNIFICANCE

      /** Value for <code>required</code> target significance */
      public static final String TARGET_SIGNIFICANCE_REQUIRED = "Required";

      /** Value for <code>preferred</code> target significance */
      public static final String TARGET_SIGNIFICANCE_PREFERRED = "Preferred";

      /** Default value for target significance */
      //chetan liberty change
      //currently we have only 1 ME per Liberty profile.Hence making target significance to required
      public static final String TARGET_SIGNIFICANCE_DEFAULT = TARGET_SIGNIFICANCE_REQUIRED;

    /**
     * The key value for the optional <code>connectionProximity</code> property. The <code>String</code>
     * value for this property sets a limit on the proximity of a suitable messaging engine to
     * which the client can be connected.
     * Valid values for this property are:
     *
     * <ul>
     * <li><code>Server</code> means any messaging engine in the same server process is suitable
     * <li><code>Cluster</code> means any messaging in the same clustered BusMember is suitable
     * <li><code>Host</code> means any messaging in the same host as the client is suitable
     * <li><code>Bus</code> means any messaging engine in the bus is suitable
     * </ul>
     *
     * When searching for a suitable messaging engine the search proceeds as:
     *
     * <ol>
     * <li><code>Server</code>
     * <li><code>Cluster</code>
     * <li><code>Host</code>
     * <li><code>Bus</code>
     * </ol>
     *
     * For example a <code>connectionProximity</code> value of <code>Host</code>
     * would allow a search of the Server, Cluster & Host proximity for a suitable messaging engine.
     * <p>
     * The default value for this property is <code>Bus</code>.
     */

    public static final String CONNECTION_PROXIMITY = "connectionProximity";

      // Acceptable values for CONNECTION_PROXIMITY

      /** Value for <code>Server</code> proximity */
      public static final String CONNECTION_PROXIMITY_SERVER = "Server";

      /** Value for <code>Cluster</code> proximity */
      public static final String CONNECTION_PROXIMITY_CLUSTER = "Cluster";

      /** Value for <code>Host</code> proximity */
      public static final String CONNECTION_PROXIMITY_HOST = "Host";

      /** Value for <code>Bus</code> proximity */
      public static final String CONNECTION_PROXIMITY_BUS = "Bus";

      /** Default value for connection proximity */
      public static final String CONNECTION_PROXIMITY_DEFAULT = CONNECTION_PROXIMITY_BUS;

    /**
     * The key value for the optional <code>connectionMode</code> property. The <code>String</code>
     * value for this property sets the connection mode to be used.
     * Valid values for this property are:
     *
     * <ul>
     * <li><code>Normal</code> means the connection is a normal connection
     * <li><code>Recovery</code> means the connection is required for transaction recovery
     * </ul>
     *
     * <p>
     * The default value for this property is <code>Normal</code>.
     */

    public static final String CONNECTION_MODE = "connectionMode";                                            //LIDB3645

      // Acceptable values for CONNECTION_MODE                                                                  LIDB3645

      /** Value for <code>Normal</code> mode */
      public static final String CONNECTION_MODE_NORMAL = "Normal";                                           //LIDB3645

      /** Value for <code>Recovery</code> mode */
      public static final String CONNECTION_MODE_RECOVERY = "Recovery";                                       //LIDB3645

      /** Default value for connection mode */                                                                          
      public static final String CONNECTION_MODE_DEFAULT = CONNECTION_MODE_NORMAL;                            //LIDB3645

    /**
     * The key value for the optional <code>subcriptionProtocol</code> property. The <code>String</code>
     * value for this property sets the subscription protocol to be used.
     * Valid values for this property are:
     *
     * <ul>
     * <li><code>Unicast</code> means unicast protocol is used for subscriptions
     * <li><code>Multicast</code> means multicast protocol is used for subscriptions
     * <li><code>UnicastAndMulticast</code> means both unicast and multicast protocols are used for subscriptions
     * </ul>
     *
     * <p>
     * The default value for this property is <code>Unicast</code>.
     */

    public static final String SUBSCRIPTION_PROTOCOL = "subscriptionProtocol";                                //LIDB3743

      // Acceptable values for SUBSCRIPTION_PROTOCOL                                                            LIDB3743

      /** Value for <code>Unicast</code> mode */
      public static final String SUBSCRIPTION_PROTOCOL_UNICAST = "Unicast";                                   //LIDB3743

      /** Value for <code>Multicast</code> mode */
      public static final String SUBSCRIPTION_PROTOCOL_MULTICAST = "Multicast";                               //LIDB3743

      /** Value for <code>UnicastAndMulticast</code> mode */
      public static final String SUBSCRIPTION_PROTOCOL_UNICAST_AND_MULTICAST = "UnicastAndMulticast";         //LIDB3743

      /** Default value for subscription protocol */                                                                          
      public static final String SUBSCRIPTION_PROTOCOL_DEFAULT = SUBSCRIPTION_PROTOCOL_UNICAST;               //LIDB3743

    /**
     * The key value for the optional <code>multicastInterface</code> property. The <code>String</code>
     * value for this property sets the multicast interface to be used for mutlicast subscriptions.
     * Valid values for this property are:
     *
     * <ul>
     * <li>Any IP address in dotted notation, for example: 129.42.16.99
     * <li><code>none</code> means use the default local IP address
     * </ul>
     *
     * <p>
     * The default value for this property is <code>none</code>.
     */

    public static final String MULTICAST_INTERFACE = "multicastInterface";                                    //LIDB3743

      // Acceptable values for MULTICAST_INTERFACE                                                              LIDB3743

      /** Value for <code>None</code> mode */
      public static final String MULTICAST_INTERFACE_NONE = "none";                                           //LIDB3743

      /** Default value for subscription protocol */                                                                          
      public static final String MULTICAST_INTERFACE_DEFAULT = MULTICAST_INTERFACE_NONE;                      //LIDB3743

}
