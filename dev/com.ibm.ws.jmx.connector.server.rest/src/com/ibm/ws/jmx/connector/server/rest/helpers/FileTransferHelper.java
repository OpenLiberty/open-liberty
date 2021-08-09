/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.connector.server.rest.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URLDecoder;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.filetransfer.FileServiceMXBean;
import com.ibm.websphere.jmx.connector.rest.ConnectorSettings;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.filetransfer.util.FileServiceUtil;
import com.ibm.ws.jmx.connector.server.rest.APIConstants;
import com.ibm.ws.rest.handler.helper.ServletRESTRequestWithParams;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.PathUtils;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

@Component(service = { FileTransferHelper.class },
           configurationPolicy = ConfigurationPolicy.IGNORE,
           immediate = true,
           property = { "service.vendor=IBM" })
public class FileTransferHelper {

    private static final TraceComponent tc = Tr.register(FileTransferHelper.class,
                                                         APIConstants.TRACE_GROUP,
                                                         APIConstants.TRACE_BUNDLE_FILE_TRANSFER);

    //Static constants
    private static final String RANDOM_FILE_ACCESS_READ_MODE = "r";
    private static final String RANDOM_FILE_ACCESS_READ_WRITE_MODE = "rw";
    private static final int FILE_TRANSFER_DEFAULT_BUFFER_SIZE = 8 * 1024;

    public static final String HTTP_ACCEPT = "Accept";
    public static final String HTTP_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HTTP_CONTENT_ENCODING = "Content-Encoding";

    //Mime types
    private static final String FILE_TRANSFER_GZIP_MIME = "application/gzip";
    private static final String FILE_TRANSFER_ZIP_MIME = "application/zip";
    private static final String FILE_TRANSFER_PAX_MIME = "application/pax";
    private static final String FILE_TRANSFER_TAR_MIME = "application/x-tar";
    private static final String FILE_TRANSFER_TEXT_MIME = "text/plain";
    private static final String FILE_TRANSFER_HTML_MIME = "text/html";
    private static final String FILE_TRANSFER_XML_MIME = "text/xml";
    private static final String FILE_TRANSFER_BINARY_MIME = "application/octet-stream";

    //OSGi services
    private final String KEY_LOCATION_ADMIN = "wsLocationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> wsLocationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    private final String KEY_ROUTING_HELPER = "routingHelper";
    private final AtomicServiceReference<FileTransferRoutingHelper> routingHelperRef = new AtomicServiceReference<FileTransferRoutingHelper>(KEY_ROUTING_HELPER);

    //Proxy MBean
    private volatile FileServiceMXBean fileService;

    @Activate
    protected void activate(ComponentContext cc) {
        wsLocationAdminRef.activate(cc);
        routingHelperRef.activate(cc);
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        wsLocationAdminRef.deactivate(cc);
        routingHelperRef.deactivate(cc);
    }

    //Location Admin
    @Reference(name = KEY_LOCATION_ADMIN, service = WsLocationAdmin.class)
    protected void setWsLocationAdminRef(ServiceReference<WsLocationAdmin> ref) {
        wsLocationAdminRef.setReference(ref);
    }

    protected void unsetWsLocationAdminRef(ServiceReference<WsLocationAdmin> ref) {
        wsLocationAdminRef.unsetReference(ref);
    }

    @Reference(service = FileTransferRoutingHelper.class,
               name = KEY_ROUTING_HELPER,
               cardinality = ReferenceCardinality.OPTIONAL,
               policy = ReferencePolicy.DYNAMIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setRoutingHelper(ServiceReference<FileTransferRoutingHelper> routingHelper) {
        routingHelperRef.setReference(routingHelper);
    }

    protected void unsetRoutingHelper(ServiceReference<FileTransferRoutingHelper> routingHelper) {
        routingHelperRef.unsetReference(routingHelper);
    }

    private FileTransferRoutingHelper getRoutingHelper() {
        FileTransferRoutingHelper routingHelper = routingHelperRef.getService();

        if (routingHelper == null) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "OSGI_SERVICE_ERROR",
                                                                           new Object[] { "FileTransferRoutingHelper" },
                                                                           "CWWKX0122E: OSGi service is not available."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }

        return routingHelper;
    }

