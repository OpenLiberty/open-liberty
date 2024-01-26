/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.tools.ws.wscompile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.xml.bind.JAXBPermission;
import javax.xml.stream.XMLStreamException;
import javax.xml.ws.EndpointContext;

import org.xml.sax.EntityResolver;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.codemodel.CodeWriter;
import com.sun.codemodel.writer.ProgressCodeWriter;
import com.sun.istack.tools.DefaultAuthenticator;
import com.sun.istack.tools.ParallelWorldClassLoader;
import com.sun.tools.ws.ToolVersion;
import com.sun.tools.ws.api.TJavaGeneratorExtension;
import com.sun.tools.ws.processor.generator.CustomExceptionGenerator;
import com.sun.tools.ws.processor.generator.GeneratorBase;
import com.sun.tools.ws.processor.generator.JwsImplGenerator;
import com.sun.tools.ws.processor.generator.SeiGenerator;
import com.sun.tools.ws.processor.generator.ServiceGenerator;
import com.sun.tools.ws.processor.model.Model;
import com.sun.tools.ws.processor.modeler.wsdl.ConsoleErrorReporter;
import com.sun.tools.ws.processor.modeler.wsdl.WSDLModeler;
import com.sun.tools.ws.processor.util.DirectoryUtil;
import com.sun.tools.ws.resources.WscompileMessages;
import com.sun.tools.ws.resources.WsdlMessages;
import com.sun.tools.ws.util.WSDLFetcher;
import com.sun.tools.ws.wsdl.parser.MetadataFinder;
import com.sun.tools.ws.wsdl.parser.WSDLInternalizationLogic;
import com.sun.tools.xjc.util.NullStream;
import com.sun.xml.ws.api.server.Container;
import com.sun.xml.ws.util.ServiceFinder;

/**
 * @author Vivek Pandey
 */
public class WsimportTool {
    private static final String WSIMPORT = "wsimport";
    private final PrintStream out;
    private final Container container;

    /**
     * Wsimport specific options
     */
    protected WsimportOptions options = new WsimportOptions();

    public WsimportTool(OutputStream out) {
        this(out, null);
    }

    public WsimportTool(OutputStream logStream, Container container) {
        this.out = (logStream instanceof PrintStream) ? (PrintStream) logStream : new PrintStream(logStream);
        this.container = container;
    }

    protected class Listener extends WsimportListener {
        ConsoleErrorReporter cer = new ConsoleErrorReporter(out == null ? new PrintStream(new NullStream()) : out);

        @Override
        public void generatedFile(String fileName) {
            message(fileName);
        }

        @Override
        public void message(String msg) {
            out.println(msg);
        }

        @Override
        public void error(SAXParseException exception) {
            cer.error(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) {
            cer.fatalError(exception);
        }

        @Override
        public void warning(SAXParseException exception) {
            cer.warning(exception);
        }

        @Override
        public void debug(SAXParseException exception) {
            cer.debug(exception);
        }

        @Override
        public void info(SAXParseException exception) {
            cer.info(exception);
        }

        public void enableDebugging() {
            cer.enableDebugging();
        }
    }

    protected class Receiver extends ErrorReceiverFilter {

        private final Listener listener;

        public Receiver(Listener listener) {
            super(listener);
            this.listener = listener;
        }

        @Override
        public void info(SAXParseException exception) {
            if (options.verbose)
                super.info(exception);
        }

        @Override
        public void warning(SAXParseException exception) {
            if (!options.quiet)
                super.warning(exception);
        }

        @Override
        public void pollAbort() throws AbortException {
            if (listener.isCanceled())
                throw new AbortException();
        }

        @Override
        public void debug(SAXParseException exception) {
            if (options.debugMode) {
                listener.debug(exception);
            }
        }
    }

    public boolean run(String[] args) {
        Listener listener = new Listener();
        Receiver receiver = new Receiver(listener);
        return run(args, listener, receiver);
    }

