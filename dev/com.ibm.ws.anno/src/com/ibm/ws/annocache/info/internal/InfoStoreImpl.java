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
package com.ibm.ws.annocache.info.internal;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.classsource.internal.ClassSourceImpl;
import com.ibm.wsspi.annocache.classsource.ClassSource_Aggregate;
import com.ibm.wsspi.annocache.classsource.ClassSource_Exception;
import com.ibm.wsspi.annocache.info.InfoStore;
import com.ibm.wsspi.annocache.info.InfoStoreException;

public class InfoStoreImpl implements InfoStore {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.annocache.info");
    private static final Logger stateLogger = Logger.getLogger("com.ibm.ws.annocache.info.state");

    public static final String CLASS_NAME = "InfoStoreImpl";

    // Hash text (as a unique ID for logging)

    protected final String hashText;

    @Override
    public String getHashText() {
        return hashText;
    }

    //

    public InfoStoreImpl(InfoStoreFactoryImpl infoStoreFactory, ClassSource_Aggregate classSource) {
        String methodName = "<init>";

        this.hashText = getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());

        this.infoStoreFactory = infoStoreFactory;
        this.classSource = classSource;

        this.classInfoCache = new ClassInfoCacheImpl(this);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ]", getHashText());
        }
    }

    //

    protected final InfoStoreFactoryImpl infoStoreFactory;

    @Override
    public InfoStoreFactoryImpl getInfoStoreFactory() {
        return infoStoreFactory;
    }

    //

    protected final ClassSource_Aggregate classSource;

    @Override
    public ClassSource_Aggregate getClassSource() {
        return this.classSource;
    }

    // @Override
	// public com.ibm.wsspi.anno.classsource.ClassSource_Aggregate getClassSource() {
    // return null;
	// }

    /**
     * Open the InfoStore for processing. Primarily, this will open the ClassSources attached to this
     * InfoStore which will then allow classes to be accessed.
     *
     * @throws InfoStoreException Thrown if the info store could not be opened.
     */
    @Override
    public void open() throws InfoStoreException {
        String methodName = "open";
        try {
            getClassSource().open();
        } catch (ClassSource_Exception e) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level.
            // CWWKC0026W
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_INFOSTORE_OPEN1_EXCEPTION",
            // new Object[] { getHashText(), getClassSource().getHashText() });

            String eMsg = "[ " + getHashText() + " ] Failed to open class source ";
            throw InfoStoreException.wrap(logger, CLASS_NAME, methodName, eMsg, e);
        }
    }

    /**
     * Close the InfoStore to end processing. This will call close on all of the ClassSources attached to this
     * InfoStore.
     *
     * @throws InfoStoreException Thrown if the info store could not be closed.
     */
    @Override
    public void close() throws InfoStoreException {
        String methodName = "close";
        try {
            getClassSource().close();

        } catch (ClassSource_Exception e) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level.
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_INFOSTORE_CLOSE1_EXCEPTION",
            //     new Object[] { getHashText(), getClassSource().getHashText() }); // CWWKC0027W

            String eMsg = "[ " + getHashText() + " ] Failed to close class source ";
            throw InfoStoreException.wrap(logger, CLASS_NAME, methodName, eMsg, e);
        }
    }

    // Visitor helpers ...

    public void scanClass(String className) throws InfoStoreException {
        String methodName = "scanClass";

        Object[] logParms;
        if (logger.isLoggable(Level.FINER)) {
            logParms = new Object[] { getHashText(), className };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] ENTER", logParms);
        } else {
            logParms = null;
        }

        ClassInfoImpl classInfo = getNonDelayedClassInfo(className);

        if (classInfo != null) {
            if (logParms != null) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] RETURN Already loaded", logParms);
            }
            return;
        }

        scanNewClass(className); // throws AnnotationScannerException

        if (logParms != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] RETURN New load", logParms);
        }
    }

    public void scanNewClass(String className) throws InfoStoreException {
        String methodName = "scanNewClass";

        Object[] logParms;

        if (logger.isLoggable(Level.FINER)) {
            logParms = new Object[] { getHashText(), className, null };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] ENTER", logParms);

        } else {
            logParms = null;
        }

        incrementStreamCount();

        startStreamTime();

        String resourceName = ClassSourceImpl.resourceNameFromClassName(className);
        InputStream inputStream = null;
        try {
            inputStream = getClassSource().openResourceStream(className, resourceName); // throws ClassSource_Exception
        } catch (ClassSource_Exception e) {
            // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
            // Disable the following warning and defer message generation to a higher level,
            // preferably the ultimate consumer of the exception.
            //  CWWKC0028W
            // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_INFOSTORE_OPEN2_EXCEPTION",
            //     new Object[] { getHashText(), resourceName, className });
            String eMsg = "[ " + getHashText() + " ]" +
                          " Failed to open input stream for resource [ " + resourceName + " ]" +
                          " class [ " + className + " ]" +
                          " from [ " + getClassSource().getApplicationName() + ":" + getClassSource().getModuleName() + " ]";
            throw InfoStoreException.wrap(logger, CLASS_NAME, methodName, eMsg, e);
        }

        if (inputStream != null) {
            try {
                scanNewClass(resourceName, className, inputStream); // throws InfoStoreException

            } finally {
                try {
                    getClassSource().closeResourceStream(className, resourceName, inputStream); //  throws ClassSource_Exception
                } catch (ClassSource_Exception e) {
                    // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
                    // Disable the following warning and defer message generation to a higher level.
                    //  CWWKC0029W
                    // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_INFOSTORE_CLOSE2_EXCEPTION",
                    // new Object[] { getHashText(), resourceName, className });
                    String eMsg = "[ " + getHashText() + " ]" +
                                  " Failed to close input stream for resource [ " + resourceName + " ]" +
                                  " for class [ " + className + " ]";
                    throw InfoStoreException.wrap(logger, CLASS_NAME, methodName, eMsg, e);
                }

                endStreamTime();
            }
        } else {
            // input stream is null. give a warning and return....
            endStreamTime();

            if (logParms != null) {
                logParms[2] = resourceName;
                logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Failed to open input stream for Class [ {1} ], resource [ {2} ]",
                                                  logParms);
            }
        }

        if (logParms != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] Class [ {1} ] RETURN", logParms);
        }
    }

    public void scanNewClass(String resourceName, String className, InputStream inputStream) throws InfoStoreException {
        String methodName = "scanNewClass";

        Object[] logParms;
        if (logger.isLoggable(Level.FINER)) {
            logParms = new Object[] { getHashText(), className, resourceName };
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] ENTER Class [ {1} ] from resource [ {2} ]", logParms);
        } else {
            logParms = null;
        }

        startScanTime();

        try {
            ClassReader classReader;

            try {
                // Both IOException and Exception have been seen;
                // in particular ArrayindexOutOfBoundsException for a non-valid class.

                classReader = new ClassReader(inputStream); // throws IOException, Exception

            } catch (Exception e) {
                // defect 84235:we are generating multiple Warning/Error messages for each error due to each level reporting them.
                // Disable the following warning and defer message generation to a higher level.
                // logger.logp(Level.WARNING, CLASS_NAME, methodName, "ANNO_CREATE_READER_EXCEPTION",
                //     new Object[] { className, resourceName }); // CWWKC0030W
                String eMsg = "Class [ " + className + " ] from resource [ " + resourceName + " ] Exception creating reader";
                throw InfoStoreException.wrap(logger, CLASS_NAME, methodName, eMsg, e);
            }

            ClassVisitorImpl_Info infoVisitor = new ClassVisitorImpl_Info(this, className);

            try {
                classReader.accept(infoVisitor, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE);

            } catch (ClassVisitorImpl_Info.VisitEnded e) {
                // Already logged a warning from the info visitor.

                String eMsg = "Target [ " + className + " ] from resource [ " + resourceName + " ] Processing exception: " + e.getMessage();
                throw InfoStoreException.wrap(logger, CLASS_NAME, methodName, eMsg, e);
            }

        } finally {
            endScanTime();
        }

        if (logParms != null) {
            logger.logp(Level.FINER, CLASS_NAME, methodName, "[ {0} ] RETURN Class [ {1} ] from resource [ {2} ]", logParms);
        }
    }

    // Storage is delegated to a cache widget ...

    protected ClassInfoCacheImpl classInfoCache;

    public ClassInfoCacheImpl getClassInfoCache() {
        return classInfoCache;
    }

    // Name storage ...

    @Override
    public String internDescription(String name) {
        return getClassInfoCache().internDescription(name);
    }

    @Override
    public String internPackageName(String packageName) {
        return getClassInfoCache().internPackageName(packageName);
    }

    @Override
    public String internClassName(String className) {
        return getClassInfoCache().internClassName(className);
    }

    public String internMethodName(String name) {
        return getClassInfoCache().internMethodName(name);
    }

    public String internFieldName(String name) {
        return getClassInfoCache().internFieldName(name);
    }

    // Package storages ...

    protected PackageInfoImpl basicGetPackageInfo(String name) {
        return getClassInfoCache().basicGetPackageInfo(name);
    }

    protected PackageInfoImpl basicAddPackageInfo(String name, int access) {
        return getClassInfoCache().basicAddPackageInfo(name, access);
    }

    // Public API: Retrieve package information, if available.
    //
    // Do not force the package info to be created if no resource is
    // available.  However, if a resource is available, always answer
    // a package info, even if an error occurrs.

    @Override
    public PackageInfoImpl getPackageInfo(String name) {
        return getClassInfoCache().getPackageInfo(name, ClassInfoCacheImpl.DO_NOT_FORCE_PACKAGE);
    }

    // Internal API: A package reference exists relative to class information.
    //
    // Create a package info object.  Even if an error occurs, force the package
    // info to be creaed.

    protected PackageInfoImpl getPackageInfo(String name, boolean doForce) {
        return getClassInfoCache().getPackageInfo(name, doForce);
    }

    //

    @Override
    public ClassInfoImpl getDelayableClassInfo(String name) {
        return getClassInfoCache().getDelayableClassInfo(name, ClassInfoCacheImpl.DO_ALLOW_PRIMITIVE);
    }

    protected ClassInfoImpl getNonDelayedClassInfo(String name) {
        return getClassInfoCache().getNonDelayedClassInfo(name, ClassInfoCacheImpl.DO_ALLOW_PRIMITIVE);
    }

    protected ClassInfoImpl getDelayableClassInfo(Type type) {
        return getClassInfoCache().getDelayableClassInfo(type);
    }

    //

    protected boolean addClassInfo(NonDelayedClassInfoImpl classInfo) {
        return getClassInfoCache().addClassInfo(classInfo);
    }

    protected NonDelayedClassInfoImpl createClassInfo(String name, String superName, int access, String[] interfaces) {
        return getClassInfoCache().createClassInfo(name, superName, access, interfaces);
    }

    //

    protected NonDelayedClassInfoImpl resolveClassInfo(String name) {
        return getClassInfoCache().resolveClassInfo(name);
    }

    //

    protected void recordAccess(NonDelayedClassInfoImpl classInfo) {
        getClassInfoCache().recordAccess(classInfo);
    }

    protected void removeAsDelayable(NonDelayedClassInfoImpl classInfo) {
        getClassInfoCache().removeAsDelayable(classInfo);
    }

    //

    @Override
    public void logState() {
        if (stateLogger.isLoggable(Level.FINER)) {
            log(stateLogger);
        }
    }

    @Override
    public void log(Logger useLogger) {
        String methodName = "log";

        if ( !useLogger.isLoggable(Level.FINER) ) {
            return;
        }

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "BEGIN STATE [ {0} ]", getHashText());

        getClassInfoCache().log(useLogger);

        useLogger.logp(Level.FINER, CLASS_NAME, methodName, "END STATE [ {0} ]", getHashText());
    }

    //
    
    @Override
    @Trivial
    public void log(TraceComponent tc) {

        Tr.debug(tc, MessageFormat.format("BEGIN STATE [ {0} ]", getHashText()));

        getClassInfoCache().log(tc);

        Tr.debug(tc, MessageFormat.format("END STATE [ {0} ]", getHashText()));
    }

    //

    protected int streamCount;

    @Override
    public long getStreamCount() {
        return streamCount;
    }

    protected void incrementStreamCount() {
        streamCount++;
    }

    protected ActivityTimerImpl activityTimer = new ActivityTimerImpl();

    protected void startActivity() {
        activityTimer.startActivity();
    }

    protected long endActivity() {
        return activityTimer.endActivity();
    }

    protected long streamTime;

    @Override
    public long getStreamTime() {
        return streamTime;
    }

    protected void startStreamTime() {
        startActivity();
    }

    protected void endStreamTime() {
        streamTime += endActivity();
    }

    protected long scanTime;

    @Override
    public long getScanTime() {
        return scanTime;
    }

    protected void startScanTime() {
        startActivity();
    }

    protected void endScanTime() {
        scanTime += endActivity();
    }

    protected long ruleTime;

    @Override
    public long getRuleTime() {
        return ruleTime;
    }

    protected void startRuleTime() {
        startActivity();
    }

    protected void endRuleTime() {
        ruleTime += endActivity();
    }
}
