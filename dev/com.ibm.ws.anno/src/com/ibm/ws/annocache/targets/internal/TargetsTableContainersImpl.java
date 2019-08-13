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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.TargetsTableContainers;
import com.ibm.ws.annocache.targets.cache.TargetCache_ParseError;
import com.ibm.ws.annocache.targets.cache.TargetCache_Readable;
import com.ibm.ws.annocache.targets.cache.TargetCache_Reader;
import com.ibm.wsspi.annocache.classsource.ClassSource;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate.ScanPolicy;

/**
 * <p>Stamp table.</p>
 */
public class TargetsTableContainersImpl
   implements TargetsTableContainers, TargetCache_Readable {

    // Logging ...

    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = TargetsTableContainersImpl.class.getSimpleName();

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    //

    @Trivial
    public TargetsTableContainersImpl(AnnotationTargetsImpl_Factory factory) {
        String methodName = "<init>";

        this.factory = factory;

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.names = new ArrayList<String>();
        this.policies = new HashMap<String, ScanPolicy>();

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", this.hashText);
        }
    }

    //

    protected final AnnotationTargetsImpl_Factory factory;

    @Override
    @Trivial
    public AnnotationTargetsImpl_Factory getFactory() {
        return factory;
    }

    //

    protected final List<String> names;
    protected final Map<String, ScanPolicy> policies;

    @Override
    public void addName(String name, ScanPolicy policy) {
        names.add(name);
        policies.put(name, policy);
    }

    @Override
    public void addNameAfter(String name, ScanPolicy policy, String afterName) {
        int addOffset = names.indexOf(afterName);
        if ( addOffset == -1 ) {
            throw new IndexOutOfBoundsException("Name [ " + afterName + " ] is not within container table [ " + getHashText() + " ]");
        }

        names.add(addOffset + 1, name);
        policies.put(name, policy);
    }

    @Override
    public void addNameBefore(String name, ScanPolicy policy, String beforeName) {
        int addOffset = names.indexOf(beforeName);
        if ( addOffset == -1 ) {
            throw new IndexOutOfBoundsException("Name [ " + beforeName + " ] is not within container table [ " + getHashText() + " ]");
        }

        names.add(addOffset, name);
        policies.put(name, policy);
    }

    @Override
    public ScanPolicy removeName(String name) {
        names.remove(name);
        return policies.remove(name);
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public boolean containsName(String name) {
        return policies.containsKey(name);
    }

    @Override
    public ScanPolicy getPolicy(String name) {
        return policies.get(name);
    }

    public void addNames(ClassSource_Aggregate rootClassSource) {
        for ( ClassSource childClassSource : rootClassSource.getClassSources() ) {
            ScanPolicy scanPolicy = rootClassSource.getScanPolicy(childClassSource);
            if ( scanPolicy == ScanPolicy.EXTERNAL ) {
                continue;
            }

            addName( childClassSource.getCanonicalName(), scanPolicy );
        }
    }

    //

    @Override
    @Trivial
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Containers: BEGIN");
        for ( String name : getNames() ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  " + "Name: " + name);
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "  " + "Policy: " + getPolicy(name));
        }
        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Containers: END");
    }

    //

    public boolean sameAs(TargetsTableContainersImpl otherTable) {
        if ( otherTable == null ) {
            return false;
        } else if ( otherTable == this ) {
            return true;
        }

        List<String> theseNames = getNames();
        int thisCount = theseNames.size();

        List<String> otherNames = otherTable.getNames();
        int otherCount = otherNames.size();

        if ( thisCount != otherCount ) {
            return true;
        }

        for ( int nameNo = 0; nameNo < thisCount; nameNo++ ) {
            String thisName = theseNames.get(nameNo);
            String otherName = otherNames.get(nameNo);

            if ( !thisName.equals(otherName) ) {
                return false;
            }

            ScanPolicy thisPolicy = getPolicy(thisName);
            ScanPolicy otherPolicy = otherTable.getPolicy(thisName);

            if ( thisPolicy != otherPolicy ) {
                return false;
            }
        }

        return true;
    }

    //

    @Override
    public List<TargetCache_ParseError> readUsing(TargetCache_Reader reader) throws IOException {
        return reader.read(this); // throws IOException
    }
}
