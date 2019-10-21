package com.ibm.ws.install.featureUtility.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.featureUtility.FeatureUtility;
import com.ibm.ws.install.featureUtility.FeatureUtilityExecutor;
import com.ibm.ws.install.internal.InstallLogUtils;
import com.ibm.ws.install.internal.ProgressBar;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.ReturnCode;
import com.ibm.ws.kernel.boot.cmdline.ActionHandler;
import com.ibm.ws.kernel.boot.cmdline.Arguments;
import com.ibm.ws.kernel.boot.cmdline.ExitCode;

public class FeatureDownloadAction implements ActionHandler {

	private FeatureUtility featureUtility;
    private Logger logger;
	private ArrayList<String> artifactNames;
	private ArrayList<String> artifactShortNames;
	private List<String> argList;
	private String location;
	private boolean isOverwrite;
	private ProgressBar progressBar;

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
		this.artifactShortNames = new ArrayList<String>();
		this.argList = args.getPositionalArguments();
		this.location = args.getOption("location");
        this.isOverwrite = args.getOption("overwrite") != null;
        
        this.progressBar = ProgressBar.getInstance();
        
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
        List<String> nonMavenIds = getNonMavenIds(assetIds);
        assetIds.removeAll(nonMavenIds);
        
        
        if (isOverwrite) {
        	artifactNames.addAll(assetIds);
        	artifactShortNames.addAll(nonMavenIds);
		} else {
			List<String> missingArtifacts = new ArrayList<String>();
			List<String> missingShortNameArtifacts = new ArrayList<String>();
			try {
				featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(location)
	            		.setFeaturesToInstall(artifactNames).setIsBasicInit(true).build();
				missingArtifacts.addAll(FeatureUtility.getMissingArtifactsFromFolder(assetIds, location, false));
				missingShortNameArtifacts.addAll(FeatureUtility.getMissingArtifactsFromFolder(nonMavenIds, location, true));
			} catch (IOException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
	            return FeatureUtilityExecutor.returnCode(InstallException.IO_FAILURE);
			} catch (InstallException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
	            return FeatureUtilityExecutor.returnCode(e.getRc());
			}
			artifactNames.addAll(missingArtifacts);
			artifactShortNames.addAll(missingShortNameArtifacts);
		}
		
		return rc;
	}
	
	private List<Integer> getIncrementSize(int numArtifacts, int size) {
		List<Integer> result = new ArrayList<Integer>();
		Integer div = size / numArtifacts;
		Integer leftover = size - (div * numArtifacts);
		result.add(div);
		result.add(leftover);
		return result;
	}

	private List<String> getNonMavenIds(List<String> assetIds) {
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
		progressBar.finish();
		return rc;
	}

	private ExitCode download() {
		HashMap<String, Integer> methodMap = new HashMap<>();
		try {
			List<String> artifactNamesResolved = new ArrayList<String>();
			methodMap.put("resolveArtifact", 5);
			progressBar.setMethodMap(methodMap);
			if (!artifactShortNames.isEmpty()) {
				featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(location)
            		.setFeaturesToInstall(artifactShortNames).setIsBasicInit(true).build();
				artifactNames.addAll(featureUtility.getMavenCoords(artifactShortNames));
			}
			
            featureUtility = new FeatureUtility.FeatureUtilityBuilder().setFromDir(location)
            		.setFeaturesToInstall(artifactNames).setIsBasicInit(true).setIsDownload(true).build();
            artifactNamesResolved.addAll(featureUtility.resolveFeatures(false));
            
            List<Integer> increments = getIncrementSize(artifactNamesResolved.size(), 82);
            methodMap.put("downloadArtifact", increments.get(0));
            methodMap.put("establishConnection", 5);
            methodMap.put("downloadedArtifacts", 2 + increments.get(1));
            progressBar.setMethodMap(methodMap);
            featureUtility.downloadFeatures(artifactNamesResolved);
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
