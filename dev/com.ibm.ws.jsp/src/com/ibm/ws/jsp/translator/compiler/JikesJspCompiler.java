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

import java.io.BufferedReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.wsspi.jsp.compiler.JspCompiler;
import com.ibm.wsspi.jsp.compiler.JspCompilerResult;
import com.ibm.wsspi.jsp.compiler.JspLineId;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class JikesJspCompiler implements JspCompiler {
    static protected Logger logger;
	private static final String CLASS_NAME="com.ibm.ws.jsp.translator.compiler.JikesJspCompiler";
    private static String separatorString = System.getProperty("line.separator");  // Defect 211450
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }
    private static final long NANOS_IN_A_MILLISECOND = 1000000L;
    protected JspClassloaderContext classloaderContext = null;
    protected JspOptions options = null;
    protected CharArrayWriter out = null;
    protected String fullClasspath = null;
    protected String optimizedClasspath = null;
    protected String sourcepath = null;
    protected boolean isClassDebugInfo = false;
    protected boolean isDebugEnabled = false;
    protected boolean isVerbose = false;
    protected boolean isDeprecation = false;
    protected String jdkSourceLevel=null;

    protected boolean useOptimizedClasspath = false;
    protected String absouluteContextRoot = null;

    public JikesJspCompiler(String absouluteContextRoot, JspClassloaderContext classloaderContext, JspOptions options, String optimizedClasspath, boolean useOptimizedClasspath) {
        this.absouluteContextRoot = absouluteContextRoot;
        this.classloaderContext = classloaderContext;
        this.optimizedClasspath = optimizedClasspath;
        this.options = options;

        this.useOptimizedClasspath = useOptimizedClasspath;
        sourcepath = options.getOutputDir().getPath();
        this.isClassDebugInfo = options.isClassDebugInfo();
        this.isDebugEnabled = options.isDebugEnabled();
        this.isVerbose = options.isVerbose();
        this.isDeprecation =  options.isDeprecation();
        jdkSourceLevel =  options.getJdkSourceLevel();
        out = new CharArrayWriter();

    }

    public JspCompilerResult compile(JspResources[] jspResources, JspResources[] dependencyResources, Collection jspLineIds, List compilerOptions) {
    	return compile(jspResources[0].getGeneratedSourceFile().getPath(), jspLineIds, compilerOptions);
    }
    
    public JspCompilerResult compile(String source, Collection jspLineIds, List compilerOptions) {
        out.reset();
        int rc = 0;

        String javaHomePath= null;
        try {
            javaHomePath = new File(System.getProperty("java.home")).getCanonicalPath();
        }
        catch (IOException e) {
            out.write(e.toString(), 0, e.toString().length());
            JspCompilerResult result = new JspCompilerResultImpl(1, out.toString());
            return (result);
        }

        fullClasspath = classloaderContext.getClassPath()+ File.pathSeparatorChar + options.getOutputDir().getPath();
        fullClasspath = createJikesClasspath(fullClasspath,javaHomePath);
        optimizedClasspath = createJikesClasspath(optimizedClasspath,javaHomePath);

        String cp = null;
        boolean directoryCompile = (source.charAt(0)=='@');
        if (!directoryCompile) {
            source="\""+source+"\"";
        }

        if (directoryCompile && !useOptimizedClasspath) {
            cp = fullClasspath;
        }
        else {
            cp = optimizedClasspath;
        }
        rc = runCompile(source, compilerOptions, cp);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "compile", "rc = " + rc + " directoryCompile = " + directoryCompile + " useOptimizedClasspath = " + useOptimizedClasspath);
        }

        if (rc != 0 && directoryCompile == false && useOptimizedClasspath == false) {
        	// Defect 211450 - change log level to FINE from WARNING
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "compile", "Warning: failed to compile " + source + " with optimized classpath ["+cp+"]");
            }

            out.reset();
            rc = runCompile(source, compilerOptions, fullClasspath);
        }
        String output = null;

        if (rc != 0 || (rc==0 && (isVerbose || isDeprecation))) {
            output = out.toString();
            if (rc!=0) {
                output = getJspLineErrors(output, jspLineIds);
            }
        }
        JspCompilerResult result = new JspCompilerResultImpl(rc, output);
        return (result);
    }

    private int runCompile(String source, List compilerOptions, String cp) {
        int rc = 0;

        List argList = buildArgList(source, compilerOptions, cp);
        String[] argsArray = new String[argList.size()];
        argsArray = (String[])argList.toArray(argsArray);
        StringBuffer argsbuffer=new StringBuffer();
        for (int i=0; i<argsArray.length;i++) {
            argsbuffer.append(argsArray[i]);
            argsbuffer.append(" ");
        }

        String args = "jikes " + argsbuffer.toString();

        long start = System.nanoTime();

        try {
            Process process = Runtime.getRuntime().exec(args, null, new File(absouluteContextRoot));            
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = br.readLine();
            while (line !=null) {
                out.write(line+separatorString);  // Defect 211450
                line = br.readLine();
            }
            process.waitFor();
            rc = process.exitValue();
        }
        catch (InterruptedException e) {
            out.write(e.toString(), 0, e.toString().length());
            rc = 1;
        }
        catch (IOException e) {
            out.write(e.toString(), 0, e.toString().length());
            rc = 1;
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "runCompile", "compiling " + source);
            logger.logp(Level.FINE, CLASS_NAME, "runCompile", "classpath [" + cp+ "]");
        }

        long end = System.nanoTime();

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "runCompile", "compile complete for " + source + " time = " + ((end - start) / NANOS_IN_A_MILLISECOND) + " Milliseconds rc = " + rc);
        }
        return rc;
    }

    private List buildArgList(String source, List compilerOptions, String classpath) {
        List argList = new ArrayList();

        if (isClassDebugInfo || this.isDebugEnabled) {
            argList.add("-g");
        }

        if (isVerbose) {
            argList.add("-verbose");
        }

        if (isDeprecation) {
            argList.add("-deprecation");
        }

        // Jikes must default to -source 1.3; override if jdkSourceLevel is set 
        //487396.1 jdkSourceLevel is 15 by default now ... should get into if statement
        argList.add("-source");
        if (jdkSourceLevel.equals("14")) {
            argList.add("1.4");
        }
        else if (jdkSourceLevel.equals("15")) {
            argList.add("1.5");
        }
        // PM04610 start
        else if (jdkSourceLevel.equals("16")) {
            argList.add("1.6");
        }
        // PM04610 end
        else if (jdkSourceLevel.equals("17")) {
            argList.add("1.7");
        }
        // 126902 start
        else if (jdkSourceLevel.equals("18")) {
            argList.add("1.8");
        }
        // 126902 end
        else {
            argList.add("1.3");
        }

        argList.add("-sourcepath");
        argList.add(sourcepath);
        argList.add("-classpath");
        argList.add(classpath);
        argList.add("-Xstdout");        

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

        argList.add(source);
        return argList;
    }

    /**
     * @param string
     * @return
     */
    private String createJikesClasspath(String string, String javaHomePath) {
        StringBuffer classpath = new StringBuffer();
        classpath.append(javaHomePath + File.separator + "lib" + File.separator + "core.jar" + File.pathSeparatorChar);
        classpath.append(javaHomePath + File.separator + "lib" + File.separator + "graphics.jar" + File.pathSeparatorChar);
        classpath.append(javaHomePath + File.separator + "lib" + File.separator + "security.jar" + File.pathSeparatorChar);
        classpath.append(javaHomePath + File.separator + "lib" + File.separator + "server.jar" + File.pathSeparatorChar);
        classpath.append(javaHomePath + File.separator + "lib" + File.separator + "xml.jar" + File.pathSeparatorChar);
        classpath.append(string);
        return classpath.toString();
    }

    public String getJspLineErrors(String compilerOutput, Collection jspLineIds) {

        StringBuffer errorMsg = new StringBuffer();
        BufferedReader br = new BufferedReader(new StringReader(compilerOutput));

        try {
            String line = br.readLine();
            int warningIndex = -1;
            String javaName = null;
            while (line != null) {

                // Get .java file name
                int javaNameEnd = line.indexOf(".java"+"\":");
                if (javaNameEnd > 0) {
                    // file name begins after first "
                    int firstQuote = line.indexOf("\"");
                    if (firstQuote>0) {
                        javaName = line.substring(firstQuote+1, javaNameEnd + 5);
                        // path separator consistent for platform
                        javaName = javaName.replace ('\\', '/');
                        javaName = javaName.replace('/', File.separatorChar);
                    }
                }
                warningIndex = line.indexOf("*** Semantic Warning:");

                if (!(javaName == null
                    || line.startsWith("[read ")
                    || line.startsWith("] ")
                    || warningIndex >=0 )){

                    try {
                        // line number is from beginning of line to first period (.).
                        int firstDot = line.indexOf('.');
                        if (firstDot>0){
                            String nr = line.substring(0, firstDot);
                            String lineNumber="";
                            for (int i=0;i<nr.length();i++) {
                                if (nr.charAt(i)>='0' && nr.charAt(i)<='9')
                                    lineNumber+=nr.charAt(i);
                            }
                            int lineNr = Integer.parseInt(lineNumber);

                            // Now do the mapping
                            String mapping = findMapping(jspLineIds, lineNr, javaName);
                            if (mapping == null) {
                                errorMsg.append(separatorString);  // Defect 211450
                            }
                            else {
                                errorMsg.append(mapping);
                            }
                        }
                    }
                    catch (NumberFormatException ex) {
                        // If for some reason our guess at the location of the line
                        // number failed, time to give up.
                    }
                }
                errorMsg.append (line);
                errorMsg.append(separatorString);  // Defect 211450
                line = br.readLine();
            }
            br.close();
            //map.clear();
        }
        catch (IOException e) {
			logger.logp(Level.WARNING, CLASS_NAME, "getJspLineErrors", "Failed to find line number mappings for compiler errors", e);
        }
        return errorMsg.toString();
    }

    private String findMapping(Collection jspLineIds, int lineNr, String javaName) {
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
	//	defect 203009 - add logic to 1. narrow down error to single line in JSP, if possible, 2) improve
	// 					error messages to indicate when a file is statically included, and give
	//					name of parent file 
    private String createErrorMsg(JspLineId jspLineId, int errorLineNr) {
        StringBuffer compilerOutput = new StringBuffer();

        if (jspLineId.getSourceLineCount() <= 1) {
			Object[] objArray = new Object[] { new Integer(jspLineId.getStartSourceLineNum()), jspLineId.getFilePath()};
			if (jspLineId.getFilePath().equals(jspLineId.getParentFile())) {
				compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.single.line.number", objArray));  // Defect 211450
			}
			else {
				compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.single.line.number.included.file", objArray));  // Defect 211450
			}
        }
        else {
			// compute exact JSP line number 
			int actualLineNum=jspLineId.getStartSourceLineNum()+(errorLineNr-jspLineId.getStartGeneratedLineNum());
			if (actualLineNum>=jspLineId.getStartSourceLineNum() && actualLineNum <=(jspLineId.getStartSourceLineNum() + jspLineId.getSourceLineCount() - 1)) {
				Object[] objArray = new Object[] { new Integer(actualLineNum), jspLineId.getFilePath()};
				if (jspLineId.getFilePath().equals(jspLineId.getParentFile())) {
					compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.single.line.number", objArray));  // Defect 211450
				}
				else {
					compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.single.line.number.included.file", objArray));  // Defect 211450
				}
			}
			else {
				Object[] objArray = new Object[] {
					new Integer(jspLineId.getStartSourceLineNum()),
					new Integer((jspLineId.getStartSourceLineNum()) + jspLineId.getSourceLineCount() - 1),
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

		compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.corresponding.servlet",new Object[] { jspLineId.getParentFile()}));  // Defect 211450
		    //152470 starts
	        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
	            logger.logp(Level.FINER, CLASS_NAME, "createErrorMsg", "The value of the JSP attribute jdkSourceLevel is ["+ jdkSourceLevel +"]");
	        }
	        //152470 ends
        return compilerOutput.toString();
    }
}
