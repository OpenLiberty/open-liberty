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
package com.ibm.ws.sip.container.servlets;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipSessionAttributeListener;
import javax.servlet.sip.SipSessionBindingEvent;
import javax.servlet.sip.SipSessionEvent;
import javax.servlet.sip.SipSessionListener;
import javax.servlet.sip.UAMode;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.sip.IBMSipSession;
import com.ibm.ws.sip.container.events.ContextEstablisher;
import com.ibm.ws.sip.container.events.EventsDispatcher;
import com.ibm.ws.sip.container.failover.ReplicatableImpl;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.tu.TransactionUserWrapper;
import com.ibm.ws.sip.container.was.ThreadLocalStorage;
//TODO Liberty import com.ibm.wsspi.sip.hamanagment.logicalname.ILogicalName;

/**
 * @author Amir Perlman, Feb 18, 2003
 * 
 * Implementation for Sip Session API.
 * @upodate 6/Feb/2008 Moti: should extends Abstract SipSession object
 * furthermore, this class instance should be use for non-HA environment (like standalone).
 * For DRS or other relication services , use derived classes.
 *  
 */
public class SipSessionImplementation extends ReplicatableImpl implements IBMSipSession  {
      
    /** Serialization UID (do not change) */
    private static final transient long serialVersionUID = -4671996658675281828L;

    /**
     * Class Logger.
     */
    private static final transient LogMgr c_logger = Log
            .get(SipSessionImplementation.class);
    
    /**
     * Creation time
     */
    protected long m_creationTime;

    
    /**
     * Map of attributes names associated with this session.
     */
    protected HashSet<String> m_attributes;
    
    /**
     * Last Accessed time.
     */
    protected long m_lastAccessedTime;

    /**
     * Handle for the Transaction User 
     */
    protected TransactionUserWrapper _transactionUser;
    
      
    /**
     * Indicates if the created notification was sent only for Derived SipSession 
     */
    protected transient boolean _createdNotificationWasSent = false;
        
    /**
     * Id of this SipSession
     */
    protected String _sipSessionId;
     
    /**
     * Indicates that this session is being invalidated
     */
    protected transient boolean _isDuringInvalidation = false;

	/**

     * Holds reference to B2B linked SipSession id.
     * We need it to get the linked SipSessionId after failover
     */
    protected String _b2bLinkedSessionId = null;
	
	/**
     * Holds reference to B2B linked SipSession
     */
    protected SipSessionImplementation _b2bLinkedSession;
    
	/**
	 * Has item changed .
	 */
	protected transient boolean _isDirty = false;

	/**
	 * Member that Developer can set to define that this SipSession
	 * should be invalidated by the Container when state of all related sessionse 
	 * will become TERMINATED or INITIAL (if not a dialog).
	 */
	protected boolean m_invalidateWhenReady = false;
	
	
	private transient String m_sessionCallID = null;
	
	
	private transient SipAppDesc m_appDesc = null;
	
	
    /**
     * Construct a new Sip Session
     * use SipSessionFactory to create instances
     * @param sipMessage
     *            The Sip Message that was associated with this session.
     * @param router
     *            Router used for passing message to siplets
     * @param isServerTransaction
     *            Indicates whether the session is associated with a server or a
     *            client transaction.
     * @pre sipMessage != null
     *  
     */
    SipSessionImplementation(TransactionUserWrapper tUser) {
    	_transactionUser = tUser;
        init();
    }

    
    /**
     * Ctor that is used to create deriver SipSession
     * use SipSessionFactory to create instances
     * @param tUser
     * @param origianlSipSession
     */
    SipSessionImplementation(TransactionUserWrapper tUser,
    		SipSessionImplementation originalSipSession) {
    	
    	if(c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug(this,"SipSessionImplementation",
        							" Create derived SipSession for base = " + originalSipSession.getId());
        }    	
//		A derived SipSession is essentially a copy of the SipSession associated with the original
//		request. It is constructed at the time the message creating the new dialog is passed to the application.
//		The new SipSession differs only in the values for the tag parameter of the address of the
//		callee (this is the value used for the To header in subsequent outgoing requests) and possibly the
//		route set. These values are derived from the dialog-establishing message as defined by the SIP
//		specification. The set of attributes in the cloned SipSession is the same as that of the original
//		in particular, the values are not cloned.
		
    	_transactionUser = tUser;
    	
    	init();
    	createdDerivedAttributes(originalSipSession);
    }
        
    /**
     * Return related TransactionUser
     * @return
     */
    public TransactionUserWrapper getTransactionUser() {
		return _transactionUser;
	}
    
