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
package com.ibm.ws.sib.utils.comms;

import java.security.AccessControlException;
import java.security.AccessController;
import java.util.List;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.UtConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A class which represents an end point as a host, port and chain.
 */
public class ProviderEndPoint
{
  /* ************************************************************************** */
  /**
   * An IncorrectCallException indicates that the parse of a ProviderEndPoint list failed.
   * It provide a set of inserts suitable for use in the CWSIT0055 message:
   *
   * CWSIT0055E: An incorrect value {0} was passed for the connection property {1}, valid values are: {2}.
   *
   * (We can't actually use this message in this class as it's in TRM and sib.trm depends (at build
   * time on sib.utils, which means if we depended on trm we would have a circular build problem)
   */
  /* ************************************************************************** */
  public static class IncorrectCallException extends Exception
  {
    /** Comment for <code>serialVersionUID</code> */
    private static final long serialVersionUID = 4554836035251059637L;
    private final String _badString;

    /* -------------------------------------------------------------------------- */
    /* IncorrectCallException constructor
    /* -------------------------------------------------------------------------- */
    /**
     * Construct a new IncorrectCallException.
     *
     * @param bad The incorrect value which makes the parse fail
     */
    public IncorrectCallException(final String bad)
    {
      _badString = bad;
    }

    /* -------------------------------------------------------------------------- */
    /* getInserts method
    /* -------------------------------------------------------------------------- */
    /**
     * @return a set of inserts suitable for use in a CWSIT0055 message
     */
    public String[] getInserts()
    {
      return new String[] { _badString, PROVIDER_ENDPOINTS, "[<IPv6 address>]:<port>:<chain>" };
    }
  }

   private static final TraceComponent tc = SibTr.register(ProviderEndPoint.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);

   /* ******************************************************************************************************************** */
   /* Technically we should use the constants in com.ibm.wsspi.sib.core.trm.SibTrmConstants, but that would                */
   /* cause a circular build dependency, so we'll have to have our own version and get a unittest to cross-validate        */
   /* that the values are actually the same                                                                                */
   /*                                                                                                                      */
      /** Value for <code>InboundBasicMessaging</code> transport chain */
      public static final String TARGET_TRANSPORT_CHAIN_BASIC = "InboundBasicMessaging";

      /** Value for <code>InboundSecureMessaging</code> transport chain */
      public static final String TARGET_TRANSPORT_CHAIN_SECURE = "InboundSecureMessaging";

      /** Value for <code>BootstrapBasicMesaging</code> bootstrap protocol */
      public static final String BOOTSTRAP_TRANSPORT_CHAIN_BASIC = "BootstrapBasicMessaging";

      /** Value for <code>BootstrapSecureMessaging</code> bootstrap protocol */
      public static final String BOOTSTRAP_TRANSPORT_CHAIN_SECURE = "BootstrapSecureMessaging";

      /** Value for <code>BootstrapTunneledMessaging</code> bootstrap protocol */
      public static final String BOOTSTRAP_TRANSPORT_CHAIN_TUNNELED = "BootstrapTunneledMessaging";

      /** Value for <code>BootstrapTunneledSecureMessaging</code> bootstrap protocol */
      public static final String BOOTSTRAP_TRANSPORT_CHAIN_TUNNELED_SECURE = "BootstrapTunneledSecureMessaging";

      public static final String PROVIDER_ENDPOINTS = "providerEndpoints";

      // Default provider endpoint values

      /** Default bootstrap server host */
      public static final String PROVIDER_ENDPOINTS_LOCALHOST = "localhost";

      /** Default bootstrap server ports */
      public static final String PROVIDER_ENDPOINTS_PORT_BASIC      = "7276";
      public static final String PROVIDER_ENDPOINTS_PORT_SECURE     = "7286"; // d313487
   /* End of constants stolen from SibTrmConstants                                                                         */
   /* ********************************************************************************************************************* */

