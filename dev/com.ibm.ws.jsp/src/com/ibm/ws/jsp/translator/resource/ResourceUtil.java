/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.resource;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.util.FileSystem;
import com.ibm.ws.util.WSUtil;
import com.ibm.wsspi.adaptable.module.Entry;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

public class ResourceUtil {
	//	begin 213703: add logging for isoutdated checks		
	private static Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.resource.ResourceUtil";
	
	// PK75069  
    private static final boolean isOS400= System.getProperty("os.name").toLowerCase().equals ("os/400") ;
    private static final boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    static{
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    //	end 213703: add logging for isoutdated checks

    public static void sync(long sourceFileTimeStamp, File generatedSourceFile, File classFile, String className, boolean keepgenerated, boolean keepGeneratedclassfiles) {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
            logger.logp(Level.FINEST, CLASS_NAME, "sync", "Synching for sourceFile ts [" + sourceFileTimeStamp + "]");
        }
        if (keepGeneratedclassfiles == false) {
            boolean delete = classFile.delete();
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                    logger.logp(Level.FINEST, CLASS_NAME, "sync", (delete?"Deleted":"Unable to delete") + " classFile [" + classFile +"]");
            }
            File[] icList = generatedSourceFile.getParentFile().listFiles(new InnerclassFilenameFilter(className)); //205761
            for (int i=0;i<icList.length;i++) {
                if (icList[i].isFile()) {
                    boolean innerDelete = icList[i].delete();
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                        logger.logp(Level.FINEST, CLASS_NAME, "sync", (innerDelete?"Deleted":"Unable to delete")+" inner classFile [" + icList[i] +"]");
                    }
                }
            }           
        }
        else {
        boolean rc = classFile.setLastModified(sourceFileTimeStamp);
                    if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                            logger.logp(Level.FINEST, CLASS_NAME, "sync", (rc?"Updated":"Unable to update") +" lastModified timestamp for classFile [" + classFile +"] [" + classFile.lastModified()+ "]");
                    }
        }
        
        if (generatedSourceFile.exists()) {
        if (keepgenerated == false) {
            boolean delete = generatedSourceFile.delete();
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                                    logger.logp(Level.FINEST, CLASS_NAME, "sync", (delete?"Deleted":"Unable to delete") + " generatedSourceFile [" + generatedSourceFile +"]");
                            }
        }
        else {
                            boolean rc = generatedSourceFile.setLastModified(sourceFileTimeStamp);
                            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                                    logger.logp(Level.FINEST, CLASS_NAME, "sync", (rc?"Updated":"Unable to update") +" lastModified timestamp for generatedSourceFile [" + generatedSourceFile +"] [" + generatedSourceFile.lastModified()+ "]");
                            }
        }
        }
    }
    
    public static void sync(File sourceFile, File generatedSourceFile, File classFile, String className, boolean keepgenerated, boolean keepGeneratedclassfiles) {
        if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
            logger.logp(Level.FINEST, CLASS_NAME, "sync", "Synching for sourceFile [" + sourceFile +"] ts [" + sourceFile.lastModified() + "]");
        }
        sync(sourceFile.lastModified(), generatedSourceFile, classFile, className, keepgenerated, keepGeneratedclassfiles);
    }

    
    public static boolean isOutdated(Entry containerEntry, File sourceFile, File generatedSourceFile, File classFile, Entry webinfClassEntry, File webinfClassFile) {
        return isOutdated(-1L, containerEntry, sourceFile, generatedSourceFile, classFile, webinfClassEntry, webinfClassFile);
    }
    public static boolean isOutdated(long lastModified, Entry containerEntry, File sourceFile, File generatedSourceFile, File classFile, Entry webinfClassEntry, File webinfClassFile) {
        boolean outdated = true;
  
        //if the containerEntry was not found, then it will be null - it will only be non-null when a container entry was found
        //therefore, if the 

        boolean hasContainer = (containerEntry!=null);
        boolean isContainerOrEDR = (lastModified != -1L);
        
        long sourceLastModified = lastModified;
        if(hasContainer){
            sourceLastModified = containerEntry.getLastModified();
            isContainerOrEDR = true;
        }
        if (sourceFile!=null || isContainerOrEDR) {
            if (!isContainerOrEDR && sourceFile.exists() == false) {
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                        logger.logp(Level.FINER, CLASS_NAME, "isOutdated", "sourceFile [" + sourceFile + "] does not exist");
                                }
                return true;    // source file does not exist.
            } else {
                if(!isContainerOrEDR){
                    sourceLastModified = sourceFile.lastModified();
                }
            }
            try {       
                // PK75069 - call private jspCaseCheck() instead of FileSytem.uriCaseCheck()
                if (classFile.exists() && jspCaseCheck(classFile, classFile.getAbsolutePath())) {
                    if (sourceLastModified == classFile.lastModified()) {
                        outdated = false;
                    }
                    else{
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                    if (containerEntry!=null) {
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "containerEntry [" + containerEntry.getPath() + "]");    
                                    } else if (sourceFile != null) {
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "sourceFile [" + sourceFile + "]");
                                    } else {
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "lastModified passed directly [" + lastModified + "]");
                                    }
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "classFile [" + classFile + "]");
                                        logger.logp(Level.FINER, CLASS_NAME, "isOutdated", "sourceFile ts [" + sourceLastModified + "] differs from tempDirClassFile ts [" +classFile.lastModified() +"]. Recompile JSP.");
                                }
                    }
                // PK75069 - call private jspCaseCheck() instead of FileSytem.uriCaseCheck()
                } else if ((webinfClassFile!=null && webinfClassFile.exists() && jspCaseCheck(webinfClassFile, webinfClassFile.getAbsolutePath())) ||
                          (webinfClassEntry!=null)) {
                    long webinfLastModified = 0;
                    if (webinfClassEntry!=null) {
                        webinfLastModified = webinfClassEntry.getLastModified();
                    } else {
                        webinfLastModified = webinfClassFile.lastModified();
                    }
                    if (sourceLastModified == webinfLastModified) {
                        outdated = false;
                    }
                        else{
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                                    if (containerEntry!=null) {
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "containerEntry [" + containerEntry.getPath() + "]");    
                                    } else if (sourceFile != null){
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "sourceFile [" + sourceFile + "]");
                                    } else {
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "lastModified passed directly [" + lastModified + "]");
                                    }
                                    if (webinfClassEntry!=null) {
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "webinfClassEntry [" + webinfClassEntry.getPath() + "]");
                                    } else {
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "webinfClassFile [" + webinfClassFile + "]");
                                    }
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", "sourceFile ts [" + sourceLastModified + "] differs from webinfClassFile ts [" + webinfLastModified +"]. Recompile JSP.");
                                }
                        }
                }
            } catch (IOException e) {
                //The IOException came from the FileSystem.uriCaseCheck - just say that it is outdated
                return true;
            }
            if (outdated && generatedSourceFile.getParentFile().exists() == false){
                boolean rc = FileUtils.ensureDirExists(generatedSourceFile.getParentFile());
                                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                                        logger.logp(Level.FINEST, CLASS_NAME, "isOutdated", (rc?"Created":"Unable to create") +" directory for generated source file ["+generatedSourceFile.getParentFile() +"]");
                                }
            }
        }
        else {
            outdated = false;
        }

        return (outdated);
    }
    
    public static boolean isOutdated(File sourceFile, File generatedSourceFile, File classFile, File webinfClassFile) {
        return ResourceUtil.isOutdated(null, sourceFile, generatedSourceFile, classFile, null, webinfClassFile);
    }

    public static boolean isTagFileOutdated(long sourceLastModified, File generatedSourceFile, File classFile, Entry webinfEntry, File webinfClassFile) {
        boolean outdated = true;
        /*
        I think this was being called for the directory which did return a sourceLastModified of 0
        The source file would have to be there in order for a recompile, so I don't think we need this check
        if (sourceLastModified == 0){
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                logger.logp(Level.FINER, CLASS_NAME, "isTagFileOutdated", "sourceFile does not exist");
            }
            return true;    // tag file does not exist.
        }*/
        
        // begin 213703: change outdated checks to mirror algorithm used in isOutdated.
        if (generatedSourceFile.exists()){
            if(sourceLastModified == generatedSourceFile.lastModified()) {
                outdated = false;
            }
            else{
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    //logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "sourceFile [" + sourceFile + "]");
                    logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "generatedSourceFile [" + generatedSourceFile + "]");
                    logger.logp(Level.FINER, CLASS_NAME, "isTagfileOutdated", "sourceFile ts [" + sourceLastModified + "] differs from generatedSourceFile ts [" +generatedSourceFile.lastModified() +"]. Recompile tag file.");
                }
            }
        }
        else if (classFile.exists()){
            if(sourceLastModified == classFile.lastModified()) {
                outdated = false;
            }
            else{
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    //logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "sourceFile [" + sourceFile + "]");
                    logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "classFile [" + classFile + "]");
                    logger.logp(Level.FINER, CLASS_NAME, "isTagfileOutdated", "sourceFile ts [" + sourceLastModified + "] differs from tempDirClassFile ts [" + classFile.lastModified() +"]. Recompile tag file.");
                }
            }
        }
        else if (webinfClassFile!=null && webinfClassFile.exists()){ 
            if(sourceLastModified == webinfClassFile.lastModified()) {
                outdated = false;
            }
            else{
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    //logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "sourceFile [" + sourceFile + "]");
                    logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "webinfClassFile [" + webinfClassFile + "]");
                    logger.logp(Level.FINER, CLASS_NAME, "isTagfileOutdated", "sourceFile ts [" + sourceLastModified + "] differs from webinfClassFile ts [" + webinfClassFile.lastModified() +"]. Recompile tag file.");
                }
            }
        } else if (webinfEntry!=null) {
            if (sourceLastModified == webinfEntry.getLastModified()) {
                outdated = false;
            } else {
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    //logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "sourceFile [" + sourceFile + "]");
                    logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", "webinfEntry [" + webinfEntry.getPath() + "]");
                    logger.logp(Level.FINER, CLASS_NAME, "isTagfileOutdated", "sourceFile ts [" + sourceLastModified + "] differs from webinfEntry ts [" + webinfEntry.getLastModified() +"]. Recompile tag file.");
                }
            }
        }
        //end 213703: change outdated checks to mirror algorithm used in isOutdated.

        if(outdated && generatedSourceFile.getParentFile().exists() == false){
            boolean rc = FileUtils.ensureDirExists(generatedSourceFile.getParentFile());
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                    logger.logp(Level.FINEST, CLASS_NAME, "isTagFileOutdated", (rc?"Created":"Unable to create") +" directory for generated source file ["+generatedSourceFile.getParentFile() +"]");
            }
        }

        return (outdated);
    }
    
    public static boolean isTagFileOutdated(File sourceFile, File generatedSourceFile, File classFile, File webinfClassFile) {
        if (sourceFile.exists() == false){
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)){
                    logger.logp(Level.FINER, CLASS_NAME, "isTagFileOutdated", "sourceFile [" + sourceFile + "] does not exist");
            }
            return true;    // tag file does not exist.
        }
        return isTagFileOutdated(sourceFile.lastModified(), generatedSourceFile, classFile, null, webinfClassFile);
    }

    public static void syncGeneratedSource(File sourceFile, File generatedSourceFile) {
        syncGeneratedSource(sourceFile.lastModified(), generatedSourceFile);
    }       
    
    public static void syncGeneratedSource(long sourceFileTime, File generatedSourceFile) {
        if (generatedSourceFile.exists()){
			boolean rc = generatedSourceFile.setLastModified(sourceFileTime);
			if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
				logger.logp(Level.FINEST, CLASS_NAME, "syncGeneratedSource", (rc?"Updated":"Unable to update") +" lastModified timestamp for generatedSourceFile [" + generatedSourceFile +"] [" + generatedSourceFile.lastModified()+ "]");
			}
        }
            
    }

    public static void syncTagFile(File sourceFile, File generatedSourceFile, File classFile, boolean keepgenerated, boolean keepGeneratedclassfiles) {
        syncTagFile(sourceFile.lastModified(), generatedSourceFile, classFile, keepgenerated, keepGeneratedclassfiles);
    }
    
    public static void syncTagFile(long sourceFileTime, File generatedSourceFile, File classFile, boolean keepgenerated, boolean keepGeneratedclassfiles) {
        if (sourceFileTime == generatedSourceFile.lastModified()) {
            if (keepGeneratedclassfiles == false) {
                boolean delete = classFile.delete();
				if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
					logger.logp(Level.FINEST, CLASS_NAME, "syncTagFile", (delete?"Deleted":"Unable to delete") + " classFile [" + classFile +"]");
				}
            }
            else {
                boolean rc = classFile.setLastModified(sourceFileTime);
				if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
					logger.logp(Level.FINEST, CLASS_NAME, "syncTagFile", (rc?"Updated":"Unable to update") +" lastModified timestamp for classFile [" + classFile +"] [" + classFile.lastModified()+ "]");
				}
            }
            if (generatedSourceFile.exists()) {
                if (keepgenerated == false){
					boolean delete = generatedSourceFile.delete();
					if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
						logger.logp(Level.FINEST, CLASS_NAME, "syncTagFile", (delete?"Deleted":"Unable to delete") + " generatedSourceFile [" + generatedSourceFile +"]");
					}
                }
                else{
					boolean rc = generatedSourceFile.setLastModified(sourceFileTime);
					if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
						logger.logp(Level.FINEST, CLASS_NAME, "syncTagFile", (rc?"Updated":"Unable to update") +" lastModified timestamp for generatedSourceFile [" + generatedSourceFile +"] [" + generatedSourceFile.lastModified()+ "]");
					}
                }
            }
        }
    }

    // start PK75069
	private static boolean jspCaseCheck (File file, String matchString) throws java.io.IOException {  
		// private version of FileSystem.uriCaseCheck() for windows only

		if(isOS400){	//if OS/400 and not windows, still call the old method
			return FileSystem.uriCaseCheck(file, matchString);
		}		
		if (isWindows) {	
			//if Windows, check for class file only to avoid problems with windows shortnames
			//we already know the path is valid since classFile.exists() must return true to get here 
			matchString = WSUtil.resolveURI(matchString);
			
			matchString = matchString.replace ('/', File.separatorChar);
			int lastSeparator = matchString.lastIndexOf(File.separatorChar);
			matchString = matchString.substring(++lastSeparator);
			
			String canPath = file.getCanonicalPath();
			lastSeparator = canPath.lastIndexOf(File.separatorChar);
			canPath = canPath.substring(++lastSeparator);
			
			if(!matchString.equals(canPath)){
				return false;
			}
		}
		return true;
	} 
    // end PK75069
    
	//	defect 205761 begin    
	private static class InnerclassFilenameFilter implements FilenameFilter {
		String filename=null;
		public InnerclassFilenameFilter(String filename){
			this.filename=filename;
		}
		public boolean accept(File dir, String name) {
			int dollarIndex = name.indexOf("$");
			if (dollarIndex > -1) {
				String nameStart = name.substring(0, dollarIndex);
				if (this.filename.equals(nameStart)) {
					return true;
				}
			}
			return false;
		}
	}
	//	defect 205761 end    
}
