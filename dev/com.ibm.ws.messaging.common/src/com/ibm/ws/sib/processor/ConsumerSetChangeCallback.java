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
package com.ibm.ws.sib.processor;

/**
 ConsumerSetChangeCallback is an interface that can be implemented by a component
 that supports the registration of producers, to allow it to be alerted to changes
 in the set of potential consumers. The consumerSetChange method is called when 
 the potential set of consumers drops to zero or rises above zero.
*/

public interface ConsumerSetChangeCallback 
{
  public void consumerSetChange(boolean isEmpty);
}
