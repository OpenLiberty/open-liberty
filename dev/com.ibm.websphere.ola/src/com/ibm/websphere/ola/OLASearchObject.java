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

/**
 * Object used to manage search criteria when searching for registrations
 * using the Optimized Local Adapters MBean.
 */
public class OLASearchObject implements Serializable             /* @F003691C*/
{
  /**
   * Serialization key
 */
	private static final long serialVersionUID = 8150275233315429404L;

	/**
   * Fields that we are serializing
	 */
  private static ObjectStreamField[] serialPersistentFields =    /* @F003691A*/
    new ObjectStreamField[]                                      /* @F003691A*/
    {                                                            /* @F003691A*/
      new ObjectStreamField("_name", String.class),              /* @F003691A*/
      new ObjectStreamField("_jobNum", String.class),            /* @F003691A*/
      new ObjectStreamField("_uuid", String.class),              /* @F003691A*/
	    new ObjectStreamField("_jobName", String.class),           /* @F003691A*/
	    new ObjectStreamField("_flagDaemonType", Boolean.class),   /* @F003691A*/
	    new ObjectStreamField("_flagServerType", Boolean.class),   /* @F003691A*/
	    new ObjectStreamField("_flagExternalAddressType", Boolean.class), /* @F003691A*/
      new ObjectStreamField("_flagSecurity", Boolean.class),     /* @F003691A*/
      new ObjectStreamField("_flagWLM", Boolean.class),          /* @F003691A*/
      new ObjectStreamField("_flagTransactionSupport", Boolean.class), /* @F003691A*/
	    new ObjectStreamField("_flagActive", Boolean.class),       /* @F003691A*/
      new ObjectStreamField("_minConnections", Integer.class),   /* @F003691A*/
      new ObjectStreamField("_minConnectionsOperator", String.class), /* @F003691A*/
      new ObjectStreamField("_maxConnections", Integer.class),   /* @F003691A*/
      new ObjectStreamField("_maxConnectionsOperator", String.class), /* @F003691A*/
      new ObjectStreamField("_currentActiveConnectionCount", Integer.class), /* @F003691A*/
      new ObjectStreamField("_currentActiveConnectionCountOperator", String.class), /* @F003691A*/
      new ObjectStreamField("_state", Integer.class),            /* @F003691A*/
      new ObjectStreamField("_stateOperator", String.class),     /* @F003691A*/
      new ObjectStreamField("_version", Short.class),            /* @F003691A*/
      new ObjectStreamField("_versionOperator", String.class),   /* @F003691A*/
      new ObjectStreamField("_serverRGEAddress", Long.class),    /* @F003691A*/
	    new ObjectStreamField("_serverRGEAddressOperator", String.class) /* @F003691A*/
    };                                                           /* @F003691A*/
  
	private String _name = null;                                   /* @F003691C*/
	private String _jobNum = null;                                 /* @F003691C*/
	private String _uuid = null;                                   /* @F003691C*/
	private String _jobName = null;                                /* @F003691C*/
	private Boolean _flagDaemonType = null;                        /* @F003691C*/
	private Boolean _flagServerType = null;                        /* @F003691C*/
	private Boolean _flagExternalAddressType = null;               /* @F003691C*/
	private Boolean _flagSecurity = null;                          /* @F003691C*/
	private Boolean _flagWLM = null;                               /* @F003691C*/
	private Boolean _flagTransactionSupport = null;                /* @F003691C*/
	private Boolean _flagActive = null;                            /* @F003691C*/
	private Integer _minConnections = null;                        /* @F003691C*/
	private String _minConnectionsOperator = null;                 /* @F003691C*/
	private Integer _maxConnections = null;                        /* @F003691C*/
	private String _maxConnectionsOperator = null;                 /* @F003691C*/
	private Integer _currentActiveConnectionCount = null;          /* @F003691C*/
	private String _currentActiveConnectionCountOperator = null;   /* @F003691C*/
	private Integer _state = null;                                 /* @F003691C*/
	private String _stateOperator = null;                          /* @F003691C*/
	private Short _version = null;                                 /* @F003691C*/
	private String _versionOperator = null;                        /* @F003691C*/
	private Long _serverRGEAddress = null;                         /* @F003691C*/
	private String _serverRGEAddressOperator = null;               /* @F003691C*/

