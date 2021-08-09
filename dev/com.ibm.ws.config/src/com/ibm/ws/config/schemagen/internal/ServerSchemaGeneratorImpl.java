/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.schemagen.internal;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.management.StandardMBean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.config.mbeans.ServerSchemaGenerator;
import com.ibm.websphere.metatype.OutputVersion;
import com.ibm.websphere.metatype.SchemaGenerator;
import com.ibm.websphere.metatype.SchemaGeneratorOptions;
import com.ibm.websphere.metatype.SchemaVersion;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

@Component(service = ServerSchemaGenerator.class,
           property = { "jmx.objectname=" + ServerSchemaGenerator.OBJECT_NAME,
                       "service.vendor=IBM" })
public class ServerSchemaGeneratorImpl extends StandardMBean implements ServerSchemaGenerator {

    private static final TraceComponent tc = Tr.register(ServerSchemaGeneratorImpl.class);

    public static final String TRACE_BUNDLE_SCHEMA = "com.ibm.ws.config.internal.resources.SchemaData";

    private static final String SERVER_SCHEMA_DIR = WsLocationConstants.SYMBOL_SERVER_OUTPUT_DIR + File.separator + "schemagen";

    //Context used to get bundles during generation
    private ComponentContext context;

    //OSGi references
    private static final String KEY_SCHEMA_GEN = "schemaGen";
    private final AtomicServiceReference<SchemaGenerator> schemaGenRef = new AtomicServiceReference<SchemaGenerator>(KEY_SCHEMA_GEN);

    private static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    //Constants
    private static final String SCHEMA_GEN_PREFIX = "schemaGenerator_";
    private static final String SERVER_SCHEMA_GEN_FILE = "server.xsd";
    private static final String SCHEMA_GEN_SUFFIX = ".xsd";

    public ServerSchemaGeneratorImpl() {
        super(ServerSchemaGenerator.class, false);
    }

    @Activate
    protected void activate(ComponentContext ctxt) throws Exception {
        context = ctxt;
        schemaGenRef.activate(ctxt);
        locationAdminRef.activate(ctxt);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        schemaGenRef.deactivate(ctxt);
        locationAdminRef.deactivate(ctxt);
    }

