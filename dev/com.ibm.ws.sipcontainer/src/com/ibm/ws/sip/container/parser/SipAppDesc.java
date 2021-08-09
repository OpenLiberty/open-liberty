/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.parser;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSessionActivationListener;
import javax.servlet.sip.SipApplicationSessionAttributeListener;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipErrorListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.TimerListener;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.sip.SipApplicationSessionStateListener;
import com.ibm.websphere.sip.SipSessionStateListener;
import com.ibm.websphere.sip.unmatchedMessages.UnmatchedMessageListener;
import com.ibm.ws.javaee.dd.common.ParamValue;
import com.ibm.ws.javaee.dd.web.common.Servlet;
import com.ibm.ws.session.SessionContext;
import com.ibm.ws.sip.container.annotation.ApplicationKeyHelper;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.rules.Condition;
import com.ibm.ws.sip.container.virtualhost.VirtualHostAlias;
import com.ibm.ws.sip.container.virtualhost.VirtualHostAliasImpl;
import com.ibm.ws.sip.parser.SipConstants;
import com.ibm.ws.sip.stack.transport.virtualhost.SipVirtualHostAdapter;
import com.ibm.ws.webcontainer.webapp.WebApp;

/**
 * @author Amir Perlman, Jun 24, 2003
 * 
 * Represents a Sip Application as defined by the application's sip.xml
 * document. Maintains a list of all siplets contained by the application.
 * each variable can be changed only once - to prevent annotations to change configuration from the sip.xml
 */
public class SipAppDesc extends GenericAppDesc {
	
	/**
	 * To identify which primitives were set we use a bit set with bit per primitive and they are defined as an enum
	 */
	private static enum PRIMITIVE_LOCATION  {
		APP_TTL_BIT , WEB_TRASNSPORT_PORT_BIT , WEB_TRASNSPORT_SSL_BIT , IS_DISTRIBUTDED_BIT ,PROXY_TIMEOUT_BIT, HAS_MAIN_SERVLET_BIT
	}