    /**
     * Get the MIME type for the given path
     */
    private String getMIMEType(String path) {
        String extension = getFileExtension(path);

        if (extension == null) {
            return FILE_TRANSFER_BINARY_MIME;
        }

        if ("zip".equals(extension) || "ear".equals(extension) || "war".equals(extension) || "jar".equals(extension) || "eba".equals(extension)) {
            return FILE_TRANSFER_ZIP_MIME;

        } else if ("pax".equals(extension)) {
            return FILE_TRANSFER_PAX_MIME;

        } else if ("gz".equals(extension) || "gzip".equals(extension)) {

            return FILE_TRANSFER_GZIP_MIME;

        } else if ("tar".equals(extension)) {
            return FILE_TRANSFER_TAR_MIME;

        } else if ("txt".equals(extension) || "log".equals(extension) || "trace".equals(extension) || "properties".equals(extension)) {
            return FILE_TRANSFER_TEXT_MIME;

        } else if ("xml".equals(extension) || "xslt".equals(extension) || "xsl".equals(extension)) {
            return FILE_TRANSFER_XML_MIME;

        } else if ("html".equals(extension) || "htm".equals(extension)) {
            return FILE_TRANSFER_HTML_MIME;

        } else {
            return FILE_TRANSFER_BINARY_MIME;
        }
    }

