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

import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.utils.ras.SibTr;

public class JsObject implements RuntimeEventListener {

  public static final String $sccsid = "@(#) 1.33 src/com/ibm/ws/sib/admin/internal/JsObject.java, SIB.admin";
  private static final String CLASS_NAME = "com.ibm.ws.sib.admin.internal.JsObject";
  private static final TraceComponent tc = SibTr.register(JsObject.class,JsConstants.TRGRP_AS,JsConstants.MSG_BUNDLE);
  static MBeanServer mbs ;

  // Debugging aid
  static {
    if (tc.isDebugEnabled())
      SibTr.debug(tc, "Source info: src/com/ibm/ws/sib/admin/impl/JsObject.java, SIB.admin");
    mbs= ManagementFactory.getPlatformMBeanServer();
  }

  // JMX ObjectName
  private ObjectName iObjectName;

  // The MBean type
  private String _mbeanType = null;

  // The name of this object
  private String _name = null;

  // Has instance been activated as an MBean
  private boolean _activated = false;

  // SIB sequence numbering for Event Notifications.
  // Sequence numbering common to WAS would be better.
  private static long sequenceNumber = 0;

  /**
   * Constructor: Create a default instance 
   */
  public JsObject() {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, CLASS_NAME + ".<init>");
    if (tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME + ".<init>");
  }

  /**
   * Constructor: Create a default instance with the supplied name
   * @param name
   * @deprecated This constructor is believed to be superflous
   */
  public JsObject(String name) {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, CLASS_NAME + ".<init>", new Object[] { name });
    _name = name;
    if (tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME + ".<init>");
  }

  /**
   * Constructor: Create an instance which is to be rendered as an MBean
   * @param mbeanType
   * @param name
   */
  public JsObject(String mbeanType, String name) {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, CLASS_NAME + ".<init>", new Object[] { mbeanType, name });
    _mbeanType = mbeanType;
    _name = name;
    if (tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME + ".<init>");
  }

  /**
   * Constructor: Create an instance which is to be rendered as an MBean. The
   * MBean configID is derived from the supplied Liberty Configuration
   * configuration ConfigObject.
   * @param mbeanType
   * @param eo
   * @param name
   */
  public JsObject(String mbeanType, StandardMBean eo, String name) {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, CLASS_NAME + ".<init>", new Object[] { mbeanType, name });
    _mbeanType = mbeanType;
    _name = name;
//    activateMBean(mbeanType, eo, name, null);
    if (tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME + ".<init>");
  }

  /**
   * Constructor: Create an instance which is to be rendered as an MBean which
   * is further extended with some properties.
   * @param mbeanType
   * @param name
   * @param props
   */
  public JsObject(String mbeanType, String name, java.util.Properties props) {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        CLASS_NAME + ".<init>",
        new Object[] { mbeanType, name, props });
    _mbeanType = mbeanType;
    _name = name;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME + ".<init>");
  }

  /**
   * Constructor: Create an instance which is to be rendered as an MBean which
   * is further refined with some properties. The MBean configID is derived from
   * the supplied WCCM configuration ConfigObject.
   * @param mbeanType
   * @param eo
   * @param name
   * @param props
   */
  public JsObject(
    String mbeanType,
    StandardMBean eo,
    String name,
    java.util.Properties props) {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        CLASS_NAME + ".<init>",
        new Object[] { mbeanType, eo, name, props });
    _mbeanType = mbeanType;
    _name = name;
