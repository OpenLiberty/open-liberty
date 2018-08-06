/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2012, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.info.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Service;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.InfoStoreFactory;

public class InfoStoreFactoryImpl implements InfoStoreFactory {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.anno.info");

    private static final String CLASS_NAME = "InfoStoreFactoryImpl";

    //

    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public InfoStoreFactoryImpl(
        AnnotationServiceImpl_Service annoService,
        UtilImpl_Factory utilFactory) {

        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.annoService = annoService;
        this.utilFactory = utilFactory;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Created",
                        this.hashText);
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] Util Factory [ {1} ]",
                        new Object[] { this.hashText, this.utilFactory.getHashText() });
        }
    }

    //

    protected final AnnotationServiceImpl_Service annoService;

    public AnnotationServiceImpl_Service getAnnotationService() {
        return annoService;
    }

    //

    protected final UtilImpl_Factory utilFactory;

    @Override
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    //

    @Override
    public InfoStoreException newInfoStoreException(Logger useLogger, String message) {
        String methodName = "newInfoStoreException";

        InfoStoreException exception = new InfoStoreException(message);

        if (useLogger.isLoggable(Level.FINER) ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Created [ {0} ]", exception.getMessage());
        }

        return exception;
    }

    @Override
    public InfoStoreException wrapIntoInfoStoreException(
        Logger useLogger,
        String callingClassName,
        String callingMethodName,
        String message,
        Throwable th) {

        return InfoStoreException.wrap(useLogger, callingClassName, callingMethodName, message, th);
    }

    //

    @Override
    public InfoStoreImpl createInfoStore(ClassSource_Aggregate classSource) throws InfoStoreException {
        return new InfoStoreImpl(this, classSource); // throws InfoStoreException
    }
}