    protected boolean run(String[] args, Listener listener,
                          Receiver receiver) {
        for (String arg : args) {
            if (arg.equals("-version")) {
                listener.message(
                                 WscompileMessages.WSIMPORT_VERSION(ToolVersion.VERSION.MAJOR_VERSION));
                return true;
            }
            if (arg.equals("-fullversion")) {
                listener.message(
                                 WscompileMessages.WSIMPORT_FULLVERSION(ToolVersion.VERSION.toString()));
                return true;
            }
        }

        try {
            parseArguments(args, listener, receiver);

            try {
                Model wsdlModel = buildWsdlModel(listener, receiver);
                if (wsdlModel == null)
                    return false;

                if (!generateCode(listener, receiver, wsdlModel, true))
                    return false;

                /*
                 * Not so fast!
                 * } catch(AbortException e){
                 * //error might have been reported
                 *
                 */
            } catch (IOException e) {
                receiver.error(e);
                return false;
            } catch (XMLStreamException e) {
                receiver.error(e);
                return false;
            }
            if (!options.nocompile) {
                if (!compileGeneratedClasses(receiver, listener)) {
                    listener.message(WscompileMessages.WSCOMPILE_COMPILATION_FAILED());
                    return false;
                }
            }
            try {
                if (options.clientjar != null) {
                    //add all the generated class files to the list of generated files
                    addClassesToGeneratedFiles();
                    jarArtifacts(listener);

                }
            } catch (IOException e) {
                receiver.error(e);
                return false;
            }

        } catch (Options.WeAreDone done) {
            usage(done.getOptions());
        } catch (BadCommandLineException e) {
            if (e.getMessage() != null) {
                System.out.println(e.getMessage());
                System.out.println();
            }
            usage(e.getOptions());
            return false;
        } finally {
            deleteGeneratedFiles();
            if (!options.disableAuthenticator) {
                DefaultAuthenticator.reset();
            }
        }
        if (receiver.hadError()) {
            return false;
        }
        return true;
    }

    private void deleteGeneratedFiles() {
        Set<File> trackedRootPackages = new HashSet<File>();

        if (options.clientjar != null) {
            //remove all non-java artifacts as they will packaged in jar.
            Iterable<File> generatedFiles = options.getGeneratedFiles();
            synchronized (generatedFiles) {
                for (File file : generatedFiles) {
                    if (!file.getName().endsWith(".java")) {
                        boolean deleted = file.delete();
                        if (options.verbose && !deleted) {
                            System.out.println(MessageFormat.format("{0} could not be deleted.", file));
                        }
                        trackedRootPackages.add(file.getParentFile());
                    }
                }
            }
            //remove empty package dirs
            for (File pkg : trackedRootPackages) {

                while (pkg.list() != null && pkg.list().length == 0 && !pkg.equals(options.destDir)) {
                    File parentPkg = pkg.getParentFile();
                    boolean deleted = pkg.delete();
                    if (options.verbose && !deleted) {
                        System.out.println(MessageFormat.format("{0} could not be deleted.", pkg));
                    }
                    pkg = parentPkg;
                }
            }
        }
        if (!options.keep) {
            options.removeGeneratedFiles();
        }
    }

