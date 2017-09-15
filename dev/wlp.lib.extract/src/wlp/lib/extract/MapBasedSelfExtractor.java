/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package wlp.lib.extract;

import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is a loosely-typed interface onto the SelfExtractor, it allows the installation from a
 * program that is not able to make direct java calls. The idea is the calling code would create
 * a URLClassLoader to the install jar and then instanciate this class. Once instanciated it
 * initiates the install via the use of the put and get methods on the map. All other methods
 * throw UnsupportedOperationException.
 *
 * <p>This class name should not be directly relied on. The install jar will have a Map-Based-Self-Extractor header
 * which shall be used to identify the class name.</p>
 *
 * <p>This class consists of several keys which can be put and got. The keys that can be put are:
 * <ul>
 * <li><i>install.dir</i> - A java.io.File object that indicates where to install to.</li>
 * <li><i>license.accept</i> - A java.lang.Boolean representing true. This is required in order to install.
 * <li><i>install.monitor</i> - A java.util.List that will be called by the extractor to help monitor progress. The
 * extractor will call add with the String file name for each file installed as it is installed. Non-Strings may be passed
 * into the add method, these objects indicate a piece of work has occurred, but there is no user visible status for that
 * piece of work. If the add method returns false then the install will be aborted and rolled back.</li>
 * <li><i>download.file.monitor</i> - A java.util.List that will be called by the extractor to help monitor progress. The
 * extractor will call add with a Map&lt;String,Object&gt; before downloading each external dependency, if any are present. The Map
 * contains the following keys: 1. <b>download.url</b> - a java.net.URL object representing where the file is being downloaded
 * from. 2. <b>download.target.file</b> - points to a File object representing where the file is being downloaded to.</li>
 * <li><i>download.monitor</i> - A java.util.List that will be called by the extractor to help monitor progress. The
 * extractor will call add with an Integer that is the number of bytes downloaded by each read operation (the buffer is 4kB).</li>
 * <li><i>install.version</i> - The value must be either a java.lang.Integer or a List of java.lang.Integers. It contains
 * all the install versions the caller understands. If single java.lang.Integer from the list is returned this is the varient
 * of the install protocol that should be used. If null is returned no compatable version is understood. This installer
 * understands a version of one only.</li>
 * <li><i>download.deps</i> - A java.lang.Boolean that specifies whether to download any external dependencies declared
 * in the archive. This value defaults to true.</li>
 * <li><i>target.user.directory</i> - A java.io.File object that indicates which user directory to install sample content into.
 * If unspecified, the default behaviour of SelfExtractor is used, which checks for server.env files, WLP_USER_DIR environment
 * variables, and failing that, falls back to a subdirectory called 'usr' in the install directory.</li>
 * </ul>
 * </p>
 *
 * <p>Several values can be got from this map. Calling get on some of these is unnecessary but advised.
 * <ul>
 * <li><i>installer.init.code</i> - Returns a java.lang.Integer indicating the error code for creating the installer. If this
 * is zero then the installer was built correctly. If it is non-zero an error occurred. It is not necessary to get this
 * value, but if the build failed then the call that initiates the install will not do anything.</li>
 * <li><i>installer.init.error.message</i> - Returns a friendly translated message that indicates why the installer failed to build.</li>
 * <li><i>install.monitor.size</i> - Returns a java.lang.Integer indicating how many files are to be installed. This influences how many
 * times add will be called on the List monitor (if one is provided).</li>
 * <li><i>download.monitor.size</i> - Returns a java.lang.Integer indicating the aggregate size of all external dependencies for this
 * archive, in bytes.</li>
 * <li><i>product.name</i> - The String name of the product being installed by this installer.</li>
 * <li><i>license.name</i> - The String name of the license for the product being installed.</li>
 * <li><i>license.agreement</i> - A java.io.Reader that can be used to read the License Agreement.</li>
 * <li><i>license.info</i> - A java.io.Reader that can be used to read the License Information.</li>
 * <li><i>install.code</i> - Calling get on this is special. If the installer was built correctly and an <i>install.dir</i> has
 * been provided then it will initiate the install. Once the install has completed it will return a java.lang.Integer with
 * a code indicating how it functioned. If it returns zero everything went well. If it returned null then the call was
 * invalid. If it returns a non zero return value an error occurred during install.</li>
 * <li><i>install.error.message</i> - Returns a friendly translated message that indicates why the installer failed to install.</li>
 * <li><i>license.present</i> - Returns a java.lang.Boolean object which represents whether the archive contains a license. This is
 * deemed to be true if the archive has both License-Agreement and License-Information headers in the manifest.</li>
 * <li><i>has.external.deps</i> - Returns a java.lang.Boolean which is true if the archive declares external dependencies (by the
 * presence of an externaldependencies.xml file in the root of the archive).
 * <li><i>list.external.deps</i> - Returns a List&lt;Map&lt;String,Object&gt;&gt;, where each Map represents one external dependency declared
 * by the archive, and contains the following keys: 1. <b>download.url</b> - points to a
 * java.net.URL object representing where the file is being downloaded from. 2. <b>download.target</b> - a java.lang.String object
 * that contains the path to download the file to on disk, relative to the user directory.</li>
 * <li><i>external.deps.description</i> - A java.lang.String that describes the external dependencies, as provided in the
 * externaldependencies.xml file.</li>
 * <li><i>archive.content.type</i> -A java.lang.String that returns the content type of the archive, i.e. the Archive-Content-Type header in
 * the archive manifest. For example, in a sample, this is "sample".</li>
 * </ul>
 * </p>
 */
