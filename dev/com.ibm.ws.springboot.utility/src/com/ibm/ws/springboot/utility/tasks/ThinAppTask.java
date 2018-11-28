/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.springboot.utility.tasks;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.ibm.ws.app.manager.springboot.internal.SpringConstants;
import com.ibm.ws.app.manager.springboot.util.SpringBootThinUtil;
import com.ibm.ws.springboot.utility.IFileUtility;
import com.ibm.ws.springboot.utility.SpringBootUtilityReturnCodes;
import com.ibm.ws.springboot.utility.utils.ConsoleWrapper;

/**
 *
 */
public class ThinAppTask extends BaseCommandTask {
    static final String SLASH = String.valueOf(File.separatorChar);

    static final String ARG_SOURCE_APP = "--sourceAppPath";
    static final String ARG_DEST_THIN_APP = "--targetThinAppPath";
    static final String ARG_DEST_LIB_CACHE = "--targetLibCachePath";
    static final String ARG_PARENT_LIB_CACHE = "--parentLibCachePath";
    static final Set<String> knownArguments = new HashSet<>(Arrays.asList(ARG_SOURCE_APP, ARG_DEST_THIN_APP, ARG_DEST_LIB_CACHE, ARG_PARENT_LIB_CACHE));

    private final IFileUtility fileUtility;
    protected ConsoleWrapper stdin;
    protected PrintStream stdout;
    protected PrintStream stderr;

    public ThinAppTask(IFileUtility fileUtility, String scriptName) {
        super(scriptName);
        this.fileUtility = fileUtility;

    }

    /** {@inheritDoc} */
    @Override
    public String getTaskName() {
        return "thin";
    }

    @Override
    public String getTaskDescription() {
        return getOption("thin.desc", true);
    }

    /** {@inheritDoc} */
    @Override
    public String getTaskHelp() {
        return getTaskHelp("thin.desc", "thin.usage.options",
                           "thin.required-key.", "thin.required-desc.",
                           "thin.optional-key.", "thin.optional-desc.",
                           scriptName);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SpringBootUtilityReturnCodes handleTask(ConsoleWrapper stdin, PrintStream stdout, PrintStream stderr, String[] args) throws Exception {
        this.stdin = stdin;
        this.stdout = stdout;
        this.stderr = stderr;

        validateArgumentList(args);

        String sourceAppPath = getArgumentValue(ARG_SOURCE_APP, args);
        String destThinAppPath = getArgumentValue(ARG_DEST_THIN_APP, args);
        String destLibCachePath = getArgumentValue(ARG_DEST_LIB_CACHE, args);
        String parentLibCachePath = getArgumentValue(ARG_PARENT_LIB_CACHE, args);

        if (!fileUtility.isFile(sourceAppPath)) {
            stdout.println(getMessage("thin.abort"));
            stdout.println(getMessage("thin.appNotFound", sourceAppPath));
            return SpringBootUtilityReturnCodes.ERR_APP_NOT_FOUND;
        }
        File sourceAppFile = new File(sourceAppPath).getAbsoluteFile();

        if (destThinAppPath == null) {
            String appName = sourceAppFile.getName();
            int lastDot = appName.lastIndexOf('.');
            if (lastDot > 0) {
                appName = appName.substring(0, lastDot + 1) + SpringConstants.SPRING_APP_TYPE;
            } else {
                appName += "." + SpringConstants.SPRING_APP_TYPE;
            }
            destThinAppPath = (sourceAppFile.getParent() == null ? "" : sourceAppFile.getParent()) + File.separator + appName;
        }
        if (fileUtility.isDirectory(destThinAppPath)) {
            stdout.println(getMessage("thin.abort"));
            stdout.println(getMessage("thin.appTargetIsDirectory", destThinAppPath));
            return SpringBootUtilityReturnCodes.ERR_APP_DEST_IS_DIR;
        }
        File thinAppFile = new File(destThinAppPath).getAbsoluteFile();
        if (!fileUtility.mkDirs(thinAppFile.getParentFile(), stdout)) {
            return SpringBootUtilityReturnCodes.ERR_MAKE_DIR;
        }

        if (destLibCachePath == null) {
            destLibCachePath = (sourceAppFile.getParent() == null ? "" : sourceAppFile.getParent()) + File.separator + SpringConstants.SPRING_LIB_CACHE_NAME;
        }
        if (fileUtility.isFile(destLibCachePath)) {
            stdout.println(getMessage("thin.abort"));
            stdout.println(getMessage("thin.libCacheIsFile", destLibCachePath));
            return SpringBootUtilityReturnCodes.ERR_LIB_DEST_IS_FILE;
        }
        File libCache = new File(destLibCachePath).getAbsoluteFile();
        if (!fileUtility.mkDirs(libCache, stdout)) {
            return SpringBootUtilityReturnCodes.ERR_MAKE_DIR;
        }
        File parentLibCache = null;
        if (parentLibCachePath != null) {
            parentLibCache = new File(parentLibCachePath).getAbsoluteFile();
            if (!fileUtility.mkDirs(parentLibCache, stdout)) {
                return SpringBootUtilityReturnCodes.ERR_MAKE_DIR;
            }
        }

        performThinTask(sourceAppFile, thinAppFile, libCache, parentLibCache);

        return SpringBootUtilityReturnCodes.OK;
    }

    /**
     * @param serverDir
     * @param applicationFile
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private void performThinTask(File applicationFile, File thinAppFile, File libCache, File parentLibCache) throws IOException, NoSuchAlgorithmException {
        stdout.println(getMessage("thin.creating", applicationFile.getAbsolutePath()));

        try (SpringBootThinUtil thinUtil = new SpringBootThinUtil(applicationFile, thinAppFile, libCache, parentLibCache)) {
            thinUtil.execute();
        }
        stdout.println(getMessage("thin.libraryCache", libCache));
        stdout.println(getMessage("thin.applicationLoc", thinAppFile.getAbsolutePath()));
    }

    /** {@inheritDoc} */
    @Override
    boolean isKnownArgument(String arg) {
        return knownArguments.contains(arg);
    }

    /** {@inheritDoc} */
    @Override
    void checkRequiredArguments(String[] args) {
        String message = "";
        // We expect at least one arguments and the task name
        if (args.length < 2) {
            message = getMessage("insufficientArgs");
        }

        boolean sourceAppFound = false;
        for (String arg : args) {
            if (arg.startsWith(ARG_SOURCE_APP)) {
                sourceAppFound = true;
            }
        }
        if (!sourceAppFound) {
            //missingArg need --sourceAppPath
            message += " " + getMessage("missingArg", ARG_SOURCE_APP);
        }
        if (!message.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * @see BaseCommandTask#getArgumentValue(String, String[], String, String, ConsoleWrapper, PrintStream)
     */
    private String getArgumentValue(String arg, String[] args) {
        return getArgumentValue(arg, args, stdin, stdout);
    }
}