    /**
     * @return true if session belong to JSR289 application, false otherwise
     */
    private boolean belongToJSR289Application(){
    	if (m_appDesc != null) {
    		return m_appDesc.isJSR289Application();
    	}
    	return false;
    }
    


	/**
     * Initialization operations common to all constructors
     *
     */
    protected void init() {
    	
    	String defaultHandler = ThreadLocalStorage.getSipServletName(); 
    	
    	try {
			this.setHandler(defaultHandler);
		} catch (ServletException e) {}
    	
    	setSharedId(_transactionUser.getId());
    	//Moti: June 2008: beware of invoking here virtual methods.
    	// this method is called from SipSessionImpl#ctor
    	// which means the derived classes and its virtual method are not 
    	// yet initialized. its a JVM limitation. you will
    	// get unexpected results if you try.
    	// therefore , I removed the call to virtual replicate() method.
    	m_creationTime = System.currentTimeMillis();
		m_lastAccessedTime = m_creationTime;
		m_appDesc = _transactionUser.getSipServletDesc().getSipApp();
		
		if(m_appDesc.isJSR289Application()){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "SipSessionImplementation",
						"this is 289 app - m_invalidateWhenReady is true");
			}
			m_invalidateWhenReady = true;
		}
		m_sessionCallID = _transactionUser.getCallId();
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "SipSessionImplementation",
					"New SipSession id = " + getId());
		}
		
		//Moti: for some reason, at creation time of a sip session
		// the TuWrapper is still null in the derived classes , so I
		// place here the real member.
		_transactionUser.logToContext(SipSessionSeqLog.INIT, getId());
		updatePerformanceAboutNewSession();
    }
    
    /**
     * @see javax.servlet.sip.SipSession#createRequest(String)
     */
    public SipServletRequest createRequest(String method) {
    	
    	if(_isDuringInvalidation){ 
	    		throw new IllegalStateException(
	    				"This session is already invalidated");
    		}
    	
    	SipServletRequest req= getInternalTuWrapper().createRequest(method);

    	return req;
    }
    
    /**
     * @see javax.servlet.sip.SipSession#getApplicationSession()
     */
    public SipApplicationSession getApplicationSession() {
    	
    	return getInternalTuWrapper().getApplicationSession(true);
    }
    
    /**
     * Returns the application session with which this <code>SipSession</code>
     * is associated.
     * 
     * @param create indicates whether or not to create the SAS, if it doesn't exist
     * 
     * @return the application session for this <code>SipSession</code>, or
     * null if none exists and create=false
     */
    public SipApplicationSession getApplicationSession(boolean create) {
    	return getInternalTuWrapper().getApplicationSession(create);
    }

    /**
     * @see javax.servlet.sip.SipSession#getAttribute(String)
     */
    public synchronized Object getAttribute(String name) {
    	
    	if (name == null) throw new NullPointerException();
    	
    	checkIsSessionValid();
        
        Object rValue = null; 
        if(null != m_attributes)
        {
        	rValue = SessionRepository.getInstance().getSipSessAttr(this , name);
        }
        
        return rValue;
    }
    
    /**
     * Helper method that checks if this SipSession was invalidated
     * and if this is a DIALOG stated if the TransactionUser is Active
     * @throws IllegalStateException if TU is inactive
     */
    private void checkIsSessionValid() throws IllegalStateException{
    	    	
    	if(getInternalTuWrapper() != null){
    		getInternalTuWrapper().ensureTUActive();
    	}
    }
   
    /**
     * @see javax.servlet.sip.SipSession#getAttributeNames()
     */
    public synchronized Enumeration getAttributeNames() {
    	
    	checkIsSessionValid();
        
        Enumeration e; 
        if(m_attributes != null)
        {
        	Vector v = new Vector(m_attributes);
        	e = v.elements();
        }
        else
        {
            e = EmptyEnumeration.getInstance();
        }
            
        return e;  
    }
    
    /**
     * @see javax.servlet.sip.SipSession#removeAttribute(String)
     */
    public synchronized void removeAttribute(String name) {

    	checkIsSessionValid();
    	
    	if( _isDuringInvalidation){
    		return;
    	}
    		
        if (null == m_attributes || m_attributes.isEmpty()) {
    		if (c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this, "removeAttribute", "No attributes to remove.");
    		}
            return;
        }
	
        synchronized (m_attributes) {
        	boolean removeSucceeded = m_attributes.remove(name);
        	if (!removeSucceeded){
        		if (c_logger.isTraceDebugEnabled()) {
        			c_logger.traceDebug(this, "removeAttribute", "Failed to remove attribute:" + name);
        		}
        		return;
        	}
        	
        }
    	
		EventsDispatcher.SipSessionAttributeBounding(this, name, m_appDesc, false);
		
    	Object obj = null;
		if( !_isDuringInvalidation){
			obj = SessionRepository.getInstance().removeAttribute(this, name);
    	}
        
        //Notify application about removed attribute
        sendAttrNotification(name,
        						LstNotificationType.SESSION_ATTRIBUTE_REMOVED);
    }
 
    
    /**
     * Helper method that used during creation derived SipSession and responsible to copy 
     * attributed form the original SipSession to the derived one.
     */
    private synchronized void createdDerivedAttributes(SipSessionImplementation origSipSession) {

    	checkIsSessionValid();
    	 
    	Enumeration attrNames = origSipSession.getAttributeNames();

         while (attrNames.hasMoreElements()) {
             String attrName = (String)attrNames.nextElement();
             //Moti: 23/Jan/2008 prevent NPE
             Object value = origSipSession.getAttribute(attrName);
             if (attrName==null || value == null) {
            	 if (c_logger.isTraceDebugEnabled()) {
                     c_logger.traceDebug(this, "createdDerivedAttributes",
                       "null value was detected for attribute:" + attrName);
                 }
            	 continue ; // do not copy this attribute...
             }
             
             if(m_attributes == null)
             {
            	 m_attributes = new LinkedHashSet(2);
             }
             SessionRepository.getInstance().putSSAttr(this, attrName, value);
             m_attributes.add(attrName);
             _isDirty=true;
         } 
    }
    
    /**
     * @see javax.servlet.sip.SipSession#setAttribute(String, Object)
     */
    public synchronized void setAttribute(String name, Object attribute) {
        Object prev = changeAttribute(name, attribute);
    	_isDirty = true;
    }

    /**
     * 
     * @param name - the attribute name
     * @param value - the attribute value
     * @return The previous attribute value (if overriden). otherwise null.
     */
	protected Object changeAttribute(String name, Object value) {
		
    	// javadoc explicit
    	if (name == null)
    		throw new NullPointerException("name of attribute is null"); 
		
		checkIsSessionValid();
		//SPR #RDUH6C323P
    	if(m_appDesc != null && m_appDesc.isDistributed() &&
            value instanceof Serializable == false){
            if (c_logger.isTraceDebugEnabled()) {
                c_logger.traceDebug(this, "changeAttribute",
                  "Attribute must implement Serializable ... attrName = " + name
                  + this);
            }
            throw new IllegalArgumentException(
            		"Attribute not Serializable: Attribute name=" + name + ", Attribute=" + value);            
        }
        
        if(null == m_attributes)
        {
            m_attributes = new LinkedHashSet<String>(2);
        }
        
        Object prev = null;
        synchronized (m_attributes) {
        	prev =  SessionRepository.getInstance().putSSAttr(this, name, value);
        	m_attributes.add(name);
        }
        
        
//      Notify object about bounded value
        EventsDispatcher.SipSessionAttributeBounding(this, name, m_appDesc, true);
        
        if (prev == null) {
            sendAttrNotification(name, 
            						LstNotificationType.SESSION_ATTRIBUTE_ADDED);
        }
        else {
        	sendAttrNotification(name, 
        						LstNotificationType.SESSION_ATTRIBUTE_REPLACED);
        }
		return prev;
	}

  	/**
     * Send event to Applications listener. This is a common method used  to
     * notify listeners.
     * @param listeners
     * @param evt
     * @param attrNotifyType
     */
    private <T extends EventListener> void  sendEvent( Collection<T> listeners,
    						Object evt,
    						LstNotificationType attrNotifyType) {
		
    	Iterator iter = listeners.iterator();

		
    	ContextEstablisher contextEstablisher = m_appDesc.getContextEstablisher();
		
		ClassLoader currentThreaClassLoader = null;
		
		try {
			if (contextEstablisher != null) {
				currentThreaClassLoader = contextEstablisher
						.getThreadCurrentClassLoader();
				contextEstablisher.establishContext();
			}
			while (iter.hasNext()) {
				try {
					
				switch (attrNotifyType) {
				case SESSION_ATTRIBUTE_ADDED:
					((SipSessionAttributeListener) iter.next())
						.attributeAdded((SipSessionBindingEvent)evt);
					break;	
				case SESSION_ATTRIBUTE_REPLACED:
					((SipSessionAttributeListener) iter.next())
						.attributeReplaced((SipSessionBindingEvent)evt);
					break;	
				case SESSION_ATTRIBUTE_REMOVED:
					((SipSessionAttributeListener) iter.next())
						.attributeRemoved((SipSessionBindingEvent)evt);
					break;
				case SESSION_CREATED:
					((SipSessionListener) iter.next()).
							sessionCreated((SipSessionEvent)evt);
					break;
				case SESSION_DESTROYED:
					((SipSessionListener) iter.next()).
							sessionDestroyed((SipSessionEvent)evt);
					break;
					
				case SESSION_READY_TO_INVALIDATE:
					((SipSessionListener) iter.next()).
							sessionReadyToInvalidate((SipSessionEvent)evt);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger
							.traceDebug(this, "sendSessionReadyToInvalidateEvt",
									"Exception  = " + e.getMessage());
				}
			}
		}
		} 
		finally {
			if (contextEstablisher != null) {
				contextEstablisher.removeContext(currentThreaClassLoader);
			}
		}
    }
    
    /**
     * Send a attribute Added notification to Attributes Listeners.
     */
    public void sendAttrNotification(String attrName, 
    									LstNotificationType type) {
        if (c_logger.isTraceEntryExitEnabled()) {
            StringBuffer buff = new StringBuffer(attrName);
            buff.append(type);
            c_logger.traceEntryExit(this, "sendAttributeAddedNotify", buff
                    .toString());
        }

        if(m_appDesc != null){
	        Collection<SipSessionAttributeListener> listeners = m_appDesc.getSessionAttributesListeners();
	
			if (!listeners.isEmpty()) {
				SipSessionBindingEvent evt = 
							new SipSessionBindingEvent(this,attrName);
				sendEvent(listeners,evt,type);
			}
        }		
        else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendAttributeAddedNotify",
						"Unable to send notification, "
								+ "SIP app Descriptor not available");
			}
		}
    }
    
    /**
	 * @see javax.servlet.sip.SipSession#getCallId()
	 */
    public String getCallId() {

    	// Keep WAS6.1 behavior 
    	if (m_appDesc != null && !m_appDesc.isJSR289Application()){
	    	if(getInternalTuWrapper().isTUDialog() == false){
	    		// in the JSR 116 in section 10.2.2.1 -  
	    		// only the dialog should have the callId in the SipSession
	    		if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "getLocalParty", 
							"This SipSession is not a dialog - callId will be null");
				}
	    		return null;
	    	}
    	}
        return m_sessionCallID;
    }

    
    /**
	 * @see javax.servlet.sip.SipSession#getCreationTime()
	 */
    public long getCreationTime() {
    	checkIsSessionValid();
    	return m_creationTime;
    }

    /**
     * @see javax.servlet.sip.SipSession#getLastAccessedTime()
     */
    public long getLastAccessedTime() {
        return m_lastAccessedTime;
    }

    /**
     * @see javax.servlet.sip.SipSession#getLocalParty()
     */
    public Address getLocalParty() {
    	if(!m_appDesc.isJSR289Application() && _transactionUser != null && !getInternalTuWrapper().isTUDialog()){
    		//TODO only in 116, need to check if this is at all true
//    		in the jsr 116 in section 10.2.2.1 -  
//    		only the dialog should have the localParty in the SipSession
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getLocalParty", 
						"This SipSession is not a dialog - localParty will be null");
			}
    		return null;
    	}
    	return getInternalTuWrapper().getLocalParty();
    }

    /**
     * @see javax.servlet.sip.SipSession#getRemoteParty()
     */
    public Address getRemoteParty() {
    	if( _transactionUser != null && !m_appDesc.isJSR289Application() && !getInternalTuWrapper().isTUDialog()){
    		//TODO only in 116, need to check if this is at all true
//    		in the jsr 116 in section 10.2.2.1 -  
//    		only the dialog should have the remoteParty in the SipSession
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "getLocalParty", 
						"This SipSession is not a dialog - repmoteParty will be null");
			}
    		return null;
    	}
        return getInternalTuWrapper().getRemoteParty();
    }

    /**
     * @see javax.servlet.sip.SipSession#invalidate()
     */
    public void invalidate() {
    	
    	// javadoc explicit 
    	if (!this.isValid()) throw new IllegalStateException("Can not be called on invalidated SipSession");
    	
    	synchronized (getInternalTuWrapper().getSynchronizer()) {
    		checkIsSessionValid();
	     	getInternalTuWrapper().invalidateTU(true, true);    	
    	}
    }
    
    /** 
     * Removed from javax - hide 289 API
     * @see javax.servlet.sip.SipSession#isOngoingTransaction()
     */
    public boolean hasOngoingTransaction(){
    	return getInternalTuWrapper().hasOngoingTransactions();
    }
    
    /**
     * Send notifications about invalidated SipSession
     */
    public synchronized void invalidateSipSession() {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this, "invalidateSipSession", getId());
    	}
    	
    	//if we are are failover we need to make sure that this session is linked to 
    	//its linked session before we remove it from the session repository tables.
    	//this call is linking the 2 sessions and saving local references that does not exist
    	//after failover.
    	getLinkedSession();
    	
    	if (_isDuringInvalidation) {
    		return;
    	}
    	
    	_isDuringInvalidation = true;
    	try{
    		sendSessionNotification(LstNotificationType.SESSION_DESTROYED);
    	 	
    		//Cleanup Attributes table
    		if(m_attributes == null || m_attributes.isEmpty()){
    			if (c_logger.isTraceDebugEnabled()) {
    				c_logger.traceDebug(this, "invalidateSipSession", "No attributes");
    			}    		
    		} else {
    			synchronized (m_attributes) {
    				EventsDispatcher.SipSessionAllAttributeUnbounding(this, m_appDesc);
    			}
    		}    			
    	} finally{
    		removeFromStorage();
    		updatePerformanceAboutInvalidatedSession();
    		if (c_logger.isTraceEntryExitEnabled()) {
    			c_logger.traceExit(this, "invalidateSipSession",getId());
    		}
    	}
    }
    
    /**
	 * Update Performance manager about new SipSession that created
	 * 
	 */
    public void updatePerformanceAboutNewSession() {

    	PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null && null != m_appDesc) {
			// Notify the Performance manager about new SipSession
			perfMgr.sipSessionCreated(m_appDesc.getApplicationName(), m_appDesc.getAppIndexForPmi());
		} 
		else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updatePerformance",
						"Unable to update PerfManager "
								+ "SIP app descriptor not available");
			}
		}
	}
    	
    
    /**
	 * Update Performance manager about new SipSession that created
	 * 
	 */
    public void updatePerformanceAboutInvalidatedSession() {
    	PerformanceMgr perfMgr = PerformanceMgr.getInstance();
		if (perfMgr != null && null != m_appDesc) {
			// Notify the Performance manager about new SipSession
			perfMgr.sipSessionDestroyed(m_appDesc.getApplicationName(), m_appDesc.getAppIndexForPmi());
		} 
		else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updatePerformanceAboutInvalidatedSession",
						"Unable to update PerfManager "
								+ "SIP app descriptor not available");
			}
		}
    }
    
    /**
     * Send created Notification for derived SipSession
     */
    public void sendDerivedSipSessionCreatedNotification() {
    	if (! _createdNotificationWasSent ){
    		sendSessionNotification(LstNotificationType.SESSION_CREATED);
//			If this SipSession is Derived SipSession we should set the flag to prevent sending this notification
//			at the next time
    		_createdNotificationWasSent = true;
    	}
    }
    
    /**
     * Send a attribute Added notification to Attributes Listeners.
     */
    public void sendSessionNotification(LstNotificationType type) {
    	
        if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntryExit(this, "sendSessionNotification", type);
        }

        if(m_appDesc != null){
	        Collection<SipSessionListener> listeners = m_appDesc.getSessionListeners();
	
			if (!listeners.isEmpty()) {
				SipSessionEvent evt = new SipSessionEvent(this);
				sendEvent(listeners,evt,type);
			}
        }		
        else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendSessionNotification",
						"Unable to send notification, "
								+ "SIP app Descriptor not available");
			}
		}
    }

    /**
	 * Send a session activation notification to session listeners on case 
	 * of activation after failover. This method can only be called after 
	 * the attributes and the transactionUser were already set on the activation.
	 * @param activation if true notification is for activation otherwise for passivation
	 */
    public void notifyOnActivationOrPassivation( boolean activation) {
    	if (c_logger.isTraceEntryExitEnabled()) {
    		c_logger.traceEntry(this,"notifyOnAppSessionActivationOrPassivation", new Object[] {
    							activation , _sipSessionId});
    	}
    	if (m_appDesc == null) {
    		m_appDesc = getInternalTuWrapper().getSipServletDesc().getSipApp();
    	}
    	
    	if (m_sessionCallID == null) {
    		m_sessionCallID = getInternalTuWrapper().getInternalCallId();
    	}
    	
		if (null != m_appDesc) {
			if( activation){
				EventsDispatcher.SipSessionActivated(this, m_appDesc);
				updatePerformanceAboutNewSession();
			}else{
				EventsDispatcher.SipSessionWillPassivate(this, m_appDesc);
			}
		} else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				StringBuffer buff = new StringBuffer(100);
				buff.append("Unable to send Session Activate notification, ");
				buff.append("SIP app descriptor not available");
				buff.append("sessionId = ");
				buff.append(getId());
				c_logger.traceDebug(this, "sendSessionActivateNotification",
						buff.toString());
			}
		}
	}

    /**
	 * @see javax.servlet.sip.SipSession#setHandler(String)
	 */
    public  void setHandler(String name) throws ServletException {
    	
    	// javadoc explicit 
		if (!this.isValid() && this.belongToJSR289Application())
			throw new IllegalStateException("Can not setInvalidateWhenReady on invalid session");

    	
    	synchronized (getInternalTuWrapper().getSynchronizer()) {
    		getInternalTuWrapper().setHandler(name);
    	}
    	
    }
       
    /**
     * Helper method that sets the Last Accessed time to the SipSession
     */
    public void setLastAccessedTime(){
    	m_lastAccessedTime = System.currentTimeMillis();
    	_isDirty = true;
    }
     
    /**
     * Helper method that sets the Last Accessed time to the SipApplicationSession
     */
    public void setAppLastAccessedTime(){
    	SipApplicationSessionImpl sipApp = getInternalTuWrapper().getAppSessionForInternalUse();
		if(sipApp != null){
			sipApp.setLastAccessedTime();
		}
    }
    
    /**
     * @see com.ibm.ws.sip.container.failover.Replicatable#store()
     */
    public void store(){
    	SessionRepository.getInstance().put(this);
    }

    
    /**
     * Setting transaction user. Using this method on activation after failover
     * (in TU activation())
     * @param transactionUser
     */
    public void setTransactionUser(TransactionUserWrapper transactionUser){
    	_transactionUser = transactionUser;
    }
    

	/**
	 * @see javax.servlet.sip.SipSession#getId()
	 */
	public String getId() {
		return getSharedId();
	}

	
	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#removeFromStorage()
	 */
	public void removeFromStorage() {
		//removing the attribute will be done inside removeSipSession
		SessionRepository.getInstance().removeSipSession(this);
	}

	public void removeAllAttributes() {
		if( m_attributes != null && !m_attributes.isEmpty()){
        	synchronized(m_attributes){
	        	for( Iterator itr = m_attributes.iterator(); itr.hasNext();){
	        		String attrName = (String)itr.next();
	        		SessionRepository.getInstance().removeAttribute(this, attrName);
	        	}
			    m_attributes.clear();
	        }
        }
	}
	
	/**
	 * @see com.ibm.ws.sip.container.failover.Replicatable#notifyOnActivation()
	 */
	public void notifyOnActivation(){
		notifyOnActivationOrPassivation(true);
	}


	


    /**
     * Returns the Id of the related Application Session
     */
    public String getApplicationSessionId(){
    	return getInternalTuWrapper().getApplicationId();
    }

	/** 
	 * @see com.ibm.websphere.sip.IBMSipSession#getRelatedSipSession()
	 */
    public SipSession getRelatedSipSession() {
		return getInternalTuWrapper().getRelatedSipSession();
	}
        
	/**
	 * @see javax.servlet.sip.SipSession#isValid()
	 */
    public boolean isValid() {
		return getInternalTuWrapper().isValid();
	}
	
    /**
     * @see com.ibm.websphere.sip.IBMSipSession#getRegion()
     */
    public SipApplicationRoutingRegion getRegion() {
    	return this.getInternalTuWrapper().getRegion();
	}


    /**
	 * @see com.ibm.ws.sip.container.servlets.SipSession#setOutboundInterface()
	 * @param uri - the SipURI which represents the outbound interface
     * @throws IllegalStateException - if this method is called on an invalidated session
     * @throws IllegalArgumentException - if the uri is not understood by the container 
     * as one of its outbound interface
     * @throws NullPointerException - on null uri
	 */
    public void setOutboundInterface(InetSocketAddress address)
    	throws NullPointerException,IllegalStateException,IllegalArgumentException
    {
    	if (address == null) {
    		throw new NullPointerException();
    	}
    	//if the session is invalid next line will throw and exception.
    	checkIsSessionValid();
    	
    	if ((SipProxyInfo.getInstance().getIndexOfIface(address, "udp") < 0) &&
    		(SipProxyInfo.getInstance().getIndexOfIface(address, "tcp") < 0) &&
    	    (SipProxyInfo.getInstance().getIndexOfIface(address, "tls") < 0))
    	{
    		throw new IllegalArgumentException("address:"+address+" is not an allowed outbound interface.");
    	}
    	
    	//	Make sure the transactionUser has the outbound interface to use.
    	getInternalTuWrapper().setOutboundInterface(address);
	}
    
	public void setOutboundInterface(InetAddress address) throws IllegalStateException, IllegalArgumentException, NullPointerException
	{
    	if (address == null) {
    		throw new NullPointerException();
    	}
    	//if the session is invalid next line will throw and exception.
    	checkIsSessionValid();
    	
    	if ((SipProxyInfo.getInstance().getIndexOfIface(address, "udp") < 0) &&
    		(SipProxyInfo.getInstance().getIndexOfIface(address, "tcp") < 0) &&
    	    (SipProxyInfo.getInstance().getIndexOfIface(address, "tls") < 0))
    	{
    		throw new IllegalArgumentException("address:"+address+" is not an allowed outbound interface.");
    	}
    	
    	//	Make sure the transactionUser has the outbound interface to use.
    	getInternalTuWrapper().setOutboundInterface(address);
	}
	    
	public List<SipServletMessage> getPendingMessages(UAMode mode){
		return getInternalTuWrapper().getPendingMessages(mode);
	}
	
	/**
	 * Link one sip session to another
	 * @param session a session to link to this session
	 * 
	 * @throws IllegalStateException in case this application has already retrieved a proxy
	 */
	public void linkSipSession(SipSessionImplementation session) 
		throws IllegalStateException{
		
		if(getInternalTuWrapper().isProxying()){
			throw new IllegalStateException("the application has already retrieved a proxy");
		}
		
		_b2bLinkedSession = session;
		_b2bLinkedSessionId = session.getId();
		getInternalTuWrapper().setB2buaMode();
	}
	
	/**
	 * Link one sip session to another
	 * @param session a session to link to this session
	 * 
	 * @throws IllegalStateException in case this application has already retrieved a proxy
	 */
	public void linkSipSessionAfterFailover(SipSessionImplementation session) {
		if (c_logger.isTraceDebugEnabled()) {
			StringBuffer buff = new StringBuffer(100);
			buff.append("Current session = ");
			buff.append(getId());
			buff.append(" Link session = ");
			buff.append(session.getId());
			c_logger.traceDebug(this, "linkSipSessionAfterFailover",
					buff.toString());
		}
		
		_b2bLinkedSession = session;
		_b2bLinkedSessionId = session.getId();
	}
	
	
	
	/**
	 * Removing the B2B link to the other session
	 */
	public void unlinkSipSession(){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(SipSessionImplementation.class.getName(),
					"unlinkSipSession");
		}
		
		_b2bLinkedSession = null;
		_b2bLinkedSessionId = null;
	}
	
    /**
	 * Returns the B2B linked SipSession, or null if none
	 */
    public SipSessionImplementation getLinkedSession() {
    	return _b2bLinkedSession;
	}
    
    /**
     * Retrieve is failed response sent on dialog initial request
     * 
     * @return true or false 
     */
    public boolean isFailedResponseSent(){
    	return getInternalTuWrapper().isFailedResponseSent();
    }
    
    /**
     * Retrieve is session has been terminated
     * @return true or false 
     */
    public boolean isTerminated() {
		return getInternalTuWrapper().isTerminated();
	}
    
    /**
     *  Removed from javax - hide 289 API
     *  @see javax.servlet.sip.SipSession#getState()
     */
    public State getState(){
    	
    	// javadoc explicit 
    	if (!this.isValid())
    		throw new IllegalStateException("Can not return state of invalid session");
    	
    	return getInternalTuWrapper().getState();
    }
    
    /**
     *  Removed from javax - hide 289 API
     *  @see com.ibm.websphere.sip.IBMSipSession#getSubscriberURI()
     */
    public URI getSubscriberURI(){
    	return this.getInternalTuWrapper().getSubscriberUri();
    }
    
	public boolean isDoingInvalidation()
	{
		return _isDuringInvalidation;
	}

	/**
	 *  @see javax.servlet.sip.SipSession#getInvalidateWhenReady()
	 */
    public boolean getInvalidateWhenReady() throws IllegalStateException {
    	
    	// javadoc explicit 
    	if (!this.isValid()) throw new IllegalStateException("Can not be called on invalidated SipSession");
    	
		return m_invalidateWhenReady;
	}

    /**
	 *  @see javax.servlet.sip.SipSession#getServletContext()
     */
    public ServletContext getServletContext() {

		if (m_appDesc != null) {
			return m_appDesc.getContextEstablisher().getServletContext();
		}

		return null;
	}

	/**
	 *  @see javax.servlet.sip.SipSession#isReadyToInvalidate()
	 */
	public boolean isReadyToInvalidate() throws IllegalStateException {
		
    	// javadoc explicit 
		if (!this.isValid())
			throw new IllegalStateException("The method is called on invalid session");
		
		return getInternalTuWrapper().isReadyToInvalidate();
	}


	 /**
     * This method might be executed multi-threaded or single threaded,
     * depends on the TasksInvoker definition 
     * @see com.ibm.ws.sip.container.events.TasksInvoker
     * @see com.ibm.ws.sip.container.SipContainer#setTimerInvoker()
     * @see java.lang.Runnable#run()
     */
	public void readyToInvalidate() {
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { getId(), isValid()};
  			c_logger.traceEntry(this, " readyToInvalidate", params);
  		}
		SipApplicationSessionImpl appSession = null;
		
		if (isValid()) {
			// First check if this flag is still true.
			if (getInvalidateWhenReady() == true) {

				// Notify listeners that the session expired.
				sendSessionReadyToInvalidateEvt();

				// Check if session life time has been extended by the
				// listeners.
				if (getInvalidateWhenReady() == true) {
					
					// keep reference to the sip app session before invalidating.
					// once SipSession is invalidate we cannot retrieve this reference
					appSession = (SipApplicationSessionImpl) getApplicationSession();
					
					// Session expired - Invalidate.
					try {
						invalidate();
					} 
					catch (IllegalStateException e) {
						if (c_logger.isTraceDebugEnabled()) {
							c_logger
									.traceDebug(this, "readyToInvalidate",
											"SipApplication session was already invalidated");
						}
					}
				} else {
					if (c_logger.isTraceDebugEnabled()) {
						c_logger
								.traceDebug(this, "readyToInvalidate",
										"getInvalidateWhenReady() is false - invalidate later");
					}
				}

				if (appSession == null) {
					appSession = (SipApplicationSessionImpl) getApplicationSession();
				}
				// first verifying that the sip app session wasn't already validated.
				if (appSession != null) {
					if (appSession.getInvalidateWhenReady() && appSession.isReadyToInvalidate()) {
						appSession.readyToInvalidate();
					}					
				}

			}
		}
        else
        {
        	if (c_logger.isTraceDebugEnabled()) {
                c_logger
                    .traceDebug(this, "readyToInvalidate",
                                "Session has already been invalidated. Just ignore.");
            } 
        }
	}
	
	 /**
		 * Method that send notification to listerens when this SipSession is
		 * ready to be invalidated.
		 */
	private void sendSessionReadyToInvalidateEvt() {
		if (c_logger.isTraceEntryExitEnabled()) {
            c_logger.traceEntryExit(this, "sendSessionReadyToInvalidateEvt",getId());
        }

        if(m_appDesc != null){
	        Collection<SipSessionListener> listeners = m_appDesc.getSessionListeners();
	
			if (!listeners.isEmpty()) {
				SipSessionEvent evt = new SipSessionEvent(this);
				sendEvent(listeners,evt,LstNotificationType.SESSION_READY_TO_INVALIDATE);
			}
        }		
        else {
			// Will happen for application sessions created through the
			// factory.
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "sendSessionNotification",
						"Unable to send notification, "
								+ "SIP app Descriptor not available");
			}
		}
	}

	
	/**
	 *  @see javax.servlet.sip.SipSession#setInvalidateWhenReady(boolean)
	 */
	public void setInvalidateWhenReady(boolean invalidateWhenReady) throws IllegalStateException {
		if (c_logger.isTraceEntryExitEnabled()) {
  			Object[] params = { getId(),invalidateWhenReady};
  			c_logger.traceEntry(this, " setInvalidateWhenReady", params);
  		}
		
    	// javadoc explicit 
		if (!this.isValid())
			throw new IllegalStateException("Can not setInvalidateWhenReady on invalid session");
		
		m_invalidateWhenReady = invalidateWhenReady;
		
	}

	public TransactionUserWrapper getInternalTuWrapper()
	{
		//Moti: defect 568973. made public for ObjectGrid operation.
		return _transactionUser;
	}
	
	/**
	 * 
	 * @param key
	 * @return true if we have such attribute
	 */
	public boolean isContainsAttr(String key)     
	{    	
		//Moti: defect 568973 this method is being used in ObjectGrid code.
		return m_attributes.contains(key);    
	}
}
