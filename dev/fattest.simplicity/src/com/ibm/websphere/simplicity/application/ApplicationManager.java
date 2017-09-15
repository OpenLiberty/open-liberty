/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.Node;
import com.ibm.websphere.simplicity.OperationResults;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.application.loose.VirtualArchive;
import com.ibm.websphere.simplicity.application.loose.VirtualArchiveFactory;
import com.ibm.websphere.simplicity.application.tasks.ApplicationTask;
import com.ibm.websphere.simplicity.exception.ApplicationNotInstalledException;
import com.ibm.websphere.simplicity.exception.NotImplementedException;
import com.ibm.websphere.simplicity.exception.NullArgumentException;
import com.ibm.websphere.simplicity.log.Log;
import componenttest.common.apiservices.Bootstrap;
import componenttest.topology.impl.LibertyServer;

/**
 * Maintains state information for applications running on the cell,
 * and provides APIs to install and uninstall applications.
 */
public class ApplicationManager {

    private static Class c = ApplicationManager.class;
    private static final String CHANGE_KEY_APPS = "applications";

    protected Scope scope;
    protected Set<Application> applications;
    private LibertyServer target;

    enum InstallType {
        NORMAL_FILE, NORMAL_EXTRACTION, LOOSE_EXTRACTION
    };

    public ApplicationManager(Scope scope) throws Exception {
        this.scope = scope;
    }

    /**
     * @param type The type of application install root you want to retrieve.
     * @return The root path to which applications of the specified type are installed.
     * @throws Exception
     */
    public String getInstallLocation(ApplicationType type) throws Exception {
        String path = ((Node) scope).getProfileDir();
        String imageName = System.getProperty("image.name", "wlp");
        path += "/" + imageName + "/usr/shared/apps/";
        return path;
    }

    /**
     * Installing an application has a few steps, the most important of which is
     * setting the options prior to installation. The install wrapper provides
     * access to those options. When you are ready to install, call
     * ApplicationManager.install(InstallWrapper).
     * <p>
     * The app file parameter can point to any file on any machine, as long as
     * it's accessible from the local machine. Simplicity will perform the
     * necessary copy operations to install the application.
     *
     * @param appFile A pointer to the application file to install.
     * @return An InstallWrapper instance in which application options can be set.
     * @throws Exception
     */
    public InstallWrapper getInstallWrapper(RemoteFile appFile) throws Exception {
        if (!appFile.getName().endsWith(".ear"))
            throw new Exception("This API is valid only for EAR-type archives.  Please use the alternative getInstallWrapper API for other archive types.");
        return getInstallWrapper(appFile, ArchiveType.EAR);
    }

    public InstallWrapper getInstallWrapper(RemoteFile appFile, ArchiveType archiveType) throws Exception {
        if (!archiveType.equals(ArchiveType.EAR))
            throw new NotImplementedException();
        List<ApplicationTask> tasks = new ArrayList<ApplicationTask>();
        InstallWrapper ret = new InstallWrapper(appFile, tasks, scope, archiveType);
        return ret;
    }

    /**
     * @param name The name of the application.
     * @return True if the specified application is installed on the cell from which this ApplicationManager instance was obtained.
     * @throws Exception
     */
    public boolean isInstalled(String name) throws Exception {
        List<String> list = getApplicationNames();
        for (String s : list)
            if (s.equalsIgnoreCase(name))
                return true;
        return false;
    }

    /**
     * @return A list of names of applications installed on the cell from which this ApplicationManager was obtained.
     * @throws Exception
     */
    public List<String> getApplicationNames() throws Exception {
        Log.entering(c, "getList");
        List<String> result = listApplications();

        Log.exiting(c, "getList", result.toArray());
        return result;
    }

