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
package com.ibm.tx.util.logging;

public interface TraceComponent
{
    public boolean isEntryEnabled();

    public boolean isEventEnabled();

    public boolean isDebugEnabled();

    public void setWarningEnabled(boolean enabled);

    public void setEntryEnabled(boolean enabled);

    public void setEventEnabled(boolean enabled);

    public void setDebugEnabled(boolean enabled);

    public boolean isWarningEnabled();

    public Object getData();
}
