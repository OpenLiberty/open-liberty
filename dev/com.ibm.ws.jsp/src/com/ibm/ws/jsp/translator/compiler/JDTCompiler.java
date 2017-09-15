/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.translator.compiler;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ClassFile;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies;
import org.eclipse.jdt.internal.compiler.ICompilerRequestor;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFormatException;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;

import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.util.FileSystem;
import com.ibm.wsspi.jsp.compiler.JspCompiler;
import com.ibm.wsspi.jsp.compiler.JspCompilerResult;
import com.ibm.wsspi.jsp.compiler.JspLineId;
import com.ibm.wsspi.jsp.resource.translation.JspResources;

public class JDTCompiler implements JspCompiler {
    private static final String separatorString = System.getProperty("line.separator");
    static protected final Logger logger = Logger.getLogger("com.ibm.ws.jsp"); ;
        private static final String CLASS_NAME="com.ibm.ws.jsp.translator.compiler.JDTCompiler";
    
        private ClassLoader loader = null;
        private String javaEncoding = null; 
        private String outputDir = null;
        private boolean isClassDebugInfo = false;
        private boolean isDebugEnabled = false;
        private boolean isVerbose = false;
        private boolean isDeprecation = false; 
        private String jdkSourceLevel=null;
        private boolean useFullPackageNames = false;
        
        public JDTCompiler(ClassLoader loader, JspOptions options) {
                this.loader = loader;
                javaEncoding = options.getJavaEncoding();
                outputDir = options.getOutputDir().getPath();
        isClassDebugInfo = options.isClassDebugInfo();
        isDebugEnabled = options.isDebugEnabled();
        isVerbose = options.isVerbose();
        isDeprecation =  options.isDeprecation();
        jdkSourceLevel =  options.getJdkSourceLevel();
        useFullPackageNames = options.isUseFullPackageNames();
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "JDTCompiler", "Entering JDTCompiler.");
        }
        }

        public JspCompilerResult compile(String source, Collection jspLineIds, List compilerOptions) {
                return null;
        }
        
        public JspCompilerResult compile(JspResources[] jspResources, JspResources[] dependencyResources, Collection jspLineIds, List compilerOptions) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "JDTCompiler", "Entering JDTCompiler.compile");
        }
                JspCompilationUnit[] jspCompilationUnits = new JspCompilationUnit[jspResources.length];
                for (int i = 0; i < jspResources.length; i++) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "JDTCompiler", "Adding compilation unit for ["+jspResources[i].getClassName()+"]");
                }
                        jspCompilationUnits[i] = new JspCompilationUnit(jspResources[i]); 
                }
                Map<String,String> compilerOptionsMap = new HashMap<String,String>();
        if (isClassDebugInfo || this.isDebugEnabled) {
                compilerOptionsMap.put(CompilerOptions.OPTION_LineNumberAttribute, CompilerOptions.GENERATE);
            compilerOptionsMap.put(CompilerOptions.OPTION_LocalVariableAttribute, CompilerOptions.GENERATE);
            compilerOptionsMap.put(CompilerOptions.OPTION_SourceFileAttribute, CompilerOptions.GENERATE);
            compilerOptionsMap.put(CompilerOptions.OPTION_PreserveUnusedLocal, CompilerOptions.PRESERVE);            
        }
        compilerOptionsMap.put(CompilerOptions.OPTION_Encoding, javaEncoding);
        
        //487396.1 jdkSourceLevel is 15 by default now ... should get into if statement
        if (jdkSourceLevel.equals("14")) {
            compilerOptionsMap.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_4);
            compilerOptionsMap.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_4);  //PM32704
            compilerOptionsMap.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);  //PM32704
        }
        else if (jdkSourceLevel.equals("15")) {
            compilerOptionsMap.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
            compilerOptionsMap.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);  // 341708        
            compilerOptionsMap.put(CompilerOptions.OPTION_TargetPlatform,CompilerOptions.VERSION_1_5); // 412312
        }
        //PM04610 start
        else if (jdkSourceLevel.equals("16")) {
            compilerOptionsMap.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_6);
            compilerOptionsMap.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_6);  // 341708        
            compilerOptionsMap.put(CompilerOptions.OPTION_TargetPlatform,CompilerOptions.VERSION_1_6); // 412312
        }
        //PM04610 end
        else if (jdkSourceLevel.equals("17")) {
            compilerOptionsMap.put(CompilerOptions.OPTION_Source, "1.7"); //can I do this?? no CompilerOptions.VERSION_1_7 yet
            compilerOptionsMap.put(CompilerOptions.OPTION_Compliance, "1.7");       
            compilerOptionsMap.put(CompilerOptions.OPTION_TargetPlatform, "1.7");
        }
        //126902 start
        else if (jdkSourceLevel.equals("18")) {
            compilerOptionsMap.put(CompilerOptions.OPTION_Source, "1.8"); //Verify, no CompilerOptions.VERSION_1_8 yet.
            compilerOptionsMap.put(CompilerOptions.OPTION_Compliance, "1.8");  
            compilerOptionsMap.put(CompilerOptions.OPTION_TargetPlatform, "1.8");
        }
        //126902 end
        else {
            compilerOptionsMap.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_3);
            compilerOptionsMap.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_3);  //PM32704
            compilerOptionsMap.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_3);  //PM32704
        }
        
        if (isDeprecation) {
                compilerOptionsMap.put(CompilerOptions.OPTION_ReportDeprecation, CompilerOptions.WARNING);
        }
        
        if (isVerbose) {
                
        }
        
        JspCompilerRequestor jspCompilerRequestor = new JspCompilerRequestor(jspLineIds);
        