   /** Hack for consumability defect 526214. Each row in this table represents a possible set of synonyms for a chain name.
    *  If the chain name matches (ignoring case) any of the entries in the row, we use the first string in the row as
    *  the chain name (i.e., the first entry must be the bootstrap one)
    */
   private static final String[][] SYNONYMS = { { BOOTSTRAP_TRANSPORT_CHAIN_BASIC,  TARGET_TRANSPORT_CHAIN_BASIC,  "BasicMessaging" }
                                              , { BOOTSTRAP_TRANSPORT_CHAIN_SECURE, TARGET_TRANSPORT_CHAIN_SECURE, "SecureMessaging" }
                                              , { BOOTSTRAP_TRANSPORT_CHAIN_TUNNELED }
                                              , { BOOTSTRAP_TRANSPORT_CHAIN_TUNNELED_SECURE }
                                              };

   private String hostname;
   private Integer portNumber;
   private String chainName;

   /**
    * Create an endpoint from a hostname string and a port number
    * @param host The hostname.
    * @param port The port.
    */
   public ProviderEndPoint(String host, Integer port)
   {
      hostname = host;
      portNumber = port;
   }

   /**
    * Create an endpoint from a hostname string, a port number and a chain name string
    * @param host The hostname.
    * @param port The port.
    * @param chain The chain.
    */
   public ProviderEndPoint(String host, Integer port, String chain)
   {
      hostname = host;
      portNumber = port;
      chainName = chain;
   }

   /**
    * Returns the hostname for this end point.
    * @return String The hostname.
    */
   public String getHost()
   {
      return hostname;
   }

   /**
    * Returns the port number for this end point
    * @return Integer The port number.
    */
   public Integer getPort()
   {
      return portNumber;
   }

   /**
    * Returns the chain name for this end point
    * @return String The chain name.
    */
   public String getChain()
   {
      return chainName;
   }

   /* -------------------------------------------------------------------------- */
  /* equals method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.lang.Object#equals(java.lang.Object)
   * @param other
   * @return Compare another object with this one
   */
  public boolean equals(Object other)
   {
      boolean equal = false;

      if (other == this)
      {
         equal = true;
      }
      else if ((other != null) && (other instanceof ProviderEndPoint))
      {
         final ProviderEndPoint otherEndpoint = (ProviderEndPoint)other;

         final boolean hostnamesEqual;
         if (hostname == null)
            hostnamesEqual = otherEndpoint.hostname == null;
         else
            hostnamesEqual = hostname.equals(otherEndpoint.hostname);

         final boolean portNumbersEqual;
         if (portNumber == null)
            portNumbersEqual = otherEndpoint.portNumber == null;
         else
            portNumbersEqual = portNumber.equals(otherEndpoint.portNumber);

         final boolean chainNamesEqual;
         if (chainName == null)
            chainNamesEqual = otherEndpoint.chainName == null;
         else
            chainNamesEqual = chainName.equals(otherEndpoint.chainName);

         equal = hostnamesEqual && portNumbersEqual && chainNamesEqual;
      }

      return equal;
   }

   /* -------------------------------------------------------------------------- */
  /* hashCode method
  /* -------------------------------------------------------------------------- */
  /**
   * @see java.lang.Object#hashCode()
   * @return a hashcode
   */
  public int hashCode()
   {
       final int hostnameHash;
       if (hostname == null) hostnameHash = 12345;
       else hostnameHash = hostname.hashCode();

       final int portNumberHash;
       if (portNumber == null) portNumberHash = 54321;
       else portNumberHash = portNumber.hashCode();

       final int chainNameHash;
       if (chainName == null) chainNameHash = 12543;
       else chainNameHash = chainName.hashCode();

       return hostnameHash ^ portNumberHash ^ chainNameHash;
   }

   /**
    * @return Returns some info about this object
    */
   public String toString()
   {
     return "EndPoint@" + Integer.toHexString(System.identityHashCode(this)) + ":- " + getEndPointInfo();
   }//toString

