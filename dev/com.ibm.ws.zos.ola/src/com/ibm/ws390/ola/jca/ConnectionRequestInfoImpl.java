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
package com.ibm.ws390.ola.jca;                                   /* @F003691C*/

import java.util.regex.Pattern;

import javax.resource.spi.ConnectionRequestInfo;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The ConnectionRequestInfoImpl is the server side representation of
 * the ConnectionSpecImpl.  It holds information specific to the
 * connection.
 * @author kaczyns
 */
public class ConnectionRequestInfoImpl implements ConnectionRequestInfo {

	private static final TraceComponent tc = Tr.register(ConnectionRequestInfoImpl.class);

    public static final int REGISTER_NAME_LENGTH = 12;             /* @F003691C*/

	private static final int LINK_TASK_TRAN_ID_LENGTH = 4;         /* @580321A*/
	private static final int LINK_TASK_CONT_ID_LENGTH = 16;        /* @580321A*/
	private static final int LINK_TASK_CHAN_ID_LENGTH = 16;        /* @F014448A */
	private static final int MVS_USER_ID_LENGTH = 8;               /* @580321A*/
	private static final int LINK_TASK_DEFAULT_CONTTYPE_CHAR = 0;  /* @580321A*/
	public static final int OTMA_CLIENT_NAME_LENGTH = 16;
	public static final int OTMA_SERVER_NAME_LENGTH = 16;          /* @F003694A*/
	public static final int OTMA_GROUPID_LENGTH = 8;               /* @F003694A*/
	public static final int OTMA_SYNCLEVEL_LENGTH = 8;             /* @F003694A*/
	public static final int OTMA_SENDMSEG_LENGTH = 4;              /* @670111AA*/
	public static final int OTMA_RECVMSEG_LENGTH = 4;              /* @670111AA*/
        public static final Pattern OTMAClientNamePattern = Pattern.compile("[A-Z0-9@#$]{1," + OTMA_CLIENT_NAME_LENGTH + "}");
        public static final Pattern OTMAServerNamePattern = Pattern.compile("[A-Z0-9@#$]{1," + OTMA_SERVER_NAME_LENGTH + "}");
        public static final Pattern OTMAGroupIDPattern    = Pattern.compile("[A-Z0-9@#$]{1," + OTMA_GROUPID_LENGTH     + "}");
	/**
	 * Place to store the register name.
	 */
	private String _registerName = null;
	
	/**
	 * Connection timeout - defaults to 30 seconds
	 */
	private int _connectionWaitTimeout = 30;
	
	/**
	 * Connection Wait timeout from CSI flag
	 */
	private boolean _connectionWaitTimeoutFromCSI = false;             /* @F013381A*/

	/**
	 * CICS Link task Transaction id
	 */
	private String _linkTaskTranID = null;                         /* @580321C*/
	
	/**
	 * CICS Link task Request Container ID
	 */
	private String _linkTaskReqContID = null;                      /* @580321C*/
	
	/**
	 * CICS Link task Response Container ID
	 */
	private String _linkTaskRspContID = null;                      /* @580321C*/

	/**
	 * CICS Link task Request Container Type
	 */
	private int _linkTaskReqContType = 0;

	/**
	 * CICS Link task Request Container Type from CSI flag
	 */
	private boolean _linkTaskReqContTypeFromCSI = false;

	
	/**
	 * CICS Link task Response Container Type
	 */
	private int _linkTaskRspContType = 0;
	
	/**
	 * CICS Link task Response Container Type from CSI flag
	 */
	private boolean _linkTaskRspContTypeFromCSI = false;

	/**
	 * CICS Link task Channel ID
	 */
	private String _linkTaskChanID = null;                       /* @F014448A */

	/**
	 * CICS Link task Channel Type
	 */
	private int _linkTaskChanType = 0;                           /* @F014448A */

	/**
	 * CICS Link task Channel Type from CSI flag
	 */
	private boolean _linkTaskChanTypeFromCSI = false;            /* @F014448A */

	/**
	 * Use CICS Containers - defaults to false = No
	 */
	private boolean _useCICSContainer = false; /* @578669C*/

	/**
	 * Use CICS Containers from CSI flag
	 */
	private boolean _useCICSContainerFromCSI = false;             /* @F013381A*/

	/**
	 * Use OTMA for call - defaults to false = No
	 */
	private boolean _useOTMA = false; /* @F003694A*/