//      Compiler compiler = new Compiler(new JspNameEnvironment(jspResources, dependencyResources),
//      DefaultErrorHandlingPolicies.proceedWithAllProblems(),
//      compilerOptionsMap,
//      jspCompilerRequestor,
//      new DefaultProblemFactory(Locale.getDefault()));
        
        Compiler compiler = new Compiler(new JspNameEnvironment(jspResources, dependencyResources),
                DefaultErrorHandlingPolicies.proceedWithAllProblems(),
                new CompilerOptions(compilerOptionsMap),
                jspCompilerRequestor,
                new DefaultProblemFactory(Locale.getDefault()));
        
        
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, "JDTCompiler", "about to call compiler.compile()");
        }
                compiler.compile(jspCompilationUnits);
                return jspCompilerRequestor.getJspCompilerResult();
        }
        
        public class JspCompilationUnit implements ICompilationUnit {
                private String sourcePath = null;
                private String jspClassName = null;
                private String sourceFileName=null;  // defect 351116
                
                public JspCompilationUnit(JspResources jspResources) {
                        sourceFileName=jspResources.getGeneratedSourceFile().getName(); // defect 351116
                        sourcePath = jspResources.getGeneratedSourceFile().getPath();
                        jspClassName = ((jspResources.getPackageName().length() != 0) ? (jspResources.getPackageName() + ".") : "") + jspResources.getClassName();
                }
                
        public char[] getContents() {
                        char[] contents = null;

            Reader r = null;
            try {
                r = new BufferedReader(new InputStreamReader(new FileInputStream(sourcePath), javaEncoding));
                CharArrayWriter writer = new CharArrayWriter();
                char[] buff = new char[4096];
                int charCount = 0;
                while ((charCount = r.read(buff, 0, buff.length)) > 0) {
                    writer.write(buff, 0, charCount);
                }
                contents = writer.toCharArray();
            } 
            catch (IOException e) {
            } 
            finally {
                if (r != null) { try { r.close();} catch (IOException e) {}}
            }
            return contents;
                }
        
                public char[] getMainTypeName() {
                        char[] mainTypeName = null;
            int dot = jspClassName.lastIndexOf('.');
            if (dot > 0) {
                mainTypeName = jspClassName.substring(dot + 1).toCharArray();
            }
            else {
                mainTypeName = jspClassName.toCharArray();
            }
            return mainTypeName;
                }
                
                public char[][] getPackageName() {
            StringTokenizer st = new StringTokenizer(jspClassName, ".");
            char[][] packages = new char[st.countTokens()-1][];
            for (int i = 0; i < packages.length; i++) {
                String token = st.nextToken();
                packages[i] = token.toCharArray();
            }
            return packages;
                }
                
                public char[] getFileName() {
                        return sourceFileName.toCharArray();  // defect 351116
                }
                
                public String getJspClassName() {
                        return jspClassName;
                }
                
                public String getSourcePath() {
                        return sourcePath;
                }

                /* (non-Javadoc)
                 * @see org.eclipse.jdt.internal.compiler.env.ICompilationUnit#ignoreOptionalProblems()
                 */
                @Override
                public boolean ignoreOptionalProblems() {
                    return true;
                }
        }
        
        public class JspNameEnvironment implements INameEnvironment {
                private JspCompilationUnit[] jspCompilationUnits = null;
        private JspResources[] dependencyResources = null;
                
                public JspNameEnvironment(JspResources[] jspResources, JspResources[] dependencyResources) {
                        jspCompilationUnits = new JspCompilationUnit[jspResources.length];
                        for (int i = 0; i < jspResources.length; i++) {
                                jspCompilationUnits[i] = new JspCompilationUnit(jspResources[i]); 
                        }
            this.dependencyResources = dependencyResources;
                }
                
                public void cleanup() {
                }
                
                public NameEnvironmentAnswer findType(char[] typeName, char[][] packageName) {
            String result = "";
            String sep = "";
            for (int i = 0; i < packageName.length; i++) {
                result += sep;
                result += new String(packageName[i]);
                sep = ".";
            }
            result += sep;
            result += new String(typeName);
            return getClass(result);
                }
                
                public NameEnvironmentAnswer findType(char[][] compoundTypeName) {
            String result = "";
            String sep = "";
            for (int i = 0; i < compoundTypeName.length; i++) {
                result += sep;
                result += new String(compoundTypeName[i]);
                sep = ".";
            }
            return getClass(result);
                }
                
                public boolean isPackage(char[][] parentPackageName, char[] packageName) {
            String result = "";
            String sep = "";
            if (parentPackageName != null) {
                for (int i = 0; i < parentPackageName.length; i++) {
                    result += sep;
                    String str = new String(parentPackageName[i]);
                    result += str;
                    sep = ".";
                }
            }
            String str = new String(packageName);
            if (Character.isUpperCase(str.charAt(0))) {
                if (!isPackage(result)) {
                    return false;
                }
            }
            result += sep;
            result += str;
            return isPackage(result);
                }
                
                private boolean isPackage(String result) {
            InputStream is = null;
            boolean retbool=false;
                        try {
                                for (int i = 0; i < jspCompilationUnits.length; i++) {
                            if (result.equals(jspCompilationUnits[i].getJspClassName())) {
                                return false;
                            }
                                }
                    String resourceName = result.replace('.', '/') + ".class";
                    is = loader.getResourceAsStream(resourceName);
                    retbool=(is==null);
                    
                  //Start PK61357
                 if(!retbool && FileSystem.isCaseInsensitive){
                    String dir = result.replace('.', '/');
                    
                    // There is no way to make this work for zip files that were created without
                    // explicit directory entries.
                    URL u = loader.getResource(dir + "/");
                    if (u != null){
                         if(u.getProtocol().equals("file")){
                              // On most unix platforms, "/path/to/file/." does not exist, but
                              // "/path/to/dir/." does, so we can use that as a descriminator.  On
                              // Windows only, "/path/to/file/." does exist, so we can't use that.
                              // Fortunately, "/path/to/file/..." does not exist, but
                              // "/path/to/dir/..." does, so we can use that instead.
                              // For iSeries it appears that /. exists for both files and directories
                              // but that /.. is unique to directories.
                              String toAppend = "/.";
                              if(System.getProperty("os.name").toLowerCase().startsWith("windows"))
                                   toAppend = "/...";
                              else if(System.getProperty("os.name").toLowerCase().equals ("os/400"))
                                   toAppend = "/..";
                              
                              String dir2 = dir + toAppend;
                              retbool = loader.getResource(dir2) != null;
                              if (logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, "JDTCompiler", "retbool =" + retbool + " for path " + dir2);
                            }
                         } else {
                              if (logger.isLoggable(Level.FINE)) {
                                logger.logp(Level.FINE, CLASS_NAME, "JDTCompiler", "loader.getResource returned non null and protocol was not file");
                            }
                              retbool = true;
                         }
                    }    
                 } // End PK61357
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException exc) {
                        }
                    }
                }
                return retbool;
                }
                
                private NameEnvironmentAnswer getClass(String className) {
                InputStream is = null;
            ByteArrayOutputStream baos = null;
            NameEnvironmentAnswer answer = null;
            try {
                for (int i = 0; i < jspCompilationUnits.length; i++) {
                    if (className.equals(jspCompilationUnits[i].getJspClassName())) {
                        return new NameEnvironmentAnswer(jspCompilationUnits[i], null);
                    }
                }
                
                if (dependencyResources != null) {
                    for (int i = 0; i < dependencyResources.length; i++) {
                        String sourcePath = dependencyResources[i].getGeneratedSourceFile().getPath();
                        String dependencyClassName = ((dependencyResources[i].getPackageName().length() != 0) ? (dependencyResources[i].getPackageName() + ".") : "") + dependencyResources[i].getClassName();
                        
                        if (className.equals(dependencyClassName)) {
                            byte[] tagFileClassBytes = getClassFileBytes(sourcePath);
                            if (tagFileClassBytes != null) {
                                ClassFileReader classFileReader = new ClassFileReader(tagFileClassBytes, className.toCharArray(), true);
                                return new NameEnvironmentAnswer(classFileReader, null);
                            }
                        }
                    }
                }
                
                String resourceName = className.replace('.', '/') + ".class";
                is = loader.getResourceAsStream(resourceName);
                if (is != null) {
                    byte[] classBytes;
                    byte[] buff = new byte[4096];
                    baos = new ByteArrayOutputStream(buff.length);
                    int count;
                    while ((count = is.read(buff, 0, buff.length)) > 0) {
                        baos.write(buff, 0, count);
                    }
                    baos.flush();
                    classBytes = baos.toByteArray();
                    ClassFileReader classFileReader = new ClassFileReader(classBytes, className.toCharArray(), true);
                    answer = new NameEnvironmentAnswer(classFileReader, null);
                }
            } catch (IOException e) {
                FFDCFilter.processException(e, this.getClass().getName() + ".getClass(String)", "403");
            } catch (ClassFormatException e) {
                FFDCFilter.processException(e, this.getClass().getName() + ".getClass(String)", "405");
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException exc) {
                    }
                }
                if (baos != null) {
                    try {
                        baos.close();
                    } catch (IOException exc) {
                    }
                }
            }
            return answer;
                }
        
        
        public byte[] getClassFileBytes(String sourcePath) {
            byte[] classBytes = null;
            InputStream is = null;
            ByteArrayOutputStream baos = null;
            try {
                String classFilePath = sourcePath.substring(0, sourcePath.lastIndexOf('.'));
                classFilePath += ".class";
                is = new FileInputStream(classFilePath);
                byte[] buff = new byte[4096];
                baos = new ByteArrayOutputStream(buff.length);
                int count;
                while ((count = is.read(buff, 0, buff.length)) > 0) {
                    baos.write(buff, 0, count);
                }
                baos.flush();
                classBytes = baos.toByteArray();
            }
            catch (IOException e) {
            }
            finally {
                if (is != null) { try { is.close();} catch (IOException e) {}}
                if (baos != null) { try { baos.close();} catch (IOException e) {}}
            }
            return classBytes;             
        }
        }
        
        public class JspCompilerRequestor implements ICompilerRequestor {
                private int rc = 0;
                private StringBuffer compilerMessage = null;
        private Collection jspLineIds = null;
                
                public JspCompilerRequestor(Collection jspLineIds) {
            this.jspLineIds = jspLineIds;
        }
                
                public void acceptResult(CompilationResult compilationResult) {
            boolean compileSucceeded = true;
                        if (compilationResult.hasProblems()) {
                //IProblem[] problems = compilationResult.getProblems();
                CategorizedProblem[] problems = compilationResult.getProblems();                                
                if (compilerMessage == null) {
                    compilerMessage = new StringBuffer();
                }
                for (int i = 0; i < problems.length; i++) {
                    IProblem problem = problems[i];
                    if (problem.isError()) {
                        compileSucceeded = false;
                        rc = 1;
                        addMessage(problem, ((JspCompilationUnit)compilationResult.getCompilationUnit()).getSourcePath());
                    }
                    else if (problem.isWarning() && (isVerbose || isDeprecation)) {
                        addMessage(problem, ((JspCompilationUnit)compilationResult.getCompilationUnit()).getSourcePath());
                    }
                }
                        }
                        if (compileSucceeded) {
                                OutputStream os = null;
                ClassFile[] classFiles = compilationResult.getClassFiles();
                try {
                        for (int i = 0; i < classFiles.length; i++) {
                            ClassFile classFile = classFiles[i];
                            char[][] compoundName = classFile.getCompoundName();
                            String className = "";
                            String sep = "";
                            for (int j = 0; j < compoundName.length; j++) {
                                className += sep;
                                className += new String(compoundName[j]);
                                sep = ".";
                            }
                            byte[] bytes = classFile.getBytes();
                            String outFile = null;
                            if (useFullPackageNames)
                                outFile = outputDir + "/" + className.replace('.', '/') + ".class";
                            else {
                                outFile = ((JspCompilationUnit)compilationResult.getCompilationUnit()).getSourcePath();
                                outFile = outFile.substring(0, outFile.lastIndexOf(java.io.File.separatorChar)+1);
                                outFile += new String (compoundName[compoundName.length -1]);
                                outFile += ".class";
                            }
                            os =  new BufferedOutputStream(new FileOutputStream(outFile));
                            os.write(bytes);
                            os.close();
                        }
                }
                catch (IOException e) {
                    FFDCFilter.processException(e, this.getClass().getName() + ".acceptResult()", "510");
                }
                finally {
                    if (os != null) {try {os.close();} catch (IOException exc) {}}
                }
                        }
                }
        
        private void addMessage(IProblem problem, String sourcePath) {
            String mapping = findMapping(problem.getSourceLineNumber(), sourcePath);
            if (mapping == null) {
                compilerMessage.append(separatorString);
            }
            else {
                compilerMessage.append(mapping);
                compilerMessage.append(separatorString);
            }
            
            compilerMessage.append(sourcePath);
            compilerMessage.append(" : ");
            compilerMessage.append(problem.getSourceLineNumber());
            compilerMessage.append(" : ");
            compilerMessage.append(problem.getMessage());
            compilerMessage.append(separatorString);
        }
                
                public JspCompilerResult getJspCompilerResult() {
                        JspCompilerResult jspCompilerResult = new JspCompilerResult() {
                            public int getCompilerReturnValue() {
                                return rc;
                            }
                            public String getCompilerMessage() {
                                if (compilerMessage != null) {
                                        return compilerMessage.toString();
                                }
                                else {
                                        return "";
                                }
                            }
                            //PK72039 start
                            // not needed for JDTCompiler...just return null
                            public List getCompilerFailureFileNames() {
                                return null;
                            }
                            //PK72039 end
                                
                        };
                        return jspCompilerResult;
                }
        
        private String findMapping(int lineNr, String javaName) {
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
        //  defect 203009 - add logic to 1. narrow down error to single line in JSP, if possible, 2) improve
        //                  error messages to indicate when a file is statically included, and give
        //                  name of parent file 
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

            compilerOutput.append(separatorString+JspCoreException.getMsg("jsp.error.corresponding.servlet",new Object[] { jspLineId.getParentFile()})+separatorString);  // Defect 211450
            //152470 starts
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINER)) {
                logger.logp(Level.FINER, CLASS_NAME + "$JspCompilerRequestor", "createErrorMsg", "The value of the JSP attribute jdkSourceLevel is ["+ jdkSourceLevel +"]");
            }
            //152470 ends
            return compilerOutput.toString();
        }
        }
}