    @Reference(name = KEY_LOCATION_ADMIN, service = WsLocationAdmin.class)
    protected void setLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.setReference(ref);
    }

    protected void unsetLocationAdmin(ServiceReference<WsLocationAdmin> ref) {
        locationAdminRef.unsetReference(ref);
    }

    protected WsLocationAdmin getWsLocationAdmin() {
        WsLocationAdmin locationAdmin = locationAdminRef.getService();

        if (locationAdmin == null) {
            throwMissingServiceError("WsLocationAdmin");
        }

        return locationAdmin;
    }

    @Reference(name = KEY_SCHEMA_GEN, service = SchemaGenerator.class)
    protected void setSchemaGen(ServiceReference<SchemaGenerator> ref) {
        schemaGenRef.setReference(ref);
    }

    protected void unsetSchemaGen(ServiceReference<SchemaGenerator> ref) {
        schemaGenRef.unsetReference(ref);
    }

    protected SchemaGenerator getSchemaGenerator() {
        SchemaGenerator schemaGen = schemaGenRef.getService();

        if (schemaGen == null) {
            throwMissingServiceError("SchemaGenerator");
        }

        return schemaGen;
    }

    @Override
    public String generate() {
        OutputStream out = new ByteArrayOutputStream();
        String str = null;
        SchemaGeneratorOptions options = new SchemaGeneratorOptions();
        options.setEncoding("UTF-8");
        options.setBundles(context.getBundleContext().getBundles());
        options.setIsRuntime(true);
        try {
            getSchemaGenerator().generate(out, options);
            str = out.toString();
        } catch (IOException e) {
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            // https://websphere.pok.ibm.com/~liberty/secure/docs/dev/API/com.ibm.ws.ras/com/ibm/ws/ffdc/annotation/FFDCIgnore.html
        }
        return str;
    }

    @Override
    public Map<String, Object> generateInstallSchema(String schemaVersion, String outputVersion, String encoding, String locale) {
        return commonGenerateInstallSchema(schemaVersion, outputVersion, encoding, locale, false, false);
    }

    @Override
    public Map<String, Object> generateInstallSchema(String schemaVersion, String outputVersion, String encoding, String locale, boolean compactOutput) {
        return commonGenerateInstallSchema(schemaVersion, outputVersion, encoding, locale, compactOutput, true);
    }

    protected Map<String, Object> commonGenerateInstallSchema(String schemaVersion, String outputVersion, String encoding, String locale, boolean compactOutput, boolean isCompact) {

        Map<String, Object> returnMap;
        try {
            //Will keep the commands
            final ArrayList<String> commandList = new ArrayList<String>();

            //Get java home
            final String javaHome = AccessController.doPrivileged(
                            new PrivilegedAction<String>() {
                                @Override
                                public String run() {
                                    return System.getProperty("java.home");
                                }

                            });

            //Build path to schemaGen jar
            final String schemaGenPath = getWsLocationAdmin().resolveString(WsLocationConstants.SYMBOL_INSTALL_DIR + "bin/tools/ws-schemagen.jar");

            //First command is to invoke the schemaGen jar
            commandList.add(javaHome + "/bin/java");
            commandList.add("-jar");
            commandList.add(schemaGenPath);

            //Encoding
            if (encoding != null && !encoding.trim().isEmpty()) {
                commandList.add("--encoding=" + encoding);
            }

            //Locale
            if (locale != null && !locale.trim().isEmpty()) {
                commandList.add("--locale=" + locale);
            }

            //CompactOutput
            if (isCompact) {
                commandList.add("--compactOutput=" + compactOutput);
            }
            //SchemaVersion
            commandList.add("--schemaVersion=" + SchemaVersion.getEnum(schemaVersion));

            //OutputVersion
            commandList.add("--outputVersion=" + OutputVersion.getEnum(outputVersion));

            //Create empty file that will be populated
            final File targetDirectory = new File(getWsLocationAdmin().resolveString(WsLocationConstants.SYMBOL_SERVER_STATE_DIR));
            if (!FileUtils.fileExists(targetDirectory)) {
                FileUtils.fileMkDirs(targetDirectory);
            }

            final File generatedFile = File.createTempFile(SCHEMA_GEN_PREFIX, SCHEMA_GEN_SUFFIX, targetDirectory);

            //Filename goes last in the script's invoke
            commandList.add(generatedFile.getAbsolutePath());

            //Debug command list
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder();
                for (String command : commandList) {
                    sb.append(command);
                    sb.append("\n");
                }
                Tr.debug(tc, "List of commands:\n" + sb.toString());
            }

            //Run the command
            ProcessBuilder builder = new ProcessBuilder(commandList);
            builder.redirectErrorStream(true); //merge error and output together

            Process schemaGenProc = builder.start();
            String output = getOutput(schemaGenProc);
            int exitVal = schemaGenProc.waitFor();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ExitVal: " + exitVal);
            }

            if (exitVal != 0) {
                return retError(output);
            }

            //Populate return map with generated file location
            returnMap = new HashMap<String, Object>();
            returnMap.put(ServerSchemaGenerator.KEY_FILE_PATH, generatedFile.getAbsolutePath());
            returnMap.put(ServerSchemaGenerator.KEY_OUTPUT, output);
            returnMap.put(ServerSchemaGenerator.KEY_RETURN_CODE, ServerSchemaGenerator.RETURN_CODE_OK);
        } catch (IllegalArgumentException iae) {
            return retError("java.lang.IllegalArgumentException: " + iae.getMessage());
        } catch (IOException ioe) {
            return retError("java.io.IOException: " + ioe.getMessage());
        } catch (InterruptedException ie) {
            return retError("java.lang.InterruptedException: " + ie.getMessage());
        }

        return returnMap;
    }

    /**
     * @param joinProc
     * @return
     * @throws IOException
     */
    private String getOutput(Process joinProc) throws IOException {
        InputStream stream = joinProc.getInputStream();

        char[] buf = new char[512];
        int charsRead;
        StringBuilder sb = new StringBuilder();
        InputStreamReader reader = null;
        try {
            //Ignoring findbugs for this constructor, since we actually want the default encoding here (given the file was generated in default encoding)
            reader = new InputStreamReader(stream);
            while ((charsRead = reader.read(buf)) > 0) {
                sb.append(buf, 0, charsRead);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return sb.toString();
    }

    private void throwMissingServiceError(String service) {
        throw new RuntimeException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", service));
    }

    private Map<String, Object> retError(String msg) {
        //Populate return map with error message
        Map<String, Object> returnMap = new HashMap<String, Object>();
        returnMap.put(ServerSchemaGenerator.KEY_OUTPUT, msg);
        returnMap.put(ServerSchemaGenerator.KEY_RETURN_CODE, ServerSchemaGenerator.RETURN_CODE_ERROR);
        return returnMap;
    }

    @Override
    public Map<String, Object> generateServerSchema(String schemaVersion, String outputVersion, String encoding, String locale) {
        Map<String, Object> returnMap;
        if (encoding == null || encoding.isEmpty())
            encoding = "UTF-8";
        SchemaGeneratorOptions options = new SchemaGeneratorOptions();

        final File targetDirectory = new File(getWsLocationAdmin().resolveString(SERVER_SCHEMA_DIR));
        if (!FileUtils.fileExists(targetDirectory)) {
            FileUtils.fileMkDirs(targetDirectory);
        }

        File generatedFile = null;
        try {

            generatedFile = new File(targetDirectory, SERVER_SCHEMA_GEN_FILE);

            PrintWriter writer = new PrintWriter(generatedFile, encoding);
            options.setEncoding(encoding);
            options.setLocale(getLocale(locale));
            options.setBundles(context.getBundleContext().getBundles());
            options.setIsRuntime(true);
            options.setOutputVersion(outputVersion);
            options.setSchemaVersion(schemaVersion);
            getSchemaGenerator().generate(writer, options);
        } catch (IOException ioe) {
            return retError("java.io.IOException: " + ioe.getMessage());
        }
        //Populate return map with generated file location
        returnMap = new HashMap<String, Object>();
        returnMap.put(ServerSchemaGenerator.KEY_FILE_PATH, generatedFile.getAbsolutePath());
        returnMap.put(ServerSchemaGenerator.KEY_RETURN_CODE, ServerSchemaGenerator.RETURN_CODE_OK);
        returnMap.put(ServerSchemaGenerator.KEY_OUTPUT, Integer.toString(ServerSchemaGenerator.RETURN_CODE_OK));

        return returnMap;
    }

    private Locale getLocale(String localeStr) {
        Locale retLocale = Locale.getDefault();
        if (localeStr != null) {
            int index = localeStr.indexOf('_');
            String country = (index == -1) ? localeStr : localeStr.substring(0, index);
            retLocale = ((index == -1) ? new Locale(country) : new Locale(localeStr.substring(index + 1)));
        }
        return retLocale;
    }

}
