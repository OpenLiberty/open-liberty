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

package com.ibm.websphere.ola;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Object encapsulating the details of an Optimized Local Adapters registration
 */
public class OLARGEInformation implements Serializable  {
	
	/**
	 * Serialization key
	 */
	private static final long serialVersionUID = -5460052819866623277L;

  /**
   * Fields that we are serializing
   */
  private static ObjectStreamField[] serialPersistentFields =    /* @F003691A*/
    new ObjectStreamField[]                                      /* @F003691A*/
    {                                                            /* @F003691A*/
      new ObjectStreamField("_argeEye", String.class),           /* @F003691A*/
      new ObjectStreamField("_argeVersion", Short.class),        /* @F003691A*/
      new ObjectStreamField("_argeSize", Integer.class),         /* @F003691A*/
      new ObjectStreamField("_argeFlagDaemon", Boolean.class),   /* @F003691A*/
      new ObjectStreamField("_argeFlagServer", Boolean.class),   /* @F003691A*/
      new ObjectStreamField("_argeFlagExternalAddress", Boolean.class), /* @F003691A*/
      new ObjectStreamField("_argeFlagSecurity", Boolean.class), /* @F003691A*/
      new ObjectStreamField("_argeFlagWLM", Boolean.class),      /* @F003691A*/
      new ObjectStreamField("_argeFlagTransaction", Boolean.class), /* @F003691A*/
      new ObjectStreamField("_argeFlagActive", Boolean.class),   /* @F003691A*/
      new ObjectStreamField("_argeName", String.class),          /* @F003691A*/
      new ObjectStreamField("_argeJobName", String.class),       /* @F003691A*/
      new ObjectStreamField("_argeJobNum", Integer.class),       /* @F003691A*/
      new ObjectStreamField("_argeUUID", String.class),          /* @F003691A*/
      new ObjectStreamField("_argeConnectionsMin", Integer.class), /* @F003691A*/
      new ObjectStreamField("_argeConnectionsMax", Integer.class), /* @F003691A*/
      new ObjectStreamField("_argeActiveConnectionCount", Long.class), /* @F003691A*/
      new ObjectStreamField("_argeTraceLevel", Integer.class),   /* @F003691A*/
      new ObjectStreamField("_argeState", Long.class),           /* @F003691A*/
      new ObjectStreamField("_connectionHandles", List.class)    /* @F003691A*/
    };                                                           /* @F003691A*/

	/** Eye Catcher 'BBOARGE' in EBCDIC */
	private String _argeEye;                                       /* @F003691C*/

	/** Version number for this RGE */
	private Short _argeVersion;                                    /* @F003691C*/

	/** This is how we know how to slice the large byte buffer */
	private Integer _argeSize;                                     /* @F003691C*/

	// Flags that describe the RGE type.
	
	/** Daemon type flag */
	private Boolean _argeFlagDaemon;                               /* @F003691C*/

	/** Server type flag */
	private Boolean _argeFlagServer;                               /* @F003691C*/

	/** External address type flag */
	private Boolean _argeFlagExternalAddress;                      /* @F003691C*/
	
	/** Security enabled flag */
	private Boolean _argeFlagSecurity = null;                      /* @F003691C*/

	/** WLM flag */
	private Boolean _argeFlagWLM = null;                           /* @F003691C*/

	/** Transaction support type flag */
	private Boolean _argeFlagTransaction;                          /* @F003691C*/

	/** Flag that indicates if the RGE is active */
	private Boolean _argeFlagActive;                               /* @F003691C*/

	/** Register name in Cp1047 */
	private String _argeName;                                      /* @F003691C*/

	/** Registering job name in Cp1047 */
	private String _argeJobName;                                   /* @F003691C*/

	/** Registering job number */
	private Integer _argeJobNum;                                   /* @F003691C*/

	/** The UUID for this entry */
	private String _argeUUID;                                      /* @F003691C*/

	/** Min connections for this RGE */
	private Integer _argeConnectionsMin;                           /* @F003691C*/

	/** Max connections for this RGE */
	private Integer _argeConnectionsMax;                           /* @F003691C*/

	/** Number of current active connections */
	private Long _argeActiveConnectionCount;                       /* @F003691C*/
	
	/** Trace Level for the RGE */
	private Integer _argeTraceLevel = null;                        /* @F003691C*/
	
	/** State for the RGE */
	private Long _argeState = null;                                /* @F003691C*/
	
  /** List of connection handle information */
	private List<OLAConnectionHandle> _connectionHandles;          /* @F003691C*/
	
