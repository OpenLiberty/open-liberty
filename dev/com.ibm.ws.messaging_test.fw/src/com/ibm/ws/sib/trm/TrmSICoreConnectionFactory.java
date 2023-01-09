/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
 
package com.ibm.ws.sib.trm;

import com.ibm.ws.sib.api.jmsra.stubs.TrmSICoreConnectionFactoryStub;
import com.ibm.wsspi.sib.core.SICoreConnectionFactory;

/**
 * Version of TrmSICoreConnectionFactory that hooks into core API stubs.
 */
public abstract class TrmSICoreConnectionFactory implements SICoreConnectionFactory
{

  public static TrmSICoreConnectionFactory getInstance()
  {
    return new TrmSICoreConnectionFactoryStub();
  }

}