public class MapBasedSelfExtractor implements Map {

    private final SelfExtractor extract;
    public static final String INSTALL_BUILD_CODE = "installer.init.code";
    public static final String INSTALL_BUILD_ERROR_MESSAGE = "installer.init.error.message";
    public static final String INSTALL_CODE = "install.code";
    public static final String INSTALL_ERROR_MESSAGE = "install.error.message";
    public static final String INSTALL_DIR = "install.dir";
    public static final String INSTALL_MONITOR = "install.monitor";
    public static final String INSTALL_MONITOR_SIZE = "install.monitor.size";
    public static final String PRODUCT_NAME = "product.name";
    public static final String LICENSE_NAME = "license.name";
    public static final String LICENSE_AGREEMENT = "license.agreement";
    public static final String LICENSE_ACCEPT = "license.accept";
    public static final String LICENSE_INFO = "license.info";
    public static final String INSTALL_VERSION = "install.version";
    public static final String HAS_EXTERNAL_DEPS = "has.external.deps";
    public static final String LIST_EXTERNAL_DEPS = "list.external.deps";
    public static final String EXTERNAL_DEPS_DESCRIPTION = "external.deps.description";
    public static final String ARCHIVE_CONTENT_TYPE = "archive.content.type";
    public static final String PROVIDED_FEATURES = "provided.features";
    public static final String TARGET_USER_DIR = "target.user.directory";
    public static final String DOWNLOAD_DEPS = "download.deps";
    public static final String DOWNLOAD_MONITOR = "download.monitor";
    public static final String DOWNLOAD_MONITOR_SIZE = "download.monitor.size";
    public static final String DOWNLOAD_FILE_MONITOR = "download.file.monitor";
    public static final String LICENSE_PRESENT = "license.present";
    public static final Integer INSTALL_VERSION_ONE = new Integer(1);
    public static final String PRODUCT_INSTALL_TYPE = "product.install.type";
    public static final String ALLOW_NON_EMPTY_INSTALL_DIR = "allow.non.empty.install.directory";
    public static final String CLOSE = "close";

    private final Map data = new HashMap();
    private File installDir;
    private final ListBasedExtractProgress monitor = new ListBasedExtractProgress();
    private Boolean licenseAccepted = Boolean.FALSE;
    private Boolean downloadDependencies = Boolean.FALSE;

    private static class ListBasedExtractProgress implements ExtractProgress {
        private List extractedFiles;
        private List downloadedFiles;
        private List downloadSizeMonitor;
        private boolean canceled;

        public void downloadingFile(URL sourceUrl, File targetFile) {
            if (downloadedFiles != null) {
                Map downloadInformation = new HashMap();
                downloadInformation.put(SelfExtractUtils.DOWNLOAD_URL, sourceUrl);
                downloadInformation.put(SelfExtractUtils.DOWNLOAD_TARGET_FILE, targetFile);
                canceled = !!!downloadedFiles.add(downloadInformation);
            }
        }

        public void dataDownloaded(int numBytes) {
            if (downloadSizeMonitor != null) {
                downloadSizeMonitor.add(Integer.valueOf(numBytes));
            }
        }

        public void extractedFile(String f) {
            if (extractedFiles != null) {
                canceled = !!!extractedFiles.add(f);
            }
        }

        public void setFilesToExtract(int count) {}

        public void commandRun(List args) {}

        public void commandsToRun(int count) {}

        public boolean isCanceled() {
            return canceled;
        }

        public void skippedFile() {
            if (extractedFiles != null) {
                canceled = !!!extractedFiles.add(INSTALL_VERSION_ONE);
            }
        }
    };

