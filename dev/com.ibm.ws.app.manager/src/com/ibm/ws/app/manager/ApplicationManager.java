/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.logging.hpel.LogRecordContext;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.internal.AppManagerConstants;

import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.websphere.csi.J2EEName;

@Component(service = ApplicationManager.class,
           immediate = true,
           configurationPid = AppManagerConstants.MANAGEMENT_PID,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = "service.vendor=IBM")
public class ApplicationManager {
    private static final TraceComponent _tc = Tr.register(ApplicationManager.class);    

    // Standard OSGI service APIS ...
    
    /**
     * Standard OSGI component activation: Scrape the application manager properties,
     * then read prior extraction data, and register the application name callback.
     */
    protected void activate(ComponentContext context, Map<String, Object> properties) {
        modified(context, properties);

        File useExtractsLog = context.getBundleContext().getBundle().getDataFile("expandApps");
        readExtractsLog(useExtractsLog);

        registerAppNameCallback();
    }

    /**
     * Standard OSGI component de-activation: Clear the application name callback and
     * write the new extraction data.
     * 
     * TODO: The write is problematic.  See the comments on 'writeExtractionLog'.
     */    
    protected void deactivate(ComponentContext context) {
        deregisterAppNameCallback();

        writeExtractsLog();
    }

    /**
     * Standard OSGI component update: Scrape the application manager properties.
     * Forward the start and stop time-outs to the application state
     * coordinator.
     * 
     * This is used for normal activation and for configuration updates.
     * 
     * TODO: Enabling auto-expand does NOT cause the extraction data
     *       to be read.
     */
    @Modified
    protected void modified(ComponentContext context, Map<String, Object> properties) {
        // The setters have auto-generated trace; no trace is needed here.
        
        // Need the local typed assignments to force the type on 'getProperty'.

        // TODO: The defaults expressed here should be provided in the configuration
        //       metatype.

        Boolean autoExpandValue = getProperty(properties, "autoExpand", false);
        setExpandApps(autoExpandValue);

        String expandLocationDir = getProperty(properties, "expandLocation", null);
        setExpandLocation(expandLocationDir);

        Boolean useJandexValue = getProperty(properties, "useJandex", false);
        setUseJandex(useJandexValue);

        // TODO: These default timeouts seem to not interact with other
        //       timeouts, such as the server start and stop timeouts and with
        //       quiesce timeouts.

        long startTimeoutValue = getProperty(properties, "startTimeout", 30L);
        setStartTimeout(startTimeoutValue);

        long stopTimeoutValue = getProperty(properties, "stopTimeout", 30L);
        setStopTimeout(stopTimeoutValue);

        // TODO: Should this be in a different method?
        //       This step has a somewhat different character than scraping
        //       application manager properties.

        ApplicationStateCoordinator.setApplicationStartTimeout(startTimeoutValue);
        ApplicationStateCoordinator.setApplicationStopTimeout(stopTimeoutValue);        
    }

    /* Name to use as an extension key for the application name */
    private final static String APPNAME_KEY = "appName";

    /**
     * Callback used to retrieve the application J2EE name from component metadata.
     * 
     * TODO: Where is this used and why is it needed? The appearance here is not
     *       at all explained.
     *
     * TODO: Type dependencies present here would be better expressed elsewhere.
     */
    private static void registerAppNameCallback() {
        LogRecordContext.registerExtension(APPNAME_KEY,
            new LogRecordContext.Extension() {
                @Override
                @Trivial
                public String getValue() {
                    ComponentMetaData metaData =
                        ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
                    if ( metaData != null ) {
                        J2EEName name = metaData.getJ2EEName();
                        if ( name != null ) {
                            return name.getApplication();
                        }
                    }
                    return null;
                }
            } );
    }
    
    private static void deregisterAppNameCallback() {
        LogRecordContext.unregisterExtension(APPNAME_KEY);        
    }
    
