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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
//import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.ws.jsp.Constants;
import com.ibm.ws.jsp.JspCoreException;
import com.ibm.ws.jsp.JspOptions;
import com.ibm.ws.jsp.configuration.JspConfigurationManager;
import com.ibm.ws.jsp.configuration.JspXmlExtConfig;
import com.ibm.ws.jsp.runtime.JspClassInformation;
import com.ibm.ws.jsp.taglib.GlobalTagLibraryCache;
import com.ibm.ws.jsp.taglib.TagLibraryCache;
import com.ibm.wsspi.jsp.taglib.config.GlobalTagLibConfig;
import com.ibm.ws.jsp.tools.JspFileUtils.InnerclassFilenameFilter;
import com.ibm.ws.jsp.translator.utils.JspTranslationResult;
import com.ibm.ws.jsp.translator.utils.JspTranslatorUtil;
import com.ibm.ws.jsp.translator.utils.SmapGenerator;
import com.ibm.ws.jsp.translator.visitor.xml.ParserFactory;
import com.ibm.ws.jsp.webcontainerext.JSPExtensionClassLoader;
import com.ibm.ws.jsp.webcontainerext.JspDependent;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;
import com.ibm.wsspi.jsp.compiler.JspCompiler;
import com.ibm.wsspi.jsp.compiler.JspCompilerResult;
import com.ibm.wsspi.jsp.context.JspClassloaderContext;
import com.ibm.wsspi.jsp.context.translation.JspTranslationContext;
import com.ibm.wsspi.jsp.resource.JspInputSource;
import com.ibm.wsspi.jsp.resource.translation.JspResources;
import com.ibm.wsspi.jsp.resource.translation.TagFileResources;
import com.ibm.wsspi.jsp.tools.JspToolsOptionKey;
import com.ibm.wsspi.jsp.tools.JspToolsOptionsMap;

public abstract class AbstractJspModC {

    protected String _appDir = null;
    protected String _tmpDir = null;
    protected String _configDir = null;
    protected String classpath = "";
    //begin 241038: if user specified appClasspath
    protected String _appClasspath = "";
    //end 241038: if user specified appClasspath
    protected String jspFile = null;
    protected String dir = null;
    protected int returnCode = 0;
    private boolean forceCompilation = false;
    private boolean removeV4Files = false; // defect 196156
    private boolean recurse = true;
    private boolean compileJsps = true;
    private ClassLoader loader = null;
    boolean searchClasspathForResources = false; // defect 201520
    private JspToolsOptionsMap optionOverrides = null;
    private boolean createDebugClassfiles = false;
    private boolean keepGeneratedclassfiles = true;
    private Map looseLibMap=null;
    private Map taglibMap=null;
    static private final String WEB_XML = "/WEB-INF/web.xml";
    private static final String CLASS_NAME="com.ibm.ws.jsp.tools.AbstractJspModC";

    private List extensionFilter = null;
    private List compilerOptions = null;

    private Logger logger=null;
    //private Level defaultLogLevel = Level.INFO;  // defect 198631
    //private Handler consoleHandler = null;
    // defect 201384 begin
    private File genWebXml=null;
    private Document genWebXmlDocument = null;
    private List genWebXmlMappings=null;
    // defect 201384 end
    private String[] sharedLibraryJarList = null;
    private GlobalTagLibConfig[] tagLibConfigArray=null;

    private JspFileUtils fileUtil=null; // defect 196156
    protected Properties webConProperties = new Properties();// defect 400645
    
    private List translatedJspsFileNames=null;  //PK72039
    private boolean compileAfterFailure = false; //PK72039
    
    private String extDocumentRoot = null; //Feature F003834
    private String preFragmnetExtendedDocumentRoot = null;
    
    public AbstractJspModC() {
        this(null, null);
    }

    public AbstractJspModC(String appDir, String tmpDir) {
        _appDir = appDir;
        _tmpDir = tmpDir;
    }

    public AbstractJspModC(String[] args) {
        parseCmdLine(args);
    }


    public int compileApp(String appDir, String tmpDir) {
        _appDir = appDir;
        _tmpDir = tmpDir;
        return compileApp();
    }

    public void createLogger() {
        if (logger == null) {
            logger = Logger.getLogger("com.ibm.ws.jsp","com.ibm.ws.jsp.resources.messages");
            /*   defect 198631
            logger.setUseParentHandlers(false);
            logger.setLevel(defaultLogLevel);

            Handler[] handlers = logger.getHandlers();

            for (int i = 0; i < handlers.length; i++) {
                if (handlers[i] instanceof ConsoleHandler) {
                    return;
                }
            }

            consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(defaultLogLevel);
            consoleHandler.setFormatter(new BasicLogFormatter());
            logger.addHandler(consoleHandler);*/
        }
    }

