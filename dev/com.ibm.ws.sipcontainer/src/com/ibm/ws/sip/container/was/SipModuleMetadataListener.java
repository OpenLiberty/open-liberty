/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.metadata.MetaDataEvent;
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.router.SipAppDescManager;
import com.ibm.ws.sip.container.util.SipLogExtension;
import com.ibm.ws.webcontainer.osgi.metadata.WebModuleMetaDataImpl;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.ws.webcontainer.webapp.WebAppConfiguration;

/**
 * The class is used to get the moduleMetaData and retrieve the WebApp object
 * that it contains. The WebApp is saved by the SIP Container to initialize the
 * application when needed
 * 
 * @author SAGIA
 * 
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, configurationPid = "com.ibm.ws.sip.container.was.SipModuleMetadataListener", service = ModuleMetaDataListener.class, property = { "service.vendor=IBM" })
public class SipModuleMetadataListener implements ModuleMetaDataListener {

	/**
	 * Trace variable
	 */
	private static final TraceComponent tc = Tr
			.register(SipModuleMetadataListener.class);

	

	/**
	 * the appDesc\webApp manger
	 */
	private SipAppDescManager _appDescMangar = null;

	/**
	 * DS method to activate this component.
	 * 
	 * @param context
	 *            : Component context of the component
	 * @param properties
	 *            : Map containing service & config properties
	 *            populated/provided by config admin
	 */
	protected void activate(ComponentContext context,
			Map<String, Object> properties) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "activated", properties);
		}
		_appDescMangar = SipAppDescManager.getInstance();

	}

	/**
	 * DS method to deactivate this component.
	 * 
	 * @param reason
	 *            int representation of reason the component is stopping
	 */
	public void deactivate(int reason) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "SipModuleMetadataListener deactivated, reason="
					+ reason);
		}

		_appDescMangar = null;
	}

	/**
	 * DS method to modify components properties configuration
	 * 
	 * @param properties
	 *            : Map containing service & config properties
	 *            populated/provided by config admin
	 */
	protected void modified(Map<String, Object> properties) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "modified", properties);
		}
	}

	/**
	 * This function is called when an application is installed. If the
	 * application is a SIP application then it is added to the SipAppDesc of
	 * the application and if it's the first application the SIP Container is
	 * initialized.
	 * 
	 * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataCreated(com.ibm.ws.container.service.metadata.MetaDataEvent)
	 */
	@Override
	public void moduleMetaDataCreated(MetaDataEvent<ModuleMetaData> event) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "moduleMetaDataCreated start");

		}

		ModuleMetaData metaData = event.getMetaData();

		if (metaData instanceof WebModuleMetaDataImpl) {
			WebModuleMetaDataImpl webMetaData = (WebModuleMetaDataImpl) metaData;
			WebAppConfiguration webConfig = ((WebAppConfiguration) (webMetaData.getConfiguration()));

			WebApp webApp = webConfig.getWebApp();
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,"moduleMetaDataCreated looking for app name: "+ webApp.getName());
			}
			SipAppDesc appDesc = _appDescMangar.updateWebApp(webApp);

			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(tc,"moduleMetaDataCreated got appDesc: "+ appDesc);
			}
			if (appDesc == null) {
				return;
			}

			
			SipContainer sipCon = SipContainer.getInstance();
			
			// if the router is initialized we need to load the application
			// configuration to the router now.
			if (sipCon.getRouter().isInitialized()) {
				sipCon.getRouter().loadAppConfiguration(webApp);

			}

			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "moduleMetaDataCreated done installing app" , webApp.getName());
			}
		}



	}

	/**
	 * The function is called when uninstalling an application. The application
	 * is removed from the map at the app holder.
	 * 
	 * @see com.ibm.ws.container.service.metadata.ModuleMetaDataListener#moduleMetaDataDestroyed(com.ibm.ws.container.service.metadata.MetaDataEvent)
	 */
	@Override
	public void moduleMetaDataDestroyed(MetaDataEvent<ModuleMetaData> event) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc,"moduleMetaDataDestoyed start");
		}
		ModuleMetaData metaData = event.getMetaData();
		if (metaData instanceof WebModuleMetaDataImpl) {
			WebModuleMetaDataImpl webMetaData = (WebModuleMetaDataImpl) metaData;
			WebAppConfiguration webConfig = ((WebAppConfiguration) (webMetaData.getConfiguration()));
			WebApp webApp = webConfig.getWebApp();
			_appDescMangar.removeApp(webApp);
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc,"moduleMetaDataDestoyed removed app: ", webApp.getName());
			}
		}
		
		if (_appDescMangar.getSipAppDescs().size() == 0) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc, "stopping", "setting server weight to 0");
			}
	
			PerformanceMgr perfMgr = PerformanceMgr.getInstance();
			if (perfMgr != null) {
				perfMgr.setServerWeight(0); //Make this container unavailable for more requests
				//When last application stopped. Fix for 387747
				//Stop PMI timers (defect 677439)
	          	perfMgr.stopTimers();
			}
			//Unregister the HPEL log extension.
			SipLogExtension.destroy();
		}
	}
}