    /**
     * Type safe property retrieval. Retrieve a named property. Answer
     * the supplied default if the named property is not present, or has
     * a null value. (A null value is not expected.)
     * 
     * The return value is cast to the parameter type. If that fails,
     * log the exception using auto FFDC and return the default value.
     * 
     * @param <T> Type parameter: The type of the property value.
     * @param properties Storage for properties.
     * @param name The name of the property which is to be retrieved.
     * @param deflt The default property value.
     * 
     * @return The property value. Either, the value retrieved from the
     *     properties collection, or the default property value.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getProperty(Map<String, Object> properties, String name, T deflt) {
        // Return the default if either the property is not present, or if the
        // property value is null, or if a different typed value is present.

        T value;
        try {
            T prop = (T) properties.get(name);
            if ( prop != null ) {
                value = prop;
            } else {
                value = deflt; // Assign the default if present and null.
            }
        } catch ( ClassCastException e ) {
            // Auto FFDC
            // TODO: This check for a class cast exception seems unnecessary:
            //       Configuration metadata type should always put properly
            //       typed values.
            value = deflt;
        }
        return value;
    }
    
    //

    // TODO: The read and write of the extract log is expected
    //       to occur within managed service operations, and does
    //       need to be synchronized. Updates do puts on a synchronized
    //       map, and are safe as well.
    //
    //       However, there guards might be needed if late expansions
    //       are possible.
    //
    //       Also, changing the write to occur on every extraction will
    //       require synchronization.
    
    /**
     * Tell if a target application archive (EAR or WAR) should be expanded.
     * 
     * @param id A unique ID assigned to the application.
     * @param appArchiveFile An application archive which is to be tested.
     * @param expandedDir The target location to which the application is to be expanded.
     * 
     * @return True or false telling if the application should be expanded.
     */
    @Trivial // Use explicit trace; we want to see the logic details    
    public boolean shouldExpand(String id, File appArchiveFile, File expandedDir) {
        String methodName = "shouldExpand";
        if ( _tc.isDebugEnabled() ) {
                Tr.debug(_tc,
                    "{0} App ID [ {1} ] Path [ {2} ]",
                    methodName, id, appArchiveFile.getAbsolutePath());
        }
        
        long currentModified = appArchiveFile.lastModified();
        long currentSize = appArchiveFile.length();
        
        ExtractedLogData priorData = extractsData.get(id);
        
        if ( _tc.isDebugEnabled() ) {
            Tr.debug(_tc,
                "{0} Current modified [ {1} ] size [ {2} ]",
                methodName, currentModified, currentSize);
            if ( priorData == null ) {
                Tr.debug(_tc, "{0} No prior data", methodName);
            } else {
                Tr.debug(_tc, methodName,
                    "{0} Prior modified [ {1} ] currentSize [ {2} ]",
                    methodName, priorData.lastUpdated, priorData.size);
            }
        }

        boolean shouldExpand;
        String shouldExpandCase;

        if ( !expandedDir.exists() ) {
            shouldExpand = true;
            if ( priorData != null ) {
                shouldExpandCase = "Strange: Expansion file is not present but expansion data is present";
            } else {
                shouldExpandCase = "No prior expansion";
            }
        } else {
            if ( priorData == null ) {
                shouldExpand = true;
                shouldExpandCase = "Strange: Expansion file is present but expansion data is not present";
            } else {
                if ( priorData.lastUpdated != currentModified ) {
                    shouldExpand = true;
                    shouldExpandCase = "Changed modified date";
                } else if ( priorData.size != currentSize ) {
                    shouldExpand = true;
                    shouldExpandCase = "Changed size";
                } else {
                    shouldExpand = false;
                    shouldExpandCase = "Valid prior expansion";
                }
            }
        }

        if ( _tc.isDebugEnabled() ) {
            Tr.debug(_tc,
                "{0} Should expand [ {1} ] ({2})",
                methodName, shouldExpand, shouldExpandCase);
        }
        
        // TODO: This put to the log is problematic:
        //
        //     The put should only be performed if the expansion was successful. This put
        //     occurs before the extraction, which is before knowing if the expansion
        //     was successful.
        //
        //     See com.ibm.ws.app.manager.ear.internal.EARDeployedAppInfoFactoryImpl.expand
        //     and com.ibm.ws.app.manager.war.internal.EARDeployedAppInfoFactoryImpl.expand.
        //
        //     The write, which is currently performed when this service component is
        //     de-activated, should happen immediately following a successful expansion.
        //     Even a failed expansion should update the log by removing the expansion
        //     record.

        if ( shouldExpand ) {
            putExtractsData(id, currentModified, currentSize);
        }
        return shouldExpand;
    }

    // Managing the extraction data ...
    
    private final int EXTRACTION_LOG_FORMAT = 0;
    
    private void readExtractsLog(File useExtractsLog) {
        String methodName = "readExtractionLog";
        
        // The extraction log must always be set: De-activation
        // will delete the expansion log if expansion was not enabled.

        extractsLog = useExtractsLog;
        if ( _tc.isDebugEnabled() ) {
            Tr.debug(_tc, "{0} Extraction log [ {1} ]", methodName, extractsLog.getAbsolutePath());
        }
        
        String readCase;
        
        if ( !expandApps ) {
            readCase = "No read: Extraction is not enabled";
        } else if ( !extractsLog.exists() ) {
            readCase = "No read: Extraction log does not exist";
        } else {
            
            try ( FileInputStream fin = new FileInputStream(extractsLog);
                  DataInputStream din = new DataInputStream(fin) ) {

                long format = din.readLong();
                if ( format != EXTRACTION_LOG_FORMAT ) {
                    throw new IOException("Unsupported extraction log format [ " + format + " ]");
                }
                 
                int numRecords = din.readInt();
                for ( int recordNo = 0; recordNo < numRecords; recordNo++ ) {
                    putExtractsData( readLogData(din) );
                }

                if ( _tc.isDebugEnabled() ) {
                    readCase = "Read [ " + numRecords + " ]";
                } else {
                    readCase = null;
                }

            } catch ( IOException ioe ) {
                // Auto FFDC
                
                // If we get a failure assume the file is corrupted and delete
                // worst case is we will re-extract.

                clearExtraction();

                readCase = "Exception [ " + ioe + " ]";                
            }
        }
        
        if ( _tc.isDebugEnabled() ) {
            Tr.debug(_tc, "{0} {1}", methodName, readCase);
        }        
    }
    
