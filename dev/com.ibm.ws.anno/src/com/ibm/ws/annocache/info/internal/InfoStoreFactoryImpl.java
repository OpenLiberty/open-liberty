/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.info.internal;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Service;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.info.InfoStoreException;
import com.ibm.wsspi.annocache.info.InfoStoreFactory;

public class InfoStoreFactoryImpl implements InfoStoreFactory {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");

    private static final String CLASS_NAME = "InfoStoreFactoryImpl";

    //

    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public InfoStoreFactoryImpl(
        AnnotationCacheServiceImpl_Service annoService,
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

    protected final AnnotationCacheServiceImpl_Service annoService;

    public AnnotationCacheServiceImpl_Service getAnnotationService() {
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

    //

    @Override
    public InfoStoreImpl createInfoStore(com.ibm.wsspi.anno.classsource.ClassSource_Aggregate classSource)
        throws com.ibm.wsspi.anno.info.InfoStoreException {

        if ( classSource instanceof ClassSource_Aggregate ) {
            return createInfoStore( (ClassSource_Aggregate) classSource);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public com.ibm.wsspi.anno.info.InfoStoreException newInfoStoreException(TraceComponent tc, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public com.ibm.wsspi.anno.info.InfoStoreException wrapIntoInfoStoreException(TraceComponent tc,
        String callingClassName, String callingMethodName, String message, Throwable th) {
        throw new UnsupportedOperationException();
    }
}
