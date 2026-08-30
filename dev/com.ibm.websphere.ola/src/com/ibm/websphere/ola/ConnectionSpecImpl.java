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

import javax.resource.cci.ConnectionSpec;

import com.ibm.websphere.ras.annotation.Sensitive;

import java.io.ObjectInputStream;                                /* @F003705A*/
import java.io.ObjectOutputStream;                               /* @F003705A*/
import java.io.ObjectStreamField;                                /* @F003705A*/
import java.io.Serializable;                                     /* @F003705A*/

/**
 * The ConnectionSpec is used to pass resource-specific data to the resource for
 * purposes of establishing the connection.
 *
 * @ibm-api
 */
public class ConnectionSpecImpl 
  implements ConnectionSpec, Serializable                        /* @F003705C*/
{
  static final long serialVersionUID = 4205477718188402899L;     /* @F003705A*/

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
   * Use a BIT container
   */
  public static final int BIT_CONTAINER = 1; /* @578669C*/


  /**
   * Use a CHAR container
   */
  public static final int CHAR_CONTAINER = 0; /* @578669C*/
  
  /**
   * Use a BIT channel
   */
  public static final int BIT_CHANNEL = 1;    /* @F014448A */

  /**
   * Use a CHAR channel
   */
  public static final int CHAR_CHANNEL = 0;   /* @F014448A */

  /**
   * Default OTMA Client Name 
   */
  public static final String DEFAULT_OTMA_CLIENT_NAME = "DEFAULT";
  
  /**
   * Fields for serialization
   */
  private final static ObjectStreamField[] serialPersistentFields =
    new ObjectStreamField[]                                      /* @F003705A*/
    {
      new ObjectStreamField("_version", Integer.TYPE),
      new ObjectStreamField("_registerName", String.class),
      new ObjectStreamField("_connectionWaitTimeout", Integer.TYPE),
      new ObjectStreamField("_connectionWaitTimeoutFromCSI", Boolean.TYPE),
      new ObjectStreamField("_linkTaskTranID", String.class),
      new ObjectStreamField("_linkTaskReqContID", String.class),
      new ObjectStreamField("_linkTaskReqContType", Integer.TYPE),
      new ObjectStreamField("_linkTaskReqContTypeFromCSI", Boolean.TYPE),
      new ObjectStreamField("_linkTaskRspContID", String.class),
      new ObjectStreamField("_linkTaskRspContType", Integer.TYPE),
      new ObjectStreamField("_linkTaskRspContTypeFromCSI", Boolean.TYPE),
      new ObjectStreamField("_useCICSContainer", Boolean.TYPE),
      new ObjectStreamField("_useCICSContainerFromCSI", Boolean.TYPE),
	  new ObjectStreamField("_linkTaskChanID", String.class),                /* @F014448A */
	  new ObjectStreamField("_linkTaskChanType", Integer.TYPE),              /* @F014448A */
	  new ObjectStreamField("_linkTaskChanTypeFromCSI", Boolean.TYPE),       /* @F014448A */
      new ObjectStreamField("_useOTMA", Boolean.TYPE),
      new ObjectStreamField("_OTMAClientName", String.class),
      new ObjectStreamField("_OTMAServerName", String.class),
      new ObjectStreamField("_OTMAGroupID", String.class),
      new ObjectStreamField("_OTMASyncLevel", String.class),
      new ObjectStreamField("_OTMAMaxSegments", Integer.TYPE),
      new ObjectStreamField("_OTMAMaxSegmentsFromCSI", Boolean.TYPE),
      new ObjectStreamField("_OTMAMaxRecvSize", Integer.TYPE),
      new ObjectStreamField("_OTMAMaxRecvSizeFromCSI", Boolean.TYPE),
      new ObjectStreamField("_reqLLZZ", Boolean.TYPE),
      new ObjectStreamField("_reqLLZZFromCSI", Boolean.TYPE),
      new ObjectStreamField("_respLLZZ", Boolean.TYPE),
      new ObjectStreamField("_respLLZZFromCSI", Boolean.TYPE),
      new ObjectStreamField("_username", String.class),
      new ObjectStreamField("_password", String.class),
      new ObjectStreamField("_RRSTransactional", Boolean.TYPE),
      new ObjectStreamField("_RRSTransactionalFromCSI", Boolean.TYPE)
    };

  /**
   * This object version.  
   */
  private int _version = CURRENT_VERSION;                        /* @F003705A*/
  
	
	/**
	 * Place to store the register name.
	 */
	private String _registerName = null;

	/**
	 * Sets the register name.
   * @param registerName The register name to connect to.
	 */
	public void setRegisterName(String registerName)
	{
		_registerName = registerName;
	}
	
	/**
	 * Gets the register name to connect to.
   * @return The register name.
	 */
	public String getRegisterName()
	{
		return _registerName;
	}
	
	/**
	 * Connection timeout - defaults to 30 seconds
	 */
	private int _connectionWaitTimeout = 30;
	private boolean _connectionWaitTimeoutFromCSI = false;    /* @F013381A*/
  	
	/**
	 * Sets the connection wait timeout value.  This is the amount of time
   * to wait for a connection to become available to the target register
   * name.
   * @param connectionWaitTimeout The number of seconds to wait for a
   *        connection
	 */
	public void setConnectionWaitTimeout(int connectionWaitTimeout)
	{
		_connectionWaitTimeout = connectionWaitTimeout;
		_connectionWaitTimeoutFromCSI = true;                  /* @F013381A*/
	}
	
	/**
	 * Gets the connection wait timeout value.
   * @return The timeout value, in seconds.
	 */
	public int getConnectionWaitTimeout()
	{
		return _connectionWaitTimeout;
	}
	
	/**
	 * Gets the flag indicating set connection wait timeout was called.
     * @return True or False
     */
	public boolean getConnectionWaitTimeoutFromCSI()
	{
		return _connectionWaitTimeoutFromCSI;
	}

	/**
	 * CICS Link task Transaction id
	 */
	private String _linkTaskTranID = null;                    /* @F013381A*/
	
	/**
	 * Sets the CICS Link Transaction ID.  This is the CICS transaction which
   * will be used to run the Program Link invocation task.  If this value
   * is not set, it will default to BBO#.
   * This method only applies to applications calling into the CICS link server.
   * @param linkTaskTranID The transaction to use as the Program Link invocation
   *        task.  The transaction name is between 1 and 4 characters in length.
	 */
	public void setLinkTaskTranID(String linkTaskTranID)
	{
		_linkTaskTranID = linkTaskTranID;
	}
	
	/**
	 * Gets the CICS Link Transaction ID set by setLinkTaskTranID.
   * This method only applies to applications calling into the CICS link server.
   * @return The CICS transaction name used to run the Program Link invocation task.
	 */
	public String getLinkTaskTranID()
	{
		return _linkTaskTranID;
	}
	
	/**
	 * CICS Link task Request Container ID
	 */
	private String _linkTaskReqContID = null;                  /* @F013381A*/
	
	/**
	 * Sets the CICS Link Request Container ID.
   * This method only applies to applications calling into the CICS link server.
   * @param linkTaskReqContID The name of the container to pass the request
   *        parameters into.
	 */
	public void setLinkTaskReqContID(String linkTaskReqContID)
	{
		_linkTaskReqContID = linkTaskReqContID;
	}
	
	/**
	 * Gets the CICS Link Request Container ID set by linkTaskReqContID()
   * This method only applies to applications calling into the CICS link server.
   * @return The name of the container to pass the request parameters into.
	 */
	public String getLinkTaskReqContID()
	{
		return _linkTaskReqContID;
	}
	
	/**
	 * CICS Link task Request Container Type
	 */
	private int _linkTaskReqContType = 0;
	private boolean _linkTaskReqContTypeFromCSI = false;     /* @F013381A*/
	
	/**
	 * Sets the CICS Link Request Container Type.  The type can be CHAR or BIT.  To
   * specify CHAR, use the value 0.  To specify BIT, use the value 1.  You can also
   * specify the constants CHAR_CONTAINER or BIT_CONTAINER defined in this class.
   * If no value is specified, the default is to use a CHAR container.
   * This method only applies to applications calling into the CICS link server.
   * @param linkTaskReqContType The type of container to use.  Specify 0 to use
   *        a CHAR container, or 1 to use a BIT container.
	 */
	public void setLinkTaskReqContType(int linkTaskReqContType)
	{
		_linkTaskReqContType = linkTaskReqContType;
		_linkTaskReqContTypeFromCSI = true;                       /* @F013381A*/
	}
	
	/**
	 * Gets the CICS Link Request Container Type set by linkTaskReqContType()
   * This method only applies to applications calling into the CICS link server.
   * @return The type of container to be used.
	 */
	public int getLinkTaskReqContType()
	{
		return _linkTaskReqContType;
	}

	/**
	 * Gets the flag indicating set LinkTaskReqContType was called.
     * @return True or False
     */
	public boolean getlinkTaskReqContTypeFromCSI()
	{
		return _linkTaskReqContTypeFromCSI;
	}
	

	/**
	 * CICS Link task Response Container ID
	 */
	private String _linkTaskRspContID = null;                 /* @F013381A*/
	
	/**
	 * Sets the CICS Link Response Container ID.
   * This method only applies to applications calling into the CICS link server.
   * @param linkTaskReqContID The name of the container to pass the response
   *        parameters into.
	 */
	public void setLinkTaskRspContID(String linkTaskRspContID)
	{
		_linkTaskRspContID = linkTaskRspContID;
	}
	
	/**
	 * Gets the CICS Link Response Container ID set by linkTaskReqContID()
   * This method only applies to applications calling into the CICS link server.
   * @return The name of the container to pass the response parameters into.
	 */
	public String getLinkTaskRspContID()
	{
		return _linkTaskRspContID;
	}
	
	/**
	 * CICS Link task Response Container Type - default null = CHAR
	 */
	private int _linkTaskRspContType = 0;
	private boolean _linkTaskRspContTypeFromCSI = false;     /* @F013381A*/
	
	/**
	 * Sets the CICS Link Response Container Type.  The type can be CHAR or BIT.  To
   * specify CHAR, use the value 0.  To specify BIT, use the value 1.  You can also
   * specify the constants CHAR_CONTAINER or BIT_CONTAINER defined in this class.
   * If no value is specified, the default is to use a CHAR container.
   * This method only applies to applications calling into the CICS link server.
   * @param linkTaskRspContType The type of container to use.  Specify 0 to use
   *        a CHAR container, or 1 to use a BIT container.
	 */
	public void setLinkTaskRspContType(int linkTaskRspContType)
	{
		_linkTaskRspContType = linkTaskRspContType;
		_linkTaskRspContTypeFromCSI = true;                       /* @F013381A*/
	}
	
	/**
	 * Gets the CICS Link Response Container Type set by linkTaskRspContType()
   * This method only applies to applications calling into the CICS link server.
   * @return The type of container to be used.
	 */
	public int getLinkTaskRspContType()
	{
		return _linkTaskRspContType;
	}

	/**
	 * Gets the flag indicating set LinkTaskRspContType was called.
     * @return True or False
     */
	public boolean getlinkTaskRspContTypeFromCSI()
	{
		return _linkTaskRspContTypeFromCSI;
	}

	/**
	 * Use CICS Containers - defaults to false = No
	 */
	private boolean _useCICSContainer = false; /* @578669C*/
	private boolean _useCICSContainerFromCSI = false;     /* @F013381A*/

	/**
	 * Tells the connection to use the containers when communicating with CICS.
   * The names and types of containers used for the request and response can
   * be set by calling other methods on this class.
   * This method only applies to applications calling into the CICS link server.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskReqContID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskRspContID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskReqContType(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskRspContType(int)
   * @deprecated Use {@link #setUseCICSContainer(boolean)}
   * @param useCICSContainer Set this value to '1' to use containers when
   *        communicating with the CICS Link Server.
	 */
	public void setUseCICSContainer(int useCICSContainer)
	{
    if (useCICSContainer == 0) /* @578669C*/
      setUseCICSContainer(false);
    else
      setUseCICSContainer(true);
	}
	
	/**
	 * Tells the connection to use containers when communicating with CICS.
   * The names and types of containers used for the request and response can
   * be set by calling other methods on this class.
   * This method only applies to applications calling into the CICS link server.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskReqContID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskRspContID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskReqContType(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setLinkTaskRspContType(int)
   * @param useCICSContainer Set this value to true to use containers when
   *        communicating with the CICS Link Server.
	 */
	public void setUseCICSContainer(boolean useCICSContainer) /* @578669A*/
	{
		_useCICSContainer = useCICSContainer;
		_useCICSContainerFromCSI = true;                     /* @F013381A*/
	}
  

	/**
	 * Gets the value set by setUseCICSContainer.
   * @return true if containers are used, false if not.
	 */
  public boolean getUseCICSContainer() /* @578669C*/
  {
    return _useCICSContainer;
  }

	/**
	 * Gets the flag indicating set useCICSContainers was called.
 * @return True or False
 */
	public boolean getuseCICSContainerFromCSI()
	{
		return _useCICSContainerFromCSI;
	}
	
	/**
	 * CICS Link task Channel ID
	 */
	private String _linkTaskChanID = null;                                            /* @F014448A */
	
	/**
	 * Sets the CICS Link Channel ID.
     * This method only applies to applications calling into the CICS link server.
     * @param linkTaskChanID The name of the channel to pass the request
     *        parameters into.
	 */
	public void setLinkTaskChanID(String linkTaskChanID)                              /* @F014448A */
	{                                                                                 /* @F014448A */
		_linkTaskChanID = linkTaskChanID;                                             /* @F014448A */
	}                                                                                 /* @F014448A */
	
	/**
	 * Gets the CICS Link Channel ID set by linkTaskChanID()
     * This method only applies to applications calling into the CICS link server.
     * @return The name of the channel to pass the request parameters into.
	 */
	public String getLinkTaskChanID()                                                 /* @F014448A */
	{                                                                                 /* @F014448A */
		return _linkTaskChanID;                                                       /* @F014448A */
	}                                                                                 /* @F014448A */
	
	/**
	 * CICS Link task Channel Type
	 */
	private int _linkTaskChanType = 0;                                                /* @F014448A */
	private boolean _linkTaskChanTypeFromCSI = false;                                 /* @F014448A */
	
	/**
	 * Sets the CICS Link Channel Type.  The type can be CHAR or BIT.  To
     * specify CHAR, use the value 0.  To specify BIT, use the value 1.  You can also
     * specify the constants CHAR_CHANNEL or BIT_CHANNEL defined in this class.
     * If no value is specified, the default is to use a CHAR channel.
     * This method only applies to applications calling into the CICS link server.
     * @param linkTaskChanType The type of channel to use.  Specify 0 to use
     *        a CHAR channel, or 1 to use a BIT channel.
	 */
	public void setLinkTaskChanType(int linkTaskChanType)                             /* @F014448A */
	{                                                                                 /* @F014448A */
		_linkTaskChanType = linkTaskChanType;                                         /* @F014448A */
		_linkTaskChanTypeFromCSI = true;                                              /* @F014448A */
	}                                                                                 /* @F014448A */
	
	/**
	 * Gets the CICS Link Channel Type set by linkTaskChanType()
     * This method only applies to applications calling into the CICS link server.
     * @return The type of channel to be used.
	 */
	public int getLinkTaskChanType()                                                  /* @F014448A */
	{                                                                                 /* @F014448A */
		return _linkTaskChanType;                                                     /* @F014448A */
	}                                                                                 /* @F014448A */

	/**
	 * Gets the flag indicating set LinkTaskChanType was called.
     * @return True or False
     */
	public boolean getlinkTaskChanTypeFromCSI()                                       /* @F014448A */
	{                                                                                 /* @F014448A */
		return _linkTaskChanTypeFromCSI;                                              /* @F014448A */
	}                                                                                 /* @F014448A */
	
	/**
	 * Use IMS OTMA for call request - defaults to false = No
	 */
	private boolean _useOTMA = false; /* @F003694A*/
	
	/**
	 * Tells the connection to use the IMS OTMA access method for call requests 
   * This method only applies to applications calling into IMS programs.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
   * @param useOTMA Set this value to true to use OTMA when
   *        communicating with IMS.
	 */
	public void setUseOTMA(boolean useOTMA) /* @F003694A*/
	{
		_useOTMA = useOTMA;                 /* @F003694A*/
	}
  
	/**
	 * Gets the value set by setOTMA().
   * @return true if IMS OTMA is to be used, false if not.
	 */
  public boolean getUseOTMA()                  /* @F003694A*/
  {
    return _useOTMA;
  }
	
	/**
	 * IMS OTMA Client Name 
	 */
	private String _OTMAClientName = null;

	/**
     * Sets the OTMA Client Name. 
     * This method only applies to applications calling into IMS programs.
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMA(boolean)
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
     * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
     * @param OTMAClientName Set this value to the name of the OTMA Client
     *        to use when communicating with IMS.
     */
	public void setOTMAClientName(String OTMAClientName)
	{
		_OTMAClientName = OTMAClientName;
	}
  
	/**
     * Gets the value set by setOTMAClientName().
     * @return The name of the OTMA Client used for IMS OTMA calls.
     */
    public String getOTMAClientName()
    {
      return _OTMAClientName;
    }
	
	/**
	 * IMS OTMA Server Name 
	 */
	private String _OTMAServerName = null;     /* @F003694A*/
	
	/**
	 * Sets the OTMA Server Name. 
   * This method only applies to applications calling into IMS programs.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMA(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
   * @param OTMAServerName Set this value to the name of the OTMA Server
   *        to use when communicating with IMS.
	 */
	public void setOTMAServerName(String OTMAServerName)  /* @F003694A*/
	{
		_OTMAServerName = OTMAServerName;                 /* @F003694A*/
	}
  
	/**
	 * Gets the value set by setOTMAServerName().
   * @return The name of the OTMA Server used for IMS OTMA calls.
	 */
  public String getOTMAServerName()                     /* @F003694A*/
  {
    return _OTMAServerName;
  }
	
    /**
	 * IMS OTMA XCF Group ID Name 
	 */
	private String _OTMAGroupID = null;                  /* @F003694A*/

    /**
     * Sets the OTMA XCF Group ID Name. 
   * This method only applies to applications calling into IMS programs.
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMA(boolean)
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
   * @param OTMAGroupID Set this value to the name of the OTMA XCF Grouup ID
   *        to use when communicating with IMS.
   	*/
    public void setOTMAGroupID(String OTMAGroupID)  /* @F003694A*/
    {
    	_OTMAGroupID = OTMAGroupID;                 /* @F003694A*/
    }

    /**
     * Gets the value set by setOTMAGroupID().
   * @return The name of the OTMA XCF Group ID used for IMS OTMA calls.
   	*/
    public String getOTMAGroupID()                 /* @F003694A*/
    {
    	return _OTMAGroupID;
    }

	/**
	 * IMS OTMA Sync Level 0|1
	 */
	private String _OTMASyncLevel = null;      /* @F003694A*/

    /**
     * Sets the OTMA Sync Level 0=None | 1=Confirm 
   * This method only applies to applications calling into IMS programs.
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMA(boolean)
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
   	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
   * @param OTMASyncLevel Set this value to the request Sync Level 0|1
   *        to use when communicating with IMS.
   	*/
    public void setOTMASyncLevel(String OTMASyncLevel)  /* @F003694A*/
    {
    	_OTMASyncLevel = OTMASyncLevel;                 /* @F003694A*/
    }

    /**
     * Gets the value set by setOTMASyncLevel().
   * @return The name of the OTMA Sync Level used for IMS OTMA calls.
   	*/
    public String getOTMASyncLevel()                    /* @F003694A*/
    {
    	return _OTMASyncLevel;
    }

	/**
	 * IMS OTMA Set maximum number of segments supported for send/receive @670111A
	 */
	private int _OTMAMaxSegments = 0;                       /* @670111A*/
	private boolean _OTMAMaxSegmentsFromCSI = false;        /* @F013381A*/

	/**                                                        25@670111A
    * Tells the connection the maximum number of segments that are supported   
     * for send and receive processing - if not used, defaults to 1.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setUseOTMA(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
    * @param OTMAMaxSegments Set this value to the max segments that may be sent 
    *         or received in a multi-segment message from IMS.
	 */
	public void setOTMAMaxSegments(int OTMAMaxSegments)     /* @670111A*/
	{
		_OTMAMaxSegments = OTMAMaxSegments;                  /* @670111A*/
		_OTMAMaxSegmentsFromCSI = true;                       /* @F013381A*/
	}

	/**
	 * Gets the value set by setOTMAMaxSegments().
     * @return max number of IMS OTMA Multi-segments or zero if multi-segment messages
     *        are not supported on the connection.
	 */
	public int getOTMAMaxSegments()                         /* @677477A*/
	{
		return _OTMAMaxSegments;
	}

	/**
	 * Gets the flag indicating set OTMAMaxSegments was called.
   * @return True or False
   */
	public boolean getOTMAMaxSegmentsFromCSI()
	{
		return _OTMAMaxSegmentsFromCSI;
	}

	/**
	 * IMS OTMA Set maximum receive message size @670111A
	 */
	private int _OTMAMaxRecvSize = 0;                       /* @670111A*/
	private boolean _OTMAMaxRecvSizeFromCSI = false;        /* @F013381A*/


	/**                                                      
	* Tells the connection the maximum size for a multi-segment message received 
	 * using OTMA - if not used, defaults to 32760 bytes.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setUseOTMA(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
    * @param OTMAMaxRecvSize Set this value to the max number bytes that may be received
     *        in a multi-segment message from IMS.
	 */
	public void setOTMAMaxRecvSize(int OTMAMaxRecvSize)     /* @670111A*/
	{
		_OTMAMaxRecvSize = OTMAMaxRecvSize;                 /* @670111A*/
		_OTMAMaxRecvSizeFromCSI = true;                       /* @F013381A*/
	}

	/**
	 * Gets the value set by setOTMAMaxRecvSize().
     * @return max receive message size for IMS OTMA requests.
	 */
	public int getOTMAMaxRecvSize()                         /* @670111A*/
	{
		return _OTMAMaxRecvSize;
	}

	/**
	 * Gets the flag indicating set OTMAMaxRecvSize was called.
   * @return True or False
   */
	public boolean getOTMAMaxRecvSizeFromCSI()
	{
		return _OTMAMaxRecvSizeFromCSI;
	}

	/**
	 * IMS OTMA Set request message length format LLZZ 
	 */
	private boolean _reqLLZZ = true;            /* @670111A*/
	private boolean _reqLLZZFromCSI = false;    /* @F013381A*/

	/**
	* Tells the connection that requests to be sent to IMS over OTMA 
     * are formatted with LLZZ-style length prefixes.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLLL(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLLL(boolean)
   * @param reqLLZZ Set this value to true to indicate that LLZZ format lengths 
   *        are passed to this routine for message segments to be sent to IMS. When false, 
   *        message segments are prefixed instead by 4 byte LLLL length indicator.   
	 */
	public void setOTMARequestLLZZ(boolean reqLLZZ) /* @670111A*/
	{
		_reqLLZZ = reqLLZZ;                         /* @670111A*/
		_reqLLZZFromCSI = true;                     /* @F013381A*/
	}

	/**
	 * Gets the value set by setOTMARequestLLZZ().
 * @return true if LLZZ-formatted messages are passed in and are to be
 *         sent to IMS, false if LLLL-formatted messages are in use.
	 */
	public boolean getOTMARequestLLZZ()               /* @670111AA*/
	{
		return _reqLLZZ;
	}
	
	/**
	 * Gets the flag indicating set OTMARequestLLZZ was called.
   * @return True or False
   */
	public boolean getOTMARequestLLZZFromCSI()
	{
		return _reqLLZZFromCSI;
	}

	/**
	 * IMS OTMA Set request message length format LLLL 
	 */

	/**
	* Tells the connection that requests to be sent to IMS over OTMA 
     * are formatted with LLLLZZ-style length prefixes.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLLL(boolean)
   * @param reqLLLL Set this value to true to indicate that LLLL format lengths 
   *        are passed to this routine for message segments to be sent to IMS. When false, 
   *        message segments are prefixed instead by 4 byte LLZZ length indicator.   
	 */
	public void setOTMARequestLLLL(boolean reqLLLL) /* @670111A*/
	{
		if (reqLLLL == true)
		  _reqLLZZ = false;                         /* @670111A*/
		else
		  _reqLLZZ = true;                          /* @670111A*/
		_reqLLZZFromCSI = true;                     /* @F013381A*/
	}

	/**
	 * Gets the value set by setOTMARequestLLLL().
   * @return true if LLLL-formatted messages are in use, false if LLZZ is used.
	 */
  public boolean getOTMARequestLLLL()               /* @670111AA*/
  {
	  if (_reqLLZZ == true)
		  return false;
	  else
		  return true;                              /* @670111A*/
  }

	/**
	 * IMS OTMA Set response message length format LLZZ 
	 */
	private boolean _respLLZZ = false;          /* @670111A*/
	private boolean _respLLZZFromCSI = false;   /* @F013381A*/

	/**
	* Tells the connection that responses received from IMS over OTMA 
   * are to be formatted with LLZZ-style length prefixes.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
 * @param respLLZZ Set this value to true to indicate that LLZZ format lengths 
 *        are desired for message segments received from IMS. When false, message segments
 *        are prefixed instead by 4 byte LLLL length indicator.   
	 */
	public void setOTMAResponseLLZZ(boolean respLLZZ) /* @670111A*/
	{
		_respLLZZ = respLLZZ;                         /* @670111A*/
		_respLLZZFromCSI = true;                     /* @F013381A*/
	}

	/**
	 * Gets the value set by setOTMAResponseLLZZ().
 * @return true if LLZZ-formatted messages are expected for responses from
 *         IMS, false if LLLL format messages are expected.
	 */
	public boolean getOTMAResponseLLZZ()               /* @670111AA*/
	{
		return _respLLZZ;
	}

	/**
	 * Gets the flag indicating set OTMAResponseLLZZ was called.
   * @return True or False
   */
	public boolean getOTMAResponseLLZZFromCSI()
	{
		return _respLLZZFromCSI;
	}

	/**
	 * IMS OTMA RRS Transactional flag 
	 */
	private boolean _RRSTransactional = false;           /* @F014447A*/
	private boolean _RRSTransactionalFromCSI = false;    /* @F014447A*/

	/**
	* Tells the connection that RRS transactions are to be enabled. 
 * @param RRSTransactional indicates that RRS is to be enabled when set to 1. 
	 */
	public void setRRSTransactional(boolean RRSTransactional) /* @F014447A*/
	{
		_RRSTransactional = RRSTransactional;             /* @F014447A*/
		_RRSTransactionalFromCSI = true;                  /* @F013381A*/
	}

	/**
	 * Gets the value set by setRRSTransactional().
 * @return true if RRS transactions are to be enabled for calls to IMS
 * applications.
	 */
	public boolean getRRSTransactional()                   /* @F014447A*/
	{
		return _RRSTransactional;                          /* @F014447A*/
	}

	/**
	 * Gets the flag indicating set RRSTransactional was called.
   * @return True or False
   */
	public boolean getRRSTransactionalFromCSI()            /* @F014447A*/
	{
		return _RRSTransactionalFromCSI;                   /* @F014447A*/
	}
	
	/**
	 * IMS OTMA Set response message length format LLLL 
	 */

	/**
	* Tells the connection that responses received from IMS over OTMA 
   * are to be formatted with LLLLZZ-style length prefixes.
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAClientName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAServerName(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAGroupID(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMASyncLevel(String)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxSegments(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAMaxRecvSize(int)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMAResponseLLZZ(boolean)
	 * @see com.ibm.websphere.ola.ConnectionSpecImpl#setOTMARequestLLLL(boolean)
 * @param respLLLL Set this value to true to indicate that LLLL format lengths 
 *        are desired for message segments received from IMS. When false, message segments
 *        are prefixed instead by 4 byte LLZZ length indicator.   
	 */
	public void setOTMAResponseLLLL(boolean respLLLL) /* @670111A*/
	{
		if (respLLLL == true)
		  _respLLZZ = false;                         /* @670111A*/
		else
		  _respLLZZ = true;                          /* @670111A*/

		_respLLZZFromCSI = true;                     /* @F013381A*/
	}

	/**
	 * Gets the value set by setOTMAResponseLLLL().
   * @return true if LLLL-formatted messages are in use, false if LLZZ is used.
	 */
  public boolean getOTMAResponseLLLL()               /* @670111AA*/
  {
	  if (_respLLZZ == true)
		  return false;
	  else
		  return true;                              /* @670111A*/
  }


  /**
   * The remote user ID
   */
  private String _username = null;                               /* @F003705A*/


  /**
   * Sets the user ID to use to log into the remote WebSphere
   * Application Server for z/OS server when using the remote proxy EJB.
   * @param userID The user ID
   */
  public void setUsername(String username)                       /* @F003705A*/
  {
    _username = username;
  }

  /**
   * Gets the user ID used to log into the remote WebSphere
   * Application Server for z/OS server when using the remote proxy
   * EJB.
   * @return The user ID
   */
  public String getUsername()                                    /* @F003705A*/
  {
    return _username;
  }

  /**
   * The remote password
   */
  private String _password = null;                               /* @F003705A*/


  /**
   * Sets the password to use to log into the remote WebSphere Application
   * Server for z/OS server when using the remote proxy EJB.
   * @param password The password
   */
  public void setPassword(@Sensitive String password)                       /* @F003705A*/
  {
    _password = password;
  }

  /**
   * Gets the password used to log into the remote WebSphere Application
   * Server for z/OS server when using the remote proxy EJB.
   * @return The password
   */
  @Sensitive
  public String getPassword()                                    /* @F003705A*/
  {
    return _password;
  }
  

  /**
   * Generates a hash code for object comparison
   */
  public int hashCode()                                          /* @F003705A*/
  {
    return ((_registerName != null) ? _registerName.hashCode() : 0);
  }

  /**
   * Compares two objects
   */
  public boolean equals(Object thatObject)                       /* @F003705A*/
  {
    boolean result = false;

    if (thatObject == this)
    {
      result = true;
    }
    else if ((thatObject != null) && 
             (thatObject instanceof ConnectionSpecImpl))
    {
      ConnectionSpecImpl that = (ConnectionSpecImpl)thatObject;

      result = ((this._version == that._version) &&
                (compareObjects(this._registerName, that._registerName)) &&
                (this._connectionWaitTimeout == that._connectionWaitTimeout) &&
                (compareObjects(this._linkTaskTranID, that._linkTaskTranID)) &&
                (compareObjects(this._linkTaskReqContID, 
                                that._linkTaskReqContID)) &&
                (this._linkTaskReqContType == that._linkTaskReqContType) &&
                (compareObjects(this._linkTaskRspContID, 
                                that._linkTaskRspContID)) &&
                (this._linkTaskRspContType == that._linkTaskRspContType) &&
				(compareObjects(this._linkTaskChanID, that._linkTaskChanID)) &&          /* @F014448A */
				(this._linkTaskChanType == that._linkTaskChanType) &&                    /* @F014448A */
                (this._useCICSContainer == that._useCICSContainer) &&
                (this._useOTMA == that._useOTMA) &&
                (compareObjects(this._OTMAClientName, that._OTMAClientName)) &&
                (compareObjects(this._OTMAServerName, that._OTMAServerName)) &&
                (compareObjects(this._OTMAGroupID, that._OTMAGroupID)) &&
                (compareObjects(this._OTMASyncLevel, that._OTMASyncLevel)) &&
                (this._reqLLZZ == that._reqLLZZ) &&
                (this._respLLZZ == that._respLLZZ) &&
                (compareObjects(this._username, that._username)) &&
                (compareObjects(this._password, that._password)) &&
                (this._RRSTransactional == that._RRSTransactional));    /* @F014447A*/
    }

    return result;
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
    putField.put("_registerName", _registerName);
    putField.put("_connectionWaitTimeout", _connectionWaitTimeout);
    putField.put("_connectionWaitTimeoutFromCSI", _connectionWaitTimeoutFromCSI);
    putField.put("_linkTaskTranID", _linkTaskTranID);
    putField.put("_linkTaskReqContID", _linkTaskReqContID);
    putField.put("_linkTaskReqContType", _linkTaskReqContType);
    putField.put("_linkTaskReqContTypeFromCSI", _linkTaskReqContTypeFromCSI);
    putField.put("_linkTaskRspContID", _linkTaskRspContID);
    putField.put("_linkTaskRspContType", _linkTaskRspContType);
    putField.put("_linkTaskRspContTypeFromCSI", _linkTaskRspContTypeFromCSI);
    putField.put("_useCICSContainer", _useCICSContainer);
    putField.put("_useCICSContainerFromCSI", _useCICSContainerFromCSI);
	putField.put("_linkTaskChanID", _linkTaskChanID);                                /* @F014448A */
	putField.put("_linkTaskChanType", _linkTaskChanType);                            /* @F014448A */
	putField.put("_linkTaskChanTypeFromCSI", _linkTaskChanTypeFromCSI);              /* @F014448A */
    putField.put("_useOTMA", _useOTMA);
    putField.put("_OTMAClientName", _OTMAClientName);
    putField.put("_OTMAServerName", _OTMAServerName);
    putField.put("_OTMAGroupID", _OTMAGroupID);
    putField.put("_OTMASyncLevel", _OTMASyncLevel);
    putField.put("_OTMAMaxSegments", _OTMAMaxSegments);
    putField.put("_OTMAMaxSegmentsFromCSI", _OTMAMaxSegmentsFromCSI);
    putField.put("_OTMAMaxRecvSize", _OTMAMaxRecvSize);
    putField.put("_OTMAMaxRecvSizeFromCSI", _OTMAMaxRecvSizeFromCSI);
    putField.put("_reqLLZZ", _reqLLZZ);
    putField.put("_reqLLZZFromCSI", _reqLLZZFromCSI);
    putField.put("_respLLZZ", _respLLZZ);
    putField.put("_respLLZZFromCSI", _respLLZZFromCSI);
    putField.put("_username", _username);
    putField.put("_password", _password);
    putField.put("_RRSTransactional", _RRSTransactional);               /* @F014447A*/
    putField.put("_RRSTransactionalFromCSI", _RRSTransactionalFromCSI); /* @F014447A*/
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
    
    if (getField.defaulted("_version"))
    {
      throw new java.io.InvalidObjectException(
        "No version number in serialized object");
    }
    
    _version = getField.get("_version", (int)CURRENT_VERSION);

    if (_version > CURRENT_VERSION)
    {
      throw new java.io.InvalidObjectException(
        "Version number in serialized object is not supported (" + _version + 
        " > " + CURRENT_VERSION + ")");
    }

    _registerName = (String)getField.get("_registerName", null);
    _connectionWaitTimeout = getField.get("_connectionWaitTimeout", (int)0);
    _connectionWaitTimeoutFromCSI = getField.get("_connectionWaitTimeoutFromCSI", false);
    _linkTaskTranID = (String)getField.get("_linkTaskTranID", null);
    _linkTaskReqContID = (String)getField.get("_linkTaskReqContID", null);
    _linkTaskReqContType = getField.get("_linkTaskReqContType", 
                                        (int)CHAR_CONTAINER);
    _linkTaskReqContTypeFromCSI = getField.get("_linkTaskReqContTypeFromCSI",false); 
    _linkTaskRspContID = (String)getField.get("_linkTaskRspContID", null);
    _linkTaskRspContType = getField.get("_linkTaskRspContType", 
                                        (int)CHAR_CONTAINER);
    _linkTaskRspContTypeFromCSI = getField.get("_linkTaskRspContTypeFromCSI",false); 
    _useCICSContainer = getField.get("_useCICSContainer", false);
    _useCICSContainerFromCSI = getField.get("_useCICSContainerFromCSI", false);
	_linkTaskChanID = (String)getField.get("_linkTaskChanID", null);                         /* @F014448A */
	_linkTaskChanType = getField.get("_linkTaskChanType", (int)CHAR_CHANNEL);                /* @F014448A */
	_linkTaskChanTypeFromCSI = getField.get("_linkTaskChanTypeFromCSI", false);              /* @F014448A */
    _useOTMA = getField.get("_useOTMA", false);
    _OTMAClientName = (String) getField.get("_OTMAClientName", null);
    _OTMAServerName = (String) getField.get("_OTMAServerName", null);
    _OTMAGroupID = (String)getField.get("_OTMAGroupID", null);
    _OTMASyncLevel = (String)getField.get("_OTMASyncLevel", null);
    _OTMAMaxSegments = getField.get("_OTMAMaxSegments", (int)0);
    _OTMAMaxSegmentsFromCSI = getField.get("_OTMAMaxSegmentsFromCSI", false);
    _OTMAMaxRecvSize = getField.get("_OTMAMaxRecvSize", (int)0);
    _OTMAMaxRecvSizeFromCSI = getField.get("_OTMAMaxRecvSizeFromCSI", false);
    _reqLLZZ = getField.get("_reqLLZZ", false);
    _reqLLZZFromCSI = getField.get("_reqLLZZFromCSI", false);
    _respLLZZ = getField.get("_respLLZZ", false);
    _respLLZZFromCSI = getField.get("_respLLZZFromCSI", false);
    _username = (String)getField.get("_username", null);
    _password = (String)getField.get("_password", null);
    _RRSTransactional = getField.get("_RRSTransactional", false);               /* @F014447A*/
    _RRSTransactionalFromCSI = getField.get("_RRSTransactionalFromCSI", false); /* @F014447A*/
  }
}
