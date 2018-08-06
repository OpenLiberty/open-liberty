/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.wsspi.anno.info;

import java.util.logging.Logger;

import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.util.Util_Factory;

public interface InfoStoreFactory {
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
