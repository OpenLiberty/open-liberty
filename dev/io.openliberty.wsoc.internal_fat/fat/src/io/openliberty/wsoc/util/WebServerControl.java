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
package io.openliberty.wsoc.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.ibm.websphere.simplicity.ConnectionInfo;
import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.OperatingSystem;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.websphere.simplicity.runtime.ProcessStatus;
import com.ibm.ws.fat.util.Props;
import componenttest.exception.TopologyException;

public class WebServerControl {
    private static final Class<?> c = WebServerControl.class;
    private static Boolean webserverInFront = false;
    private static Machine machine = null;
    private static ProcessStatus status = null;
    private static String workdir = null;
    private static RemoteFile apacheInstallRoot;
    private static RemoteFile pluginInstallRoot;

    //obtained from properties
    private static String hostname; //defaults to localhost
    private static String port; //defaults to 80
    private static String name; //defaults to webserver1
    private static String ihsDir;
    private static String plgDir;
    private static String rtcHost;

    /**
     * @return
     * @throws Exception
     */
    public static Boolean isWebserverInFront() {
        try {
            getMachine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return webserverInFront;
    }

    public static String getHostname() throws Exception {
        getMachine();
        return hostname;
    }

    public static String getPort() throws Exception {
        getMachine();
        return port;
    }

    public static String getPlgDir() throws Exception {
        getMachine();
        return plgDir;
    }

    public static int getSecurePort() {
        return 443;
    }

    public static ProcessStatus getStatus() throws Exception {
        getMachine();
        return status;
    }

    public static RemoteFile getPluginInstallRoot() throws Exception {
        getMachine();
        return pluginInstallRoot;
    }

    public static String getLogPath() throws Exception {
        if (Machine.getLocalMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)
            && rtcHost.equals("localhost")) // TODO: better way to identify ihs target platform
            return plgDir + "\\logs\\" + WebServerControl.getName() + "\\http_plugin.log";
        else
            return plgDir + "/logs/" + WebServerControl.getName() + "/http_plugin.log";
    }

    public static String getRtcHost() throws Exception {
        getMachine();
        return rtcHost;
    }

    /**
     * @return name
     */
    public static String getName() throws Exception {
        getMachine();
        return name;
    }

    public static Machine getMachine() throws Exception {
        if (machine != null)
            return machine;

        /* required, unless not webserver */
        plgDir = Props.getInstance().getProperty("ihs.websphere.plugins.dir");
        if (plgDir.equals(""))
            return null;

        /* required, unless not webserver */
        ihsDir = Props.getInstance().getProperty("ihs.install.dir");
        if (ihsDir.equals(""))
            return null;

        /* optional, defaults to localhost */
        hostname = Props.getInstance().getProperty("ihs.host");
        if (hostname.equals(""))
            hostname = "localhost";

        /* optional, defaults to 80 */
        port = Props.getInstance().getProperty("ihs.port");
        if (port.equals(""))
            port = "80";

        /* optional, defaults to webserver1 */
        name = Props.getInstance().getProperty("ihs.websphere.plugins.config.name");
        if (name.equals(""))
            name = "webserver1";

        Log.info(c, "getMachine", "Plugin Install Directory: " + plgDir + "IHS install directory" + ihsDir);
        /*
         * auth options:
         * - local
         * - remote user + pass
         * - remote user + keystore
         */
        String user = Props.getInstance().getProperty("ihs.user");
        String pass = Props.getInstance().getProperty("ihs.pass");
        ConnectionInfo connInfo = null;
        if (!user.equals("") && pass.equals("")) {
            String keystorePath = Props.getInstance().getProperty("keystore");
            if (!keystorePath.equals("")) {
                File keystore = new File(keystorePath);
                connInfo = new ConnectionInfo(hostname, keystore, user, null);
            }
            else
                return null;
        } else
            connInfo = new ConnectionInfo(hostname, user, pass);

        machine = Machine.getMachine(connInfo);
        machine.connect();

        Log.info(c, "getMachine", machine.getHostname() + (machine.isConnected() ? " is " : " is NOT ") + "connected");

        //TODO: we need a better way to determine Machine for Liberty server(s)
        //      for now just assume Liberty server is co-located with junit execution
        //      when running with a non-local webserver create the rtc.host property
        //      with a routable address and use this so webserver can route to it.
        rtcHost = Props.getInstance().getProperty("rtc.host");
        if (rtcHost.equals("") || machine.isLocal())
            rtcHost = "localhost";

        workdir = ihsDir + "/bin";
        webserverInFront = true;

        Log.info(c, "getMachine", "WebServer In Front is:" + webserverInFront);

        apacheInstallRoot = new RemoteFile(machine, ihsDir);
        pluginInstallRoot = new RemoteFile(machine, plgDir);

        //TODO: determine real process status
        status = ProcessStatus.STOPPED;

        return machine;
    }

