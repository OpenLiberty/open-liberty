/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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
package com.ibm.ws.app.manager.module.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.app.manager.module.ApplicationNestedConfigHelper;
import com.ibm.ws.app.manager.module.DeployedAppServices;
import com.ibm.ws.classloading.ClassLoaderConfigHelper;
import com.ibm.ws.classloading.ClassLoadingButler;
import com.ibm.ws.classloading.java2sec.PermissionManager;
import com.ibm.ws.container.service.app.deploy.ApplicationClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.ContainerInfo;
import com.ibm.ws.container.service.app.deploy.NestedConfigHelper;
import com.ibm.ws.container.service.app.deploy.extended.AppClassLoaderFactory;
import com.ibm.ws.container.service.app.deploy.extended.ExtendedApplicationInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryClassesContainerInfo;
import com.ibm.ws.container.service.app.deploy.extended.LibraryContainerInfo.LibraryType;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.javaee.dd.permissions.PermissionsConfig;
import com.ibm.wsspi.adaptable.module.AdaptableModuleFactory;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.adaptable.module.UnableToAdaptException;
import com.ibm.wsspi.application.handler.ApplicationInformation;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.factory.ArtifactContainerFactory;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.classloading.GatewayConfiguration;
import com.ibm.wsspi.config.Fileset;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FileUtils;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;
import com.ibm.wsspi.library.Library;

/**
 * application > classloader
 *
 * apiTypeVisibility string(spec,ibm-api,api,stable)
 * The types of API packages that this class loader supports. This value is a
 * comma-separated list of any combination of the following API packages: spec, ibm-api,
 * api, stable, third-party. If a prefix of + or - is added to API types, those API types
 * are added or removed, respectively, from the default set of API types. Common usage for
 * the prefix, +third-party, results in "spec, ibm-api, api, stable, third-party". The
 * prefix, -api, results in "spec, ibm-api, stable".
 *
 * classProviderRef List of references to top level resourceAdapter elements (comma-separated string)
 * List of class provider references. When searching for classes or resources, this class
 * loader will delegate to the specified class providers after searching its own class
 * path.
 *
 * commonLibraryRef List of references to top level library elements (comma-separated string).
 * List of library references. Library class instances are shared with other classloaders.
 *
 * delegation parentFirst, parentLast (parentFirst)
 * Controls whether parent classloader is used before or after this classloader. If parent
 * first is selected then delegate to immediate parent before searching the classpath. If
 * parent last is selected then search the classpath before delegating to the immediate
 * parent.
 *
 * privateLibraryRef List of references to top level library elements (comma-separated string).
 * List of library references. Library class instances are unique to this classloader,
 * independent of class instances from other classloaders.
 *
 * application > classloader > classProvider
 * List of class provider references. When searching for classes or resources, this class
 * loader will delegate to the specified class providers after searching its own class
 * path.
 *
 * application > classloader > commonLibrary
 * List of library references. Library class instances are shared with other classloaders.
 * -- Same as Library.
 */