    public MapBasedSelfExtractor() {
        ReturnCode rc = SelfExtractor.buildInstance();
        data.put(INSTALL_BUILD_CODE, new Integer(rc.getCode()));
        data.put(INSTALL_BUILD_ERROR_MESSAGE, rc.getErrorMessage());
        extract = SelfExtractor.getInstance();
        if (extract != null) {
            data.put(INSTALL_MONITOR_SIZE, new Integer(extract.getSize()));
            if (extract.hasLicense()) {
                data.put(PRODUCT_NAME, extract.getProgramName());
                data.put(LICENSE_NAME, extract.getLicenseName());
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#clear()
     */
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object arg0) {
        return data.containsKey(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    public boolean containsValue(Object arg0) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#entrySet()
     */
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#get(java.lang.Object)
     */
    public Object get(Object arg0) {
        if (INSTALL_CODE.equals(arg0) && extract != null && installDir != null && (!extract.hasLicense() || licenseAccepted.booleanValue())) {
            ReturnCode rc = extract.validate(installDir);
            if (rc == ReturnCode.OK) {
                rc = extract.extract(installDir, monitor);
            }
            data.put(INSTALL_CODE, new Integer(rc.getCode()));
            data.put(INSTALL_ERROR_MESSAGE, rc.getErrorMessage());
        } else if (LICENSE_AGREEMENT.equals(arg0)) {
            try {
                return extract.hasLicense() ? new InputStreamReader(extract.getLicenseAgreement(), "UTF-16") : null;
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        } else if (LICENSE_INFO.equals(arg0)) {
            try {
                return extract.hasLicense() ? new InputStreamReader(extract.getLicenseInformation(), "UTF-16") : null;
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        } else if (LICENSE_PRESENT.equals(arg0)) {
            return Boolean.valueOf(extract.hasLicense());
        } else if (HAS_EXTERNAL_DEPS.equals(arg0)) {
            return Boolean.valueOf(extract.hasExternalDepsFile());
        } else if (LIST_EXTERNAL_DEPS.equals(arg0)) {
            try {
                return SelfExtractUtils.convertDependenciesListToMapsList(extract.getExternalDependencies());
            } catch (Exception e) {
                return null;
            }
        } else if (ARCHIVE_CONTENT_TYPE.equals(arg0)) {
            return extract.getArchiveContentType();
        } else if (PROVIDED_FEATURES.equals(arg0)) {
            return extract.getProvidedFeatures();
        } else if (EXTERNAL_DEPS_DESCRIPTION.equals(arg0)) {
            try {
                return extract.getExternalDependencies().getDescription();
            } catch (Exception e) {
                return null;
            }
        } else if (DOWNLOAD_MONITOR_SIZE.equals(arg0)) {
            return new Integer(extract.getTotalDepsSize());
        } else if (CLOSE.equals(arg0)) {
            return close();
        }
        return data.get(arg0);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#isEmpty()
     */
    public boolean isEmpty() {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#keySet()
     */
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public Object put(Object arg0, Object arg1) {
        Object old = null;
        if (INSTALL_DIR.equals(arg0)) {
            if (arg1 instanceof File) {
                old = installDir;
                installDir = (File) arg1;

            } else {
                throw new IllegalArgumentException();
            }
        } else if (INSTALL_MONITOR.equals(arg0)) {
            if (arg1 instanceof List) {
                if (monitor.extractedFiles != null) {
                    old = monitor.extractedFiles;
                }
                monitor.extractedFiles = (List) arg1;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_FILE_MONITOR.equals(arg0)) {
            if (arg1 instanceof List) {
                if (monitor.downloadedFiles != null) {
                    old = monitor.downloadedFiles;
                }
                monitor.downloadedFiles = (List) arg1;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_MONITOR.equals(arg0)) {
            if (arg1 instanceof List) {
                if (monitor.downloadSizeMonitor != null) {
                    old = monitor.downloadSizeMonitor;
                }
                monitor.downloadSizeMonitor = (List) arg1;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (INSTALL_VERSION.equals(arg0)) {
            if (arg1 instanceof Integer) {
                if (INSTALL_VERSION_ONE.equals(arg1)) {
                    old = arg1;
                }
            } else if (arg1 instanceof List) {
                Iterator it = ((List) arg1).iterator();
                while (it.hasNext()) {
                    Object val = it.next();
                    if (INSTALL_VERSION_ONE.equals(val)) {
                        old = val;
                        break;
                    }
                }
            }
        } else if (LICENSE_ACCEPT.equals(arg0)) {
            if (arg1 instanceof Boolean) {
                licenseAccepted = (Boolean) arg1;
            } else {
                throw new IllegalArgumentException();
            }
        } else if (DOWNLOAD_DEPS.equals(arg0)) {
            if (arg1 instanceof Boolean) {
                downloadDependencies = (Boolean) arg1;
                extract.setDoExternalDepsDownload(downloadDependencies.booleanValue());
            } else {
                throw new IllegalArgumentException();
            }
        } else if (TARGET_USER_DIR.equals(arg0)) {
            if (arg1 instanceof File) {
                extract.setUserDirOverride((File) arg1);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (PRODUCT_INSTALL_TYPE.equals(arg0)) {
            if (arg1 instanceof String) {
                extract.setProductInstallTypeOveride((String) arg1);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (ALLOW_NON_EMPTY_INSTALL_DIR.equals(arg0)) {
            if (arg1 instanceof Boolean) {
                extract.allowNonEmptyInstallDirectory((Boolean) arg1);
            } else {
                throw new IllegalArgumentException();
            }
        }
        return old;
    }

    /**
     * Close extract to allow deletion
     */
    public String close() {
        return extract.close();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#putAll(java.util.Map)
     */
    public void putAll(Map arg0) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#remove(java.lang.Object)
     */
    public Object remove(Object arg0) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#size()
     */
    public int size() {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.Map#values()
     */
    public Collection values() {
        throw new UnsupportedOperationException();
    }

}