	private transient OLARGE _nextServerRGE = null;                /* @F003691C*/
	private transient OLARGE _headOfConnectionPoolRGE = null;      /* @F003691C*/

	/**
   * Default constructor
	 */
	public OLASearchObject() {
		super();
	}

	/**
   * Returns the registration name that will be used to search registrations.
   * If null is returned, the registration name is not used to match
   * registrations.
	 * @return the registration name used to search for registrations.
	 */
	public String get_name() {
		return _name;
	}

	/**
   * Supplies the registration name that will be used to search registrations.
	 * @param name the registration name used to search for registrations.
	 */
	public void set_name(String name) {
		_name = name;
	}

	/**
   * Returns the job name that will be used to search registrations.  Any
   * registrations which were created by this job name will be matched.
   * If null is returned, the job name is not used to match registrations.
	 * @return the job name used by the search object
	 */
	public String get_jobName() {
		return _jobName;
	}

	/**
   * Sets the job name that will be used to search registrations.  Any
   * registrations which were created by this job name will be matched.
	 * @param jobName the job name used by the search object
	 */
	public void set_jobName(String jobName) {
		_jobName = jobName;
	}

	/**
   * Returns the job number that will be used to search registrations.  Any
   * registrations created by this job number will be matched.  If null is
   * returned, the job number is not used to match registrations.
	 * @return the job number used by the search object
	 */
	public String get_jobNum() {
		return _jobNum;
	}

	/**
   * Sets the job number that will be used to search registrations.  Any
   * registrations created by this job number will be matched.  Note that the
   * job number is supplied as a string.
	 * @param jobNum the job number used by the search object
	 */
	public void set_jobNum(String jobNum) {
		_jobNum = jobNum;
	}

	/**
   * Returns the uuid that will be used to search registered servers.  Any
   * WebSphere Application Server for z/OS server instance using this uuid
   * will be matched.  If null is returned, the uuid is not used to match
   * registrations.
	 * @return the uuid used by the search object.
	 */
	public String get_uuid() {
		return _uuid;
	}

	/**
   * Sets the uuid that will be used to search registered servers.  Any
   * WebSphere Application Server for z/OS server instance using this uuid
   * will be matched.
	 * @param uuid the uuid used by the search object
	 */
	public void set_uuid(String uuid) {
		_uuid = uuid;
	}

	/**
   * Returns the value of the daemon registration flag used by the search
   * object.  If the flag is set to true, any registrations representing
   * WebSphere Application Server for z/OS daemon processes will be returned.
   * If this method returns null, this flag is not used to match
   * registrations.
	 * @return the daemon flag value
	 */
	public Boolean get_flagDaemonType() {
		return _flagDaemonType;
	}

	/**
   * Sets the value of the daemon registration flag used by the search object.
   * If the flag is set to true, any registrations representing WebSphere
   * Application Server for z/OS daemon processes will be returned.
	 * @param daemonType the value of the daemon flag variable
	 */
	public void set_flagDaemonType(Boolean daemonType) {
		_flagDaemonType = daemonType;
	}

	/**
   * Returns the value of the server registration flag used by the search
   * object.  If the flag is set to true, any registrations representing
   * WebSphere Application Server for z/OS server processes will be returned.
   * If this method returns null, this flag is not used to match
   * registrations.
	 * @return the server flag value
	 */
	public Boolean get_flagServerType() {
		return _flagServerType;
	}

	/**
   * Sets the value of the server registration flag used by the search object.
   * If the flag is set to true, any registrations representing WebSphere
   * Application Server for z/OS server processes will be returned.
	 * @param serverType the value of the server flag variable
	 */
	public void set_flagServerType(Boolean serverType) {
		_flagServerType = serverType;
	}

	/**
   * Returns the value of the external address space flag used by the
   * search object.  If the flag is set to true, any registration representing
   * client registrations will be returned.  These are registrations created
   * by an address space using the BBOA1REG API.  If this method returns
   * null, the external address space flag is not used to match registrations.
	 * @return the value of the external address space flag
	 */
	public Boolean get_flagExternalAddressType() {
		return _flagExternalAddressType;
	}