   /**
    * @return Returns some info about this object - d326401
    */
   public String getEndPointInfo()
   {
     // d273600 - If the host name contains : characters then it is an IPv6 address,
     // and should be contained with [ ... ] braces to avoid confusion.
     // (moved from TrmEndPoint under d274452)

     String hostString = "";

     if (hostname != null) {
       if (hostname.indexOf(":") != -1) {
         hostString += "["+hostname+"]";
       } else {
         hostString += hostname;
       }
     } else {
       // nb - null
       hostString += hostname;
     }

     if(chainName == null)
     {
        hostString += ":" + portNumber;
     }
     else
     {
        hostString += ":" + portNumber  + ":" + chainName;
     }

     return hostString;
   }//getEndPointInfo

  /**
   * Takes the provider endpoint string provided and parses it into the appropriate
   * number of TrmEndPoint objects, which are then added to the endpoints List
   * parameter of this class.
   *
   * Provider endpoints use the format
   *   <host>:<port>:<chain>,<host>:<port>:<chain>,...
   *
   * For IPv6 addresses the <host> element must be enclosed in square braces  [ ... ]
   * as in the following example;
   *    [2002:914:fc12:179:9:20:141:42]:7123:BootstrapBasicMessaging
   *
   * A string that does not contain [ ] characters is assumed to be IPv4 (or a hostname
   * rather than an IP address)
   *
   * @param providerEndpoints The provider endpoint string as specified by the client
   *                          (for example in the JMS ConnectionFactory).
   * @param listOfEndPoints   The populated list of endpoints described by the providerEndpoints string
   *                          Each of the elements of the list are an instance of TrmEndPoint.
   *                          The list is not allowed to be null.
   * @param whenDefaultingUseSecurePort true if, when using defaults, the secure port should be used
   * @return true if the parse shows that one or more user defined endpoints are in the list
   * @throws IncorrectCallException Thrown if the [,] braces do not match up.
   */
  public static boolean parseProviderEndpoints(String providerEndpoints,
      List<ProviderEndPoint> listOfEndPoints, boolean whenDefaultingUseSecurePort)
      throws IncorrectCallException {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "parseProviderEndpoints", new Object[]{ providerEndpoints, listOfEndPoints, whenDefaultingUseSecurePort });

    // Indicate whether the user specified their own endpoints,
    // or whether we made them up on their behalf.
    boolean hasUserDefinedEndPoints = false;

    if (providerEndpoints != null && providerEndpoints.trim().equals("")) {
      providerEndpoints = null;
    }

