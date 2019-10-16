package com.ibm.ws.install.featureUtility.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.featureUtility.FeatureUtility;
import com.ibm.ws.install.featureUtility.FeatureUtilityExecutor;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;

public class FeatureDownloadAction implements ActionHandler {

	private FeatureUtility featureUtility;
    private Logger logger;
	private ArrayList<String> artifactNames;
	private List<String> argList;
	private String location;
	private boolean isOverwrite;

	@Override
    public ExitCode handleTask(PrintStream stdout, PrintStream stderr, Arguments args) {
    	ExitCode rc = initialize(args);
        if (!!!rc.equals(ReturnCode.OK)) {
            return rc;
        }
        rc = execute();
        return rc;
    }

	private ExitCode initialize(Arguments args) {
		ExitCode rc = ReturnCode.OK;
		
		this.logger = InstallLogUtils.getInstallLogger();
		this.artifactNames = new ArrayList<String>();
		this.argList = args.getPositionalArguments();
		this.location = args.getOption("location");
        this.isOverwrite = args.getOption("overwrite") != null;
        
        if (location == null) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_MISSING_DIRECTORY", "download"));
            return ReturnCode.BAD_ARGUMENT;
        }
        if (location.isEmpty()) {
            InstallLogUtils.getInstallLogger().log(Level.SEVERE,
                                                   Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_TOOL_DIRECTORY_REQUIRED", "location"));
            return ReturnCode.BAD_ARGUMENT;
        }
        List<String> assetIds = new ArrayList<String>(argList);
        List<String> improperIds = checkMavenArtifactFormat(assetIds);
        if (!improperIds.isEmpty()) {
        	logger.log(Level.SEVERE, "The following asset IDs are not in proper maven artifact ID format: " + improperIds);
        	return ReturnCode.BAD_ARGUMENT; 
        }
        if (isOverwrite) {
        	artifactNames.addAll(assetIds);
		} else {
			List<String> missingArtifacts;
			try {
				featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(location).setFeaturesToInstall(artifactNames).build();
				missingArtifacts = featureUtility.getMissingArtifactsFromFolder(assetIds, location);
			} catch (IOException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
	            return FeatureUtilityExecutor.returnCode(InstallException.IO_FAILURE);
			} catch (InstallException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
	            return FeatureUtilityExecutor.returnCode(e.getRc());
			}
			artifactNames.addAll(missingArtifacts);
		}
		
		return rc;
	}
	
	private List<String> checkMavenArtifactFormat(List<String> assetIds) {
		List<String> result = new ArrayList<String>();
		
		for (String id: assetIds) {
			if (id.split(":").length != 3) {
				result.add(id);
			}
		}
		
		return result;
	}

	private ExitCode execute() {
		ExitCode rc = download();
		return rc;
	}

	private ExitCode download() {
		try {
            featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(location)
            		.setFeaturesToInstall(artifactNames).build();
            featureUtility.downloadFeatures();
        } catch (InstallException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return FeatureUtilityExecutor.returnCode(e.getRc());
        } catch (Throwable e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return FeatureUtilityExecutor.returnCode(InstallException.IO_FAILURE);
        }
        return ReturnCode.OK;
	}

}