    /**
     * returns lower cased extension
     *
     * TODO: if we support the path being a folder (to be zipped), then
     * this method will need to be smarter in terms of searching for an "extension"
     */
    private static String getFileExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index == -1) {
            return null;
        }

        return path.substring(index + 1).toLowerCase(Locale.ENGLISH);
    }

    public static String getParentDir(String filePath) {
        String parentDir = filePath.substring(0, filePath.lastIndexOf("/"));

        if (!parentDir.contains("/")) {
            //catch cases where filePath is something like C:/temp.zip or /home.zip
            parentDir = parentDir + "/";
        }

        return parentDir;
    }

    public static String removeTrailingSlash(String file) {
        if (file.charAt(file.length() - 1) == '/') {
            file = file.substring(0, file.length() - 1);
        }
        return file;
    }

    public static String appendFilename(String directory, String filename) {
        if (directory.endsWith("/")) {
            //There's already a slash separating the two, so just append
            return directory + filename;
        }

        return directory + "/" + filename;
    }

    @FFDCIgnore({ UnsupportedEncodingException.class })
    public static String decodeFilePath(String filePath) {
        //Decode the pathInfo
        try {
            return URLDecoder.decode(filePath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        }
    }

    private String processAndValidateFilePath(String filePath, boolean readOnly) {
        final String processedPath = getWsLocationAdmin().resolveString(decodeFilePath(filePath));

        if (!checkAccess(processedPath, readOnly)) {
            //Return an exception
            Object[] params = new String[] { processedPath };
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "SERVER_ACCESS_DENIED_ERROR",
                                                                           params,
                                                                           "CWWKX0121E: Access denied to the " + processedPath + " path."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_BAD_REQUEST);
        }

        return processedPath;
    }

    public static boolean deleteLocalFile(String filePath) {
        //Get a file handle
        final File deleteFile = new File(filePath);

        //If the file is not there it's a no-op
        if (FileUtils.fileExists(deleteFile)) {
            //Delete file
            return FileUtils.fileDelete(deleteFile);
        }
        return true;
    }

    /**
     * Delete the local file and it's parent directory (only if empty)
     *
     * @param filePath
     * @return Indicates whether the local file and the parent directory were deleted
     */
    public static boolean deleteLocalFileAndParentDir(String filePath) {
        return deleteLocalFile(filePath) && deleteLocalFile(getParentDir(filePath));
    }

    public static void recursiveDelete(final File dir) {
        if (FileUtils.fileExists(dir)) {
            //file exists..
            for (File f : FileUtils.listFiles(dir)) {
                //handling each file under that directory..
                if (FileUtils.fileIsDirectory(f)) {
                    recursiveDelete(f);
                } else {
                    FileUtils.fileDelete(f);
                }
            }
            //Delete the (now empty) directory
            FileUtils.fileDelete(dir);
        }
    }

    /**
     * Check if the specified path is open for read/write access
     */
    public boolean checkAccess(String path, boolean readOnly) {
        final FileServiceMXBean fileService = getFileService();
        if (readOnly) {
            //we can read from both the read and write list
            return (FileServiceUtil.isPathContained(fileService.getReadList(), path) || FileServiceUtil.isPathContained(fileService.getWriteList(), path));
        } else {
            //we can write only to the write list
            return FileServiceUtil.isPathContained(fileService.getWriteList(), path);
        }
    }

    private boolean expandArchive(String sourcePath, String targetPath) {
        FileServiceMXBean fileService = getFileService();
        return fileService.expandArchive(sourcePath, targetPath);
    }

    public static String getTempArchiveName(RESTRequest request, String path) {
        String archiveName = getTempArchiveName(path);
        if (archiveName.endsWith("_original")) {
            //The requested target directory did not have an extension, so append closest match to ensure our temp archive has an extension
            String contentType = request.getContentType();
            if (contentType != null && contentType.contains(FILE_TRANSFER_PAX_MIME)) {
                archiveName = archiveName + ".pax";
            } else {
                //default is zip, since zip/jar/war/ear are using the same MIME
                archiveName = archiveName + ".zip";
            }
        }

        return archiveName;
    }

    protected static String getTempArchiveName(String path) {
        int indexDot = path.lastIndexOf(".");
        int indexFSlash = path.lastIndexOf("/");
        int indexBSlash = path.lastIndexOf("\\");

        if (indexDot == -1 || indexBSlash > indexDot || indexFSlash > indexDot) {
            return path + "_original";
        } else {
            return path.substring(0, indexDot) + "_original" + path.substring(indexDot);
        }
    }

    public void writeResponseFromFile(String processedPath, RESTRequest request, RESTResponse response, boolean legacyFileTransfer) {
        writeResponseFromFile(processedPath, 0, -1, request, response, legacyFileTransfer);
    }

    @FFDCIgnore({ FileNotFoundException.class })
    protected void writeResponseFromFile(String processedPath, long startOffset, long endOffset, RESTRequest request, RESTResponse response,
                                         boolean legacyFileTransfer) {
        OutputStream outStream = null;
        RandomAccessFile downloadFile = null;
        try {

            downloadFile = new RandomAccessFile(processedPath, RANDOM_FILE_ACCESS_READ_MODE);

            //Grab the outgoing stream
            outStream = response.getOutputStream();

            if (legacyFileTransfer) {
                //V2 (legacy) file transfer was always doing gzip and setting the following content type
                response.setContentType(FILE_TRANSFER_GZIP_MIME);
                outStream = new GZIPOutputStream(outStream, FILE_TRANSFER_DEFAULT_BUFFER_SIZE);
            } else {
                //Get the value of Accept-Encoding and Accept headers
                String acceptEncoding = request.getHeader(HTTP_ACCEPT_ENCODING);
                String accept = request.getHeader(HTTP_ACCEPT);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Accept-Encoding: " + acceptEncoding + " | Accept: " + accept);
                }

                //If the caller specified a particular Content-Type, then we set it. Otherwise figure out the best MIME type.
                if (accept != null && !accept.contains("*/*") && !accept.contains(",")) {
                    response.setContentType(accept);
                } else {
                    response.setContentType(getMIMEType(processedPath));
                }

                //Use GZIP if we're compressing
                if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
                    response.setResponseHeader(HTTP_CONTENT_ENCODING, "gzip");
                    outStream = new GZIPOutputStream(outStream, FILE_TRANSFER_DEFAULT_BUFFER_SIZE);
                }
            }

            if (startOffset < 0) {
                startOffset = 0;
            }

            downloadFile.seek(startOffset);

            //Consider: improve on this byte transfer by exploring lower-level channel access to the outgoing stream. Look at what the webcontainer can provide
            //Transfer bytes to output file
            byte[] buf = new byte[FILE_TRANSFER_DEFAULT_BUFFER_SIZE];
            int bytesRead;
            if (endOffset <= -1) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Transfer until the end of file is reached.");
                }
                while ((bytesRead = downloadFile.read(buf)) > 0) {
                    outStream.write(buf, 0, bytesRead);
                }
            } else {
                int bytesToRead = startOffset > endOffset ? 0 : (int) (endOffset - startOffset) + 1;
                int length = bytesToRead > FILE_TRANSFER_DEFAULT_BUFFER_SIZE ? FILE_TRANSFER_DEFAULT_BUFFER_SIZE : bytesToRead;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "initial : bytesToRead=" + bytesToRead + " : length=" + length);
                }

                while ((bytesRead = downloadFile.read(buf, 0, length)) > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "bytesToRead=" + bytesToRead + " : length=" + length + " : bytesRead=" + bytesRead);
                    }
                    outStream.write(buf, 0, bytesRead);
                    bytesToRead -= bytesRead;
                    if (bytesToRead <= 0) {
                        break;
                    }
                    if (bytesToRead < FILE_TRANSFER_DEFAULT_BUFFER_SIZE) {
                        length = bytesToRead;
                    }
                }
            }
            outStream.flush();

        } catch (FileNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);
        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        } finally {
            FileUtils.tryToClose(outStream);
            FileUtils.tryToClose(downloadFile);
        }
    }

    @FFDCIgnore({ FileNotFoundException.class })
    public void readRequestIntoFile(String processedPath, RESTRequest request, boolean legacyFileTransfer) {
        InputStream is = null;
        RandomAccessFile uploadFile = null;
        boolean nodeDeployment = false;

        if (request instanceof ServletRESTRequestWithParams) {
            ServletRESTRequestWithParams req = (ServletRESTRequestWithParams) request;
            String deployType = req.getParam("deployService");
            if (deployType != null && "node.js".equals(deployType.trim())) {
                nodeDeployment = true;
            }
        }
        try {
            //Get the incoming stream
            String actionHeader = request.getHeader(ConnectorSettings.POST_TRANSFER_ACTION);

            if (ConnectorSettings.POST_TRANSFER_ACTION_FIND_SERVER_NAME.equals(actionHeader) || nodeDeployment) {
                is = request.getPart("file");
                if (is == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "******************* Error InputStream is NULL **************************");
                    }
                }
            } else {
                is = request.getInputStream();
            }

            //In V2 (legacy), the file was always GZIPed.  In V3+, jax-rs automatically ungzips the file for us
            if (legacyFileTransfer) {
                is = new GZIPInputStream(is, FILE_TRANSFER_DEFAULT_BUFFER_SIZE);
            }

            // check the folder(s)
            File parentFolder = new File(getParentDir(processedPath));
            if (!FileUtils.fileExists(parentFolder)) {
                FileUtils.fileMkDirs(parentFolder);
            }

            //Get access to file
            uploadFile = new RandomAccessFile(processedPath, RANDOM_FILE_ACCESS_READ_WRITE_MODE);

            //Transfer bytes to output file
            byte[] buf = new byte[FILE_TRANSFER_DEFAULT_BUFFER_SIZE];
            int bytesRead;
            long totalBytesRead = 0;
            while ((bytesRead = is.read(buf)) > 0) {
                uploadFile.write(buf, 0, bytesRead);
                totalBytesRead += bytesRead;
            }
            uploadFile.setLength(totalBytesRead);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Size of new file [" + processedPath + "] = " + uploadFile.length());
            }

        } catch (FileNotFoundException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_BAD_REQUEST);

        } catch (IOException e) {
            throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);

        } finally {
            FileUtils.tryToClose(is);
            FileUtils.tryToClose(uploadFile);
        }
    }

    public WsLocationAdmin getWsLocationAdmin() {
        WsLocationAdmin wsLocationAdmin = wsLocationAdminRef.getService();

        if (wsLocationAdmin == null) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "OSGI_SERVICE_ERROR",
                                                                           new Object[] { "WsLocationAdmin" },
                                                                           "CWWKX0122E: OSGi service is not available."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
        }
        return wsLocationAdmin;
    }

    //File Service MXBean
    private synchronized FileServiceMXBean getFileService() {
        if (fileService == null) {
            try {
                fileService = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), new ObjectName(FileServiceMXBean.OBJECT_NAME), FileServiceMXBean.class);
            } catch (MalformedObjectNameException e) {
                throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            } catch (NullPointerException e) {
                throw ErrorHelper.createRESTHandlerJsonException(e, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }
        }

        return fileService;
    }

    /**
     * Return a unique directory within the workarea directory
     */
    public String getWritableLocation() {
        String writableLocation = getWsLocationAdmin().resolveString("${server.output.dir}/workarea/" + UUID.randomUUID() + "/");
        if (writableLocation == null) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "NO_WRITE_LOCATION",
                                                                           null,
                                                                           "CWWKX0128E: There are no configured writable locations on the routing server."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_BAD_REQUEST);
        }

        return writableLocation;
    }

    /**
     * Get the filename from a given path
     */
    public static String getFilename(String path) {
        //The filename is after the last slash
        final int index = path != null ? path.lastIndexOf("/") : -1;
        if (index == -1) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(FileTransferHelper.class,
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "PATH_NOT_VALID",
                                                                           new String[] { path },
                                                                           "CWWKX0127E: The path " + path + " is not valid."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_BAD_REQUEST);
        }

        return path.substring(index + 1);
    }

    public static String processRoutingPathLight(String originalPath) {
        //First we decode the path
        originalPath = decodeFilePath(originalPath);

        //Then we do a light normalization for any dots or slashes
        return PathUtils.normalize(originalPath);
    }

    public String processRoutingPath(String originalPath, String targetHost, String targetServer, String targetUserDir) {
        //Do a light processing
        String path = processRoutingPathLight(originalPath);

        //Last step is to resolve symbols.
        //NOTE: we cannot call WsLocationAdmin.resolveString here because the symbol needs to be resolved in terms of the
        //TARGET location, not THIS (Controller) location.

        //quick exit if no symbols are present
        if (path == null || path.isEmpty() || !path.contains("$")) {
            return path;
        }

        //If path ends with a symbol we must add a trailing slash so that we can properly
        //replace with the symbol's value, which always has a trailing slash
        if (path.charAt(path.length() - 1) == '}') {
            path = path + '/';
        }

        ServerPath symbolToResolve = null;
        if (path.contains(ServerPath.INSTALL_DIR.getSymbol())) {
            symbolToResolve = ServerPath.INSTALL_DIR;

        } else if (path.contains(ServerPath.USER_DIR.getSymbol())) {
            symbolToResolve = ServerPath.USER_DIR;

        } else if (path.contains(ServerPath.OUTPUT_DIR.getSymbol())) {
            symbolToResolve = ServerPath.OUTPUT_DIR;

        } else if (path.contains(ServerPath.CONFIG_DIR.getSymbol())) {
            symbolToResolve = ServerPath.CONFIG_DIR;

        } else if (path.contains(ServerPath.SHARED_CONFIG_DIR.getSymbol())) {
            symbolToResolve = ServerPath.SHARED_CONFIG_DIR;

        } else if (path.contains(ServerPath.SHARED_APPS_DIR.getSymbol())) {
            symbolToResolve = ServerPath.SHARED_APPS_DIR;

        } else if (path.contains(ServerPath.SHARED_RESC_DIR.getSymbol())) {
            symbolToResolve = ServerPath.SHARED_RESC_DIR;
        } else {
            //Unknown symbol found. Return null to let the caller handle it.
            return null;
        }

        return getRoutingHelper().processSymbolicRoutingPath(path, targetHost, targetServer, targetUserDir, symbolToResolve);
    }

    //Note: for delete operation we don't need to know if it's from legacy file transfer (V2) or not, because gzip is not involved here
    public void deleteInternal(String filePath, boolean recursive) {
        //Process and validate path
        final String processedPath = processAndValidateFilePath(filePath, false);

        boolean deleted;
        if (recursive) {
            File fileToDelete = new File(processedPath);
            if (FileUtils.fileIsDirectory(fileToDelete)) {
                recursiveDelete(fileToDelete);
            } else {
                FileUtils.fileDelete(fileToDelete);
            }
            deleted = FileUtils.fileExists(fileToDelete) == false;
        } else {
            deleted = deleteLocalFile(processedPath);
        }

        if (!deleted) {
            IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                           APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                           "DELETE_REQUEST_ERROR",
                                                                           null,
                                                                           "CWWKX0126E: Delete request for file " + processedPath + " could not be completed."));
            throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_BAD_REQUEST);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
            Tr.info(tc, "DELETE_REQUEST_COMPLETE_INFO", processedPath);
        }
    }

    public void uploadInternal(RESTRequest request, String filePath, boolean expansion, boolean legacyFileTransfer) {
        //Process and validate path
        final String processedPath = processAndValidateFilePath(filePath, false);

        if (expansion) {
            //We need to change our temp filename slightly so that we can have a folder with the same name under the same dir
            String archivePath = FileTransferHelper.getTempArchiveName(request, processedPath);
            archivePath = appendFilename(processedPath, getFilename(archivePath));

            //Read contents of stream into file
            readRequestIntoFile(archivePath, request, legacyFileTransfer);

            //Expand the archive using FilServiceMXBean
            if (!expandArchive(archivePath, processedPath)) {
                //something went wrong..
                IOException ioe = new IOException(TraceNLS.getFormattedMessage(this.getClass(),
                                                                               APIConstants.TRACE_BUNDLE_FILE_TRANSFER,
                                                                               "UPLOAD_EXPANSION_ERROR",
                                                                               new Object[] { processedPath },
                                                                               "CWWKX0129E: Uploaded archive could not be expanded."));
                throw ErrorHelper.createRESTHandlerJsonException(ioe, null, APIConstants.STATUS_INTERNAL_SERVER_ERROR);
            }

            //Delete temp archive
            if (!deleteLocalFile(archivePath)) {
                //if we coudn't delete the temporary file, log it but don't throw error, since the overall operation worked.
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Could not delete temporary file: " + archivePath);
                }
            }

        } else {
            //Not doing archive expansion, so just read contents of stream into file
            readRequestIntoFile(processedPath, request, legacyFileTransfer);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
            Tr.info(tc, "UPLOAD_REQUEST_COMPLETE_INFO", processedPath);
        }

    }

    public void downloadInternal(RESTRequest request, RESTResponse response, String filePath, boolean legacyFileTransfer) {
        downloadInternal(request, response, filePath, 0, -1, legacyFileTransfer);
    }

    public void downloadInternal(RESTRequest request, RESTResponse response, String filePath, long startOffset, long endOffset, boolean legacyFileTransfer) {
        //Process and validate path
        final String processedPath = processAndValidateFilePath(filePath, true);

        //Write contents of file into stream
        writeResponseFromFile(processedPath, startOffset, endOffset, request, response, legacyFileTransfer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isInfoEnabled()) {
            Tr.info(tc, "DOWNLOAD_REQUEST_COMPLETE_INFO", processedPath);
        }
    }

    public void routedDeleteInternal(RESTRequest request, String filePath, boolean recursiveDelete) {
        getRoutingHelper().routedDeleteInternal(this, request, filePath, recursiveDelete);
    }

    public void routedUploadInternal(RESTRequest request, String filePath, boolean expansion, boolean legacyFileTransfer) {
        getRoutingHelper().routedUploadInternal(this, request, filePath, expansion, legacyFileTransfer);
    }

    public void routedDownloadInternal(RESTRequest request, RESTResponse response, String filePath, boolean legacyFileTransfer) {
        getRoutingHelper().routedDownloadInternal(this, request, response, filePath, legacyFileTransfer);
    }
}