	/**
	 * OTMA Client name 
	 */
	private String _OTMAClientName = null;

	/**
	 * OTMA Server name 
	 */
	private String _OTMAServerName = null;          /* @F003694A*/

	/**
	 * OTMA XCF Group ID name
	 */
	private String _OTMAGroupID= null;              /* @F003694A*/
	
	/**
	 * OTMA Sync Level setting
	 */
	private String _OTMASyncLevel = null;           /* @F003694A*/

	/**
	 * OTMA Send/Receive Max segments setting
	 */
	private int _OTMAMaxSegments = 0;               /* @670111A*/

	/**
	 * RRS Transactional flag
	 */
	private boolean _RRSTransactional = false;       /* @F014447A*/

	/**
	 * OTMA Send/Receive Max segments from CSI flag
	 */
	private boolean _OTMAMaxSegmentsFromCSI = false; /* @F013381A*/

	/**
	 * OTMA Receive Max message size setting
	 */
	private int _OTMAMaxRecvSize = 0;               /* @670111A*/

	/**
	 * OTMA Max Receive size from CSI flag
	 */
	private boolean _OTMAMaxRecvSizeFromCSI = false; /* @F013381A*/

	/**
	 * OTMA request segments use LLZZs
	 */
	private boolean _OTMAReqLLZZ = true;            /* @670111A*/

	/**
	 * OTMA request segments use LLZZs from CSI flag
	 */
	private boolean _OTMARequestLLZZFromCSI = false;     /* @F013381A*/

	/**
	 * OTMA response segments use LLZZs
	 */
	private boolean _OTMARespLLZZ = false;          /* @670111A*/

	/**
	 * OTMA response segments use LLZZs from CSI flag
	 */
	private boolean _OTMAResponseLLZZFromCSI = false;   /* @F013381A*/

	/**
	 * RRS Transactional from CSI flag
	 */
	private boolean _RRSTransactionalFromCSI = false;   /* @F014447A*/
	

	/**
   * Username
   */
  private String _username = null;                /* @F003705A*/
  
  /**
   * Password
   */
  private String _password = null;                /* @F003705A*/

  private TraceUtil _tu = new TraceUtil(tc);
  