    public static void deployPluginConfigurationFile(File cfgFile) throws Exception {
        RemoteFile plugincfgfile = new RemoteFile(getPluginInstallRoot(), "config/" + name + "/" + cfgFile.getName());
        LocalFile localplugincfg = new LocalFile(cfgFile.getAbsolutePath());

        localplugincfg.copyToDest(plugincfgfile, false, true);
    }

    public static void copyLocalPluginConfig(File orig, File dest) throws Exception {
        LocalFile origFile = new LocalFile(orig.getAbsolutePath());
        LocalFile newFile = new LocalFile(dest.getAbsolutePath());

        origFile.copyToDest(newFile);

    }

    public static void startWebServer() throws Exception {
        String command;
        if (!getStatus().equals(ProcessStatus.RUNNING)) {
            if (getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
                command = workdir + "\\apache -k start -n ihs";
            } else {
                command = workdir + "/apachectl start";
            }
            ProgramOutput po = machine.execute(command, workdir);
            Log.info(c, "startWebServer:", "rc==>" + po.getReturnCode());
            Log.info(c, "startWebServer:stdout:", po.getStdout());
            Log.info(c, "startWebServer:stderr:", po.getStderr());

            //TODO: check status
            status = ProcessStatus.RUNNING;
        }
    }

    public static void stopWebServer(String serverName) throws Exception {
        String command;
        if (!getStatus().equals(ProcessStatus.STOPPED)) {
            if (getMachine().getOperatingSystem().equals(OperatingSystem.WINDOWS)) {
                command = workdir + "\\apache -k stop -n ihs";
            } else {
                command = workdir + "/apachectl stop";
            }
            ProgramOutput po = machine.execute(command, workdir);
            Log.info(c, "stopWebServer:", "rc==>" + po.getReturnCode());
            Log.info(c, "stopWebServer:stdout:", po.getStdout());
            Log.info(c, "stopWebServer:stderr:", po.getStderr());

            status = ProcessStatus.STOPPED;

            //TODO: check status
            postStopServerArchive(serverName);
        }
    }

    /**
     * This method is used to archive server logs after a stopServer.
     * This is particularly required for tWAS FAT buckets as it is not known
     * when these finish, using this method will ensure logs are collected.
     * Also, this will stop the server log contents being lost (over written) in a restart case.
     */
    static public void postStopServerArchive(String serverName) throws Exception {
        final String method = "postStopServerArchive";

        Log.info(c, method, "Moving logs & config to the output folder");
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        Date d = new Date(System.currentTimeMillis());
        RemoteFile toCopy;
        LocalFile toReceive;

        /*
         * autoFVT
         */
        String pathToAutoFVTOutputFolder = "output/servers";
        String serverToUse = serverName + "-webserver1";
        String logDirectoryName = pathToAutoFVTOutputFolder + "/" + serverToUse + "-" + sdf.format(d);

        /*
         * Apache
         */
        LocalFile logFolder = new LocalFile(logDirectoryName + "/apache");
        toCopy = new RemoteFile(machine, apacheInstallRoot, "conf/httpd.conf");
        toReceive = new LocalFile(logFolder, "httpd.conf");
        toReceive.copyFromSource(toCopy);

        // Copy the log files: try to move them instead if we can
        RemoteFile serverlogs = new RemoteFile(machine, apacheInstallRoot, "logs");
        recursivelyCopyDirectory(serverlogs, logFolder, true, true, true);

        /*
         * Plugin
         */
        logFolder = new LocalFile(logDirectoryName + "/plugin");
        toCopy = new RemoteFile(machine, pluginInstallRoot, "config/" + name + "/plugin-cfg.xml");
        toReceive = new LocalFile(logFolder, "plugin-cfg.xml");
        toReceive.copyFromSource(toCopy);

        // Copy the log files: try to move them instead if we can
        RemoteFile pluginlogs = new RemoteFile(machine, pluginInstallRoot, "logs/" + name);
        recursivelyCopyDirectory(pluginlogs, logFolder, true, true, true);

        Log.exiting(c, method);
    }

