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

package com.ibm.ws.sib.admin.internal;

import java.util.Properties;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.ForeignBusDefinition;
import com.ibm.ws.sib.admin.internal.JsAdminFactory;
import com.ibm.ws.sib.admin.JsBus;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsPermittedChainUsage;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JsBusImpl implements JsBus {

	private static final String CLASS_NAME = "com.ibm.ws.sib.admin.impl.JsBusImpl";
	private static final TraceComponent tc = SibTr.register(JsBusImpl.class,
			JsConstants.TRGRP_AS, JsConstants.MSG_BUNDLE);

	// The service in which we are contained
	private final JsMainImpl _mainImpl;

	private final JsMEConfig meConfig;

	// Our factory class for creating Admin objects
	private JsAdminFactory _jsaf = null;

	// Destination cache and query
	private JsDestinationCache _destinationCache = null;

	// Mediation cache and query
	// private JsMediationCache _mediationCache = null;

	// Bus audit allowed
	// private boolean _busAuditAllowed = true;

	// The name of this bus
	private String _name = null;
	
	private final String MBEAN_TYPE=JsConstants.MBEAN_TYPE_BUS;

	// The UUID of this bus
	private SIBUuid8 _uuid = null;

	// Cached copy of configurationReloadEnabled flag
	private final Boolean configurationReload = null;

	// Custom Properties for the Bus
	private final Properties customProperties = new Properties();

	public JsBusImpl(JsMEConfig co, JsMainImpl mainImpl, String name) {

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, CLASS_NAME + ".<init>", name);

		_mainImpl = mainImpl;

		try {
			_jsaf = JsAdminFactory.getInstance();
		} catch (Exception e) {
			// No FFDC code needed
		}

		_name = name;
		meConfig = co;
		// Load the audit document
		// loadAuditAllowed();

		_destinationCache = new JsDestinationCache(this);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, CLASS_NAME + ".<init>");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.impl.JsEObjectImpl#setEObject()
	 * 
	 * Override super class method to ensure audit document gets reloaded if
	 * EObject is refreshed during dynamic config
	 */
	// public void setEObject(LWMConfig co) {
	// //super.setEObject(co);
	// loadAuditAllowed();
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.impl.JsEObjectImpl#getName()
	 */
	public String getName() {
		return _name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#isSecure()
	 */
	public boolean isSecure() {
		boolean retValue;
		boolean secureBus = false;

		// if _mainImpl. TBD check for secure bus in JsMainImpl
		return false; // TBd
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getUuid()
	 */
	public SIBUuid8 getUuid() {
		if (_uuid == null) {
			String s = "B0CEFAD4D3454A2E";
			_uuid = new SIBUuid8(s);
		}
		return _uuid;
	}

	/**
	 * @return
	 */
	public JsDestinationCache getDestinationCache() {
		return _destinationCache;
	}

	/**
	 * @return
	 */
	// public JsMediationCache getMediationCache() {
	// return _mediationCache;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getSIBDestination(java.lang.String,
	 * java.lang.String)
	 */
	public BaseDestinationDefinition getSIBDestination(String busName,
			String name) throws SIBExceptionBase,
			SIBExceptionDestinationNotFound {
		return getDestinationCache().getSIBDestination(busName, name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getSIBDestination(java.lang.String,
	 * java.lang.String, com.ibm.ws.sib.admin.DestinationDefinition)
	 */
	public void getSIBDestination(String busName, String name,
			DestinationDefinition dd) throws SIBExceptionBase,
			SIBExceptionDestinationNotFound {
		getDestinationCache().getSIBDestination(busName, name, dd);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.ws.sib.admin.JsBus#getSIBDestinationLocalitySet(java.lang.String,
	 * java.lang.String)
	 */
	public Set getSIBDestinationLocalitySet(String busName, String uuid)
			throws SIBExceptionBase {
		return getDestinationCache()
				.getSIBDestinationLocalitySet(busName, uuid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getSIBMediation(java.lang.String,
	 * java.lang.String)
	 */
	// public MediationDefinition getSIBMediation(String busName, String name)
	// throws SIBExceptionBase, SIBExceptionDestinationNotFound {
	// return getMediationCache().getSIBMediation(busName, name);
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getSIBMediation(java.lang.String,
	 * java.lang.String, com.ibm.ws.sib.admin.MediationDefinition)
	 */
	// public void getSIBMediation(String busName, String name,
	// MediationDefinition md)
	// throws SIBExceptionBase, SIBExceptionDestinationNotFound {
	// getMediationCache().getSIBMediation(busName, name, md);
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.ibm.ws.sib.admin.JsBus#getSIBMediationLocalitySet(java.lang.String,
	 * java.lang.String)
	 */
	// public Set getSIBMediationLocalitySet(String busName, String uuid) throws
	// SIBExceptionBase {
	// return getMediationCache().getSIBMediationLocalitySet(busName, uuid);
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getForeignBus(java.lang.String)
	 */
	// public ForeignBusDefinition getForeignBus(String name) {
	// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	// SibTr.entry(tc, "getForeignBus", name);

	// List foreignBuses =
	// ((ConfigObject)getEObject()).getObjectList(CT_SIBus.FOREIGNBUS_NAME);
	// Iterator iter = foreignBuses.iterator();
	// while (iter.hasNext()) {
	// ConfigObject fb = (ConfigObject)iter.next();
	// if (name.equals(fb.getString(CT_SIBForeignBus.NAME_NAME,
	// CT_SIBForeignBus.NAME_DEFAULT))) {
	// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	// SibTr.exit(tc, "getForeignBus", fb);
	// return ((JsAdminFactoryImpl) _jsaf).createForeignBusDefinition(fb);
	// }
	// }

	// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	// SibTr.exit(tc, "getForeignBus", null);
	// // TODO: MEDIUM: Should this be an exception? Interface change?
	// return null;
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getForeignBusForLink(java.lang.String)
	 */
	// public ForeignBusDefinition getForeignBusForLink(String uuid) {
	// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	// SibTr.entry(tc, "getForeignBusForLink", uuid);
	//
	// List foreignBuses = ((ConfigObject)
	// getEObject()).getObjectList(CT_SIBus.FOREIGNBUS_NAME);
	// Iterator iter = foreignBuses.iterator();
	// while (iter.hasNext()) {
	// ConfigObject fb = (ConfigObject) iter.next();
	// if ((fb.getObject(CT_SIBForeignBus.VIRTUALLINK_NAME) != null) &&
	// (uuid.equals(fb.getObject(CT_SIBForeignBus.VIRTUALLINK_NAME).getString(CT_SIBVirtualLink.UUID_NAME,
	// CT_SIBVirtualLink.UUID_DEFAULT)))) {
	// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	// SibTr.exit(tc, "getForeignBusForLink", fb);
	// return ((JsAdminFactoryImpl) _jsaf).createForeignBusDefinition(fb);
	// }
	// }
	//
	// SibTr.error(tc, "INTERNAL_ERROR_SIAS0003", "Bus '" + _name +
	// "' does not contain a definition for a Foreign Bus with virtual link UUID '"
	// + uuid
	// +
	// "'. You may have a link configured on a messaging engine that specifies a foreign bus that has been deleted.");
	//
	// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	// SibTr.exit(tc, "getForeignBusForLink", null);
	// // TODO: MEDIUM: Should this be an exception? Interface change?
	// return null;
	// }

	/**
	 * Reload the destination cache by creating a new JsCacheMediation object
	 * pointing to this.
	 */
	// public void refreshMediationCache() {
	// _mediationCache = new JsMediationCache(this);
	// _mediationCache.refreshDestinationCacheWithMediations();
	// }

	/**
	 * Reload the destination cache by creating a new JsCacheDestination object
	 * pointing to this.
	 */
	public void refreshDestinationCache() {
		_destinationCache = new JsDestinationCache(this);

	}

	/**
	 * Return the state of the configurationReloadEnabled flag on the WCCM Bus
	 * object. This value is cached to prevent dynamic config altering it.
	 * 
	 * @return The state of the configurationReloadEnabled flag on server
	 *         startup.
	 */
	boolean isConfigurationReloadEnabled() {

		return true;
	}

	/**
	 * Set a custom property for the bus
	 */
	public void setCustomProperties() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "setCustomProperties", this);

		// Set the properties that belong to the bus
		/*
		 * List props =
		 * ((ConfigObject)getEObject()).getObjectList(CT_SIBus.PROPERTIES_NAME);
		 * for (Iterator iter = props.iterator(); iter.hasNext();) {
		 * ConfigObject p = (ConfigObject)iter.next(); String name =
		 * p.getString(CT_Property.NAME_NAME, CT_Property.NAME_DEFAULT); String
		 * value = p.getString(CT_Property.VALUE_NAME,
		 * CT_Property.VALUE_DEFAULT); setCustomProperty(name, value); }
		 */

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "setCustomProperties");
	}

	/**
	 * Set an individual custom property for the bus
	 */
	public void setCustomProperty(String name, String value) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "setCustomProperty", name + " " + value);

		// Set the properties that belong to the bus
		// customProperties.put(name,value);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "setCustomProperty");
	}

	/**
	 * This method returns a custom property that was configured for the bus.
	 * 
	 * @see JsBus#getCustomProperty(java.lang.String)
	 * @param name
	 *            The name of the custom property.
	 * @return The value of the custom property.
	 */
	public String getCustomProperty(String name) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "getCustomProperty", name);

		// String value = customProperties.getProperty(name);

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "getCustomProperty", "value");

		return null;// tbd
	}

	/**
	 * Test whether Event Notification is enabled for the bus.
	 */
	public Boolean isEventNotificationPropertySet() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "isEventNotificationPropertySet", this);

		Boolean enabled = null; // enabled can be null/TRUE/FALSE, ie three-way

		// Test whether custom properties contain a notification prop
		if (customProperties
				.containsKey(JsConstants.SIB_EVENT_NOTIFICATION_KEY)) {
			String value = customProperties
					.getProperty(JsConstants.SIB_EVENT_NOTIFICATION_KEY);

			if (value != null) {
				if (value
						.equals(JsConstants.SIB_EVENT_NOTIFICATION_VALUE_ENABLED)) {
					if (TraceComponent.isAnyTracingEnabled()
							&& tc.isDebugEnabled())
						SibTr.debug(tc,
								"Event Notification is enabled at the Bus");
					enabled = Boolean.TRUE;
				} else if (value
						.equals(JsConstants.SIB_EVENT_NOTIFICATION_VALUE_DISABLED)) {
					if (TraceComponent.isAnyTracingEnabled()
							&& tc.isDebugEnabled())
						SibTr.debug(tc,
								"Event Notification is disabled at the Bus");
					enabled = Boolean.FALSE;
				} else {
					// Value MIS set, treat as NOT set
					if (TraceComponent.isAnyTracingEnabled()
							&& tc.isDebugEnabled())
						SibTr.debug(tc,
								"Event Notification Bus property set to: "
										+ value);
				}
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "isEventNotificationPropertySet", enabled);
		return enabled;
	}

	/* ------------------------------------------------------------------------ */
	/*
	 * getPermittedChains method /*
	 * ------------------------------------------------------------------------
	 */
	/**
	 * This method returns the set of string chain names that are listed as
	 * permitted by this bus.
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getPermittedChains()
	 * @return the set of permitted chain names.
	 */
	public Set<String> getPermittedChains() {
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.entry(tc, "getPermittedChains");
		//
		// Set<String> permittedChains = new HashSet<String>();
		// List pChains = ((ConfigObject)
		// getEObject()).getObjectList(CT_SIBus.PERMITTEDCHAINS_NAME);
		// Iterator it = pChains.iterator();
		//
		// while (it.hasNext()) {
		// ConfigObject chain = (ConfigObject) it.next();
		// permittedChains.add(chain.getString(CT_SIBPermittedChain.NAME_NAME,
		// CT_SIBPermittedChain.NAME_DEFAULT));
		// }
		//
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.exit(tc, "getPermittedChains", permittedChains);
		return null;
	}

	/* ------------------------------------------------------------------------ */
	/*
	 * getPermittedChainUsage method /*
	 * ------------------------------------------------------------------------
	 */
	/**
	 * This method returns the permitted chain usage.
	 * 
	 * @see com.ibm.ws.sib.admin.JsBus#getPermittedCh ainUsage()
	 * @return the permitted chain usage.
	 */
	public JsPermittedChainUsage getPermittedChainUsage() {
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.entry(tc, "getPermittedChainUsage");
		//
		// String configUsage = ((ConfigObject)
		// getEObject()).getString(CT_SIBus.USEPERMITTEDCHAINS_NAME,
		// CT_SIBus.USEPERMITTEDCHAINS_DEFAULT);
		//
		// JsPermittedChainUsage usage = null;
		//
		// if (configUsage.equals(CT_SIBPermittedChainUsage.LISTED)) {
		// usage = JsPermittedChainUsage.LISTED;
		// } else if (configUsage.equals(CT_SIBPermittedChainUsage.ALL)) {
		// usage = JsPermittedChainUsage.ALL;
		// } else if (configUsage.equals(CT_SIBPermittedChainUsage.SSL_ENABLED))
		// {
		// usage = JsPermittedChainUsage.SSL_ENABLED;
		// }
		//
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.exit(tc, "getPermittedChainUsage", usage);
		return null;
	}

	public boolean isBootstrapAllowed() {
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.entry(tc, "isBootstrapAllowed");
		//
		// boolean result = false;
		//
		// String bootstrapPolicy = ((ConfigObject)
		// getEObject()).getString(CT_SIBus.BOOTSTRAPMEMBERPOLICY_NAME,
		// CT_SIBus.BOOTSTRAPMEMBERPOLICY_DEFAULT);
		//
		// if
		// (CT_SIBBootstrapMemberPolicy.SIBSERVICE_ENABLED.equals(bootstrapPolicy))
		// {
		// result = true; // this method will never be called if SIBService is
		// not enabled.
		// }
		// // we do this for the other policy types.
		// else {
		// try {
		// // a call to getBus will only return a JsBus if this server is a bus
		// // member of that bus. As a result we just call getBus and if it
		// returns
		// // a non-null we know that bootstrap is allowed.
		// JsBus busThisServerIsAMemberOf =
		// JsAdminService.getInstance().getBus(getName());
		//
		// if (busThisServerIsAMemberOf != null)
		// result = true;
		// } catch (SIBExceptionBusNotFound e) {
		// // No FFDC Code Needed in this case the server is not a member of
		// this bus.
		// }
		// }
		//
		// // if bootstrap is not already allowed and nominated members should
		// be
		// // consulted we start looking at that level too.
		// if (!result &&
		// CT_SIBBootstrapMemberPolicy.MEMBERS_AND_NOMINATED.equals(bootstrapPolicy))
		// {
		// @SuppressWarnings("unchecked")
		// List<ConfigObject> bootstrapMembers = ((ConfigObject)
		// getEObject()).getObjectList(CT_SIBus.NOMINATEDBOOTSTRAPMEMBERS_NAME);
		//
		// Server server = (Server)
		// JsAdminService.getInstance().getService(Server.class);
		// String thisClusterName = server.getClusterName();
		// String thisNodeName = server.getNodeName();
		// String thisServerName = server.getName();
		//
		// boolean isInACluster = thisClusterName != null &&
		// !"".equals(thisClusterName);
		//
		// for (ConfigObject bootstrapMember : bootstrapMembers) {
		// String nodeName =
		// bootstrapMember.getString(CT_SIBBootstrapMember.NODE_NAME,
		// CT_SIBBootstrapMember.NODE_DEFAULT);
		// String serverName =
		// bootstrapMember.getString(CT_SIBBootstrapMember.SERVER_NAME,
		// CT_SIBBootstrapMember.SERVER_DEFAULT);
		// String clusterName =
		// bootstrapMember.getString(CT_SIBBootstrapMember.CLUSTER_NAME,
		// CT_SIBBootstrapMember.CLUSTER_DEFAULT);
		//
		// result = (thisNodeName.equals(nodeName) &&
		// thisServerName.equals(serverName)) ||
		// (isInACluster && thisClusterName.equals(clusterName));
		//
		// if (result)
		// break;
		// }
		// }
		//
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.exit(tc, "isBootstrapAllowed", result);
		return true;
	}

	/* ------------------------------------------------------------------------ */
	/*
	 * isBusAuditAllowed method /*
	 * ------------------------------------------------------------------------
	 */
	/**
	 * This method returns true if audit is allowed on the bus.
	 * 
	 * @return true if audit allowed on the bus
	 */
	public boolean isBusAuditAllowed() {
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.entry(tc, "isBusAuditAllowed");
		//
		// if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
		// SibTr.exit(tc, "isBusAuditAllowed", _busAuditAllowed);
		return false;
	}

	// Loads the audit document for this bus
	private void loadAuditAllowed() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.entry(tc, "loadAuditAllowed", this);
		//
		// ConfigObject sibAudit = null;
		// try {
		// // Get config service
		// ConfigService service = (ConfigService)
		// WsServiceRegistry.getService(this, ConfigService.class);
		//
		// // Create server scope
		// ConfigScope scope = service.createScope(ConfigScope.BUS);
		// scope.set(ConfigScope.BUS, _name);
		//
		// // Get iterator over contents of audit document, could by from
		// dynamic config so don't use cache
		// List auditList = service.getDocumentObjects(scope,
		// JsConstants.WCCM_DOC_SECURITY_AUDIT, false);
		// if (auditList != null && !auditList.isEmpty()) {
		// // There will only be one entry
		// sibAudit = (ConfigObject) auditList.get(0);
		// }
		// } catch (FileNotFoundException e) {
		// // No FFDC code needed
		// // Document doesn't exist yet
		// sibAudit = null;
		// } catch (Exception e) {
		// com.ibm.ws.ffdc.FFDCFilter.processException(e, CLASS_NAME +
		// ".<init>", "5", this);
		// SibTr.debug(tc, "CONFIG_LOAD_FAILED_SIAS0008", "buses\\" + _name +
		// "\\" + JsConstants.WCCM_DOC_SECURITY_AUDIT);
		// sibAudit = null;
		// }
		//
		// // Read the configuration for the specified bus, and get bus
		// auditAllowed
		// if (sibAudit != null) {
		// // Bus auditAllowed value
		// _busAuditAllowed = sibAudit.getBoolean(CT_SIBAudit.ALLOWAUDIT_NAME,
		// CT_SIBAudit.ALLOWAUDIT_DEFAULT);
		// } else {
		// // Default to true
		// _busAuditAllowed = true;
		// }

		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
			SibTr.exit(tc, "loadAuditAllowed");
	}

	public JsMEConfig getLWMMEConfig() {
		return meConfig;
	}

	/** {@inheritDoc} */
	@Override
	public ForeignBusDefinition getForeignBus(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ForeignBusDefinition getForeignBusForLink(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getMBeanType() {
		return MBEAN_TYPE;
	}
}
