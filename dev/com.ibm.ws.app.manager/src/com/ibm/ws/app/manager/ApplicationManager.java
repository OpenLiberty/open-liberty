/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.app.manager.internal.AppManagerConstants;

@Component(service = ApplicationManager.class,
           immediate = true,
           configurationPid = AppManagerConstants.MANAGEMENT_PID,
           configurationPolicy = ConfigurationPolicy.REQUIRE,
           property = "service.vendor=IBM")
public class ApplicationManager {

    private boolean expandApps;
    private boolean useJandex;
    private long startTimeout;
    private long stopTimeout;

    private File extractedLog;
    private ConcurrentMap<String, ExtractedLogData> extractsDataLog = new ConcurrentHashMap<>();

    protected void activate(ComponentContext compcontext, Map<String, Object> properties) {
        modified(compcontext, properties);

        extractedLog = compcontext.getBundleContext().getBundle().getDataFile("expandApps");
        if (expandApps && extractedLog.exists()) {
            try (DataInputStream din = new DataInputStream(new FileInputStream(extractedLog))) {
                long format = din.readLong();
                if (format == 0) {
                    int len = din.readInt();
                    for (int i = 0; i < len; i++) {
                        String id = din.readUTF();
                        long updated = din.readLong();
                        long size = din.readLong();
                        extractsDataLog.put(id, new ExtractedLogData(id, updated, size));
                    }
                }
            } catch (IOException ioe) {
                // If we get a failure assume the file is corrupted and delete
                // worst case is we will reextract.
                extractedLog.delete();
            }
        }
    }

    /**
     * DS method to deactivate this component
     *
     * @param compcontext the context of this component
     */
    protected void deactivate(ComponentContext compcontext) {
        if (expandApps) {
            try (DataOutputStream dout = new DataOutputStream(new FileOutputStream(extractedLog))) {
                dout.writeLong(0); // file format version
                dout.writeInt(extractsDataLog.size()); // number of entries
                for (ExtractedLogData data : extractsDataLog.values()) {
                    dout.writeUTF(data.id); // the id
                    dout.writeLong(data.lastUpdated); // the last time it was updated
                    dout.writeLong(data.size); // the file size
                }
            } catch (IOException ioe) {
                // If we hit this just delete the file on the assumption that
                // we will just reextract which is less efficient, but not
                // a total failure.
                extractedLog.delete();
            }
        } else if (extractedLog.exists()) {
            extractedLog.delete(); // attempt to delete since we aren't running expanded apps.
        }
    }

    /**
     * DS method to modify the configuration of this component
     *
     * @param compcontext the context of this component
     * @param properties the updated configuration properties
     */
    @Modified
    protected void modified(ComponentContext compcontext, Map<String, Object> properties) {
        Boolean autoExpandValue = (Boolean) properties.get("autoExpand");
        setExpandApps(autoExpandValue == null ? false : autoExpandValue);

        Boolean useJandexValue = getProperty(properties, "useJandex", false);
        setUseJandex(useJandexValue == null ? false : useJandexValue);
        //System.setProperty("com.ibm.ws.jandex.enable", useJandexValue.toString()); // Temporary -- REMOVE THIS LATER ////

        long startTimeoutValue = getProperty(properties, "startTimeout", 30L);
        setStartTimeout(startTimeoutValue);
        long stopTimeoutValue = getProperty(properties, "stopTimeout", 30L);
        setStopTimeout(stopTimeoutValue);
        ApplicationStateCoordinator.setApplicationStartTimeout(startTimeoutValue);
        ApplicationStateCoordinator.setApplicationStopTimeout(stopTimeoutValue);
    }

    //get a property and if not set, use the supplied default
    @SuppressWarnings("unchecked")
    private <T> T getProperty(Map<String, Object> properties, String name, T deflt) {
        T value = deflt;
        try {
            T prop = (T) properties.get(name);
            if (prop != null) {
                value = prop;
            }
        } catch (ClassCastException e) {
            //auto FFDC and allow the default value to be returned so that the server still starts
        }
        return value;
    }

    /**
     * @return true if the app should expand, false otherwise
     */
     public boolean shouldExpand(String id, File warFile, File expandedDir) {

         boolean result = true;

         long lastModified = -1;
         long size = -1;

         ExtractedLogData data = extractsDataLog.get(id);
         lastModified = warFile.lastModified();
         size = warFile.length();

         if (expandedDir.exists()) {
             if (data != null) {
                 result = data.lastUpdated != lastModified || data.size != size;
             }
         }

         if (result) {
             extractsDataLog.put(id, new ExtractedLogData(id, lastModified, size));
         }



         return result;
     }

    /**
     * @return
     */
    public boolean getExpandApps() {
        return this.expandApps;
    }

    /**
     * @param b
     */
    private void setExpandApps(boolean b) {
        this.expandApps = b;
    }

    /**
     * @return
     */
    public boolean getUseJandex() {
        return this.useJandex;
    }

    /**
     * @param b
     */
    private void setUseJandex(boolean b) {
        this.useJandex = b;
    }

    /**
     * @return
     */
    public long getStartTimeout() {
        return this.startTimeout;
    }

    /**
     * @param b
     */
    private void setStartTimeout(long b) {
        this.startTimeout = b;
    }

    /**
     * @return
     */
    public long getStopTimeout() {
        return this.stopTimeout;
    }

    /**
     * @param b
     */
    private void setStopTimeout(long b) {
        this.stopTimeout = b;
    }

    private static class ExtractedLogData {
        private String id;
        private long lastUpdated;
        private long size;

        public ExtractedLogData(String id, long updated, long size) {
            this.id = id;
            this.lastUpdated = updated;
            this.size = size;
        }
    }
}