	/**
	 * Constructor that takes a OLARGE object as a parameter,
	 * and sets all of the attributes of the current OLARGEInformation object
	 * with the same attributes of the passed in RGE. 
	 * 
	 * @param currentRGE The RGE that the information will be copied from
	 * @throws Exception
	 */
	public OLARGEInformation(OLARGE currentRGE) throws Exception
	{
		this._argeEye = currentRGE.get_argeEye();
		this._argeVersion = currentRGE.get_argeVersion();
		this._argeSize = currentRGE.get_argeSize();
		this._argeFlagDaemon = currentRGE.get_argeFlagDaemon();
		this._argeFlagServer = currentRGE.get_argeFlagServer();
		this._argeFlagExternalAddress= currentRGE.get_argeFlagExternalAddress();
		this._argeFlagSecurity = currentRGE.get_argeFlagSecurity();
		this._argeFlagWLM = currentRGE.get_argeFlagWLM();
		this._argeFlagTransaction = currentRGE.get_argeFlagTransaction();
		this._argeFlagActive = currentRGE.get_argeFlagActive();
		this._argeName = currentRGE.get_argeName();
		this._argeJobName = currentRGE.get_argeJobName();
		this._argeJobNum = currentRGE.get_argeJobNum();
		this._argeUUID = currentRGE.get_argeUUID();
		this._argeConnectionsMax = currentRGE.get_argeConnectionsMax();
		this._argeConnectionsMin = currentRGE.get_argeConnectionsMin();
		this._argeActiveConnectionCount = currentRGE.get_argeActiveConnectionCount();
		this._argeTraceLevel = currentRGE.get_argeTraceLevel();
		this._argeState = currentRGE.get_argeState();		
		
		this._connectionHandles = currentRGE.get_connectionHandles();
	}

	/**
   * Returns the value of the eye catcher for this registration
	 * @return the eye catcher as a string
	 */
	public String get_argeEye() 
  {
		return _argeEye;
	}

	/**
   * Returns the version of the registration
	 * @return the version number
	 */
	public Short get_argeVersion() 
  {
		return _argeVersion;
	}

	/**
   * Returns the number of bytes of memory used by the registration object
	 * @return the number of bytes of memory used by the registration object
	 */
	public Integer get_argeSize() 
{
		return _argeSize;
	}

	/**
   * Indicates that this registration represents a WebSphere Application
   * Server for z/OS daemon address space.
	 * @return true if a daemon
	 */
	public Boolean get_argeFlagDaemon() {
		return _argeFlagDaemon;
	}

	/**
   * Indicates that this registration represents a WebSphere Application
   * Server for z/OS server address space
	 * @return true if a server
	 */
	public Boolean get_argeFlagServer() {
		return _argeFlagServer;
	}

	/**
   * Indicates that this registration represents a client registration.
	 * @return true if a client
	 */
	public Boolean get_argeFlagExternalAddress() {
		return _argeFlagExternalAddress;
	}

	/**
   * Indicates that this registration was created with security enabled
	 * @return true if security is enabled
	 */
	public Boolean get_argeFlagSecurity() {
		return _argeFlagSecurity;
	}

	/**
   * Indicates that this registration supports the propagation of the
   * transaction ID from a CICS performance block into WebSphere.
	 * @return true if transaction ID is propagated
	 */
	public Boolean get_argeFlagWLM() {
		return _argeFlagWLM;
	}

	/**
   * Indicates that this registration was created with transactions enabled
	 * @return true if transactions are enabled
	 */
	public Boolean get_argeFlagTransaction() {
		return _argeFlagTransaction;
	}

	/**
   * Indicates that this registration is active
	 * @return true if active
	 */
	public Boolean get_argeFlagActive() {
		return _argeFlagActive;
	}

	/**
   * Returns the register name for this registration
	 * @return the register name
	 */
	public String get_argeName() {
		return _argeName;
	}

	/**
   * Returns the name of the job which created this registration
	 * @return the job name
	 */
	public String get_argeJobName() {
		return _argeJobName;
	}

	/**
   * Returns the job number which created this registration
	 * @return the job number
	 */
	public Integer get_argeJobNum() {
		return _argeJobNum;
	}

	/**
   * If this registration represents a WebSphere Application Server for z/OS
   * server, returns the UUID for that server.
	 * @return the UUID for a server, or null if this registration does not
   *         represent a server
	 */
	public String get_argeUUID() {
		return _argeUUID;
	}

	/**
   * If this registration represents a client, returns the minimum number of
   * connections in the connection pool.
	 * @return the minimum number of connections in the connection pool.
	 */
	public Integer get_argeConnectionsMin() {
		return _argeConnectionsMin;
	}

