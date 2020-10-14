/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws390.ola.jca; /* @F003691C*/

import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.LocalTransactionException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.SecurityException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.websphere.ola.ConnectionSpecImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.zos.channel.wola.WolaJcaRequestInfo;
import com.ibm.ws.zos.channel.wola.WolaOtmaRequestInfo;
import com.ibm.ws390.ola.ManagedConnectionFactoryImpl;
import com.ibm.wsspi.security.credentials.saf.SAFCredential;

/**
 * This is the javax.resource.spi.ManagedConnection implementation.
 * The MangedConnection has a 1:1 ratio to Connection objects used by the client.  It
 * is the server side version.
 */
public class ManagedConnectionImpl implements ManagedConnection, WolaJcaRequestInfo, WolaOtmaRequestInfo {

	private static final TraceComponent tc = Tr.register(ManagedConnectionImpl.class);
	private static final TraceUtil tu = new TraceUtil(tc);

	private static Boolean printedUnspecOTMAClientNameMsg = Boolean.FALSE;
	private static Object  printedUnspecOTMAClientNameMsg_LOCK = new Object();

	/**
	 * This variable holds the connection ID counter for the WOLA JCA
   * connections.  Each connection in the servant region gets a unique 
   * integer ID.  This is combined with the servant STOKEN to get a 12 byte
   * unique connection ID for this WAS server (controller + servants).
	 *
	 * It is important that all WOLA connection factories in this servant are
   * using the same counter to get the connection ID, making each ID 
   * unique.  The counter is stored here, because this class is loaded by
   * the bundle's class loader, and therefore all managed connection factories
   * will reference the same static variable.  If the counter were stored
   * in the managed connection factory (which makes some sense), we may end
   * up with multiple counters, since there can be a different class loader
   * for each resource adapter, and static variables are scoped to the
   * class loader.
	 */
	private static AtomicInteger connectionIdGenerator = /* @PM75316A */
	new AtomicInteger(0); /* @PM75316A */

	private boolean FALSE_ON_NULL = false; /* @PI28422 */
	private boolean TRUE_ON_NULL = true; /* @PI28422 */


	/**
	 * Trace header.
	 */
	private String header = new String(" !!ManagedConnection " + System.identityHashCode(this) + ": ");

	/**
	 * List of connections associated with this MangagedConnection.
	 */
	private LinkedList<ConnectionImpl> conList = new LinkedList<ConnectionImpl>();

	/**
	 * List of ConnectionEventListeners.  The listeners can be registed and notified
	 * of events such as connection close.
	 */
	private LinkedList<ConnectionEventListener> connectionEventListeners = null;

	/**
	 * Logging PrintWriter
	 */
	private PrintWriter log = null;

	/**
	 * Flag to indicate whether or not this object is destroyed. When it is
	 * destroyed, it can't be used.
	 */
	private boolean isDestroyed = false;

	/**
	 * Local transaction state flag
	 */
	private boolean _localTranStarted = false; /* @F003691A */

	/**
   * Has an interaction been driven yet in the transaction currently associated
   * with this managed connection?
	 */
	private boolean _firstMethodInTran = false; /* @F003691A */

	/**
	 * The unique connection ID
	 */
	private int _connectionID;

	/**
	 * The MVS user ID that we obtained using either:
	 *  1) The _username and _password
	 *  2) The runAs Subject if no _username was supplied.
	 */
	private String _mvsUserID = "        ";

	/**
	 * The connection request info
	 */
	private ConnectionRequestInfoImpl _crii = null; /* @580321A */

	/**
	 * Debug switch
	 */
	private boolean _debugMode;

	/**
	 * The register name used by this connection. If the register name is
   * specified on the connection request info, we use that, otherwise we
   * use the register name supplied by the MCF.
	 */
	private String _registerName = null; /* @F003691M */

	/**
	 * The register name provided by the MCF. This may differ from the register
	 * name we are using in this connection.
	 */
	private String _registerNameFromMCF = null; /* @F003691A */

	/**
	 * The OTMA group name used by this connection. If the group name is
   * specified on the connection request info, we use that, otherwise we
   * use the group name supplied by the MCF.
	 */
	private String _OTMAGroupID = null; /* @F003694A */

	/**
	 * The OTMA server name used by this connection. If the server name is
   * specified on the connection request info, we use that, otherwise we
   * use the server name supplied by the MCF.
	 */
	private String _OTMAServerName = null; /* @F003694A */

	/**
	 * The OTMA client name used by this connection.
	 */
	private String _OTMAClientName = null;

	/**
	 * The OTMA sync level used by this connection. If the sync level is
   * specified on the connection request info, we use that, otherwise we
   * use the sync level supplied by the MCF.
	 */
	private String _OTMASyncLevel = "0"; /* @F003694A */

	/**
   * The OTMA Send/Receive Max segments setting used by this connection.  If this is
   * specified on the connection request info, we use that, otherwise we
   * use the value supplied by the MCF.
	 */
	private int _OTMAMaxSegments = 0; /* @670111A */

	/**
   * The OTMA Receive Max message size setting used by this connection.  If this is
   * specified on the connection request info, we use that, otherwise we
   * use the value supplied by the MCF.
	 */
	private int _OTMAMaxRecvSize = 0; /* @670111A */

	/**
	 * The current use OTMA setting.
	 */
	private boolean _useOTMA = false; /* @682993A */

	/**
	 * OTMA requests messages are in LLZZ format.
	 */
	private int _OTMARequestLLZZ = 0; /* @F013381A */

	/**
	 * OTMA response messages are in LLZZ format.
	 */
	private int _OTMAResponseLLZZ = 0; /* @F013381A */

	/**
	 * Use CICS containers flag.
	 */
	private int _UseCICSContainer = 0; /* @F013381A */

	/**
	 * CICS Link task transaction ID.
	 */
	private String _LinkTaskTranID = null; /* @F013381A */

	/**
	 * CICS Link task request container ID.
	 */
	private String _LinkTaskReqContID = null; /* @F013381A */

	/**
	 * CICS Link task request container type.
	 */
	private int _LinkTaskReqContType = 0; /* @F013381A */

	/**
	 * CICS Link task response container ID.
	 */
	private String _LinkTaskRspContID = null; /* @F013381A */

	/**
	 * CICS Link task response container type.
	 */
	private int _LinkTaskRspContType = 0; /* @F013381A */

	/**
	 * CICS Link task channel ID.
	 */
	private String _LinkTaskChanID = null; /* @F014448A */

	/**
	 * CICS Link task channel type.
	 */
	private int _LinkTaskChanType = 0; /* @F014448A */

	/**
	 * Time in seconds to wait for client connection to arrive.
	 */
	private int _ConnectionWaitTimeout = 30; /* @F013381A */

	/**
	 * RRS Transactional flag - for WAS to IMS over OTMA.
	 */
	private boolean _RRSTransactional = false; /* @F014447A */

	/**
	 * Remote proxy information
	 */
	private RemoteProxyInformation _rpi = null; /* @F003705A */

	/**
     * The user ID to use when obtaining the MVS user ID for registrations
     * using security.
	 */
	private String _username = null; /* @F003705A */

	/**
	 * The password to use with the user ID.
	 */
	private String _password = null; /* @F003705A */

	/**
	 * XID for the current global transaction (if any)
	 */
	private Xid _xid = null; /* @F003691A */

	/**
	 * XAResource object associated with this ManagedConnection
	 */
	private XAResource _xares = null; /* @F003691A */

	/**
	 * The managed connection factory that created this connection.
	 */
	private ManagedConnectionFactoryImpl _mcf = null;
	
	/**
	 * Package-protected CTOR used only for unit testing.
	 * 
	 * @param blah - just making sure there's at least 1 parm
	 *               so as not to create a no-arg CTOR.
	 */
	ManagedConnectionImpl(int blah) {}

