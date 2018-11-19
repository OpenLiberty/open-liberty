/*******************************************************************************
 * Copyright (c) 2013, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal.commands;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ibm.ws.kernel.boot.BootstrapConfig;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.EmbeddedServerImpl;
import com.ibm.ws.kernel.boot.LaunchArguments;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.BootstrapConstants;
import com.ibm.ws.kernel.boot.internal.FileUtils;
import com.ibm.ws.kernel.boot.internal.ServerLock;
import com.ibm.ws.kernel.boot.internal.commands.ArchiveProcessor.Pair;
import com.ibm.ws.kernel.boot.internal.commands.PackageProcessor.PackageOption;
import com.ibm.ws.kernel.boot.logging.TextFileOutputStreamFactory;
import com.ibm.wsspi.kernel.embeddable.Server;
import com.ibm.wsspi.kernel.embeddable.ServerBuilder;

/**
 *
 */
public class PackageCommand {
    final String serverName;
    final File wlpOutputRoot;
    final String serverConfigDir;
    final String serverOutputDir;
    final String osRequest;
    final BootstrapConfig bootProps;
    final String includeOption;
    final String archiveOption;
    final String rootOption;
    private final static List<String> SUPPORTED_EXTENSIONS = new ArrayList<>();

    static {
      SUPPORTED_EXTENSIONS.add(".zip");
      SUPPORTED_EXTENSIONS.add(".jar");
      SUPPORTED_EXTENSIONS.add(".pax");
      SUPPORTED_EXTENSIONS.add(".tar");
      SUPPORTED_EXTENSIONS.add(".tar.gz");
    }

    public PackageCommand(BootstrapConfig bootProps, LaunchArguments launchArgs) {
        this.serverName = bootProps.getProcessName();
        this.bootProps = bootProps;

        // Use the system property bootstrap config set: all conversion/endings will be the same
        serverConfigDir = bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVCFG_DIR);
        serverOutputDir = bootProps.get(BootstrapConstants.LOC_PROPERTY_SRVOUT_DIR);
        wlpOutputRoot = bootProps.getCommonOutputRoot();