	/**
   * If this registration represents a client, returns the maximum number of
   * connections in the connection pool.
	 * @return the maximum number of connections in the connection pool.
	 */
	public Integer get_argeConnectionsMax() {
		return _argeConnectionsMax;
	}

	/**
   * If this registration represents a client, returns the number of currently
   * active connections.
	 * @return the number of active connections
	 */
	public Long get_argeActiveConnectionCount() {
		return _argeActiveConnectionCount;
	}

	/**
   * Returns the level of tracing used by this registration
	 * @return the trace level
	 */
	public Integer get_argeTraceLevel() {
		return _argeTraceLevel;
	}

	/**
   * Returns the state of the registration
	 * @return the state
	 */
	public Long get_argeState() {
		return _argeState;
	}
	
  /**
   * Returns a list of objects representing the active connection
   * handles for this registration
   * @return A list of objects representing connection handles for this
   *         registration.
   */
	public Iterator<OLAConnectionHandle> getConnectionHandlesIterator()
	{
		return _connectionHandles.iterator();
	}
	
	/**
	 * Displays the RGE type. Could be one of three: 'd' for daemon, 's' for
   * server, or 'e' for external address space (client).
	 * @return The RGE type, as a single character string
	 */
	public String getTypeString() {
		if (this.get_argeFlagDaemon())
			return "d";
		
		if (get_argeFlagExternalAddress())
			return "e";
		
		if (get_argeFlagServer())
			return "s";

		return "?";
	}

	  /**
	   * Formats all of the OLARGEInformation information to a human readable format.
	   */
	public String toString() {
		try {
			StringBuffer sb = new StringBuffer();
			
			sb.append("OLARGE@");
			sb.append(System.identityHashCode(this));
			
			sb.append(", EYE = ");
			sb.append(this.get_argeEye());
			
      sb.append(", Register Name = ");                           /* @F003691A*/
      sb.append(this.get_argeName());                            /* @F003691A*/

			sb.append(", UUID = ");
			sb.append(this.get_argeUUID());
			
			sb.append(", Job Name = ");
			sb.append(this.get_argeJobName());
			
			sb.append(", Job Number = ");			
			sb.append(this.get_argeJobNum());
			
			sb.append(", flag_daemon = ");			
			sb.append(this.get_argeFlagDaemon());
			
			sb.append(", flag_server = ");			
			sb.append(this.get_argeFlagServer());
			
			sb.append(", flag_external_address_space = ");			
			sb.append(this.get_argeFlagExternalAddress());
			
			sb.append(", flag_active = ");			
			sb.append(this.get_argeFlagActive());
			
      sb.append(", flag_security = ");                           /* @F003691A*/
      sb.append(this.get_argeFlagSecurity());                    /* @F003691A*/

      sb.append(", flag_transaction = ");                        /* @F003691A*/
      sb.append(this.get_argeFlagTransaction());                 /* @F003691A*/

      sb.append(", flag_WLM = ");                                /* @F003691A*/
      sb.append(this.get_argeFlagWLM());                         /* @F003691A*/

			sb.append(", connections min = ");			
			sb.append(this.get_argeConnectionsMin());

			sb.append(", connections max = ");			
			sb.append(this.get_argeConnectionsMax());

			sb.append(", trace level = ");			
			sb.append(this.get_argeTraceLevel());

			sb.append(", state = ");			
			sb.append(this.get_argeState());
			
			return sb.toString();
		} catch (Exception e) {
		}
		return ("There was an error in calling toString() for OLARGE");
	}

  /**
   * Hash code
   */
  public int hashCode()                                          /* @F003691A*/
  {
    return _argeName.hashCode();
  }

  /**
   * Equals
   */
  public boolean equals(Object thatObject)                       /* @F003691A*/
  {
    boolean result = false;

    if (thatObject == this)
    {
      result = true;
    }
    else if ((thatObject != null) && (thatObject instanceof OLARGEInformation))
    {
      OLARGEInformation that = (OLARGEInformation)thatObject;

      if ((this._argeEye.equals(that._argeEye)) &&
          (this._argeVersion.equals(that._argeVersion)) &&
          (this._argeSize.equals(that._argeSize)) &&
          (this._argeFlagDaemon.equals(that._argeFlagDaemon)) &&
          (this._argeFlagServer.equals(that._argeFlagServer)) &&
          (this._argeFlagExternalAddress.
           equals(that._argeFlagExternalAddress)) &&
          (this._argeFlagWLM.equals(that._argeFlagWLM)) &&
          (this._argeFlagSecurity.equals(that._argeFlagSecurity)) &&
          (this._argeFlagTransaction.equals(that._argeFlagTransaction)) &&
          (this._argeFlagActive.equals(that._argeFlagActive)) &&
          (this._argeName.equals(that._argeName)) &&
          (this._argeJobName.equals(that._argeJobName)) && 
          (this._argeJobNum.equals(that._argeJobNum)) &&
          (this._argeUUID.equals(that._argeUUID)) &&
          (this._argeConnectionsMin.equals(that._argeConnectionsMin)) &&
          (this._argeConnectionsMax.equals(that._argeConnectionsMax)) &&
          (this._argeActiveConnectionCount.
           equals(that._argeActiveConnectionCount)) && 
          (this._argeTraceLevel.equals(that._argeTraceLevel)) &&
          (this._argeState.equals(that._argeState)) &&
          (this._connectionHandles.equals(that._connectionHandles)))
      {
        result = true;
      }
    }
    
    return result;
  }