	/**
	 * Default Constructor.
	 */
	public ManagedConnectionImpl(boolean debugMode, ManagedConnectionFactoryImpl mcf,
			int connectionID,
			ConnectionRequestInfoImpl crii,
			RemoteProxyInformation rpi) throws SecurityException {

		tu.debug("ManagedConnectionImpl: constructor", debugMode);

		boolean zos = isZOS(); /* @F003705A */

		_crii = crii;
		_mcf = mcf;

		/*-----------------------------------------------------------------------*/
		/* Ignore the connection ID that came in from the MCF, and generate one. */
		/*-----------------------------------------------------------------------*/
		_connectionID = connectionIdGenerator.incrementAndGet(); /* @PM75316C */
		_debugMode = debugMode;
		_registerNameFromMCF = mcf.getRegisterName();

		/*-----------------------------------------------------------------------*/
		/* Make sure that the register name was specified, either in the MCF or  */
		/* in the connection spec. */
		/*-----------------------------------------------------------------------*/
		String registerName = null; /* @F003691C */

		if (crii != null) /* @F003691A */
		{ /* @F003691A */
			registerName = crii.getRegisterName(); /* @F003691A */
		} /* @F003691A */

		if ((registerName == null) || /* @F003691A */
				(registerName.length() == 0) || /* @F013381C */
				(registerName.trim().length() == 0)) { /* @F013381A */
			registerName = _registerNameFromMCF;
		} /* @F003691A */

		_registerName = registerName; /* @F003691A */

		tu.debug("ManagedConnectionImpl: register name: " + _registerName, debugMode);
		tu.debug("ManagedConnectionImpl: OTMAGroupIDFromMCF: " + mcf.getOTMAGroupID(), debugMode);
		tu.debug("ManagedConnectionImpl: OTMAServerNameFromMCF: " + mcf.getOTMAServerName(), debugMode);
		tu.debug("ManagedConnectionImpl: OTMAClientNameFromMCF: " + mcf.getOTMAClientName(), debugMode);
		tu.debug("ManagedConnectionImpl: OTMASyncLevelFromMCF: " + mcf.getOTMASyncLevel(), debugMode);

		/*-----------------------------------------------------------------------*/
		/* Sort out the OTMA parameters, which can be specified on the           */
		/* connection spec or on the MCF.                                        */
		/*-----------------------------------------------------------------------*/
		String OTMAGroupID = null; /* @F003694A */

		if (crii != null) /* @F003691A */
		{ /* @F003691A */
			OTMAGroupID = crii.getOTMAGroupID(); /* @F003694A */
			tu.debug("ManagedConnectionImpl: OTMAGroupID from crii: "+OTMAGroupID, debugMode);

		} /* @F003694A */

		if ((OTMAGroupID == null) ||
				(OTMAGroupID.length() == 0) ||                          /* @F013381C*/
				(OTMAGroupID.trim().length() == 0)) { /* @F013381A */

			tu.debug("ManagedConnectionImpl: OTMAGroupID not provided  use from MCF", debugMode);

			OTMAGroupID = mcf.getOTMAGroupID(); /* @F003694A */
		}

		if (OTMAGroupID != null) { /* @F003694A */
			if (!ConnectionRequestInfoImpl.OTMAGroupIDPattern.matcher(OTMAGroupID.trim()).matches())
			{
				throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKB0395E", new Object[] { "Group", OTMAGroupID, ConnectionRequestInfoImpl.OTMA_GROUPID_LENGTH } ));
			}

			crii.setUseOTMA(true); /* @F003694A */
			_useOTMA = true; /* @682993A */

			if (registerName == null) /* @F013381A */
			{ /* @F013381A */
				_registerName = OTMAGroupID; /* @F013381A */
			}
		} /* @F003694A */
		else
		{
			if (registerName == null) /* @F013381C */
			{ /* @F013381C */
				throw new IllegalArgumentException("Register name must be specified on the ManagedConnectionFactory or provided in the ConnectionSpec parameter of the ConnectionFactory.getConnection() call"); /* @F013381C*/
			} /* @F013381C */
		}

		_OTMAGroupID = OTMAGroupID; /* @F003694A */
		tu.debug("ManagedConnectionImpl: OTMAGroupID : "+OTMAGroupID, debugMode);

		String OTMAServerName = null; /* @F003694A */

		if (crii != null) /* @F003691A */
		{ /* @F003691A */
			OTMAServerName = crii.getOTMAServerName(); /* @F003694A */
		} /* @F003694A */

		if ((OTMAServerName == null) ||
				(OTMAServerName.length() == 0) ||                       /* @F013381C*/
				(OTMAServerName.trim().length() == 0)) { /* @F013381A */

			tu.debug("ManagedConnectionImpl: OTMAServerName not provided  use from MCF", debugMode);
			OTMAServerName = mcf.getOTMAServerName();
		} /* @F003694A */

		if (OTMAServerName != null) { /* @F003694A */
			if (!ConnectionRequestInfoImpl.OTMAServerNamePattern.matcher(OTMAServerName.trim()).matches())
			{
				throw new IllegalArgumentException(Tr.formatMessage(tc, "CWWKB0395E", new Object[] { "Server", OTMAServerName, ConnectionRequestInfoImpl.OTMA_SERVER_NAME_LENGTH } ));
			}

			crii.setUseOTMA(true); /* @F003694A */
		} /* @F003694A */

		_OTMAServerName = OTMAServerName; /* @F003694A */
		tu.debug("ManagedConnectionImpl: OTMAServerName : "+OTMAServerName, debugMode);

		String OTMAClientNameFromCRII = null;
		if (crii != null) 
		{
			OTMAClientNameFromCRII = crii.getOTMAClientName();
		}

		if ((OTMAClientNameFromCRII != null) && (OTMAClientNameFromCRII.trim().length() > 0)) {
			if (!ConnectionRequestInfoImpl.OTMAClientNamePattern.matcher(OTMAClientNameFromCRII.trim()).matches())
			{
				Tr.warning(tc, "CWWKB0393W", new Object[] { OTMAClientNameFromCRII, ConnectionRequestInfoImpl.OTMA_CLIENT_NAME_LENGTH });
			} 
			else 
			{
				_OTMAClientName = OTMAClientNameFromCRII;
			}
		}
		
		if (_OTMAClientName == null) 
		{
			String OTMAClientNameFromMCF = toUpperCase(mcf.getOTMAClientName());

			if (OTMAClientNameFromMCF == null) 
			{
				synchronized(printedUnspecOTMAClientNameMsg_LOCK) 
				{
					if ((crii != null) && (crii.getUseOTMA() == 1) && (printedUnspecOTMAClientNameMsg == false)) 
					{
						Tr.warning(tc, "CWWKB0392W");
						printedUnspecOTMAClientNameMsg = true;
					}
				}
				_OTMAClientName = ConnectionSpecImpl.DEFAULT_OTMA_CLIENT_NAME;
			}
			else if (ConnectionRequestInfoImpl.OTMAClientNamePattern.matcher(OTMAClientNameFromMCF.trim()).matches())
			{
				_OTMAClientName = OTMAClientNameFromMCF;
			}
			else 
			{
				/* I'm not sure this will ever run since we checked the MCF name when we built the MCF. */
				Tr.warning(tc, "CWWKB0393W", new Object[] { OTMAClientNameFromMCF, ConnectionRequestInfoImpl.OTMA_CLIENT_NAME_LENGTH });
				_OTMAClientName = ConnectionSpecImpl.DEFAULT_OTMA_CLIENT_NAME;
			}
		}

		// TODO: Need to restrict when this message comes out so we don't flood the log.
		//       An idea - keep a map for the names which we printed the message, and only print once per name.
		//Tr.info(tc, "CWWKB0394I", new Object[] { _OTMAClientName });
		tu.debug("The OTMA Client name (" + _OTMAClientName + ") will be used.", debugMode);

		String OTMASyncLevel = null; /* @F003694A */

		if (crii != null) /* @F003694A */
		{ /* @F003694A */
			OTMASyncLevel = crii.getOTMASyncLevel(); /* @F003694A */
		} /* @F003694A */

		if (OTMASyncLevel == null)
		{ /* @F003694A */
			tu.debug("ManagedConnectionImpl: OTMASyncLevel not provided  use from MCF", debugMode);

			OTMASyncLevel = mcf.getOTMASyncLevel();
		} /* @F003694A */

		_OTMASyncLevel = OTMASyncLevel; /* @F003694A */
		tu.debug("ManagedConnectionImpl: OTMASyncLevel : "+OTMASyncLevel, debugMode);

		int OTMAMaxSegments = -1; /* @F013381C */

		if (crii != null) /* @670111A */
		{ /* @670111A */
			if (crii.getOTMAMaxSegmentsFromCSI() == true) /* @F013381A */
				OTMAMaxSegments = crii.getOTMAMaxSegments(); /* @670111A */
		} /* @670111A */

		if (OTMAMaxSegments == -1) /* @F013381C */
		{ /* @670111A */
			tu.debug("ManagedConnectionImpl: OTMAMaxSegments not provided  use from MCF", debugMode);

			OTMAMaxSegments = mcf.getOTMAMaxSegments();
		} /* @670111A */

		_OTMAMaxSegments = OTMAMaxSegments; /* @670111A */
		tu.debug("ManagedConnectionImpl: OTMAMaxSegments : "+OTMAMaxSegments, debugMode);

		int OTMAMaxRecvSize = -1; /* @F013381C */

		if (crii != null) /* @670111A */
		{ /* @670111A */
			if (crii.getOTMAMaxRecvSizeFromCSI() == true) /* @F013381A */
				OTMAMaxRecvSize = crii.getOTMAMaxRecvSize(); /* @670111A */
		} /* @670111A */

		if (OTMAMaxRecvSize == -1)
		{ /* @670111A */
			tu.debug("ManagedConnectionImpl: OTMAMaxRecvSize not provided  use from MCF", debugMode);

			OTMAMaxRecvSize = mcf.getOTMAMaxRecvSize();
		} /* @670111A */

		int maxRecvSize = _OTMAMaxSegments *32768;
		if(OTMAMaxRecvSize > maxRecvSize ) {
			if(_useOTMA) {
				Tr.warning(tc, "CWWKB0397W", new Object[] { Integer.toString(OTMAMaxRecvSize), Integer.toString(_OTMAMaxSegments), Integer.toString(maxRecvSize) });
			}
			_OTMAMaxRecvSize = maxRecvSize; 
		} else { 
			_OTMAMaxRecvSize = OTMAMaxRecvSize;
		}
		tu.debug("ManagedConnectionImpl: OTMAMaxRecvSize : "+OTMAMaxRecvSize, debugMode);

		//
		//  Support added for all properties in MCF - for tooling feature F013381.
		//
		int OTMARequestLLZZ = -1; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			if (crii.getOTMARequestLLZZFromCSI() == true) /* @F013381A */
				OTMARequestLLZZ = crii.getOTMAReqLLZZ(); /* @F013381A */
		} /* @F013381A */

