/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.tx;

import com.ibm.ws.Transaction.UOWCallback;

/**
 * A service providing this interface will have the UOWEvent method called at various points during the lifecycle of a unit of work.
 */
public interface UOWEventListener
{
    public static final int POST_BEGIN = UOWCallback.POST_BEGIN;
    public static final int POST_END = UOWCallback.POST_END;
    public static final int SUSPEND = 100;
    public static final int RESUME = 110;
    public static final int REGISTER_SYNC = 120;

    /**
     * @param uow The Unit of work to which this event applies
     * @param event The event code
     * @param data Data associated with the event
     */
    public void UOWEvent(UOWEventEmitter uow, int event, Object data);
}