	/**
   * Sets the value of the external address space flag used by the search
   * object.  If the flag is set to true, any registrations representing client
   * registrations will be returned.  These are registrations created by an
   * address space using the BBOA1REG API.
	 * @param externalAddressType the value of the external address space flag
	 */
	public void set_flagExternalAddressType(Boolean externalAddressType) {
		_flagExternalAddressType = externalAddressType;
	}

	/**
   * Returns the value of the security flag used by the search object.  If the
   * flag is set to true, any registrations made with security enabled on the
   * BBOA1REG call will be returned.  If this method returns null, the
   * security flag is not used to match registrations.
	 * @return the value of the security flag
	 */
	public Boolean get_flagSecurity() {
		return _flagSecurity;
	}

	/**
   * Sets the value of the security flag used by the search object.  If the
   * flag is set to true, any registrations made with security enabled on the
   * BBOA1REG call will be returned.
	 * @param security the the value of the security flag to set
	 */
	public void set_flagSecurity(Boolean security) {
		_flagSecurity = security;
	}

	/**
   * Returns the value of the transaction flag used by the search object.  If
   * the flag is set to true, any registrations specifying transactions on
   * the BBOA1REG call will be returned.  If null is returned, the transaction
   * flag is not used to match registrations.
	 * @return the value of the transaction flag
	 */
	public Boolean get_flagTransactionSupport() {
		return _flagTransactionSupport;
	}

	/**
   * Sets the value of the transaction flag used by the search object.  If
   * the flag is set to true, any registrations specifying transactions on
   * the BBOA1REG call will be returned.
	 * @param transactionSupport the value of the transaction flag
	 */
	public void set_flagTransactionSupport(Boolean transactionSupport) {
		_flagTransactionSupport = transactionSupport;
	}

	/**
   * Returns the value of the active flag used by the search object.  If
   * the flag is set to true, only active registrations will be returned.
   * All registrations are considered active unless they have been
   * unregistered with the BBOA1URG API, or the address space performing the
   * registration terminates.  If null is returned, the active flag is not
   * used to match registrations.
	 * @return the value of the active flag
	 */
	public Boolean get_flagActive() {
		return _flagActive;
	}

	/**
   * Sets the value of the active flag used by the search object.  If the flag
   * is set to true, only active registrations will be returned.  All
   * registrations are considered active unless they have been unregistered
   * with the BBOA1URG API, or the address space performing the registration
   * terminates.  No guarantees are made as to the behavior when returning
   * inactive registrations, as the control blocks used to store the
   * registration information are deleted during the unregister process.
	 * @param active the value of the active flag
	 */
	public void set_flagActive(Boolean active) {
		_flagActive = active;
	}

	/**
   * Returns the number of minimum connections used by the search object.  If 
   * this value is set, it is used along with the minConnectionsOperator 
   * to build a list of matching registrations.  If null is returned, the
   * minimum number of connections is not used to match registrations.
	 * @return the value of minConnections
	 */
	public Integer get_minConnections() {
		return _minConnections;
	}

	/**
   * Sets the number of minimum connections used by the search object.  This
   * value is used along with the minConnectionsOperator to build a list of
   * matching registrations.
	 * @param connections the value of minimum connections
	 */
	public void set_minConnections(Integer connections) {
		_minConnections = connections;
	}

	/**
   * Returns the value of the minimum connections operator.  The operator
   * is used along with the value of minConnections to build a list of
   * matching registrations.  If null is returned, an operator of "=" is
   * implied.
	 * @return the minConnections operator
	 */
	public String get_minConnectionsOperator() {
		return _minConnectionsOperator;
	}

	/**
   * Sets the value of the minimum connections operator.  The operator is used
   * along with the value of minConnections to build a list of matching
   * registrations.  The operator can be one of the following values:  =, >,
   * <, >= or <=.  The registration's value for minConnections is used to
   * build the following conditional expression:  registrationValue operator 
   * minConnectionsValue.  If this expression is true, the registration
   * matches and is added to the list of matching registrations.
	 * @param connectionsOperator the minConnectionsOperator to set
	 */
	public void set_minConnectionsOperator(String connectionsOperator) {
		_minConnectionsOperator = connectionsOperator;
	}

