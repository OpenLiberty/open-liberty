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
package com.ibm.ws.anno.info.internal;

import java.text.MessageFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.anno.service.internal.AnnotationServiceImpl_Logging;
import com.ibm.ws.anno.util.internal.UtilImpl_Factory;
import com.ibm.wsspi.anno.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.anno.info.InfoStore;
import com.ibm.wsspi.anno.info.InfoStoreException;
import com.ibm.wsspi.anno.info.InfoStoreFactory;

public class InfoStoreFactoryImpl implements InfoStoreFactory {

    public static final TraceComponent tc = Tr.register(InfoStoreFactoryImpl.class);
    public static final String CLASS_NAME = InfoStoreFactoryImpl.class.getName();

    //

    protected String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public InfoStoreFactoryImpl(UtilImpl_Factory utilFactory) {
        super();

        this.hashText = AnnotationServiceImpl_Logging.getBaseHash(this);

        this.utilFactory = utilFactory;

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, MessageFormat.format("[ {0} ] Created", this.hashText));
            Tr.debug(tc, MessageFormat.format("[ {0} ] Util Factory [ {1} ]",
                                              this.hashText, this.utilFactory.getHashText()));
        }
    }

    //

    protected UtilImpl_Factory utilFactory;

    @Override
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    //

    @Override
    public InfoStoreException newInfoStoreException(TraceComponent logger, String message) {
        InfoStoreException exception = new InfoStoreException(message);

        if (logger.isDebugEnabled()) {
            Tr.debug(logger, exception.getMessage(), exception);
        }

        return exception;
    }

    @Override
    public InfoStoreException wrapIntoInfoStoreException(TraceComponent logger, String callingClassName, String callingMethodName, String message, Throwable th) {
        return InfoStoreException.wrap(logger, callingClassName, callingMethodName, message, th);
    }

    //

    @Override
    public InfoStore createInfoStore(ClassSource_Aggregate classSource) throws InfoStoreException {
        return new InfoStoreImpl(this, classSource); // throws InfoStoreException
    }

}