        osRequest = launchArgs.getOption("os");
        includeOption = launchArgs.getOption(BootstrapConstants.CLI_PACKAGE_INCLUDE_VALUE);
        archiveOption = launchArgs.getOption(BootstrapConstants.CLI_ARG_ARCHIVE_TARGET);
        rootOption = launchArgs.getOption(BootstrapConstants.CLI_ROOT_PACKAGE_NAME);

    }

    /**
     * Package the server
     *
     * @return
     */
    public ReturnCode doPackage() {
        boolean isClient = bootProps.getProcessType() == BootstrapConstants.LOC_PROCESS_TYPE_CLIENT;
        if (isClient) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.clientPackaging"), serverName));
        } else {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverPackaging"), serverName));
        }

        // Use initialized bootstrap configuration to find the server lock file.
        ServerLock serverLock = ServerLock.createServerLock(bootProps);

        ReturnCode packageRc = ReturnCode.PACKAGE_ACTION;

        // create timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yy.MM.dd_HH.mm.ss");
        Date date = new Date();
        String packageTimestamp = sdf.format(date);

        File archive = null;
        if (serverLock.testServerRunning()) {
            packageRc = ReturnCode.SERVER_ACTIVE_STATUS;
        } else {
            archive = getArchive(serverName, new File(serverOutputDir));

            //generate package txt
            File packageInfoFile = new File(serverOutputDir, BootstrapConstants.SERVER_PACKAGE_INFO_FILE_PREFIX + packageTimestamp + ".txt");
            generatePackageInfo(packageInfoFile, serverName, date);

            packageRc = packageServerRuntime(archive, false);

            //clean up package txt
            FileUtils.recursiveClean(packageInfoFile);
        }

        if (isClient) {
            if (packageRc == ReturnCode.OK) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.clientPackageComplete"), serverName, archive.getAbsolutePath()));
            } else if (packageRc == ReturnCode.SERVER_ACTIVE_STATUS) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.clientIsRunning"), serverName));
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.clientPackageUnreachable"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.clientPackageException"), serverName));
            }
        } else {
            if (packageRc == ReturnCode.OK) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverPackageComplete"), serverName, archive.getAbsolutePath()));
            } else if (packageRc == ReturnCode.SERVER_ACTIVE_STATUS) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverIsRunning"), serverName));
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverPackageUnreachable"), serverName));
            } else {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverPackageException"), serverName));
            }
        }

        return packageRc;
    }

    /**
     * @return
     */
    private File getArchive(String archiveBaseName, File outputDir) {
        File archive;
        String packageTarget;

        String defaultExtension = getDefaultPackageExtension();

        if (archiveOption == null || archiveOption.isEmpty()) {
            packageTarget = archiveBaseName + "." + defaultExtension;
//            archive = new File(outputDir, packageTarget);
        } else {
            int index = archiveOption.lastIndexOf(".");
            if (index > 0 && isSupportedExtension(archiveOption)) {
                packageTarget = archiveOption;
            } else {
                packageTarget = archiveOption + '.' + defaultExtension;
            }
        }

        archive = new File(packageTarget);
        if (!archive.isAbsolute()) {
            archive = new File(outputDir, packageTarget);
        }
        return archive;
    }

    public boolean isSupportedExtension(String fileName) {
        fileName = fileName.toLowerCase();
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Package the server
     *
     * @return
     */
    public ReturnCode doPackageRuntimeOnly() {
        System.out.println(BootstrapConstants.messages.getString("info.runtimePackaging"));

        File archive = getArchive("wlp", wlpOutputRoot);

        ReturnCode packageRc = packageServerRuntime(archive, true);

        if (packageRc == ReturnCode.OK) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.runtimePackageComplete"), archive.getAbsolutePath()));
        } else {
            System.out.println(BootstrapConstants.messages.getString("info.runtimePackageException"));
        }

        return packageRc;
    }

    /*
     * Return true for include values of:
     * include=minify
     * include=minify,runnable
     *
     * Otherwise return false.
     */
    private boolean includeMinifyorMinifyRunnable(String val) {
        return PackageProcessor.IncludeOption.MINIFY.matches(val);
    }

    public ReturnCode packageServerRuntime(File packageFile, boolean runtimeOnly) {
        //if package options request minify, we need to launch a mini-liberty to recover the currently
        //configured features, and associated files.
        String includeValue = includeOption;
        Set<String> libertyFiles = null;
        if (includeMinifyorMinifyRunnable(includeValue)) {
            try {
                libertyFiles = getMinifyPathsForPackage();
            } catch (FailedWithReturnCodeException fwrc) {
                //message has already been output by method..
                return fwrc.getReturnCode();
            } catch (FileNotFoundException fnf) {
                //fnf is used by queryFeatureInformation to convey that requested content was found missing..
                Debug.printStackTrace(fnf);
                String osMsg = osRequest;
                if (osMsg == null)
                    osMsg = "any";
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("unable.to.package.missing.file"), serverName, osMsg, fnf.getMessage()));
                return ReturnCode.ERROR_SERVER_PACKAGE;
            } catch (IOException e) {
                //io exception is a little more generic.. but still means bad stuff happened.
                Debug.printStackTrace(e);
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.unable.to.package"), serverName, e));
                return ReturnCode.ERROR_SERVER_PACKAGE;
            }

            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverPackagingBuildingArchive"),
                                                    serverName));

        } else {
            if (osRequest != null) {
                System.out.println(BootstrapConstants.messages.getString("error.os.without.include"));
                return ReturnCode.ERROR_SERVER_PACKAGE;
            }
        }

        PackageProcessor processor;

        if (null != includeValue) {
            List<Pair<PackageOption, String>> options = new ArrayList<Pair<PackageOption, String>>();
            options.add(new Pair<PackageOption, String>(PackageOption.INCLUDE, includeValue));
            processor = new PackageProcessor(serverName, packageFile, bootProps, options, libertyFiles);
        } else {
            processor = new PackageProcessor(serverName, packageFile, bootProps, null, libertyFiles);
        }

        if (rootOption != null) {
            if (processor.hasProductExtentions() && rootOption.trim().equalsIgnoreCase("")) {
                System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.package.root.invalid"),
                                                        serverName));
            } else {
                if (!PackageProcessor.IncludeOption.RUNNABLE.matches(includeOption)) {
                    processor.setArchivePrefix(rootOption);
                } else {
                    // --server-root and --include=runnable is not a valid combo, thus --server-root will be ignored.
                    System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("warning.package.root.invalid.runnable"),
                                                            serverName));
                }
            }
        }

        //this will now go onto do the package, libertyFiles will be non-null in the minify case only.
        return processor.execute(runtimeOnly);
    }

    private void generatePackageInfo(File packageInfoFile, String serverName, Date date) {
        BufferedWriter writer = null;
        try {
            FileOutputStream packageInfoStream = TextFileOutputStreamFactory.createOutputStream(packageInfoFile);
            writer = new BufferedWriter(new OutputStreamWriter(packageInfoStream));
            String includeValue = includeOption;
            if (null != includeValue) {
                if (null != osRequest) {
                    writer.append("Package server " + serverName + " with include option \"" + includeValue + "\" and os " + osRequest + " at " + date.toString() + ".\n");
                } else {
                    writer.append("Package server " + serverName + " with include option \"" + includeValue + "\" at " + date.toString() + ".\n");
                }
            } else {
                writer.append("Package server " + serverName + " all at " + date.toString() + ".\n");
            }

        } catch (IOException e) {
            Debug.printStackTrace(e);
        } finally {
            Utils.tryToClose(writer);
        }
    }

    /**
     * Determine the default package format for the current operating system.
     *
     * @return "pax" on z/OS and "zip" for all others
     */
    private String getDefaultPackageExtension() {

        // Default package format on z/OS is a pax
        if ("z/OS".equalsIgnoreCase(bootProps.get("os.name"))) {
            return "pax";
        }

        if (PackageProcessor.IncludeOption.RUNNABLE.matches(includeOption)) {
            return "jar";
        }

        return "zip";
    }

    private static class FailedWithReturnCodeException extends Exception {
        private static final long serialVersionUID = 1L;
        private final ReturnCode rc;

        FailedWithReturnCodeException(ReturnCode rc) {
            this.rc = rc;
        }

        ReturnCode getReturnCode() {
            return this.rc;
        }
    }

    private Set<String> getMinifyPathsForPackage() throws FileNotFoundException, IOException, FailedWithReturnCodeException {
        Set<String> results = null;

        //tell user we are collecting.
        System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("info.serverPackagingCollectingInformation"),
                                                serverName));
        EmbeddedServerImpl server = (EmbeddedServerImpl) (new ServerBuilder()).setName(serverName).setOutputDir(new File(serverOutputDir).getParentFile()).setUserDir(bootProps.getUserRoot())
                        //.setServerEventListener(this)
                        .build();

        //add in the 'do not pass go' property.
        //this will prevent the server raising the start level, preventing features starting
        //but allowing the kernel to be alive enough for us to query the configured features.
        Map<String, String> minifyServerProps = new HashMap<String, String>();
        minifyServerProps.put(BootstrapConstants.REQUEST_SERVER_CONTENT_PROPERTY, "1.0.0");
        Future<Server.Result> launchFuture = server.start(minifyServerProps);
        boolean startProblem = false;
        try {
            // We're only partially starting the server, so this should complete
            // very quickly, but use a timeout in case something hangs.  Use a
            // timeout that should be excessive even on very slow machines.
            Server.Result rc = launchFuture.get(10, TimeUnit.MINUTES);
            if (rc == null || !rc.successful()) {
                if (rc != null) {
                    Debug.println(rc.getReturnCode());
                    if (rc.getException() != null) {
                        Debug.printStackTrace(rc.getException());
                    }
                }
                startProblem = true;
            }
        } catch (TimeoutException to) {
            Debug.printStackTrace(to);
            startProblem = true;
        } catch (InterruptedException e) {
            Debug.printStackTrace(e);
            startProblem = true;
        } catch (ExecutionException e) {
            Debug.printStackTrace(e);
            startProblem = true;
        }
        if (startProblem) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.minify.unable.to.start.server"),
                                                    serverName));
            throw new FailedWithReturnCodeException(ReturnCode.ERROR_SERVER_PACKAGE);
        }

        //invoke the getServerContent method & remember the result..
        //NOTE: will throw FNF / IOException if mandatory content is absent.
        //      this will ripple up to caller who will handle appropriately.
        results = server.getServerContent(osRequest);

        //results can be null if the stars didn't quite align to allow the getServerContent call.
        if (null == results) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.minify.unable.to.determine.features"),
                                                    serverName));
            throw new FailedWithReturnCodeException(ReturnCode.ERROR_SERVER_PACKAGE);
        }

        Future<Server.Result> stopFuture = server.stop();
        boolean stopProblem = false;
        try {
            Server.Result rc = stopFuture.get(60, TimeUnit.SECONDS);
            if (rc == null || !rc.successful()) {
                if (rc != null) {
                    Debug.println(rc.getReturnCode());
                    if (rc.getException() != null) {
                        Debug.printStackTrace(rc.getException());
                    }
                }
                stopProblem = true;
            }
        } catch (TimeoutException to) {
            Debug.printStackTrace(to);
            stopProblem = true;
        } catch (InterruptedException e) {
            Debug.printStackTrace(e);
            stopProblem = true;
        } catch (ExecutionException e) {
            Debug.printStackTrace(e);
            stopProblem = true;
        }
        if (stopProblem) {
            System.out.println(MessageFormat.format(BootstrapConstants.messages.getString("error.minify.unable.to.stop.server"),
                                                    serverName));
            throw new FailedWithReturnCodeException(ReturnCode.ERROR_SERVER_PACKAGE);
        }
        return results;
    }

}
