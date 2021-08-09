/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webserver.plugin.utility.tasks;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Set;

import com.ibm.ws.http.plugin.merge.PluginMergeTool;
import com.ibm.ws.http.plugin.merge.PluginMergeToolFactory;
import com.ibm.ws.product.utility.CommandConsole;
import com.ibm.ws.product.utility.ExecutionContext;
import com.ibm.ws.webserver.plugin.utility.utils.CommandUtils;


public class MergePluginFilesTask extends BasePluginConfigCommandTask {

    static final String ARG_REQ_SOURCE_PATH = "--sourcePath";
    static final String ARG_OPT_TARGET_PATH = "--targetPath";
    static final String FILE_SELECT_REG_EXP = ".*plugin-cfg.*.xml";
    static final String TARGET_MERGED_FILE_NAME = "merged-plugin-cfg.xml";
    static final String CURRENT_DIR = "user.dir";
    
    public MergePluginFilesTask(String scriptName) {
        super(scriptName);
    }

    @Override
    public String getTaskName() {
        return "merge";
    }

    @Override
    public Set<String> getSupportedOptions() {
        return null;
    }

    @Override
    public void execute(ExecutionContext context) {
        
        reqArgs.add(ARG_REQ_SOURCE_PATH);
        knownArgs.add(ARG_OPT_TARGET_PATH);

        knownArgs.addAll(reqArgs);

        validateArgumentList(context.getArguments(), true);

        String sourcePath = getArgumentValue(ARG_REQ_SOURCE_PATH, context.getArguments(), null);
        String targetPath = getArgumentValue(ARG_OPT_TARGET_PATH, context.getArguments(), null);

        String[] sourceFileNames = sourcePath.split(",");

        if (sourceFileNames.length == 1) {
            mergePluginFilesFromDir(context.getCommandConsole(),sourcePath, targetPath);
        } else {
            mergePluginFilesFromList(context.getCommandConsole(),sourceFileNames, targetPath);
        }
        
    }

    public String getTaskUsage() {
        return CommandUtils.getMessage("MergePluginFilesTask.usage.options","");
    }

    @Override
    public String getTaskHelp() {
        String footer =  getOption("global.options") + NL +
                buildScriptOptions("MergePluginFilesTask.required-option-key.sourcePath",
                                           "MergePluginFilesTask.required-option-desc.sourcePath")
                        +
                        buildScriptOptions("MergePluginFilesTask.required-option-key.targetPath",
                                           "MergePluginFilesTask.required-option-desc.targetPath")
                        +
                        NL;

        return getTaskHelp("MergePluginFilesTask.desc", "MergePluginFilesTask.usage.options",
                           null, null,
                           null, footer,
                           scriptName);
    }

    @Override
    public String getTaskDescription() {
        StringBuilder scriptHelp = new StringBuilder();
        String UseOption=getOption("MergePluginFilesTask.usage.options");
        UseOption = TAB+UseOption.substring(5);
        scriptHelp.append(UseOption);
        scriptHelp.append(NL);
        scriptHelp.append(getOption("MergePluginFilesTask.desc"));
        scriptHelp.append(NL);
        return scriptHelp.toString();
     }

    private void mergePluginFilesFromList(final CommandConsole console,String[] sourceFileNames, String targetPath) {
        console.printlnInfoMessage(getMessage("MergePluginFilesTask.merging.plugin.list"));

        for (String strFile : sourceFileNames) {
            File file = new File(strFile);

            if (!file.exists()) {
                abort(console,getMessage("MergePluginFilesTask.merging.plugin.file.not.exists", strFile));
                return;
            }    

            if (file.isDirectory()) {
                abort(console,getMessage("MergePluginFilesTask.merging.plugin.file.is.directory", file.getAbsolutePath()));
                return;
            }    

        }

        String strTargetDir = System.getProperty(CURRENT_DIR);

        if (targetPath == null) {
            targetPath = strTargetDir + File.separator + TARGET_MERGED_FILE_NAME;
        } else {
            File targetPathFile = new File(targetPath);
            if (targetPathFile.exists() && targetPathFile.isDirectory()) {
                targetPath = targetPath + File.separator + TARGET_MERGED_FILE_NAME;
            }
        }

        console.printlnInfoMessage(getMessage("MergePluginFilesTask.merging.plugin.target.file.generating", targetPath));

        String[] args = new String[sourceFileNames.length + 1];
        System.arraycopy(sourceFileNames, 0, args, 0, sourceFileNames.length);
        args[sourceFileNames.length] = targetPath;
        merge(args);

        console.printlnInfoMessage(getMessage("MergePluginFilesTask.merging.plugin.target.file.generated"));

    }

    private void mergePluginFilesFromDir(final CommandConsole console, String sourcePath, String targetPath) {
        console.printlnInfoMessage(getMessage("MergePluginFilesTask.merging.plugin.dir", sourcePath));

        File sourcePathFile = new File(sourcePath);

        if (!sourcePathFile.exists()) {
            abort(console, getMessage("MergePluginFilesTask.merging.plugin.source.dir.not.exists", sourcePathFile));
            return;
        }    

        if (!sourcePathFile.isDirectory()) {
            abort(console, getMessage("MergePluginFilesTask.merging.plugin.source.file.not.directory", sourcePathFile));
            return;
        }    

        File[] files = sourcePathFile.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean result = name.matches(FILE_SELECT_REG_EXP);
                if (result)
                    console.printlnInfoMessage(getMessage("MergePluginFilesTask.merging.plugin.source.file.selected", name));
                return result;
            }
        });

        // If there is not at least 1 file to be merged abort
        if(files.length < 1) {
            abort(console, getMessage("MergePluginFilesTask.merging.plugin.insufficent.number.of.source.files"));
            return;
        }
        
        String strTargetDir = System.getProperty(CURRENT_DIR);

        if (targetPath == null) {
            targetPath = strTargetDir + File.separator + TARGET_MERGED_FILE_NAME;
        } else {
            File targetPathFile = new File(targetPath);
            if (targetPathFile.exists() && targetPathFile.isDirectory()) {
                targetPath = targetPath + File.separator + TARGET_MERGED_FILE_NAME;
            }
        }

        console.printlnInfoMessage(getMessage("MergePluginFilesTask.merging.plugin.target.file.generating", targetPath));

        String[] sourceFileNames = new String[files.length];
        for (int i = 0; i < files.length; i++)
            sourceFileNames[i] = files[i].getAbsolutePath();

        String[] args = new String[sourceFileNames.length + 1];
        System.arraycopy(sourceFileNames, 0, args, 0, sourceFileNames.length);
        args[sourceFileNames.length] = targetPath;
        
        merge(args);

        console.printlnInfoMessage(getMessage("MergePluginFilesTask.merging.plugin.target.file.generated"));

    }

    private void merge(String[] args) {
        PluginMergeTool mergeTool = PluginMergeToolFactory.getMergeToolInstance();
        mergeTool.merge(args);
    }

}