    /**
     * Get the currently installed {@link Application}s
     *
     * @return A Set of {@link Application}s representing the currently
     *         installed apps
     * @throws Exception
     */
    public Set<Application> getApplications() throws Exception {
        final String method = "getApplications";
        Log.entering(c, method);
        if (this.applications == null) {
            List<String> appNames = this.getApplicationNames();
            this.applications = new HashSet<Application>();
            for (String appName : appNames) {
                // TODO Will need to update this for other application types...
                this.addApplication(Application.create(this, ApplicationType.EAR, appName, this.scope));
            }
        }
        Log.exiting(c, method, this.applications);
        return new HashSet<Application>(this.applications);
    }

    /**
     * Performs all of the steps necessary to prepare, copy and install the application
     * defined by the options parameter.
     *
     * @param appName The name of the application to be installed.
     * @param options The settings for the application to be installed
     * @return An OperationResults instance for an application installation. That instance will contain a reference to the newly installed app.
     * @throws Exception
     */
    public OperationResults<Application> install(String appName, InstallWrapper options) throws Exception {
        options.getAppDeploymentOptions().setApplicationName(appName);
        return install(options);
    }

    /**
     * Get an {@link Application} that has the specified name
     *
     * @param name
     *            The name of the application to get
     * @return The {@link Application} with the specified name or null if no
     *         application with that name exists
     * @throws Exception
     */
    public Application getApplicationByName(String name) throws Exception {
        final String method = "getApplicationByName";
        Log.entering(c, method, name);
        Set<Application> apps = this.getApplications();
        for (Application app : apps) {
            if (app.getName().equalsIgnoreCase(name)) {
                Log.exiting(c, method, app);
                return app;
            }
        }
        return null;
    }

    /**
     * Performs all of the steps necessary to prepare, copy and install the application
     * defined by the options parameter.
     *
     * @param options The settings for the application to be installed
     * @return An OperationResults instance for an application installation. That instance will contain a reference to the newly installed app.
     * @throws Exception
     */
    public OperationResults<Application> install(InstallWrapper options) throws Exception {
        Log.entering(c, "install", options);
        if (options == null)
            throw new NullArgumentException("options");

        RemoteFile file = options.getEarFile();
        String name = options.getAppDeploymentOptions().getApplicationName();
        if (name == null)
            name = file.getName();
        installApplication(name, file, ApplicationType.EAR);

        this.addApplication(Application.create(this, ApplicationType.EAR, name, this.scope));

        /*
         * final String appName = options.getAppDeploymentOptions().getApplicationName();
         * 
         * if (isInstalled(appName))
         * throw new ApplicationAlreadyInstalledException(appName);
         * 
         * ensureMinimumInstallOptions(appName, options);
         * 
         * scope.getWorkspace().registerConfigChange(this, CHANGE_KEY_APPS, getApplications());
         * 
         * final Cell fcell = this.scope;
         * final RemoteFile fearFile = options.getEarFile();
         * final InstallWrapper fwrapper = options;
         * Log.finer(c, "install", "Performing installation", options.toTaskString());
         * OperationResults<Application> results = OperationsProviderFactory.getProvider().getApplicationOperationsProvider().installApplication(fcell, appName, fearFile, fwrapper,
         * fcell.getActiveSession());
         * 
         * Application app = null;
         * if (results.isSuccess()) {
         * app = Application.create(this, ApplicationType.Enterprise, appName, this.scope);
         * results.setResult(app);
         * this.addApplication(app);
         * }
         */
        OperationResults<Application> results = new OperationResults<Application>(false);
        Log.exiting(c, "install", results.isSuccess());
        return results;
    }

    /**
     * Add an Application to the internal Set
     *
     * @param app
     *            The Application to add
     */
    protected void addApplication(Application app) {
        if (this.applications == null) {
            this.applications = new HashSet<Application>();
        }
        this.applications.add(app);
    }

