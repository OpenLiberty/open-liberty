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

package com.ibm.ws.jsp.translator.compiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.translator.compiler.utils.JspFileManager;
import com.ibm.ws.jsp.translator.compiler.utils.JspFileObject;
import com.ibm.wsspi.jsp.compiler.JspCompiler;
import com.ibm.wsspi.jsp.compiler.JspCompilerResult;
import com.ibm.wsspi.jsp.compiler.JspLineId;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class StandardJspCompiler implements JspCompiler  {
    static final protected Logger logger;
    private static final String CLASS_NAME="com.ibm.ws.jsp.translator.compiler.StandardJspCompiler";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    private static final long NANOS_IN_A_MILLISECOND = 1000000L;
    private static String separatorString = System.getProperty("line.separator");  // Defect xxxxxx

    protected boolean isClassDebugInfo = false;
    protected boolean isDebugEnabled = false;
    protected boolean isVerbose = false;
    protected boolean isDeprecation = false; 
    protected String jdkSourceLevel=null; 
    protected String javaEncoding = null; 
    protected String outputDir = null;
    protected JavaCompiler compiler;
    protected ClassLoader classLoader = null;
    protected DiagnosticCollector<JavaFileObject> diagnostics;
    protected StandardJavaFileManager fileManager;
    protected JspFileManager fileManagerImpl;

    public StandardJspCompiler(JspClassloaderContext context, JspOptions options, String optimizedClasspath, boolean useOptimizedClasspath) {
        
        this.compiler = ToolProvider.getSystemJavaCompiler(); //This is not null as we checked in AbstractJspExtensionProcessor
        this.classLoader = context.getClassLoader();
        this.diagnostics = new DiagnosticCollector<JavaFileObject>(); //Needed for error/warning reporting
        this.fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        this.fileManagerImpl = new JspFileManager(fileManager, classLoader);
        
        this.isClassDebugInfo = options.isClassDebugInfo();
        this.isDebugEnabled = options.isDebugEnabled();
        this.isVerbose = options.isVerbose();
        this.isDeprecation =  options.isDeprecation();
        this.javaEncoding = options.getJavaEncoding();
        this.jdkSourceLevel=  options.getJdkSourceLevel();
        this.outputDir = options.getOutputDir().getPath();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "StandardJspCompiler", "Entering StandardJspCompiler.");
        }
    }
    
    public JspCompilerResult compile(JspResources[] jspResources, JspResources[] dependencyResources, Collection jspLineIds, List compilerOptions) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "compile", "Entering StandardJspCompiler.compile");
        }

        boolean rc = false;

        rc = runCompile(jspResources, dependencyResources, compilerOptions);
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "compile", "rc = " + rc);
        }
        
        int rci = rc?0:1;
        String output = null;
        
        if (rci != 0 || (rci == 0 && (isVerbose || isDeprecation))) {
            if (rci != 0) {
                output = getJspLineErrors(jspLineIds);
            }
        }

        JspCompilerResult result = new JspCompilerResultImpl(rci, output);

        return (result);
    }
    
    private boolean runCompile(JspResources[] jspResources, JspResources[] dependencyResources, List compilerOptions) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "compile", "Entering StandardJspCompiler.runCompile");
        }
        List<String> argList = null;
        if (jspResources.length > 1) {
            /*
             * This means that the jspResources we are trying to compile are tag files.
             * Using the outputDir as the location of where to place the compiled tag files.
             */
            argList = buildArgList(compilerOptions, outputDir);
            fileManagerImpl.setAreTagFiles(true);
        } else {
            //jspResource[0] has the best location to place the .class files.
            argList = buildArgList(compilerOptions, jspResources[0].getGeneratedSourceFile().getParent());
        }
        
        ArrayList<JavaFileObject> classesToCompile = new ArrayList<JavaFileObject>();
        fileManagerImpl.addDependencies(dependencyResources);
        for(JspResources jspResource : jspResources) {
            classesToCompile.add(new JspFileObject(jspResource, Kind.SOURCE, javaEncoding));
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            for(JspResources jspResource : jspResources)
                logger.logp(Level.FINE, CLASS_NAME, "runCompile", "compiling " + jspResource.getGeneratedSourceFile().getPath());
            if(dependencyResources != null) { //preventing NPE, it could be null
                logger.logp(Level.FINE, CLASS_NAME, "runCompile", "Dependencies: ");
                for(JspResources jspDependency : dependencyResources)
                    logger.logp(Level.FINE, CLASS_NAME, "runCompile", "Dependency resource: " + jspDependency.getGeneratedSourceFile().getPath());
            }
            logger.logp(Level.FINE, CLASS_NAME, "runCompile", "classloader [" + classLoader + "]");
        }

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManagerImpl, diagnostics, argList, null, classesToCompile);
        
        long start = System.nanoTime();
        
        boolean success = task.call();
        
        long end = System.nanoTime();
        
        try {
            fileManager.close();
            fileManagerImpl.close();
        } catch (IOException e) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "runCompile", "Problem closing FileManagers", e);
            }
        }
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            for(JspResources jspResource : jspResources)
                logger.logp(Level.FINE, CLASS_NAME, "runCompile", "Compilation completed for " + jspResource.getGeneratedSourceFile().getPath());
            logger.logp(Level.FINE, CLASS_NAME, "runCompile", "Compilation completed in " + ((end - start) / NANOS_IN_A_MILLISECOND) + " Milliseconds; success = " + success);
        }
        return success;
    }

    private List<String> buildArgList(List compilerOptions, String outputDirectory) {
        List<String> argList = new ArrayList<String>();
        
        if (isClassDebugInfo || this.isDebugEnabled) { 
            argList.add("-g");
        }
        
        if (isVerbose) {
            argList.add("-verbose");
        }
        
        if (isDeprecation) {
            argList.add("-deprecation");
        }
        
        //487396.1 jdkSourceLevel is 15 by default now ... should get into if statement
        argList.add("-source");
        if (jdkSourceLevel.equals("14")) {
            argList.add("1.4");
        }
        else if (jdkSourceLevel.equals("15")) {
            argList.add("1.5");
        }
        //PM04610 start
        else if (jdkSourceLevel.equals("16")) {
            argList.add("1.6");
        }
        //PM04610 end
        else if (jdkSourceLevel.equals("17")) {
            argList.add("1.7");
        }
        //126902 start
        else if (jdkSourceLevel.equals("18")) {
            argList.add("1.8");
        }
        //126902 end
        else {
            argList.add("1.3");
        }
        
        // Annotations don't need to be processed for JSPs.
        argList.add("-proc:none");
        
        argList.add("-encoding");
        argList.add(this.javaEncoding);
        argList.add("-XDjsrlimit=1000");
        
        argList.add("-d");
        argList.add(outputDirectory);
        
        /*
         * Not needed anymore as we now provide classes source code as JspFileObject objects.
         * argList.add("-sourcepath");
         * argList.add(sourcepath);
         */
        
        /*
         * Not needed anymore as we are now using the JSP Classloader object.
         * The classpath as string is not accessible on Liberty anyway.
         * argList.add("-classpath");
         * argList.add(classpath);
         */
        
        if (compilerOptions!=null) {
            for (int i=0;i<compilerOptions.size();i++) {
                String compilerOption = (String)compilerOptions.get(i);
                if (compilerOption.equals("-verbose")) {
                    isVerbose = true; 
                }
                if (compilerOption.equals("-deprecation")) {
                    isDeprecation = true; 
                }
                argList.add(compilerOption);
            }
        }
        
        /* 
         * Not needed anymore, the source of the JSP classes to compile are provided as JspFileObject objects.
         * argList.add(source);
         */
        return argList;
    }

    public String getJspLineErrors(Collection jspLineIds) {
        StringBuffer errorMsg = new StringBuffer();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind().equals(javax.tools.Diagnostic.Kind.ERROR) || (isVerbose || isDeprecation)){
                
                //A new File object needs to be created to get the correct path in the file system.
                String mapping = diagnostic.getSource() == null ? null :
                    findMapping(jspLineIds, diagnostic.getLineNumber(), new File(diagnostic.getSource().toUri().getSchemeSpecificPart()).getPath());
                
                if (mapping == null) {
                    errorMsg.append(separatorString);
                }
                else {
                    errorMsg.append(mapping);
                    errorMsg.append(separatorString);
                }
                
                //Append the actual error message
                /*
                 * Example:
                 * /C:/wlp/usr/servers/server/workarea/org.eclipse.osgi/61/data/temp/default_node/SMF_WebContainer/PM80935/PM80935/_compilationError.java:93: error: ';' expected
                 * java.util.Date myDate
                 *                      ^
                 */
                errorMsg.append(diagnostic.toString());
                errorMsg.append(separatorString);
            }
        }
        return errorMsg.toString();
    }

    private String findMapping(Collection jspLineIds, long lineNr, String javaName) {
        String errorMsg = null;
        for (Iterator itr = jspLineIds.iterator(); itr.hasNext();) {
            JspLineId lineId = (JspLineId)itr.next();
            if (lineId.getGeneratedFilePath().equals(javaName)) {
                if (lineId.getStartGeneratedLineCount() <= 1 && lineId.getStartGeneratedLineNum() == lineNr) {
                    errorMsg =  createErrorMsg(lineId, lineNr);
                    break;
                }
                else if (lineId.getStartGeneratedLineNum() <= lineNr && 
                         (lineId.getStartGeneratedLineNum() + lineId.getStartGeneratedLineCount() - 1) >= lineNr) {
                    errorMsg =  createErrorMsg(lineId, lineNr);
                    break;
                }
            }
        }
        return errorMsg;
    }

    /**
     * Create error message including the jsp line numbers and file name
     */
    //    defect 203009 - add logic to 1. narrow down error to single line in JSP, if possible, 2) improve
    //                    error messages to indicate when a file is statically included, and give
    //                    name of parent file 
    private String createErrorMsg(JspLineId jspLineId, long errorLineNr) {
        StringBuffer compilerOutput = new StringBuffer();

        if (jspLineId.getSourceLineCount() <= 1) {
            Object[] objArray = new Object[] { Integer.valueOf(jspLineId.getStartSourceLineNum()), jspLineId.getFilePath() };
            if (jspLineId.getFilePath().equals(jspLineId.getParentFile())) {
                compilerOutput.append(separatorString + JspCoreException.getMsg("jsp.error.single.line.number", objArray)); // Defect 211450
            } else {
                compilerOutput.append(separatorString + JspCoreException.getMsg("jsp.error.single.line.number.included.file", objArray)); // Defect 211450
            }
        }
        else {
            // compute exact JSP line number 
            long actualLineNum=jspLineId.getStartSourceLineNum()+(errorLineNr-jspLineId.getStartGeneratedLineNum());
            if (actualLineNum>=jspLineId.getStartSourceLineNum() && actualLineNum <=(jspLineId.getStartSourceLineNum() + jspLineId.getSourceLineCount() - 1)) {
                Object[] objArray = new Object[] { Long.valueOf(actualLineNum), jspLineId.getFilePath()};
                if (jspLineId.getFilePath().equals(jspLineId.getParentFile())) {
                    compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.single.line.number", objArray));  // Defect 211450
                }
                else {
                    compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.single.line.number.included.file", objArray));  // Defect 211450
                }
            }
            else {
                Object[] objArray = new Object[] {
                    Integer.valueOf(jspLineId.getStartSourceLineNum()),
                    Integer.valueOf((jspLineId.getStartSourceLineNum()) + jspLineId.getSourceLineCount() - 1),
                    jspLineId.getFilePath()};
                if (jspLineId.getFilePath().equals(jspLineId.getParentFile())) {
                    compilerOutput.append(separatorString+  // Defect 211450
                        JspCoreException.getMsg(
                            "jsp.error.multiple.line.number", objArray));
                }
                else {
                    compilerOutput.append(separatorString+  // Defect 211450
                        JspCoreException.getMsg(
                            "jsp.error.multiple.line.number.included.file",objArray));
                }
            }
        }

        compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.corresponding.servlet",new Object[] { jspLineId.getParentFile()})+separatorString);  // Defect 211450
        //152470 starts
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, "createErrorMsg", "The value of the JSP attribute jdkSourceLevel is ["+ jdkSourceLevel +"]");
        }
        //152470 ends
        return compilerOutput.toString();
    }

    /* (non-Javadoc)
     * @see com.ibm.wsspi.jsp.compiler.JspCompiler#compile(java.lang.String, java.util.Collection, java.util.List)
     */
    @Override
    public JspCompilerResult compile(String arg0, Collection arg1, List arg2) {
        // TODO Auto-generated method stub
        return null;
    }

}
