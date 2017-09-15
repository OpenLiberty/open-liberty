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
package com.ibm.ws.filetransfer.internal.mbean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.filetransfer.FileServiceMXBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.filetransfer.util.FileServiceUtil;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

/**
 * This class provides an implementation of the FileServiceMXBean and also publishes an event for every change to either
 * attributes (ReadList or WriteList).
 */
@Component(service = EventHandler.class,
           configurationPid = "com.ibm.ws.management.filetransfer",
           immediate = true,
           property = {
                       "jmx.objectname=" + FileServiceMXBean.OBJECT_NAME,
                       "publishAttributesToCollectiveController=true",
                       "service.vendor=IBM",
                       EventConstants.EVENT_TOPIC
                                       + "=com/ibm/wsspi/collective/repository/publishStatus/mbean"
                       , EventConstants.EVENT_FILTER
                         + "=(mbeanObjectName="
                         + FileServiceMXBean.OBJECT_NAME
                         + ")" })
public class FileService extends StandardMBean implements FileServiceMXBean, EventHandler {

    private static final TraceComponent tc = Tr.register(FileService.class);

    //Location service
    static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    //EventAdmin service
    static final String KEY_EVENT_ADMIN = "eventAdmin";
    private final AtomicServiceReference<EventAdmin> eventAdminRef = new AtomicServiceReference<EventAdmin>(KEY_EVENT_ADMIN);

    //Constants
    private static final String READ_LIST_CONFIGURATION = "readDir";
    private static final String WRITE_LIST_CONFIGURATION = "writeDir";
    private static final String EMPTY_PATH_STRING = "";

    private List<String> ReadList;
    private List<String> WriteList;

