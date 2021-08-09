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

package com.ibm.wsspi.sib.core.selector;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * <p>
 * A Type safe enum of types of SICoreConnectionFactory that a user of the Core
 * SPI can use.
 * </p>
 * 
 * <p>
 * SIB build component: sib.core.selector
 * </p>
 * 
 * <p>
 * This class is a type safe enum with values representing each of the various
 * implementations of the Core SPI SICoreConnectionFactory that a user of the
 * Core SPI is allowed to obtain.
 * </p>
 * 
 * <p>
 * There is currently only a single type of SICoreConnectionFactory that Core
 * SPI users can use. That is the TrmSICoreConnectionFactory. This
 * implementation takes care of choosing whether a local SICoreConnectionFactory
 * or a Remote one should be created and which Messaging Engine to connect to.
 * </p>
 * 
 * @author nottinga
 * @version 1.11
 * @since 1.0
 */
public class FactoryType
{
  /** The name of the messages file. */
  private static final String TRACE_MESSAGES = "com.ibm.wsspi.sib.core.selector.Messages";

  /** The trace component */
  private static final TraceComponent _tc = SibTr.register(
                                                         FactoryType.class,
                                                         TraceGroups.TRGRP_CORE,
                                                         TRACE_MESSAGES);

  /** The trace nls component */
  private static final TraceNLS _nls = TraceNLS.getTraceNLS(TRACE_MESSAGES);

  /**
   * <p>
   * This constructor is protected to ensure that it can only be called by
   * subclasses.
   * </p>
   * 
   * <p>
   * Their are a limited number of allowed subclasses of this class, so the
   * constructor performs a check to ensure that the caller is a valid subclass.
   * </p>
   *  
   */
  protected FactoryType()
  {
    if (_tc.isEntryEnabled())
    {
      SibTr.entry(_tc, "FactoryType");
    }
    
    // Perform the check to ensure that the class is of the correct type.
    checkType(this.getClass().getName());

    if (_tc.isEntryEnabled())
    {
      SibTr.exit(_tc, "FactoryType", this);
    }
  }

  /**
   * <p>
   * This method checks to ensure that the class being instanciated is one of
   * the valid FactoryTypes or subclasses.
   * <p>
   * 
   * <p>
   * There is currently a single valid subclass of this class:
   * <ul>
   * <li>com.ibm.wsspi.sib.core.selector.PrivateFactoryType</li>
   * </ul>
   * It should be noted that the use of that subclass is with prior approval
   * from the relevant Jetstream representatives.
   * </p>
   * 
   * @param className
   */
  private void checkType(String className)
  {
    if (_tc.isEntryEnabled())
    {
      SibTr.entry(_tc, "checkType", new Object[]{this, className});
    }
    
    // Of the classname supplied is not one of
    // com.ibm.wsspi.sib.core.selector.FactoryType or
    // com.ibm.wsspi.sib.core.selector.PrivateFactoryType then an exception is
    // thrown.
    if (!(className.equals("com.ibm.wsspi.sib.core.selector.FactoryType") || className
        .equals("com.ibm.ws.sib.core.selector.PrivateFactoryType")))
    {
      // It would be preferable to throw an Exception version of this, however
      // the exception version
      // is a checked exception which can only be thrown when using reflection
      // to invoke classes.
      // As a result the Error version is thrown.
      IllegalAccessError e = new IllegalAccessError(_nls.getFormattedMessage(
          "INVALID_SUBCLASS_CWSJC0006E", new Object[]{className}, null));

      if (_tc.isEventEnabled())
      {
        SibTr.event(_tc, "throwing", e);
      }

      if (_tc.isEntryEnabled())
      {
        SibTr.exit(_tc, "checkType", e);
      }
      
      throw e;
    }

    if (_tc.isEntryEnabled())
    {
      SibTr.exit(_tc, "checkType");
    }
  }

  /**
   * <p>
   * When this value is passed to the SICoreConnectionFactorySelector the
   * returned SICoreConnectionFactory will obtain connections using TRM.
   * </p>
   */
  public static final FactoryType TRM_CONNECTION = new FactoryType();
  /**
   * <p>
   * When this value is passed to the SICoreConnectionFactorySelector the
   * returned SICoreConnectionFactory will obtain connections managed by the JCA
   * Connection Manager.
   * </p>
   */
  public static final FactoryType RA_CONNECTION  = new FactoryType();
}