    public void preStartServerLogsTidy() {
        //stop
        //tidy
    }

    private static void recursivelyCopyDirectory(RemoteFile remoteDirectory, LocalFile destination, boolean ignoreFailures, boolean skipArchives, boolean moveFile) throws Exception {
        String method = "recursivelyCopyDirectory";
        Log.entering(c, method);
        destination.mkdirs();

        ArrayList<String> logs = new ArrayList<String>();
        logs = listDirectoryContents(remoteDirectory, null);
        for (String l : logs) {
            RemoteFile toCopy = new RemoteFile(machine, remoteDirectory, l);
            LocalFile toReceive = new LocalFile(destination, l);
            Log.info(c, "recursivelyCopyDirectory", "Getting: " + toCopy.getAbsolutePath());

            if (toCopy.isDirectory()) {
                // Recurse
                //recursivelyCopyDirectory(toCopy, toReceive, ignoreFailures, skipArchives, moveFile);
            } else {
                try {
                    // We're only going to attempt to move log files. Because of ffdc log checking, we
                    // can't move those. But we should move other log files.. 
                    boolean isLog = (toCopy.getAbsolutePath().contains("logs") && !toCopy.getAbsolutePath().contains("ffdc"));

                    if (moveFile && isLog) {
                        boolean copied = false;
                        boolean moved = false;

                        // If we're local, try to rename the file instead.. 
                        if (machine.isLocal() && toCopy.rename(toReceive)) {
                            moved = true; // well, we moved it, but it counts.
                            Log.info(c, "recursivelyCopyDirectory", "MOVE: " + l + " to " + toReceive.getAbsolutePath());
                        }

                        if (!moved && toReceive.copyFromSource(toCopy)) {
                            copied = true;
                            // copy was successful, clean up the source log
                            toCopy.delete();
                            Log.info(c, "recursivelyCopyDirectory", "No MOVE, now COPY: " + l + " to " + toReceive.getAbsolutePath());
                        }
                    } else {
                        toReceive.copyFromSource(toCopy);
                        Log.info(c, "recursivelyCopyDirectory", "COPY: " + l + " to " + toReceive.getAbsolutePath());
                    }
                } catch (Exception e) {
                    Log.info(c, "recursivelyCopyDirectory", "unable to copy or move " + l + " to " + toReceive.getAbsolutePath());
                    // Ignore on request and carry on copying the rest of the files
                    if (!ignoreFailures) {
                        throw e;
                    }
                }
            }

        }
        Log.exiting(c, method);
    }

    private static ArrayList<String> listDirectoryContents(RemoteFile serverDir, String fileName) throws Exception {

        final String method = "serverDirectoryContents";
        String s = "The specified directoryPath \'"
                   + serverDir.getAbsolutePath() + "\' ";
        Log.entering(c, method);
        if (serverDir.exists()
            && !serverDir.isDirectory()
            && !serverDir.isFile()) {
            Log.info(c, "listDirectoryContents", "serverDir exists & !Dir & !File !?  Recreate serverDir,retry and hope for the best...");
            serverDir = new RemoteFile(machine, serverDir.getAbsolutePath());
        }

        if (!serverDir.isDirectory() || !serverDir.exists())
            throw new TopologyException(s + "was not a directory");

        RemoteFile[] firstLevelFiles = serverDir.list(false);
        ArrayList<String> firstLevelFileNames = new ArrayList<String>();

        for (RemoteFile f : firstLevelFiles) {
            if (!f.isFile())
                continue;

            if (fileName == null) {
                firstLevelFileNames.add(f.getName());
            } else if (f.getName().contains(fileName)) {
                firstLevelFileNames.add(f.getName());

            }
        }

        return firstLevelFileNames;
    }

}