		if (OTMARequestLLZZ == -1) /* @F013381C */
		{ /* @F013381A */
			tu.debug("ManagedConnectionImpl: OTMARequestLLZZ not provided use from MCF", debugMode);

			OTMARequestLLZZ = mcf.getOTMARequestLLZZ();
		} /* @F013381A */

		_OTMARequestLLZZ = OTMARequestLLZZ; /* @F013381A */
		tu.debug("ManagedConnectionImpl: OTMARequestLLZZ : "+OTMARequestLLZZ, debugMode);

		int OTMAResponseLLZZ = -1; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			if (crii.getOTMAResponseLLZZFromCSI() == true) /* @F013381A */
				OTMAResponseLLZZ = crii.getOTMARespLLZZ(); /* @F013381A */
		} /* @F013381A */

		if (OTMAResponseLLZZ == -1) /* @F013381C */
		{ /* @F013381A */
			tu.debug("ManagedConnectionImpl: OTMAResponseLLZZ not provided  use from MCF", debugMode);

			OTMAResponseLLZZ = mcf.getOTMAResponseLLZZ();
		} /* @F013381A */

		_OTMAResponseLLZZ = OTMAResponseLLZZ; /* @F013381A */
		tu.debug("ManagedConnectionImpl: OTMAResponseLLZZ : "+OTMAResponseLLZZ, debugMode);

		int UseCICSContainer = -1; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			if (crii.getuseCICSContainerFromCSI() == true) /* @F013381A */
				UseCICSContainer = crii.getUseCICSContainer(); /* @F013381A */
		} /* @F013381A */

		if (UseCICSContainer == -1) /* @F013381C */
		{ /* @F013381A */
			tu.debug("ManagedConnectionImpl: UseCICSContainer not provided  use from MCF", debugMode);

			UseCICSContainer = mcf.getUseCICSContainer();
		} /* @F013381A */

		_UseCICSContainer = UseCICSContainer; /* @F013381A */
		tu.debug("ManagedConnectionImpl: UseCICSContainer : "+UseCICSContainer, debugMode);

		String LinkTaskTranID = null; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			LinkTaskTranID = crii.getLinkTaskTranID(); /* @F013381A */
		} /* @F013381A */

		if ((LinkTaskTranID == null) ||
				(LinkTaskTranID.length() == 0)  ||                       /* @F013381C*/
				(LinkTaskTranID.trim().length() == 0)) {                 /* @F013381C*/

			tu.debug("ManagedConnectionImpl: LinkTaskTranID not provided  use from MCF", debugMode);
			LinkTaskTranID = mcf.getLinkTaskTranID();
		} /* @F013381A */

		_LinkTaskTranID = LinkTaskTranID; /* @F013381A */
		tu.debug("ManagedConnectionImpl: LinkTaskTranID : "+LinkTaskTranID, debugMode);

		String LinkTaskReqContID = null; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			LinkTaskReqContID = crii.getLinkTaskReqContID(); /* @F013381A */
		} /* @F013381A */

		if ((LinkTaskReqContID == null) ||
				(LinkTaskReqContID.length() == 0) ||                     /* @F013381C*/
				(LinkTaskReqContID.trim().length() == 0)) {              /* @F013381A*/

			tu.debug("ManagedConnectionImpl: LinkTaskReqContID not provided  use from MCF", debugMode);
			LinkTaskReqContID = mcf.getLinkTaskReqContID();
		} /* @F013381A */

		_LinkTaskReqContID = LinkTaskReqContID; /* @F013381A */
		tu.debug("ManagedConnectionImpl: LinkTaskReqContID : "+LinkTaskReqContID, debugMode);

		int LinkTaskReqContType = -1; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			if (crii.getlinkTaskReqContTypeFromCSI() == true) /* @F013381A */
				LinkTaskReqContType = crii.getLinkTaskReqContType(); /* @F013381A */
		} /* @F013381A */

		if (LinkTaskReqContType == -1) /* @F013381C */
		{ /* @F013381A */
			tu.debug("ManagedConnectionImpl: LinkTaskReqContType not provided  use from MCF", debugMode);

			LinkTaskReqContType = mcf.getLinkTaskReqContType();
		} /* @F013381A */

		_LinkTaskReqContType = LinkTaskReqContType; /* @F013381A */
		tu.debug("ManagedConnectionImpl: LinkTaskReqContType : "+LinkTaskReqContType, debugMode);

		String LinkTaskRspContID = null; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			LinkTaskRspContID = crii.getLinkTaskRspContID(); /* @F013381A */
		} /* @F013381A */

		if ((LinkTaskRspContID == null) ||
				(LinkTaskRspContID.length() == 0) ||                     /* @F013381C*/
				(LinkTaskRspContID.trim().length() == 0)) {              /* @F013381A*/

			tu.debug("ManagedConnectionImpl: LinkTaskRspContID not provided  use from MCF", debugMode);
			LinkTaskRspContID = mcf.getLinkTaskRspContID();
		} /* @F013381A */

		_LinkTaskRspContID = LinkTaskRspContID; /* @F013381A */
		tu.debug("ManagedConnectionImpl: LinkTaskRspContID : "+LinkTaskRspContID, debugMode);

		int LinkTaskRspContType = -1; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			if (crii.getlinkTaskRspContTypeFromCSI() == true) /* @F013381A */
				LinkTaskRspContType = crii.getLinkTaskRspContType(); /* @F013381A */
		} /* @F013381A */

		if (LinkTaskRspContType == -1) /* @F013381C */
		{ /* @F013381A */
			tu.debug("ManagedConnectionImpl: LinkTaskRspContType not provided  use from MCF", debugMode);

			LinkTaskRspContType = mcf.getLinkTaskRspContType();
		} /* @F013381A */

		_LinkTaskRspContType = LinkTaskRspContType; /* @F013381A */
		tu.debug("ManagedConnectionImpl: LinkTaskRspContType : "+LinkTaskRspContType, debugMode);

		String LinkTaskChanID = null; /* @F014448A */

		if (crii != null) /* @F014448A */
		{ /* @F014448A */
			LinkTaskChanID = crii.getLinkTaskChanID(); /* @F014448A */
		} /* @F014448A */

		if ((LinkTaskChanID == null) ||
				(LinkTaskChanID.length() == 0) ||                        /* @F014448A */
				(LinkTaskChanID.trim().length() == 0)) {                 /* @F014448A */

			tu.debug("ManagedConnectionImpl: LinkTaskChanID not provided  use from MCF", debugMode);
			LinkTaskChanID = mcf.getLinkTaskChanID();
		} /* @F014448A */

		_LinkTaskChanID = LinkTaskChanID; /* @F014448A */
		tu.debug("ManagedConnectionImpl: LinkTaskChanID : "+LinkTaskChanID, debugMode);

		int LinkTaskChanType = -1; /* @F014448A */

		if (crii != null) /* @F014448A */
		{ /* @F014448A */
			if (crii.getlinkTaskChanTypeFromCSI() == true) /* @F014448A */
				LinkTaskChanType = crii.getLinkTaskChanType(); /* @F014448A */
		} /* @F014448A */

		if (LinkTaskChanType == -1) /* @F014448A */
		{ /* @F014448A */
			tu.debug("ManagedConnectionImpl: LinkTaskChanType not provided  use from MCF", debugMode);

			LinkTaskChanType = mcf.getLinkTaskChanType();
		} /* @F014448A */

		_LinkTaskChanType = LinkTaskChanType; /* @F014448A */
		tu.debug("ManagedConnectionImpl: LinkTaskChanType : "+LinkTaskChanType, debugMode);

		int ConnectionWaitTimeout = -1; /* @F013381A */

		if (crii != null) /* @F013381A */
		{ /* @F013381A */
			if (crii.getConnectionWaitTimeoutFromCSI() == true) /* @F013381A */
				ConnectionWaitTimeout = crii.getConnectionWaitTimeout(); /* @F013381A */
		} /* @F013381A */

		if (ConnectionWaitTimeout == -1) /* @F013381C */
		{ /* @F013381A */
			tu.debug("ManagedConnectionImpl: ConnectionWaitTimeout not provided  use from MCF", debugMode);

			ConnectionWaitTimeout = mcf.getConnectionWaitTimeout();
		} /* @F013381A */

		_ConnectionWaitTimeout = ConnectionWaitTimeout; /* @F013381A */

		tu.debug("ManagedConnectionImpl: ConnectionWaitTimeout : "+ConnectionWaitTimeout, debugMode);


		boolean RRSTransactional = false; /* @F014447A */

		if (crii != null) /* @F014447A */
		{ /* @F014447A */
			if (crii.getRRSTransactionalFromCSI() == true) /* @F014447A */
				RRSTransactional = crii.getRRSTransactional(); /* @F014447A */
			else
				RRSTransactional = mcf.getRRSTransactional();
		} /* @F014447A */
		else {
			tu.debug("ManagedConnectionImpl: RRSTransactional not provided  use from MCF", debugMode);
			RRSTransactional = mcf.getRRSTransactional();
		} /* @F014447A */

		_RRSTransactional = RRSTransactional; /* @F014447A */

		tu.debug("ManagedConnectionImpl: RRSTransactional : "+RRSTransactional, debugMode);

		_rpi = rpi; /* @703982M */

		/*-----------------------------------------------------------------------*/
		/* Make sure that the proper remote parameters were specified if running */
		/* on a non-z/OS platform. */
		/*-----------------------------------------------------------------------*/
		if ((zos == false) && (_rpi == null)) /* @703982C */
		{
			throw new IllegalArgumentException("Remote hostname must be specified on the ManagedConnectionFactory when running on a non-z/OS platform");
		}

		/*-----------------------------------------------------------------------*/
		/* Make sure that if the remote proxy information was specified, that it */
		/* is complete. */
		/*-----------------------------------------------------------------------*/
		if (_rpi != null) /* @703982C */
		{
			if ((_rpi.getHostname() == null) ||
					(_rpi.getHostname().length() == 0))
			{
				throw new IllegalArgumentException("Remote hostname must be specified on the ManagedConnectionFactory when using the OLA Proxy EJB");
			}

			int port = _rpi.getPort();

			if (port <= 0)
			{
				throw new IllegalArgumentException("Remote port specification (" + port + ") is invalid when using the OLA Proxy EJB.  Please specify a valid remote port number in the ManagedConnectionFactory");
			}

			String remoteJNDI = _rpi.getJNDIName();

			if ((remoteJNDI == null) || (remoteJNDI.length() == 0))
			{
				throw new IllegalArgumentException("Remote JNDI name must be specified on the ManagedConnectionFactory when using the OLA Proxy EJB");
			}
		}

		/*----------------------------------------------------------------------*/
		/* We will authenticate and set the MVS user ID on the getConnection()  */
		/* request.  This allows us to support connection re-authentication and */
		/* makes it easier for the connection manager to pool connections.      */
		/*----------------------------------------------------------------------*/

		/*-----------------------------------------------------------------------*/
		/* Create an empty array of connection event listeners. */
		/*-----------------------------------------------------------------------*/
		connectionEventListeners = new LinkedList<ConnectionEventListener>();

		tu.debug(header + "<init>,", debugMode);
		tu.debug("registerName = " + _registerName, debugMode);
		tu.debug("connectionID = " + _connectionID, debugMode);
		tu.debug("OTMAGroupID = " + _OTMAGroupID, debugMode);
		tu.debug("OTMAServerName = " + _OTMAServerName, debugMode);
		tu.debug("OTMAClientName = " + _OTMAClientName, debugMode);
		tu.debug("OTMASyncLevel = " + _OTMASyncLevel, debugMode);
		tu.debug("OTMAMaxSegments = " + _OTMAMaxSegments, debugMode);
		tu.debug("OTMAMaxRecvSize = " + _OTMAMaxRecvSize, debugMode);
		tu.debug("ManagedConnectionImpl: leaving!", debugMode);

	}

	private String toUpperCase(String mayNotBeUpperCase) {
		return (mayNotBeUpperCase != null) ? mayNotBeUpperCase.toUpperCase() : null;
	}

	/**
	 * Gets a connection to correspond to this ManagedConnection. There can be
     * more than one ConnectionImpl object associated with this ManagedConnection
     * at any one time.  When a Connection is closed, it is disassociated from 
     * the ManagedConnection.  If the connection manager decides to
	 * re-use this ManagedConnection due to a MatchManagedConnection call on the
	 * MCF, we will create a new connection here.
	 * @see javax.resource.spi.ManagedConnection#getConnection(Subject, ConnectionRequestInfo)
	 */
	public Object getConnection(Subject subject, ConnectionRequestInfo cri)
		throws ResourceException
	{
		tu.debug(header + " getConnection() called", _debugMode); /* @F003691C*/

		if (isDestroyed)
			throw new ResourceException("Cannot get a connection from this ManagedConnection because it has been destroyed.");

		if ((cri != null) && ((cri instanceof ConnectionRequestInfoImpl) == false))
			throw new ResourceException("ConnectionRequestInfo instance is not the correct type");
		
		// Begin authenticating the user.  Figure out which user ID and password to use.
		// The order of precedence is :  
		//  1) Userid/password on Connection Spec,  
	    //  2) Userid/password in Subject passed to createManagedConnection(),
		//  3) Userid/password on MCF.
		//
		// NOTE:  The ID/PWSD in the subject will be the ID/PSWD from the container managed
	    //        JAAS Alias if one is specified for the CF and in the application's DD.
		String effectiveUsername = _mcf.getUsername();
		String effectivePassword = _mcf.getPassword();
		
	    if (subject != null) {
	    	// With feature F013381 we support providing the UserID and Password to be 
			// used on the z/OS side using a J2C JAAS alias.
	    	String usernameFromSubject = null;
	    	String passwordFromSubject = null;
			
	        PasswordCredential pCred = extractPasswordCredential(subject);
				                                          
			if(pCred != null) {                                          /* @F013381A*/                  
				usernameFromSubject = pCred.getUserName();   
				char[] passwordChar = pCred.getPassword();
				passwordFromSubject = (passwordChar != null) ? new String(passwordChar) : null;
			} else {                                                       /* @F013381A*/
				tu.debug("ManagedConnectionImpl: getConnection() private credential from subject is null.", _debugMode);
			}                                                               /* @F013381A*/

			if ((usernameFromSubject != null) && (usernameFromSubject.length() > 0)) {
				effectiveUsername = usernameFromSubject;                      /* @PM89577C*/
				effectivePassword = passwordFromSubject;                      /* @PM89577C*/

				tu.debug("ManagedConnectionImpl: getConnection() use ID from Subject: " + usernameFromSubject, _debugMode);
			}
		}

	    if (cri != null) {
	    	ConnectionRequestInfoImpl crii = (ConnectionRequestInfoImpl)cri;
	    	String userNameFromCRII = crii.getUsername();
	    	String passwordFromCRII = crii.getPassword();
	    	if ((userNameFromCRII != null) && (userNameFromCRII.length() > 0)) {
	    		effectiveUsername = userNameFromCRII;
	    		effectivePassword = passwordFromCRII;
	    		tu.debug("ManagedConnectionImpl: getConnection() use ID from CRII: " + userNameFromCRII,  _debugMode);
	    	}
	    }
		
	    // Check to see if this is the same username and password we authenticated with last time.
	    // If it is, we already have the MVS user ID that we need.  If not, go ahead and authenticate
	    // and extract the MVS user ID.
	    if ((compareStrings(effectiveUsername, _username, FALSE_ON_NULL)) &&
	    	(compareStrings(effectivePassword, _password, FALSE_ON_NULL))) {
	    	tu.debug("ManagedConnectionImpl: getConnection() already authenticated user: " + _username + " MVS User ID: " + _mvsUserID, _debugMode);
	    } else {
	    	_mvsUserID = extractMvsUserId(effectiveUsername, effectivePassword);
	    	_username = effectiveUsername;
	    	_password = effectivePassword;
	    }
		
		// TODO: Someday if connection management supports connection factory failover, we should add back
		//       the code from WAS traditional which checks for an RGE or for an OTMA anchor.

		ConnectionImpl con = new ConnectionImpl(this);
		conList.add(con);

		return con;
	}

	/** Extracts the password credential for our MCF from the subject. */
	private PasswordCredential extractPasswordCredential(Subject subject) /* @PM89577A*/
	{
		Set<Object> privateCreds = subject.getPrivateCredentials(); 
	    PasswordCredential pCred = null;
	      
	    Iterator<Object> itr = (privateCreds != null) ? privateCreds.iterator() : null;

	    while ((itr != null) && (itr.hasNext()) && (pCred == null)) {
	    	Object testCred = itr.next();                
	    	if (testCred instanceof PasswordCredential) {
	    		ManagedConnectionFactory testMcf =         
	    			((PasswordCredential)testCred).getManagedConnectionFactory();
	    		if ((testMcf != null) && (testMcf.equals(_mcf))) { 
	    			tu.debug("ManagedConnectionImpl: extractPasswordCredential() " + 
	    					 "private credential is present.", _debugMode);
	          
	    			pCred = (PasswordCredential) testCred;           
	    		}                                                  
	    	}                                                    
	    }                                                      

	    return pCred;
	}

	/**
	 * Tells WAS connection managment whether or not the initial connection that was unavailable
	 * earlier is available yet.  Calls findRGE() method passing the register name each time. If 
	 * the registration is available, it returns indicating a switch back can occur.
	 * @see javax.resource.spi.ManagedConnection#testConnection()
	 */