    private void addClassesToGeneratedFiles() throws IOException {
        Iterable<File> generatedFiles = options.getGeneratedFiles();
        final List<File> trackedClassFiles = new ArrayList<File>();
        for (File f : generatedFiles) {
            if (f.getName().endsWith(".java")) {
                String relativeDir = DirectoryUtil.getRelativePathfromCommonBase(f.getParentFile(), options.sourceDir);
                final String className = f.getName().substring(0, f.getName().indexOf(".java"));
                File classDir = new File(options.destDir, relativeDir);
                if (classDir.exists()) {
                    classDir.listFiles(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            if (name.equals(className + ".class") || (name.startsWith(className + "$") && name.endsWith(".class"))) {
                                trackedClassFiles.add(new File(dir, name));
                                return true;
                            }
                            return false;
                        }
                    });
                }
            }
        }
        for (File f : trackedClassFiles) {
            options.addGeneratedFile(f);
        }
    }

    private void jarArtifacts(WsimportListener listener) throws IOException {
        File zipFile = new File(options.clientjar);
        if (!zipFile.isAbsolute()) {
            zipFile = new File(options.destDir, options.clientjar);
        }

        FileOutputStream fos;
        if (!options.quiet) {
            listener.message(WscompileMessages.WSIMPORT_ARCHIVING_ARTIFACTS(zipFile));
        }

        BufferedInputStream bis = null;
        FileInputStream fi = null;
        fos = new FileOutputStream(zipFile);
        JarOutputStream jos = new JarOutputStream(fos);
        try {
            String base = options.destDir.getCanonicalPath();
            for (File f : options.getGeneratedFiles()) {
                //exclude packaging the java files in the jar
                if (f.getName().endsWith(".java")) {
                    continue;
                }
                if (options.verbose) {
                    listener.message(WscompileMessages.WSIMPORT_ARCHIVE_ARTIFACT(f, options.clientjar));
                }
                String entry = f.getCanonicalPath().substring(base.length() + 1).replace(File.separatorChar, '/');
                fi = new FileInputStream(f);
                bis = new BufferedInputStream(fi);
                JarEntry jarEntry = new JarEntry(entry);
                jos.putNextEntry(jarEntry);
                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = bis.read(buffer)) != -1) {
                    jos.write(buffer, 0, bytesRead);
                }
            }
        } finally {
            try {
                if (bis != null) {
                    bis.close();
                }
            } finally {
                if (jos != null) {
                    jos.close();
                }
                if (fi != null) {
                    fi.close();
                }
            }
        }
    }

    protected void parseArguments(String[] args, Listener listener,
                                  Receiver receiver) throws BadCommandLineException {
        options.parseArguments(args);
        options.validate();
        if (options.debugMode)
            listener.enableDebugging();
        options.parseBindings(receiver);
    }

    protected Model buildWsdlModel(Listener listener, final Receiver receiver) throws BadCommandLineException, XMLStreamException, IOException {
        //set auth info
        //if(options.authFile != null)
        if (!options.disableAuthenticator) {
            class AuthListener implements DefaultAuthenticator.Receiver {

                private final boolean isFatal;

                AuthListener(boolean isFatal) {
                    this.isFatal = isFatal;
                }

                @Override
                public void onParsingError(String text, Locator loc) {
                    error(new SAXParseException(WscompileMessages.WSIMPORT_ILLEGAL_AUTH_INFO(text), loc));
                }

                @Override
                public void onError(Exception e, Locator loc) {
                    if (e instanceof FileNotFoundException) {
                        error(new SAXParseException(WscompileMessages.WSIMPORT_AUTH_FILE_NOT_FOUND(
                                                                                                   loc.getSystemId(), WsimportOptions.defaultAuthfile), null));
                    } else {
                        error(new SAXParseException(WscompileMessages.WSIMPORT_FAILED_TO_PARSE(loc.getSystemId(), e.getMessage()), loc));
                    }
                }

                private void error(SAXParseException e) {
                    if (isFatal) {
                        receiver.error(e);
                    } else {
                        receiver.debug(e);
                    }
                }
            }

            DefaultAuthenticator da = DefaultAuthenticator.getAuthenticator();
            if (options.proxyAuth != null) {
                da.setProxyAuth(options.proxyAuth);
            }
            if (options.authFile != null) {
                da.setAuth(options.authFile, new AuthListener(true));
            } else {
                da.setAuth(new File(WsimportOptions.defaultAuthfile), new AuthListener(false));
            }
        }

        if (!options.quiet) {
            listener.message(WscompileMessages.WSIMPORT_PARSING_WSDL());
        }

        MetadataFinder forest = new MetadataFinder(new WSDLInternalizationLogic(), options, receiver);
        forest.parseWSDL();
        if (forest.isMexMetadata)
            receiver.reset();

        WSDLModeler wsdlModeler = new WSDLModeler(options, receiver, forest);
        Model wsdlModel = wsdlModeler.buildModel();
        if (wsdlModel == null) {
            listener.message(WsdlMessages.PARSING_PARSE_FAILED());
        }

        if (options.clientjar != null) {
            if (!options.quiet)
                listener.message(WscompileMessages.WSIMPORT_FETCHING_METADATA());
            options.wsdlLocation = new WSDLFetcher(options, listener).fetchWsdls(forest);
        }

        return wsdlModel;
    }

    protected boolean generateCode(Listener listener, Receiver receiver,
                                   Model wsdlModel, boolean generateService) throws IOException {
        //generated code
        if (!options.quiet)
            listener.message(WscompileMessages.WSIMPORT_GENERATING_CODE());

        TJavaGeneratorExtension[] genExtn = ServiceFinder.find(TJavaGeneratorExtension.class).toArray();
        CustomExceptionGenerator.generate(wsdlModel, options, receiver);
        SeiGenerator.generate(wsdlModel, options, receiver, genExtn);
        if (receiver.hadError()) {
            throw new AbortException();
        }
        if (generateService) {
            ServiceGenerator.generate(wsdlModel, options, receiver);
        }
        for (GeneratorBase g : ServiceFinder.find(GeneratorBase.class)) {
            g.init(wsdlModel, options, receiver);
            g.doGeneration();
        }

        List<String> implFiles = null;
        if (options.isGenerateJWS) {
            implFiles = JwsImplGenerator.generate(wsdlModel, options, receiver);
        }

        for (Plugin plugin : options.activePlugins) {
            try {
                plugin.run(wsdlModel, options, receiver);
            } catch (SAXException sex) {
                // fatal error. error should have been reported
                return false;
            }
        }

        CodeWriter cw;
        if (options.filer != null) {
            cw = new FilerCodeWriter(options.sourceDir, options);
        } else {
            cw = new WSCodeWriter(options.sourceDir, options);
        }

        if (options.verbose)
            cw = new ProgressCodeWriter(cw, out);
        options.getCodeModel().build(cw);

        if (options.isGenerateJWS) {
            //move Impl files to implDestDir
            return JwsImplGenerator.moveToImplDestDir(implFiles, options, receiver);
        }

        return true;
    }

    public void setEntityResolver(EntityResolver resolver) {
        this.options.entityResolver = resolver;
    }

    /*
     * To take care of JDK6-JDK6u3, where 2.1 API classes are not there
     */
    private static boolean useBootClasspath(Class clazz) {
        try {
            ParallelWorldClassLoader.toJarUrl(clazz.getResource('/' + clazz.getName().replace('.', '/') + ".class"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean compileGeneratedClasses(ErrorReceiver receiver, WsimportListener listener) {
        List<String> sourceFiles = new ArrayList<String>();

        for (File f : options.getGeneratedFiles()) {
            if (f.exists() && f.getName().endsWith(".java")) {
                sourceFiles.add(f.getAbsolutePath());
            }
        }

        if (sourceFiles.size() > 0) {
            String classDir = options.destDir.getAbsolutePath();
            String classpathString = createClasspathString();
            int majorJavaVersion = getMajorJavaVersion();
            boolean bootCP = useBootClasspath(EndpointContext.class) || useBootClasspath(JAXBPermission.class);
            List<String> args = new ArrayList<String>();
            args.add("-d");
            args.add(classDir);
            args.add("-classpath");
            if (majorJavaVersion < 9) {
                args.add(classpathString);
                //javac is not working in osgi as the url starts with a bundle
                if (bootCP) {
                    args.add("-Xbootclasspath/p:"
                             + JavaCompilerHelper.getJarFile(EndpointContext.class)
                             + File.pathSeparator
                             + JavaCompilerHelper.getJarFile(JAXBPermission.class));
                }
            } else {
                args.add(classpathString + File.pathSeparator + options.classpath);
            }

            if (options.debug) {
                args.add("-g");
            }

            if (options.encoding != null) {
                args.add("-encoding");
                args.add(options.encoding);
            }

            if (options.javacOptions != null) {
                args.addAll(options.getJavacOptions(args, listener));
            }

            for (int i = 0; i < sourceFiles.size(); ++i) {
                args.add(sourceFiles.get(i));
            }

            listener.message(WscompileMessages.WSIMPORT_COMPILING_CODE());
            if (options.verbose) {
                StringBuilder argstr = new StringBuilder();
                for (String arg : args) {
                    argstr.append(arg).append(" ");
                }
                listener.message("javac " + argstr.toString());
            }

            return JavaCompilerHelper.compile(args.toArray(new String[args.size()]), out, receiver);
        }
        //there are no files to compile, so return true?
        return true;
    }

    private String createClasspathString() {
        StringBuilder classpathStr = new StringBuilder(System.getProperty("java.class.path"));
        for (String s : options.cmdlineJars) {
            classpathStr.append(File.pathSeparator);
            classpathStr.append(new File(s).toString());
        }
        return classpathStr.toString();
    }

    protected void usage(Options options) {
        System.out.println(WscompileMessages.WSIMPORT_HELP(WSIMPORT));
        System.out.println(WscompileMessages.WSIMPORT_USAGE_EXTENSIONS());
        System.out.println(WscompileMessages.WSIMPORT_USAGE_EXAMPLES());
    }

    private static int getMajorJavaVersion() {
        String version = System.getProperty("java.version");
        String[] versionElements = version.split("\\D");
        int i = Integer.valueOf(versionElements[0]) == 1 ? 1 : 0;
        return Integer.valueOf(versionElements[i]);
    }
}
