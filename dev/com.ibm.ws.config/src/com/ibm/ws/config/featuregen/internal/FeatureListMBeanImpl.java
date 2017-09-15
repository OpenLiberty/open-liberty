/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.featuregen.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.config.mbeans.FeatureListMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FileUtils;

@Component(service = FeatureListMBean.class,
           property = {
                       "jmx.objectname=" + FeatureListMBean.OBJECT_NAME,
                       "service.vendor=IBM" })
public class FeatureListMBeanImpl extends StandardMBean implements FeatureListMBean {

    private static final TraceComponent tc = Tr.register(FeatureListMBeanImpl.class);

    public static final String TRACE_BUNDLE_SCHEMA = "com.ibm.ws.config.internal.resources.SchemaData";

    //Location service
    static final String KEY_LOCATION_ADMIN = "locationAdmin";
    private static final AtomicServiceReference<WsLocationAdmin> locationAdminRef = new AtomicServiceReference<WsLocationAdmin>(KEY_LOCATION_ADMIN);

    //Constants
    private static final String FEAT_LIST_PREFIX = "featureList_";
    private static final String FEAT_LIST_SUFFIX = ".xml";

    public FeatureListMBeanImpl() throws NotCompliantMBeanException {
        super(FeatureListMBean.class, false);
    }

    @Activate
    protected void activate(ComponentContext ctxt) throws IOException {
        locationAdminRef.activate(ctxt);
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
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

    /**
     * {@inheritDoc}
     * 
     * @see com.ibm.ws.kernel.feature.internal.FeatureListMBean#generate(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public Map<String, Object> generate(String encoding, String locale, String productExt) {

        Map<String, Object> returnMap;
        try {
            //keeps list of commands
            final ArrayList<String> commandList = new ArrayList<String>();

            //Get java home
            final String javaHome = AccessController.doPrivileged(
                            new PrivilegedAction<String>() {
                                @Override
                                public String run() {
                                    return System.getProperty("java.home");
                                }
                            });

            //Build path to ws-featurelist.jar
            final String featureListGenPath = getWsLocationAdmin().resolveString(WsLocationConstants.SYMBOL_INSTALL_DIR + "bin/tools/ws-featurelist.jar");

            //First command is to invoke the featureList jar
            commandList.add(javaHome + "/bin/java");
            commandList.add("-jar");
            commandList.add(featureListGenPath);

            //Encoding
            if (encoding != null && !encoding.trim().isEmpty()) {
                commandList.add("--encoding=" + encoding);
            }

            //Locale
            if (locale != null && !locale.trim().isEmpty()) {
                commandList.add("--locale=" + locale);
            }

            //Product Extension
            if (productExt != null && !productExt.trim().isEmpty()) {
                commandList.add("--productExtension=" + productExt);
            }

            //Create empty file that will be populated
            final File targetDirectory = new File(getWsLocationAdmin().resolveString(WsLocationConstants.SYMBOL_SERVER_STATE_DIR));
            if (!FileUtils.fileExists(targetDirectory)) {
                FileUtils.fileMkDirs(targetDirectory);
            }

            final File generatedFile = File.createTempFile(FEAT_LIST_PREFIX, FEAT_LIST_SUFFIX, targetDirectory);

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

            Process featureListGenProc = builder.start();
            String output = getOutput(featureListGenProc);
            int exitVal = featureListGenProc.waitFor();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "ExitVal: " + exitVal);
            }

            if (exitVal != 0) {
                return retError(output);
            }

            returnMap = new HashMap<String, Object>();
            returnMap.put(FeatureListMBean.KEY_FILE_PATH, generatedFile.getAbsolutePath());
            returnMap.put(FeatureListMBean.KEY_OUTPUT, output);
            returnMap.put(FeatureListMBean.KEY_RETURN_CODE, FeatureListMBean.RETURN_CODE_OK);

        } catch (IOException ioe) {
            return retError(ioe.getMessage());
        } catch (InterruptedException ie) {
            return retError(ie.getMessage());
        }

        return returnMap;
    }

    private Map<String, Object> retError(String msg) {
        //Populate return map with error message
        Map<String, Object> resultMap = new HashMap<String, Object>();
        resultMap.put(FeatureListMBean.KEY_OUTPUT, msg);
        resultMap.put(FeatureListMBean.KEY_RETURN_CODE, FeatureListMBean.RETURN_CODE_ERROR);
        return resultMap;
    }

    private void throwMissingServiceError(String service) {
        throw new RuntimeException(Tr.formatMessage(tc, "OSGI_SERVICE_ERROR", service));
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

}
