/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.TargetsTableTimeStamp;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Readable;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;

/**
 * <p>Stamp table.</p>
 */
public class TargetsTableTimeStampImpl
    implements TargetsTableTimeStamp, TargetCache_Readable {

    // Logging ...

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsTableTimeStampImpl.class.getSimpleName();

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    public TargetsTableTimeStampImpl() {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.name = null;
        this.stamp = null;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    public TargetsTableTimeStampImpl(String name) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.name = name;
        this.stamp = null;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ]",
                        new Object[] { this.hashText, this.name });
        }
    }

    public TargetsTableTimeStampImpl(String name, String stamp) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.name = name;
        this.stamp = stamp;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ] [ {2} ]",
                        new Object[] { this.hashText, this.name, this.stamp });
        }
    }

    //

    protected String name;

    @Override
    public String setName(String name) {
        String oldName = this.name;
        this.name = name;
        return oldName;
    }

    @Override
    @Trivial
    public String getName() {
        return name;
    }

    //

    protected String stamp;

    @Override
    public String setStamp(String stamp) {
        String oldStamp = this.stamp;
        this.stamp = stamp;
        return oldStamp;
    }

    @Override
    @Trivial
    public String getStamp() {
        return stamp;
    }

    //

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Stamp Data: BEGIN");
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  " + "Name: " + getName());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  " + "Stamp: " + getStamp());
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Stamp Data: END");
    }

    //

    @Override
    public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
        return reader.read(this);
    }
}