	/**
	 * Default constructor.
	 */
	public ConnectionRequestInfoImpl(
       String registerName, int connectionWaitTimeout,
	   String linkTaskTranID, String linkTaskReqContID, int linkTaskReqContType, 
	   String linkTaskRspContID, int linkTaskRspContType, boolean useCICSContainer,
	   String linkTaskChanID, int linkTaskChanType,                                   /* @F014448A */
	   boolean useOTMA, String OTMAClientName, String OTMAServerName, String OTMAGroupID, /* @648942C*/
	   String OTMASyncLevel, int OTMAMaxSegments, int OTMAMaxRecvSize, 
	   boolean OTMAReqLLZZ, boolean OTMARespLLZZ, 
	   String username, @Sensitive String password,
	   boolean RRSTransactional,                                                /* @F014447A*/
	   boolean connectionWaitTimeoutFromCSI,  									/* @F013381A*/
	   boolean linkTaskReqContTypeFromCSI,  boolean linkTaskRspContTypeFromCSI, /* @F013381A*/
	   boolean linkTaskChanTypeFromCSI,                                         /* @F014448A */
	   boolean useCICSContainerFromCSI,  boolean OTMAMaxSegmentsFromCSI, 		/* @F013381A*/
	   boolean OTMAMaxRecvSizeFromCSI, 											/* @F013381A*/
	   boolean OTMARequestLLZZFromCSI, boolean OTMAResponseLLZZFromCSI,  		/* @F013381A*/
	   boolean RRSTransactionalFromCSI)                                  		/* @F014447A*/
	   /* @F003705C*/
	{
    /*----------------------------------------------------------------------*/
    /* Some register name validation is done in the MC, because the         */
    /* register name could also be specified in the MCF.                    */
    /*----------------------------------------------------------------------*/
		if (registerName != null)                                    /* @580321A*/
		{
			int registerNameLength = registerName.length();

			if ((registerNameLength == 0) ||
					(registerNameLength > REGISTER_NAME_LENGTH))
			{
				throw new IllegalArgumentException(
						"Register name (" + registerName + ") cannot be longer than " + 
						REGISTER_NAME_LENGTH + " characters");
			}
		}

		_registerName = registerName;

		if (connectionWaitTimeout < 0)                               /* @580321A*/
		{
			throw new IllegalArgumentException(
					"Connection wait timeout (" + connectionWaitTimeout + ") must be " +
					"greater than or equal to zero");
		}

		_connectionWaitTimeout = connectionWaitTimeout;

		if (linkTaskTranID != null)                                  /* @580321A*/
		{
			int linkTaskTranIDLength = linkTaskTranID.length();

			if ((linkTaskTranIDLength == 0) ||
					(linkTaskTranIDLength > LINK_TASK_TRAN_ID_LENGTH))
			{
				throw new IllegalArgumentException(
						"Link task tran ID (" + linkTaskTranID + ") must be " +
						"between 1 and " + LINK_TASK_TRAN_ID_LENGTH + " characters");
			}
		}

		_linkTaskTranID        = linkTaskTranID;

		if (linkTaskReqContID != null)                               /* @580321A*/
		{
			int linkTaskReqContIDLength = linkTaskReqContID.length();
      
			if ((linkTaskReqContIDLength == 0) ||
					(linkTaskReqContIDLength > LINK_TASK_CONT_ID_LENGTH))
			{
				throw new IllegalArgumentException(
						"Link task request container ID (" + linkTaskReqContID + 
						") must be between 1 and " + LINK_TASK_CONT_ID_LENGTH + 
						" characters");
			}
		}

		_linkTaskReqContID     = linkTaskReqContID;
		_linkTaskReqContType   = linkTaskReqContType;

		if (linkTaskRspContID != null)                               /* @580321A*/
		{
			int linkTaskRspContIDLength = linkTaskRspContID.length();

			if ((linkTaskRspContIDLength == 0) ||
					(linkTaskRspContIDLength > LINK_TASK_CONT_ID_LENGTH))
			{
				throw new IllegalArgumentException(
						"Link task response container ID (" + linkTaskRspContID + 
						") must be between 1 and " + LINK_TASK_CONT_ID_LENGTH + 
						" characters");
			}
		}

		_linkTaskRspContID     = linkTaskRspContID;
		_linkTaskRspContType   = linkTaskRspContType;
		
		if (linkTaskChanID != null)                                                /* @F014448A */
		{                                                                          /* @F014448A */
			int linkTaskChanIDLength = linkTaskChanID.length();                    /* @F014448A */

			if ((linkTaskChanIDLength == 0) ||                                     /* @F014448A */
					(linkTaskChanIDLength > LINK_TASK_CHAN_ID_LENGTH))             /* @F014448A */
			{                                                                      /* @F014448A */
				throw new IllegalArgumentException(                                /* @F014448A */
						"Link task channel ID (" + linkTaskChanID +                /* @F014448A */
						") must be between 1 and " + LINK_TASK_CHAN_ID_LENGTH +    /* @F014448A */
						" characters");                                            /* @F014448A */
			}                                                                      /* @F014448A */
		}                                                                          /* @F014448A */
		
		_linkTaskChanID		= linkTaskChanID;                                      /* @F014448A */
		_linkTaskChanType	= linkTaskChanType;                                    /* @F014448A */

		_useCICSContainer      = useCICSContainer;
	
		_useOTMA               = useOTMA;

		if ((OTMAClientName != null) && (!OTMAClientNamePattern.matcher(OTMAClientName.trim()).matches()))
		{
			Tr.warning(tc, "CWWKB0393W", new Object[] { OTMAClientName, OTMA_CLIENT_NAME_LENGTH });
		}

		if ((OTMAServerName != null) && (!OTMAServerNamePattern.matcher(OTMAServerName.trim()).matches()))
		{
			throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKB0395E", new Object[] { "Server", OTMAServerName, OTMA_SERVER_NAME_LENGTH } ));
		}                                                            /* @F003694A*/

		if ((OTMAGroupID != null) && (!OTMAGroupIDPattern.matcher(OTMAGroupID.trim()).matches()))
		{
			throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKB0395E", new Object[] { "Group", OTMAGroupID, OTMA_GROUPID_LENGTH } ));
		}                                                            /* @F003694A*/
			
		_OTMAClientName = OTMAClientName;
		_OTMAServerName = OTMAServerName;                            /* @F003694A*/  
		_OTMAGroupID = OTMAGroupID;                                  /* @F003694A*/  

		if (OTMASyncLevel != null)                                   /* @F003694A*/
		{
			int OTMASyncLevelLength = OTMASyncLevel.length();        /* @F003694A*/
	
			if ((OTMASyncLevelLength == 0) ||                    
					(OTMASyncLevelLength > 1))                       /* @F003694A*/
			{
				throw new IllegalArgumentException(
						"OTMA Sync level (" + OTMASyncLevel + ") must be a single character 0|1)");/* @F003694A*/
			}
		}                                                            /* @F003694A*/
    
		_OTMASyncLevel = OTMASyncLevel;                              /* @F003694A*/  
		_OTMAMaxSegments = OTMAMaxSegments;                          /* @670111A*/  
		_OTMAMaxRecvSize = OTMAMaxRecvSize;                          /* @670111A*/  
		_OTMAReqLLZZ = OTMAReqLLZZ;                                  /* @670111A*/  
		_OTMARespLLZZ = OTMARespLLZZ;                                /* @670111A*/  
		
      _username = username;                                     /* @F003705A*/
      _password = password;                                     /* @F003705A*/
	  _RRSTransactional = RRSTransactional;                           /* @F014447AA*/  
      
      _connectionWaitTimeoutFromCSI = connectionWaitTimeoutFromCSI;   /* @F013381A*/
      _linkTaskReqContTypeFromCSI = linkTaskReqContTypeFromCSI;       /* @F013381A*/
      _linkTaskRspContTypeFromCSI = linkTaskRspContTypeFromCSI;       /* @F013381A*/
	  _linkTaskChanTypeFromCSI = linkTaskChanTypeFromCSI;             /* @F014448A */
      _useCICSContainerFromCSI = useCICSContainerFromCSI;             /* @F013381A*/
      _OTMAMaxSegmentsFromCSI = OTMAMaxSegmentsFromCSI;               /* @F013381A*/
      _OTMAMaxRecvSizeFromCSI = OTMAMaxRecvSizeFromCSI;               /* @F013381A*/
      _OTMARequestLLZZFromCSI = OTMARequestLLZZFromCSI;               /* @F013381A*/
      _OTMAResponseLLZZFromCSI = OTMAResponseLLZZFromCSI;             /* @F013381A*/
      _RRSTransactionalFromCSI = RRSTransactionalFromCSI;             /* @F014447A*/
	}

