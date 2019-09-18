/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.info;

import java.util.logging.Logger;

import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.util.Util_Factory;

public interface InfoStoreFactory extends com.ibm.wsspi.anno.info.InfoStoreFactory {
    String getHashText();

    //

    Util_Factory getUtilFactory();

    //

    InfoStoreException newInfoStoreException(Logger logger, String message);

    InfoStoreException wrapIntoInfoStoreException(Logger logger,
                                                  String callingClassName,
                                                  String callingMethodName,
                                                  String message, Throwable th);

    //

    InfoStore createInfoStore(ClassSource_Aggregate classSource) throws InfoStoreException;
}
