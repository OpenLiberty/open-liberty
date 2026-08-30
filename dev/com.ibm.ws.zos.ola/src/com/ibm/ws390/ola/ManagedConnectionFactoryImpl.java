/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws390.ola;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws390.ola.jca.ConnectionFactoryImpl;
import com.ibm.ws390.ola.jca.ConnectionManagerImpl;
import com.ibm.ws390.ola.jca.ConnectionRequestInfoImpl;
import com.ibm.ws390.ola.jca.ManagedConnectionImpl;
import com.ibm.ws390.ola.jca.RemoteProxyInformation;
import com.ibm.ws390.ola.jca.TraceUtil;

/**
 * Implementation of the javax.resource.spi.ManagedConnectionFactory interface.
 * The ManagedConnectionFactory is created on the server side to create
 * ManagedConnection objects.  It also pulls the properties out of the ra.xml file
 * and keeps them.  These properties tell the ManagedConnectionFactory how to connect
 * to the EIS backend, or for us, the MockXAResourceServer.
 * When a client asks for a connection, the request is passed along from the
 * ConnectionFactory to the ManagedConnectionFactory, which creates a ManagedConnection,
 * from which a Connection is derived and passed back to the client.
 */
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory, ResourceAdapterAssociation {

	private static final TraceComponent tc = Tr.register(ManagedConnectionFactoryImpl.class);
	private static final TraceUtil tu = new TraceUtil(tc);

	private static final long serialVersionUID = 4813729835458187035L;

  /**
   * First version of this object
   */
  private static final int VERSION_1 = 1;                        /* @F003705A*/

  /**
   * Second version of this object
   */
  private static final int VERSION_2 = 2;                        /* @F013381A*/

  /**
   * Current object version.  Only needs to be changed if something drastic
   * alters the object structure, since small additions/deletions can be
   * handled by the Java serialization mechanism.
   */
  private static final int CURRENT_VERSION = VERSION_2;          /* @F013381C*/

  /**
   * Fields for serialization
   */
  private final static ObjectStreamField[] serialPersistentFields =
    new ObjectStreamField[]                                      /* @F013381C*/
    {
      new ObjectStreamField("_version", Integer.TYPE),
      new ObjectStreamField("highestConnectionID", Integer.TYPE),
      new ObjectStreamField("debugMode", Boolean.TYPE),
      new ObjectStreamField("_registerName", String.class),
      new ObjectStreamField("_OTMAGroupID", String.class),
      new ObjectStreamField("_OTMAServerName", String.class),
      new ObjectStreamField("_OTMAClientName", String.class),
      new ObjectStreamField("_OTMASyncLevel", String.class),
      new ObjectStreamField("_OTMAMaxSegments", Integer.TYPE),
      new ObjectStreamField("_OTMAMaxRecvSize", Integer.TYPE),
      new ObjectStreamField("_remoteHostname", String.class),
      new ObjectStreamField("_remotePort", Integer.TYPE),
      new ObjectStreamField("_remoteJNDI", String.class),
      new ObjectStreamField("_username", String.class),
      new ObjectStreamField("_password", String.class),
      new ObjectStreamField("_remoteRealm", String.class),
      new ObjectStreamField("_jndiUsername", String.class),
      new ObjectStreamField("_jndiPassword", String.class),
      new ObjectStreamField("_OTMARequestLLZZ", Integer.TYPE),
      new ObjectStreamField("_OTMAResponseLLZZ", Integer.TYPE),
      new ObjectStreamField("_UseCICSContainer", Integer.TYPE),
      new ObjectStreamField("_LinkTaskTranID", String.class),
      new ObjectStreamField("_LinkTaskReqContID", String.class),
      new ObjectStreamField("_LinkTaskReqContType", Integer.TYPE),
      new ObjectStreamField("_LinkTaskRspContID", String.class),
      new ObjectStreamField("_LinkTaskRspContType", Integer.TYPE),
      new ObjectStreamField("_LinkTaskChanID", String.class),       /* @F014448A */
      new ObjectStreamField("_LinkTaskChanType", Integer.TYPE),     /* @F014448A */
      new ObjectStreamField("_ConnectionWaitTimeout", Integer.TYPE),
      new ObjectStreamField("_RRSTransactional", Boolean.TYPE)  /* @F014447A*/
    };

	/**
	 * Trace header.
	 */
	private transient final String header =                        /* @F003705C*/
    new String(" !!ManagedConnectionFactoryImpl: ");
	
	/**
	 * Logging mechanism.
	 */
	private transient PrintWriter log = null;                      /* @F003705C*/

	/**
	 * The ResourceAdapter instance, set by the application server when
	 * the MCF is created.
	 */
	private transient ResourceAdapter adapter = null;              /* @F003705A*/
	
  /**
   * This object version.  
   */
  private int _version = CURRENT_VERSION;                        /* @F003705A*/

	/**
	 * The highest unique connection ID that gets assigned to connections.
	 * This gets incremented on every new connection request.
	 */
	private int highestConnectionID = 0;

	/**
	 * Boolean flag indicating whether or not we issue debugging messages.
	 */
	public boolean debugMode = false;
	
 /**
   * The register name this MCF is connected to, if set.  If not set, the
   * register name is determined by the ConnectionSpecImpl.
   */
  private String _registerName = null;                           /* @F003691A*/

 /**
   * OTMA XCF group name.  If not set with setOTMAGroupID, the name 
   * is determined by the ConnectionSpecImpl.
   */
  private String _OTMAGroupID = null;                            /* @F003694A*/

 /**
   * OTMA Server name.  If not set with setOTMAServerName, the name 
   * is determined by the ConnectionSpecImpl.
   */
  private String _OTMAServerName = null;                         /* @F003694A*/

 /**
   * OTMA Client name.  If not set with setOTMAClientName, the name 
   * is determined by the ConnectionSpecImpl. 
   */
  private String _OTMAClientName = null;

  /**
   * OTMA Sync level setting (0|1).  If not set with setOTMASyncLevel, the setting 
   * is determined by the ConnectionSpecImpl. Blanks or nulls defaults to SyncLevel0 or
   * SyncOnReturn.
   */
  private String _OTMASyncLevel = null;                         /* @F003694A*/

  /**
   * OTMA Receive Multi-segment messages - max received segment.
   */
  private int _OTMAMaxSegments = 0;                                  /* @F013381C*/

  /**
   * The OTMA Receive Multi_segment setting as provided by the MCF.  
   * This may differ from the setting we are using in this connection.
   */
  private int _OTMAMaxRecvSize = 32768;                              /* @F013381C*/

  /**
   * The OTMA Request prefix format LLZZ provided by the MCF.  
   * This may differ from the setting we are using in this connection.
   */
  private int _OTMARequestLLZZ = 1;                         	     /* @F013381A*/

  /**
   * The OTMA Response prefix format LLZZ provided by the MCF.  
   * This may differ from the setting we are using in this connection.
   */
  private int _OTMAResponseLLZZ = 0;                                /* @F013381A*/

  /**
   * Remote hostname for distributed proxy
   */
  private String _remoteHostname = null;                         /* @F003705A*/

  /**
   * Remote port number for distributed proxy
   */
  private int _remotePort = 0;                                   /* @F003705A*/

  /**
   * Remote JNDI name for distributed proxy EJB
   */
  private String _remoteJNDI = null;                             /* @F003705A*/

  /**
   * Remote user ID for the distributed proxy EJB
   */
  private String _username = null;                               /* @F003705A*/

  /**
   * Remote password for the distributed proxy EJB.
   */
  private String _password = null;                               /* @F003705A*/

  /**
   * Remote security realm for the distributed proxy
   */
  private String _remoteRealm = null;                            /* @F003705A*/

  /**
   * Username to use with JNDI lookup.
   */
  private String _jndiUsername = null;                           /* @F003705A*/
  
  /**
   * Password to use with JNDI lookup.
   */
  private String _jndiPassword = null;                           /* @F003705A*/

  /**
   * CICS link server link task transaction ID.
   */
  private String _LinkTaskTranID = null;                         /* @F013381A*/

  /**
   * Use CICS containers flag
   */
  private int _UseCICSContainer = 0;                             /* @F013381A*/

  /**
   * CICS link server link task request container ID.
   */
  private String _LinkTaskReqContID = null;                      /* @F013381A*/

  /**
   * CICS link server link task request container type.
   */
  private int _LinkTaskReqContType = 0;                          /* @F013381A*/

  /**
   * CICS link server link task response container ID.
   */
  private String _LinkTaskRspContID = null;                      /* @F013381A*/

  /**
   * CICS link server link task response container type.
   */
  private int _LinkTaskRspContType = 0;                          /* @F013381A*/

  /**
   * CICS link server link task channel ID.
   */
  private String _LinkTaskChanID = null;                         /* @F014448A */
  
  /**
   * CICS link server link task channel type.
   */
  private int _LinkTaskChanType = 0;                             /* @F014448A */

  /**
   * Connection wait time in seconds before resource exception for timeout.
   */
  private int _ConnectionWaitTimeout = 30;                       /* @F013381A*/

  /**
   * IMS OTMA 2PC support requires connections that use RRS.
   */
  private boolean _RRSTransactional = false;                     /* @F014447A*/


  /**
	 * Default Constructor
	 */
	public ManagedConnectionFactoryImpl()
	{
  	  boolean debugMode = getDebugMode().booleanValue();
	  tu.debug("ManagedConnectionFactoryImpl: inside default constructor.", debugMode);
	}

	/**
	 * Sets the resource adapter.
	 */
	public void setResourceAdapter(ResourceAdapter adapter)
		throws ResourceException
	{
		if (this.adapter != null)
		{
			throw new ResourceException("ResourceAdapter already associated");
		}

		this.adapter = adapter;
	}
	
	/**
	 * Gets the resource adapter.
	 */
	public ResourceAdapter getResourceAdapter()
	{
		return adapter;
	}

	/**
	 * Creates a connection factory, which is bound into JNDI.  The client looks up
	 * this connection factory to create connections.
	 * Uses a connection manager supplied by the J2EE server.
	 * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory(ConnectionManager)
	 */
	public Object createConnectionFactory(ConnectionManager cm)
		throws ResourceException
	{
		debugMode = getDebugMode().booleanValue();
		tu.debug("ManagedConnectionFactoryImpl: inside createConnectionFactory for ConnectionManager cm: "+cm, debugMode);
		return new ConnectionFactoryImpl(this, cm);
	}

	/**
	 * Creates a connection factory in a non-managed environment.  This has
	 * never been tested but there's not a whole lot that can go wrong.
	 * @see javax.resource.spi.ManagedConnectionFactory#createConnectionFactory()
	 */
	public Object createConnectionFactory() throws ResourceException
	{
		debugMode = getDebugMode().booleanValue();
		ConnectionManager defaultCM = new ConnectionManagerImpl(debugMode);
		tu.debug("ManagedConnectionFactoryImpl: inside createConnectionFactory() newed up a ConnectionManagerImpl defaultCM: "+defaultCM, debugMode);
		
		return new ConnectionFactoryImpl(this, defaultCM);
	}

	/**
	 * Creates a new ManagedConnection.
	 * @see javax.resource.spi.ManagedConnectionFactory#createManagedConnection(Subject, ConnectionRequestInfo)
	 */
	public ManagedConnection createManagedConnection(
			Subject subject,
			ConnectionRequestInfo info)
		throws ResourceException
	{
		boolean debugMode = getDebugMode().booleanValue();

		tu.debug("ManagedConnectionFactoryImpl: createManagedConnection(), Subject = " +
				((subject == null) ? "null" : subject.toString()), debugMode); /* @PM89577C*/
	
		ConnectionRequestInfoImpl crii = null; /* @580321A*/

		/*---------------------------------------------------------------------*/
		/* Verify that the info provided is valid.                             */
		/*---------------------------------------------------------------------*/
		if (info != null)
		{
			if ((info instanceof ConnectionRequestInfoImpl) == false) /* @580321A*/
			{
				throw new ResourceException("ConnectionRequestInfo is invalid");
			}

			crii = (ConnectionRequestInfoImpl)info;                   /* @580321A*/
		}

		RemoteProxyInformation rpi = null;                          /* @F003705A*/

		if (_remoteHostname != null)                                /* @F003705A*/
		{
			rpi = new RemoteProxyInformation(_remoteHostname,
											 _remotePort,
											 _remoteJNDI,
											 _remoteRealm,
											 _jndiUsername,
											 _jndiPassword);          /* @F003705A*/
		}                                                           /* @F003705A*/

		tu.debug("ManagedConnectionFactoryImpl: leaving createManagedConnection() - driving ManagedConnectionImpl", debugMode);

		/*---------------------------------------------------------------------*/
		/* Check the configured values of the OTMA receive buffers.  If the    */
		/* configured values are not appropriate, issue a warning message      */
		/* once per connection factory.  Note that these values can be         */
		/* over-ridden by the individual connections using a ConnectionSpec.   */
		/* Those problems will be exposed as warnings on the Interaction.      */
		/*---------------------------------------------------------------------*/
		if (_OTMAMaxRecvSize > (_OTMAMaxSegments * 32768)) {       /* @PI52653A*/
			Tr.warning(tc, "CWWKB0397W", new Object[] {Integer.toString(_OTMAMaxRecvSize),
                                                       Integer.toString(_OTMAMaxSegments),
                                                       Integer.toString(_OTMAMaxSegments * 32768)});
			_OTMAMaxRecvSize = _OTMAMaxSegments * 32768;
		}


		return new ManagedConnectionImpl(debugMode,
		     							 this,
		     							 ++highestConnectionID, 
		     							 crii,
		     							 rpi);  
	}

	/**
	 * Matches a requested ManagedConnection to a set of ManagedConnections 
   * that the application server has open.  We must match the properties on 
   * this MCF as well as the properties contained in the ConnectionRequestInfo.
	 * @see javax.resource.spi.ManagedConnectionFactory#matchManagedConnections(Set, Subject, ConnectionRequestInfo)
	 */
	@SuppressWarnings("rawtypes")
	public ManagedConnection matchManagedConnections(
			Set arg0,
			Subject arg1,
			ConnectionRequestInfo arg2)
		throws ResourceException   {

		ManagedConnectionImpl con = null;
		ConnectionRequestInfoImpl crii = null;
		Iterator i = null;

		// Make sure we were passed a valid ConnectionSet.		
		if (arg0 == null)
		{
			tu.debug(header + "ConnectionSet is null", debugMode);
			return null;
		}
		else
		{
			i = arg0.iterator();
			tu.debug(header + "Entering matchManagedConnections, size = " + arg0.size(), debugMode);
		}

		// Make sure we were passed a valid ConnectionRequestInfo object.
		if ((arg2 == null) || (!(arg2 instanceof ConnectionRequestInfoImpl)))
		{
			tu.debug(header + "ConnectionRequestInfo is invalid: " + arg2, debugMode);
			return null;
		}
		else
		{
			crii = (ConnectionRequestInfoImpl)arg2;
		}

		// Since we can specify some properties in the MCF or in the
		// ConnectionSpec/ConnectionRequestInfo, we need to figure out what
		// the properties are that we are checking against.
		String registerName = (crii.getRegisterName() == null ? 
                           _registerName : 
                           crii.getRegisterName());
		String OTMAGroupID = (crii.getOTMAGroupID() == null ?
                          _OTMAGroupID:
                          crii.getOTMAGroupID());
		String OTMAServerName = (crii.getOTMAServerName() == null ?
                             _OTMAServerName :
                             crii.getOTMAServerName());
		String OTMAClientName = (crii.getOTMAClientName() == null ?
                             _OTMAClientName :
                             crii.getOTMAClientName());
		String OTMASyncLevel = (crii.getOTMASyncLevel() == null ?
                             _OTMASyncLevel :
                             crii.getOTMASyncLevel());
		int OTMAMaxSegments = (crii.getOTMAMaxSegmentsFromCSI() == false ?
                            _OTMAMaxSegments : 
                            crii.getOTMAMaxSegments());
		int OTMAMaxRecvSize = (crii.getOTMAMaxRecvSizeFromCSI() == false ?
                            _OTMAMaxRecvSize :
                            crii.getOTMAMaxRecvSize());

		// Added these properties with the tooling RA support.       @F013381A
		int OTMARequestLLZZ  = (crii.getOTMARequestLLZZFromCSI() == false ?
            _OTMARequestLLZZ :
            crii.getOTMAReqLLZZ());                       /* @F013381A*/
		int OTMAResponseLLZZ = (crii.getOTMAResponseLLZZFromCSI() == false ?
            _OTMAResponseLLZZ :                    
            crii.getOTMARespLLZZ());                      /* @F013381A*/
		String LinkTaskTranID = (crii.getLinkTaskTranID() == null ?
            _LinkTaskTranID :
            crii.getLinkTaskTranID());                    /* @F013381A*/
		int UseCICSContainer = (crii.getuseCICSContainerFromCSI() == false ?
            _UseCICSContainer :
            crii.getUseCICSContainer());                  /* @F013381A*/
		String LinkTaskReqContID = (crii.getLinkTaskReqContID() == null ?
            _LinkTaskReqContID :
            crii.getLinkTaskReqContID());                 /* @F013381A*/
		int LinkTaskReqContType = (crii.getlinkTaskReqContTypeFromCSI() == false ?
            _LinkTaskReqContType :
            crii.getLinkTaskReqContType());               /* @F013381A*/
		String LinkTaskRspContID = (crii.getLinkTaskRspContID() == null ?
            _LinkTaskRspContID :
            crii.getLinkTaskRspContID());                 /* @F013381A*/
		int LinkTaskRspContType = (crii.getlinkTaskRspContTypeFromCSI() == false ?
            _LinkTaskRspContType :
            crii.getLinkTaskRspContType());               /* @F013381A*/
    String LinkTaskChanID = (crii.getLinkTaskChanID() == null ?                     /* @F014448A */
            _LinkTaskChanID :                                                       /* @F014448A */
            crii.getLinkTaskChanID());                                              /* @F014448A */
    int LinkTaskChanType = (crii.getlinkTaskChanTypeFromCSI() == false ?            /* @F014448A */
            _LinkTaskChanType :                                                     /* @F014448A */
            crii.getLinkTaskChanType());                                            /* @F014448A */
		int ConnectionWaitTimeout = (crii.getConnectionWaitTimeoutFromCSI() == false ?
            _ConnectionWaitTimeout :
            crii.getConnectionWaitTimeout());             /* @F013381A*/
    
		boolean RRSTransactional = _RRSTransactional;         /* @F014447A*/
    
		// If the register name and OTMA Group ID are both null, we can't match the connection.
		if ((registerName == null) && (OTMAGroupID == null))  /* @F013381C*/ 
		{
			throw new IllegalArgumentException("Register name must be specified on the ManagedConnectionFactory or provided in the ConnectionSpec parameter of the ConnectionFactory.getConnection() call");
		}
		
		// Iterate over the connection set.
		while (i.hasNext())  {
			Object o = i.next();
			
			tu.debug(header + "Attempting to match this object: " + o, debugMode);
			
			if ((o != null) || (o instanceof ManagedConnectionImpl))
			{
				con = (ManagedConnectionImpl)o;

				if (con.doesConnectionMatch(registerName, OTMAGroupID, OTMAServerName, OTMAClientName,
                                    OTMASyncLevel, OTMAMaxSegments,
                                    OTMAMaxRecvSize,
                                    OTMARequestLLZZ, OTMAResponseLLZZ,
                                    LinkTaskTranID, UseCICSContainer,
                                    LinkTaskReqContID, LinkTaskReqContType,
                                    LinkTaskRspContID, LinkTaskRspContType,
									LinkTaskChanID, LinkTaskChanType,          /* @F014448A */
                                    ConnectionWaitTimeout,
                                    RRSTransactional,
                                    crii) == true)                  /* @F014447A*/
				{
					tu.debug(header + "Found a match, " + con.toString(), debugMode);
					tu.debug("ManagedConnectionFactoryImpl: UseCICSContainer: "+UseCICSContainer, debugMode);
					tu.debug("ManagedConnectionFactoryImpl: LinkTaskReqContID: "+LinkTaskReqContID, debugMode);
					tu.debug("ManagedConnectionFactoryImpl: LinkTaskRspContID: "+LinkTaskRspContID, debugMode);
					
					return con;
				}
			}
		}
		tu.debug(header + "Could not find a managed connection in the connection set.", debugMode);
		return null;
	}

	/**
	 * Sets the log writer for this ManagedConnectionFactory.  Not sure what this
	 * does exactly.
	 * @see javax.resource.spi.ManagedConnectionFactory#setLogWriter(PrintWriter)
	 */
	public void setLogWriter(PrintWriter arg0) throws ResourceException
	{
		log = arg0;
	}

	/**
	 * Retrieves the log writer for this ManagedConnectionFactory.
	 * @see javax.resource.spi.ManagedConnectionFactory#getLogWriter()
	 */
	public PrintWriter getLogWriter() throws ResourceException {
		return log;
	}
	
	/**
	 * Retrieves a property in the RAR xml that tells the connector whether or not to
	 * output debugging information.
	 */
	public Boolean getDebugMode()
	{
		return new Boolean(debugMode);
	}
	
	/**
	 * Sets the debugMode property.  This is somehow magically called when the MCF
	 * is created.
	 */
	public void setDebugMode(Boolean debugMode)
	{
		this.debugMode = debugMode.booleanValue();
		tu.debug(header + "set debugMode to " + this.debugMode, this.debugMode);
	}

 /**
   * Retrieves a property in the RAR xml that tells us what registration
   * name we are using.
   */
  public String getRegisterName()                                /* @F003691A*/
  {
    return _registerName;
  }

  /**
   * Sets the register name property.
   */
  public void setRegisterName(String registerName)               /* @F003691A*/
  {
    if (registerName != null) 
    {
      int registerNameLength = registerName.length();

      if (registerNameLength == 0)
      {
        _registerName = null;
      }
      else if (registerNameLength > 
               ConnectionRequestInfoImpl.REGISTER_NAME_LENGTH)
      {
        throw new IllegalArgumentException(
          "Register name (" + registerName + ") cannot be longer than " + 
          ConnectionRequestInfoImpl.REGISTER_NAME_LENGTH + " characters");
      }
      else
      {
        _registerName = registerName;
      }
    }
    else
    {
      _registerName = null;
    }
  }

 /**
   * Retrieves a property in the RAR xml that tells us what OTMA XCF goup 
   * name we are using.
   */
  public String getOTMAGroupID()                                 /* @F003694A*/
  {
    return _OTMAGroupID;
  }

  /**
   * Sets the OTMA XCF group name property.
   */
  public void setOTMAGroupID(String OTMAGroupID)                 /* @F003694A*/
  {
 	  debugMode = getDebugMode().booleanValue();

	  tu.debug(header + "OTMAGroupID from MCF: " + OTMAGroupID, debugMode);
    if (OTMAGroupID != null)                                     /* @F003694A*/
    {
      int OTMAGroupIDLength = OTMAGroupID.length();              /* @F003694A*/

      if (OTMAGroupIDLength == 0)
      {
    	_OTMAGroupID = null;
      }
      else if (ConnectionRequestInfoImpl.OTMAGroupIDPattern.matcher(OTMAGroupID.trim()).matches())
      {
    	  _OTMAGroupID = OTMAGroupID;
    	  tu.debug(header + "OTMAGroupID set from MCF", debugMode);
      }
      else
      {
          throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKB0395E", new Object[] { "Group", OTMAGroupID, ConnectionRequestInfoImpl.OTMA_GROUPID_LENGTH } ));
      }	  
    }                                                            /* @F003694A*/
    else
    {
      _OTMAGroupID = null;
    }
  }

  public String getOTMAServerName()                              /* @F003694A*/
  {
    return _OTMAServerName;
  }

  /**
   * Sets the OTMA XCF server/partner name property.
   */
  public void setOTMAServerName(String OTMAServerName)            /* @F003694A*/
  {
    debugMode = getDebugMode().booleanValue();

    tu.debug(header + "OTMAServerName from MCF: " + OTMAServerName, debugMode);
    if (OTMAServerName != null)                                   /* @F003694A*/
    {
      int OTMAServerNameLength = OTMAServerName.length();         /* @F003694A*/

      if (OTMAServerNameLength == 0)
      {
    	_OTMAServerName = null;
      }
      else if (ConnectionRequestInfoImpl.OTMAServerNamePattern.matcher(OTMAServerName.trim()).matches())
      {
    	  _OTMAServerName = OTMAServerName;
          tu.debug(header + "OTMAServerName set from MCF", debugMode);
      }
      else
      {
          throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKB0395E", new Object[] { "Server", OTMAServerName, ConnectionRequestInfoImpl.OTMA_SERVER_NAME_LENGTH } ));
      }
    }                                                             /* @F003694A*/
    else
    {
      _OTMAServerName = null;
    }
  }


  public String getOTMAClientName()
  {
    return _OTMAClientName;
  }

  /**
   * Sets the OTMA XCF Client name property.
   */
  public void setOTMAClientName(String OTMAClientName)
  {
    debugMode = getDebugMode().booleanValue();
    tu.debug(header + "OTMAClientName from MCF: " + OTMAClientName, debugMode);
			
    if (OTMAClientName == null)
    {
        _OTMAClientName = null;
    }
    else
    {
      OTMAClientName = OTMAClientName.toUpperCase();
      if (ConnectionRequestInfoImpl.OTMAClientNamePattern.matcher(OTMAClientName.trim()).matches())
      {
    	  _OTMAClientName = OTMAClientName;
          tu.debug(header + "OTMAClientName set from MCF", debugMode);
      }
      else
      {
          Tr.warning(tc, "CWWKB0393W", new Object[] { OTMAClientName, ConnectionRequestInfoImpl.OTMA_CLIENT_NAME_LENGTH });
      }
    }
  }


  /**
   * Retrieves a property in the RAR xml that tells us what OTMA Sync level 
   * we are using.
   */
  public String getOTMASyncLevel()                                /* @F003694A*/
  {
    return _OTMASyncLevel;
  }

  /**
   * Sets the OTMA Sync level property.
   */
  public void setOTMASyncLevel(String OTMASyncLevel)             /* @F003694A*/
  {
 	debugMode = getDebugMode().booleanValue();
 	
 	tu.debug(header + "OTMASyncLevel from MCF: " + OTMASyncLevel, debugMode);
    if (OTMASyncLevel != null)                                   /* @F003694A*/
    {
      int OTMASyncLevelLength = OTMASyncLevel.length();          /* @F003694A*/

      if (OTMASyncLevelLength == 0)
      {
    	_OTMASyncLevel = null;
      }
      else if (OTMASyncLevelLength > 
               ConnectionRequestInfoImpl.OTMA_SYNCLEVEL_LENGTH) /* @F003694A*/
      {
    	  throw new IllegalArgumentException(
    	     "OTMA Sync level (" + OTMASyncLevel + ") must be a single character 0|1)");  /* @F003694A*/
    	      
      }
      else
      {
    	  _OTMASyncLevel = OTMASyncLevel;

          tu.debug(header + "OTMASyncLevel set from MCF", debugMode);
      }
    }                                                             /* @F003694A*/
    else
    {
      _OTMASyncLevel = null;
    }
  }

  /**
   * Retrieves a property in the RAR xml that tells us what the maximum number 
   * of IMS message segments are supported. 
   */
  public Integer getOTMAMaxSegments()                                   /* @F013381C*/
  {
	  return new Integer(_OTMAMaxSegments);
  }

  /**
   * Sets the OTMA Max Multi-segment messages property.
   */
  public void setOTMAMaxSegments(Integer OTMAMaxSegments)             /* @678472C*/
  {
 	debugMode = getDebugMode().booleanValue();
 	
 	tu.debug(header + "OTMAMaxSegments from MCF: " + OTMAMaxSegments.intValue(), debugMode);
    
 	if (OTMAMaxSegments > 0)                                       /* @670111A*/
    {
    	  _OTMAMaxSegments = OTMAMaxSegments.intValue();          /* @F013381C*/

          tu.debug(header + "OTMAMaxSegments set from MCF", debugMode); /* @670111A*/
    }                                                             /* @670111A*/
    else                                                          /* @670111A*/   
    {                                                             /* @670111A*/ 
      _OTMAMaxSegments = 0;                                       /* @670111A*/ 
    }                                                             /* @670111A*/   
  }                                                               /* @670111A*/    

  /**
   * Retrieves a property in the RAR xml that tells us what the maximum Receive  
   * message size is for IMS Multi-segment messages. 
   */
  public Integer getOTMAMaxRecvSize()                                 /* @F013381C*/
  {
	  return new Integer(_OTMAMaxRecvSize);
  }

  /**
   * Sets the OTMA Max Receive message size property.
   */
  public void setOTMAMaxRecvSize(Integer OTMAMaxRecvSize)             /* @678472C*/
  {
 	debugMode = getDebugMode().booleanValue();
 	
 	tu.debug(header + "OTMAMaxRecvSize from MCF: " + OTMAMaxRecvSize.intValue(), debugMode);
    
 	if (OTMAMaxRecvSize > 0)                                       /* @670111A*/
    {
    	  _OTMAMaxRecvSize = OTMAMaxRecvSize.intValue();          /* @F013381C*/

          tu.debug(header + "OTMAMaxRecvSize set from MCF", debugMode); /* @670111A*/
    }                                                             /* @670111A*/
    else                                                          /* @670111A*/   
    {                                                             /* @670111A*/ 
      _OTMAMaxRecvSize = 0;                                       /* @670111A*/ 
    }                                                             /* @670111A*/   
  }                                                               /* @670111A*/    

  /**
   * Gets the remote host name property
   */
  public String getRemoteHostname()                             /* @F003705A*/
  {
    return _remoteHostname;
  }

  /**
   * Sets the remote host name property
   */
  public void setRemoteHostname(String name)                    /* @F003705A*/
  {
    _remoteHostname = name;
  }

  /**
   * Gets the remote port number property
   */
  public Integer getRemotePort()                                /* @F003705A*/
  {
    return new Integer(_remotePort);
  }

  /**
   * Sets the remote port number property
   */
  public void setRemotePort(Integer port)                       /* @F003705A*/
  {
    if (port != null)
    {
      _remotePort = port.intValue();
    }
  }

  /**
   * Gets the remote JNDI name property
   */
  public String getRemoteJNDIName()                             /* @F003705A*/
  {
    return _remoteJNDI;
  }

  /**
   * Sets the remote JNDI name property
   */
  public void setRemoteJNDIName(String jndiName)                /* @F003705A*/
  {
    _remoteJNDI = jndiName;
  }

  /**
   * Gets the username to be used on the JNDI lookup of the proxy EJB.
   */
  public String getRemoteJNDIUsername()                         /* @F003705A*/
  {
    return _jndiUsername;
  }

  /**
   * Sets the username to be used on the JNDI lookup of the proxy EJB.
   */
  public void setRemoteJNDIUsername(String username)            /* @F003705A*/
  {
    _jndiUsername = username;
  }

  /**
   * Gets the password to be used with jndiUsername
   */
  public String getRemoteJNDIPassword()                         /* @F003705A*/
  {
    return _jndiPassword;
  }

  /**
   * Sets the password to be used with jndiUsername
   */
  public void setRemoteJNDIPassword(String password)            /* @F003705A*/
  {
    _jndiPassword = password;
  }

  /**
   * Gets the remote user ID property
   */
  public String getUsername()                                   /* @F003705A*/
  {
    return _username;
  }

  /**
   * Sets the remote user ID property
   */
  public void setUsername(String username)                      /* @F003705A*/
  {
    _username = username;
  }

  /**
   * This method is driven by the WebSphere app server J2C code, to set the
   * user ID from a component managed JAAS authentication alias into the
   * managed connection factory.  The component managed authentication alias
   * is used when the application is configured to use application managed
   * authorization, there is no username or password defined programattically,
   * and a username and password were not provided explicitly on the connection
   * factory.  In this case, if a component managed JAAS authentication alias
   * is provided, it will be used.
   *
   * This is all pretty confusing so here is a summary (not necessarily in
   * the most logical order):
   *  - Default username and password can be provided in multiple ways to
   *    this MCF.  They can be set on the MCF itself via properties.  They
   *    can be set by J2C by declaring a component managed JAAS alias.  The
   *    component managed JAAS alias will take precedence over properties
   *    specified on the MCF.
   *  - For container managed authentication, the container will pass a 
   *    Subject on a getConnection request.  The username and password from
   *    this subject will over-ride any default username and password provided.
   *    Note that no Subject is passed if using application authentication.
   *  - For application managed authentication, username and password in the
   *    connection spec are used.  If none is provided in the connection spec,
   *    the default username and password from the MCF are used.
   * 
   * Note that it is assumed that if the application is using container managed
   * authentication, they are not passing a username and password on the
   * connection spec.  This is implied in section 8.4.1 of the JCA 1.5 spec.
   * This allows the Subject passed by the container to take precedence.  If
   * username and password are provided on the connection spec, then the
   * connection will use these values instead.
   *
   * ** IMPORTANT **
   * There is not "getUserName" method because this is not a MCF property.
   * This method is only included because the J2C code (in J2CUtilityClass)
   * assumes that this method is going to be present on the MCF (because
   * there should be a property called UserName).  Our user name property
   * is "Username" and to avoid confusion and breaking existing customers,
   * we only provide the setter, which allows the J2C code to set the user
   * ID from the component managed JAAS alias.
   */
  public void setUserName(String x)                         /* @PM89577A*/
  {                                                         /* @PM89577A*/
    setUsername(x);                                         /* @PM89577A*/
  }                                                         /* @PM89577A*/

  /**
   * Gets the remote password property
   */
  public String getPassword()                                   /* @F003705A*/
  {
    return _password;
  }

  /**
   * Sets the remote password property
   */
  public void setPassword(String password)                      /* @F003705A*/
  {
    _password = password;
  }

  /**
   * Gets the remote security realm
   */
  public String getRemoteJNDIRealm()                            /* @F003705A*/
  {
    return _remoteRealm;
  }

  /**
   * Sets the remote security realm
   */
  public void setRemoteJNDIRealm(String realm)                  /* @F003705A*/
  {
    _remoteRealm = realm;
  }
  /**
   * Gets the OTMA Request LLZZ value
   */
  public Integer getOTMARequestLLZZ()                            /* @F013381A*/
  {
    return new Integer(_OTMARequestLLZZ);
  }

  /**
   * Sets the OTMA Request LLZZ value
   */
  public void setOTMARequestLLZZ(Integer OTMARequestLLZZ)        /* @F013381A*/
  {
	  if (OTMARequestLLZZ != null) {
		  _OTMARequestLLZZ = OTMARequestLLZZ.intValue();
	  }
  }

  /**
   * Gets the OTMA Response LLZZ value
   */
  public Integer getOTMAResponseLLZZ()                            /* @F013381A*/
  {
    return new Integer(_OTMAResponseLLZZ);
  }

  /**
   * Sets the OTMA Response LLZZ value
   */
  public void setOTMAResponseLLZZ(Integer OTMAResponseLLZZ)        /* @F013381A*/
  {
	  if (OTMAResponseLLZZ != null) {
		  _OTMAResponseLLZZ = OTMAResponseLLZZ.intValue();
	  }
  }

  /**
   * Gets the CICS Link task transaction ID value
   */
  public String getLinkTaskTranID()                           /* @F013381A*/
  {
    return _LinkTaskTranID;
  }

  /**
   * Sets the CICS Link task transaction ID value
   */
  public void setLinkTaskTranID(String LinkTaskTranID)        /* @F013381A*/
  {
    _LinkTaskTranID = LinkTaskTranID;
  }

  /**
   * Gets the Use CICS containers value
   */
  public Integer getUseCICSContainer()                            /* @F013381A*/
  {
    return new Integer(_UseCICSContainer);
  }

  /**
   * Sets the Use CICS containers value
   */
  public void setUseCICSContainer(Integer useCICSContainer)       /* @F013381A*/
  {
    _UseCICSContainer = useCICSContainer.intValue();
  }

  /**
   * Gets the CICS Request Container ID value
   */
  public String getLinkTaskReqContID()                        /* @F013381A*/
  {
    return _LinkTaskReqContID;
  }

  /**
   * Sets the CICS Request Container ID value
   */
  public void setLinkTaskReqContID(String LinkTaskReqContID)  /* @F013381A*/
  {
    _LinkTaskReqContID = LinkTaskReqContID;
  }

  /**
   * Gets the CICS Link task request container type value
   */
  public Integer getLinkTaskReqContType()                         /* @F013381A*/
  {
    return new Integer(_LinkTaskReqContType);
  }

  /**
   * Sets the CICS Link task Request container type value
   */
  public void setLinkTaskReqContType(Integer LinkTaskReqContType) /* @F013381A*/
  {
    _LinkTaskReqContType = LinkTaskReqContType.intValue();
  }

  
  /**
   * Gets the CICS Response Container ID value
   */
  public String getLinkTaskRspContID()                        /* @F013381A*/
  {
    return _LinkTaskRspContID;
  }

  /**
   * Sets the CICS Response Container ID value
   */
  public void setLinkTaskRspContID(String LinkTaskRspContID)  /* @F013381A*/
  {
    _LinkTaskRspContID = LinkTaskRspContID;
  }

  /**
   * Gets the CICS Link task response container type value
   */
  public Integer getLinkTaskRspContType()                         /* @F013381A*/
  {
    return new Integer(_LinkTaskRspContType);
  }

  /**
   * Sets the CICS Link task Response container type value
   */
  public void setLinkTaskRspContType(Integer LinkTaskRspContType) /* @F013381A*/
  {
    _LinkTaskRspContType = LinkTaskRspContType.intValue();
  }
  
  /**
   * Gets the CICS Channel ID value
   */
  public String getLinkTaskChanID()                                  /* @F014448A */
  {                                                                  /* @F014448A */
    return _LinkTaskChanID;                                          /* @F014448A */
  }                                                                  /* @F014448A */

  /**
   * Sets the CICS Channel ID value
   */
  public void setLinkTaskChanID(String LinkTaskChanID)               /* @F014448A */
  {                                                                  /* @F014448A */
    _LinkTaskChanID = LinkTaskChanID;                                /* @F014448A */
  }                                                                  /* @F014448A */

  /**
   * Gets the CICS Link task channel type value
   */
  public Integer getLinkTaskChanType()                               /* @F014448A */
  {                                                                  /* @F014448A */
    return new Integer(_LinkTaskChanType);                           /* @F014448A */
  }                                                                  /* @F014448A */

  /**
   * Sets the CICS Link task channel type value
   */
  public void setLinkTaskChanType(Integer LinkTaskChanType)          /* @F014448A */
  {                                                                  /* @F014448A */
    _LinkTaskChanType = LinkTaskChanType.intValue();                 /* @F014448A */
  }                                                                  /* @F014448A */

  /**
   * Gets the Connection Wait Timeout value
   */
  public Integer getConnectionWaitTimeout()                         /* @F013381A*/
  {
    return new Integer(_ConnectionWaitTimeout);
  }

  /**
   * Sets the Connection Wait Timeout value
   */
  public void setConnectionWaitTimeout(Integer ConnectionWaitTimeout) /* @F013381A*/
  {
    _ConnectionWaitTimeout = ConnectionWaitTimeout.intValue();
  }

  /**
   * Sets the RRS Transactional flag
   */
  public void setRRSTransactional(Boolean RRSTransactional)           /* @F014447A*/
  {
 	debugMode = getDebugMode().booleanValue();
	 	
 	tu.debug(header + "RRSTransactional from MCF: " + RRSTransactional, debugMode);
 	
    _RRSTransactional = RRSTransactional;
  }

  /**
   * Gets the RRS Transactional flag from MCF
   */
  public Boolean getRRSTransactional()                                   /* @F014447A*/
  {
 	debugMode = getDebugMode().booleanValue();
	 	
 	tu.debug(header + "Returning RRSTransactional MC: " + _RRSTransactional, debugMode);
    
 	return _RRSTransactional;
  }


	/**
	 * Creates a hash code so that the app server can managed the pools of MCFs.
	 */
	public int hashCode()
	{
       /* Unfortunately registerName and the OTMA parms are mutually exclusive.
          We'll have to hash against them both. */
         int registerHash = (_registerName != null ? _registerName.hashCode() : 0);
         int otmaSvrHash = 
           (_OTMAServerName != null ? _OTMAServerName.hashCode() : 0);
         int otmaClntHash = 
           (_OTMAClientName != null ? _OTMAClientName.hashCode() : 0);
         int otmaGrpHash = 
           (_OTMAGroupID != null ? _OTMAGroupID.hashCode() : 0);
         int otmaSyncHash =
           (_OTMASyncLevel != null ? _OTMASyncLevel.hashCode() : 0);

	   return ((registerHash / 5) + (otmaSvrHash / 5) + (otmaGrpHash / 5) +
            (otmaSyncHash / 5) + (otmaClntHash / 5)); // TODO: Over 5 now?
	}
	
	
	/**
	 * Determines if two ManagedConnectionFactoryImpls are equal.
	 */
	public boolean equals(Object o)
	{
    boolean equal = false;                                  

    if (o == this)                                               /* @F003705A*/
    {                                                            /* @F003705A*/
      equal = true;                                              /* @F003705A*/
    }                                                            /*2@F003705A*/
    else if ((o != null) && (o instanceof ManagedConnectionFactoryImpl))
    {                                                            /*2@F003705A*/
      ManagedConnectionFactoryImpl that = (ManagedConnectionFactoryImpl)o;

      equal = ((compareObjects(this.header, that.header)) &&     /* @F003705A*/
               (compareObjects(this.log, that.log)) &&
               (compareObjects(this.adapter, that.adapter)) &&
               (this._version == that._version) &&
               (this.highestConnectionID == that.highestConnectionID) &&
               (this.debugMode == that.debugMode) &&
               (compareObjects(this._registerName, that._registerName)) &&
               (compareObjects(this._OTMAGroupID, that._OTMAGroupID)) &&
               (compareObjects(this._OTMAServerName, that._OTMAServerName)) &&
               (compareObjects(this._OTMAClientName, that._OTMAClientName)) &&
               (compareObjects(this._OTMASyncLevel, that._OTMASyncLevel)) &&
               (compareObjects(this._OTMAMaxSegments, that._OTMAMaxSegments))&&
               (compareObjects(this._OTMAMaxRecvSize, that._OTMAMaxRecvSize))&&
               (compareObjects(this._remoteHostname, that._remoteHostname)) &&
               (this._remotePort == that._remotePort) &&
               (compareObjects(this._remoteJNDI, that._remoteJNDI)) &&
               (compareObjects(this._username, that._username)) &&
               (compareObjects(this._password, that._password)) &&
               (compareObjects(this._remoteRealm, that._remoteRealm)) &&
               (compareObjects(this._jndiUsername, that._jndiUsername)) &&
               (compareObjects(this._jndiPassword, that._jndiPassword)) &&
               (this._OTMARequestLLZZ == that._OTMARequestLLZZ)&&                     /* @F013381A*/
               (this._OTMAResponseLLZZ == that._OTMAResponseLLZZ)&&                   /* @F013381A*/
               (compareObjects(this._LinkTaskTranID, that._LinkTaskTranID)) &&        /* @F013381A*/
               (this._UseCICSContainer == that._UseCICSContainer) &&                  /* @F013381A*/
               (compareObjects(this._LinkTaskReqContID, that._LinkTaskReqContID)) &&  /* @F013381A*/
               (this._LinkTaskReqContType == that._LinkTaskReqContType) &&            /* @F013381A*/
               (compareObjects(this._LinkTaskRspContID, that._LinkTaskRspContID)) &&  /* @F013381A*/
               (this._LinkTaskRspContType == that._LinkTaskRspContType) &&            /* @F013381A*/
			   (compareObjects(this._LinkTaskChanID, that._LinkTaskChanID)) &&        /* @F014448A */
			   (this._LinkTaskChanType == that._LinkTaskChanType) &&                  /* @F014448A */
      	       (this._ConnectionWaitTimeout == that._ConnectionWaitTimeout) &&        /* @F013381A*/
	           (this._RRSTransactional == that._RRSTransactional));                   /* @F014447A*/
    }                                                            /* @F003705A*/

    return equal;
	}

  /**
   * Compares two objects, either of which could be null
   */
  private boolean compareObjects(Object a, Object b)             /* @F003705A*/
  {
    boolean result = false;

    if (a == b) result = true;
    else if ((a != null) && (b != null))
    {
      result = a.equals(b);
    }

    return result;
  }
	
	public String toString()
	{
		java.lang.StringBuffer sb = new java.lang.StringBuffer();
		sb.append("\n ManagedConnectionFactoryImpl:");
		sb.append("\n debugMode = " + debugMode);
		return sb.toString();
	}

  /**
   * Serialization support to write an object.  Note that this is implemented
   * so that we can support object versioning in the future, even though there
   * is only one object version now.
   */
  private void writeObject(ObjectOutputStream s)                 /* @F003705A*/
    throws java.io.IOException
  {
    ObjectOutputStream.PutField putField = s.putFields();

    putField.put("_version", _version);
    putField.put("highestConnectionID", highestConnectionID);
    putField.put("debugMode", debugMode);
    putField.put("_registerName", _registerName);
    putField.put("_OTMAGroupID", _OTMAGroupID);
    putField.put("_OTMAServerName", _OTMAServerName);
    putField.put("_OTMAClientName", _OTMAClientName);
    putField.put("_OTMASyncLevel", _OTMASyncLevel);
    putField.put("_OTMAMaxSegments", _OTMAMaxSegments);
    putField.put("_OTMAMaxRecvSize", _OTMAMaxRecvSize);
    putField.put("_remoteHostname", _remoteHostname);
    putField.put("_remotePort", _remotePort);
    putField.put("_remoteJNDI", _remoteJNDI);
    putField.put("_username", _username);
    putField.put("_password", _password);
    putField.put("_remoteRealm", _remoteRealm);
    putField.put("_jndiUsername", _jndiUsername);
    putField.put("_jndiPassword", _jndiPassword);
    putField.put("_OTMARequestLLZZ", _OTMARequestLLZZ);              /* @F133381A*/
    putField.put("_OTMAResponseLLZZ", _OTMAResponseLLZZ);            /* @F133381A*/
    putField.put("_UseCICSContainer", _UseCICSContainer);            /* @F133381A*/
    putField.put("_LinkTaskTranID", _LinkTaskTranID);                /* @F133381A*/
    putField.put("_LinkTaskReqContID", _LinkTaskReqContID);          /* @F133381A*/
    putField.put("_LinkTaskReqContType", _LinkTaskReqContType);      /* @F133381A*/
    putField.put("_LinkTaskRspContID", _LinkTaskRspContID);          /* @F133381A*/
    putField.put("_LinkTaskRspContType", _LinkTaskRspContType);      /* @F133381A*/
	putField.put("_LinkTaskChanID", _LinkTaskChanID);                /* @F014448A */
	putField.put("_LinkTaskChanType", _LinkTaskChanType);            /* @F014448A */
    putField.put("_ConnectionWaitTimeout", _ConnectionWaitTimeout);  /* @F133381A*/
    putField.put("_RRSTransactional", _RRSTransactional);            /* @F014447A*/

    s.writeFields();
  }

  /**
   * Serialization support to read an object.  Note that this is implemented
   * so that we can support object versioning in the future, even though there
   * is only one object version now.
   */
  private void readObject(ObjectInputStream s)                   /* @F003705A*/
    throws java.io.IOException, 
           java.lang.ClassNotFoundException
  {
    ObjectInputStream.GetField getField = s.readFields();
    
    _version = getField.get("_version", (int)VERSION_1);

    if (_version > CURRENT_VERSION)
    {
      throw new java.io.InvalidObjectException(
        "Version number in serialized object is not supported (" + _version + 
        " > " + CURRENT_VERSION + ")");
    }

    highestConnectionID = getField.get("highestConnectionID", (int)0);
    debugMode = getField.get("debugMode", false);

    if (getField.defaulted("_registerName"))
    {
      throw new java.io.InvalidObjectException("No register name in object");
    }

    _registerName = (String) getField.get("_registerName", null);
    _OTMAGroupID = (String) getField.get("_OTMAGroupID", null);
    _OTMAServerName = (String) getField.get("_OTMAServerName", null);
    _OTMAClientName = (String) getField.get("_OTMAClientName", null);
    _OTMASyncLevel = (String) getField.get("_OTMASyncLevel", null);
    _OTMAMaxSegments = (Integer)getField.get("_OTMAMaxSegments", null);
    _OTMAMaxRecvSize = (Integer)getField.get("_OTMAMaxRecvSize", null);
    _remoteHostname = (String) getField.get("_remoteHostname", null);
    _remotePort = getField.get("_remotePort", (int)0);
    _remoteJNDI = (String) getField.get("_remoteJNDI", null);
    _username = (String) getField.get("_username", null);
    _password = (String) getField.get("_password", null);
    _remoteRealm = (String) getField.get("_remoteRealm", null);
    _jndiUsername = (String) getField.get("_jndiUsername", null);
    _jndiPassword = (String) getField.get("_jndiPassword", null);
    _OTMARequestLLZZ = getField.get("_OTMARequestLLZZ",  (int)0);             /* @F133381A*/
    _OTMAResponseLLZZ = getField.get("_OTMAResponseLLZZ",  (int)0);           /* @F133381A*/
    _LinkTaskTranID = (String) getField.get("_LinkTaskTranID", null);         /* @F133381A*/
    _UseCICSContainer = getField.get("_UseCICSContainer",  (int)0);           /* @F133381A*/
    _LinkTaskReqContID = (String) getField.get("_LinkTaskReqContID", null);   /* @F133381A*/
    _LinkTaskReqContType = getField.get("_LinkTaskReqContType", (int)0);      /* @F133381A*/
    _LinkTaskRspContID = (String) getField.get("_LinkTaskRspContID", null);   /* @F133381A*/
    _LinkTaskRspContType = getField.get("_LinkTaskRspContType", (int)0);      /* @F133381A*/
	_LinkTaskChanID = (String) getField.get("_LinkTaskChanID", null);         /* @F014448A */
	_LinkTaskChanType = getField.get("_LinkTaskChanType", (int)0);            /* @F014448A */
    _ConnectionWaitTimeout = getField.get("_ConnectionWaitTimeout", (int)0);  /* @F133381A*/
    _RRSTransactional = getField.get("_RRSTransactional", false);             /* @F014447A*/
  }
}
