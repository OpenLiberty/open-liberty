/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.logging.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import com.ibm.ejs.ras.TrLevelConstants;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.logging.hpel.LogRecordContext;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TraceStateChangeListener;
import com.ibm.ws.kernel.boot.logging.WsLogManager;

/**
 * WebSphere extension of a Java Logger object. This uses the deprecated RAS
 * level due to not having Class information to register, only names.
 */
public class WsLogger extends Logger implements TraceStateChangeListener {

    //
    // ResourceBundle cached with this WsLogger
    //
    private String ivCachedResourceBundleName = null;
    private ResourceBundle ivCachedResourceBundle = null;

    //
    // Special LogRecord payload extensions
    //
    private String ivComponent; // used specifically by TraceLogFormatter
    private String ivOrganization; // used specifically by TraceLogFormatter
    private String ivProduct; // used specifically by TraceLogFormatter

    /**
     * The integer representation of the lowest level at which localization will
     * be performed
     */
    private int ivMinimumLocalizationLevelIntValue = TrLevelConstants.MIN_LOCALIZATION.intValue();

    /**
     * The corresponding TC
     */
    private TraceComponent ivTC;

    /**
     * If we're creating the logger for an existing trace component, this will
     * let us find it
     */
    public static final ThreadLocal<TraceComponent> loggerRegistrationComponent = new ThreadLocal<TraceComponent>();

    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level and with
     * useParentHandlers true.
     *
     * @param name
     *            A name for the logger. This should be a dot-separated name and
     *            should normally be based on the package name or class name of
     *            the subsystem, such as java.net or javax.swing. It may be null
     *            for anonymous Loggers.
     * @param resourceBundleName
     *            name of ResourceBundle to be used for localizing messages for
     *            this logger. May be null if none of the messages require
     *            localization.
     * @throws MissingResourceException
     *             if the ResourceBundleName is non-null and no corresponding
     *             resource can be found.
     */
    public WsLogger(String name, Class<?> c, String resourceBundleName) {
        super(name, resourceBundleName);

        if (resourceBundleName != null && !resourceBundleName.equals("")) {
            // store the bundle for quick access
            this.ivCachedResourceBundleName = resourceBundleName;
            this.ivCachedResourceBundle = getResourceBundle(resourceBundleName);
        }

        ivTC = loggerRegistrationComponent.get();
        if (ivTC == null)
            registerTraceComponent(name, c, resourceBundleName);

        LogManager.getLogManager().addLogger(this);
    }

    /**
     * Set the minimum localization level. Logging requests made subsequent to
     * this call will not be localized by WAS handlers unless their level is at
     * or above the specified minimum. This is a performance optimization to
     * reduce overhead of trying to localize messages that are not meant to be
     * localized.
     *
     * @param level
     */
    public void setMinimumLocalizationLevel(Level level) {
        if (level == null) {
            return;
        }
        this.ivMinimumLocalizationLevelIntValue = level.intValue();
    }

    /*
     * @see java.util.logging.Logger#log(java.util.logging.LogRecord)
     */
    @Override
    public void log(LogRecord logRecord) {
        if (logRecord == null) {
            return;
        }

        Filter f = getFilter();
        if (isLoggable(logRecord.getLevel()) && (f == null || f.isLoggable(logRecord))) {

            // LogRecord computes sourceClassName and sourceMethodName as the
            // caller to Logger.log method. WsLogger is the caller of Logger.log. We
            // don't want to be identified as the caller, so we call getSourceClassName
            // here before calling Logger.log. If sourceClassName is floating,
            // this will set it to null.
            logRecord.getSourceClassName();
            super.log(logRecord);
        }
    }

