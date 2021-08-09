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
package com.ibm.ws.sib.msgstore.test.persistence;
/*
 * Change activity:
 *
 *  Reason         Date     Origin   Description
 * --------------- -------- -------- ------------------------------------------
 * SIB0112i.ms.1   22/02/07 gareth   Changes to handling of STORE_MAYBE Items
 * SIB0112d.ms.2   04/07/07 gareth   MemMgmt: SpillDispatcher improvements - datastore
 * ============================================================================
 */

import com.ibm.ws.sib.msgstore.*;

public class StoreMaybeItemStream extends ItemStream
{
    public StoreMaybeItemStream()
    {
        super();
    }

    public int getStorageStrategy()
    {
        return STORE_MAYBE;
    }
}