  /**
   * Serialization support to write an object.
   */
  private void writeObject(ObjectOutputStream s)                 /* @F003691A*/
    throws java.io.IOException
  {
    ObjectOutputStream.PutField putField = s.putFields();
    putField.put("_argeEye", _argeEye);
    putField.put("_argeVersion", _argeVersion);
    putField.put("_argeSize", _argeSize);
    putField.put("_argeFlagDaemon", _argeFlagDaemon);
    putField.put("_argeFlagServer", _argeFlagServer);
    putField.put("_argeFlagExternalAddress", _argeFlagExternalAddress);
    putField.put("_argeFlagSecurity", _argeFlagSecurity);
    putField.put("_argeFlagWLM", _argeFlagWLM);
    putField.put("_argeFlagTransaction", _argeFlagTransaction);
    putField.put("_argeFlagActive", _argeFlagActive);
    putField.put("_argeName", _argeName);
    putField.put("_argeJobName", _argeJobName);
    putField.put("_argeJobNum", _argeJobNum);
    putField.put("_argeUUID", _argeUUID);
    putField.put("_argeConnectionsMin", _argeConnectionsMin);
    putField.put("_argeConnectionsMax", _argeConnectionsMax);
    putField.put("_argeActiveConnectionCount", _argeActiveConnectionCount);
    putField.put("_argeTraceLevel", _argeTraceLevel);
    putField.put("_argeState", _argeState);
    putField.put("_connectionHandles", _connectionHandles);
    s.writeFields();
  }

  /**
   * Serialization support to read an object.
   */
  private void readObject(ObjectInputStream s)                   /* @F003691A*/
    throws java.io.IOException,
           java.lang.ClassNotFoundException
  {
    ObjectInputStream.GetField getField = s.readFields();
 
    _argeEye = (String)getField.get("_argeEye", new String("null"));
    _argeVersion = (Short)getField.get("_argeVersion", new Short((short)-1));
    _argeSize = (Integer)getField.get("_argeSize", new Integer(-1));
    _argeFlagDaemon = (Boolean)getField.get("_argeFlagDaemon", 
                                            new Boolean(false));
    _argeFlagServer = (Boolean)getField.get("_argeFlagServer", 
                                            new Boolean(false));
    _argeFlagExternalAddress = 
      (Boolean)getField.get("_argeFlagExternalAddress", new Boolean(false));
    _argeFlagSecurity = (Boolean)getField.get("_argeFlagSecurity", 
                                              new Boolean(false));
    _argeFlagWLM = (Boolean)getField.get("_argeFlagWLM", new Boolean(false));
    _argeFlagTransaction = (Boolean)getField.get("_argeFlagTransaction", 
                                                 new Boolean(false));
    _argeFlagActive = (Boolean)getField.get("_argeFlagActive", 
                                            new Boolean(false));
    _argeName = (String)getField.get("_argeName", new String("UNKNOWN"));
    _argeJobName = (String)getField.get("_argeJobName", new String("UNKNOWN"));
    _argeJobNum = (Integer)getField.get("_argeJobNum", new Integer(-1));
    _argeUUID = (String)getField.get("_argeUUID", new String("UNKNOWN"));
    _argeConnectionsMin = (Integer)getField.get("_argeConnectionsMin", 
                                                new Integer(-1));
    _argeConnectionsMax = (Integer)getField.get("_argeConnectionsMax", 
                                                new Integer(-1));
    _argeActiveConnectionCount = 
      (Long)getField.get("_argeActiveConnectionCount", new Long(-1L));
    _argeTraceLevel = (Integer)getField.get("_argeTraceLevel", 
                                            new Integer(-1));
    _argeState = (Long)getField.get("_argeState", new Long(-1L));
    _connectionHandles = 
      (List<OLAConnectionHandle>)getField.get("_connectionHandles", 
                                              new LinkedList<OLAConnectionHandle>());
  }
}
