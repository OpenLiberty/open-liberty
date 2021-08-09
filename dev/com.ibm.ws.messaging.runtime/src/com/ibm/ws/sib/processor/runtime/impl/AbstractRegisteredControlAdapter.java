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
package com.ibm.ws.sib.processor.runtime.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.ControllableRegistrationService;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.admin.SIBExceptionInvalidValue;
import com.ibm.ws.sib.admin.exception.AlreadyRegisteredException;
import com.ibm.ws.sib.admin.exception.NotRegisteredException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A control adapter which is registered with an mbean, as opposed to one 
 * which is never registered, only obtained through a query.
 * 
 */
public abstract class AbstractRegisteredControlAdapter extends AbstractControlAdapter implements Controllable
{

  private static TraceComponent tc =
    SibTr.register(
      AbstractRegisteredControlAdapter.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private RuntimeEventListener _runtimeEventListener = null;

  private boolean _isRegistered = false;

  private MessageProcessor _messageProcessor = null;
  
  private ControllableType _type = null ;

  public AbstractRegisteredControlAdapter(
    MessageProcessor messageProcessor,
    ControllableType type )
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "AbstractRegisteredControlAdapter", 
        new Object[] { messageProcessor , type } );
    
    _messageProcessor = messageProcessor;
    _type = type ;
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "AbstractRegisteredControlAdapter", this);
  }

  protected RuntimeEventListener getRuntimeEventListener()
  {
    return _runtimeEventListener;
  }

  protected MessageProcessor getMessageProcessor()
  {
    return _messageProcessor;
  }

  protected boolean isRegistered()
  {
    return _isRegistered;
  }

  protected void setType(ControllableType type)
  {
    _type = type;
  }

  public synchronized void registerControlAdapterAsMBean()
  {
    
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "registerControlAdapterAsMBean", this );

        
    if( ! _isRegistered ) 
    {
        
      ControllableRegistrationService regService =
        (ControllableRegistrationService) _messageProcessor.getMEInstance(
          SIMPConstants.JS_MBEAN_FACTORY);
      if (regService == null)
      {

        SibTr.error(tc, 
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            AbstractRegisteredControlAdapter.class ,
            "1:128:1.9"
          }
        );
      }
      else
      {
        try
        {
          _runtimeEventListener = regService.register(this, _type);
          _isRegistered = true;
        }
        catch (AlreadyRegisteredException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.registerControlAdapterAsMBean",
            "1:145:1.9", 
            this);
            
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.registerControlAdapterAsMBean", 
                           "1:150:1.9", 
                           SIMPUtils.getStackTrace(e) });
          SibTr.exception(tc, e);
        }
        catch (SIBExceptionInvalidValue e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.registerControlAdapterAsMBean",
            "1:160:1.9", 
            this);
            
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.registerControlAdapterAsMBean", 
                           "1:165:1.9", 
                           SIMPUtils.getStackTrace(e) });
          SibTr.exception(tc, e);
        }
        catch (Exception e)
        {
          // FFDC 
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.registerControlAdapterAsMBean",
            "1:175:1.9", 
            this);
            
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.registerControlAdapterAsMBean", 
                           "1:180:1.9", 
                           SIMPUtils.getStackTrace(e) });
          SibTr.exception(tc, e);
  
        }
      }
    }
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "registerControlAdapterAsMBean");
  }

  public synchronized void deregisterControlAdapterMBean()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "deregisterControlAdapterMBean", this );
    
    // Don't de-register if we don't think we're currently registered.
    if (_isRegistered)
    {
      ControllableRegistrationService regService =
        (ControllableRegistrationService) _messageProcessor.getMEInstance(
          SIMPConstants.JS_MBEAN_FACTORY);
      if (regService == null)
      {
        SibTr.error(tc, 
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            AbstractRegisteredControlAdapter.class ,
            "1:208:1.9"
          }
        );
            
          
      }
      else
      {
        try
        {
          regService.deregister(this, _type);
          _isRegistered = false;
          _runtimeEventListener = null;
        }
        catch (NotRegisteredException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.deregisterControlAdapterMBean",
            "1:228:1.9", 
            this);
            
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.deregisterControlAdapterMBean", 
                           "1:233:1.9", 
                           SIMPUtils.getStackTrace(e) });
          SibTr.exception(tc, e);
        }
        catch (SIBExceptionInvalidValue e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.deregisterControlAdapterMBean",
            "1:243:1.9", 
            this);
            
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.deregisterControlAdapterMBean", 
                           "1:248:1.9", 
                           SIMPUtils.getStackTrace(e) });
          SibTr.exception(tc, e);
        }
        catch (Exception e)
        {
          //        FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.deregisterControlAdapterMBean",
            "1:258:1.9", 
            this);
            
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.runtime.AbstractRegisteredControlAdapter.deregisterControlAdapterMBean", 
                           "1:263:1.9", 
                           SIMPUtils.getStackTrace(e) });
          SibTr.exception(tc, e);
        }
      }
    }
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "deregisterControlAdapterMBean");
  }
  
  public void dereferenceControllable()
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "dereferenceControllable");
      
    if( _isRegistered ) {
      deregisterControlAdapterMBean( );
    }
      
    _messageProcessor = null ;
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "dereferenceControllable");
  }
  
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public synchronized void runtimeEventOccurred(RuntimeEvent event)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "runtimeEventOccurred", 
                  new Object[] { event , this } );
    
    if( _isRegistered ) 
    {
      // Check that we have a RuntimeEventListener      
      if( _runtimeEventListener != null ) 
      {
        // Fire a Notification if Eventing is enabled
        if(_messageProcessor.
             getMessagingEngine().
             isEventNotificationEnabled())
        {
          // Fire the event---- This is not needed as of now in liberty release
//          _runtimeEventListener.
//            runtimeEventOccurred(_messageProcessor.
//                                   getMessagingEngine(),
//                                 event.getType(),
//                                 event.getMessage(),
//                                 (Properties)event.getUserData());    
        }
        else
        {
          if (tc.isDebugEnabled())
            SibTr.debug(tc, "Eventing is disabled, cannot fire event");   
        }        
      }
      else
      {
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Null RuntimeEventListener, cannot fire event");   
      }
    }
    else
    {
      if (tc.isDebugEnabled())
        SibTr.debug(tc, "Not registered, cannot fire event");   
    }
    
    if (tc.isEntryEnabled()) 
      SibTr.exit(tc, "runtimeEventOccurred");
  }
  

}