    public int internalBuildProcessCompileApp(String appDir, String tmpDir) throws JspCoreException {
        _appDir = appDir;
        _tmpDir = tmpDir;
        boolean doTraceCheck = true; //since the isAnyTracingEnabled check doesn't work for the build process, always check
        int retval = _internalCompileApp(doTraceCheck);
        if (logger!=null) {
            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                logger.logp(Level.INFO, CLASS_NAME,"internalBuildProcessCompileApp", "Returning from internalBuildProcessCompileApp. returnCode: ["+this.returnCode+"]");
            }
        }
        return retval;
    }

    public int compileApp() {
        int retval=1;
        try {
            retval = _internalCompileApp(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled());
        } catch (JspCoreException e) {
            // Do not print a stack trace!  Callers do not expect it.
        }
        return retval;
    }

    private int _internalCompileApp(boolean doTraceCheck) throws JspCoreException {
        if (this.returnCode > 0) {
            return this.returnCode;
        }
        // begin 238852.3: keep track if jarClassLoader was created.
        boolean jarClassloaderCreatedThisCall = false;
        // end 238852.3: keep track if jarClassLoader was created.
        // SDJ 03/07/2005
        ClassLoader oldLoader=null;
        // SDJ 03/07/2005
        try {
            createLogger();
            if (logger!=null) {
                if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                    logger.logp(Level.INFO, CLASS_NAME,"_internalCompileApp", "_internalCompileApp()  Dir To Compile: ["+this._appDir+"] Compiling to: ["+this._tmpDir+"]");
                }
            }
        // defect 196156 - begin
            this.fileUtil=new JspFileUtils(this.logger);
            // defect 196156 - end
            String contextDir=null;
            if(_configDir != null){
                contextDir = _configDir;
            }
            else{
                contextDir = _appDir;
            }
            boolean webXmlExists = AbstractJspModC.checkForWebxml(contextDir);
            Map tagLibraryMap=null;
            JspXmlExtConfig jspCfg= null;
            if (webXmlExists) {
                jspCfg = createConfig(contextDir);
                tagLibraryMap = jspCfg.getTagLibMap();
            }
            else {
                tagLibraryMap = new HashMap();
            }
            // merge TaglibMap into tagLibraryMap
            if (taglibMap != null && !taglibMap.isEmpty()) {
                tagLibraryMap.putAll(taglibMap);
            }

            if (loader == null) {
                loader = createClassloader();
                jarClassloaderCreatedThisCall = true;
            }
            else{
                loader = addAdditionalClasspathToClassloader(loader);           //PM06503
                if (logger!=null)
                    logger.logp(Level.FINE, CLASS_NAME,"compileApp","Using "+ loader.getClass().getName() +" for compiling and translating JSPs");
            }
            
            JspOptions options=null;
            if (webXmlExists) {
                options = jspCfg.getJspOptions();
            }
            else {
                options = new JspOptions(new Properties());
            }
            // override options with properties set by caller:
            if (this.optionOverrides != null && options != null) {
                Boolean booleanProperty = null;
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.keepGeneratedKey);
                if (booleanProperty != null) {
                    options.setKeepGenerated(booleanProperty.booleanValue());
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.verboseKey);
                if (booleanProperty != null) {
                    options.setVerbose(booleanProperty.booleanValue());
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.deprecationKey);
                if (booleanProperty != null) {
                    options.setDeprecation(booleanProperty.booleanValue());
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useJDKCompilerKey);
                if (booleanProperty != null) {
                    options.setUseJDKCompiler(booleanProperty.booleanValue());
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useJikesKey);
                if (booleanProperty != null) {
                    options.setUseJikes(booleanProperty.booleanValue());
                }
                // Normalize compileWithAssert and jdkSourceLevel; compileWithAssert with value true means compile with
                // jdk 1.4 source level.  Only jdkSourceLevel will be used elsewhere in the JSP container.
                Boolean assertProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.compileWithAssertKey);
                String jdkLevel = (String) this.optionOverrides.get(JspToolsOptionKey.jdkSourceLevelKey);
                if (assertProperty!=null && assertProperty.booleanValue()==true) {
                    if (logger!=null) {
                        if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                            logger.logp(Level.INFO, CLASS_NAME,"compileApp", "compileWithAssert is deprecated.  Use jdkSourceLevel with value of '14' instead. ");
                        }
                    }
                    // if jdkSourceLevel was not set, then set it to 1.5 level
                    //487396.1 changed default from 1.3 to 1.5
                    //F000743-28610 changed default from 1.5 to 1.6
                    if (jdkLevel==null) {
                        jdkLevel="16";
                        if (logger!=null) {
                            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                                logger.logp(Level.INFO, CLASS_NAME,"compileApp", "Setting jdkSourceLevel to '16' because compileWithAssert is true.");
                            }
                        }
                    }
                    else if (jdkLevel.equals("13")){
                        // if jdkSourceLevel was set to 1.3, then set it to 1.4 level to honor higher setting of compileWithAssert
                        jdkLevel="14";
                        if (logger!=null) {
                            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                                logger.logp(Level.INFO, CLASS_NAME,"compileApp", "jdkSourceLevel was set to '13'.  Setting jdkSourceLevel to '14' because compileWithAssert is true.");
                            }
                        }
                    }
                    else if (jdkLevel.equals("15")){
                        // if jdkSourceLevel was set to 1.5, then leave it and log situation
                        if (logger!=null) {
                            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                                logger.logp(Level.INFO, CLASS_NAME,"compileApp", "jdkSourceLevel is set to '15' and compileWithAssert is 'true'.  Leaving jdkSourceLevel at higher '15' level.");
                            }
                        }
                    }
                    //PM04610 start
                    else if (jdkLevel.equals("16")){
                        // if jdkSourceLevel was set to 1.6, then leave it and log situation
                        if (logger!=null) {
                            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                                logger.logp(Level.INFO, CLASS_NAME,"compileApp", "jdkSourceLevel is set to '16' and compileWithAssert is 'true'.  Leaving jdkSourceLevel at higher '16' level.");
                            }
                        }
                    }
                    //PM04610 end
                    else if (jdkLevel.equals("17")){
                        // if jdkSourceLevel was set to 1.7, then leave it and log situation
                        if (logger!=null) {
                            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                                logger.logp(Level.INFO, CLASS_NAME,"compileApp", "jdkSourceLevel is set to '17' and compileWithAssert is 'true'.  Leaving jdkSourceLevel at higher '17' level.");
                            }
                        }
                    }
                    //126902 start
                    else if (jdkLevel.equals("18")){
                        // if jdkSourceLevel was set to 1.8, then leave it and log situation
                        if (logger!=null) {
                            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                                logger.logp(Level.INFO, CLASS_NAME,"compileApp", "jdkSourceLevel is set to '18' and compileWithAssert is 'true'.  Leaving jdkSourceLevel at higher '18' level.");
                            }
                        }
                    }
                    //126902 end
                }
                if (jdkLevel!=null
                  && !jdkLevel.equals("13")
                  && !jdkLevel.equals("14")
                  && !jdkLevel.equals("15")
                  && !jdkLevel.equals("16") //PM04610
                  && !jdkLevel.equals("17")
                  && !jdkLevel.equals("18")) { //126902
                    jdkLevel=null;
                }
                if (jdkLevel != null) {
                    options.setJdkSourceLevel(jdkLevel);
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.usePageTagPoolKey);
                if (booleanProperty != null) {
                    options.setUsePageTagPool(booleanProperty.booleanValue());
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useThreadTagPoolKey);
                if (booleanProperty != null) {
                    options.setUseThreadTagPool(booleanProperty.booleanValue());
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.trackDependenciesKey);
                if (booleanProperty != null) {
                    options.setTrackDependencies(booleanProperty.booleanValue());
                }
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useFullPackageNamesKey);
                if (booleanProperty != null) {
                    options.setUseFullPackageNames(booleanProperty.booleanValue());
                }
                String compileClasspath = (String) this.optionOverrides.get(JspToolsOptionKey.jspCompileClasspathKey);
                if (compileClasspath != null) {
                    options.setJspCompileClasspath(compileClasspath);
                }
                String javaEncoding = (String) this.optionOverrides.get(JspToolsOptionKey.javaEncodingKey);
                if (javaEncoding != null) {
                    options.setJavaEncoding(javaEncoding);
                }
                //PK26741
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useRepeatInt);
                if (booleanProperty != null) {
                    options.setUseRepeatInt(booleanProperty.booleanValue());
                }
                //PK29373
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useScriptVarDupInit);
                if (booleanProperty != null) {
                    options.setUseScriptVarDupInit(booleanProperty.booleanValue());
                }
                //393421
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.allowJspOutputElementMismatch);
                if (booleanProperty != null) {
                    options.setAllowJspOutputElementMismatch(booleanProperty.booleanValue());
                }
                //396002
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.allowTaglibPrefixRedefinition);
                if (booleanProperty != null) {
                    options.setAllowTaglibPrefixRedefinition(booleanProperty.booleanValue());
                }
                //396002
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.allowTaglibPrefixUseBeforeDefinition);
                if (booleanProperty != null) {
                    options.setAllowTaglibPrefixUseBeforeDefinition(booleanProperty.booleanValue());
                }
                //PK30879
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.allowUnmatchedEndTag);
                if (booleanProperty != null) {
                    options.setAllowUnmatchedEndTag(booleanProperty.booleanValue());
                }
                //PK31135,402921
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useIterationEval);
                if (booleanProperty != null) {
                    options.setUseIterationEval(booleanProperty.booleanValue());
                }
				//PK34989
				booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.useCDataTrim);
				if (booleanProperty != null) {
					options.setUseCDataTrim(booleanProperty.booleanValue());
				}
                //PK47738
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.disableURLEncodingForParamTag);
                if (booleanProperty != null) {
                    options.setDisableURLEncodingForParamTag(booleanProperty.booleanValue());
                }
                //PK53233
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.evalQuotedAndEscapedExpression);
                if (booleanProperty != null) {
                    options.setEvalQuotedAndEscapedExpression(booleanProperty.booleanValue());
                }
                //PK62809
                // - The allowNullParentInTagFile is for safety...in case a tag needs to have setParent(null)..we can't think of why though.
                // - The allowNullParentInTagFile is not documented and we should leave it that way unless we run in to a problem..
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.allowNullParentInTagFile);
                if (booleanProperty != null) {
                    options.setAllowNullParentInTagFile(booleanProperty.booleanValue());
                }
                //PK65013 - start
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.modifyPageContextVar);
                if (booleanProperty != null) {
                    options.setModifyPageContextVariable(booleanProperty.booleanValue());
                }
                //PK65013 - end
                
                //PK72039 start
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.compileAfterFailureKey);
                if (booleanProperty != null) {
                    options.setCompileAfterFailure(booleanProperty.booleanValue());
                }
                //PK72039 end
                //650003 start
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.disableResourceInjection);
                if (booleanProperty != null) {
                    options.setDisableResourceInjection(booleanProperty.booleanValue());
                }
                //650003 end
                //PM21395 start
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.enableDoubleQuotesDecoding);
                if (booleanProperty != null) {
                    options.setEnableDoubleQuotesDecoding(booleanProperty.booleanValue());
                }
                //PM21395 end
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.enableCDIWrapper);
                if (booleanProperty != null) {
                    options.setEnableCDIWrapper(booleanProperty.booleanValue());
                }
                
                //PM41476 start
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.removeXmlnsFromOutput);
                if (booleanProperty != null) {
                    options.setRemoveXmlnsFromOutput(booleanProperty.booleanValue());
                }
                //PM41476 end
                //PM94792 start
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.doNotEscapeWhitespaceCharsInExpression);
                if(booleanProperty != null) {
                    options.setDoNotEscapeWhitespaceCharsInExpression(booleanProperty.booleanValue());
                }
                //PM94792 end
                //PI12939 start
                booleanProperty = (Boolean) this.optionOverrides.get(JspToolsOptionKey.deleteClassFilesBeforeRecompile);
                if (booleanProperty != null) {
                    options.setDeleteClassFilesBeforeRecompile(booleanProperty.booleanValue());
                }
                //PI12939 end
            }
            if (options!=null) {
                // Feature LIDB4293-2 force inmemory to off, but leave trackdependencies as is
                boolean trackDep=options.isTrackDependencies();
                options.setUseInMemory(false);
                options.setUseDevMode(false);
                options.setUseDevModeSet(false);
                options.setTrackDependencies(trackDep);
                if(doTraceCheck&&logger.isLoggable(Level.FINEST)){
                    logger.logp(Level.FINEST, CLASS_NAME,"_internalCompileApp", "JspOptions [" + options.toString()+"]");
                }            	
            }

            JspClassloaderContext jspClassloaderContext = createJspClassloaderContext(loader, jspCfg);

            // SDJ 03/07/2005
            oldLoader = ThreadContextHelper.getContextClassLoader();
            ThreadContextHelper.setClassLoader(loader);
            // SDJ 03/07/2005

            String commandLineFileExts=null;
            if (this.optionOverrides != null) {
                commandLineFileExts = (String) this.optionOverrides.get(JspToolsOptionKey.jspFileExtensionsKey);
            }
            JspConfigurationManager jspConfigurationManager = createJspConfigurationManager(webXmlExists, jspCfg, commandLineFileExts, fileUtil);
            if (options!=null && options.isEnableCDIWrapper()) {
                jspConfigurationManager.setJCDIEnabled(options.isEnableCDIWrapper());
            }

            List fileExtList = jspConfigurationManager.getJspExtensionList();
            extensionFilter = AbstractJspModC.buildJspFileExtensionList (Constants.STANDARD_JSP_EXTENSIONS, fileExtList);

            //Feature F003834
            // see if the global extDocumentRoot variable was set from the JspBatchCBase class first.
            //if not, then check the options.getExtenedDocumentRoot() like we originally did.

            //String extDocumentRoot = null;
            if (options != null) {
                if (extDocumentRoot == null) { //Feature F003834 - only check if it's null...since variables may be resolved and we don't want to overwrite them.
                    extDocumentRoot = options.getExtendedDocumentRoot();
                }
                if (this.preFragmnetExtendedDocumentRoot==null) {
                	preFragmnetExtendedDocumentRoot = options.getPreFragmentExtendedDocumentRoot();
                }
                options.setOutputDir(new File(_tmpDir));
                options.setDebugEnabled(this.createDebugClassfiles);
                options.setKeepGeneratedclassfiles(keepGeneratedclassfiles);
                options.setLooseLibMap(this.looseLibMap);
            }
            
            
            // defect 201520 - add searchClasspathForResources
            FileBasedJspContext context = new FileBasedJspContext(_appDir, options, extDocumentRoot, preFragmnetExtendedDocumentRoot,loader, jspClassloaderContext, searchClasspathForResources);

            GlobalTagLibraryCache globalTagLibraryCache = new GlobalTagLibraryCache(); // defect 213137 //TODO Why did tooling need to provide an application classloader ?
            if (tagLibConfigArray==null) {
                tagLibConfigArray=loadTagLibraryCache();
            }
            if (tagLibConfigArray!=null) {
                for (int i=0; i<tagLibConfigArray.length; i++) {
                    globalTagLibraryCache.addGlobalTagLibConfig(tagLibConfigArray[i]);
                }
            }
            
            if(doTraceCheck&&logger.isLoggable(Level.FINE)){
                logger.logp(Level.FINE, CLASS_NAME,"_internalCompileApp", "tagLibraryMap [" + tagLibraryMap +"]" + "sharedLibraryJarList ["+sharedLibraryJarList+"]");
            }
            
            TagLibraryCache tlc = new TagLibraryCache(context,
                                                      tagLibraryMap,
                                                      options,
                                                      jspConfigurationManager,
                                                      globalTagLibraryCache.getGlobalTagLibMapForWebApp(context, jspCfg),
                                                      globalTagLibraryCache.getImplicitTagLibPrefixMap(),
                                                      globalTagLibraryCache.getOptimizedTagConfigMap());

            if (sharedLibraryJarList != null) {
                List l = new ArrayList();
                for (int i = 0; i < sharedLibraryJarList.length; i++) {
                    File f = new File(sharedLibraryJarList[i]);
                    if(doTraceCheck&&logger.isLoggable(Level.FINE)){
                        logger.logp(Level.FINE, CLASS_NAME,"_internalCompileApp", "file f [" + f +"]" );
                    }
                    if (f.exists()) {
                        URL url = f.toURL();
                        if(doTraceCheck&&logger.isLoggable(Level.FINE)){
                            logger.logp(Level.FINE, CLASS_NAME,"_internalCompileApp", "url [" + url +"]" );
                        }
                        url = new URL("jar:"+url.toExternalForm()+"!/");
                        if(doTraceCheck&&logger.isLoggable(Level.FINE)){
                            logger.logp(Level.FINE, CLASS_NAME,"_internalCompileApp", "url [" + url +"] "+"sharedLibraryJarList[i] [" + sharedLibraryJarList[i] +"] "+"l [" + l +"] "+"jspCfg [" + jspCfg +"]");
                        }
                        tlc.loadTldsFromJar(url, sharedLibraryJarList[i], l, jspCfg);
                    }
                }
            }

            // defect 201384 begin
            if (options.isUseFullPackageNames()) {
                genWebXml =new File(_appDir + File.separator + "WEB-INF" + File.separator + Constants.GENERATED_WEBXML_FILENAME);
                this.genWebXmlMappings=new ArrayList();
                try {
                    genWebXmlDocument = ParserFactory.newDocument(false, false);
                    genWebXmlDocument.appendChild(genWebXmlDocument.createElement("web-app"));
                    genWebXmlDocument.getDocumentElement().appendChild(genWebXmlDocument.createComment("Auto-generated by WebSphere batch compiler @ "+new Date()));
                }
                catch (ParserConfigurationException e) {
                    StringWriter stringWriter = new StringWriter();
        			e.printStackTrace(new PrintWriter(stringWriter));
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"_internalCompileApp","Exception caught during processing  1  " +e.getMessage()+"  "+ stringWriter.toString());
                    genWebXml=null;
                }
            }
            // defect 201384 end
            if (jspFile != null) {
                // here if we're compiling a single JSP
                if (jspFile.charAt(0) != '/') {
                    jspFile = "/" + jspFile;
                }
                if (jspFile.startsWith("/META-INF") == false) {
                    String filePath = jspFile.substring(0,jspFile.lastIndexOf('/')+1);
                    JspDirectory files = new JspDirectory(filePath);
                    files.add(jspFile);
                    translateDir(files, context, jspConfigurationManager, options, tlc, true);
                }
                else {
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"_internalCompileApp","jsp.batchcompiler.compiling.in.metainf.not.allowed",jspFile);
                    this.returnCode = 1;
                }
            }
            else {
                // Here if we're compiling one or more directories of JSPs
                List dirFiles = new ArrayList();
                if (dir != null) { // specific directory and its subdirectories (PQ63812)
                    if (dir.length() == 0 || dir.charAt(0) != '/') {
                        dir = "/" + dir;
                    }
                    collectDirs(context, dir, dirFiles, this.recurse);
                }
                else { // the whole webapp
                    collectDirs(context, "/", dirFiles);
                }

                // if useFullPackageNames is false, remove the directory into which full packages
                //  are compiled (Constants.JSP_PACKAGE_PREFIX)
                if (options.isUseFullPackageNames()==false) {
                    String outputDir = options.getOutputDir().toString();
                    File dirToRemove = new File(outputDir + "/"+Constants.JSP_PACKAGE_PREFIX);
                    if (dirToRemove.exists() && dirToRemove.isDirectory()) {
                        JspFileUtils.deleteDirs(dirToRemove, this.logger);
                    }
                }

                if (dirFiles.isEmpty()) {
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"compileApp","No JSPs found in ["+context.getRealPath("/")+"]");
                }
                // defect 241285
                if (options.isUseFullPackageNames()) {
                    String base=(dir!=null?dir:"/");
                    JspDirectory files=new JspDirectory(base);
                    for (Iterator itr = dirFiles.iterator(); itr.hasNext();) {
                        files.addAll((JspDirectory) itr.next());
                    }
                    translateDir(files, context, jspConfigurationManager, options, tlc, false);
                }
                else {
                    for (Iterator itr = dirFiles.iterator(); itr.hasNext();) {
                        JspDirectory files = (JspDirectory) itr.next();
                        translateDir(files, context, jspConfigurationManager, options, tlc, false);
                    }
                }           }

            // defect 201384 begin
            // write servlet config info to file
            if (options.isUseFullPackageNames() && genWebXml !=null) {
                try {
                    for (Iterator itr = this.genWebXmlMappings.iterator(); itr.hasNext();) {
                        WebXmlMappings mapping = (WebXmlMappings) itr.next();
                        genWebXmlDocument.getDocumentElement().appendChild(genWebXmlDocument.createTextNode("\n\t"));
                        Element servletMappingElement = genWebXmlDocument.createElement("servlet-mapping");
                        Element servletNameElement = genWebXmlDocument.createElement("servlet-name");
                        Element urlPatternElement = genWebXmlDocument.createElement("url-pattern");
                        servletNameElement.appendChild(genWebXmlDocument.createTextNode(mapping.getServletName()));
                        urlPatternElement.appendChild(genWebXmlDocument.createTextNode(mapping.getServletMapping()));
                        servletMappingElement.appendChild(genWebXmlDocument.createTextNode("\n\t\t"));
                        servletMappingElement.appendChild(servletNameElement);
                        servletMappingElement.appendChild(genWebXmlDocument.createTextNode("\n\t\t"));
                        servletMappingElement.appendChild(urlPatternElement);
                        servletMappingElement.appendChild(genWebXmlDocument.createTextNode("\n\t"));
                        genWebXmlDocument.getDocumentElement().appendChild(servletMappingElement);
                    }

                    TransformerFactory tfactory = TransformerFactory.newInstance();
                    Transformer serializer = tfactory.newTransformer();
                    Properties oprops = new Properties();
                    oprops.put("method", "xml");
                    oprops.put("indent", "yes");
                    serializer.setOutputProperties(oprops);
                    Result result = new StreamResult(genWebXml);
                    serializer.transform(new DOMSource(genWebXmlDocument), result);
                }
                catch (TransformerConfigurationException e) {
                    genWebXml=null;
                    StringWriter stringWriter = new StringWriter();
        			e.printStackTrace(new PrintWriter(stringWriter));
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"_internalCompileApp","Exception caught during processing  2  "+e.getMessage()+"  "+ stringWriter.toString());
                }
                catch (TransformerException e) {
                    genWebXml=null;
                    StringWriter stringWriter = new StringWriter();
        			e.printStackTrace(new PrintWriter(stringWriter));
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"_internalCompileApp","Exception caught during processing  3   "+e.getMessage()+"  "+ stringWriter.toString());
                }
            }
            // defect 201384 end

        }
        catch (JspCoreException e) {
            this.returnCode = 1;
            StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
            if (logger!=null)
                logger.logp(Level.INFO,CLASS_NAME,"_internalCompileApp","Exception caught during processing  4   "+e.getMessage()+"  "+ stringWriter.toString());
            throw e;
        }
        catch (Throwable e){
            this.returnCode = 1;
            StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new PrintWriter(stringWriter));
            if (logger!=null)
                logger.logp(Level.INFO,CLASS_NAME,"_internalCompileApp","Exception caught during processing  5  "+e.getMessage()+"  "+ stringWriter.toString());
        }
        finally { // begin 238852.3
            // If a jarClassLoader has been created during this call, then invoke its dispose
            // method and discard it.  This addresses a memory leak from JSP compilation during
            // application deployment.
            if (jarClassloaderCreatedThisCall) {
                disposeOfClassloader(loader);
                loader = null;
            }
            // SDJ 03/07/2005
            ThreadContextHelper.setClassLoader(oldLoader);
            // SDJ 03/07/2005
        } // end 238852.3
        if (logger!=null) {
            if(doTraceCheck&&logger.isLoggable(Level.INFO)){
                logger.logp(Level.INFO, CLASS_NAME,"_internalCompileApp", "_internalCompileApp() returning returnCode: ["+this.returnCode+"]");
            }
        }
        return this.returnCode;

    }

    /**
     * @param webXmlExists
     * @param jspCfg
     * @return
     * @throws JspCoreException
     */
    public static JspConfigurationManager createJspConfigurationManager(boolean webXmlExists, JspXmlExtConfig jspCfg, String commandLineFileExts, JspFileUtils fileUtil) throws JspCoreException {
        // if JSP file extensions were given in the batchcompiler command, they override
        //    any extensions given in the JSP engine property jsp.file.extensions, which
        //    is returned by jspCfg.getJspFileExtensions().
        List jspFileExtensions = null;
        if (commandLineFileExts != null) {
            jspFileExtensions = new ArrayList();
            fileUtil.createJspFileExtensionList(commandLineFileExts, jspFileExtensions);
        }
        JspConfigurationManager jspConfigurationManager = null;
        boolean jcdiEnabled = false;
        if (jspCfg!=null) {
            jcdiEnabled=jspCfg.isJCDIEnabledForRuntimeCheck();
        }
        if (webXmlExists) {
            if (jspFileExtensions!=null && !jspFileExtensions.isEmpty()) {
                jspConfigurationManager = new JspConfigurationManager(jspCfg.getJspPropertyGroups(), jspCfg.isServlet24(), jspCfg.isServlet24_or_higher(), jspFileExtensions, jcdiEnabled);
            }
            else {
                jspConfigurationManager = new JspConfigurationManager(jspCfg.getJspPropertyGroups(), jspCfg.isServlet24(), jspCfg.isServlet24_or_higher(), jspCfg.getJspFileExtensions(), jcdiEnabled);
            }
        }
        else {
            jspConfigurationManager = new JspConfigurationManager(Collections.EMPTY_LIST, false, false, Collections.EMPTY_LIST, jcdiEnabled);
        }
        return jspConfigurationManager;
    }

    private void collectDirs(JspTranslationContext context, String path, List dirFiles) {
        collectDirs(context, path, dirFiles, this.recurse);
    }

    private void collectDirs(JspTranslationContext context, String path, List dirFiles, boolean recurseVar) {
        Set libSet = context.getResourcePaths(path);
        if (libSet != null) {
            Iterator it = libSet.iterator();
            JspDirectory files = null;
            while (it.hasNext()) {
                String resourcePath = (String) it.next();
                if (resourcePath.startsWith("/META-INF") == false) {
                    if (resourcePath.endsWith("/")) {
                        if (recurseVar) {
                            collectDirs(context, resourcePath, dirFiles);
                        }
                    }
                    else if (isJspFile(resourcePath)) {
                        if (files == null)
                            files = new JspDirectory(path);
                        files.add(resourcePath);
                    }
                }
            }
            if (files != null)
                dirFiles.add(files);
        }
    }

    public static List buildJspFileExtensionList (String[] standardExtensions, List additionalExtensions){
        List extFilter = new ArrayList();
        for(int i=0; i < standardExtensions.length; i++){
            extFilter.add(standardExtensions[i].substring(standardExtensions[i].lastIndexOf(".")+1));
        }
        Object[] additionalExt = additionalExtensions.toArray();
        for(int i=0; i < additionalExt.length; i++){
            String extMap = (String)additionalExt[i];
            extMap = extMap.substring(extMap.lastIndexOf(".")+1);
            if (!extFilter.contains(extMap)) {
                extFilter.add(extMap);
            }
        }
        return extFilter;
    }

    private boolean isJspFile(String resourcePath){
        String ext = resourcePath.substring(resourcePath.lastIndexOf(".") + 1);
        return extensionFilter.contains(ext);
    }


    private void translateDir(
        JspDirectory files,
        JspTranslationContext context,
        JspConfigurationManager jspConfigurationManager,
        JspOptions options,
        TagLibraryCache tlc,
        boolean singleFile)
        throws JspCoreException, IOException {

        List translatedJsps = new ArrayList();
        Map translationResultMap = new HashMap();

        int upToDateCount = 0;
        int translatedCount = 0;
        int translationFailureCount = 0;
        boolean translationSucceeded;

        if (logger!=null) {
            logger.logp(Level.INFO,CLASS_NAME,"translateDir","translateDir");
        }
        List dependentsList = new ArrayList();
        JspResources jspResources = null;
        boolean removeV4FirstTime=true;
        List<JspResources> upToDateJspsList = new ArrayList<JspResources>(); // for saving a list of JSPs to add to web.xml later 
        for (Iterator itr = files.iterator(); itr.hasNext();) {
            String resourcePath = (String) itr.next();
            JspInputSource inputSource = context.getJspInputSourceFactory().createJspInputSource(resourcePath);
            jspResources = context.getJspResourcesFactory().createJspResources(inputSource);

            // defect 196156 - begin
            // remove v4 .java and .dat files, if removeV4Files is true and a directory (not single file)
            //    is being operated on
            if (this.removeV4Files && removeV4FirstTime && !singleFile) {
                String outputDir = options.getOutputDir().toString();
                File directory =jspResources.getGeneratedSourceFile().getParentFile();
                fileUtil.removeVersion4Files(directory,outputDir);
                removeV4FirstTime=false;
            } // defect 196156 - end

            // if useFullPackageNames is true, identify JSP classfiles that are in the
            //  packages Constants.JSP_FIXED_PACKAGE_NAME and Constants.OLD_JSP_PACKAGE_NAME,
            //  and delete them and related files (.dat, .java)
            if (options.isUseFullPackageNames() && !singleFile) {
                String outputDir = options.getOutputDir().toString();
                fileUtil.removeFixedPackageFiles(outputDir, context, resourcePath);
            }

            if (forceCompilation && jspResources.getGeneratedSourceFile().getParentFile().exists() == false) {
                jspResources.getGeneratedSourceFile().getParentFile().mkdirs();
            }
            boolean translationRequired = (forceCompilation || jspResources.isOutdated());
            if (options.isTrackDependencies() && translationRequired == false) {
                translationRequired = checkForTranslation(context, options, tlc, dependentsList, resourcePath, jspResources);
            }
            //  begin 228118: JSP container should recompile if debug enabled and jsp was not compiled in debug.
            if (options.isDebugEnabled() && translationRequired == false){
                ClassLoader targetLoader = createClassLoader(jspResources.getClassName(), context, resourcePath, this.logger, options.isUseFullPackageNames(), options.getOutputDir(), jspResources.getGeneratedSourceFile());
                try{
                    JspClassInformation jci = (JspClassInformation)Class.forName(jspResources.getPackageName() + "." + jspResources.getClassName(),true,targetLoader).newInstance();
                    translationRequired = (jci.isDebugClassFile() == false);
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"translateDir","JSP " + resourcePath + " originally compiled with debug =[" + (!translationRequired) + "]");
                }catch (Throwable t){
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"translateDir","Exception caught during check if JSP is debuggable",t);
                }
            }
            // end 228118: JSP container should recompile if debug enabled and jsp was not compiled in debug.

            if (translationRequired) {
                translationSucceeded = true;
                try {
                    translate(context, jspConfigurationManager, options, tlc, translatedJsps, translationResultMap, resourcePath, jspResources);
                }
                catch (JspCoreException e) {
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"translateDir","Failed to translate " + resourcePath);
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    if (logger!=null)
                        logger.logp(Level.INFO,CLASS_NAME,"translateDir","Exception caught during processing file "+resourcePath+"  "+e.getMessage()+"  "+ stringWriter.toString()); //defect 203009
                    translationFailureCount++;
                    translationSucceeded = false;
                    this.returnCode = 1;
                    //PK50519
                    //Commenting this out so we continue and translate the other JSPs
                    //throw e; 
                }
                if (translationSucceeded) {
                    translatedCount++;
                }
            }
            else {
                // Populating a list of JSPs that are up to date to add them to the generated web.xml later
                upToDateJspsList.add(jspResources); 
                if (logger!=null)
                    logger.logp(Level.INFO,CLASS_NAME,"translateDir",resourcePath + " is up to date.");
                upToDateCount++;
            }
        }
        if (logger!=null)
            logger.logp(Level.INFO,CLASS_NAME,"translateDir",translatedCount + " JSPs were successfully translated.");
        if (logger!=null)
            logger.logp(Level.INFO,CLASS_NAME,"translateDir",translationFailureCount + " JSPs had translation errors.");
        if (logger!=null)
            logger.logp(Level.INFO,CLASS_NAME,"translateDir",upToDateCount + " JSPs were up to date.");

        if (translatedJsps.size() > 0) {
            File sourceFiles = null;
            int compileCount = 0;
            String javaFileName=null;
            // begin 199761: change temp file to be be deleted after compilation not when the JVM exits.
            try {
            	if (options.isUseJDKCompiler()) {
	                if (translatedJsps.size() > 1) {
	                    sourceFiles = File.createTempFile("jsp", "files");
	                    if (logger!=null){
	                      logger.logp(Level.FINER,CLASS_NAME,"translateDir", "Created temp file to store list of to be compiled JSPs [" +sourceFiles.getCanonicalFile() +"]");
	                    }
	                    compileCount = createFilesList(sourceFiles, translatedJsps, options);
	                }
	                else {
	                  javaFileName = createJavaFileName(translatedJsps);
	                  compileCount++;
	                }
            	}
                JspCompiler compiler = context.getJspCompilerFactory().createJspCompiler();
                Collection jspLineIds = new ArrayList();

                List dependecyList = new ArrayList();
                for (Iterator itr = translationResultMap.values().iterator(); itr.hasNext();) {
                    JspTranslationResult translationResult = (JspTranslationResult) itr.next();
                    dependecyList.addAll(translationResult.getTagFileDependencyList());
                    jspLineIds.addAll(translationResult.getJspLineIds());
                }
                if (compileJsps) {
                    JspResources[] resourcesToCompile = new JspResources[translatedJsps.size()];
                    resourcesToCompile = (JspResources[])translatedJsps.toArray(resourcesToCompile);
                    if (! options.isUseJDKCompiler()) {
                    	JDTcompile(files, options, translatedJsps.size(), resourcesToCompile, compiler, jspLineIds, dependecyList);
                    }
                    else {
                    	compile(files, options, compileCount, resourcesToCompile, javaFileName, sourceFiles, compiler, jspLineIds);
                    }
                }
                syncAndSmap(options, translatedJsps, translationResultMap);
            }
            finally {
                if (sourceFiles != null) {
                    boolean deleted = sourceFiles.delete();
                    if (logger!=null){
                        logger.logp(Level.FINER,CLASS_NAME,"translateDir", "Check if [" + sourceFiles.getCanonicalFile() + "] was deleted (" + deleted + ") exists? [" + sourceFiles.exists() + "]");
                    }
                    if (deleted == false && sourceFiles.exists()) { // check if delete worked
                        if (logger!=null){
                            logger.logp(Level.FINER,CLASS_NAME,"translateDir", "Deletion request failed. Marking [" + sourceFiles.getCanonicalFile() + "] for deletion when JVM exits.");
                        }
                        sourceFiles.deleteOnExit(); // delete request failed... try to delete when JVM terminates.
                    }
                }
            }
            // end 199761
         }
         
        // defect 201384 begin
        // collect servlet config information
        if (options.isUseFullPackageNames() && genWebXml !=null) {
            String servletClass;
            JspResources jspRes = null;
            // Add JSPs to web.xml
            List<JspResources> processedJspsJspResources = new ArrayList<JspResources>(translatedJsps);
            processedJspsJspResources.addAll(upToDateJspsList);
            
            for (Iterator itr = processedJspsJspResources.iterator(); itr.hasNext();) {
                jspRes = (JspResources) itr.next();
                servletClass=jspRes.getPackageName()+"."+jspRes.getClassName();
                genWebXmlDocument.getDocumentElement().appendChild(genWebXmlDocument.createTextNode("\n\t"));
                Element servletElement = genWebXmlDocument.createElement("servlet");
                Element servletNameElement = genWebXmlDocument.createElement("servlet-name");
                Element servletClassElement = genWebXmlDocument.createElement("servlet-class");
                servletNameElement.appendChild(genWebXmlDocument.createTextNode(servletClass));
                servletClassElement.appendChild(genWebXmlDocument.createTextNode(servletClass));
                servletElement.appendChild(genWebXmlDocument.createTextNode("\n\t\t"));
                servletElement.appendChild(servletNameElement);
                servletElement.appendChild(genWebXmlDocument.createTextNode("\n\t\t"));
                servletElement.appendChild(servletClassElement);
                servletElement.appendChild(genWebXmlDocument.createTextNode("\n\t"));
                genWebXmlDocument.getDocumentElement().appendChild(servletElement);
            }
            for (Iterator itr = processedJspsJspResources.iterator(); itr.hasNext();) {
                jspRes = (JspResources) itr.next();
                servletClass=jspRes.getPackageName()+"."+jspRes.getClassName();
                WebXmlMappings mapping=new WebXmlMappings(servletClass,jspRes.getInputSource().getRelativeURL());
                this.genWebXmlMappings.add(mapping);
            }
        }
        // defect 201384 end

    }

    private int createFilesList(File sourceFiles, List translatedJsps, JspOptions options) throws IOException {
        String javaFileName;
        int compileCount = 0;
        if (compileAfterFailure) {
            translatedJspsFileNames = new ArrayList(); //PK72039
        }
        //defect 204307
        FileOutputStream fos = new FileOutputStream(sourceFiles);

        //begin 235442: zOS does not handle UTF-8 encoding: defer to OS for encoding.
        //PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fos, "UTF-8")));
        PrintWriter pw = null;
        if (options.isZOS()){
            if (logger!=null){
                logger.logp(Level.FINER,CLASS_NAME,"createFilesList", "Platform is zOS; defer to zOS for encoding");
            }
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fos)));
        }
        else{
            // Assume characters are encoding in valid UTF-8 encoding. Defer handling of character encoding
            // for the javac compiler to the underlying OS' current locale.
            pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fos, "UTF-8")));
        }
        // end 235442: zOS does not handle UTF-8 encoding: defer to OS for encoding.

        for (Iterator itr = translatedJsps.iterator(); itr.hasNext();) {
            JspResources jspResources = (JspResources) itr.next();
            javaFileName = jspResources.getGeneratedSourceFile().getPath();
            if (javaFileName.indexOf('\\') != -1)
                javaFileName = javaFileName.replace('\\', '/');
            pw.println("\"" + javaFileName + "\"");
            if (compileAfterFailure) {
                translatedJspsFileNames.add("\"" + javaFileName + "\""); //PK72039
            }
            compileCount++;
        }
        pw.close();
        return compileCount;
    }

    //PK72039 start
    private int createNewFilesList(File sourceFiles, List compilerFailureFileNames, JspOptions options) {
        String javaFileName;
        int compileCount = 0;

        if (compilerFailureFileNames != null && compilerFailureFileNames.size() > 0) {
            int firstFailLoc = translatedJspsFileNames.indexOf((String)compilerFailureFileNames.get(0));
            
            if (logger!=null){
                logger.logp(Level.FINER,"JspModC","createNewFileList", "location of the first failing JSP: " + firstFailLoc);
            }
                
            if (firstFailLoc == -1) {
                return 0;
            }
                
            PrintWriter pw = null;
            try {
                //defect 204307
                FileOutputStream fos = new FileOutputStream(sourceFiles);
                    
                //begin 235442: zOS does not handle UTF-8 encoding: defer to OS for encoding.
                //PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fos, "UTF-8")));
                    
                if (options.isZOS()){
                    if (logger!=null){
                        logger.logp(Level.FINER,"JspModC","createNewFilesList", "Platform is zOS; defer to zOS for encoding");
                    }
                    pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fos)));
                }
                else{
                    // Assume characters are encoding in valid UTF-8 encoding. Defer handling of character encoding
                    // for the javac compiler to the underlying OS' current locale.
                    pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(fos, "UTF-8")));
                }
                // end 235442: zOS does not handle UTF-8 encoding: defer to OS for encoding.
                
                for(int i = (firstFailLoc+1); i < translatedJspsFileNames.size(); i++) {
                    if (compilerFailureFileNames.indexOf((String)translatedJspsFileNames.get(i)) == -1) {
                        if (logger!=null){
                            logger.logp(Level.FINER,"JspModC","createNewFilesList", "Adding the following file to the compile list: " + (String)translatedJspsFileNames.get(i));
                        }
                        pw.println((String)translatedJspsFileNames.get(i));
                        compileCount++;
                    }
                }

            } catch (IOException ioe) {
                if (logger!=null){
                    logger.logp(Level.FINER,"JspModC","createNewFileList", "Exception occurred while writing to a temporary file: " + ioe);
                }
                compileCount = 0;
            }
            finally {
                if (pw != null)
                    pw.close();
            }
        }
        return compileCount;
    }
    //PK72039 end

    private String createJavaFileName(List translatedJsps) {
        String javaFileName=null;
        JspResources jspResources = (JspResources) translatedJsps.get(0);
        javaFileName = jspResources.getGeneratedSourceFile().getPath();
        if (javaFileName.indexOf('\\') != -1)
            javaFileName = javaFileName.replace('\\', '/');
        return javaFileName;
    }

    private void translate(
        JspTranslationContext context,
        JspConfigurationManager jspConfigurationManager,
        JspOptions options,
        TagLibraryCache tlc,
        List translatedJsps,
        Map translationResultMap,
        String resourcePath,
        JspResources jspResources)
        throws JspCoreException {
        if (logger!=null) {
            logger.logp(Level.INFO,CLASS_NAME,"translate","Translating " + resourcePath);
        }

        JspTranslationResult result = JspTranslatorUtil.translateJsp(jspResources,
            context,
            jspConfigurationManager.getConfigurationForUrl(resourcePath),
            options,
            tlc,
            forceCompilation,
            this.compilerOptions);

        if (result.getTagFileCompileResult() != null) {
            if (result.getTagFileCompileResult().getCompilerReturnValue() != 0) {
                if (logger!=null)
                    logger.logp(Level.INFO,CLASS_NAME,"compile","TagFile Compilation errors were encountered!");
                this.returnCode = 1;
            }
            if (result.getTagFileCompileResult().getCompilerReturnValue() != 0 || ((options!=null && (options.isVerbose() || options.isDeprecation())) || (compilerOptions!=null && (compilerOptions.contains("-verbose") || compilerOptions.contains("-deprecation"))))) {
                if (logger!=null && result.getTagFileCompileResult().getCompilerMessage().length()>0){
                    logger.logp(Level.INFO,CLASS_NAME,"compile"," [" + result.getTagFileCompileResult().getCompilerMessage() + "]");
                }
            }
        }
        translatedJsps.add(jspResources);

        translationResultMap.put(jspResources.getInputSource().getAbsoluteURL().toExternalForm(), result);
    }

    private boolean checkForTranslation(
        JspTranslationContext context,
        JspOptions options,
        TagLibraryCache tlc,
        List dependentsList,
        String resourcePath,
        JspResources jspResources) {
        boolean translationRequired = false;
        ClassLoader targetLoader = createClassLoader(jspResources.getClassName(), context, resourcePath, this.logger, options.isUseFullPackageNames(), options.getOutputDir(), jspResources.getGeneratedSourceFile());
        if (targetLoader != null) {
            String[] dependents = getDependents(jspResources.getPackageName() + "." + jspResources.getClassName(), targetLoader);
            if (dependents != null) {
                loadDependentList(options, dependents, dependentsList, context);
                translationRequired = isDependentOutdated(dependentsList, tlc);
            }
            else {
                translationRequired = true;
            }
        }
        return translationRequired;
    }

    private int JDTcompile(JspDirectory files,
                        JspOptions options,
                        int compileCount,
                        JspResources[] compileFiles,
                        JspCompiler compiler,
                        Collection jspLineIds,
                        List dependecyList) {

        JspCompilerResult compileResult;
        int compileReturnValue=0;
        if (logger!=null)
            logger.logp(Level.INFO,CLASS_NAME,"compile","Compiling " + compileCount + " JSPs in " + files.getDirectory());
		JspResources[] dependencies = null;
		if (dependecyList.size() > 0) {
		    dependencies = new JspResources[dependecyList.size()];
		    dependencies = (JspResources[])dependecyList.toArray(dependencies);
		}
		
		// Remove class files for the sources that are going to be compiled.
		removeClassfiles(compileFiles);
		
		compileResult = compiler.compile(compileFiles, dependencies, jspLineIds, this.compilerOptions);
        if (logger!=null)
            logger.logp(Level.INFO,CLASS_NAME,"compile","Compile complete for " + files.getDirectory());
        if (compileResult.getCompilerReturnValue() != 0) {
            if (logger!=null)
                logger.logp(Level.INFO,CLASS_NAME,"compile","Compilation errors were encountered!");
            this.returnCode = 1;
        }
        if (compileResult.getCompilerReturnValue() != 0 || ((options!=null && (options.isVerbose() || options.isDeprecation())) || (compilerOptions!=null && (compilerOptions.contains("-verbose") || compilerOptions.contains("-deprecation"))))) {
            if (logger!=null && compileResult.getCompilerMessage().length()>0){
                logger.logp(Level.INFO,CLASS_NAME,"compile"," [" + compileResult.getCompilerMessage() + "]");
            }
        }
        compileReturnValue = compileResult.getCompilerReturnValue();
        return compileReturnValue;
    }

    private int compile(JspDirectory files, JspOptions options, int compileCount, JspResources[] compileFiles, String javaFileName, File sourceFiles, JspCompiler compiler, Collection jspLineIds) {
        JspCompilerResult compileResult;
        int compileReturnValue=0;
        if (logger!=null)
            logger.logp(Level.INFO,CLASS_NAME,"compile","Compiling " + compileCount + " JSPs in " + files.getDirectory());
		
		// Remove class files for the sources that are going to be compiled.
		removeClassfiles(compileFiles);
		
        if (compileCount>1) {
            compileResult = compiler.compile("@" + sourceFiles.getPath(), jspLineIds, this.compilerOptions);
        }
        else {
            compileResult = compiler.compile(javaFileName, jspLineIds, this.compilerOptions);
        }

        //PK72039 start
        if (compileAfterFailure) {
            if (compileResult.getCompilerReturnValue() != 0 && compileCount > 1) {
                //add check for jsp attribute here
                    
                if (logger!=null)
                    logger.logp(Level.INFO,"JspModC","compile","Compilation errors were encountered, attempting to compile the rest of the files in the directory.");
                        
                int newCompileCount = 0;
                        
                List failedFiles = compileResult.getCompilerFailureFileNames();

                if (failedFiles != null) {
                    newCompileCount = createNewFilesList(sourceFiles, failedFiles, options);
                }

                if (newCompileCount < compileCount) {
                    compiler.compile("@" + sourceFiles.getPath(), jspLineIds, this.compilerOptions);
                }
            }
        }
        //PK72039 end

        if (logger!=null)
            logger.logp(Level.INFO,CLASS_NAME,"compile","Compile complete for " + files.getDirectory());
        if (compileResult.getCompilerReturnValue() != 0) {
            if (logger!=null)
                logger.logp(Level.INFO,CLASS_NAME,"compile","Compilation errors were encountered!");
            this.returnCode = 1;
        }
        if (compileResult.getCompilerReturnValue() != 0 || ((options!=null && (options.isVerbose() || options.isDeprecation())) || (compilerOptions!=null && (compilerOptions.contains("-verbose") || compilerOptions.contains("-deprecation"))))) {
            if (logger!=null && compileResult.getCompilerMessage().length()>0){
                logger.logp(Level.INFO,CLASS_NAME,"compile"," [" + compileResult.getCompilerMessage() + "]");
            }
        }
        compileReturnValue = compileResult.getCompilerReturnValue();
        return compileReturnValue;
    }

    private void removeClassfiles(JspResources[] compileFiles) {
        String classfileName=null;
        if (compileFiles!=null &&compileFiles.length > 0) {
            // log files we will compile
            if (logger!=null) {
                logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","Files to compile: ");
                for (int i=0;i<compileFiles.length;i++) {
                    logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","File "+ (i+1) + " ["+compileFiles[i].getGeneratedSourceFile()+"]");
                }
            }
            // delete classfiles we will produce
            for (int i=0;i<compileFiles.length;i++) {
                classfileName=compileFiles[i].getGeneratedSourceFile().toString();
                int end=classfileName.lastIndexOf(".");
                classfileName=classfileName.substring(0,end);
                classfileName+=".class";
                if (logger!=null) logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","removing classfile "+ (i+1) + " ["+classfileName+"]");
                File clFile=new File(classfileName);
                if (clFile.exists() && clFile.isFile()) {
                    boolean retval=clFile.delete();
                    if (logger!=null) {
                        logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","Removed file "+compileFiles[i].getClassName()+"? : ["+retval+"]");
                    }
                }
                else {
                    if (logger!=null) {
                        logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","File ["+clFile.toString()+"] does not exist.");
                    }
                }
                // delete inner classes for this class
                end=classfileName.lastIndexOf(File.separatorChar);
                String directoryName=classfileName.substring(0,end);
                File directory = new File(directoryName);
                File[] icList = directory.listFiles(new InnerclassFilenameFilter(compileFiles[i].getClassName()));
                for (int j=0;j<icList.length;j++) {
                    if (logger!=null) {
                        logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","removing innerclassfile "+ (j+1) + " ["+icList[j]+"]");
                    }
                    if (icList[j].exists() && icList[j].isFile()) {
                        boolean retval=icList[j].delete();
                        end=icList[j].toString().lastIndexOf(File.separatorChar);
                        String innerClassName=icList[j].toString().substring(end+1);
                        if (logger!=null) {
                            logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","Removed innerclassfile "+innerClassName+"? : ["+retval+"]");
                        }
                    }
                    else {
                        if (logger!=null) {
                            logger.logp(Level.FINE,CLASS_NAME,"removeClassfiles","Innerclassfile ["+icList[j].toString()+"] does not exist.");
                        }
                    }
                }

            }
        }
    }

    private void syncAndSmap(JspOptions options, List translatedJsps, Map translationResultMap) throws JspCoreException {
        //if (!compileJsps || compileReturnValue == 0) {
            for (Iterator itr = translatedJsps.iterator(); itr.hasNext();) {
                JspResources jspResources = (JspResources) itr.next();
                if (jspResources instanceof TagFileResources == false) {
                    JspTranslationResult translationResult = (JspTranslationResult)translationResultMap.get(jspResources.getInputSource().getAbsoluteURL().toExternalForm());
                    if (translationResult.hasSmap()) {
                        SmapGenerator smapGenerator = translationResult.getSmapGenerator(jspResources.getInputSource().getAbsoluteURL().toExternalForm());
                        JspTranslatorUtil.installSmap(jspResources, smapGenerator);
                    }
                    if (logger!=null) {
                        logger.logp(Level.FINE,CLASS_NAME,"syncAndSmap","Synching ["+jspResources.getClassName()+"]");
                    }
                    jspResources.sync();
                    //JspTranslatorUtil.syncTagFileFiles(options, translationResult);
                }
            }
        //}
    }

    protected void parseCmdLine(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                if ("-appDir".equals(args[i])) {
                    _appDir = args[++i];
                }
                else if ("-tmpDir".equals(args[i])) {
                    _tmpDir = args[++i];
                }
                else if ("-forceCompilation".equals(args[i])) {
                    forceCompilation = Boolean.valueOf(args[++i]).booleanValue();
                }
                else if ("-additionalClasspath".equals(args[i])) {
                    classpath = args[++i];
                }
                else if ("-jspFile".equals(args[i])) {
                    jspFile = args[++i];
                }
                else if ("-dir".equals(args[i])) {
                    dir = args[++i];
                }
                else if ("-createDebugClassfiles".equals(args[i])) {
                    createDebugClassfiles = Boolean.valueOf(args[++i]).booleanValue();
                }
                else {
                    if (this.returnCode == 0)
                        usage();
                }
            }
            if (this.returnCode > 0) {
                return;
            }
            if ((_appDir == null) || (_tmpDir == null)) {
                usage();
            }
        }
        catch (Exception e) {
            if (this.returnCode == 0)
                usage();
        }
    }

    private void usage() {
        System.out.println(JspCoreException.getMsg("jsp.jspmodc.usage"));
        this.returnCode = 1;
    }

    protected static void getWebAppURLs(String dir, List urlList) {
        urlList.add(new File(dir + File.separator + "WEB-INF" + File.separator + "classes").toString());
        File webappDir = new File(dir + File.separator + "WEB-INF" + File.separator + "lib");

        if (webappDir.exists() && webappDir.isDirectory()) {
            File[] dirList = webappDir.listFiles();
            for (int i = 0; i < dirList.length; i++) {
                if (dirList[i].isFile() && (dirList[i].getName().endsWith(".jar") || dirList[i].getName().endsWith(".zip"))) {
                    urlList.add(dirList[i].toString());
                }
            }
        }
        urlList.add(new File(dir).toString());
    }

    protected static void getServerURLs(List urlList) {
        String installRoot = (String) java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction()
                {public Object run() {return System.getProperty("was.install.root");}});
        if (installRoot!=null) {
            File root = new File(installRoot + File.separator + "plugins");

            if (root.exists() && root.isDirectory()) {
                File[] dirList = root.listFiles();
                for (int i = 0; i < dirList.length; i++) {
                    if (dirList[i].isFile() && (dirList[i].getName().endsWith(".jar") || dirList[i].getName().endsWith(".zip"))) {
                        urlList.add(dirList[i].toString());
                    }
                }
            }
            urlList.add(root.toString());
        }
    }

    protected static void getLibURLs(String classpath, List urlList) {
        StringTokenizer st = new StringTokenizer(classpath, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            File file = new File(s);

            if (file.exists() && file.isDirectory()) {
                if(urlList.contains( (file.toString())) == false){  // defect 241038: do not add duplicates
                    urlList.add(file.toString());
                }
                File[] dirList = file.listFiles();
                if(dirList != null){    //241038: add check for null (if no files exist in this directory).
                    for (int i = 0; i < dirList.length; i++) {
                        if (dirList[i].isFile() && (dirList[i].getName().endsWith(".jar") || dirList[i].getName().endsWith(".zip"))) {
                            if(urlList.contains( (dirList[i].toString())) == false){    // defect 218270: do not add duplicates
                                urlList.add(dirList[i].toString());
                            }
                        }
                    }
                }
            }
            else if (s.endsWith(".jar") || s.endsWith(".zip")) {
                if(urlList.contains( (file.toString())) == false){  // defect 218270: do not add duplicates
                    urlList.add(file.toString());
                }
            }
        }
    }

    protected static void getManifestURLs(String path, List manifestPaths) {
        File f = new File(path);
        if (f.exists()) {
            if (f.isDirectory()) {
                File manifestFile = new File(path + File.separator + "META-INF" + File.separator + "MANIFEST.MF");
                if (manifestFile.exists()) {
                    FileInputStream fin = null;
                    try {
                        fin = new FileInputStream(manifestFile);
                        Manifest manifest = new Manifest(fin);
                        getManifestClassPaths(manifest, f.getParent(), manifestPaths);
                    }
                    catch (IOException e) {
                        //ignore IOExceptions
                    }
                    finally {
                        if (fin != null) {
                            try {
                                fin.close(); //make attempt to close. Manifest was locked at deletion time.
                            }
                            catch (IOException io) {}
                            fin = null;
                        }
                    }
                }
            }
            else {
                JarFile jarFile = null;
                try {
                    jarFile = new JarFile(f);
                    Manifest manifest = jarFile.getManifest();
                    if (manifest != null)
                        getManifestClassPaths(manifest, f.getParent(), manifestPaths);
                }
                catch (IOException e) {}
                finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close(); // attempt to close jar file
                        }
                        catch (IOException io) {}
                        jarFile = null;
                    }
                }
            }
        }
    }

    protected static void getManifestClassPaths(Manifest manifest, String archivePath, List manifestPaths) {
        Attributes main = manifest.getMainAttributes();
        String classPath = main.getValue(Attributes.Name.CLASS_PATH);
        if (classPath != null) {
            StringTokenizer st = new StringTokenizer(classPath, " ");
            while (st.hasMoreTokens()) {
                String path = archivePath + File.separator + st.nextToken();
                File file = new File(path);
                if (file.exists()) {
                    if(manifestPaths.contains( (file.toString())) == false){    // defect 218270: do not add duplicates
                        manifestPaths.add(file.toString());
                    }
                }
            }
        }
    }

    public static ClassLoader createClassLoader(String classname, JspTranslationContext context, String jspUri, Logger logger, boolean useFullPackageNames, File outputDir, File generatedFile) {
        URL[] urls = null;
        CodeSource codeSource = null;
        PermissionCollection permissionCollection = null;
        try {
            if (useFullPackageNames==false) {
                urls = new URL[4];
                //urls[0] = jspResources.getGeneratedSourceFile().getParentFile().toURL();
                urls[0] = generatedFile.getParentFile().toURL();
                urls[1] = outputDir.toURL();
                urls[2] = new File(context.getRealPath("/WEB-INF/classes") + jspUri.substring(0,jspUri.lastIndexOf("/")+1)).toURL();
                urls[3] = new File(context.getRealPath("/WEB-INF/classes")).toURL();
            }
            else {
                urls = new URL[2];
                urls[0] = outputDir.toURL();
                urls[1] = new File(context.getRealPath("/WEB-INF/classes")).toURL();
            }

            ClassLoader targetLoader = new JSPExtensionClassLoader(urls,
                                                                   context.getJspClassloaderContext(),
                                                                   classname,
                                                                   codeSource,
                                                                   permissionCollection);
            return targetLoader;
        }
        catch (MalformedURLException e) {
            if (logger!=null)
                logger.logp(Level.WARNING,CLASS_NAME,"createClassLoader", "failed to create JSP class loader for dependency tracking", e);
        }
        return null;
    }

    private String[] getDependents(String className, ClassLoader targetLoader)
    {
        String[] dependents=null;
        try
        {
            JspClassInformation claz = (JspClassInformation)Class.forName(className,true,targetLoader).newInstance();
            dependents = claz.getDependants();
        }
        catch (Throwable e)
        {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            if (logger!=null)
                logger.logp(Level.INFO,CLASS_NAME,"getDependents","Exception caught during processing dependencies  "+e.getMessage()+"  "+ stringWriter.toString());
        }
        return dependents;
    }

    private boolean isDependentOutdated(List dependentsList, TagLibraryCache tlc) {
        boolean outdated = false;
        for (Iterator itr = dependentsList.iterator(); itr.hasNext();) {
            JspDependent jspDependent = (JspDependent)itr.next();

            if (jspDependent.isOutdated()) {
                String dependentPath=jspDependent.getDependentFilePath();
                if (dependentPath!=null) {
                    if (dependentPath.endsWith(".tld")) {
                        try {
                            tlc.reloadTld(dependentPath, jspDependent.getTimestamp());
                        }
                        catch (JspCoreException e) {
                            StringWriter stringWriter = new StringWriter();
                            e.printStackTrace(new PrintWriter(stringWriter));
                            if (logger!=null)
                                logger.logp(Level.INFO,CLASS_NAME,"isDependentOutdated","Exception caught during processing dependencies "+e.getMessage()+"  "+ stringWriter.toString());
                            break;
                        }
                    }
                }
                outdated = true;
                break;
            }
        }
        return outdated;
    }

    private void loadDependentList(JspOptions options, String[] dependents, List dependentsList, JspTranslationContext context) {
        if (options.isTrackDependencies()) {
            synchronized (this) {
                dependentsList.clear();
                if (dependents != null) {
                    for (int i = 0; i < dependents.length; i++) {
                        JspDependent jspDependent = new JspDependent(dependents[i], context);
                        dependentsList.add(jspDependent);
                    }
                }
            }
        }
    }

    public static boolean checkForWebxml(String contextDir) {
        String context=contextDir;
        context = context.replace('\\', '/');
        if (context.endsWith("/")) {
            context=context.substring(0, context.length()-1);
        }
        File webXml=new File(context+WEB_XML);
        return (webXml.exists());
    }

    private class JspDirectory extends ArrayList {
        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = 3546359522244768563L;
        private String jspDirectory = null;

        public JspDirectory(String jspDirectory) {
            this.jspDirectory = jspDirectory;
        }

        public String getDirectory() {
            return jspDirectory;
        }
    }

    // defect 201384 begin
    private class WebXmlMappings extends ArrayList {
        /**
         * Comment for <code>serialVersionUID</code>
         */
        private static final long serialVersionUID = 3616446821543523896L;
        private String servletName = null;
        private String servletMapping = null;

        public WebXmlMappings(String servletName, String servletMapping) {
            this.servletName = servletName;
            this.servletMapping = servletMapping;
        }

        public String getServletName() {
            return servletName;
        }

        public String getServletMapping() {
            return servletMapping;
        }
    }
    // defect 201384 end

    public boolean isForceCompilation() {
        return forceCompilation;
    }

    public void setForceCompilation(boolean forceCompilation) {
        this.forceCompilation = forceCompilation;
    }

    public void setClasspath(String classpath) {
        this.classpath = classpath;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setClassloader(ClassLoader loader) {
        this.loader = loader;
    }

    //reg 2007-12-18 I don't think this code every gets called
    public void setOptions(JspToolsOptionsMap options) {
        if (logger!=null) {
            if(com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled()&&logger.isLoggable(Level.FINEST)){
                logger.logp(Level.FINEST, CLASS_NAME,"setOptions", "setOptions is called");
            }
        }
        this.optionOverrides = options;
    }

    public void setJspFile(String jspFile) {
        this.jspFile = jspFile;
    }

    public void setKeepGeneratedclassfiles(boolean keepGeneratedclassfiles) {
        this.keepGeneratedclassfiles = keepGeneratedclassfiles;
    }

    public void setCreateDebugClassfiles(boolean createDebugClassfiles) {
        this.createDebugClassfiles = createDebugClassfiles;
    }

    public void setLooseLibMap(Map map) {
        looseLibMap = map;
    }

    public void setTaglibMap(Map map) {
        taglibMap = map;
    }

    public void setLogger(Logger loggerIn) {
        logger=loggerIn;
    }
    // Defect 202493
    public Logger getLogger() {
        return this.logger;
    }
    public void setCompilerOptions(List compilerOptions) {
        this.compilerOptions = compilerOptions;
    }
    public void setWebModuleConfigDir(String dir){
        this._configDir = dir;
    }
    public void setReturnCode(int retcode){
        this.returnCode=retcode;
    }
    public int getReturnCode(){
        return this.returnCode;
    }
    public void setRecurse(boolean recurse){
        this.recurse=recurse;
    }
    public void setCompileJsps(boolean compileJsps) {
        this.compileJsps = compileJsps;
    }

    /*public void setTranslateJsps(boolean translateJsps) {
        this.translateJsps = translateJsps;
    }*/

    // defect 196156 - begin
    /**
     * @return
     */
    public boolean isRemoveV4Files() {
        return removeV4Files;
    }

    public void setRemoveV4Files(boolean b) {
        removeV4Files = b;
    }
    // defect 196156 - end

    // defect 201520 - begin
    public boolean isSearchClasspathForResources() {
        if (logger!=null)
            logger.logp(Level.INFO, CLASS_NAME,"isSearchClasspathForResources", "searchClasspathForResources: {"+searchClasspathForResources+"]");
        return searchClasspathForResources;
    }

    public void setSearchClasspathForResources(boolean searchClasspathForResources) {
        if (logger!=null)
            logger.logp(Level.INFO, CLASS_NAME,"setSearchClasspathForResources", "searchClasspathForResources: {"+searchClasspathForResources+"]");
        this.searchClasspathForResources = searchClasspathForResources;
    }
    // defect 201520 - end

    //begin 241038: add new API for setting classpath
    public void setAppClasspath (String classpath){
        this._appClasspath = classpath;
    }
    //end 241038: add new API for setting classpath

    public void setSharedLibraryJarList(String[] sharedLibraryJarList) {
        this.sharedLibraryJarList = sharedLibraryJarList;
    }

    public void setGlobalTagLibConfigs(GlobalTagLibConfig[] tagLibConfigArrayArg) {
        tagLibConfigArray=tagLibConfigArrayArg;
    }
    //defect 400645
    public void setWebContainerProperties(Properties webConProperties) {
    	this.webConProperties=webConProperties;
    }
    
    public Properties getWebContainerProperties() {
    	return webConProperties;
    }
    //defect 400645

    //PK72039 start
    public void setCompileAfterFailure (boolean compileAF){
        this.compileAfterFailure = compileAF;
    }
    //PK72039 end
    
    //Feature F003834 start
    public void setExtendedDocumentRoot(String edr) {
        this.extDocumentRoot = edr;
    }
    //Feature F003834 end

    protected abstract JspClassloaderContext createJspClassloaderContext(ClassLoader loaderArg, JspXmlExtConfig webAppConfig);

    protected abstract GlobalTagLibConfig[] loadTagLibraryCache();

    protected abstract JspXmlExtConfig createConfig(String contextDir);

    protected abstract ClassLoader addAdditionalClasspathToClassloader(ClassLoader cl);         //PM06503

    protected abstract ClassLoader createClassloader();

    protected abstract void disposeOfClassloader(ClassLoader loaderArg);
}