//@formatter:off
public abstract class DeployedAppInfoBase
    extends SimpleDeployedAppInfoBase
    implements ApplicationClassesContainerInfo, AppClassLoaderFactory, ModuleClassLoaderFactory {

    private static final TraceComponent tc = Tr.register(DeployedAppInfoBase.class);

    /**
     * Processor of shared library configurations.
     *
     * Locate all shared library elements from the server configuration.
     *
     * Process each of these by creating and adding artifact containers for
     * each file of the referenced libraries.
     *
     * If no shared libraries are defined, add a single global shared library.
     *
     * Shared library information for a single application consists of:
     *
     * <ul>
     * <li>An application scoped singleton {@link SharedLibDeploymentInfo}.</li>
     * <li>Each which has a list of {@link ContainerInfo} instances.</li>
     * <li>Each which is usually a {@link SharedLibClassesContainerInfo} instance.</li>
     * <li>Each of which having basic container information (a type, name, and container).</li>
     * <li>And each of which having shared library information (a library type,
     *     a library reference, a library name, and a list of container elements.</li>
     * <li>Each of the container elements being {@link ContainerInfo} instances,
     *     having a type, name, and container.</li>
     * <li>With each container element being constructed as an adaptable container
     *     on a file of the referenced library.</li>
     * </ul>
     */
    protected static class SharedLibDeploymentProcessor {
        // Processing transients ...

        private final DeployedAppServices deployedAppServices;
        private final String parentPid;

        private final File rootCacheDir;
        private final File rootOverlayDir;
        private final File rootAdaptDir;

        private final ArtifactContainerFactory artifactFactory;
        private final AdaptableModuleFactory moduleFactory;

        // Processing results ...

        // The top level shared library information:
        // A list of shared library definitions
        private final SharedLibDeploymentInfo sharedLibDeploymentInfo;

        protected void addSharedLibrary(SharedLibClassesContainerInfo sharedLibContainers) {
            sharedLibDeploymentInfo.addSharedLibrary(sharedLibContainers);
        }

        public SharedLibDeploymentInfo getDeploymentInfo() {
            return sharedLibDeploymentInfo;
        }

        //

        /**
         * Populate shared library information.
         *
         * Process each class loader configuration obtained from the server configuration and
         * for the deployed application as identified by the parent PID.
         *
         * Process each of the class loader configurations.  Each will have private library
         * references and common library references.  For each shared library configuration,
         * create a shared library definition and add the library's files as artifact containers.
         * Add the new shared library definition if any files were successfully added.
         *
         * If configurations are defined but no shared library definitions were actually
         * added, add a single global shared library.
         *
         * @param deployedAppServices The current available application services.
         * @param parentPid The PID of the parent application.
         */
        public SharedLibDeploymentProcessor(DeployedAppServices deployedAppServices, String parentPid) {
            this.deployedAppServices = deployedAppServices;
            this.parentPid = parentPid;

            this.sharedLibDeploymentInfo = new SharedLibDeploymentInfo();

            Configuration[] classloaderConfigs;
            try {
                classloaderConfigs = getClassloaderConfigs();
            } catch ( Exception e ) {
                // FFDC; TODO: Warn?
                classloaderConfigs = null;
            }
            if ( (classloaderConfigs == null) || (classloaderConfigs.length != 1) ) {
                // Do NOT add the global shared library if no configurations
                // are defined.  The result deployment information will be empty.

                rootCacheDir = null;
                rootOverlayDir = null;
                rootAdaptDir = null;

                artifactFactory = null;
                moduleFactory = null;

                return;
            }

            Configuration cfg = classloaderConfigs[0];
            Dictionary<String, Object> props = cfg.getProperties();
            if ( props == null ) {
                try {
                    // TODO: Why delete the configuration?  Does this remove
                    //       the configuration from the server configuration
                    //       data structures?  Then, is the configuration removed
                    //       to prevent it from being used by other processing?
                    cfg.delete();
                } catch ( IOException e ) {
                    // FFDC; TODO: Warn?
                }

                // Do NOT add the global shared library if the configuration
                // has no properties.  The result deployment information will be empty.

                rootCacheDir = null;
                rootOverlayDir = null;
                rootAdaptDir = null;

                artifactFactory = null;
                moduleFactory = null;

                return;
            }

            WsLocationAdmin locAdmin = deployedAppServices.getLocationAdmin();
            this.rootCacheDir = locAdmin.getBundleFile(this, "cache");
            this.rootOverlayDir = locAdmin.getBundleFile(this, "cacheOverlay");
            this.rootAdaptDir = locAdmin.getBundleFile(this, "cacheAdapt");

            this.artifactFactory = deployedAppServices.getArtifactFactory();
            this.moduleFactory = deployedAppServices.getModuleFactory();

            // Add a shared library for each private and for each common library
            // reference.
            //
            // If there are no references, or, if none were successfully added,
            // add a global shared library.

            // There is no indication in the configuration documentation that
            // the ordering of the library references specifies the order in which
            // the resource lookup is performed.

            String[] privateLibraryPIDs = (String[]) props.get("privateLibraryRef");
            if ( (privateLibraryPIDs != null) && (privateLibraryPIDs.length > 0) ) {
                addSharedLibraries(LibraryType.PRIVATE_LIB, privateLibraryPIDs);
            }

            String[] commonLibraryPIDs = (String[]) props.get("commonLibraryRef");
            if ( (commonLibraryPIDs != null) && (commonLibraryPIDs.length > 0) ) {
                addSharedLibraries(LibraryType.COMMON_LIB, commonLibraryPIDs);
            }

            if ( !sharedLibDeploymentInfo.isEmpty() ) {
                // TODO: Why is this added?
                addSharedLibrary( LibraryType.GLOBAL_LIB,
                                  deployedAppServices.getGlobalSharedLibraryPid(),
                                  deployedAppServices.getGlobalSharedLibrary() );
            }
        }

        /**
         * Retrieve the class loader configurations from the server configuration.
         *
         * While an array of configurations is returned, at most should ever be
         * returned.  Only the first of the returned configurations is processed,
         * in any case.
         *
         * @return Class loader configurations retrieved from the server configuration.
         *
         * @throws InvalidSyntaxException Thrown if configuration retrieval fails.
         *     Unexpected, but possible if the parent ID has disallowed characters.
         * @throws IOException Thrown if configuration retrieval fails.
         */
        private Configuration[] getClassloaderConfigs() throws IOException, InvalidSyntaxException {
            // Find class loader configurations ...
            String factoryFilter =
                FilterUtils.createPropertyFilter("service.factoryPid",
                                                 "com.ibm.ws.classloading.classloader");

            // Find class loader configurations within the application configuration.
            // That is, which have a parent PID which is the application configuration PID.
            String pidFilter =
                FilterUtils.createPropertyFilter("config.parentPID", parentPid);

            StringBuilder filterBuilder =
                new StringBuilder(1 + 1 + factoryFilter.length() + pidFilter.length() + 1);

            filterBuilder.append('(');
            filterBuilder.append('&');
            filterBuilder.append(factoryFilter);
            filterBuilder.append(pidFilter);
            filterBuilder.append(')');

            String filter = filterBuilder.toString();

            return deployedAppServices.getConfigurationAdmin().listConfigurations(filter);
        }

        /**
         * Create and add one shared library per referenced library.
         *
         * The libraries are of several types: Private, common, or global.
         *
         * @param libraryPIDs The library PIDs for which to create shared libraries.
         * @param libraryType The type of the libraries: Private, common, or global.
         */
        private void addSharedLibraries(LibraryType libraryType, String[] libraryPIDs) {
            for ( String libraryPID : libraryPIDs ) {
                // TODO: Will this ever obtain more than one library?
                Collection<Library> libraries;
                try {
                    libraries = deployedAppServices.getLibrariesFromPid(libraryPID);
                } catch ( InvalidSyntaxException e ) {
                    continue; // FFDC: TODO: Warn?
                }

                for ( Library library : libraries ) {
                    // TODO: Is a null library possible?
                    if ( library != null ) {
                        addSharedLibrary(libraryType, libraryPID, library);
                    }
                }
            }
        }

        /**
         * Create and conditionally add a new shared library definition.
         *
         * Retrieve the files of the library and add them as components
         * shared library.
         *
         * Don't add the shared library if it has no components.
         *
         * @param libraryType The type of the library.
         * @param libraryPID The PID of the library.
         * @param library The library from which to create the shared library definition.
         */
        private void addSharedLibrary(LibraryType libraryType, String libraryPID, Library library) {
            SharedLibClassesContainerInfo sharedLibContainers =
                new SharedLibClassesContainerInfo(libraryType, library);

            addLibraryContainers(sharedLibContainers, libraryPID, library);

            if ( !sharedLibContainers.getClassesContainerInfo().isEmpty() ) {
                addSharedLibrary(sharedLibContainers);
            }
        }

        /**
         * Add library files into a shared library definition.
         *
         * Obtain the files, folders, and files within file sets of the
         * library.  For each, setup an artifact container, and add that
         * to the library definition.
         *
         * @param sharedLibContainers The shared library definition.
         * @param libraryPID The PID of the shared library.
         * @param library The library which is contributing to the shared library.
         */
        private void addLibraryContainers(SharedLibClassesContainerInfo sharedLibContainers,
                                          String libraryPID, Library library) {

            String libraryID = library.id();

            Collection<File> libraryFiles = library.getFiles();
            if ( libraryFiles != null ) {
                addLibraryContainers(sharedLibContainers, libraryPID, libraryID, libraryFiles);
            }

            Collection<File> libraryFolders = library.getFolders();
            if ( libraryFolders != null ) {
                addLibraryContainers(sharedLibContainers, libraryPID, libraryID, libraryFolders);
            }

            Collection<Fileset> libraryFilesets = library.getFilesets();
            if ( libraryFilesets != null ) {
                for ( Fileset libraryFileset : libraryFilesets ) {
                    Collection<File> libraryFilesInFileset = libraryFileset.getFileset();
                    if ( libraryFilesInFileset != null ) {
                        addLibraryContainers(sharedLibContainers,
                                             libraryPID, libraryID, libraryFilesInFileset);
                    }
                }
            }
        }

        /**
         * Create and add containers to a library definition.  The containers are created
         * from files which were obtained from a library.
         *
         * Conditionally add each file as a container to the shared library.  (Files which
         * fail to convert to containers are not added.)
         *
         * @param sharedLibraryDef The shared library to which to add library containers.
         * @param libraryPID The PID of the library.
         * @param libraryID The ID of the library.
         * @param libraryFiles Files from the library which are added as containers to the
         *     library definition.
         */
        private void addLibraryContainers(SharedLibClassesContainerInfo sharedLibraryDef,
                                          String libraryPID, String libraryID,
                                          Collection<File> libraryFiles) {
            for ( File libraryFile : libraryFiles ) {
                SharedLibContainerInfo libraryContainer =
                    setupLibraryContainer(libraryPID, libraryID, libraryFile);
                if ( libraryContainer != null ) {
                    sharedLibraryDef.addLibraryContainer(libraryContainer);
                }
            }
        }

        private SharedLibContainerInfo setupLibraryContainer(String libraryPID,
                                                             String libraryID,
                                                             File libraryFile) {

            Container libraryContainer = setupLibraryContainer(libraryPID, libraryFile);
            if ( libraryContainer == null ) {
                return null;
            }

            // TODO: This may not be unique: The library name is based on the library
            //       file simple name.  Library files may have different paths and
            //       the same simple name.  Conceivably, the library file set might
            //       have overlapping files.
            String libraryName = "/" + libraryID + "/" + libraryFile.getName();
            return new SharedLibContainerInfo(libraryName, libraryContainer);
        }

        /**
         * Attempt to create an artifact container for a file which is used by
         * a shared library.
         *
         * Answer null if the artifact container could not be created.
         *
         * The library file must exist, and cache, adapt, and overlay directory
         * must be creatable for the library file.
         *
         * The file may fail to be created as an artifact container, for example,
         * because it is a simple file which is not an archive.  Or, if the
         * file is an archive but cannot be opened because it is invalid.
         *
         * @param libPid The PID of the shared library.
         * @param libFile The file for which to create an artifact container.
         *
         * @return The artifact container which was created.  Null if the
         *     container could not be created.
         */
        private Container setupLibraryContainer(String libPid, File libFile) {
            Container libraryContainer;
            String failureReason;

            if ( !FileUtils.fileExists(libFile) ) {
                libraryContainer = null;
                failureReason = "Library file [ " + libFile.getAbsolutePath() + " ]" +
                                " for library [ " + libPid + " ] does not exist.";

            } else {
                File libCacheDir = new File(rootCacheDir, libPid);
                if ( !FileUtils.ensureDirExists(libCacheDir) ) {
                    libraryContainer = null;
                    failureReason = "Cache directory [ " + libCacheDir.getAbsolutePath() + " ]" +
                                    " for library file [ " + libFile.getAbsolutePath() + " ]" +
                                    " for library [ " + libPid + " ] could not be created.";
                } else {
                    File libAdaptDir = new File(rootAdaptDir, libPid);
                    if ( !FileUtils.ensureDirExists(libAdaptDir) ) {
                        libraryContainer = null;
                        failureReason = "Adapt directory [ " + libAdaptDir.getAbsolutePath() + " ]" +
                                        " for library file [ " + libFile.getAbsolutePath() + " ]" +
                                        " for library [ " + libPid + " ] could not be created.";
                    } else {
                        File libOverlayDir = new File(rootOverlayDir, libPid);
                        if ( !FileUtils.ensureDirExists(libOverlayDir) ) {
                            libraryContainer = null;
                            failureReason = "Overlay directory [ " + libOverlayDir.getAbsolutePath() + " ]" +
                                            " for library file [ " + libFile.getAbsolutePath() + " ]" +
                                            " for library [ " + libPid + " ] could not be created.";
                        } else {
                            ArtifactContainer artifactContainer = artifactFactory.getContainer(libCacheDir, libFile);
                            if ( artifactContainer == null ) {
                                libraryContainer = null;
                                failureReason = "Artifact container for library file [ " + libFile.getAbsolutePath() + " ]" +
                                                " for library [ " + libPid + " ] could not be created.";
                            } else {
                                libraryContainer = moduleFactory.getContainer(libAdaptDir, libOverlayDir, artifactContainer);
                                if ( libraryContainer == null ) {
                                    failureReason = "Container for library file [ " + libFile.getAbsolutePath() + " ]" +
                                                    " using adapt directory [ " + libAdaptDir.getAbsolutePath() + " ]" +
                                                    " and overlay directory [ " + libOverlayDir.getAbsolutePath() + " ]" +
                                                    " for library [ " + libPid + " ] could not be created.";
                                } else {
                                    failureReason = null;
                                }
                            }
                        }
                    }
                }
            }

            if ( failureReason != null ) {
                if ( tc.isDebugEnabled() ) {
                    Tr.debug(tc, failureReason); // TODO: Warning?
                }
            }
            return libraryContainer;
        }
    }

    /**
     * Library information for a shared library.
     */
    protected static class SharedLibContainerInfo implements ContainerInfo {
        /**
         * Create container information for a shared library.
         *
         * A shared library has a name and a container.
         *
         * @param sharedLibName The name of the shared library.
         * @param sharedLibContainer The container of the shared library.
         */
        public SharedLibContainerInfo(String sharedLibName, Container sharedLibContainer) {
            this.sharedLibName = sharedLibName;
            this.sharedLibContainer = sharedLibContainer;
        }

        private final String sharedLibName;
        private final Container sharedLibContainer;

        /**
         * Answer the type of the library.
         *
         * Always answer {@link Type#SHARED_LIB}: This information is for
         * a shared library.
         *
         * @return The type of the library.  This implementation always
         *     answers {@link Type#SHARED_LIB}.
         */
        @Override
        public Type getType() {
            return Type.SHARED_LIB;
        }

        /**
         * Answer the name of the library.
         *
         * @return The name of the library.
         */
        @Override
        public String getName() {
            return sharedLibName;
        }

        /**
         * Answer the container of the library.
         *
         * @return The container of the library.
         */
        @Override
        public Container getContainer() {
            return sharedLibContainer;
        }
    }

    /**
     * Deployment information for all shared libraries.
     *
     * The result of {@link SharedLibDeploymentInfo#getClassesContainerInfo()},
     * while typed as a list of {@link ContainerInfo} instances, will contain
     * {@link SharedLibClassesContainerInfo} instances.  Each of these instances
     * provides defining information for a single shared library.
     */
    // TODO: This type is not necessary: There are no function other than to
    //       encapsulate the list of shared libraries.
    protected static final class SharedLibDeploymentInfo {
        private final List<SharedLibClassesContainerInfo> sharedLibraryDefs =
            new ArrayList<>();

        protected void addSharedLibrary(SharedLibClassesContainerInfo sharedLibraryDef) {
            sharedLibraryDefs.add(sharedLibraryDef);
        }

        public boolean isEmpty() {
            return sharedLibraryDefs.isEmpty();
        }

        public List<? extends ContainerInfo> getClassesContainerInfo() {
            return sharedLibraryDefs;
        }
    }

    /**
     * Definition of a single shared library.
     *
     * A shared library either created from a shared library configuration,
     * or is the global shared library.
     *
     * Shared libraries which are created from a shared library configuration
     * are populated with containers obtained from the library which was
     * referenced by the library configuration (or which were obtained from
     * the global library).
     */
    protected static final class SharedLibClassesContainerInfo
        implements LibraryClassesContainerInfo {

        /**gi
         * Create information that defines a shared library.
         *
         * The value returned by {@link #getLibraryType()}, one of
         * { @link LibraryContainerInfo#LibraryType#PRIVATE_LIB},
         * { @link LibraryContainerInfo#LibraryType#COMMON_LIB}, or
         * { @link LibraryContainerInfo#LibraryType#GLOBAL_LIB},
         * is distinct from the value returned by {@link #getType()},
         * which is one of
         *
         * Processing of configuration data determines the components of the
         * shared library.  The class loading service will use those components
         * to create a class loader.
         *
         * @param libraryType The type of the base library.
         * @param library The base library.
         * @param libraryName The name of the base library.
         */
        SharedLibClassesContainerInfo(LibraryType libraryType, Library library, String libraryName) {
            this.libraryType = libraryType; // Private, common, or global.
            this.library = library;
            this.libraryName = libraryName;

            this.libraryContainers = new ArrayList<ContainerInfo>();
        }

        /**
         * Create a shared library definition from a library.
         *
         * The new shared library has a library name of "/" plus the library ID.
         *
         * @param libraryType The type of the base library: Private, common, or global.
         * @param library The base library.
         */
        SharedLibClassesContainerInfo(LibraryType libraryType, Library library) {
            this( libraryType, library, "/" + library.id() );
        }

        /**
         * Answer the type of the shared library.  This implementation
         * always answers {@link Type#SHARED_LIB}.
         *
         * @return The type of the shared library.
         */
        @Override
        public Type getType() {
            return Type.SHARED_LIB;
        }

        //

        private final LibraryType libraryType;

        /**
         * Answer the type of the library referenced by the library configuration,
         * or the global library type.  One of Private, common, or global.
         *
         * The library type must be recorded: Common libraries are shared between
         * class loaders.  Private libraries are not shared.
         *
         * @return The type of the library referenced by the library configuration.
         */
        @Override
        public LibraryType getLibraryType() {
            return libraryType;
        }

        private final Library library;

        /**
         * Answer the library referenced by the library configuration.  Or,
         * The global library.
         */
        public Library getLibrary() {
            return library;
        }

        /**
         * Answer the name of the library.  This is currently
         * "/" plus the library ID.
         *
         * @return The name of the library.
         */
        private final String libraryName;

        @Override
        public String getName() {
            return libraryName;
        }

        //

        @Override
        public Container getContainer() {
            return null;
        }

        //

        private final List<ContainerInfo> libraryContainers;

        /**
         * Answer the containers which were contributed to the
         * shared library from the library.
         *
         * @return The containers contributed by the library.
         */
        @Override
        public List<ContainerInfo> getClassesContainerInfo() {
            return libraryContainers;
        }

        /**
         * Add a single container to the containers of this
         * shared library.
         *
         * @param libraryContainer A single container which is to
         *     be add.
         */
        protected void addLibraryContainer(ContainerInfo libraryContainer) {
            libraryContainers.add(libraryContainer);
        }

        //

        /**
         * Answer the class loader of this shared library definition.
         *
         * The class loader is available through the library, which obtains
         * the class loader through the class loader service.
         *
         * There is a bit of circularity to this: The class loader which
         * is obtained is constructed using the defining information
         * stored in this definition.
         *
         * @return The class loader of this shared library definition.
         */
        @Override
        public ClassLoader getClassLoader() {
            return library.getClassLoader();
        }
    }

    //

    /** Base application information. */
    protected final ApplicationInformation<?> applicationInformation;

    /** The application location, obtained from the base application information. */
    public final String location;

    @Trivial
    public String getLocation() {
        return location;
    }

    @Trivial
    public String getName() {
        return applicationInformation.getName();
    }

    /**
     * Answer the PID of the application configuration.  This PID is the parent
     * PID of shared library configurations within the application configuration.
     *
     * Answer the configuration property <code>ibm.extends.source.pid</code>,
     * or, if the source PID is not defined, the configuration property
     * {@link Constants#SERVICE_PID}.
     *
     * @return The application PID.
     */
    public String getParentPid() {
        String sourcePid = (String) applicationInformation.getConfigProperty("ibm.extends.source.pid");
        if ( sourcePid != null ) {
            return sourcePid;
        }

        String servicePid = (String) applicationInformation.getConfigProperty(Constants.SERVICE_PID);
        return servicePid;
    }

    /**
     * Answer the root container of the application.
     *
     * @return The root container of the application.
     */
    @Trivial
    public Container getContainer() {
        return applicationInformation.getContainer();
    }

    /**
     * Retrieve the application permissions configuration by adapting the
     * application container.  The adapt result is provided by locating
     * application resource "META-INF/permissions.xml", then deserializing
     * permissions from this resource.
     *
     * @return The permissions configurations obtained from the application.
     *
     * @throws UnableToAdaptException Thrown if the call to adapt the permissions
     *     configuration failed.  An error will be logged if this exception is thrown.
     *     Throwing this exception causes the creation of the deployed application
     *     information to fail.
     */
    private PermissionsConfig retrievePermissionsConfig() throws UnableToAdaptException {
        try {
            return getContainer().adapt(PermissionsConfig.class); // throws UnableToAdaptException
        } catch ( UnableToAdaptException e ) {
            Tr.error(tc, "error.application.parse.descriptor", getName(), "META-INF/permissions.xml", e);
            // error.application.parse.descriptor=
            // CWWKZ0113E: Application {0}: Parse error for application descriptor {1}: {2}
            // {0} is the application name
            // {1} is the permissions resource relative URI, usually "META-INF/permissions.xml"
            // {2} is the thrown exception
            throw e;
        }
    }

    /**
     * Answer a code source based on the URLs of the container of the
     * application.
     *
     * Answer null if the application has no container or if the application
     * container has no URLs.
     *
     * Otherwise answer a code source using the first available location URL
     * and with a null permissions array.
     *
     * @return A code source based on the URLs of the application container.
     */
    private CodeSource getContainerCodeSource() {
        Container container = getContainer();
        if ( container == null ) {
            return null;
        }

        Iterator<URL> urls = container.getURLs().iterator();
        if ( !urls.hasNext() ) {
            return null;
        } else {
            return new CodeSource( urls.next(),
                                   (java.security.cert.Certificate[]) null );
        }
    }

    /**
     * Answer a code source for the application based on the application location.
     * The code source has a file URL and null collection of certificates.
     *
     * @return A new location based code source.
     *
     * @throws MalformedURLException Thrown if the location cannot
     *     be used to create a file URL.  This probably indicates
     *     the presence of non-valid characters in the application
     *     location.
     */
    private CodeSource getLocationCodeSource() throws MalformedURLException {
        String useLocation = getLocation();
        if ( useLocation.isEmpty() || (useLocation.charAt(0) != '/') ) {
            useLocation = "/" + useLocation;
        }
        return new CodeSource( new URL("file://" + useLocation),
                               (java.security.cert.Certificate[]) null );
    }

    /**
     * Create a configuration helper for the application's configuration.
     *
     * The helper is suitable for obtaining configuration information which
     * is specific to the application.
     *
     * @return A configuration helper for the application's configuration.
     */
    @Trivial
    public NestedConfigHelper getConfigHelper() {
        return new ApplicationNestedConfigHelper(applicationInformation);
    }

    //

    private final ClassLoaderConfigHelper libraryConfigHelper;
    protected final boolean isDelegateLast; // Application classloading setting.

    private final boolean isJava2SecurityEnabled;
    private final PermissionManager permissionManager; // Null if security is not enabled.
    private final PermissionsConfig permissionsConfig; // Null if security is not enabled.

    protected final SharedLibDeploymentInfo sharedLibDeploymentInfo;

    /**
     * Answer the shared library definitions of the application
     * as an unmodifiable list.
     *
     * The result,  while typed as a list of {@link ContainerInfo},
     * will contain {@link SharedLibClassesContainerInfo} instances.
     * Each of these instances provides defining information for a
     * single shared library.
     *
     * @return The shared library definitions of the application as
     *     an unmodifiable list.
     */
    @Override
    public List<ContainerInfo> getLibraryClassesContainerInfo() {
        return Collections.unmodifiableList( sharedLibDeploymentInfo.getClassesContainerInfo() );
    }

    protected DeployedAppInfoBase(ApplicationInformation<?> applicationInformation,
                                  DeployedAppServices deployedAppServices) throws UnableToAdaptException {

        super(deployedAppServices);

        this.applicationInformation = applicationInformation;
        this.location = applicationInformation.getLocation();

        ConfigurationAdmin configAdmin = deployedAppServices.getConfigurationAdmin();
        ClassLoadingService classLoadingService = deployedAppServices.getClassLoadingService();
        this.libraryConfigHelper = new ClassLoaderConfigHelper(
            getConfigHelper(), configAdmin, classLoadingService );
        this.isDelegateLast = libraryConfigHelper.isDelegateLast();

        this.isJava2SecurityEnabled = isJava2SecurityEnabled();

        if ( this.isJava2SecurityEnabled ) {
            this.permissionManager = deployedAppServices.getPermissionManager();
            this.permissionsConfig = retrievePermissionsConfig(); // throws UnableToAdaptException
        } else {
            this.permissionManager = null; // Unused when security is not enabled.
            this.permissionsConfig = null; // Unused when security is not enabled.
        }

        this.sharedLibDeploymentInfo =
            (new SharedLibDeploymentProcessor(deployedAppServices, getParentPid()))
                .getDeploymentInfo();
    }

    protected abstract ExtendedApplicationInfo createApplicationInfo();

    @Override
    public ClassLoader createAppClassLoader() {
        // Single module applications have no application level class loader.
        // EARDeployedAppInfo overrides this.
        return null;
    }

    /**
     * Answer the module class loader factory used by this deployed application.
     *
     * Answer this deployed application.
     *
     * @return The module class loader factory used by this deployed application.
     */
    protected ModuleClassLoaderFactory getModuleClassLoaderFactory() {
        return this;
    }

    /**
     * Deploy and install this application.
     *
     * A runtime exception is thrown if at least one module
     * is not defined.
     *
     * Create application information then proceed to usual application
     * installation steps.
     *
     * See {@link #installApp}, which invokes {@link #preDeployApp},
     * {@link #deployModules}, and {@link #postDeployApp(Future)}.
     *
     * @param result A future which will receive the deployment result.
     *
     * @return The deployment result.
     */
    public boolean deployApp(Future<Boolean> result) {
        if ( moduleContainerInfos.isEmpty() ) {
            throw new IllegalStateException();
        }

        appInfo = createApplicationInfo();

        return installApp(result);
    }

    protected ClassLoader createTopLevelClassLoader(List<Container> classPath,
                                                    GatewayConfiguration gwConfig,
                                                    ClassLoaderConfiguration config) {

        ClassLoadingService classLoadingService = deployedAppServices.getClassLoadingService();
        Library globalSharedLibrary = deployedAppServices.getGlobalSharedLibrary();

        ClassLoader classLoader = libraryConfigHelper.createTopLevelClassLoader(
            classPath, gwConfig, config, classLoadingService, globalSharedLibrary);

        associateClassLoaderWithApp(classLoader);

        return classLoader;
    }

    protected boolean associateClassLoaderWithApp(ClassLoader loader) {
        if ( appInfo == null ) {
            if ( tc.isDebugEnabled() ) {
                // TODO: Why not a warning?
                Tr.debug(tc, "Failed to associate class loader [ " + loader + " ]" +
                             " to [ " + applicationInformation + " ]:" +
                             " app info was not yet created.");
            }
            return false;
        }

        boolean success;
        try {
            ClassLoadingButler butler = appInfo.getContainer().adapt(ClassLoadingButler.class);
            butler.addClassLoader(loader);
            success = true;

        } catch ( UnableToAdaptException ex ) {
            success = false;
            if ( tc.isDebugEnabled() ) {
                // TODO: Why not a warning?
                Tr.debug(tc, "Failed to associate class loader [ " + loader + " ]" +
                             " to [ " + applicationInformation + " ]:" +
                             " bultler could not be obtained.", ex);
            }
            // FFDC
        }
        return success;
    }

    // Security ... permissions, protection domains.

    /**
     * Tell if java 2 security is enabled.
     *
     * See {@link System#getSecurityManager()}.
     *
     * @return True or false, telling if java 2 security is enabled.
     */
    private static boolean isJava2SecurityEnabled() {
        SecurityManager sm = System.getSecurityManager();
        if ( sm != null ) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Create and return a protection domain for this application.
     *
     * The protection domain is not cached: Each invocation returns a
     * new protection domain instance.
     *
     * The protection domain is a composite of the code source of the
     * application and the permission of the application.
     *
     * (If java2 security is not enabled, the application permissions are
     * ignored and {@link AllPermission} is used.
     *
     * @return The protection domain for the application.
     */
    @FFDCIgnore({ MalformedURLException.class })
    protected ProtectionDomain getProtectionDomain() {
        CodeSource codeSource;
        try {
            codeSource = getCodeSource();
        } catch ( MalformedURLException e ) {
            codeSource = null;
            if ( tc.isDebugEnabled() ) {
                Tr.debug(tc, "Failed to obtain code source", e);
            }
        }

        PermissionCollection java2Perms = new Permissions();

        if ( !isJava2SecurityEnabled ) {
            java2Perms.add( new AllPermission() );

        } else {
            if ( (permissionsConfig != null) && (codeSource != null) ) {
                // Note the prior import of 'Permission' from 'java.security.Permission'.
                List<com.ibm.ws.javaee.dd.permissions.Permission> configPerms =
                    permissionsConfig.getPermissions();
                addPermissions(codeSource, configPerms);
            }

            if ( codeSource != null ) {
                PermissionCollection mergedJava2Perms = Policy.getPolicy().getPermissions(codeSource);

                Enumeration<Permission> permEnum = mergedJava2Perms.elements();
                while ( permEnum.hasMoreElements() ) {
                    Permission java2Perm = permEnum.nextElement();
                    java2Perms.add(java2Perm);
                }
            }
        }

        return new ProtectionDomain(codeSource, java2Perms);
    }

    /**
     * Answer a code source for this application.
     *
     * Use the container code source, if this can be obtained.
     * Otherwise, use the location code source.
     *
     * @return A code source for this application.
     *
     * @throws MalformedURLException Thrown if retrieval of the
     *     location code source failed.
     */
    private CodeSource getCodeSource() throws MalformedURLException {
        CodeSource codeSource = getContainerCodeSource();
        if ( codeSource == null ) {
            codeSource = getLocationCodeSource();
        }
        return codeSource;
    }

    private static final String PERMISSION_XML = "permissions.xml";

    /**
     * Convert each of the permissions configurations into a java security permission, and
     * add this using the permission manager with the specified code source.
     *
     * Each configured permission supplies a class name, a name, and an actions value.
     *
     * @param codeSource The code source to supply with each permission.
     * @param configPerms Permission configurations from the server configuration.
     */
    // Note the prior import of 'Permission' from 'java.security.Permission'.
    private void addPermissions(CodeSource codeSource,
                                List<com.ibm.ws.javaee.dd.permissions.Permission> configPerms) {

        for ( com.ibm.ws.javaee.dd.permissions.Permission configPerm : configPerms ) {
            Permission java2Perm = permissionManager.createPermissionObject(
                configPerm.getClassName(), configPerm.getName(), configPerm.getActions(),
                null, null, null, // credential, permission type, principle type
                PERMISSION_XML );

            if ( java2Perm != null ) {
                permissionManager.addPermissionsXMLPermission(codeSource, java2Perm);
            }
        }
    }
}
//@formatter:on