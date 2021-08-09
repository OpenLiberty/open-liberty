/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.internal;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.service.internal.AnnotationCacheServiceImpl_Logging;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_DataApps;
import com.ibm.ws.annocache.targets.cache.internal.TargetCacheImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_Factory;
import com.ibm.ws.annocache.util.internal.UtilImpl_InternMap;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Exception;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Factory;
import com.ibm.wsspi.annocache.targets.AnnotationTargets_Fault;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Factory;
import com.ibm.wsspi.annocache.util.Util_Factory;
import com.ibm.wsspi.annocache.util.Util_InternMap;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM"})
public class AnnotationTargetsImpl_Factory implements AnnotationTargets_Factory {
    protected static final Logger logger = AnnotationCacheServiceImpl_Logging.ANNO_LOGGER;

    public static final String CLASS_NAME = AnnotationTargetsImpl_Factory.class.getSimpleName();

    //

    protected final String hashText;

    @Override
    @Trivial
    public String getHashText() {
        return hashText;
    }

    @Activate
    public AnnotationTargetsImpl_Factory(
        @Reference Util_Factory utilFactory,
        @Reference ClassSource_Factory classSourceFactory,
        @Reference TargetCache_Factory annoCacheFactory) {

        super();

        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.utilFactory = (UtilImpl_Factory) utilFactory;
        this.cacheFactory = (TargetCacheImpl_Factory) annoCacheFactory;

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                "[ {0} ] using [ {1} ]",
                new Object[] { this.hashText, this.utilFactory.getHashText() });
        }
    }

    //

    protected final UtilImpl_Factory utilFactory;

    @Override
    @Trivial
    public UtilImpl_Factory getUtilFactory() {
        return utilFactory;
    }

    //

    protected final TargetCacheImpl_Factory cacheFactory;

    @Trivial
    public TargetCacheImpl_Factory getCacheFactory() {
        return cacheFactory;
    }

    @Trivial
    public TargetCacheImpl_DataApps getCache() {
        return getCacheFactory().getCache();
    }

    //

    @Override
    @Trivial
    public AnnotationTargets_Exception newAnnotationTargetsException(Logger useLogger, String message) {
        String methodName = "newAnnotationTargetsException";

        AnnotationTargets_Exception exception = new AnnotationTargets_Exception(message);

        if ( useLogger.isLoggable(Level.FINER) ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName, "Created [ {0} ]", message);
        }

        return exception;
    }

    @Override
    @Trivial
    public AnnotationTargets_Exception wrapIntoAnnotationTargetsException(Logger useLogger,
                                                                          String callingClassName,
                                                                          String callingMethodName,
                                                                          String message, Throwable th) {
        String methodName = "wrapIntoAnnotationTargetsException";

        AnnotationTargets_Exception wrappedException = new AnnotationTargets_Exception(message, th);

        if ( useLogger.isLoggable(Level.FINER) ) {
            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "[ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                        new Object[] { callingClassName,
                                       callingMethodName,
                                       th.getClass().getName(),
                                       wrappedException.getClass().getName() });

            useLogger.logp(Level.FINER, CLASS_NAME, methodName,
                        "Wrapped [ {0} ] [ {1} ]",
                        new Object[] { th.getMessage(), th.getClass().getName() });
        }

        return wrappedException;
    }

    // Global scan APIs ...

    @Override
    @Trivial
    public AnnotationTargetsImpl_Targets createTargets()
        throws AnnotationTargets_Exception {

        return createTargets(AnnotationTargets_Factory.IS_NOT_LIGHTWEIGHT);
        // throws AnnotationTargets_Exception
    }
    
    @Override
    @Trivial
    public AnnotationTargetsImpl_Targets createTargets(boolean isLightweight)
        throws AnnotationTargets_Exception {

        return new AnnotationTargetsImpl_Targets( this,
                                                  getCache(),
                                                  createClassNameInternMap(),
                                                  createFieldNameInternMap(),
                                                  createMethodSignatureInternMap(),
                                                  isLightweight );
        // throws AnnotationTargets_Exception
    }

    // These are needed for concurrent scanning.

    @Trivial
    protected UtilImpl_InternMap createClassNameInternMap() {
        return getUtilFactory().createInternMap(Util_InternMap.ValueType.VT_CLASS_NAME, "classes and package names");
    }

    @Trivial
    protected UtilImpl_InternMap createFieldNameInternMap() {
        return getUtilFactory().createInternMap(Util_InternMap.ValueType.VT_FIELD_NAME, "field names");
    }

    @Trivial
    protected UtilImpl_InternMap createMethodSignatureInternMap() {
        return getUtilFactory().createInternMap(Util_InternMap.ValueType.VT_OTHER, "method signatures");
    }

    //

    @Override
    @Trivial
    public AnnotationTargetsImpl_Fault createFault(String unresolvedText) {
        return new AnnotationTargetsImpl_Fault(unresolvedText);
    }

    @Override
    @Trivial
    public AnnotationTargetsImpl_Fault createFault(String unresolvedText, String parameter) {
        return new AnnotationTargetsImpl_Fault(unresolvedText, new String[] { parameter });
    }

    @Override
    public AnnotationTargets_Fault createFault(String unresolvedText, String[] parameters) {
        return new AnnotationTargetsImpl_Fault(unresolvedText, parameters);
    }

    //

    @Override
    @Trivial
    public AnnotationTargetsImpl_Targets createTargets(ClassSource_Aggregate classSource)
        throws AnnotationTargets_Exception {

        AnnotationTargetsImpl_Targets targets = createTargets(); // throws AnnotationTargets_Exception

        targets.scan(classSource); // This just sets the class source ... the scan is done on demand.

        return targets;
    }

    // Limited scan APIs ...

    @Override
    public AnnotationTargetsImpl_Targets createTargets(ClassSource_Aggregate classSource,
                                                       Set<String> specificClassNames,
                                                       Set<String> specificAnnotationClassNames)
        throws AnnotationTargets_Exception {

        AnnotationTargetsImpl_Targets specificTargets = createTargets();
        // throws AnnotationTargets_Exception

        specificTargets.scan(classSource, specificClassNames, specificAnnotationClassNames);
        // throws AnnotationTargets_Exception

        return specificTargets;
    }

    @Override
    public AnnotationTargetsImpl_Targets createTargets(ClassSource_Aggregate classSource,
                                                       Set<String> specificClassNames)
        throws AnnotationTargets_Exception {

        AnnotationTargetsImpl_Targets specificTargets = createTargets();
        // throws AnnotationTargets_Exception

        specificTargets.scan(classSource, specificClassNames, TargetsVisitorClassImpl.SELECT_ALL_ANNOTATIONS);
        // throws AnnotationTargets_Exception

        return specificTargets;
    }

    //

    @Override
    public AnnotationTargets_Exception newAnnotationTargetsException(TraceComponent useLogger, String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public com.ibm.wsspi.anno.targets.AnnotationTargets_Exception wrapIntoAnnotationTargetsException(
            TraceComponent tc, String callingClassName, String callingMethodName, String message, Throwable th) {
        throw new UnsupportedOperationException();
    }
}
