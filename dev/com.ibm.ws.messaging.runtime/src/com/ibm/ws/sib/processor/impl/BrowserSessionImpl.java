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

package com.ibm.ws.sib.processor.impl;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPNoLocalisationsException;
import com.ibm.ws.sib.processor.impl.corespitrace.CoreSPIBrowserSession;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MPDestinationSession;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.BrowserSession;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.SICoreConnection;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SISessionUnavailableException;

/**
 * Implements BrowserSession as defined by the Core API.
 * This is done via a BrowseCursor on the Local QueueingPoint.
 * This only makes sense for Pt-Pt but if the destination returns
 * a local CD for pub-sub, it can be browsed anyway.
 * 
 * @author tevans
 * @see com.ibm.ws.sib.processor.BrowserSession
 */
public final class BrowserSessionImpl implements BrowserSession, MPDestinationSession
{
  
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  private static final TraceNLS nls_cwsik =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      BrowserSessionImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  private SelectionCriteria _selectionCriteria;
  private ConnectionImpl _conn;
  private boolean _closed;
  private DestinationHandler _dest;
  private BrowseCursor _browseCursor;
  private ConsumerManager _consumerManager;
  /** The destination address this browser is connected to */
  private SIDestinationAddress _destinationAddress;

  private SIBUuid12 uuid;