    public FileService() throws NotCompliantMBeanException {
        super(FileServiceMXBean.class, true);
    }

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) throws IOException {

        locationAdminRef.activate(context);
        eventAdminRef.activate(context);

        //Get the read/write lists from the configuration (defaults are already defined in the metatype file)
        processReadWriteLists(properties);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "FileServiceMXBean has been fully activated");
        }

    }

    @Modified
    protected void modified(Map<String, Object> properties) throws IOException {
        processReadWriteLists(properties);
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        locationAdminRef.deactivate(context);
        eventAdminRef.deactivate(context);
    }

    /**
     * This method makes sure the paths are using the separators according to the operating system.
     */
    private List<String> normalizePaths(String[] paths) {
        List<String> pathList = new ArrayList<String>();
        for (String path : paths) {
            pathList.add(normalizePath(path));
        }

        return pathList;
    }

    private String normalizePath(String path) {
        //We don't log empty path as an error here because it is valid to have an empty path for read/write lists
        //In the places where empty path is not allowed we log the error and do not call normalizePath
        if (EMPTY_PATH_STRING.equals(path)) {
            return EMPTY_PATH_STRING;
        }

        WsLocationAdmin wsLocation = getWsLocationAdmin();
        //If the WsLocationAdmin is null we will have already logged the fact that this service was missing,
        //but we can be robust and keep going.
        if (wsLocation != null) {
            path = wsLocation.resolveString(path);
        }

        //Check for trailing slashes..
        if (path != null && !path.isEmpty() && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    @SuppressWarnings("unchecked")
    private void processReadWriteLists(Map<String, Object> properties) throws IOException {

        //read lists
        Object readListConfig = properties.get(READ_LIST_CONFIGURATION);
        if (readListConfig instanceof String[]) {
            //Convert paths
            ReadList = normalizePaths((String[]) readListConfig);
        } else {
            //Default is to be able to read from install, user and output directories
            //We don't need to normalize these, because they come from the WsLocation service, so they
            //are assumed to be correct for the operation system.
            ReadList = new ArrayList<String>();

            WsLocationAdmin wsLocation = getWsLocationAdmin();

            if (wsLocation == null) {
                //in this case, we really cannot proceed without the location admin, so throw error.
                throw new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                   TraceConstants.MESSAGE_BUNDLE,
                                                                   "OSGI_SERVICE_ERROR",
                                                                   new Object[] { "WsLocationAdmin" },
                                                                   "CWWKX7911E: OSGi service is not available."));
            }

            ReadList.add(normalizePath(wsLocation.resolveString(WsLocationConstants.SYMBOL_INSTALL_DIR)));
            ReadList.add(normalizePath(wsLocation.resolveString(WsLocationConstants.SYMBOL_USER_DIR)));
            ReadList.add(normalizePath(wsLocation.resolveString(WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR)));
        }

        //write lists
        Object writeListConfig = properties.get(WRITE_LIST_CONFIGURATION);
        if (writeListConfig instanceof String[]) {
            WriteList = normalizePaths((String[]) writeListConfig);
        } else {
            //Default is an empty list
            WriteList = Collections.EMPTY_LIST;
        }

        //START DEBUG
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();

            //Log the readList
            if (ReadList.isEmpty()) {
                sb.append("empty");
            } else {
                for (String readLocation : ReadList) {
                    sb.append(readLocation);
                    sb.append("   ");
                }
            }
            Tr.debug(this, tc, "readList: " + sb.toString());

            //Log the writeList
            sb = sb.delete(0, sb.length());
            if (WriteList.isEmpty()) {
                sb.append("empty");
            } else {
                for (String writeLocation : ReadList) {
                    sb.append(writeLocation);
                    sb.append("   ");
                }
            }
            Tr.debug(this, tc, "writeList: " + sb.toString());

        } //END DEBUG

        //Publish values
        EventAdmin eventAdmin = getEventAdmin();

        if (eventAdmin == null) {
            //in this case, we really cannot proceed without the location admin, so throw error.
            throw new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                               TraceConstants.MESSAGE_BUNDLE,
                                                               "OSGI_SERVICE_ERROR",
                                                               new Object[] { "EventAdmin" },
                                                               "CWWKX7911E: OSGi service is not available."));
        }

        publishAttributeChange(eventAdmin, FileServiceMXBean.ATTRIBUTE_NAME_READ_LIST, ReadList.toArray(new String[ReadList.size()]));
        publishAttributeChange(eventAdmin, FileServiceMXBean.ATTRIBUTE_NAME_WRITE_LIST, WriteList.toArray(new String[WriteList.size()]));
    }

    @Reference(name = KEY_LOCATION_ADMIN, service = WsLocationAdmin.class)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    protected WsLocationAdmin getWsLocationAdmin() {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();

        if (locationAdmin == null) {
            logMissingService("WsLocationAdmin");
        }

        return locationAdmin;
    }

    @Reference(name = KEY_EVENT_ADMIN, service = EventAdmin.class)
    protected void setEventAdminService(ServiceReference<EventAdmin> ref) {
        eventAdminRef.setReference(ref);
    }

    protected void unsetEventAdminService(ServiceReference<EventAdmin> ref) {
        eventAdminRef.unsetReference(ref);
    }

    protected EventAdmin getEventAdmin() {
        EventAdmin eventAdmin = eventAdminRef.getService();

        if (eventAdmin == null) {
            logMissingService("EventAdmin");
        }

        return eventAdmin;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getReadList() {
        return ReadList;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getWriteList() {
        return WriteList;
    }

    /** {@inheritDoc} */
    @Override
    public MetaData getMetaData(String path, String requestOptions) {

        if (path == null || EMPTY_PATH_STRING.equals(path)) {
            logInvalidPath(EMPTY_PATH_STRING);
            return null;
        }

        path = normalizePath(path);

        if (!FileServiceUtil.isPathContained(getReadList(), path)
            && !FileServiceUtil.isPathContained(getWriteList(), path)) {
            logAccessDenied(path);
            return null;
        }

        // get the metadata
        //
        File pathFile = new File(path);
        if (!FileUtils.fileExists(pathFile)) { // d92777   
            return null;
        }
        Boolean directory = null;
        Boolean readOnly = null;
        Date lastModified = null;
        Long size = null;

        final boolean requestedAll = requestOptions.contains(FileServiceMXBean.REQUEST_OPTIONS_ALL);

        if (requestedAll || requestOptions.contains(FileServiceMXBean.REQUEST_OPTIONS_IS_DIRECTORY)) {
            directory = FileUtils.fileIsDirectory(pathFile);
        }

        if (requestedAll || requestOptions.contains(FileServiceMXBean.REQUEST_OPTIONS_LAST_MODIFIED)) {
            lastModified = new Date(FileUtils.fileLastModified(pathFile));
        }

        if (requestedAll || requestOptions.contains(FileServiceMXBean.REQUEST_OPTIONS_SIZE)) {
            size = Long.valueOf(FileUtils.fileLength(pathFile));
        }

        if (requestedAll || requestOptions.contains(FileServiceMXBean.REQUEST_OPTIONS_READ_ONLY)) {
            readOnly = !FileUtils.fileCanWrite(pathFile);
        }

        MetaData retData = new MetaData(directory, lastModified, size, readOnly, path);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Returning MetData for path[" + path + "] and options[" + requestOptions + "] with " +
                               "\nsize=" + retData.getSize() +
                               "\ndirectory=" + retData.getDirectory() +
                               "\nreadOnly=" + retData.getReadOnly() +
                               "\nlastModified=" + retData.getLastModified() +
                               "\nfileName=" + retData.getFileName());
        }

        return retData;
    }

    /** {@inheritDoc} */
    @Override
    public MetaData[] getDirectoryEntries(String directory, boolean recursive, String requestOptions) {

        //Filter invalid requests
        //
        if (directory == null || EMPTY_PATH_STRING.equals(directory)) {
            logInvalidPath(EMPTY_PATH_STRING);
            return null;
        }
        directory = normalizePath(directory);
        File dir = new File(directory);
        if (!FileUtils.fileIsDirectory(dir)) {
            logInvalidPath(directory);
            return null;
        }
        if (!FileServiceUtil.isPathContained(getReadList(), directory) && !FileServiceUtil.isPathContained(getWriteList(), directory)) {
            logAccessDenied(directory);
            return null;
        }

        //Get the files/directories for the given directory
        //
        MetaData[] md = getEntries(dir, recursive, requestOptions);

        return md;
    }

    private MetaData[] getEntries(File dir, boolean recurse, String requestOptions) {

        ArrayList<MetaData> v = new ArrayList<MetaData>();
        v = processDir(dir, recurse, requestOptions, v);
        MetaData[] md = new MetaData[v.size()];
        v.toArray(md);

        return md;
    }

    private ArrayList<MetaData> processDir(File dir, boolean recurse, String requestOptions, ArrayList<MetaData> v) {

        File[] entries = FileUtils.listFiles(dir);
        for (int i = 0; i < entries.length; i++) {
            v.add(getMetaData(entries[i].getPath(), requestOptions));
            if (FileUtils.fileIsDirectory(entries[i]) && recurse) {
                v = processDir(entries[i], recurse, requestOptions, v);
            }
        }
        return v;
    }

    /** {@inheritDoc} */
    @Override
    public boolean createArchive(String sourcePath, String targetPath) {

        boolean rc = true;

        // Validate source path
        String normalizedSourcePath = normalizePath(sourcePath);
        if (!FileServiceUtil.isPathContained(getReadList(), normalizedSourcePath)) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "ARCHIVE_CREATE_SOURCE_DENIED_WARNING", sourcePath);
            }
            rc = false;
        }

        // Validate target path
        String normalizedTargetPath = normalizePath(targetPath);
        if (!FileServiceUtil.isPathContained(getWriteList(), normalizedTargetPath)) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "ARCHIVE_CREATE_TARGET_DENIED_WARNING", targetPath);
            }
            rc = false;
        }
        if (rc == false) {
            return rc;
        }

        // Create the archive
        FileServiceUtil ftau = new FileServiceUtil();
        rc = ftau.createArchive(normalizedSourcePath, normalizedTargetPath);

        if (rc == true) {
            if (tc.isInfoEnabled()) {
                Tr.info(tc, "ARCHIVE_CREATE_SUCCESS_INFO", sourcePath, targetPath);
            }
        } else {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "ARCHIVE_CREATE_FAILURE_WARNING", sourcePath, targetPath);
            }
        }
        return rc;
    }

    /** {@inheritDoc} */
    @Override
    public boolean expandArchive(String sourcePath, String targetPath) {

        boolean rc = true;

        // Validate source path
        String normalizedSourcePath = normalizePath(sourcePath);
        if (!(FileServiceUtil.isPathContained(getReadList(), normalizedSourcePath) || FileServiceUtil.isPathContained(getWriteList(), normalizedSourcePath))) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "ARCHIVE_EXPAND_SOURCE_DENIED_WARNING", sourcePath);
            }
            rc = false;
        }

        // Validate target path
        String normalizedTargetPath = normalizePath(targetPath);
        if (!FileServiceUtil.isPathContained(getWriteList(), normalizedTargetPath)) {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "ARCHIVE_EXPAND_TARGET_DENIED_WARNING", targetPath);
            }
            rc = false;
        }
        if (rc == false) {
            return rc;
        }

        // Expand the archive
        FileServiceUtil ftau = new FileServiceUtil();
        rc = ftau.expandArchive(normalizedSourcePath, normalizedTargetPath);

        if (rc == true) {
            if (tc.isInfoEnabled()) {
                Tr.info(tc, "ARCHIVE_EXPAND_SUCCESS_INFO", sourcePath, targetPath);
            }
        } else {
            if (tc.isWarningEnabled()) {
                Tr.warning(tc, "ARCHIVE_EXPAND_FAILURE_WARNING", sourcePath, targetPath);
            }
        }
        return rc;
    }

    private void logAccessDenied(String path) {
        Tr.error(tc, "ACCESS_DENIED_ERROR", path);
    }

    private void logInvalidPath(String path) {
        Tr.error(tc, "PATH_ERROR", path);
    }

    private void logMissingService(String service) {
        Tr.error(tc, "OSGI_SERVICE_ERROR", service);
    }

    @Override
    protected String getParameterName(MBeanOperationInfo op, MBeanParameterInfo param, int sequence) {
        if ("createArchive".equals(op.getName())) {
            return sequence == 0 ? "sourcePath" : "targetPath";

        } else if ("expandArchive".equals(op.getName())) {
            return sequence == 0 ? "sourcePath" : "targetPath";

        } else if ("getDirectoryEntries".equals(op.getName())) {
            switch (sequence) {
                case 0:
                    return "directory";
                case 1:
                    return "recursive";
                case 2:
                    return "requestOptions";
            }

        } else if ("getMetaData".equals(op.getName())) {
            return sequence == 0 ? "path" : "requestOptions";
        }

        return "p" + sequence;
    }

    private void publishAttributeChange(EventAdmin eventAdmin, String attributeName, Object attributeValue) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("operation", "UPDATE");
        props.put("sendStatusEvent", "true");
        props.put("mbeanObjectName", FileServiceMXBean.OBJECT_NAME);
        props.put("mbeanAttributeName", attributeName);
        props.put("mbeanAttributeValue", attributeValue);

        eventAdmin.postEvent(new Event("com/ibm/wsspi/collective/repository/publish/mbean", props));
    }

    /**
     * Handle our completed event.
     */
    @Override
    public void handleEvent(Event statusEvent) {
        //Our filter is based on ObjectName, so we know events pushed here can only come from us.
        String attributeName = (String) statusEvent.getProperty("mbeanAttributeName");

        //Log the update confirmation
        Tr.info(tc, "ATTRIBUTE_UPDATED", attributeName);
    }
}
