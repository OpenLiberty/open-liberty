/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.schemagen.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.ibm.websphere.metatype.OutputVersion;
import com.ibm.websphere.metatype.SchemaVersion;
import com.ibm.ws.config.schemagen.internal.Generator.ReturnCode;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;

/**
 *
 */
public class GeneratorOptions {

    public static final ResourceBundle messages = ResourceBundle.getBundle(XMLConfigConstants.NLS_PROPS);
    public static final ResourceBundle options = ResourceBundle.getBundle(XMLConfigConstants.NLS_OPTIONS);

    private final HashSet<String> ignoredPids = new HashSet<String>();
    private String outputFile;
    private String encoding = "UTF-8";
    private Locale locale = Locale.getDefault();
    private SchemaVersion schemaVersion = SchemaVersion.getEnum("");
    private OutputVersion outputVersion = OutputVersion.getEnum("");
    private boolean compactOutput = false;

    /**
     * @return
     */
    public Set<String> getIgnoredPids() {
        return ignoredPids;
    }

    /**
     * @param string
     * @throws IOException
     */
    public void addExcludeFile(PidFileArgument pidFileArgument) throws IOException {

        FileInputStream stream = pidFileArgument.getFileInputStream();

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String temp;
        while ((temp = reader.readLine()) != null) {
            ignoredPids.add(temp.trim());
        }
        reader.close();
    }

    /**
     * @param args
     * @return
     */
    public ReturnCode processArgs(String[] args) {
        ReturnCode rc = ReturnCode.OK;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String argToLower = arg.toLowerCase();

            if (arg.startsWith("-")) {
                if (argToLower.contains("-help")) {
                    return ReturnCode.HELP_ACTION;
                } else if (argToLower.contains("-ignorepidsfile")) {
                    try {
                        addExcludeFile(new PidFileArgument(args[i]));
                    } catch (IOException ex) {
                        System.out.println(MessageFormat.format(messages.getString("error.fileNotFound"), args[i]));
                        System.out.println();
                        rc = ReturnCode.BAD_ARGUMENT;
                    }
                } else if (argToLower.contains("-encoding")) {
                    setEncoding(getArgumentValue(args[i]));
                } else if (argToLower.contains("-locale")) {
                    setLocale(new LocaleArgument(args[i]).getLocale());
                } else if (argToLower.contains("-schemaversion")) {
                    setSchemaVersion(SchemaVersion.getEnum(getArgumentValue(argToLower)));
                } else if (argToLower.contains("-outputversion")) {
                    setOutputVersion(OutputVersion.getEnum(getArgumentValue(argToLower)));
                } else if (argToLower.contains("-compactoutput")) {
                	setCompactOutput(Boolean.parseBoolean(getArgumentValue(args[i])));
                }  else {
                    System.out.println(MessageFormat.format(messages.getString("error.unknownArgument"), arg));
                    System.out.println();
                    rc = ReturnCode.BAD_ARGUMENT;
                }
            } else {
                if (outputFile != null) {
                    System.out.println(MessageFormat.format(messages.getString("error.unknownArgument"), arg));
                } else {
                    outputFile = arg;
                    rc = ReturnCode.GENERATE_ACTION;
                }
            }
        }

        if (outputFile == null) {
            System.out.println(messages.getString("error.targetRequired"));
            rc = ReturnCode.BAD_ARGUMENT;
        }

        return rc;
    }

    /**
     * @return
     */
    public String getOutputFile() {
        return this.outputFile;
    }

    /**
     * @return
     */
    public File getLibraryDir() {
        File libDir = getLibLocation();
        if (libDir == null)
            throw new SchemaGeneratorException(messages.getString("error.schemaGenInvalidJarLocation"));

        return libDir;
    }

    /**
     * 
     * @return A File object pointing to the Liberty lib directory
     */
    private File getLibLocation() {
        String classFullPath = Generator.class.getName().replace('.', '/') + ".class";
        ClassLoader classloader = Generator.class.getClassLoader();

        if (classloader == null) {
            //trace here
        } else {
            URL libURL = classloader.getResource(classFullPath);
            URI libURI;
            try {
                libURI = new URI(libURL.getFile());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            String libLocation = libURI.getPath();
            if (libLocation.endsWith(classFullPath)) {
                libLocation = libLocation.substring(0, libLocation.indexOf(classFullPath) - 2);
            } else {
                //trace invalid location
                throw new SchemaGeneratorException(messages.getString("error.schemaGenInvalidJarLocation"));
            }

            final File configJar = new File(libLocation);

            if (AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return configJar.isFile() && configJar.exists();
                }
            })) {
                return configJar.getParentFile();
            } else {
                //trace invalid location
                throw new SchemaGeneratorException(messages.getString("error.schemaGenInvalidJarLocation"));
            }
        }
        return null;
    }

    /**
     * @param encoding the encoding to set
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param locale the locale to set
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    private String getArgumentValue(String arg) {
        int idx = arg.lastIndexOf("=");
        if (idx < 1)
            throw new SchemaGeneratorException(MessageFormat.format(messages.getString("error.invalidArgument"), arg));

        return arg.substring(idx + 1);
    }

    private class PidFileArgument {
        private final String fileName;

        public PidFileArgument(String arg) {
            fileName = getArgumentValue(arg);
        }

        /**
         * @param excludeFile
         * @return
         */
        protected FileInputStream getFileInputStream() throws IOException {
            try {
                return AccessController.doPrivileged(new PrivilegedExceptionAction<FileInputStream>() {
                    @Override
                    public FileInputStream run() throws IOException {
                        return new FileInputStream(fileName);
                    }
                });
            } catch (PrivilegedActionException e) {
                if (e.getException() instanceof IOException)
                    throw (IOException) e.getException();
                throw new RuntimeException(e);
            }
        }
    }

    private class LocaleArgument {

        private final Locale locale;

        /**
         * @param string
         */
        public LocaleArgument(String arg) {
            String localeVal = getArgumentValue(arg);
            
            if (localeVal != null) {
              int index = localeVal.indexOf('_');
              String lang = (index == -1) ? localeVal : localeVal.substring(0, index);
              locale = (index == -1) ? new Locale(lang) : new Locale(lang, localeVal.substring(index + 1));
            } else {
              locale = Locale.ROOT;
            }
        }

        /**
         * @return
         */
        public Locale getLocale() {
            return this.locale;
        }

    }

    public SchemaVersion getSchemaVersion() {
      return schemaVersion;
    }

    public OutputVersion getOutputVersion() {
      return outputVersion;
    }
    
    public boolean getCompactOutput() {
    	return compactOutput;
    }
    
    public void setSchemaVersion(SchemaVersion v) {
      schemaVersion = v;
    }

    public void setOutputVersion(OutputVersion v) {
      outputVersion = v;
    }
    
    public void setCompactOutput(boolean compactOutput) {
    	this.compactOutput = compactOutput;
    }
}