	/**
   * Returns the number of maximum connections used by the search object.  If 
   * this value is set, it is used along with the maxConnectionsOperator 
   * to build a list of matching registrations.  If null is returned, the
   * maximum number of connections is not used to match registrations.
	 * @return the value of maxConnections
	 */
	public Integer get_maxConnections() {
		return _maxConnections;
	}

	/**
   * Sets the number of maximum connections used by the search object.  This
   * value is used along with the maxConnectionsOperator to build a list of
   * matching registrations.
	 * @param connections the value of maximum connections
	 */
	public void set_maxConnections(Integer connections) {
		_maxConnections = connections;
	}

	/**
   * Returns the value of the maximum connections operator.  The operator
   * is used along with the value of maxConnections to build a list of
   * matching registrations.  If null is returned, an operator of "=" is
   * implied.
	 * @return the maxConnections operator
	 */
	public String get_maxConnectionsOperator() {
		return _maxConnectionsOperator;
	}

	/**
   * Sets the value of the maximum connections operator.  The operator is used
   * along with the value of maxConnections to build a list of matching
   * registrations.  The operator can be one of the following values:  =, >,
   * <, >= or <=.  The registration's value for maxConnections is used to
   * build the following conditional expression:  registrationValue operator 
   * maxConnectionsValue.  If this expression is true, the registration
   * matches and is added to the list of matching registrations.
	 * @param connectionsOperator the maxConnectionsOperator to set
	 */
	public void set_maxConnectionsOperator(String connectionsOperator) {
		_maxConnectionsOperator = connectionsOperator;
	}

	/**
   * Returns the number of active connections used by the search object.  If 
   * this value is set, it is used along with the 
   * currentActiveConnectionsOperator to build a list of matching 
   * registrations.  If null is returned, the number of active connections is 
   * not used to match registrations.
	 * @return the value of activeConnectionCount
	 */
	public Integer get_currentActiveConnectionCount() {
		return _currentActiveConnectionCount;
	}

	/**
   * Sets the number of active connections used by the search object.  This
   * value is used along with the currentActiveConnectionsOperator to build a 
   * list of matching registrations.
	 * @param activeConnectionCount the value of active connections
	 */
	public void set_currentActiveConnectionCount(Integer activeConnectionCount) {
		_currentActiveConnectionCount = activeConnectionCount;
	}

	/**
   * Returns the value of the active connections operator.  The operator
   * is used along with the value of currentActiveConnectionCount to build a 
   * list of matching registrations.  If null is returned, an operator of "=" 
   * is implied.
	 * @return the currentActiveConnectionCount operator
	 */
	public String get_currentActiveConnectionCountOperator() {        
		return _currentActiveConnectionCountOperator;
	}

	/**
   * Sets the value of the active connections operator.  The operator is used
   * along with the value of currentActiveConnectionCount to build a list of m
   * atching registrations.  The operator can be one of the following values:  
   * =, >, <, >= or <=.  The registration's value for 
   * currentActiveConnectionCount is used to build the following conditional 
   * expression:  registrationValue operator currentActiveConnectionCount.  
   * If this expression is true, the registration matches and is added to the 
   * list of matching registrations.
	 * @param activeConnectioncountOperator the currentActiveConnectionsOperator 
   *        to set
	 */
	public void set_currentActiveConnectionCountOperator(String activeConnectionCountOperator) {
		_currentActiveConnectionCountOperator = activeConnectionCountOperator;
	}

	/**
   * Returns the state value used by the search object.  If this value is set, 
   * it is used along with the stateOperator to build a list of matching 
   * registrations.  If null is returned, the state is not used to match 
   * registrations.
	 * @return the state used by the search object
	 */
	public Integer get_state() {
		return _state;
	}

	/**
   * Sets the state value used by the search object.  This value is used along 
   * with the stateOperator to build a list of matching registrations.
	 * @param state the state value used by the search object
	 */
	public void set_state(Integer state) {
		_state = state;
	}

	/**
   * Returns the value of the state operator.  The operator is used along with 
   * the value of state to build a list of matching registrations.  If null is
   * returned, an operator of "=" is implied.
	 * @return the state operator
	 */
	public String get_stateOperator() {
		return _stateOperator;
	}

