/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * 
 * 
 *
 * Change activity:
 *
 * Reason          Date   Origin   Description
 * --------------- ------ -------- --------------------------------------------
 * 253203          030205 tevans   New Statestream
 * ============================================================================
 */

package com.ibm.ws.sib.processor.gd.statestream;

import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;

public class GDMessageData implements TickData
{
  public SIMPMessage msg;
  public long itemStreamIndex;
  public boolean reallocateOnCommit = false;
}