//	public boolean testConnection()
//	{
//		boolean foundRGE = false;
//
//		tu.debug(header + "testConnection() called", _debugMode); /* @F003709A*/
//
//		if ((isZOS() == true) && (_rpi == null)) /* @703982C */
//		{
//			if (_useOTMA == false) {
//        tu.debug(header + " testConnection() findRGE being called for register name "+_registerName, _debugMode); /* @F003709A*/
//
//				try {
//          foundRGE = findRGE(_registerName.getBytes("Cp1047"), 
//                             _registerName.length());                           /* @F003709A*/
//        }	
//        catch (java.io.UnsupportedEncodingException uee)
//        {
//          tu.debug("testConnection() Caught UnsupportedEncodingException exception.", _debugMode); /* @F003709A*/
//        }	
//        catch (OLANativeException one)
//        {
//          tu.debug("testConnection() Caught OLANativeException exception.", _debugMode); /* @F003709A*/
//				}
//
//			
//        tu.debug(header + "testConnection() findRGE returned with result foundRGE: " + foundRGE, _debugMode); /* @F003709A*/
//			} else {
//        // For OTMA, try to establish a connection to the IMS control region.
//				try { /* @745768A */
//					if ((_OTMAGroupID == null) || (_OTMAServerName == null)) { /* @745768A */
//            throw new ResourceException("OTMA group ID and server name must be specified on the ConnectionFactory or in the ConnectionSpec"); /* @745768A*/
//					} /* @745768A */
//					/* @745768A */
//					OLAIMSOTMAKeyMap OTMAKeyMap = OLAIMSOTMAKeyMap.getInstance(); /* @745768A */
//					OLAIMSOTMAKeyMap.Key otmaAnchorKey = new OLAIMSOTMAKeyMap.Key(_OTMAGroupID, _OTMAServerName,
//							_OTMAClientName); /* @745768A */
//					foundRGE = (OTMAKeyMap.getOTMAAnchorKey(otmaAnchorKey) != null); /* @745768A */
//				} catch (Throwable t) { /* @745768A */
//          tu.debug(header + " testConnection() caught throwable, " + t.toString(), _debugMode); /* @745768A*/
//				}
//			}
//    }
//    else
//    {
//			foundRGE = true;
//		}
//
//		return foundRGE;
//	}


	/**
	 * Called by connection managment when it is going to eject this connection
	 * from the pool. This method should close all connections to backends, etc.
	 * @see javax.resource.spi.ManagedConnection#destroy()
	 */
	public void destroy() throws ResourceException
	{
		tu.debug(header + "destroy called", _debugMode); /* @F003691C*/

		isDestroyed = true;
	}

	/**
	 * Called by connection managment to clean up all state data so that this
	 * connection can be re-used.
	 * @see javax.resource.spi.ManagedConnection#cleanup()
	 */
	public void cleanup() throws ResourceException
	{
		tu.debug(header + "cleanup called", _debugMode); /* @F003691C*/
		Iterator<ConnectionImpl> i = conList.iterator();
    while (i.hasNext())
    {
			ConnectionImpl con = i.next();
			con.invalidate(this);
		}

		conList.clear();

		_firstMethodInTran = true;
		_localTranStarted = false;
		_xid = null;
	}

	/**
	 * Associates this Managed connection with the connection handle passed
	 * in by the connection manager.  Not quite sure how this works.  Did what
	 * made sense.  Might be wrong.
	 * @see javax.resource.spi.ManagedConnection#associateConnection(Object)
	 */
	public void associateConnection(Object arg0) throws ResourceException
	{
		/* Not entirely sure what this method does... */
		tu.debug(header + "associateConnection called with " + arg0, _debugMode); /* @F003691C*/

		if (isDestroyed)
			throw new ResourceException("Attempt to associate a Connection to a destroyed ManagedConnection");

		if (arg0 instanceof ConnectionImpl)
		{
			ConnectionImpl con = (ConnectionImpl) arg0;

			con.changeAssociation(this);

      if (conList.contains(con) == false) conList.add(con);
		}
		else throw new ResourceException("Attempt to associate ManagedConnection with " + arg0);
	}

	/**
	 * Removes a connection fron the connection list, as a result of calling
	 * changeAssociation().
	 */
  protected void dissociateConnection(ConnectionImpl con)
  {
    tu.debug(header + "dissociateConnection call", _debugMode);

    if (conList.contains(con)) conList.remove(con);
	}

	/**
	 * Adds a connection event listener with the ManagedConnection.  Gives
	 * events like connection_closed.  Someday will also give local transaction
	 * events, but not yet.
	 * @see javax.resource.spi.ManagedConnection#addConnectionEventListener(ConnectionEventListener)
	 */
	public void addConnectionEventListener(ConnectionEventListener listener)
	{
		tu.debug(header + "addConnectionEventListener called", _debugMode); /* @F003691C*/
		connectionEventListeners.add(listener);
	}

	/**
	 * Removes a connection event listener from the ManagedConnection. Iterates
	 * over the list and removes all listeners that match listener.equals(arg0).
	 * @see javax.resource.spi.ManagedConnection#removeConnectionEventListener(ConnectionEventListener)
	 */
	public void removeConnectionEventListener(ConnectionEventListener listener)
	{
		tu.debug(header + "removeConnectionEventListener called", _debugMode); /* @F003691C*/
		ConnectionEventListener candidate = null;

		for (int x = connectionEventListeners.size() - 1; x >= 0; x--)
		{
			candidate = connectionEventListeners.get(x);
			if (candidate.equals(listener))
			{
				connectionEventListeners.remove(x);
				x = -1;
			}
		}
	}

	/**
	 * Gets the XAResource instance associated with this ManagedConnection
	 * @see javax.resource.spi.ManagedConnection#getXAResource()
	 */
	public synchronized XAResource getXAResource() throws ResourceException
	{
    tu.debug(header + "getXAResource called", _debugMode); /* @F003691A*/

		if (_xares == null) /* @F003691A */
		{
			/*---------------------------------------------------------------------*/
      /* Make sure the register name we are using is the same one that was   */
      /* specified on the ManagedConnectionFactory.  This is the register    */
      /* name that we are going to log.  The customer may have over-ridden   */
			/* it on the ConnectionRequestInfo object, which is invalid for a */
			/* global transaction. */
			/*---------------------------------------------------------------------*/
			if ((_registerNameFromMCF == null) || /* @F003691A */
					(_registerNameFromMCF.length() == 0)) /* @F003691A */
			{ /* @F003691A */
        throw new ResourceException("XAResource unavailable because register name was not specified on the ManagedConnectionFactory object"); /* @F003691A*/
			} /* @F003691A */

			if (_registerName.trim().equals(_registerNameFromMCF.trim()) == false) /* @F003691A */
			{ /* @F003691A */
        throw new ResourceException("XAResource unavailable because register name " + _registerName + " was specified by the client, but register name " + _registerNameFromMCF + " was specified on the ManagedConnectionFactory object."); /* @F003691A*/
			} /* @F003691A */

      _xares = new XAResourceImpl(this, _registerNameFromMCF, 
                                  _debugMode);                   /* @F003691C*/
		}

		return _xares; /* @F003691A */
	}

	/**
	 * Gets a LocalTransaction object.  This interface used by the application server
	 * to demarcate RMLTs and global transactions as the last agent.
	 * @see javax.resource.spi.ManagedConnection#getLocalTransaction()
	 */
	public LocalTransaction getLocalTransaction() throws ResourceException
	{
		tu.debug(header + "getLocalTransaction called", _debugMode); /* @F003691C*/

		// TODO: Before handing this out, we should make sure that the RGE supports
		// transactions. If not, we should throw an exception instead.
		return new LocalTransactionSpiImpl(this); /* @F003691C */
	}

	/**
	 * Gets the meta data for the Managed Connection.
	 * @see javax.resource.spi.ManagedConnection#getMetaData()
	 */
	public ManagedConnectionMetaData getMetaData() throws ResourceException
	{
		tu.debug(header + "getMetaData called", _debugMode); /* @F003691C*/

		String trimmedUserID = (_mvsUserID == null) ? null : _mvsUserID.trim(); /* @PM89577A */
		String mvsUserID = ((trimmedUserID == null) || /* 2@PM89577A */
				(trimmedUserID.length() == 0)) ? null : trimmedUserID;

		return new ManagedConnectionMetaDataImpl(mvsUserID); /* @PM89577C */
	}

	/**
	 * Sets the log writer for this connection.  Might have to get this from
	 * the ManagedConnectionFactory by default.  Not sure.
	 * @see javax.resource.spi.ManagedConnection#setLogWriter(PrintWriter)
	 */
	public void setLogWriter(PrintWriter arg0) throws ResourceException
	{
		tu.debug(header + "setLogWriter called, " + arg0, _debugMode); /* @F003691C*/

		log = arg0;
	}

	/**
	 * Gets the log writer for this connection.
	 * @see javax.resource.spi.ManagedConnection#getLogWriter()
	 */
	public PrintWriter getLogWriter() throws ResourceException
	{
		tu.debug(header + "getLogWriter called", _debugMode); /* @F003691C*/

		return log;
	}


	/**
	 * Sends a connection event to all registered ConnectionEventListeners
	 */
	private void sendConnectionEvent(int eventType)
	{
		sendConnectionEvent(eventType, null);
	}

	/**
	 * Sends a connection event to all registered ConnectionEventListeners
	 */
	private void sendConnectionEvent(int eventType, ConnectionImpl con)
	{
		tu.debug(header + "sendConnectionEvent called, " + eventType, _debugMode); /* @F003691C*/

		/* Place to iterate the listeners */
		ConnectionEventListener listener = null;

		/* Create a connection event */
		ConnectionEvent conEvent = new ConnectionEvent(this, eventType);
    if (con != null) conEvent.setConnectionHandle(con);

		/* Iterate over the listeners and pass the close */
		for (int x = connectionEventListeners.size() - 1; x >= 0; x--)
		{
			listener = (ConnectionEventListener) connectionEventListeners.get(x);

			/* It's not enough that the connection event has the type in it. */
			/* We have to call the appropriate method on the listener. */
			switch (eventType) /* @F003691A */
			{ /* @F003691A */
			case ConnectionEvent.CONNECTION_CLOSED: /* @F003691A */
				listener.connectionClosed(conEvent); /* @F003691A */
				break; /* @F003691A */
			case ConnectionEvent.LOCAL_TRANSACTION_STARTED: /* @F003691A */
				listener.localTransactionStarted(conEvent); /* @F003691A */
				break; /* @F003691A */
			case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED: /* @F003691A */
				listener.localTransactionCommitted(conEvent); /* @F003691A */
				break; /* @F003691A */
			case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK: /* @F003691A */
				listener.localTransactionRolledback(conEvent); /* @F003691A */
				break; /* @F003691A */
			case ConnectionEvent.CONNECTION_ERROR_OCCURRED: /* @F003691A */
				listener.connectionClosed(conEvent); /* @F003691A */
				break; /* @F003691A */
			} /* @F003691A */
		}
	}
	

	/**
	 * Called by ConnectionImpl when the connection is closed. Notify all
	 * connection event listeners that the connection is closing.
	 */
	protected void connectionClosed(ConnectionImpl con)
	{
		tu.debug(header + "connectionClosed called", _debugMode); /* @F003691C*/

		sendConnectionEvent(ConnectionEvent.CONNECTION_CLOSED, con);

		/* Disassociate from connection */
		conList.remove(con);
	}

	/**
	 * Tells the connectionEventListeners that an application has started a local
	 * transaction.
	 */
	protected void localTransactionStarted(boolean isClientApi)
		throws LocalTransactionException
	{
		tu.debug(header + "localTransactionStarted called", _debugMode); /* @F003691C*/

		/* Make sure the container or app did not already start a tran */
		if (_localTranStarted) /* @F003691C */
		{
			throw new LocalTransactionException("A local transaction is already associated with this managed connection");
		}

		/* Make sure there is not a global present */
		if (_xid != null) /* @F003691A */
		{ /* @F003691A */
      throw new LocalTransactionException("A global transaction is already associated with this managed connection"); /* @F003691A*/
		} /* @F003691A */

		/* Indicate that we started the tran */
		_localTranStarted = true; /* @F003691C */
		_firstMethodInTran = true; /* @F003691A */

		/* If the client called this API, notify the event listeners.  We don't */
		/* do this for server-started trans, per the JCA spec. */
		if (isClientApi)
		{
			sendConnectionEvent(ConnectionEvent.LOCAL_TRANSACTION_STARTED);
		}

    /* Notify user that the transaction is ignored on distributed   @F003705A*/
		if ((isZOS() == false) || (_rpi != null)) /* @703982C */
		{ /* @F003705A */
			OptConnOutboundUtil.issueProxyMessageMethod(); /* @F003705A */
		} /* @F003705A */
	}


	/**
	 * Tells the connectionEventListeners that an application has committed a local
	 * transaction.
	 */
	protected void localTransactionEnded(boolean isClientApi, boolean commit)
		throws LocalTransactionException
	{
		tu.debug(header + "localTransactionEnded called", _debugMode); /* @F003691C*/

		/* Make sure there is a tran running */
		if (_localTranStarted == false) /* @F003691C */
		{
			throw new LocalTransactionException("There is no transaction associated with this managed connection");
		}

		_localTranStarted = false; /* @F003691C */

		if (isClientApi)
		{
			int eventType = (commit ? ConnectionEvent.LOCAL_TRANSACTION_COMMITTED : ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK);
			sendConnectionEvent(eventType);
		}

		/* Only do OLA XA transactional notification on connections that are not     */
		/* running under RRS (IMS OTMA 2PC transactions). @F014447A */
		if (this.getRRSTransactional() == false) { /* @F014447A */

			String regname = getRegisterName(); /* @F003691A */

			if (regname == null) /* @F003691A */
			{ /* @F003691A */
				throw new LocalTransactionException("Cannot complete transaction, OLA register name is null"); /* @F003691A*/
			} /* @F003691A */

			try
			{
				if ((_firstMethodInTran == false) && (isZOS() == true) && (_rpi == null)) /* @703982C */
				{ /* @F003691A */
					byte[] regnameBytes = regname.getBytes("Cp1047"); /* @F003691A */
					OptConnOutboundUtil.notifyTransaction(regnameBytes.length, regnameBytes, _connectionID, 0, null, commit, false); /* @F003691C*/
				} /* @F003691A */
			}
			catch (Throwable t)
			{
				throw new LocalTransactionException(t); /* @F003691C */
			}
		}
	} /* @F014447A */


	/**
	 * Sets the Xid associated with the global transaction which this connection
	 * is currently associated with.
	 */
	protected void setXid(Xid xid, boolean associate) /* @F003691A */
    throws ResourceException
  {
		tu.debug(header + "setXid called, " + associate + ", " + xid, _debugMode);

    if (xid == null)
    {
			throw new IllegalArgumentException("Xid supplied to setXid was null");
		}

    if (associate)
    {
			/* Make sure there is not a local transaction running */
      if (_localTranStarted == true)
      {
        throw new ResourceException("Can not associate an Xid with this managed connection because it is already associated with a local transaction");
			}

			/* Make sure there is not another global transaction running */
      if (_xid != null)
      {
        throw new ResourceException("Can not associate an Xid with this managed connection because it is already associated with a global transaction");
			}

			_xid = xid;
			_firstMethodInTran = true;
    }
    else
    {
			/* Make sure we were associated with the transaction */
      if (_xid == null)
      {
        throw new ResourceException("An attempt to end a transaction branch failed because this ManagedConnection was not associated with a transaction branch");
			}

      if (_xid.equals(xid) == false)
      {
        throw new ResourceException("ManagedConnection was not associated with the Xid representing the transaction branch which is ending");
			}

			_xid = null;
		}
	}


	/**
	 * Gets the Xid which is currently associated with the ManagedConnection.
	 */
	protected Xid getXid() /* @F003691A */
	{
		return _xid;
	}


	/**
	 * Gets the register name
	 */
	protected String getRegisterName()
	{
		return _registerName; /* @F003691C */
	}

	/**
	 * Gets the connection ID
	 */
	public int getConnectionID()
	{
		return _connectionID;
	}

	/**
	 * Gets the connection timeout time (in seconds).
	 */
	public int getConnectionWaitTimeout() 
	{
		return _ConnectionWaitTimeout; /* @F013381C */
	}

	/**
	 * Gets the link task tran ID.
	 */
	public String getLinkTaskTranID() 
	{
		return _LinkTaskTranID; /* @F013381C */
	}

	/**
	 * Gets the link task request container ID.
	 */
	public String getLinkTaskReqContID() 
	{
		return _LinkTaskReqContID; /* @F013381C */
	}

	/**
	 * Gets the link task request container type.
	 */
	public int getLinkTaskReqContType() 
	{
		return _LinkTaskReqContType; /* @F013381C */
	}

	/**
	 * Gets the link task response container ID.
	 */
	public String getLinkTaskRspContID() 
	{
		return _LinkTaskRspContID; /* @F013381C */
	}

	/**
	 * Gets the link task response container type.
	 */
	public int getLinkTaskRspContType() 
	{
		return _LinkTaskRspContType; /* @F013381C */
	}

	/**
	 * Gets the link task channel ID.
	 */
	public String getLinkTaskChanID()
	{
		return _LinkTaskChanID; /* @F014448A */
	}

	/**
	 * Gets the link task channel type.
	 */
	public int getLinkTaskChanType()
	{
		return _LinkTaskChanType; /* @F014448A */
	}

	/**
	 * Returns the MVS User ID
	 */
	public String getMvsUserID() {
		return _mvsUserID;
	}

	/**
	 * Gets the Use Containers flag.
	 */
	public int getUseCICSContainer() 
  {
		return _UseCICSContainer; /* @F013381C */
	}

	/**
	 * Gets the Use OTMA flag.
	 */
	protected int getUseOTMA() 
  {
		return (_crii != null ? _crii.getUseOTMA() : 0); /* @F003694A */
	}

	/**
	 * Gets the OTMA Client name.
	 */
	public String getOTMAClientName() {
		return _OTMAClientName;
	}

	/**
	 * Gets the OTMA Server name.
	 */
	public String getOTMAServerName() 
  {
		return _OTMAServerName; /* @F003694A */
	}

	/**
	 * Gets the OTMA XCF Group ID.
	 */
	public String getOTMAGroupID() 
  {
		return _OTMAGroupID; /* @F003694A */
	}

	/**
	 * Gets the OTMA Sync Level.
	 */
	public String getOTMASyncLevel() 
  {
		return _OTMASyncLevel; /* @F003694A */
	}

	/**
	 * Gets the OTMA Send/Receive Multi-segment max segments.
	 */
	public int getOTMAMaxSegments() 
  {
		return _OTMAMaxSegments; /* @670111A */
	}

	/**
	 * Gets the OTMA Receive Maximum message size.
	 */
	public int getOTMAMaxRecvSize() 
  {
		return _OTMAMaxRecvSize; /* @670111A */
	}

	/**
	 * Gets the OTMA Request LLZZ flag.
	 */
	public int getOTMAReqLLZZ() 
	{
		return _OTMARequestLLZZ; /* @F013381C */
	}

	/**
	 * Gets the OTMA Response LLZZ flag.
	 */
	public int getOTMARespLLZZ() 
	{
		return _OTMAResponseLLZZ; /* @F013381C */
	}

	/**
	 * Gets RRS Transactional flag.
	 */
	protected boolean getRRSTransactional() 
	{
		return _RRSTransactional; /* @F014447A */
	}

	/**
   * Tells the caller if a transaction is running.  This method is
   * used by the InteractionImpl to notify the native code that this
   * request is transactional.
	 */
	protected boolean isTransactional() /* @F003691A */
	{
		return (_localTranStarted || (_xid != null)); /* @F003691C */
	}

	/**
	 * Tells the caller if a method has been invoked since the transaction
	 * associated with this managed connection was started. If there is no
	 * transaction, this method will return false.
	 * @return true if no interactions have been driven via InteractionImpl
	 *         since the transaction was started.
	 */
	protected boolean isFirstMethodInTran() /* @F003691A */
	{
		return _firstMethodInTran;
	}

	/**
	 * Notifies this managed connection that we are about to drive an
	 * interaction.
	 */
	protected void aboutToDriveInteraction() /* @F003691A */
	{
		_firstMethodInTran = false;
	}

	/**
	 * Tells us whether or not we are running in debug mode.
	 */
	protected boolean isDebugMode()
	{
		return _debugMode;
	}

	/**
	 * Called by the MCF to check if this connection can be used to match
	 * a new caller's connection request.  In order to match, the information
	 * passed must be the same information contained in this connection.
	 */
	public boolean doesConnectionMatch(String registerName, String OTMAGroupID, String OTMAServerName,
			String OTMAClientName, String OTMASyncLevel, int OTMAMaxSegments, int OTMAMaxRecvSize,
			int OTMARequestLLZZ, int OTMAResponseLLZZ, String LinkTaskTranID,
			int UseCICSContainer, String LinkTaskReqContID, int LinkTaskReqContType, String LinkTaskRspContID,
			int LinkTaskRspContType, String LinkTaskChanID, int LinkTaskChanType, /* @F014448A */
			int ConnectionWaitTimeout, boolean RRSTransactional, /* @F014447A */
			ConnectionRequestInfoImpl crii) {
		boolean match = false;

		// Extract all of the attributes from the ManagedConnection and compare
		// them to the input CRII, or against the input strings.  Do not compare
		// the username and password, we'll re-authenticate if necessary when
		// the connection manager calls getConnection().
		if ((registerName == null) && 
			(OTMAGroupID != null)) {                                                                /* @F013381A*/
			if ((compareStrings(OTMAGroupID, this.getOTMAGroupID(), TRUE_ON_NULL)) && /* PI28422 */
				(compareStrings(OTMAServerName, this.getOTMAServerName(), TRUE_ON_NULL)) && /* PI28422 */
				(compareStrings(OTMAClientName, this.getOTMAClientName(), TRUE_ON_NULL)) && /* PI28422 */
				(compareStrings(OTMASyncLevel, this.getOTMASyncLevel(), TRUE_ON_NULL)) && /* PI28422 */
				(OTMAMaxSegments == this.getOTMAMaxSegments()) &&
				(OTMAMaxRecvSize == this.getOTMAMaxRecvSize()) &&
				(ConnectionWaitTimeout == this.getConnectionWaitTimeout()) && /* @F013381C */
				(compareStrings(LinkTaskTranID, this.getLinkTaskTranID(), TRUE_ON_NULL)) &&          /* @F013381C,PI28422 */
				(compareStrings(LinkTaskReqContID, this.getLinkTaskReqContID(),TRUE_ON_NULL)) &&     /* @F013381C,PI28422 */
				(LinkTaskReqContType == this.getLinkTaskReqContType()) && /* @F013381C */
				(RRSTransactional == this.getRRSTransactional()) && /* @F014447A */
				(compareStrings(LinkTaskRspContID, this.getLinkTaskRspContID(),TRUE_ON_NULL)) &&     /* @F013381C,PI28422 */
				(LinkTaskRspContType == this.getLinkTaskRspContType()) && /* @F013381C */
				(compareStrings(LinkTaskChanID, this.getLinkTaskChanID(),TRUE_ON_NULL)) &&           /* @F014448A,PI28422 */
				(LinkTaskChanType == this.getLinkTaskChanType()) && /* @F014448A */
				(UseCICSContainer == this.getUseCICSContainer()) && /* @F013381C */
				(OTMARequestLLZZ == this.getOTMAReqLLZZ()) && /* @F013381C */
				(OTMAResponseLLZZ == this.getOTMARespLLZZ())) /* @F013381C */
			{
				match = true;
			}
		} /* @F013381A */
		else {
    	if ((registerName.equals(this.getRegisterName())) &&
   			(compareStrings(OTMAGroupID, this.getOTMAGroupID(),TRUE_ON_NULL)) &&                 /* PI28422 */
			(compareStrings(OTMAServerName, this.getOTMAServerName(), TRUE_ON_NULL)) && /* PI28422 */
			(compareStrings(OTMAClientName, this.getOTMAClientName(), TRUE_ON_NULL)) && /* PI28422 */
			(compareStrings(OTMASyncLevel, this.getOTMASyncLevel(), TRUE_ON_NULL)) && /* PI28422 */
   			(OTMAMaxSegments == this.getOTMAMaxSegments()) &&
   			(OTMAMaxRecvSize == this.getOTMAMaxRecvSize()) &&
   			(ConnectionWaitTimeout == this.getConnectionWaitTimeout()) && /* @F013381C */
   			(compareStrings(LinkTaskTranID, this.getLinkTaskTranID(),TRUE_ON_NULL)) &&           /* @F013381C,PI28422*/
   			(compareStrings(LinkTaskReqContID, this.getLinkTaskReqContID(),TRUE_ON_NULL)) &&     /* @F013381C,PI28422*/
   			(LinkTaskReqContType == this.getLinkTaskReqContType()) && /* @F013381C */
   			(RRSTransactional == this.getRRSTransactional()) && /* @F014447A */
   			(compareStrings(LinkTaskRspContID, this.getLinkTaskRspContID(),TRUE_ON_NULL)) &&     /* @F013381C, PI28422*/
   			(LinkTaskRspContType == this.getLinkTaskRspContType()) && /* @F013381C */
			(compareStrings(LinkTaskChanID, this.getLinkTaskChanID(),TRUE_ON_NULL)) &&           /* @F014448A,PI28422 */
			(LinkTaskChanType == this.getLinkTaskChanType()) && /* @F014448A */
			(UseCICSContainer == this.getUseCICSContainer()) && /* @F013381C */
			(OTMARequestLLZZ == this.getOTMAReqLLZZ()) && /* @F013381C */
			(OTMAResponseLLZZ == this.getOTMARespLLZZ())) /* @F013381C */
			{
				match = true;
			}
		}
		return match;
	}

	/**
	 * Compares two strings, either of which could be null. The strings are
   * first stripped of white space.  A string of length 0 (after stripping)
   * is considered to be the same as null.
	 */
	@Trivial // Marked trivial because this method is used to compare passwords,
				// which must never be traced.
	private boolean compareStrings(String a, String b, boolean result) /* PI28422 */
	{
		boolean equal = false;

		String stringA = a;
    if (stringA != null)
    {
			String stripped = a.trim();
      if (stripped.length() == 0) stringA = null;
		}

		String stringB = b;
    if (stringB != null)
    {
			String stripped = b.trim();
      if (stripped.length() == 0) stringB = null;
		}

    if (stringA == null)
    {
      if (stringB == null) equal = result;         /* PI28422 */
    }
    else
    {
			equal = stringA.equals(stringB);
		}

		return equal;
	}

	/**
	 * Gets the remote proxy information
	 */
	protected RemoteProxyInformation getRemoteProxyInformation() /* @F003705A */
	{
		return _rpi;
	}

	/**
	 * Tells us whether we're running on z/OS
	 */
	public boolean isZOS() /* @F003705A */
	{
	  // TODO: Liberty - Figure out if we're on z/OS.  Or figure out if we're allowing proxy requests.
		return true;
	}

	/**
   * Inflates a ConnectionSpecImpl based on the current state of this
   * managed connection.  This is used when talking with the distributed
   * proxy EJB.
	 */
	protected com.ibm.websphere.ola.ConnectionSpecImpl /* @F003705A */
    createConnectionSpecFromManagedConnection()
  {
		com.ibm.websphere.ola.ConnectionSpecImpl csi = null;

		if (_rpi != null) /* @703982C */
		{
			csi = new com.ibm.websphere.ola.ConnectionSpecImpl();
			csi.setRegisterName(getRegisterName());
			csi.setConnectionWaitTimeout(getConnectionWaitTimeout());
			csi.setLinkTaskTranID(getLinkTaskTranID());
			csi.setLinkTaskReqContID(getLinkTaskReqContID());
			csi.setLinkTaskReqContType(getLinkTaskReqContType());
			csi.setLinkTaskRspContID(getLinkTaskRspContID());
			csi.setLinkTaskRspContType(getLinkTaskRspContType());
			csi.setLinkTaskChanID(getLinkTaskChanID()); /* @F014448A */
			csi.setLinkTaskChanType(getLinkTaskChanType()); /* @F014448A */
			csi.setUseCICSContainer((getUseCICSContainer() == 0) ? false : true);
			csi.setUseOTMA(getUseOTMA() == 1);
			csi.setOTMAClientName(getOTMAClientName());
			csi.setOTMAServerName(getOTMAServerName());
			csi.setOTMAGroupID(getOTMAGroupID());
			csi.setOTMASyncLevel(getOTMASyncLevel());
			csi.setOTMAMaxSegments(getOTMAMaxSegments());
			csi.setOTMAMaxRecvSize(getOTMAMaxRecvSize());
			if (getOTMAReqLLZZ() == 1)
				csi.setOTMARequestLLZZ(true);
			else
				csi.setOTMARequestLLZZ(false);
			if (getOTMARespLLZZ() == 1)
				csi.setOTMAResponseLLZZ(true);
			else
				csi.setOTMAResponseLLZZ(false);
			csi.setUsername(_username);
			csi.setPassword(_password);
			csi.setRRSTransactional(getRRSTransactional());
		}

		return csi;
	}

	/**
	 * Extract the MVS user id from the Subject associated with this request.
   * The Subject is either built by logging-in the username/password supplied to the
   * connection (either directly or via JAAS alias), or if no username/password
   * was provided, then use the WSSubject.getRunAsSubject.
	 * 
   * If the Subject is null, or it doesn't contain a SAFCredential, then null is
   * returned.
	 *
	 * @return the MVS user id to use for the wola jca invocation.
	 */
	protected String extractMvsUserId(final String username, @Sensitive final String password) throws SecurityException {
		try {
			return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
				public String run() throws LoginException, WSSecurityException {
					return extractMvsUserIdPriv(username, password);
				}
			});
		} catch (PrivilegedActionException pae) {

			if (pae.getCause() instanceof LoginException) {
				// Report login failures. Other failures we ignore and
				// move on with a null mvs user id.
				throw new SecurityException(pae);
			}
		}

      // If we got here it means an exception was thrown, but it wasn't a LoginException,
		// so we ignore it and move on with no mvs user id.
		return null;
	}

	/**
	 * Called by extractMvsUserId from within a Privileged block. The user ID is
   * folded to upper case, since it will be passed to CICS where it will be used
   * to start a CICS TRANSACTION, which requires a SAF check against a profile
   * the SURROGAT class.
	 *
   * @return the MVS user id (in upper case) to use for the wola jca invocation.
	 */
	private String extractMvsUserIdPriv(final String username, @Sensitive final String password) throws LoginException, WSSecurityException {
		Subject subject = getSubject(username, password);
		if (subject == null) {
			return null;
		}

		SAFCredential safCred = getSAFCredential(subject);
		if (safCred == null) {
			return null;
		}

		String userId = safCred.getUserId();
		return (userId == null) ? null : userId.toUpperCase();
	}

	/**
	 * If a username/password was supplied (either directly or via JAAS alias),
	 * then do a login with the username/password to obtain a Subject.
	 * Otherwise, pull the Subject from the thread.
	 * 
	 * @return the subject to use for the jca request
	 */
	protected Subject getSubject(final String username, @Sensitive final String password) throws LoginException, WSSecurityException {

		if (!isEmpty(username)) {
			return login(username, password);
		} else {
			return WSSubject.getRunAsSubject();
		}
	}

	/**
   * @return the SAFCredential within the given Subject's private cred set; or null
   *         if the Subject has no SAFCredential.
	 */
	protected SAFCredential getSAFCredential(Subject subject) {
		for (SAFCredential safCred : subject.getPrivateCredentials(SAFCredential.class)) {
          return safCred;    // Just return the first one (there should only be one).
		}
		return null;
	}

	/**
   * @return true if the given string is null or "" or only blanks; false otherwise.
	 */
	protected boolean isEmpty(String s) {
		return (s == null || s.trim().length() == 0);
	}

	/**
	 * Login via the security sub-system.
	 * 
	 * @return An authenticated Subject for the given username and password.
	 */
	protected Subject login(String username, @Sensitive String password) throws AuthenticationException {

		AuthenticationService authService = SecurityServiceTracker.getAuthenticationService();
		if (authService == null) {
			// TODO: do we care?
			return null;
		}

      return authService.authenticate(JaasLoginConfigConstants.SYSTEM_DEFAULT, createAuthenticationData(username, password), null );
	}

	/**
   * @return an AuthenticationData object containing the given username/password.
	 */
	@Trivial
	protected AuthenticationData createAuthenticationData(String username, @Sensitive String password) {
		AuthenticationData authenticationData = new WSAuthenticationData();
		authenticationData.set(AuthenticationData.USERNAME, username);
		authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
		return authenticationData;
	}
}
