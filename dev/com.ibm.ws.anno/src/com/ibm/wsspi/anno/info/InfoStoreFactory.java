/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.info;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.util.Util_Factory;

public interface InfoStoreFactory {
    String getHashText();

    //

    Util_Factory getUtilFactory();

    //

    InfoStoreException newInfoStoreException(TraceComponent logger, String message);

    InfoStoreException wrapIntoInfoStoreException(TraceComponent logger,
                                                  String callingClassName,
                                                  String callingMethodName,
                                                  String message, Throwable th);

    //

    InfoStore createInfoStore(ClassSource_Aggregate classSource) throws InfoStoreException;
}
