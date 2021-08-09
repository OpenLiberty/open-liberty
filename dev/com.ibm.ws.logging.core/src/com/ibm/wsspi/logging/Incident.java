/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.logging;

import java.util.Date;

public interface Incident {
    public String getSourceId();
    public String getProbeId();
    public String getExceptionName();
    int getCount();
    long getTimeStamp();
    Date getDateOfFirstOccurrence();
    String getLabel();
    public long getThreadId();
    public String getIntrospectedCallerDump();
}