    // If there are any endpoints defined by the client.
    if (providerEndpoints != null) {

      // Split the full string based on the comma separator. We know that
      // the elements on the triplets (including IPv4/v6 IP addresses) do
      // not contain commas.
      // NB. IPv6 is RFC 2373)
      StringTokenizer st = new StringTokenizer(providerEndpoints,",");

      // Look at each of the elements in turn.
      while (st.hasMoreElements()) {

        // Prepare some variables to receive the parsed data.
        String host  = null;
        String port  = null;
        String chain = null;

        String str = st.nextToken();
        if (str != null) {

          // The user has given us some explicit endpoint information.
          hasUserDefinedEndPoints = true;

          // To hold the position of the opening brace if one exists.
          int openBracePos = 0;

          // Check for IPv6 bracing [ ... ]
          if ((openBracePos = str.indexOf("[")) != -1) {
            // We have found an IPv6 opening brace.

            // To hold the position of the closing brace if one exists.
            int closeBracePos = 0;

            if ((closeBracePos = str.indexOf("]")) != -1) {
              // Found the closing brace as well.
              host = str.substring(openBracePos+1, closeBracePos);

              // Remove the parsed data from the string.
              str = str.substring(closeBracePos+1);

            } else {
              // No closing brace to match the opening brace.
              throw new IncorrectCallException(str);
            } // if closing brace.

          } else {
            // No opening brace.
            // Check for an orphaned closing brace to be on the safe side.
            if (str.indexOf("]") != -1) {
              throw new IncorrectCallException(str);
            }

          } // if opening brace.

          // Take out any whitespace that might cause confusion.
          if (str.indexOf(" ") != -1)
          {
            // Doing a quick check for contains will avoid running the
            // regex on the following line in cases where there are no
            // spaces.
            str = str.replaceAll(" ", "");
          }


          // Tokenize the remaining information (some will have been removed above if there
          // was an IPv6 address) using the : character. Use the true flag so that we get
          // given the separators too.
          StringTokenizer st2 = new StringTokenizer(str, ":", true);

          // We are going to use this counter to determine which item of information
          // we are looking for.
          int elementSearch = 1;

          // If the host was in IPv6 form we will already have set it here, and the
          // str string will look like a ":port:chain" format. This missing host
          // is handled successfully by the block below anyway.

          while (st2.hasMoreTokens())
          {
            String next = st2.nextToken();

            // If we find a separator character then we start looking at the
            // next item (host/port etc)
            if (":".equals(next))
            {
              elementSearch++;
            } else
            {

              switch(elementSearch)
              {
                case 1: // host
                  host = next;
                  break;

                case 2: // port
                  port = next;
                  break;

                case 3: // chain
                  // Allow for synonyms of the chain names to be used
                  chain = resolveSynonymousChainName(next);
                  break;

                default: // bad formatting - ignore it!
                  break;

              }//switch

            }//if

          }//while

          // Default any missing values
          if (host == null || host.trim().equals("")) {
            host = PROVIDER_ENDPOINTS_LOCALHOST;
          }

          boolean usingDefaultPort = false;

          if (port == null || port.trim().equals("")) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No port was specified");

            if (chain != null) {
              // A non-empty chain was specified. We should use this information to override
              // the defaulting of the port number if we recognise the chain name. This has
              // higher precedence than whether a password was specified because the user
              // gave us direct instructions.
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "A non-empty chain was specified");
              if (BOOTSTRAP_TRANSPORT_CHAIN_BASIC.equals(chain)) {
                whenDefaultingUseSecurePort = false;
              } else if (BOOTSTRAP_TRANSPORT_CHAIN_SECURE.equals(chain)) {
                whenDefaultingUseSecurePort = true;
              }

            } // if override port based on chain

            if (whenDefaultingUseSecurePort) {
              port = PROVIDER_ENDPOINTS_PORT_SECURE;
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "Password specified so defaulting to " + port);
            } else {
              port = PROVIDER_ENDPOINTS_PORT_BASIC;
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No password specified so defaulting to " + port);
            } // if isPasswordSpecified
            usingDefaultPort = true;
          } // if port

          if (chain == null || chain.trim().equals("")) {
              chain = BOOTSTRAP_TRANSPORT_CHAIN_BASIC;
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, "No chain specified so defaulting to " + chain);
          } // if chain

          ProviderEndPoint newEP = new ProviderEndPoint(host, new Integer(port), chain);
          if (!listOfEndPoints.contains(newEP)) {
            listOfEndPoints.add(newEP);
          }

        } // if element not null

      } // while more elements

    } // if providerEndpoints

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(tc, listOfEndPoints.toString());
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "parseProviderEndpoints", Boolean.valueOf(hasUserDefinedEndPoints));
    return hasUserDefinedEndPoints;

  } // parseProviderEndpoints

  /* -------------------------------------------------------------------------- */
  /* resolveSynonymousChainName method
  /* -------------------------------------------------------------------------- */
  /**
   * @param original The original chain name
   * @return The chain name we're actually going to use
   */
  private static String resolveSynonymousChainName(String original)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "resolveSynonymousChainName", original);

    String actual = original; // Default to just using the original value

    // Scan each row of the SYNONYMS table
    for(int i=0; i<SYNONYMS.length; i++)
    {
      // Looking for a synonym that matches the orignal value (ignoring case)
      for (int j=0; j<SYNONYMS[i].length; j++)
      {
        if (SYNONYMS[i][j].equalsIgnoreCase(original))
        {
          actual = SYNONYMS[i][0]; // Use the first entry in the row as the actual chain name
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "resolveSynonymousChainName", actual);
    return actual;
  }
}