	/**
   * Sets the value of the state operator.  The operator is used along with the
   * value of state to build a list of matching registrations.  
   * The operator can be one of the following values: =, >, <, >= or <=.  
   * The registration's value for state is used to build the following 
   * conditional expression:  registrationValue operator state.  
   * If this expression is true, the registration matches and is added to the 
   * list of matching registrations.
	 * @param stateOperator the stateOperator to set
	 */
	public void set_stateOperator(String stateOperator) {
		_stateOperator = stateOperator;
	}

	/**
   * Returns the version value used by the search object.  If this value is 
   * set, it is used along with the versionOperator to build a list of matching
   * registrations.  If null is returned, the version is not used to match 
   * registrations.
	 * @return the version used by the search object
	 */
	public Short get_version() {
		return _version;
	}

	/**
   * Sets the version value used by the search object.  This value is used 
   * along with the versionOperator to build a list of matching registrations.
	 * @param _version the version value used by the search object
	 */
	public void set_version(Short _version) {
		this._version = _version;
	}

	/**
   * Returns the value of the version operator.  The operator is used along 
   * with the value of version to build a list of matching registrations.  
   * If null is returned, an operator of "=" is implied.
	 * @return the version operator
	 */
	public String get_versionOperator() {
		return _versionOperator;
	}

	/**
   * Sets the value of the version operator.  The operator is used along with 
   * the value of version to build a list of matching registrations.  
   * The operator can be one of the following values: =, >, <, >= or <=.  
   * The registration's value for version is used to build the following 
   * conditional expression:  registrationValue operator version.  
   * If this expression is true, the registration matches and is added to the 
   * list of matching registrations.
	 * @param operator the versionOperator to set
	 */
	public void set_versionOperator(String operator) {
		_versionOperator = operator;
	}

	/**
   * Returns the server RGE address used by the search object.  If this value 
   * is set, it is used along with the serverRGEAddressOperator to build a list
   * of matching registrations.  If null is returned, the server RGE address is
   * not used to match registrations.
	 * @return the server RGE address used by the search object
	 */
	public Long get_serverRGEAddress() {
		return _serverRGEAddress;
	}

	/**
   * Sets the server RGE address used by the search object.  This value is used
   * along with the serverRGEAddressOperator to build a list of matching 
   * registrations.  The server RGE address can be located with help from IBM
   * Support to help diagnose certain kinds of problems with the Optimized
   * Local Adapter.
	 * @param _serverrge The server RGE address
	 */
	public void set_serverRGEAddress(Long _serverrge) {
		_serverRGEAddress = _serverrge;
	}

	/**
   * Returns the value of the server RGE address operator.  The operator is 
   * used along with the server RGE address to build a list of matching 
   * registrations.  If null is returned, an operator of "=" is implied.
	 * @return the server RGE address operator
	 */
  public String get_serverRGEAddressOperator()                   /* @F003691A*/
  {
    return _serverRGEAddressOperator;
  }

	/**
   * Sets the value of the server RGE address operator.  The operator is used 
   * along with the value of server RGE address to build a list of matching 
   * registrations.  The operator can be one of the following values: =, >, <, 
   * >= or <=.  The registration's value for server RGE address is used to 
   * build the following conditional expression:  registrationValue operator 
   * version.  If this expression is true, the registration matches and is 
   * added to the list of matching registrations.
	 * @param operator the serverRGEAddressOperator to set
	 */
  public void set_serverRGEAddressOperator(String operator)      /* @F003691A*/
  {
    _serverRGEAddressOperator = operator;
  }


