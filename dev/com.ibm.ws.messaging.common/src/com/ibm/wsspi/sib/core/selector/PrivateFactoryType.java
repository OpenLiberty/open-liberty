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

import com.ibm.wsspi.sib.core.selector.FactoryType;

/**
 * <p>Adds extra SICoreConnectionFactoryTypes for Jetstream internal usage.</p>
 * 
 * <p>SIB build component: sib.core.selector</p>
 * 
 * <p>This FactoryType adds a constant that when used will create SICoreConnectionFactory objects
 *   that talk directly to the message engine. This only works if the messaging engine is local.
 * </p>
 * 
 * <p>When using this mechanism a Map containing the BUS_NAME and ME_NAME must be provided in the
 *   map provided to the getSICoreConnectionFactory(FactoryType, Map) method of the
 *   SICoreConnectionFactorySelector. If this is not provided
 *   an SIInsufficientDataForFactoryTypeException will be thrown. If the named Messaging Engine does
 *   not exist locally then an SIMENotFoundException will be thrown.
 * </p>
 * 
 * @author nottinga 
 * @version 1.8
 * @since 1.0
 */
public final class PrivateFactoryType extends FactoryType
{
  /**
   * <p>A constant for putting the bus name into the properties used for obtaining the 
   * SICoreConnectionFactory.</p>
   */
  public static final String ME_NAME = "ME_NAME";

  /**
   * <p>A constant for putting the messaging engine name into the properties used for obtaining the 
   * SICoreConnectionFactory.</p>
   */
  public static final String BUS_NAME = "BUS_NAME";

  /**
   * This constructor makes it impossible for any other class to instantiate instances of
   * the PrivateFactoryType.
   *
   */
  private PrivateFactoryType()
  {
    
  }
  
  /**
   * <p>This constant when provided to the getSICoreConnectionFactory(FactoryType, Map) method of
   *   the SICoreConnectionFactorySelector will return an SICoreConnectionFactory that will create
   *   connections directly to that local ME. 
   * </p>
   * <p>
   *   It should be noted that in order for this to work the map must contain the bus and 
   *   messaging engine names.
   * </p>
   */
  public static final PrivateFactoryType LOCAL_CONNECTION = new PrivateFactoryType();
}