    /**
     * Creates a LogRecord using any available input parameters from the various
     * Logger methods.
     *
     * @param level
     * @param msg
     * @param params
     * @param sourceClassName
     * @param sourceMethodName
     * @param resourceBundleName
     * @param thrown
     * @return LogRecord
     */
    private LogRecord createLogRecord(Level level, String msg, Object[] params, String sourceClassName, String sourceMethodName, String resourceBundleName, Throwable thrown) {

        ResourceBundle resourceBundle = null;
        if (level.intValue() >= this.ivMinimumLocalizationLevelIntValue) {
            if (null == this.ivCachedResourceBundle) {
                this.ivCachedResourceBundle = super.getResourceBundle();
                this.ivCachedResourceBundleName = super.getResourceBundleName();
            }
            // if resourceBundleName not set by caller, look it up from logger
            // (and logger's parents if needed).
            if (resourceBundleName == null) {
                resourceBundleName = computeResourceBundleName();
            }

            // get the resourceBundle
            if (resourceBundleName != null) {
                if (resourceBundleName.equals(this.ivCachedResourceBundleName)) {
                    resourceBundle = this.ivCachedResourceBundle;
                } else {
                    resourceBundle = getResourceBundle(resourceBundleName);
                }
            }
            if (null == resourceBundle && null != sourceClassName) {
                try {
                    Class<?> source = Class.forName(sourceClassName);
                    resourceBundle = ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), source.getClassLoader());
                } catch (Throwable t) {
                    // unable to find the resource bundle
                }
            }
        }

        return createWsLogRecord(level, msg, params, sourceClassName, sourceMethodName, resourceBundleName, resourceBundle, thrown);
    }

    /**
     * Construct a WsLogRecord with the given level and message values.
     *
     * @param level
     *            the logging Level value. Must NOT be null
     * @param msg
     *            the message. May be a text message, pre-localized message or a
     *            message key (in a resource bundle).
     * @param sourceClassName
     * @param sourceMethodName
     * @param resourceBundleName
     * @param resourceBundle
     * @param thrown
     */
    private WsLogRecord createWsLogRecord(Level level, String msg, Object[] params, String sourceClassName, String sourceMethodName, String resourceBundleName,
                                          ResourceBundle resourceBundle, Throwable thrown) {

        WsLogRecord logRecord = new WsLogRecord(level, msg);

        if (params != null) {
            logRecord.setParameters(params);
            // special handling for byte arrays in the first param position
            if ((params.length > 0 && params[0] != null) && byte.class.equals(params[0].getClass().getComponentType()))
                logRecord.setRawData((byte[]) params[0]);
        }
        if (sourceClassName != null) {
            logRecord.setSourceClassName(sourceClassName);
        }
        if (sourceMethodName != null) {
            logRecord.setSourceMethodName(sourceMethodName);
        }
        if (resourceBundleName != null) {
            logRecord.setResourceBundleName(resourceBundleName);
        }
        if (resourceBundle != null) {
            logRecord.setResourceBundle(resourceBundle);
        }
        if (thrown != null) {
            logRecord.setThrown(thrown);
        }
        if (getName() != null) {
            logRecord.setLoggerName(getName());
        }
        if (getOrganization() != null) {
            logRecord.setOrganization(getOrganization());
        }
        if (getProduct() != null) {
            logRecord.setProduct(getProduct());
        }
        if (getComponent() != null) {
            logRecord.setComponent(getComponent());
        }

        LogRecordContext.getExtensions(logRecord.getExtensions());        
        
        logRecord.setTraceClass(ivTC.getTraceClass());

        // populate runtime data
        // Note: this is WAS version, UOW, process ID, etc
        // WsLoggerRuntimeData.getInstance().populate(logRecord);

        return logRecord;
    }

    /*
     * @see java.util.logging.Logger#log(java.util.logging.Level,
     * java.lang.String)
     */
    @Override
    public void log(Level level, String msg) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, null, null, null, null, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#log(java.util.logging.Level,
     * java.lang.String, java.lang.Object)
     */
    @Override
    public void log(Level level, String msg, Object param1) {
        if (isLoggable(level)) {
            Object params[] = { param1 };
            LogRecord logRecord = createLogRecord(level, msg, params, null, null, null, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#log(java.util.logging.Level,
     * java.lang.String, java.lang.Object[])
     */
    @Override
    public void log(Level level, String msg, Object params[]) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, params, null, null, null, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#log(java.util.logging.Level,
     * java.lang.String, java.lang.Throwable)
     */
    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, null, null, null, null, thrown);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logp(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, null, sourceClass, sourceMethod, null, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logp(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     */
    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
        if (isLoggable(level)) {
            Object params[] = { param1 };
            LogRecord logRecord = createLogRecord(level, msg, params, sourceClass, sourceMethod, null, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logp(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.Object[])
     */
    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object params[]) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, params, sourceClass, sourceMethod, null, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logp(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String,
     * java.lang.Throwable)
     */
    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, null, sourceClass, sourceMethod, null, thrown);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logrb(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, null, sourceClass, sourceMethod, bundleName, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logrb(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {
        if (isLoggable(level)) {
            Object params[] = { param1 };
            LogRecord logRecord = createLogRecord(level, msg, params, sourceClass, sourceMethod, bundleName, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logrb(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.Object[])
     */
    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object params[]) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, params, sourceClass, sourceMethod, bundleName, null);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#logrb(java.util.logging.Level,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.Throwable)
     */
    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            LogRecord logRecord = createLogRecord(level, msg, null, sourceClass, sourceMethod, bundleName, thrown);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#entering(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void entering(String sourceClass, String sourceMethod) {
        if (isLoggable(Level.FINER)) {
            logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
        }
    }

    /*
     * @see java.util.logging.Logger#entering(java.lang.String,
     * java.lang.String, java.lang.Object)
     */
    @Override
    public void entering(String sourceClass, String sourceMethod, Object param1) {
        if (isLoggable(Level.FINER)) {
            Object params[] = { param1 };
            logp(Level.FINER, sourceClass, sourceMethod, "ENTRY {0}", params);
        }
    }

    /*
     * @see java.util.logging.Logger#entering(java.lang.String,
     * java.lang.String, java.lang.Object[])
     */
    @Override
    public void entering(String sourceClass, String sourceMethod, Object params[]) {
        if (isLoggable(Level.FINER)) {
            String msg = "ENTRY";
            if (params != null) {
                StringBuilder sb = new StringBuilder(msg);
                for (int i = 0; i < params.length; i++) {
                    sb.append(" {").append(i).append("}");
                }
                msg = sb.toString();
            }
            logp(Level.FINER, sourceClass, sourceMethod, msg, params);
        }
    }

    /*
     * @see java.util.logging.Logger#exiting(java.lang.String, java.lang.String)
     */
    @Override
    public void exiting(String sourceClass, String sourceMethod) {
        if (isLoggable(Level.FINER)) {
            logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
        }
    }

    /*
     * @see java.util.logging.Logger#exiting(java.lang.String, java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void exiting(String sourceClass, String sourceMethod, Object result) {
        if (isLoggable(Level.FINER)) {
            Object params[] = { result };
            logp(Level.FINER, sourceClass, sourceMethod, "RETURN {0}", params);
        }
    }

    /*
     * @see java.util.logging.Logger#throwing(java.lang.String,
     * java.lang.String, java.lang.Throwable)
     */
    @Override
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (isLoggable(Level.FINER)) {
            LogRecord logRecord = createLogRecord(Level.FINER, "THROW", null, sourceClass, sourceMethod, null, thrown);
            log(logRecord);
        }
    }

    /*
     * @see java.util.logging.Logger#severe(java.lang.String)
     */
    @Override
    public void severe(String msg) {
        if (isLoggable(Level.SEVERE)) {
            log(Level.SEVERE, msg);
        }
    }

    /*
     * @see java.util.logging.Logger#warning(java.lang.String)
     */
    @Override
    public void warning(String msg) {
        if (isLoggable(Level.WARNING)) {
            log(Level.WARNING, msg);
        }
    }

    /*
     * @see java.util.logging.Logger#info(java.lang.String)
     */
    @Override
    public void info(String msg) {
        if (isLoggable(Level.INFO)) {
            log(Level.INFO, msg);
        }
    }

    /*
     * @see java.util.logging.Logger#config(java.lang.String)
     */
    @Override
    public void config(String msg) {
        if (isLoggable(Level.CONFIG)) {
            log(Level.CONFIG, msg);
        }
    }

    /*
     * @see java.util.logging.Logger#fine(java.lang.String)
     */
    @Override
    public void fine(String msg) {
        if (isLoggable(Level.FINE)) {
            log(Level.FINE, msg);
        }
    }

    /*
     * @see java.util.logging.Logger#finer(java.lang.String)
     */
    @Override
    public void finer(String msg) {
        if (isLoggable(Level.FINER)) {
            log(Level.FINER, msg);
        }
    }

    /*
     * @see java.util.logging.Logger#finest(java.lang.String)
     */
    @Override
    public void finest(String msg) {
        if (isLoggable(Level.FINEST)) {
            log(Level.FINEST, msg);
        }
    }

    /**
     * Worker method to load the resource bundle corresponding to the specified
     * resource bundle name using the current default Locale.
     * <p>
     *
     * @param resourceBundleName
     *            the name of the resource bundle. Must not be null.
     * @return a reference to the resource bundle. Null if not found.
     */
    private ResourceBundle getResourceBundle(String name) {

        if (ivTC != null) {
            // The best odds for finding the resource bundle are with using the
            // classloader that loaded the associated class to begin with. Start
            // there.
            try {
                return TraceNLS.getResourceBundle(ivTC.getClass(), name, Locale.getDefault());
            } catch (MissingResourceException ex) {
                // no FFDC required
            }
        } else {
            try {
                return TraceNLS.getResourceBundle(null, name, Locale.getDefault());
            } catch (MissingResourceException ex) {
                // no FFDC required
            }
        }

        return null;
    }

    /**
     * Worker method to determine the resource bundle name to use for logger. If
     * set, this is just the resource bundle name of this logger. If not set,
     * move up through the list of parents and find the first non-null resource
     * bundle name.
     *
     * @return String the resource bundle name
     */
    private String computeResourceBundleName() {
        Logger logger = this;

        while (logger != null) {
            String name = logger.getResourceBundleName();
            if (name != null) {
                return name;
            }
            logger = logger.getParent();
        }

        return null;
    }

    private void registerTraceComponent(String name, Class<?> c, String resourceBundleName) {
        if (name != null && name.length() > 0) {

            if (c == null) {
                StackFinder finder = StackFinder.getInstance();

                if (finder != null)
                    c = finder.matchCaller(name);
            }

            if (c != null) {
                // Use the matched class
                this.ivTC = Tr.register(name, c, name, resourceBundleName);
            } else {
                // We couldn't find a non-obviously ras class: set the name/group to facilitate matching
                this.ivTC = Tr.register(name, null, name, resourceBundleName);
            }

            this.ivTC.setLoggerForCallback(this);

            // if we are loading via properties file, we don't want to
            // pre-initialize the logger level, so we'll skip this call:
            if (!WsLogManager.isConfiguredByLoggingProperties()) {
                traceStateChanged();
            }
        }
    }

    public void addGroup(String group) {
        TrSharedSecrets.getInstance().addGroup(ivTC, group);
    }

    /*
     * @see
     * com.ibm.ejs.ras.TraceStateChangeListener#traceStateChanged(java.lang.
     * String )
     */
    @Override
    public void traceStateChanged() {
        final Level rasLevel = this.ivTC.getLoggerLevel();
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                setLevel(rasLevel);
                return null;
            }
        });
    }

    /**
     * Query the optional component field. This returns null if not set.
     *
     * @return String
     */
    public String getComponent() {
        return this.ivComponent;
    }

    /**
     * Query the optional organization field. This returns null if not set.
     *
     * @return String
     */
    public String getOrganization() {
        return this.ivOrganization;
    }

    /**
     * Query the optional product field. This returns null if not set.
     *
     * @return String
     */
    public String getProduct() {
        return this.ivProduct;
    }

    /**
     * Set the optional component name to the input value. This is used in the
     * advanced output of trace.
     *
     * @param component
     */
    public void setComponent(String component) {
        this.ivComponent = component;
    }

    /**
     * Set the optional organization name to the input value. This is used in
     * the advanced output of trace.
     *
     * @param organization
     */
    public void setOrganization(String organization) {
        this.ivOrganization = organization;
    }

    /**
     * Set the optional product name to the input value. This is used in the
     * advanced output of trace.
     *
     * @param product
     */
    public void setProduct(String product) {
        this.ivProduct = product;
    }

}