//    if (props != null)
//      activateMBean(mbeanType, eo, name, props);
    if (tc.isEntryEnabled())
      SibTr.exit(tc, CLASS_NAME + ".<init>");
  }




  /**
   * Activate the instance of this class as a JMX MBean
   * @param mbeanType
   * @param eo
   * @param name
   * @param props
   */
  public void activateMBean(
    final String mbeanType,
    Controllable eo,
    String name,
    final java.util.Properties props) 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "activateMBean",
        new Object[] { mbeanType, eo, name, props });
    
    	if (_activated == false) 
    	{
    		
    		try
    	    {
    			if(eo==null){
        			throw new Exception("Cannot Activate NULL MBean Implementation for type= "+_mbeanType+" with name = "+_name);
        		}
    		
    		// Do not let the type/name be changed once set
    		if (_mbeanType == null)
    			_mbeanType = mbeanType;
    		if (_name == null)
    			_name = name;
    		ObjectName objectName = new ObjectName("WebSphere:type="+_mbeanType+",name="+name);
    		if(iObjectName==null)
    		{
    			iObjectName=objectName;
    		}
    		if(mbeanType.equalsIgnoreCase(JsConstants.MBEAN_TYPE_QP))
    		{
    			mbs.registerMBean((JsQueue)eo, objectName);  
    		}
    		//      Object mbean = new Example();     TO BE DONE FOR QUEUE POINT, PUBLICATION POINT AND SUBSCRIPTION POINT         
    		_activated = true;
    		if (tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "Created/Activated MBean (name="
                + name
                + ",this="
                + this.toString());
        
    	    }
    		catch (Exception e) {
    			com.ibm.ws.ffdc.FFDCFilter.processException(
    					e,
    					CLASS_NAME + ".activateMBean",
    					"359",
    					this);
    			SibTr.warning(
    					tc,
    					"MBEAN_ACTIVATION_FAILED_SIAS0011",
    					new Object[] { _mbeanType, _name });
    		}
      
    }  
    else 
    {
      SibTr.error(
        tc,
        "INTERNAL_ERROR_SIAS0003",
        CLASS_NAME
          + ": attempted to activate a JMX MBean which was already activated");
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "activateMBean");
  }

  /**
   * Activate the instance of this class as a JMX MBean
   * @param mbeanType
   * @param name
   * @param props
   * @param configId
   */
  /*public void activateMBean(
    final String mbeanType,
    String name,
    final java.util.Properties props,
    final String configId) 
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "activateMBean",
        new Object[] { mbeanType, name, props, configId });

    if (_activated == false) 
    {

      // Do not let the type/name be changed once set
      if (_mbeanType == null)
        _mbeanType = mbeanType;
      if (_name == null)
        _name = name;



        try {
          if (props != null) {
            JsAdminServiceImpl as =
              (JsAdminServiceImpl)JsAdminService.getInstance();
            Set keys = props.keySet();
            Iterator i = keys.iterator();
            
          }
         
          _activated = true;
          if (tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "Created/Activated MBean (name="
                + name
                + ",this="
                + this.toString());
        }
        catch (Exception e) {
          com.ibm.ws.ffdc.FFDCFilter.processException(
            e,
            CLASS_NAME + ".activateMBean",
            "482",
            this);
          SibTr.warning(
            tc,
            "MBEAN_ACTIVATION_FAILED_SIAS0011",
            new Object[] { _mbeanType, _name });
        }
      }    
    else {
      SibTr.error(
        tc,
        "INTERNAL_ERROR_SIAS0003",
        CLASS_NAME
          + ": attempted to activate a JMX MBean which was already activated");
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "activateMBean");
  }*/

  /**
   * Deactivate the instance of this class as a JMX MBean
   */
  public void deactivateMBean() {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "deactivateMBean");

    if (_activated == true) {
     try{
    	 mbs.unregisterMBean(getObjectName());
        _activated = false;
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Deactivated MBean (this=" + this.toString());
      }
      catch (Exception e) {
        com.ibm.ws.ffdc.FFDCFilter.processException(
          e,
          CLASS_NAME + ".deactivateMBean",
          "537",
          this);
        SibTr.warning(
          tc,
          "MBEAN_DEACTIVATION_FAILED_SIAS0012",
          new Object[] { _mbeanType, _name });
      }
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "deactivateMBean");
  }

  /**
   * @return
   */
  public ObjectName getObjectName() {
    return iObjectName;
  }

  /**
   * @return
   */
  public String getMBeanType() {
    return _mbeanType;
  }

  /**
   * @return
   */
  public String getName() {
    return _name;
  }

  /**
   * @param s
   */
  public void setName(String s) {
    _name = s;
  }
  
  /**
   * Method getSequenceNumber.
   * Internal utility method for calculating a sequence number for notifications.
   * @return long
   */
  private synchronized long getSequenceNumber() 
  {
    return sequenceNumber++;
  }
  
  /**
   * Sends an event to the interface implementor.
   *  
   * @param me The MessagingEngine object associated with this Notification.
   * @param type The type of Notification to be propagated.
   * @param message The message to be propagated in the Notification.
   * @param properties The Properties associated with this Notification
   * type.
   */
  public void runtimeEventOccurred(JsMessagingEngine me,
                                   String type,
                                   String message,
                                   Properties properties)
  {
//    if (tc.isEntryEnabled())
//      SibTr.entry(
//        tc,
//        "runtimeEventOccurred",
//        new Object[] { me, type, message, properties });
    
    // Issue the Notification through the Runtime Collaborator
   /* if(iCollab != null)
    {
      // Instantiate a new Notification object
      Notification notification = 
        new Notification(type, // The type that we were passed
                         iCollab.getObjectName(), // The name of the emitting MBean
                         getSequenceNumber(), // generated seq num
                         message); // The message that we were passed

      // Next add the standard properties to those the caller
      // has passed. Pull these from the passed JsMessagingEngine
      // reference.
      
      String busName = me.getBusName();
      String meName = me.getName();
      String meUuid = me.getUuid().toString();
      String busUuid = "";
      if(me instanceof JsMessagingEngineImpl)
      {
        busUuid = ((JsMessagingEngineImpl)me)._bus.getUuid().toString();
      }
      
      // Include a guard for null properties, assume that the 
      // caller has no specific properties for the Notification if the
      // properties object is null
      if(properties == null)
        properties = new Properties();
      
      properties.put(SibNotificationConstants.KEY_THIS_BUS_NAME,
                     busName); 
      properties.put(SibNotificationConstants.KEY_THIS_BUS_UUID,
                     busUuid); 
      properties.put(SibNotificationConstants.KEY_THIS_MESSAGING_ENGINE_NAME,
                     meName);
      properties.put(SibNotificationConstants.KEY_THIS_MESSAGING_ENGINE_UUID,
                     meUuid); 
      
      // Now set the properties into the Notification
      notification.setUserData(properties);

      // Deliver the Notification
      try
      {
        iCollab.sendNotification(notification);
      } 
      catch (RuntimeOperationsException e)
      {
        com.ibm.ws.ffdc.FFDCFilter.processException(
            e,
            CLASS_NAME + ".runtimeEventOccurred",
            "1",
            this);
          SibTr.warning(
            tc,
            "MBEAN_SEND_NOTIFICATION_FAILED_SIAS0043",
            new Object[] { _mbeanType, _name });
      } 
      catch (MBeanException e)
      {
        com.ibm.ws.ffdc.FFDCFilter.processException(
            e,
            CLASS_NAME + ".runtimeEventOccurred",
            "2",
            this);
          SibTr.warning(
            tc,
            "MBEAN_SEND_NOTIFICATION_FAILED_SIAS0043",
            new Object[] { _mbeanType, _name });
      }
    }
    else
    {
      if (tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "RuntimeCollaborator is null, cannot send Notification");
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "runtimeEventOccurred");    */
  }
  
  public String toString(){
  	StringBuffer buf = new StringBuffer();
	String newline = System.lineSeparator();
 
  	buf.append(this.getClass().getName());
  	buf.append("@");
  	buf.append(Integer.toHexString(System.identityHashCode(this)));
  	buf.append(newline);

	buf.append("Name=");
	buf.append(_name);
	buf.append(newline);  	  	
  	
  	buf.append("MBean type=");
  	buf.append(_mbeanType);
  	buf.append(newline);
  	
  	buf.append("MBean activated=");
  	buf.append(_activated);
  	buf.append(newline);
  	
	buf.append("ObjectName=");
	buf.append(iObjectName);
	buf.append(newline);
  	
  	return buf.toString();
  }
  
}