	/**
	 * Equals method.
	 */
	public boolean equals(Object o)
	{
    // Assume equality
		boolean equal = true;                                         /* @580321C*/
		
		// Make sure that the info is ours.
		if ((o != null) && (o instanceof ConnectionRequestInfoImpl))
		{
			ConnectionRequestInfoImpl crii = (ConnectionRequestInfoImpl)o;
			
			// Get the register names of the two objects.
			String thisRegisterName = this.getRegisterName();
			String thatRegisterName = crii.getRegisterName();

			// Check the register names
			if (thisRegisterName != null) {                  /* @728847A*/
				if (thisRegisterName.equals(thatRegisterName) == false)  /* @580321C*/
					equal = false;
			}
			else {                                           /* @728847A*/
		        if (thatRegisterName != null) equal = false; /* @728847A*/
			}                                                /* @728847A*/

      // Check the connection timeout
      if (this._connectionWaitTimeout != crii._connectionWaitTimeout) /* @580321A*/
      {
        equal = false;
      }

      // Get the link task transaction IDs
      if (this._linkTaskTranID != null)                               /* @580321A*/
      {
        if (this._linkTaskTranID.equals(crii._linkTaskTranID) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._linkTaskTranID != null) equal = false;
      }

      // Get the link task request container ID
      if (this._linkTaskReqContID != null)                            /* @580321A*/
      {
        if (this._linkTaskReqContID.equals(crii._linkTaskReqContID) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._linkTaskReqContID != null) equal = false;
      }

      // Get the link task response container ID
      if (this._linkTaskRspContID != null)                            /* @580321A*/
      {
        if (this._linkTaskRspContID.equals(crii._linkTaskRspContID) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._linkTaskRspContID != null) equal = false;
      }

      // Check the CICS link task request container type
      if (this._linkTaskReqContType != crii._linkTaskReqContType)     /* @580321A*/
      {
        equal = false;
      }

      // Check the CICS link task response container type
      if (this._linkTaskRspContType != crii._linkTaskRspContType)     /* @580321A*/
      {
        equal = false;
      }
	  
      // Get the link task channel ID
      if (this._linkTaskChanID != null)                                          /* @F014448A */
      {                                                                          /* @F014448A */
        if (this._linkTaskChanID.equals(crii._linkTaskChanID) == false)          /* @F014448A */
        {                                                                        /* @F014448A */
          equal = false;                                                         /* @F014448A */
        }                                                                        /* @F014448A */
      }                                                                          /* @F014448A */
      else                                                                       /* @F014448A */
      {                                                                          /* @F014448A */
        if (crii._linkTaskChanID != null) equal = false;                         /* @F014448A */
      }                                                                          /* @F014448A */
	  
      // Check the CICS link task channel type
      if (this._linkTaskChanType != crii._linkTaskChanType)                      /* @F014448A */
      {
        equal = false;
      }

      // Check the CICS container flag
      if (this._useCICSContainer != crii._useCICSContainer)           /* @580321A*/
      {
        equal = false;
      }

      // Check the use OTMA flag
      if (this._useOTMA != crii._useOTMA)                             /* @F003694AA*/
      {
        equal = false;
      }

      // Check the OTMA Client name
      if (this._OTMAClientName != null)
      {
        if (this._OTMAClientName.equals(crii._OTMAClientName) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._OTMAClientName != null) equal = false;
      }

      // Check the OTMA Server name
      if (this._OTMAServerName != null)                            /* @F003694A*/
      {
        if (this._OTMAServerName.equals(crii._OTMAServerName) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._OTMAServerName != null) equal = false;
      }

      // Check the OTMA Group ID name
      if (this._OTMAGroupID != null)                            /* @F003694A*/
      {
        if (this._OTMAGroupID.equals(crii._OTMAGroupID) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._OTMAGroupID != null) equal = false;
      }

      // Check the OTMA Sync Level
      if (this._OTMASyncLevel != null)                            /* @F003694A*/
      {
        if (this._OTMASyncLevel.equals(crii._OTMASyncLevel) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._OTMASyncLevel != null) equal = false;
      }

      // Check the OTMA Max Segments
      if (this._OTMAMaxSegments != 0)                        /* @670111A*/
      {
        if (this._OTMAMaxSegments != crii._OTMAMaxSegments)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._OTMAMaxSegments != 0) equal = false;
      }

      // Check the OTMA Max Receive size
      if (this._OTMAMaxRecvSize != 0)                        /* @670111A*/
      {
        if (this._OTMAMaxRecvSize != crii._OTMAMaxRecvSize)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._OTMAMaxRecvSize != 0) equal = false;
      }
      
