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
package com.ibm.websphere.filetransfer;

import java.beans.ConstructorProperties;
import java.util.Date;
import java.util.List;

/**
 * This MBean provides file service operations on the host system on which
 * the server resides.
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 * <p>
 * All paths are implicitly remote, as the operations performed by this MBean
 * occur on the host which the server resides. All paths are required to be
 * absolute, either explicitly or rooted with a WebSphere variable such as
 * ${wlp.install.dir}.
 * 
 * @ibm-api
 */
public interface FileServiceMXBean {

    /**
     * A String representing the {@link javax.management.ObjectName} that this MXBean maps to.
     */
    String OBJECT_NAME = "WebSphere:feature=restConnector,type=FileService,name=FileService";

    /**
     * Option indicating all available keys should be used for the query.
     */
    String REQUEST_OPTIONS_ALL = "a";

    /**
     * Option indicating that "isDirectory" key should be used for the query.
     */
    String REQUEST_OPTIONS_IS_DIRECTORY = "d";

    /**
     * Option indicating that "isReadOnly" key should be used for the query.
     */
    String REQUEST_OPTIONS_READ_ONLY = "r";

    /**
     * Option indicating that "size" key should be used for the query.
     */
    String REQUEST_OPTIONS_SIZE = "s";

    /**
     * Option indicating that "lastModified" key should be used for the query.
     */
    String REQUEST_OPTIONS_LAST_MODIFIED = "t";

    /**
     * The name of the attribute representing the list of read locations.
     */
    String ATTRIBUTE_NAME_READ_LIST = "ReadList";

    /**
     * The name of the attribute representing the list of write locations.
     */
    String ATTRIBUTE_NAME_WRITE_LIST = "WriteList";

    /**
     * Get the configured list of read-accessible locations on the host where
     * this server resides. This list is configurable for each server by
     * modifying the server.xml. Each directory to allow for read access can
     * be specified to the &lt;remoteFileAccess&gt; configuration element
     * 'readDir' attribute.
     * <p>
     * For example:
     * <br>
     * <pre>
     * &lt;remoteFileAccess&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;readDir&gt;$ server.output.dir}/payloads&lt;/readDir&gt;
     * &lt;/remoteFileAccess&gt;
     * </pre>
     * <p>
     * The default is an empty list.
     * 
     * @return a list of Strings containing the absolute paths which are read-accessible.
     */
    List<String> getReadList();

    /**
     * Get the configured list of write-accessible locations on the host where
     * this server resides. This list is configurable for each server by
     * modifying the server.xml. Each direcoty to allow for write access can
     * be specified to the &lt;remoteFileAccess&gt; configuration element
     * 'writeDir' attribute.
     * <p>
     * For example:
     * <br>
     * <pre>
     * &lt;remoteFileAccess&gt;
     * &nbsp;&nbsp;&nbsp;&nbsp;&lt;writeDir&gt;${server.output.dir}/target&lt;/writeDir&gt;
     * &lt;/remoteFileAccess&gt;
     * </pre>
     * <p>
     * The default is a list containing 3 entries: ${wlp.install.dir},
     * ${wlp.user.dir} and ${server.output.dir}.
     * 
     * @return a list of Strings containing the absolute paths which are write-accessible.
     */
    List<String> getWriteList();

    /**
     * Get the metadata pertaining to the specified path. The result object
     * will contain the requested metadata subset as indicated by the
     * requestOptions String value, which should be constructed as a
     * concatenation of the desired values.
     * <p>
     * For example: to obtain a request that provides only the "size" and
     * "lastModified" metadata values, the user can pass in "st" as the
     * requestOptions field.
     * 
     * @param path the absolute path of the file or directory for which to retrieve the metadata
     * @param requestOptions a String representing the concatenation of the requested metadata keys. See REQUEST_OPTIONS_* fields.
     * @return a CompositeData containing the requested metadata
     */
    MetaData getMetaData(String path, String requestOptions);

    /**
     * List the directory/files for the given path.
     * 
     * @param directory the absolute path of the directory to list
     * @param recursive a boolean to specify if the search should be done to all descendant paths
     * @param requestOptions a String representing the concatenation of the requested metadata keys
     * @return an array of CompositeData, representing the requested metadata for each entry
     */
    MetaData[] getDirectoryEntries(String directory, boolean recursive, String requestOptions);

    /**
     * Create an archive of the given sourcePath.
     * <p>
     * This operation occurs on the file system on which this server resides.
     * Therefore the sourcePath and targetPath are paths on the server's host
     * system.
     * 
     * @param sourcePath the absolute path of the entity to archive
     * @param targetPath the absolute path to where resulting archive is to be stored
     * @return true if archive was successfully created, false otherwise
     */
    boolean createArchive(String sourcePath, String targetPath);

    /**
     * Expand the archive at the given sourcePath.
     * <p>
     * This operation occurs on the file system on which this server resides.
     * Therefore the sourcePath and targetPath are paths on the server's host
     * system.
     * 
     * @param sourcePath the absolute path of the archive to be expanded
     * @param targetPath the absolute path to where archive is to be expanded
     * @return true if archive was successfully expanded, false otherwise
     */
    boolean expandArchive(String sourcePath, String targetPath);

    /**
     * Return type for the getMetaData and getDirectoryEntries methods.
     * 
     * The JMX framework will convert this to a CompositeData object on the client side.
     */
    public class MetaData {

        private final Boolean directory;
        private final Boolean readOnly;
        private final Date lastModified;
        private final Long size;
        private final String fileName;

        @ConstructorProperties({ "directory", "lastModified", "size", "readOnly", "fileName" })
        public MetaData(Boolean directory, Date lastModified, Long size, Boolean readOnly, String fileName) {
            this.directory = directory;
            this.lastModified = (lastModified == null) ? null : (Date) lastModified.clone();
            this.size = size;
            this.readOnly = readOnly;
            this.fileName = fileName;
        }

        /**
         * Boolean value indicating whether or not the entity is a directory.
         * 
         * @return {@code true} if the entity is a directory, {@code false} otherwise.
         */
        public Boolean getDirectory() {
            return this.directory;
        }

        /**
         * @return Date object representing the time the entity was last modified
         */
        public Date getLastModified() {
            return (this.lastModified == null) ? null : (Date) this.lastModified.clone();
        }

        /**
         * @return Long object indicating the size of the entity.
         */
        public Long getSize() {
            return this.size;
        }

        /**
         * Boolean value indicating whether or not the entity is read-only.
         * 
         * @return {@code true} if the entity is read-only, {@code false} otherwise.
         */
        public Boolean getReadOnly() {
            return this.readOnly;
        }

        /**
         * @return String value indicating the entity's absolute path.
         */
        public String getFileName() {
            return this.fileName;
        }

    }

}