	/**
	 * toString method. Displays options all on the same line.
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("OLASearchObject@");
		sb.append(System.identityHashCode(this));		
		
		sb.append(", UUID = ");
		sb.append(this.get_uuid());
		
		sb.append(", Job Name = ");
		sb.append(this.get_jobName());
		
		sb.append(", Job Number = ");			
		sb.append(this.get_jobNum());
		
		sb.append(", serverRGEAddress = ");			
		sb.append(this.get_serverRGEAddress());
		
		sb.append(", flag_daemon = ");			
		sb.append(this.get_flagDaemonType());
		
		sb.append(", flag_server = ");			
		sb.append(this.get_flagServerType());
		
		sb.append(", flag_external_address_space = ");			
		sb.append(this.get_flagExternalAddressType());
		
		sb.append(", flag_active = ");			
		sb.append(this.get_flagActive());
		
		sb.append(", flag_transactions = ");			
		sb.append(this.get_flagTransactionSupport());
	
		sb.append(", connections min = ");			
		sb.append(this.get_minConnections());
	
		sb.append(", connections max = ");			
		sb.append(this.get_maxConnections());
		
		return sb.toString();
	}

  /**
   * Serialization support for writing this object.
   */
  private void writeObject(ObjectOutputStream s)                 /* @F003691A*/
    throws java.io.IOException
  {
    ObjectOutputStream.PutField putField = s.putFields();
    putField.put("_name", _name);
	  putField.put("_jobNum", _jobNum);
	  putField.put("_uuid", _uuid);
	  putField.put("_jobName", _jobName);
    putField.put("_flagDaemonType", _flagDaemonType);
    putField.put("_flagServerType", _flagServerType);
    putField.put("_flagExternalAddressType", _flagExternalAddressType);
    putField.put("_flagSecurity", _flagSecurity);
    putField.put("_flagWLM", _flagWLM);
    putField.put("_flagTransactionSupport", _flagTransactionSupport);
    putField.put("_flagActive", _flagActive);
    putField.put("_minConnections", _minConnections);
    putField.put("_minConnectionsOperator", _minConnectionsOperator);
    putField.put("_maxConnections", _maxConnections);
    putField.put("_maxConnectionsOperator", _maxConnectionsOperator);
    putField.put("_currentActiveConnectionCount", 
                 _currentActiveConnectionCount);
    putField.put("_currentActiveConnectionCountOperator",
                 _currentActiveConnectionCountOperator);
    putField.put("_state", _state);
    putField.put("_stateOperator", _stateOperator);
    putField.put("_version", _version);
    putField.put("_versionOperator", _versionOperator);
    putField.put("_serverRGEAddress", _serverRGEAddress);
    putField.put("_serverRGEAddressOperator", _serverRGEAddressOperator);
    s.writeFields();
  }

  /**
   * Serialization support for reading this object.
   */
  private void readObject(ObjectInputStream s)                   /* @F003691A*/
    throws java.io.IOException,
           java.lang.ClassNotFoundException
  {
    ObjectInputStream.GetField getField = s.readFields();

    _name = (String)getField.get("_name", null);
    _jobNum = (String)getField.get("_jobNum", null);
    _uuid = (String)getField.get("_uuid", null);
    _jobName = (String)getField.get("_jobName", null);
    _flagDaemonType = (Boolean)getField.get("_flagDaemonType", null);
    _flagServerType = (Boolean)getField.get("_flagServerType", null);
    _flagExternalAddressType = 
      (Boolean)getField.get("_flagExternalAddressType", null);
    _flagSecurity = (Boolean)getField.get("_flagSecurity", null);
    _flagWLM = (Boolean)getField.get("_flagWLM", null);
    _flagTransactionSupport = 
      (Boolean)getField.get("_flagTransactionSupport", null);
    _flagActive = (Boolean)getField.get("_flagActive", null);
    _minConnections = (Integer)getField.get("_minConnections", null);
    _minConnectionsOperator = 
      (String)getField.get("_minConnectionsOperator", null);
    _maxConnections = (Integer)getField.get("_maxConnections", null);
    _maxConnectionsOperator =
      (String)getField.get("_maxConnectionsOperator", null);
    _currentActiveConnectionCount = 
      (Integer)getField.get("_currentActiveConnectionCount", null);
    _currentActiveConnectionCountOperator =
      (String)getField.get("_currentActiveConnectionCountOperator", null);
    _state = (Integer)getField.get("_state", null);
    _stateOperator = (String)getField.get("_stateOperator", null);
    _version = (Short)getField.get("_version", null);
    _versionOperator = (String)getField.get("_versionOperator", null);
    _serverRGEAddress = (Long)getField.get("_serverRGEAddress", null);
    _serverRGEAddressOperator = 
      (String)getField.get("_serverRGEAddressOperator", null);
  }
}