	/** Serialization UID (do not change) */
	static final long serialVersionUID = 4431618929876845268L;

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SipAppDesc.class);

	
	/**
	 * context-param name for excluding the application from application routing.
	 * Should be like:
	 * 	<context-param>
	 *   <param-name>com.ibm.sip.container.exclude.application.routing</param-name>
	 *   <param-value>true</param-value>
	 *	</context-param>
	 */
	private static final String EXCLUDE_APPLICATION_ROUTING_ATTRIBUTE = "com.ibm.sip.container.exclude.application.routing";
	
	/**
	 * Application name from tag <display name>.
	 */	
	private String m_display_name = null;
	
	/**
	 * Application name from tag <app-name>
	 */
	private String m_app_name = null;

	/**
	 * a bit set to mark if the variable was already changed so that each var will be changed only once.
	 */
	private BitSet m_wasSet = new BitSet(PRIMITIVE_LOCATION.values().length);
	/**
	 * This index is used by PMI to identify the Application in the application
	 * table
	 */
	private Integer m_appIndexForPmi = null;

	/**
	 * A map of all Sip Servlet associated with this application. The order of
	 * the siplets matches the order within the sip.xml file. HashMap to prevent duplication
	 * key: servlet name
	 * vale: the SipServletDesc
	 */
	private Map<String, SipServletDesc> m_siplets = new LinkedHashMap<String, SipServletDesc>();

	/**
	 * A map of all context parameter associated with this application
	 */
	private Map<String,String> m_contextParams = new LinkedHashMap<String,String>();

	/**
	 * Class implementing the TimerListener events. Applications are notified of
	 * expiration of timers through the TimerListener interface.
	 */
	private TimerListener m_timerListener;

	/**
	 * Listeners for Sip Application Session Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipApplicationSessionListener
	 */
	private HashMap<String,SipApplicationSessionListener> m_appSessionListeners = new HashMap<String,SipApplicationSessionListener>();

	/**
	 * Listeners for Sip Session Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipSessionListener
	 */
	private HashMap<String,SipSessionListener> m_sessionListeners = new HashMap<String,SipSessionListener>();

	/**
	 * Listeners for Sip Session state change Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipSessionStateListener
	 */
	private HashMap<String,SipSessionStateListener> m_sessionStateListeners = new HashMap<String,SipSessionStateListener>();

	/**
	 * Listeners for Application Session State change Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipApplicationSessionStateListener
	 */
	private HashMap<String,SipApplicationSessionStateListener> m_appSessionStateListeners = new HashMap<String,SipApplicationSessionStateListener>();

	/**
	 * Listeners for Application Session activation change Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipApplicationSessionActivationListener
	 */
	private HashMap<String,SipApplicationSessionActivationListener> m_appSessionActivationListeners = new HashMap<String,SipApplicationSessionActivationListener>();

	/**
	 * Listeners for Session Attributes Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipSessionAttributeListener
	 */
	private HashMap<String,SipSessionAttributeListener> m_sessionAttributeListeners = new HashMap<String,SipSessionAttributeListener>();

	/**
	 * Listeners for ApplicationSession Attributes Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipApplicationSessionAttributeListener
	 */
	private HashMap<String,SipApplicationSessionAttributeListener> m_appSessionAttributeListeners = new HashMap<String,SipApplicationSessionAttributeListener>();

	/**
	 * Listeners for Sip Error Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipErrorListener
	 */
	private HashMap<String,SipErrorListener> m_errorListeners = new HashMap<String,SipErrorListener>();
	
	/**
	 * Listeners for Sip Servlet Events. HashMap to prevent duplication
	 * key: listener name
	 * vale: the SipServletListener
	 */
	private HashMap<String,SipServletListener> m_sipServletListeners = new HashMap<String,SipServletListener>();
	
	/**
	 * Listeners for unmatched incoming Messages (requests and responses). HashMap to prevent duplication
	 * key: listener name
	 * vale: the UnmatchedMessageListener
	 */ 
	private HashMap<String,UnmatchedMessageListener> m_unmatchedMessageListener = new HashMap<String,UnmatchedMessageListener>();
	

	/**
	 * Time to live for Application Sessions in minutes.
	 */
	private int m_appSessionTTL = 3;

	/**
	 * The root uri of the WEB Application as it is deployed in WAS. root URI +
	 * siplet's name gives us the full uri of where the siplet is located in
	 * WAS.
	 */
	private String m_rootURI = null;

	/**
	 * The SIP Factory associated with this SIP App.
	 */
	private transient SipFactory m_sipFactory;

	/**
	 * The host name of the Web Container transport that will be used to pass
	 * messages into WAS.
	 */
	private String m_webTransportHost;

	/**
	 * The port number of the Web Container transport that will be used to pass
	 * messages into WAS.
	 */
	private int m_webTransportPort = -1;

	/**
	 * The Is SSL of the Web Container transport that will be used to pass
	 * messages into WAS.
	 */
	private boolean m_webTransportIsSSL;


	/**
	 * Application's name as it appears in the Web Deployment descriptor.
	 * Because of the duplicate deployment descriptors (sip.xml and web.xml) the
	 * name of the application might differ. We need to keep this name as well
	 * since application will loaded/unloaded by websphere with this name.
	 */
	private String m_webAppName;

	/**
	 * Flag indicating whether the application descriptor, indicates that this
	 * servlet application is programmed appropriately to be deployed into a
	 * distributed servlet container
	 */
	private boolean m_isDistributed;

	/**
	 * The sequential-search-timeout element defines the default timeout for
	 * sequential searches for all proxy operations performed by this
	 * application. The specified timeout must be expressed in a whole number of
	 * seconds. The container may override this value as a result of its own
	 * local policy.
	 */
	private int _proxyTimeout = SipConstants.DEFAULT_PROXY_TIMEOUT_SECONDS;
	
	/**
	 * Application name from "description" annotation .
	 */	
	private String m_description;

	/**
	 * The Apllication Realm
	 */
	private String m_realm = "sip";
	
	
	private  List<SipServlet> onStartupServlets = Collections.synchronizedList(new ArrayList<SipServlet>());

	/**
	 * 
	 */
	private List<SecurityConstraint> m_securityConstraints = new LinkedList<SecurityConstraint>();

	/**
	 * Thread-safe List of virtual host aliases related to this application virtual host
	 */
	private List<VirtualHostAlias> _virtualHostAliases = Collections.synchronizedList(new ArrayList<VirtualHostAlias>());

	/**
	 * The virtual host this application is attached to
	 */
	private String _virtualHostName;

	/**
	 * One of the port in the VH list. We keep to provide fast access instead of
	 * looking in the list each time.
	 */
	private int _vhPort = -1;

	/**
	 * One of the host name (matching _vhHost) in the VH list. We keep it to
	 * provide fast access instead of looking in the list each time.
	 */
	private String _vhHost = null;

	/**
	 * Holds the context received from the WebContainer. This will be used to
	 * establish the context on a listener thread when listener event is invoked
	 */
	private ContextEstablisher _contextEstablisher;
	
	/**
	 * JSR 289 defines one servlet who recives all the
	 * requests, inplace of rules matching.
	 */
	private boolean hasMainServlet = false;
	private String mainSipletName = null;
	private SipServletDesc mainServlet = null;

	/**
	 * Wraps up method for application key definition. 
	 * The user can annotated a method inside the application with "@SipApplicationKey"
	 * annotation, that method will be called each time the container will assign 
	 * application session to sipplet call.
	 */
	private ApplicationKeyHelper applicationKeyHelper = null;

	
	/**
	 * The WebApp object of this application. Used in Liberty for lazy initialization of the application
	 */
	private WebApp _webApp = null; 

	/**
	 * annotated classes as @SipListeres
	 */
	private HashSet<String> _sipListeners = new HashSet<String>();

	/**
	 * Information about @ApplicationKey class and methods name;
	 */
	private String _appKeyClassName;
	private String _appKeyMethodName;

	/**
	 * This flag prevents from the SipApplication to be initialized more then once.
	 */
	private boolean m_wasInitialized = false;
	/**
	 * This flag prevents from the webApp to be initialized again if it's during initialization.
	 */
	private boolean m_isDuringWebAppInitialization = false;

	/**
	 * Stores servlets mapping rules from sip.xml
	 */
	private Map<String, Condition> m_servletsPatterns = new HashMap<String, Condition>();
	
	
	/**
	 * Should exclude the application from application routing.
	 */
	private boolean m_shouldExcludeFromApplicationRouting = false;
	
	/**
	 * Constructs a new Sip Application.
	 * 
	 * @param name
	 *            application display name. may be null.
	 */
	public SipAppDesc(String display_name, String app_name) {
		super();
		m_app_name = app_name;
		m_display_name = display_name;
		
	
	}

	/**
	 * 
	 * @param displayName
	 */
	public void setDisplayName(String displayName) {
		if(m_display_name == null) {
			m_display_name = displayName;
		}
	}

	/**
	 * 
	 * @param appName
	 */
	public void setAppName(String appName) {
		if(m_app_name == null) {
			m_app_name = appName;
		}
	}
		
	

	/**
	 * Ctor
	 * m_app_name and m_display_name will be added later.
	 */
	public SipAppDesc() {
		super();
	}

	/**
	 * generates a display name for this application in lack of a <display-name>
	 * in the deployment descriptor
	 * 
	 * @return a fictive display name TODO: change any container code that
	 *         depends on display-name, to use some other attribute from the
	 *         descriptor, as the display name is not unique by definition.
	 */
	private String generateAppName() {
		// hash the siplets into an app name
		int hashCode = 0;
		Iterator<SipServletDesc> i = m_siplets.values().iterator();
		while (i.hasNext()) {
			SipServletDesc siplet = i.next();
			hashCode += siplet.getName().hashCode();
			hashCode <<= 5;
			hashCode += siplet.getClassName().hashCode();
			hashCode <<= 5;
		}
		String name = "UnnamedSipApp-" + Integer.toHexString(hashCode);
		return name;
	}

	/**
	 * Add a new Siplet configuration to the application.
	 * 
	 * @param new siplet (SipServletDesc)
	 */
	public void addSipServlet(SipServletDesc siplet) {
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "addSipServlets", 
					"addSipServlet - getRootURI = " + siplet.getSipApp().getRootURI());
		}
		
		if(!m_siplets.containsKey(siplet.getName())){
			m_siplets.put(siplet.getName(), siplet);
		}
		
	}
	
	/**
	 * Add a new Siplet configuration to the application.
	 * 
	 * @param siplets collection of siplets (SipServletDesc)
	 */
	public void addSipServlets(Collection<SipServletDesc> siplets) {
		
	
		
		// Check if there is main servlet added to the application
		for (SipServletDesc siplet : siplets){
			
			addSipServlet(siplet);
			
			
		}
		
		if (m_app_name == null && m_display_name == null) {
			// in case there is no display name for the application,
			// use this chance to generate one that is unique, assuming
			// no other application has the same set of siplets
			m_display_name = generateAppName();
		}
	}
	

	/**
	 * Get the list all Siplets contained by this application.
	 * 
	 * @return List of SipServletDesc
	 */
	public List<SipServletDesc> getSipServlets() {
		return new ArrayList<SipServletDesc>(m_siplets.values());
	}

	/**
	 * Search for a Siplet with a matching name.
	 * 
	 * @return Sip Servlet Desc object if available, otherwise null.
	 */
	public SipServletDesc getSipServlet(String name) {
		SipServletDesc siplet = m_siplets.get(name);
	

		return siplet;
	}

	/**
	 * Get the application's name as in SIP descriptor app-name (JSR289) or display-name (JSR116).
	 * 
	 * @return String
	 */
	public String getApplicationName() {
		return (m_app_name != null) ? m_app_name : m_display_name;
	}

	/**
	 * Get TimerListener class name associated with this application.
	 * 
	 * @return Name of class implementing TimerListener interface. If not
	 *         declared in xml configuration the function will return null.
	 */
	public TimerListener getTimerListener() {
		return m_timerListener;
	}


	/**
	 * add the listener to the correct listener map.
	 * @param listener
	 */
	public void addListenerObject(Object listener){
		if (listener instanceof TimerListener) {
			setTimerListener((TimerListener) listener);
		}

		if (listener instanceof SipApplicationSessionListener) {
			addListenerToMap((SipApplicationSessionListener)listener, m_appSessionListeners, "m_appSessionListeners");
		}

		if (listener instanceof SipApplicationSessionAttributeListener) {
			addListenerToMap((SipApplicationSessionAttributeListener)listener, m_appSessionAttributeListeners, "m_appSessionAttributeListeners");
		}
		if (listener instanceof SipSessionListener) {
			addListenerToMap((SipSessionListener)listener, m_sessionListeners, "m_sessionListeners");
		}

		if (listener instanceof SipSessionStateListener) {
			addListenerToMap((SipSessionStateListener)listener, m_sessionStateListeners, "m_sessionStateListeners");
		}

		if (listener instanceof SipApplicationSessionStateListener) {
			addListenerToMap((SipApplicationSessionStateListener)listener, m_appSessionStateListeners, "m_appSessionStateListeners");
		}
		
		if (listener instanceof SipApplicationSessionActivationListener) {
			addListenerToMap((SipApplicationSessionActivationListener)listener, m_appSessionActivationListeners, "m_appSessionActivationListeners");
		}

		if (listener instanceof SipErrorListener) {
			addListenerToMap((SipErrorListener)listener, m_errorListeners, "m_errorListeners");
		}

		if (listener instanceof SipSessionAttributeListener) {
			addListenerToMap((SipSessionAttributeListener)listener, m_sessionAttributeListeners, "m_sessionAttributeListeners");
		}
		
		if (listener instanceof SipServletListener) {
			addListenerToMap((SipServletListener)listener, m_sipServletListeners, "m_sipServletListeners");
		}
		
		if (listener instanceof UnmatchedMessageListener) {
			addListenerToMap((UnmatchedMessageListener)listener, m_unmatchedMessageListener, "m_unmatchedMessageListener");
		}
		
	}
	
	/**
	 * Add a listener to the application. Listener can be anyone of the known
	 * types: Timer, App Session or Session.
	 * 
	 * @param listenerClass
	 *            Full class name.
	 * @param classLoader
	 *            Class Loader for loading application specific classes.
	 */
	protected void addListener(String listenerClass) {
		// When using the XML parser outside the container, we may get null
		Object listener = null;

		// SPR #VKAE69MPJB
		// add single instance support for servlet acting as listener
		// works only for WAS environment

		listener = ServletsInstanceHolder.getInstance().getSipletinstance(
				getApplicationName(), listenerClass);
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "addListener",
					"searched Listener existance from web container for ["
							+ getApplicationName() + "][" + listenerClass
							+ "][" + listener + "]");
		}
		if (listener == null) {
			// I assume that we are running in the application context.
			// Which means that we can load the Class using the current thread
			// class loader.
			// TODO: Check the assumption above when running in WAS environment.
			try {
				ClassLoader originalThreadClassLoader = Thread.currentThread()
						.getContextClassLoader();
				// We set the Thread class loader so that things created by
				// reflection in
				// the listener constructor will be created by the application
				// class loader
				Thread.currentThread().setContextClassLoader(_webApp.getClassLoader());
				try {
					Class c = _webApp.getClassLoader().loadClass(listenerClass);
					listener = c.newInstance();
				} finally {
					Thread.currentThread().setContextClassLoader(
							originalThreadClassLoader);
				}
			} catch (ClassNotFoundException e) {
				if (c_logger.isErrorEnabled()) {
					Object[] args = { listenerClass, getApplicationName() };
					c_logger.error("error.listener.not.found",
							Situation.SITUATION_CREATE, args, e);
				}
			} catch (InstantiationException e) {
				if (c_logger.isErrorEnabled()) {
					Object[] args = { listenerClass, getApplicationName() };
					c_logger.error("error.init.listener.failed",
							Situation.SITUATION_CREATE, args, e);
				}
			} catch (IllegalAccessException e) {
				if (c_logger.isErrorEnabled()) {
					Object[] args = { listenerClass, getApplicationName() };
					c_logger.error("error.init.listener.failed",
							Situation.SITUATION_CREATE, args, e);
				}
			} catch (NoClassDefFoundError e) {
				if (c_logger.isErrorEnabled()) {
					Object[] args = { listenerClass, getApplicationName() };
					c_logger.error("error.init.listener.failed",
							Situation.SITUATION_CREATE, args, e);
				}
			}

			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addListener", "instance created");
			}
		} else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "addListener", "instance is siplet");
			}
		}

		addListenerObject(listener);
	}

	/**
	 * Sets the TimerListener class name associated with this application.
	 * 
	 * @param timerListener
	 */
	protected void setTimerListener(TimerListener timerListener) {
		if (null != m_timerListener) {
			if (c_logger.isErrorEnabled()) {
				Object[] args = { getApplicationName() };
				c_logger.error("error.too.many.timer.listeners",
						Situation.SITUATION_CREATE, args);
			}
		} else {
			m_timerListener = timerListener;

			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer b = new StringBuffer(64);
				b.append("Setting Timer Listener: ");
				b.append(m_timerListener);
				b.append(" For Application: ");
				b.append(getApplicationName());
				c_logger.traceDebug(this, "setTimerListener", b.toString());
			}
		}
	}

	/**
	 * replace the TimerListener instance associated with this application. this
	 * function should be called by the single instance for servlets acting as
	 * listeners only
	 * 
	 * @param timerListener
	 */
	protected void replaceTimerListener(TimerListener timerListener) {
		if ((m_timerListener != null)
				&& (m_timerListener.getClass().getName().equals(timerListener
						.getClass().getName()))) {

			m_timerListener = timerListener;

			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer b = new StringBuffer(64);
				b.append("Replacing Timer Listener: ");
				b.append(m_timerListener);
				b.append(" For Application: ");
				b.append(getApplicationName());
				c_logger.traceDebug(this, "replaceTimerListener", b.toString());
			}
		}
	}

	/**
	 * replace SipApplicationSessionListener to the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void replaceAppSessionListener(
			SipApplicationSessionListener listener) {
		replaceListener(listener, m_appSessionListeners, "m_appSessionListeners");
	}

	/**
	 * replace SipSessionListener to the map of listeners associated with this
	 * application. used for single instance servlets acting as listeners
	 * 
	 * @param listener
	 */
	protected void replaceSessionListener(SipSessionListener listener) {
		replaceListener(listener, m_sessionListeners, "m_sessionListeners");
	}

	/**
	 * replace Activation Session Listener to the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void replaceSipSessionStateListener(
			SipSessionStateListener listener) {
		replaceListener(listener, m_sessionStateListeners, "m_sessionStateListeners");
	}

	/**
	 * replace Application Session state Listener to the map of listeners
	 * associated with this application. used for single instance servlets
	 * acting as listeners
	 * 
	 * @param listener
	 */
	protected void replaceApplicationSessionStateListener(
			SipApplicationSessionStateListener listener) {
		replaceListener(listener, m_appSessionStateListeners, "m_appSessionStateListeners");
	}
	
	/**
	 * replace Application Session Activation Listener to the map of listeners
	 * associated with this application. used for single instance servlets
	 * acting as listeners
	 * 
	 * @param listener
	 */
	protected void replaceApplicationSessionActivationListener(
			SipApplicationSessionActivationListener listener) {
		replaceListener(listener, m_appSessionActivationListeners, "m_appSessionActivationListeners");
	}

	/**
	 * replace Session Attributes Listener to the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void replaceSessionAttributeListener(
			SipSessionAttributeListener listener) {
		replaceListener(listener, m_sessionAttributeListeners, "m_sessionAttributeListeners");
	}

	/**
	 * replace Session Attributes Listener to the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void replaceAppSessionAttributeListener(
			SipApplicationSessionAttributeListener listener) {
		replaceListener(listener, m_appSessionAttributeListeners, "m_appSessionAttributeListeners");
	}

	/**
	 * replace Sip Error Listener to the map of listeners associated with this
	 * application. used for single instance servlets acting as listeners
	 * 
	 * @param listener
	 */
	protected void replaceErrorListener(SipErrorListener listener) {
		replaceListener(listener, m_errorListeners, "m_errorListeners");
	}
	
	
	/**
	 * replace Session Attributes Listener to the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void replaceSipServletListener(
			SipServletListener listener) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "replaceSipServletListener",listener + "");
		}
		
		replaceListener(listener, m_sipServletListeners, "m_sipServletListeners");
	}


	/**
	 * Add a new Sip Error Listener to the map of listeners associated with
	 * this application.
	 * @param listener the listener to add
	 * @param listenerMap the map of the listeners from this type
	 * @param mapName the map name
	 */
	private <T extends EventListener> void addListenerToMap(T listener, HashMap<String,T> listenerMap, String mapName) {
		listenerMap.put(listener.getClass().getName(), listener);

		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer b = new StringBuffer(64);
			b.append("Adding Listener: ");
			b.append(listener);
			b.append(" To Map: ");
			b.append(mapName);
			b.append(" For Application: ");
			b.append(getApplicationName());
			c_logger.traceDebug(this, "addListenerToMap", b.toString());
		}
	}

	/**
	 * Replace a listener in a given map of listeners
	 * 
	 * @param listener the listener to add
	 * @param listenerMap the map of the listeners from this type
	 * @param mapName the map name
	 */
	private <T extends EventListener> void replaceListener(T listener, HashMap<String,T> listenerMap, String mapName) {
		EventListener orig = listenerMap.get(listener.getClass().getName());

	

		if (orig != null) {
			listenerMap.put(listener.getClass().getName(), listener);
			
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer b = new StringBuffer(64);
				b.append("Replacing Listener: ");
				b.append(listener);
				b.append(" In map: ");
				b.append(mapName);
				b.append(" For Application: ");
				b.append(getApplicationName());
				c_logger.traceDebug(this, "replaceListener", b.toString());
			}
		}
	}
	
	/**
	 * remove SipApplicationSessionListener from the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listenerClass
	 */
	protected void removeAppSessionListener(
			String listenerClass) {
		removeListener(listenerClass, m_appSessionListeners, "m_appSessionListeners");
	}

	/**
	 * remove SipSessionListener from the map of listeners associated with this
	 * application. used for single instance servlets acting as listeners
	 * 
	 * @param listenerClass
	 */
	protected void removeSessionListener(String listenerClass) {
		removeListener(listenerClass, m_sessionListeners, "m_sessionListeners");
	}

	/**
	 * remove Activation Session Listener from the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listenerClass
	 */
	protected void removeSipSessionStateListener(
			String listenerClass) {
		removeListener(listenerClass, m_sessionStateListeners, "m_sessionStateListeners");
	}

	/**
	 * remove Application Session state Listener from the map of listeners
	 * associated with this application. used for single instance servlets
	 * acting as listeners
	 * 
	 * @param listener
	 */
	protected void removeApplicationSessionStateListener(
			String listenerClass) {
		removeListener(listenerClass, m_sessionStateListeners, "m_sessionStateListeners");
	}
	
	/**
	 * remove Application Session Activation Listener from the map of listeners
	 * associated with this application. used for single instance servlets
	 * acting as listeners
	 * 
	 * @param listener
	 */
	protected void removeApplicationSessionActivationListener(
			String listenerClass) {
		removeListener(listenerClass, m_appSessionActivationListeners, "m_appSessionActivationListeners");
	}

	/**
	 * remove Session Attributes Listener from the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void removeSessionAttributeListener(
			String listenerClass) {
		removeListener(listenerClass, m_sessionAttributeListeners, "m_sessionAttributeListeners");
	}

	/**
	 * remove Session Attributes Listener from the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void removeAppSessionAttributeListener(
			String listenerClass) {
		removeListener(listenerClass, m_appSessionAttributeListeners, "m_appSessionAttributeListeners");
	}

	/**
	 * remove Sip Error Listener from the map of listeners associated with this
	 * application. used for single instance servlets acting as listeners
	 * 
	 * @param listener
	 */
	protected void removeErrorListener(String listenerClass) {
		removeListener(listenerClass, m_errorListeners, "m_errorListeners");
	}
	
	
	/**
	 * remove Session Attributes Listener from the map of listeners associated
	 * with this application. used for single instance servlets acting as
	 * listeners
	 * 
	 * @param listener
	 */
	protected void removeSipServletListener(
			String listenerClass) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "removeSipServletListener",listenerClass + "");
		}
		
		removeListener(listenerClass, m_sipServletListeners, "m_sipServletListeners");
	}
	
	/**
	 * remove the TimerListener instance associated with this application. this
	 * function should be called by the single instance for servlets acting as
	 * listeners only
	 * 
	 * @param timerListener
	 */
	protected void removeTimerListener() {
		m_timerListener = null;
		if (c_logger.isTraceDebugEnabled()) {
				StringBuffer b = new StringBuffer(64);
				b.append("Removing Timer Listener: ");
				b.append(" For Application: ");
				b.append(getApplicationName());
				c_logger.traceDebug(this, "removeTimerListener", b.toString());
		}
	}	

	/**
	 * Remove a listener instance associated with this application. this
	 * function should be called by the single instance for servlets acting as
	 * listeners only
	 *
	 * @param listener the listener to add
	 * @param listenerMap the map of the listeners from this type
	 * @param mapName the map name
	 */
	protected <T extends EventListener> void removeListener(String listenerClass, HashMap<String, T> listenerMap, String mapName) {
		
		EventListener orig = listenerMap.remove(listenerClass);

		if (orig != null) {
			
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer b = new StringBuffer(64);
				b.append("Removing Listener: ");
				b.append(" Class: " + listenerClass);
				b.append(" From map: " + mapName);
				b.append(" For Application: ");
				b.append(getApplicationName());

				c_logger.traceDebug(this, "removeListener", b.toString());
			}
		}
	}

	/**
	 * Gets the root uri of the WEB Application as it is deployed in WAS.
	 * 
	 * @return
	 */
	public String getRootURI() {
		return m_rootURI;
	}

	/**
	 * Sets the root uri of the WEB Application as it is deployed in WAS.
	 * 
	 * @param string
	 */
	public void setRootURI(String string) {
		if(m_rootURI == null) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setRootURI", string);
			}			
			m_rootURI = string;
		}
	}

	/**
	 * Gets the collection of SIP Application Sessions Listeners associated with this SIP
	 * application descriptor.
	 * 
	 * @return
	 */
	public Collection<SipApplicationSessionListener> getAppSessionListeners() {
		return m_appSessionListeners.values();
	}

	/**
	 * Gets the collection of SIP Sessions Listeners associated with this SIP application
	 * descriptor.
	 * 
	 * @return
	 */
	public Collection<SipSessionListener> getSessionListeners() {
		return m_sessionListeners.values();
	}

	/**
	 * Gets the collection of Activation Sessions Listeners associated with this SIP
	 * application descriptor.
	 * 
	 * @return
	 */
	public Collection<SipSessionStateListener> getSessionStateListeners() {
		return m_sessionStateListeners.values();
	}

	/**
	 * Gets the collection of Activation Sessions Listeners associated with this SIP
	 * application descriptor.
	 * 
	 * @return
	 */
	public Collection<SipApplicationSessionStateListener> getAppSessionStateListeners() {
		return m_appSessionStateListeners.values();
	}
	
	/**
	 * Gets the collection of Activation Sessions Listeners associated with this SIP
	 * application descriptor.
	 * 
	 * @return
	 */
	public Collection<SipApplicationSessionActivationListener> getAppSessionActivationListeners() {
		return m_appSessionActivationListeners.values();
	}

	/**
	 * Gets the collection of Session Attribute Listeners associated with this SIP
	 * application descriptor.
	 * 
	 * @return
	 */
	public Collection<SipSessionAttributeListener> getSessionAttributesListeners() {
		return m_sessionAttributeListeners.values();
	}

	/**
	 * Get the collection of Sip Servlet Listeners which are associated with this
	 * Sip application descriptor
	 * 
	 * @return
	 */
	public Collection<SipServletListener> getSipServletListeners() {
		return m_sipServletListeners.values();
	}

	/**
	 * Get the collection of unmatched message Listeners which are associated with this
	 * Sip application descriptor
	 * 
	 * @return
	 */
	public Collection<UnmatchedMessageListener> getUnmatchedMessagesListeners() {
		return m_unmatchedMessageListener.values();
	}
	/**
	 * Gets the collection of ApplicationSession Attribute Listeners associated with this
	 * SIP application descriptor.
	 * 
	 * @return
	 */
	public Collection<SipApplicationSessionAttributeListener> getAppSessionAttributesListeners() {
		return m_appSessionAttributeListeners.values();
	}

	/**
	 * Gets the collection of SIP Error Listeners associated with this SIP application
	 * descriptor.
	 * 
	 * @return
	 */
	public Collection<SipErrorListener> getErrorListeners() {
		return m_errorListeners.values();
	}

	/**
	 * Sets the time to live for Application Sessions
	 * 
	 * @param ttm
	 *            time in minutes
	 */
	public void setAppSessionTTL(int ttl) {
		if(!m_wasSet.get(PRIMITIVE_LOCATION.APP_TTL_BIT.ordinal())) {
			m_appSessionTTL = ttl;
			m_wasSet.set(PRIMITIVE_LOCATION.APP_TTL_BIT.ordinal());
		}
	}

	/**
	 * Gets the time to live for Application Sessions
	 * 
	 * @return time in minutes
	 */
	public int getAppSessionTTL() {
		return m_appSessionTTL;
	}

	/**
	 * Gets the default Siplet out of the set of Siplets that make this
	 * application. The default Siplet will be the first Siplet entry in the
	 * sip.xml file.
	 */
	public SipServletDesc getDefaultSiplet() {
		SipServletDesc sipletDesc;
		if (m_siplets.isEmpty()) {
			throw new RuntimeException(
					"No SipServlet was defined on sip.xml for this application: "
							+ getApplicationName());
		}
		sipletDesc = (SipServletDesc) m_siplets.values().iterator().next();

		return sipletDesc;
	}

	/**
	 * Add a new context parameters to the application.
	 * 
	 * @param name the context parameter name
	 * @param value the context parameter value
	 */
	public void addContextParam(String name, String value) {
		m_contextParams.put(name,value);
	}

	/**
	 * Get the map of all context parameters contained by this application.
	 * 
	 * @return a map of context parameters
	 */
	public Map<String,String> getAppContextParams() {
		return m_contextParams;
	}

	/**
	 * Gets the SIP Factory associated with this SIP App.
	 * 
	 * @return
	 */
	public SipFactory getSipFactory() {
		return m_sipFactory;
	}

	/**
	 * Sets the SIP Factory associated with this SIP App.
	 * 
	 * @param factory
	 */
	public void setSipFactory(SipFactory factory) {
		m_sipFactory = factory;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Sip application descriptor: " + getApplicationName();
	}

	/**
	 * Sets the port number that will be used when passing messages to the Web
	 * Container.
	 * 
	 * @param port
	 */
	public void setWebTransportrPort(int port) {
		if(!m_wasSet.get(PRIMITIVE_LOCATION.WEB_TRASNSPORT_PORT_BIT.ordinal())) {
			m_webTransportPort = port;
			m_wasSet.set(PRIMITIVE_LOCATION.WEB_TRASNSPORT_PORT_BIT.ordinal());
		}
	}

	/**
	 * Sets the is SSL flag that will be used when passing messages to the Web
	 * Container.
	 * 
	 * @param isSSL
	 */
	public void setWebTransportIsSSL(boolean isSSL) {
		
		if(!m_wasSet.get(PRIMITIVE_LOCATION.WEB_TRASNSPORT_SSL_BIT.ordinal())) {
			m_webTransportIsSSL = isSSL;
			m_wasSet.set(PRIMITIVE_LOCATION.WEB_TRASNSPORT_SSL_BIT.ordinal());
		}
	}

	/**
	 * Sets the host name that will be used when passing messages to the Web
	 * Container.
	 * 
	 * @param host
	 */
	public void setWebTransportHost(String host) {
		if(m_webTransportHost == null) {
			m_webTransportHost = host;
		}
	}

	/**
	 * Get the Web Transport Host associated with this SIP App
	 * 
	 * @return
	 */
	public String getWebTransportHost() {
		return m_webTransportHost;
	}

	/**
	 * Get the Web Transport Is SSL associated with this SIP App
	 * 
	 * @return
	 */
	public boolean getWebTransportIsSSL() {
		return m_webTransportIsSSL;
	}

	/**
	 * Get the Web Transport Port associated with this SIP App
	 * 
	 * @return Returns the m_webTransportPort.
	 */
	public int getWebTransportPort() {
		return m_webTransportPort;
	}

	/**
	 * Gets the matching Web App name for this applications
	 * 
	 * @return
	 */
	public String getWebAppName() {
		return m_webAppName;
	}

	/**
	 * Sets the matcing Web App name for this application
	 * 
	 * @param
	 */
	public void setWebAppName(String webAppName) {
		if(m_webAppName == null) {
			m_webAppName = webAppName;
		}
	}

	/**
	 * Indicates whether this application has been marked for distribution
	 */
	public boolean isDistributed() {
		
		return m_isDistributed;
	}

	/**
	 * Sets the is distributed state of this Sip App
	 */
	public void setIsDistributed(boolean distributed) {
		
		if(!m_wasSet.get(PRIMITIVE_LOCATION.IS_DISTRIBUTDED_BIT.ordinal())) {
			if (c_logger.isTraceEntryExitEnabled()) {
				Object[] params = { new Boolean(distributed) };
				c_logger.traceEntry(this, "setIsDistributed", params);
			}
			m_isDistributed = distributed;
			
			m_wasSet.set(PRIMITIVE_LOCATION.IS_DISTRIBUTDED_BIT.ordinal());
		}
		
	}

	/**
	 * Gets the default sequential search timeout for proxying operations
	 * associated with this application.
	 * 
	 * @return timeout value in seconds
	 */
	public int getProxyTimeout() {
		return _proxyTimeout;
	}

	/**
	 * Sets the default sequential search timeout for proxying operations
	 * associated with this application.
	 * 
	 * @param timeout
	 *            Time in seconds
	 * @return
	 */
	public void setProxyTimeout(int timeout) {
		if (timeout <= 0) {
			if (c_logger.isWarnEnabled()) {
				Object[] args = { Integer.toString(timeout) };
				c_logger.error("warn.invalid.timeout.value",
						Situation.SITUATION_CREATE, args);
			}
			return;
		}
		if(!m_wasSet.get(PRIMITIVE_LOCATION.PROXY_TIMEOUT_BIT.ordinal())) {

			_proxyTimeout = timeout;
			
			m_wasSet.set(PRIMITIVE_LOCATION.PROXY_TIMEOUT_BIT.ordinal());
		}
		
	}

	/**
	 * @return Returns the m_appIndexForPmi. This index represents each
	 *         application in PMI tables
	 */
	public Integer getAppIndexForPmi() {
		return m_appIndexForPmi;
	}

	/**
	 * Setting the pmi index
	 * 
	 * @param index
	 */
	public void setAppIndexForPmi(Integer index) {
		m_appIndexForPmi = index;
	}

	/**
	 * Get Application Realm name
	 * 
	 * @return
	 */
	public String getRealm() {
		return m_realm;
	}

	/**
	 * Set Application Realm name
	 * 
	 * @param realm
	 */
	public void setRealm(String realm) {
		if(m_realm.equals("sip")) {
			m_realm = realm;
		}
	}

	/**
	 * add security constraint
	 * 
	 * @param cons
	 */
	public void addSecurityConstraint(SecurityConstraint cons) {
		m_securityConstraints.add(cons);
	}

	/**
	 * get security constraints
	 * 
	 * @return
	 */
	public List<SecurityConstraint> getSipSecurityConstraints() {
		return m_securityConstraints;
	}

	/**
	 * @return Returns the _VirtualHostsList.
	 */
	public List<VirtualHostAlias> getVirtualHostAliases() {
		return _virtualHostAliases;
	}

	/**
	 * @param virtualHostsList
	 *            The _VirtualHostsList to set.
	 */
	public void setVirtualHost(String name, List virtualHostsList) {
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(null, "setVirtualHost", name, virtualHostsList);
		}
		_virtualHostName = name;
		_virtualHostAliases.clear();
		_vhPort = -1;
		_vhHost = null;

		for (int i = 0; i < virtualHostsList.size(); i++) {
			VirtualHostAlias vha = new VirtualHostAliasImpl();
			vha.init((String) virtualHostsList.get(i));
			_virtualHostAliases.add(vha);

			// Keep a local copy for fast access
			_vhPort = vha.getPort();
			_vhHost = vha.getHost();
		}
		if(name.equals(SipVirtualHostAdapter.DEFAULT_VH_ID)){//special case for default_host
			SipVirtualHostAdapter.fillWithDefaultHostAliases(_virtualHostAliases);
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(null, "setVirtualHost = " + "Port = " + _vhPort + " _Host = " + _vhHost);
		}
	}

	/**
	 * @return Returns the _virtualHostName.
	 */
	public String getVirtualHostName() {
		return _virtualHostName;
	}

	/**
	 * Utility method for getting one of the available ports associated with the
	 * virtual host associated with this application. Required when dispatchig
	 * requests into WebSphere.
	 */
	public int getVHPort() {
		return _vhPort;
	}

	/**
	 * Utility method for getting one of the available hosts associated with the
	 * virtual host associated with this application. Required when dispatchig
	 * requests into WebSphere.
	 */
	public String getVHHost() {
		return _vhHost;
	}

	/**
	 * Sets the ContextEstablisher
	 * 
	 * @param md
	 */
	public void setContextEstablisher(ContextEstablisher ce) {
		_contextEstablisher = ce;
	}

	/**
	 * Gets ContextEstablisher
	 * 
	 * @return
	 */
	public ContextEstablisher getContextEstablisher() {
		return _contextEstablisher;
	}

	/**
	 * @return the classloader specific to this application, or the default
	 *         classloader if not running under WAS. the default classloader
	 *         might be null.
	 */
	public ClassLoader getClassLoader() {
		ClassLoader classLoader = _contextEstablisher == null ? getClass()
				.getClassLoader() : _contextEstablisher
				.getApplicationClassLoader();
		return classLoader;
	}

	
	/**
	 * Getter of hasMainServlet indication
	 * @return hasMainServlet
	 */
	public boolean hasMainServlet() {
		
		return hasMainServlet;
	}

	/**
	 * Setter of hasMainServlet indication
	 * @param hasMainServlet
	 */
	private void setHasMainServlet(boolean hasMainServlet) {
		if(!m_wasSet.get(PRIMITIVE_LOCATION.HAS_MAIN_SERVLET_BIT.ordinal())) {
			this.hasMainServlet = hasMainServlet;
			m_wasSet.set(PRIMITIVE_LOCATION.HAS_MAIN_SERVLET_BIT.ordinal());
		}
	}

	/**
	 * Getter for mainSipletName
	 * @return
	 */
	public String getMainSipletName() {
		return mainSipletName;
	}

	/**
	 * Setter for mainSipletName
	 * @param mainSipletName
	 */
	public void setMainSipletName(String mainSipletName) {

		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("Setting main siplet: " + mainSipletName + " for applciation: " + getApplicationName() + " old name: " + this.mainSipletName);
		}
		if(this.mainSipletName == null) {
			this.mainSipletName = mainSipletName;
			this.setHasMainServlet(true);
		}
	}

	/**
	 * Getter for mainServlet
	 * @return
	 */
	public SipServletDesc getMainSiplet() {
		return mainServlet;
	}

	/**
	 * Setter for mainServlet
	 * @param mainServlet
	 */
	public void setMainServlet(SipServletDesc mainServlet) {
		if(this.mainServlet == null) {
			this.mainServlet = mainServlet;
		}
	}
	
	/**
	 * Determine if application is a JSR 289 or 116 according to sip.xml tags. 
	 * @return true for 289 applications and false for 116 applications
	 */
	public boolean isJSR289Application() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug("isJSR289Application: m_app_name = " + m_app_name + "  =  " + hasMainServlet);
		}
		return (m_app_name != null && !m_app_name.equals("") );
	}
	
	/**
	 * Return <app-name> tag value, if this method is triggered from a 116 application
	 * which doesn't contain the <app-name> tag then the return value will be the value
	 * of <display-name> tag
	 * 
	 * Return  
	 */
	public String getAppName() {
		if (isJSR289Application())
			return m_app_name;
		return m_display_name;
	}
	
	/**
	 * Return actual application name member value
	 * @return app_name member value
	 */
	public String getAppNameMember(){
		return m_app_name;
	}
	
	/**
	 * Return <display-name> tag
	 * @return
	 */
	public String getDisplayName() {
		return m_display_name;
	}

	/**
	 * Setter - assigns applicationKey 
	 */
	public void setApplicationKeyMethod() {
		
		if(_appKeyClassName == null || _appKeyMethodName == null){
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this, "setApplicationKeyMethod, @SipApplicationKey not defined");
			}
			return;
		}
		
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "setApplicationKeyMethod, method = " + _appKeyMethodName +
									" class = " + _appKeyClassName);
		}
		try {
			Method applicationKeyMethod = null;
			Class appClass = _webApp.getClassLoader().loadClass(_appKeyClassName);
			applicationKeyMethod = appClass.getDeclaredMethod(_appKeyMethodName, SipServletRequest.class);
			
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this, "setApplicationKeyMethod", applicationKeyMethod);
			}
			if (applicationKeyMethod != null) {
				this.applicationKeyHelper = new ApplicationKeyHelper(applicationKeyMethod);
			}
		} catch (ClassNotFoundException e) {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this, "setApplicationKeyMethod-> ClassNotFoundException", e.getStackTrace());
			}
		} catch (SecurityException e) {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this, "setApplicationKeyMethod-> SecurityException", e.getStackTrace());
			}
		} catch (NoSuchMethodException e) {
			if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceEntry(this, "setApplicationKeyMethod-> NoSuchMethodException", e.getStackTrace());
			}
		}

		
	}

	/**
	 * @return true if application key method was defined. 
	 * 		   false otherwise.
	 */
	public boolean hasApplicationKeyMethod(){
		return this.applicationKeyHelper != null;
	}
	
	/**
	 * @return ApplicationKeyHelper that class wraps up
	 * 		   the invokes of user defined application key.
	 */
	public ApplicationKeyHelper getApplicationKeyHelper(){
		return this.applicationKeyHelper;
	}
	
	/**
	 * Returns Servlet Context
	 * @return
	 */
	public ServletContext getServletContext() {
		return _webApp;
	}
	
	/**
	 * Get The WebApp of this application
	 * @return
	 */
	public WebApp getWebApp() {
		return _webApp;
	}

	/**
	 * Set the WebApp for this application
	 * @param webApp
	 */
	public void setWebApp(WebApp webApp) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setWebApp"," setWebApp = " + webApp);
		}
		if(this._webApp == null) {
			this._webApp = webApp;
		}
	}

	/**
	 * Annotated with @SipListener classes
	 * @param sipListeners
	 */
	public void setSipListeners(List<String> sipListeners) {
		if(_sipListeners == null){
			_sipListeners = new HashSet<String>();
		}
		_sipListeners.addAll(sipListeners);
		
	}

	/**
	 * Setup all listeners from the list of the annotated as @SipListener classes.
	 * Also connect between Listeners and Class Loader.
	 */
	public void setupSipListeners() {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setupSipListeners"," setupSipListeners, listeners = " + _sipListeners.toString());
		}
		
		for (String listeners: _sipListeners) {
			addListener(listeners);
		}
	}

	/**
	 * Setup ApplicationKey method.
	 * Set session context to be a SIP application.
	 */
	public void setupSipApplication() {
		setApplicationKeyMethod();
		
		SessionContext sc= (SessionContext)_webApp.getSessionContext();
		if(sc != null){
			sc.setSIPApplication(true);
		}
		else{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "setupSipApplication", "SessionContext is null for webApp = " + _webApp);
			}
		}
	}

	/**
	 * Store the name of the @SipApplicationKey class and method
	 * @param cName
	 * @param mName
	 */
	public void setAppKeyInfo(String cName, String mName) {
		_appKeyClassName = cName;
		_appKeyMethodName = mName;
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setAppKeyInfo"," appKeyClassName = " + _appKeyClassName + " appKeyMethodName = " + _appKeyMethodName);
		}
	}
	
	/**
	 * return the description annotation
	 */
	public String getDescription() {
		return m_description;
	}

	/**
	 * Set the description
	 */
	public void setDescription(String description) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setDescription", "description = " + description);
		}
		if(m_description == null) {
			m_description = description;
		}
	}

	/**
	 * Set if application was initialized
	 * @param wasInitialized
	 */
	public void setWasInitialized(boolean wasInitialized) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setWasInitialized", "wasInitialized = " + wasInitialized);
		}
		m_wasInitialized = wasInitialized;
	}

	/**
	 * return if application was initialized
	 * @param wasInitialized
	 */
	public boolean wasInitialized() {
		// TODO Auto-generated method stub
		return m_wasInitialized;
	}
	
	/**
	 * Set if application is during initialization
	 * @param isDuringWebAppInitialization
	 */
	public void setIsDuringWebAppInitialization(boolean isDuringWebAppInitialization) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "setIsDuringWebAppInitialization", "isDuringWebAppInitialization = " + isDuringWebAppInitialization);
		}
		m_isDuringWebAppInitialization = isDuringWebAppInitialization;
	}
	
	/**
	 * return if application is during web initialization
	 * @return m_isDuringWebAppInitialization
	 */
	public boolean isDuringWebAppInitialization() {
		return m_isDuringWebAppInitialization;
	}
	
	/**
	 * @return
	 */
	public Map<String, Condition> getServletsPatterns() {
		return m_servletsPatterns;
	}

	/**
	 * @param servletsPatterns
	 */
	public void setServletsPatterns(Map<String, Condition> servletsPatterns) {
		m_servletsPatterns = servletsPatterns;
	}

	/**
	 * Merges the details in web.xml with the details in sip.xml
	 */
	public void mergeWebApptoSip() {
		for (Servlet ddWebServlet:_ddWebApp.getServlets()) {
			mergeSerlvetLoadOnStartup(ddWebServlet);
			mergeInitParams(ddWebServlet);
		}
		mergeContextParams();

	}
	

	/**
	 * Merges servlet's load-on-startup of web.xml and sip.xml
	 * 
	 * @param ddWebServlet the servlet's description in web.xml
	 */
	private void mergeSerlvetLoadOnStartup(Servlet ddWebServlet) {
		int loadOnStartup = -1; // the default value.
		
		// only if the load-on-startup is set in web.xml, 
		// it might be the load-on-startup for the the siplet.
		if (ddWebServlet.isSetLoadOnStartup()) {
			if (ddWebServlet.isNullLoadOnStartup()) {
				loadOnStartup = 0;
			} else {
				loadOnStartup = ddWebServlet.getLoadOnStartup();
			}
			
			String servletName = ddWebServlet.getServletName();
			SipServletDesc desc = m_siplets.get(servletName);
			if (desc != null ) {
				desc.setServletLoadOnStartup(loadOnStartup);
			}	
		}
	}
	
	/**
	 * For each servlet, merge init-params of web.xml and sip.xml
	 * 
	 * @param ddWebServlet the servlet's description in web.xml
	 */
	private void mergeInitParams(Servlet ddWebServlet) {
		String servletName = ddWebServlet.getServletName();
		SipServletDesc desc = m_siplets.get(servletName);
		if (desc != null) {
			Map<String,String> descInitParams = desc.getInitParams();
			
			// For each init-param in web.xml, only if it is not defined in sip.xml 
			// then it is added to the map in SipServletDesc.
			for (ParamValue webServletInitParam: ddWebServlet.getInitParams()) {
				String name = webServletInitParam.getName();
				String value = webServletInitParam.getValue();
				
				if (!descInitParams.containsKey(name)) {
					descInitParams.put(name, value);
				}
			}
		}
	}
	
	/**
	 * Merges context-params of web.xml and sip.xml
	 */
	private void mergeContextParams() {
		
		// For each context-param in web.xml, only if it is not defined in sip.xml 
		// then it is added to the map.
		for (ParamValue ddContextParam:_ddWebApp.getContextParams()) {
			String name = ddContextParam.getName();
			String value = ddContextParam.getValue();
			if (!m_contextParams.containsKey(name)) {
				m_contextParams.put(name,value);
			}
		}
	}
	
	/**
	 * Updates shouldExcludeFromApplicationRouting flag for the application.
	 * it will be true in case the application configures in sip.xml the context-param of
	 * com.ibm.sip.container.exclude.application.routing
	 */
	public void updateShouldExcludeFromApplicationRouting(){
		String excludeInitParam = m_contextParams.get(EXCLUDE_APPLICATION_ROUTING_ATTRIBUTE);
		m_shouldExcludeFromApplicationRouting = excludeInitParam != null;
	}
	
	/**
	 * Checks whether should exclude the application from application routing.
	 * 
	 * @return true whether should exclude the application from application routing, otherwise false.
	 */
	public boolean shouldExcludeFromApplicationRouting(){
		return 	m_shouldExcludeFromApplicationRouting;
	}
	
	
	
	public void saveLoadOnStartupServlet(SipServlet sipServlet) {
		onStartupServlets.add(sipServlet);
	}
	
	public List<SipServlet> getLoadOnStartupServlets() {
		return  onStartupServlets;
	}
}