    // TODO: This write occurs during de-activation. That is problematic.
    //       See the comments, above.

    private void writeExtractsLog() {
        String methodName = "writeExtractionLog";

        if ( _tc.isDebugEnabled() ) {
            Tr.debug(_tc, "{0} Extraction log [ {1} ]", methodName, extractsLog.getAbsolutePath());
        }
        
        String writeCase;
        
        if ( expandApps ) {
            try ( FileOutputStream fout = new FileOutputStream(extractsLog);
                  DataOutputStream dout = new DataOutputStream(fout) ) {

                dout.writeLong(EXTRACTION_LOG_FORMAT); // file format version
                dout.writeInt( extractsData.size() );
                for ( ExtractedLogData data : extractsData.values() ) {
                    data.write(dout);
                }

                if ( _tc.isDebugEnabled() ) {
                    writeCase = "Wrote [ " + extractsData.size() + " ]";
                } else {
                    writeCase = null;
                }                
                
            } catch ( IOException ioe ) {
                // Auto FFDC
                
                // If we hit this just delete the file on the assumption that
                // we will just re-extract, which is less efficient, but is not
                // a total failure.
                clearExtraction();

                writeCase = "Exception [ " + ioe + " ]";                
            }

        } else if ( extractsLog.exists() ) {
            writeCase = "Removing unused extraction log";
            
            // TODO: Is this correct? Discarding the log forces the
            //       applications to be re-expanded.

            clearExtraction();

        } else {
            writeCase = "No action; extraction disabled and no unused extraction log is present";
        }

        if ( _tc.isDebugEnabled() ) {
            Tr.debug(_tc, "{0} {1}", methodName, writeCase);
        }        
    }


    private File extractsLog;
    private final Map<String, ExtractedLogData> extractsData = new ConcurrentHashMap<>();

    private void clearExtraction() {
        extractsData.clear(); // TFB: Added
        extractsLog.delete();        
    }
    
    @Trivial
    private ExtractedLogData getExtractsData(String id) {
        return extractsData.get(id);
    }

    @Trivial    
    private void putExtractsData(ExtractedLogData data) {
        extractsData.put(data.id, data);
    }

    @Trivial    
    private void putExtractsData(String id, long modified, long size) {
        extractsData.put( id, new ExtractedLogData(id, modified, size) );            
    }    

    private static class ExtractedLogData {
        protected final String id;
        protected final long lastUpdated;
        protected final long size;

        protected ExtractedLogData(String id, long updated, long size) {
            this.id = id;
            this.lastUpdated = updated;
            this.size = size;
        }
        
        protected void write(DataOutputStream dOut) throws IOException {
            dOut.writeUTF(id);
            dOut.writeLong(lastUpdated);
            dOut.writeLong(size);
        }
    }

    private static ExtractedLogData readLogData(DataInputStream dIn) throws IOException {
        String id = dIn.readUTF();
        long updated = dIn.readLong();
        long size = dIn.readLong();

        return new ExtractedLogData(id, updated, size);
    }
        
    // Raw application manager settings ...
 
    private boolean expandApps;
    private String expandLocation;
    
    private boolean useJandex;
    
    private long startTimeout;
    private long stopTimeout;

    // These getters and setters are specifically not marked as Trivial.
    //
    // That removes the need for particular logging in code
    // which uses these values.

    public boolean getExpandApps() {
        return this.expandApps;
    }

    private void setExpandApps(boolean expandApps) {
        this.expandApps = expandApps;
    }

    private void setExpandLocation(String expandLocation) {
        this.expandLocation = expandLocation;
    }

    public String getExpandLocation() {
        return this.expandLocation;
    }
    
    public boolean getUseJandex() {
        return this.useJandex;
    }

    private void setUseJandex(boolean useJandex) {
        this.useJandex = useJandex;
    }

    public long getStartTimeout() {
        return this.startTimeout;
    }

    private void setStartTimeout(long startTimeout) {
        this.startTimeout = startTimeout;
    }

    public long getStopTimeout() {
        return this.stopTimeout;
    }

    private void setStopTimeout(long stopTimeout) {
        this.stopTimeout = stopTimeout;
    }
}