      // Check the OTMA Request LLZZ flag
      if (this._OTMAReqLLZZ != crii._OTMAReqLLZZ)           /* @670111A*/
      {
        equal = false;
      }

      // Check the OTMA Response LLZZ flag
      if (this._OTMARespLLZZ != crii._OTMARespLLZZ)           /* @670111A*/
      {
        equal = false;
      }

      // Check the username
      if (this._username != null)                                /* @F003705A*/
      {
        if (this._username.equals(crii._username) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._username != null) equal = false;
      }

      // Check the remote password
      if (this._password != null)                                /* @F003705A*/
      {
        if (this._password.equals(crii._password) == false)
        {
          equal = false;
        }
      }
      else
      {
        if (crii._password != null) equal = false;
      }
    }
    else
    {
      equal = false;
    }
		
		return equal;
	}
	
	/**
	 * Hashcode method
	 */
	public int hashCode()
	{
		int hash = 0;
		
		if (_registerName != null) hash = _registerName.hashCode();

		return hash;
	}
	
	/**
	 * Gets the register name.
	 */
	public String getRegisterName()
	{
		return _registerName;
	}
	
	/**
	 * Gets the connection wait timeout value.
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
	 * Gets the CICS Link task transaction id.
	 */
	public String getLinkTaskTranID()
	{
		return _linkTaskTranID;
	}

	/**
	 * Gets the CICS Link task request container id.
	 */
	public String getLinkTaskReqContID()
	{
		return _linkTaskReqContID;
	}

	/**
	 * Gets the CICS Link task request container type.
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
	 * Gets the CICS Link task response container id.
	 */
	public String getLinkTaskRspContID()
	{
		return _linkTaskRspContID;
	}

	/**
	 * Gets the CICS Link task response container type.
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
	 * Gets the CICS Link task channel id.
	 */
	public String getLinkTaskChanID()                 /* @F014448A */
	{                                                 /* @F014448A */
		return _linkTaskChanID;                       /* @F014448A */
	}                                                 /* @F014448A */
	
	/**
	 * Gets the CICS Link task channel type.
	 */
	public int getLinkTaskChanType()                  /* @F014448A */
	{                                                 /* @F014448A */
		return _linkTaskChanType;                     /* @F014448A */
	}                                                 /* @F014448A */
	
	/**
	 * Gets the flag indicating set LinkTaskChanType was called
	 * @return True or False
	 */
	public boolean getlinkTaskChanTypeFromCSI()       /* @F014448A */
	{                                                 /* @F014448A */
		return _linkTaskChanTypeFromCSI;              /* @F014448A */
	}                                                 /* @F014448A */
	
	/**
	 * Gets the CICS Use containers flag.
	 */
	public int getUseCICSContainer()
	{
    return (_useCICSContainer ? 1 : 0); /* @578669C*/
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
	 * Gets the use OTMA flag.
	 */
	public int getUseOTMA()
	{
    return (_useOTMA ? 1 : 0);         /* @F003694A*/
	}

	/**
	 * Sets the use OTMA flag.
	 */
	public void setUseOTMA(boolean useOTMA)
	{
      _useOTMA = useOTMA;              /* @F003694A*/
	}

	/**
	 * Gets the OTMA Client name.
	 */
	public String getOTMAClientName()
	{
		return _OTMAClientName;
	}

	/**
	 * Gets the OTMA Server Name.
	 */
	public String getOTMAServerName()
	{
		return _OTMAServerName;
	}

	/**
	 * Gets the OTMA XCF Group ID.
	 */
	public String getOTMAGroupID()
	{
		return _OTMAGroupID;
	}

	/**
	 * Gets the OTMA Sync Level setting.
	 */
	public String getOTMASyncLevel()
	{
		return _OTMASyncLevel;
	}

	/**
	 * Gets the OTMA Send/Receive Max segments setting.
	 */
	public int getOTMAMaxSegments()
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
	 * Gets the OTMA Receive Max message size setting.
	 */
	public int getOTMAMaxRecvSize()
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
	 * Gets the OTMA Request LLZZ flag.
	 */
	public int getOTMAReqLLZZ()
	{
    return (_OTMAReqLLZZ ? 1 : 0);          /* @670111A*/
	}
	
	/**
	 * Gets the flag indicating set OTMARequestLLZZ was called.
   * @return True or False
   */
	public boolean getOTMARequestLLZZFromCSI()
	{
		return _OTMARequestLLZZFromCSI;
	}

	/**
	 * Sets the use OTMA Request LLZZ flag.
	 */
	public void setOTMAReqLLZZ(boolean OTMAReqLLZZ)
	{
      _OTMAReqLLZZ = OTMAReqLLZZ;           /* @670111A*/
	}

	/**
	 * Gets the OTMA Response LLZZ flag.
	 */
	public int getOTMARespLLZZ()
	{
    return (_OTMARespLLZZ ? 1 : 0);         /* @670111A*/
	}
	
	/**
	 * Gets the flag indicating set OTMAResponseLLZZ was called.
   * @return True or False
   */
	public boolean getOTMAResponseLLZZFromCSI()
	{
		return _OTMAResponseLLZZFromCSI;
	}

	/**
	 * Sets the use OTMA Response LLZZ flag.
	 */
	public void setOTMARespLLZZ(boolean OTMARespLLZZ)
	{
      _OTMARespLLZZ = OTMARespLLZZ;         /* @670111A*/
	}

	/**
	 * Gets the RRS Transactional flag.
	 */
	public boolean getRRSTransactional()
	{
		return _RRSTransactional;
	}
	
	/**
	 * Gets the RRSTransactional has been set in the CSI flag.
   * @return True or False
   */
	public boolean getRRSTransactionalFromCSI()
	{
		return _RRSTransactionalFromCSI;
	}

  /**
   * Gets the remote username
   */
  public String getUsername()                                    /* @F003705A*/
  {
    return _username;
  }

  /**
   * Gets the remote password
   */
  public String getPassword()                                    /* @F003705A*/
  {
    return _password;
  }
}