  /* Output source info */
  /**
   * Constructs a new BrowserSessionImpl. If the given destination is
   * not null, obtain its local CD (if any).
   * 
   * @param dest The destination to be browsed
   * @param filter A filter to limit the items returned by the BrowseCursor
   * @param conn The parent connection
   * @throws SIDiscriminatorSyntaxException 
   * @throws SISelectorSyntaxException 
   */
  public BrowserSessionImpl(
    DestinationHandler dest,
    SelectionCriteria selectionCriteria,
    ConnectionImpl conn,
    SIDestinationAddress address,
    boolean gatherMessages)
    throws SINotPossibleInCurrentConfigurationException, 
           SIResourceException, 
           SISelectorSyntaxException, 
           SIDiscriminatorSyntaxException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "BrowserSessionImpl",
        new Object[] { dest, selectionCriteria, conn, address, Boolean.valueOf(gatherMessages) });

    _conn = conn;
    _dest = dest;
    _selectionCriteria = selectionCriteria;
    _destinationAddress = address;
    this.uuid = new SIBUuid12();

    if (dest != null)
    {
      if (!dest.isPubSub())
      {
        // See if the caller has fixed this browser to a particular ME (either explicity
        // via an ME in the address or implicitly to the local one using isLocalOnly())
        // (Applicable to PtoP only)

        JsDestinationAddress jsDestAddr = (JsDestinationAddress)_destinationAddress;
        // If we're fixed to the local ME, check that this ME has a queue point defined.
        // And if it does, fix us onto it. If we don't have one, we ignore the isLocalOnly flag
        if((jsDestAddr.getME() == null) && jsDestAddr.isLocalOnly())
        {
          if(dest.getLocalPtoPConsumerManager() != null)
            jsDestAddr.setME(dest.getMessageProcessor().getMessagingEngineUuid());
        }
        
        // Check that if there is a local queue point, then the creation of it is 
        // complete.  The local consumerdispatcher is created locked and unlocked
        // when the transaction under which the local queue point is created gets
        // committed
        _consumerManager = dest.chooseConsumerManager(gatherMessages?dest.getUuid():null,
                                                      jsDestAddr.getME(),
                                                      null);
  
        if (_consumerManager == null)
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "ConsumerDispatcher not found (" + jsDestAddr.getME() + ")");
          
          //We can't find a suitable localisation.
          //Although a queue must have at least one localisation this is
          //possible if the sender restricted the potential localisations
          //using a fixed ME or a scoping alias (to an out-of-date set of localisation)
          //We throw an exception to the application.
          SIMPNoLocalisationsException e = new SIMPNoLocalisationsException(
                                                          nls_cwsik.getFormattedMessage(
                                                                       "DELIVERY_ERROR_SIRC_26",
                                                                       new Object[] {jsDestAddr.getDestinationName()},
                                                                       null));

          e.setExceptionReason(SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR);
          e.setExceptionInserts(new String[] {jsDestAddr.getDestinationName()});

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "BrowserSessionImpl", e);
          
          throw e;
        }
        else 
        {
          if (_consumerManager.isLocked())
          {
            SINotPossibleInCurrentConfigurationException e = new SINotPossibleInCurrentConfigurationException(
              nls.getFormattedMessage(
                "DESTINATION_IS_LOCKED_ERROR_CWSIP0085",
                new Object[] {dest.getName(),
                              dest.getMessageProcessor().getMessagingEngineName()},
                null));
            
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "BrowserSessionImpl", e);
            throw e;
          }
          
          //Attach this session to the ConsumerManager
          _consumerManager.attachBrowser(this);
          
          //Get the Cursor from the ConsumerManager.
          _browseCursor = _consumerManager.getBrowseCursor(selectionCriteria);        
        }
      }
    }
    //if the destination was null - it was pub sub - this will create a browser
    //session which never returns any messages
    else
    {
      _browseCursor = null;
    }

    //Set the closed flag to false, making the BrowseSession available for use.
    _closed = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "BrowserSessionImpl", this);
  }

  /**
   * Gets the next item from the BrowseCursor.
   * 
   * @see com.ibm.wsspi.sib.core.BrowserSession#next()
   */
  public SIBusMessage next()
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException, 
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled())
      SibTr.entry(CoreSPIBrowserSession.tc, "next", this);

    JsMessage msg = null;
    
    synchronized(this)
    {
      //Check that the session is not closed, if it is throw an exception
      checkNotClosed();
   
      //if the browse cursor is null - it was pub sub - this
      //session will never return any messages
      if (_browseCursor != null)
      {
        try
        {
          //Get the next item from the browseCursor and cast it
          //to a SIMPMessage
          msg = _browseCursor.next();
        }
        catch (SISessionDroppedException e)
        {
          // MessageStoreException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.BrowserSessionImpl.next",
            "1:265:1.78.1.7",
            this);

          close();
          
          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.BrowserSessionImpl",
              "1:274:1.78.1.7",
              e });

          if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIBrowserSession.tc, "next", e);
          throw e;
        }
        catch (SIResourceException e)
        {
          // MessageStoreException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.BrowserSessionImpl.next",
            "1:287:1.78.1.7",
            this);
            
          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.BrowserSessionImpl",
              "1:294:1.78.1.7",
              e });

          if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled())
            SibTr.exit(CoreSPIBrowserSession.tc, "next", e);
          throw e;
        }
      }//end if
    }//end sync
    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(CoreSPIBrowserSession.tc, "next", msg);
    return msg;
  }

  /**
   * Close this BrowserSession...
   * Dereference from the parent connection and set the closed flag to true.
   * 
   * @see com.ibm.ws.sib.processor.BrowserSession#close()
   */
  public void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled())
      SibTr.entry(CoreSPIBrowserSession.tc, "close", this);

    synchronized (this)
    {
      if (!_closed)
        //if we're closed already, don't bother doing anything.
      {
        //dereference from the parent connection
        _conn.removeBrowserSession(this);

        _close();
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled())
      SibTr.exit(CoreSPIBrowserSession.tc, "close");
  }

  void _close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "_close");

    synchronized (this)
    {
      if (!_closed)
        //if we're closed already, don't bother doing anything.
      {
        if (_consumerManager != null)
        {
          //Remove the browser from the consumer dispatcher        
          _consumerManager.detachBrowser(this);
        }

        if (_browseCursor != null)
        {
          try
          {
            _browseCursor.finished();
          }
          catch (SISessionDroppedException e)
          {
            FFDCFilter.processException(
                e,
                "com.ibm.ws.sib.processor.impl.BrowserSessionImpl._close",
                "1:363:1.78.1.7",
                this);
                
            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.BrowserSessionImpl._close",
                "1:370:1.78.1.7",
                SIMPUtils.getStackTrace(e) });
          }
        }
        
        _browseCursor = null;
        _conn = null;
        _closed = true;
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "_close");
  }

  /**
   * Method _closeBrowserDestinationDeleted.
   * <p>Close the browser without dereferencing it from the Consumer Dispatcher,
   * if the destinationHandler passed in matches the destination that the
   * browser was connected through</p>
   */
  boolean _closeBrowserDestinationDeleted(DestinationHandler destinationHandler)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "_closeBrowserDestinationDeleted", destinationHandler);

    if (destinationHandler == _dest)
    {
      synchronized (this)
      {
        if (!_closed)
          //if we're closed already, don't bother doing anything.
        {
          //dereference from the parent connection
          _conn.removeBrowserSession(this);
          
          if (_consumerManager != null)
          {
            // We still call detach because we might be a gathering browser
            // and therefore need to detach any other underlying browsers
            _consumerManager.detachBrowser(this);
          }
          
          if (_browseCursor != null)
          {
            try
            {
              _browseCursor.finished();
            }
            catch (SISessionDroppedException e)
            {
              FFDCFilter.processException(
                  e,
                  "com.ibm.ws.sib.processor.impl.BrowserSessionImpl._closeBrowserDestinationDeleted",
                  "1:423:1.78.1.7",
                  this);
                  
              SibTr.exception(tc, e);
              SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.BrowserSessionImpl._closeBrowserDestinationDeleted",
                  "1:430:1.78.1.7",
                  SIMPUtils.getStackTrace(e) });
            }
          }
          
          _browseCursor = null;
          _conn = null;
          _closed = true;
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "_closeBrowserDestinationDeleted", new Boolean(_closed));
    
    return _closed;
  }

  /**
   * Check if the session is closed. If the closed flag is set to
   * true, throw an exception.
   * 
   * @throws SISessionUnavailableException thrown if the session is closed
   */
  void checkNotClosed() throws SISessionUnavailableException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkNotClosed");

    synchronized (this)
    {
      if (_closed)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "checkNotClosed", "Object closed");

        throw new SISessionUnavailableException(
          nls.getFormattedMessage("OBJECT_CLOSED_ERROR_CWSIP0081", 
                                  new Object[] { _dest.getName(),
                                                 _dest.getMessageProcessor().getMessagingEngineName() }, 
                                  null));
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkNotClosed");
  }

  /**
   * Get the parent connection used to create this BrowserSession
   * @throws SISessionUnavailableException 
   * 
   * @see com.ibm.ws.sib.processor.BrowserSession#getConnection()
   */
  public SICoreConnection getConnection() throws SISessionUnavailableException
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled())
    {
      SibTr.entry(CoreSPIBrowserSession.tc, "getConnection", this);
      SibTr.exit(CoreSPIBrowserSession.tc, "getConnection",_conn);
    }
    
    checkNotClosed();
    return _conn;
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.BrowserSession#reset()
   */
  public void reset()  
  throws SISessionUnavailableException, SISessionDroppedException,
         SIResourceException, SIConnectionLostException, 
         SIErrorException
         
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled()) 
      SibTr.entry(CoreSPIBrowserSession.tc, "reset", this);
      
    synchronized(this)
    {
      checkNotClosed();
      
      // Close out that last browser
      if (_browseCursor != null)
      {
        try
        {
          _browseCursor.finished();
        }
        catch(SISessionDroppedException e)
        {
//        MessageStoreException shouldn't occur so FFDC.
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.BrowserSessionImpl.reset",
            "1:525:1.78.1.7",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.BrowserSessionImpl",
              "1:532:1.78.1.7",
              e });
        }
      }

      //Get the local CD of the given destination (which should implement the
      //Browsable interface)
      try
      {
        _browseCursor = _consumerManager.getBrowseCursor(_selectionCriteria);
      }
      catch(Exception e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.BrowserSessionImpl.reset",
          "1:548:1.78.1.7",
          this);
        SibTr.exception(tc, e);
        SIResourceException e2 = new SIResourceException(e);
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "reset", e2);
        throw e2;
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled()) 
      SibTr.exit(CoreSPIBrowserSession.tc, "reset");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.core.DestinationSession#getDestinationAddress()
   */
  public SIDestinationAddress getDestinationAddress()
  {
    if (TraceComponent.isAnyTracingEnabled() && CoreSPIBrowserSession.tc.isEntryEnabled())
    {    
      SibTr.entry(CoreSPIBrowserSession.tc, "getDestinationAddress", this);    
      SibTr.exit(CoreSPIBrowserSession.tc, "getDestinationAddress", _destinationAddress);
    }
    return _destinationAddress;
  }
  
  /**
   * Returns the destination that the session was created against
   * @return
   */
  public DestinationHandler getNamedDestination()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getNamedDestination");
      SibTr.exit(tc, "getNamedDestination", _dest);
    }
    return _dest;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.MPDestinationSession#getUuid()
   */
  public SIBUuid12 getUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getUuid");
      SibTr.exit(tc, "getUuid", uuid);	
    }
    return uuid;
  }
}