    /**
     * Performs the steps necessary to uninstall an application from the cell.
     *
     * @param appName The name of the application to uninstall.
     * @return An OperationResults instance containing information about the uninstall, including whether it was successful.
     * @throws Exception
     */
    public OperationResults<Boolean> uninstall(String appName) throws Exception {
        final String method = "uninstall";
        Log.entering(c, method, appName);

        if (!isInstalled(appName)) {
            throw new ApplicationNotInstalledException(appName);
        }

        //scope.getWorkspace().registerConfigChange(this, CHANGE_KEY_APPS, getApplications());

        uninstallApplication(appName, ApplicationType.EAR, false);

        this.removeApplication(this.getApplicationByName(appName));

        OperationResults<Boolean> results = new OperationResults<Boolean>(true);
        Log.exiting(c, method, results.isSuccess());
        return results;
    }

    /**
     * Remove an Application from the internal Set
     *
     * @param app
     *            The Application to remove
     * @throws Exception
     */
    protected void removeApplication(Application app) throws Exception {
        if (this.applications != null && app != null) {
            this.applications.remove(app);
        }
    }

    /**
     * For internal use only.
     */
    public void commit(HashMap<String, Object> values) throws Exception {}

    /**
     * For internal use only.
     */
    @SuppressWarnings("unchecked")
    public void rollback(HashMap<String, Object> values) throws Exception {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equals(CHANGE_KEY_APPS)) {
                this.applications = (Set) value;
            }
        }
    }

    public ApplicationManager(LibertyServer target) throws Exception {
        this.target = target;
    }

    public String getApplicationPath() {
        return target.getInstallRoot() + "/usr/shared/apps";
    }

    /**
     * Installs the provides app file into the appropriate Liberty destination.
     *
     * @param name The name of the application (currently unused)
     * @param app The application archive
     * @param type The application type (currently unused)
     * @throws Exception
     */
    public void installApplication(String name, RemoteFile app, ApplicationType type) throws Exception {
        installApplication(name, app, type, false);
    }

    /**
     * Installs the provides app file into the appropriate Liberty destination.
     *
     * @param name The name of the application (currently unused)
     * @param app The application archive
     * @param type The application type (currently unused)
     * @param restart Set to "true" to restart the server after installation
     * @throws Exception
     */
    public void installApplication(String name, RemoteFile app, ApplicationType type, boolean restart) throws Exception {
        //have moved the getDeployPath into the depolyApplication method so as to support .ZIP's
        deployApplication(name, app, type, restart);
    }

    public void uninstallApplication(String name, ApplicationType type, boolean restart) throws Exception {
        final String method = "uninstallApplication";

        String path = "";
        String appNameAsStoredByLibertyServer = name;

        if (type.equals(ApplicationType.ZIP)) {
            path = getDeployPath(type) + "/" + name;
        } else {
            path = getDeployPath(type) + "/" + name + "." + type.toString().toLowerCase();

        }

        Machine m = target.getMachine();
        RemoteFile app_target = m.getFile(path);

        Log.info(c, method, "Uninstalling Application "
                            + app_target.getAbsolutePath());

        if (restart) {
            target.stopServer();
        }
        VirtualArchiveFactory factory = new VirtualArchiveFactory(); // does not maintain state; no need to cache the factory
        VirtualArchive archive = factory.unmarshal(app_target);
        if (archive != null) {
            Log.info(c, method, "Virtual archive detected; need to delete physical entries before deleting the archive itself");
            factory.delete(m, archive);
            // FIXME: Danger! What if an unsuspecting tester writes a virtual archive with an entry where sourceOnDisk="C:/"?  When we uninstall the app, we'll try to recursively delete the whole file system!  Hopefully that won't happen.
        }
        if (!app_target.delete())
            throw new Exception("Unable to delete the target application");

        target.removeInstalledAppForValidation(appNameAsStoredByLibertyServer);

        if (restart) {
            target.startServer();
        }
    }

    public List<String> listApplications() throws Exception {
        List<String> ret = new ArrayList<String>();
        Machine m = target.getMachine();
        for (ApplicationType type : ApplicationType.values()) {
            RemoteFile dir = m.getFile(getDeployPath(type));
            RemoteFile[] list = dir.list(false);
            if (list != null) {
                for (RemoteFile file : list) {
                    String name = file.getName();
                    if (file.isDirectory()) { //&&
                        //!file.getName().contains("aries") &&
                        //!file.getName().contains("webcontainer")) {
                        name = this.removeExtension(name); // remove extension of application type (.ear/.war/etc)
                    } else { // a file
                        if (name.endsWith(".xml")) {
                            name = this.removeExtension(name); // remove extension of loose archive (.xml)
                        }
                        name = this.removeExtension(name); // remove extension of application type (.ear/.war/etc)
                    }
                    ret.add(name);
                }
            }
        }
        return ret;
    }

    protected String removeExtension(String fileName) {
        if (fileName.contains(".")) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    private String getDeployPath(ApplicationType type) {
        String path = null;
        switch (type) {
            case EBA:
                path = getApplicationPath() + "/aries";
                break;
            case WAR:
            case ZIP:
            case JS:
                //case Enterprise:
            case Asset:
            case EAR:
                path = getApplicationPath() + "/webcontainer";
                break;
        }
        return path;
    }

    private String getDeployPath(String name, ApplicationType type) {
        String path = getDeployPath(type);
        return path + "/" + name + "." + type.name().toLowerCase();
    }

    /**
     * Reads the bootstrapping.properties value for "fat.installType" to determine how applications should be installed. An "extract" install will unzip application archives during
     * deployment. A "file" install will deploy the application as an archive to liberty. A "loose" install will extract application arvhives using a loose configuration and
     * generate a virtual archive XML. The default (traditional) implementation is "extract".
     *
     * @return the installation type selected by the user. This field can be configured at the personal build level.
     */
    protected InstallType getInstallType() throws Exception {
        String userInstallType = Bootstrap.getInstance().getValue("fat.installType");
        if (userInstallType != null) {
            userInstallType = userInstallType.trim();
        }
        InstallType result = InstallType.NORMAL_EXTRACTION; // default to original implementation
        if ("loose".equalsIgnoreCase(userInstallType)) {
            result = InstallType.LOOSE_EXTRACTION;
        }
        if ("file".equalsIgnoreCase(userInstallType)) {
            result = InstallType.NORMAL_FILE;
        }
        if ("extract".equalsIgnoreCase(userInstallType)) {
            result = InstallType.NORMAL_EXTRACTION;
        }
        Log.info(c, "getInstallType", "Requested Install Type='" + userInstallType + "'; Using Install Type='" + result + "'");
        return result;
    }

    private void deployApplication(String name, RemoteFile app, ApplicationType type, boolean restart) throws Exception {
        final String method = "deployApplication";

        InstallType installType = this.getInstallType();

        /*
         * When using NORMAL_FILE copy, the message that is supposed to appear after install did not happen in time,
         * [junit] [10/11/2011 18:16:26:270 EDT] 000 LibertyServer validateAppsLoaded I Waiting for app loaded confirmations:
         * "CWWKZ0001I: The application webcontainer.watchdog has started successfully." to be found in
         * C:/WAS_Sandbox/RTCworkspace/build.image/wlp/usr/servers/webcontainer-8.0_tWAS_fat_server/logs/console.log
         * [junit] [10/11/2011 18:16:58:590 EDT] 000 LibertyServer validateAppsLoaded S Exception thrown confirming apps are loaded when validating that
         * C:/WAS_Sandbox/RTCworkspace/build.image/wlp/usr/servers/webcontainer-8.0_tWAS_fat_server/logs/console.log contains application install messages.
         * [junit] componenttest.exception.TopologyException: According to the logs, application webcontainer.watchdog was never installed.
         * 
         * This causes the first few (8-20) requests to fail.
         */
        //installType = InstallType.NORMAL_FILE;

        Machine m = target.getMachine();
        //boolean copied = true;

        String targetPath = getDeployPath(name, type);
        RemoteFile app_target = null;

        //RemoteFile app_temp_target = null;

        String installedAppName = name;
        //If it's a zip file unpack into a folder name excluding the .zip postfix
        if (type.equals(ApplicationType.ZIP)) {
            app_target = m.getFile(targetPath.substring(0, targetPath.length() - 4));
            boolean b = app_target.mkdirs();
            if (!b) {
                Log.info(c, method, "problem making dirs for ZIP : " + app_target);
            }
            if (installType == InstallType.NORMAL_FILE) {
                //change how to install a zip ... use extraction
                Log.info(c, method, "installing a zip via extraction");
                installType = InstallType.NORMAL_EXTRACTION;
            }
        } else {
            if ((type.equals(ApplicationType.EAR) || type.equals(ApplicationType.WAR))
                && (installType == InstallType.NORMAL_FILE || installType == InstallType.LOOSE_EXTRACTION)) {
                String targetPathParentDir = getDeployPath(type);
                app_target = m.getFile(targetPathParentDir);
                boolean b = app_target.mkdirs();
                if (!b) {
                    Log.info(c, method, "problem making dirs : " + app_target);
                }
                //app_target = m.getFile(targetPath.substring(0, targetPath.length() - 4));
                app_target = m.getFile(targetPath);
            } else {
                app_target = m.getFile(targetPath);
                boolean b = app_target.mkdirs();
                if (!b) {
                    Log.info(c, method, "problem making dirs : " + app_target);
                }
            }
        }

        switch (installType) {
            case NORMAL_FILE:
                installNormalFile(m, app_target, app, targetPath, type);
                break;
            case LOOSE_EXTRACTION:
                installLooseExtraction(app, app_target);
                break;
            case NORMAL_EXTRACTION:
            default:
                installNormalExtraction(m, app_target, app, targetPath);
                break;

        }

        //Need to log this app is installed on the server
        target.addInstalledAppForValidation(installedAppName);

        // Restart the server
        if (restart) {
            Log.info(c, method, "Restarting server as requested as have just installed " + app.getName());
            target.restartServer();
        }
    }

    private void installNormalFile(Machine m, RemoteFile app_target, RemoteFile app, String targetPath, ApplicationType type) throws Exception {
        final String method = "installNormalFile";

        Log.info(c, method, "installing an application via file : " + app.getAbsolutePath() + " to " + app_target.getAbsolutePath());
        if (!app.getAbsolutePath().equals(targetPath)) {
            app.copyToDest(app_target);
        } else {
            //I don't think we will ever get in here
            Log.info(c, method, "using app without copying : " + app.getAbsolutePath());
            //copied = false;
        }
    }

    /**
     * Originally, I wanted to use the server root directory to store physical entries from virtual archives, in order to auto-backup these entries during server stop. However,
     * this process is <b>very</b> time consuming, so I'm storing physical entries in the unmonitored app root instead.
     *
     * @return the parent directory of all physical entries of virtual archives
     */
    protected RemoteFile getLooseConfigRoot() {
        Machine libertyMachine = this.target.getMachine();
        String looseConfigParentDir = this.getApplicationPath();//this.target.getServerRoot();
        return new RemoteFile(libertyMachine, looseConfigParentDir + "/looseConfig");
    }

    private void installLooseExtraction(RemoteFile sourceArchive, RemoteFile destinationFile) throws Exception {
        RemoteFile physicalArchive = sourceArchive;
        RemoteFile virtualLocation = this.getLooseConfigRoot();
        RemoteFile virtualArchive = destinationFile;
        VirtualArchiveFactory factory = new VirtualArchiveFactory(); // does not maintain state; no need to cache the factory
        factory.extract(physicalArchive, virtualLocation, virtualArchive);
    }

    private void installNormalExtraction(Machine m, RemoteFile app_target, RemoteFile app, String targetPath) throws Exception {
        final String method = "installNormalExtraction";

        boolean copied = true;

        RemoteFile app_temp_target = m.getFile(target.getInstallRoot() + "/simplicityTemp");
        app_temp_target.mkdirs();

        if (!app.getParentFile().getAbsolutePath().equals(targetPath)) {
            //use this temporary directory so there is no lock held on the file when it's dropped in the apps directory
            app.copyToDest(app_temp_target);
        } else {
            //I don't think we will ever get in here
            Log.info(c, method, "using app without copying : " + app.getAbsolutePath());
            copied = false;
            app_temp_target = app.getParentFile();
        }

        RemoteFile app_remote = m.getFile(app_temp_target, app.getName());

        // Unzip and delete the file
        // Use a single string instead of parameters for the command or it doesn't work so well on local linux because
        // of how LocalProvider works
        String command = m.getFile(target.getMachineJavaJarCommandPath()).getAbsolutePath();
        String[] parameters = { "xf", "\"" + app_remote.getAbsolutePath() + "\"" };
        ProgramOutput unzipEARoutput = m.execute(command, parameters, app_temp_target.getAbsolutePath());
        if (unzipEARoutput.getReturnCode() != 0) {
            throw new IOException(unzipEARoutput.getCommand() + " reported " + unzipEARoutput.getStderr());
        }

        if (copied) {
            if (!app_remote.delete()) {
                Log.info(c, method, "failure to delete temporary file: " + app_remote.getAbsolutePath());
            }
            //if (!(app_temp_target.delete())) {
            //    Log.info(c, method, "failure to delete temporary file: " + app_temp_target.getAbsolutePath());
            //}
        }

        // Iterate over all WAR files, unzipping them
        // Using jar to unzip means we can run this on different platforms but
        // makes it harder to unzip as we can't create a .war dir in the same dir as the actual war file.
        // Therefore we need to create a temp WAR file dir to use while we create the .war dirs and then
        // unzip the wars into their respective dirs
        //final String TEMP_WAR_DIR = "/tempWARS/";
        //RemoteFile tempWARFileDir = null;
        RemoteFile[] wars = app_temp_target.list(false);
        boolean recursive = true;
        boolean overwrite = true;
        for (RemoteFile war : wars) {
            if (war.isDirectory() || !war.getName().toLowerCase().endsWith(".war")) {
                //recursively move everything except war files.  We are going to extract war files.
                Log.info(c, method, "copying " + war.getAbsolutePath() + "to dest " + app_target.getAbsolutePath());
                RemoteFile newDir = null;
                if (war.isDirectory()) {
                    newDir = new RemoteFile(m, app_target, war.getName());
                    newDir.mkdirs();
                } else {
                    //must be a jar or something
                    //don't extract - just copy into this dir
                    newDir = app_target;
                }

                Log.info(c, method, "copying " + war.getAbsolutePath() + "to dest " + newDir.getAbsolutePath());
                war.copyToDest(newDir, recursive, overwrite);
                war.delete();
                continue;
            }

            //tempWARFileDir = m.getFile(targetPath + TEMP_WAR_DIR);
            //tempWARFileDir.mkdirs();

            String warname = war.getName();
            //String warAbsPath = app_target.getAbsolutePath(), warname;
            //war.getAbsolutePath();

            //Copy the WAR to the temp Dir
            //war.copyToDest(tempWARFileDir);
            //Now delete it from it's current location
            //war.delete();

            //Create the dir that will hold the contents of the unzipped WAR
            RemoteFile newWARDir = new RemoteFile(m, app_target, warname);
            newWARDir.mkdirs();

            //RemoteFile newWARFile = m.getFile(targetPath + TEMP_WAR_DIR + warname);

            //Unzip the WAR
            parameters = new String[] { "xf", war.getAbsolutePath() };
            ProgramOutput warUnzipOutput = m.execute(command, parameters, newWARDir.getAbsolutePath());
            if (warUnzipOutput.getReturnCode() != 0) {
                throw new IOException(warUnzipOutput.getCommand() + " reported " + warUnzipOutput.getStderr());
            }
            war.delete();
        }

        //Now we can delete the tempWAR folder and all it's contents
        //if (tempWARFileDir != null) {
        //    tempWARFileDir.delete();
        //}

        if (copied) {
            if (!(app_temp_target.delete())) {
                Log.info(c, method, "failure to delete temporary directory: " + app_temp_target.getAbsolutePath());
            }
        }
    }
}
