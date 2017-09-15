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
package com.ibm.ws.sib.processor.impl.store;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 *
 */
public class AIExecuteUpdate implements Runnable
{
  // Standard debug/trace
  private static final TraceComponent tc =
    SibTr.register(
      AIExecuteUpdate.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
 

  private AsyncUpdate _unit;
  private MessageProcessor _messageProcessor;

  public AIExecuteUpdate(AsyncUpdate unit, MessageProcessor messageProcessor)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "AIExecuteUpdate",
        new Object[] { unit, messageProcessor });

    _unit = unit;
    _messageProcessor = messageProcessor;

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "AIExecuteUpdate", this);
  }

  public void run()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "run");

    LocalTransaction tran = null;
    Throwable ex = null;

    try
    {
      tran = _messageProcessor.getTXManager().createLocalTransaction(false);
      _unit.execute(tran);
    }
    catch (Throwable e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
        "1:90:1.13",
        this);
      SibTr.exception(tc, e);
      ex = e;
    }

    if (ex == null)
    {
      // commit
      try
      {
        tran.commit();
      }
      catch (SIRollbackException x)
      {
        // apparently the transaction has already committed. This is a serious bug!!
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:109:1.13",
          this);
        SibTr.exception(tc, x);
        ex = x;
      }
      catch (SIConnectionLostException x)
      {
        // apparently there is a problem in the MessageStore. Probably serious
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:120:1.13",
          this);
        SibTr.exception(tc, x);
        ex = x;
      }
      catch (SIIncorrectCallException x)
      {
        // may be a serious problem
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:131:1.13",
          this);
        SibTr.exception(tc, x);
        ex = x;
      }
      catch (SIResourceException x)
      {
        // may be a serious problem
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:142:1.13",
          this);
        SibTr.exception(tc, x);
        ex = x;
      }
      catch (SIErrorException x)
      {
        // may be a serious problem
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:153:1.13",
          this);
        SibTr.exception(tc, x);
        ex = x;
      }
      catch (Throwable x)
      {
        // this is probably a serious problem
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:164:1.13",
          this);
        SibTr.exception(tc, x);
        ex = x;
      }
    } // end if (ex == null)
    else
    { // one of the execute() methods threw an exception
      // so rollback this transaction
      try
      {
        tran.rollback();
      }
      catch (SIConnectionLostException x)
      {
        // apparently the transaction has already committed. This is a serious bug!!
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:183:1.13",
          this);
        SibTr.exception(tc, x);
      }
      catch (SIIncorrectCallException x)
      {
        // apparently there is a problem in the MessageStore. Probably serious
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:193:1.13",
          this);
        SibTr.exception(tc, x);
      }
      catch (SIResourceException x)
      {
        // may be a serious problem
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:203:1.13",
          this);
        SibTr.exception(tc, x);
      }
      catch (SIErrorException x)
      {
        // may be a serious problem
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:213:1.13",
          this);
        SibTr.exception(tc, x);
      }
      catch (Throwable x)
      {
        // this is probably a serious problem
        FFDCFilter.processException(
          x,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:223:1.13",
          this);
        SibTr.exception(tc, x);
      }

    }

    // notify
    if (ex == null)
      try
      {
        _unit.committed();
      }
      catch (SIException e1)
      {
        // FFDC
        FFDCFilter.processException(
          e1,
          "com.ibm.ws.sib.processor.impl.store.AIExecuteUpdate.run",
          "1:242:1.13",
          this);
          
        if (tc.isEntryEnabled()) SibTr.exit(tc, "run", "SIErrorException");
        
        throw new SIErrorException(e1);
      }
    else
      _unit.rolledback(ex);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "run");
  } // end public void run()
}
