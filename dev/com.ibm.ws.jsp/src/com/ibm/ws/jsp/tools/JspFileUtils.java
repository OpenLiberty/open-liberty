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
package com.ibm.ws.jsp.tools;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.translator.utils.NameMangler;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;

/**
 * @author Scott Johnson
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JspFileUtils {
	private static final String CLASS_NAME="com.ibm.ws.jsp.tools.JspFileUtils";
    private static Pattern v4FilenamePattern = Pattern.compile("_jsp_[0-9]+\\.(dat|java)");
    private static Pattern classFilenamePattern = Pattern.compile("_[a-zA-Z0-9_$]*+\\.class");
    private Logger logger=null;

    public JspFileUtils() {
        this(null);
    }

    public JspFileUtils(Logger logger) {
        this.logger = logger;
    }
    public void removeVersion4Files(File directory, String outputDir) {
        // remove v4 .java, .class and .dat files
        int startPoint = outputDir.length();
        // Get the directory name(s) after the context root, i.e.,
        //   after the end of the base outputdir
        String pkgName = directory.toString().substring(startPoint);
        if (pkgName.length()==0)
            pkgName = "/";
        pkgName = pkgName.replace('\\','/');
        String mangledDirName=pkgName;
        // mangle the package name according logic used in v4
        if ( !pkgName.equals("/") ) {
            mangledDirName = NameMangler.handlePackageName (pkgName);
            mangledDirName = mangledDirName.replace('.','/');
        }
        mangledDirName = mangledDirName.replace('\\','/');
        if (mangledDirName.charAt(0) != '/')
            mangledDirName = "/"+mangledDirName;
        File v4Dir =null;
        if (outputDir.endsWith("/") )
            v4Dir = new File(outputDir + mangledDirName.substring(1));
        else
            v4Dir = new File(outputDir + mangledDirName.substring(0));

        // if the mangled package name results in the same name as the v6 directories, then
        //    we'll delete discrete files (.java, .dat, .class)
        if (mangledDirName.equals(pkgName)) {
            String classfileName=null;
            File[] v4List = v4Dir.listFiles(new WsFilenameFilter(v4FilenamePattern));
            if (v4List!=null) {
                for (int i=0;i<v4List.length;i++) {
                    if (v4List[i].isFile()) {
                        if (logger!=null) {
                            logger.logp(Level.CONFIG,"JspFileUtils","removeVersion4Files","Deleting: "+v4List[i]);
                        }
                        v4List[i].delete();
                        // create a .class file name from this filename, and delete
                        //   the underlying .class file if it exists.
                        classfileName=v4List[i].toString();
                        int end=classfileName.lastIndexOf("_jsp_");
                        classfileName=classfileName.substring(0,end);
                        classfileName+=".class";
                        File clFile=new File(classfileName);
                        if (clFile.exists() && clFile.isFile()) {
                            if (logger!=null) {
                                logger.logp(Level.CONFIG,"JspFileUtils","removeVersion4Files","Deleting: "+clFile.toString());
                            }
                            clFile.delete();
                        }
                    }
                }
            }
        }
        else {  // remove entire directory because it was mangled
            if (v4Dir.exists()) {
                if (logger!=null) {
                    logger.logp(Level.CONFIG,"JspFileUtils","removeVersion4Files","Removing: "+v4Dir.toString());
                    logger.logp(Level.CONFIG,"JspFileUtils","removeVersion4Files"," ");
                }
                deleteDirs(v4Dir, this.logger);
            }
        }
    }

    public void removeFixedPackageFiles(String outputDir, JspTranslationContext context, String resourcePath) {
        // remove v5 and v6 .java, .class and .dat files
        int endPoint = resourcePath.lastIndexOf('/');
        // Get the SOURCE directory name(s) after the context root, i.e.,
        //   after the end of the base outputdir
        String pkgName = resourcePath.substring(0,endPoint);

        if (pkgName.length()==0)
            pkgName = "/";
        pkgName = pkgName.replace('\\','/');
        if (pkgName.charAt(0) != '/')
        pkgName = "/"+pkgName;

        File dir =null;
        if (outputDir.endsWith("/") )
            dir = new File(outputDir + pkgName.substring(1));
        else
            dir = new File(outputDir + pkgName.substring(0));

        // delete discrete files (.java, .dat, .class)
        String fileName=null;
		String classname=null;
		int index=0;
        File[] fileList = dir.listFiles(new WsFilenameFilter(classFilenamePattern));
        if (fileList!=null) {
            for (int i=0;i<fileList.length;i++) {
                if (fileList[i].isFile()) {
                	// don't delete inner classes.  Remove an inner class only after its parent classfile has been removed.
					classname=fileList[i].toString();
                	if (classname.indexOf('$')<0) {
						if (logger!=null) {
							logger.logp(Level.CONFIG,"JspFileUtils","removeFixedPackageFiles","checking to see if we can delete: "+fileList[i]);
						}
						// try to load this file, using the v6 and v5 packages; if either CAN be loaded, we
						// may delete the file
						classname=fileList[i].toString();
						classname=classname.replace('\\','/');
						index=classname.lastIndexOf('/');
						classname = classname.substring(index);
						index = classname.lastIndexOf('.');
						classname = classname.substring(1, index);
						ClassLoader targetLoader = AbstractJspModC.createClassLoader(classname, context, fileList[i].toString(), this.logger, false, dir, fileList[i]);
						if (targetLoader != null) {
							boolean canDelete=true;
							String fname=Constants.JSP_FIXED_PACKAGE_NAME + "." + classname;
							if (loadClass(fname, targetLoader)==false) {
								fname=Constants.OLD_JSP_PACKAGE_NAME + "." + classname;
								if (loadClass(fname, targetLoader)==false) {
									canDelete=false;
								}
							}
							if (canDelete) {
								if (logger!=null) {
									logger.logp(Level.CONFIG,"JspFileUtils","removeFixedPackageFiles","Deleting: "+fileList[i]);
								}
								fileList[i].delete();
								// create a .dat and .java and an innerclass file name from this filename, and delete
								//   the underlying files if they exists.
								fileName=fileList[i].toString();
								int end=fileName.lastIndexOf(".");
								fileName=fileName.substring(0,end);
								String javaFileName=fileName+".java";
								String datFileName=fileName+".dat";
								File clFile=new File(javaFileName);
								if (clFile.exists() && clFile.isFile()) {
									if (logger!=null) {
										logger.logp(Level.CONFIG,"JspFileUtils","removeFixedPackageFiles","Deleting: "+clFile.toString());
									}
									clFile.delete();
								}
								clFile=new File(datFileName);
								if (clFile.exists() && clFile.isFile()) {
									if (logger!=null) {
										logger.logp(Level.CONFIG,"JspFileUtils","removeFixedPackageFiles","Deleting: "+clFile.toString());
									}
									clFile.delete();
								}
								// Delete inner classes for this class file
								File[] icList = dir.listFiles(new InnerclassFilenameFilter(classname));
								for (int j=0;j<icList.length;j++) {
									if (icList[j].isFile()) {
										if (logger!=null) {
											logger.logp(Level.CONFIG,"JspFileUtils","removeFixedPackageFiles","Deleting: "+icList[j].toString());
										}
										icList[j].delete();
									}
								}           
							}
						}
                	}
                }//if (fileList[i].isFile())
            }//for (int i=0;i<fileList.length;i++)
        }//if (fileList!=null)

        if (dir.isDirectory()) {
            dir.delete();
        }
    }
    
	public static class InnerclassFilenameFilter implements FilenameFilter {
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

    public static void deleteDirs(File file, Logger logger) {
        File[] files = file.listFiles();
        boolean didDelete=false;
        if(files != null && files.length > 0){
            for (int i=0; i<files.length;i++) {
                File aFile=files[i];
                if (aFile.isDirectory()) {
                    deleteDirs(aFile, logger);
                }
                else {
                    didDelete=aFile.delete();
                    if (logger!=null)
                        logger.logp(Level.FINEST,"JspFileUtils","deleteDirs", "Attempted to remove file "+aFile+". Removal succeeded?: "+didDelete);
                }
            }
        }
        didDelete=file.delete();
        if (logger!=null)
            logger.logp(Level.FINE, "JspFileUtils","deleteDirs","Attempted to remove directory "+file+". Removal succeeded?: "+didDelete);
    }

    private static class WsFilenameFilter implements FilenameFilter {
        Pattern filePattern;
        public WsFilenameFilter(Pattern pattern) {
            filePattern=pattern;
        }
        public boolean accept(File dir, String name) {
             return filePattern.matcher(name).find();
        }
    }

    private boolean loadClass(String className, ClassLoader targetLoader)
    {
        boolean retval=true;
        String[] dependents=null;
        try
        {
            Class.forName(className,true,targetLoader).newInstance();
        }
        catch (Throwable e)
        {
            if (logger!=null)
                logger.logp(Level.FINE,"JspFileUtils","loadClass","Exception caught during loading classfile",e);
            retval=false;
        }
        return retval;
    }
    
    public void createJspFileExtensionList(String list, List jspFileExtensions){
        StringTokenizer st = new StringTokenizer(list, ": ;");
        while (st.hasMoreTokens()){
            String ext = st.nextToken();
            if(jspFileExtensions.contains(ext)){
                if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.WARNING)){
                    logger.logp(Level.WARNING, CLASS_NAME, "createJspFileExtensionList", "duplicate value for jsp file extensions ", ext);
                }
            }else{
                jspFileExtensions.add(ext);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: JspFileUtils <packagename> <classname>");
            return;
        }
        String pkgName=args[0];
        String className=args[1];

        String mangledPkgName=null;
        mangledPkgName=NameMangler.handlePackageName (pkgName);
        mangledPkgName = mangledPkgName.replace('.','/');
        className=NameMangler.mangleClassName(className);
    }
}
