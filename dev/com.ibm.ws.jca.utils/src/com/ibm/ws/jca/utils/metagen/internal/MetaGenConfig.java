/*******************************************************************************
 * Copyright (c) 2013, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.metagen.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.metagen.MetaGenConstants;
import com.ibm.ws.jca.utils.xml.ra.RaConnector;
import com.ibm.ws.jca.utils.xml.wlp.ra.WlpIbmuiGroups;
import com.ibm.wsspi.adaptable.module.Container;

/**
 * Holds the metatype generator configuration as defined by the user
 */
public class MetaGenConfig {
    @Trivial
    public static enum GenerationMode {
        ExplicitMode,
        RarMode
    }

    @Trivial
    public enum IbmuiGroupScope {
        Global,
        Ocd
    }

    private final Map<String, Object> configProps;
    private GenerationMode genMode;
    private String adapterName;
    private String rarPath;
    private String raXmlPath;
    private String wlpRaXmlPath;
    private MetaGenInstance instance;
    private File nlsInputFile;
    private File nlsOutputFile;
    private File metatypeInputFile;
    private File metatypeOutputFile;
    private IbmuiGroupScope ibmuiGroupScope;
    private WlpIbmuiGroups ibmuiGroups;
    private boolean translate = true;
    private boolean useAnnotations = false;
    private ClassLoader rarClassLoader;
    private Container rarContainer;
    private RaConnector raConnector;
    private boolean runtime = false;
    private String moduleName;

    public MetaGenConfig(Map<String, Object> configProps) throws FileNotFoundException {
        this.configProps = configProps;

        try {
            setMetatypeFiles((String) configProps.get(MetaGenConstants.KEY_METATYPE_INPUT_PATH), (String) configProps.get(MetaGenConstants.KEY_METATYPE_OUTPUT_PATH));
            setGenerationMode((String) configProps.get(MetaGenConstants.KEY_GENERATION_MODE));
            this.adapterName = (String) configProps.get(MetaGenConstants.KEY_ADAPTER_NAME);
            setNLSFiles((String) configProps.get(MetaGenConstants.KEY_NLS_INPUT_FILE), (String) configProps.get(MetaGenConstants.KEY_NLS_OUTPUT_FILE));

            Object translate = configProps.get(MetaGenConstants.KEY_TRANSLATE);
            if (translate != null) {
                if (translate instanceof Boolean)
                    this.translate = (Boolean) translate;
                else if (translate instanceof String)
                    this.translate = Boolean.parseBoolean((String) translate);
            }

            Object useAnnos = configProps.get(MetaGenConstants.KEY_USE_ANNOTATIONS);
            if (useAnnos != null) {
                if (useAnnos instanceof Boolean)
                    useAnnotations = (Boolean) useAnnos;
                else if (useAnnos instanceof String)
                    useAnnotations = Boolean.parseBoolean((String) useAnnos);
            }

            Object raConnector = configProps.get(MetaGenConstants.RAR_DEPLOYMENT_DESCRIPTOR);
            if (raConnector != null) {
                if (raConnector instanceof RaConnector) {
                    this.raConnector = (RaConnector) raConnector;
                    this.runtime = true;
                }
                // TODOCJN else what if this isn't an instance?  Is the check for instanceof RaConnector not needed?
            } else if (useAnnotations == true) {
                // In this instance, there isn't a ra.xml, so there will only be annotations and java bean processing
                // which can only occur at runtime.
                this.runtime = true;
            }

            if (genMode == GenerationMode.ExplicitMode) {
                this.raXmlPath = (String) configProps.get(MetaGenConstants.KEY_RA_XML_PATH);
                this.wlpRaXmlPath = (String) configProps.get(MetaGenConstants.KEY_WLP_RA_XML_PATHS);
            } else if (genMode == GenerationMode.RarMode && !runtime) {
                this.rarPath = (String) configProps.get(MetaGenConstants.KEY_RAR_PATH);
            }

            Object rarCL = configProps.get(MetaGenConstants.KEY_RAR_CLASSLOADER);
            if (rarCL != null) {
                if (rarCL instanceof ClassLoader)
                    rarClassLoader = (ClassLoader) rarCL;
            }

            Object rarContainer = configProps.get(MetaGenConstants.KEY_RAR_CONTAINER);
            if (rarContainer != null) {
                if (rarContainer instanceof Container)
                    this.rarContainer = (Container) rarContainer;
            }

            moduleName = (String) configProps.get(MetaGenConstants.KEY_MODULE_NAME);

            if (useAnnotations && genMode == GenerationMode.ExplicitMode)
                throw new IllegalArgumentException("Annotation processing of resource adapters is only available when metatype generation mode is RAR.");

            if (genMode == GenerationMode.ExplicitMode)
                instance = new MetaGenInstance(adapterName, raXmlPath, wlpRaXmlPath);
            else if (genMode == GenerationMode.RarMode)
                if (runtime)
                    instance = new MetaGenInstance(adapterName, this.raConnector, moduleName);
                else
                    instance = new MetaGenInstance(adapterName, rarPath);
        } catch (Throwable e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            else if (e instanceof Error)
                throw (Error) e;
            else
                throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, T defaultValue) {
        Object value = configProps.get(key);
        return value == null ? defaultValue : (T) value;
    }

    public Container getRarContainer() {
        return rarContainer;
    }

    public ClassLoader getRarClassLoader() {
        return rarClassLoader;
    }

    public boolean useAnnotations() {
        return useAnnotations;
    }

    public boolean isRuntime() {
        return runtime;
    }

    public boolean doTranslate() {
        return translate;
    }

    public void setIbmuiGroups(WlpIbmuiGroups ibmuiGroups) {
        if (this.ibmuiGroups == null) {
            this.ibmuiGroups = ibmuiGroups;

            String scope = ibmuiGroups.getScope();
            if (scope != null) {
                if (ibmuiGroupScope == null) {
                    if (scope.equalsIgnoreCase("global"))
                        ibmuiGroupScope = IbmuiGroupScope.Global;
                    else if (scope.equalsIgnoreCase("ocd"))
                        ibmuiGroupScope = IbmuiGroupScope.Ocd;
                } else {
                    if (scope.equalsIgnoreCase("global") && ibmuiGroupScope == IbmuiGroupScope.Ocd)
                        ibmuiGroupScope = IbmuiGroupScope.Global;
                }
            }
        }
    }

    public WlpIbmuiGroups getIbmuiGroups() {
        return ibmuiGroups;
    }

    public boolean isIbmuiGroupScopeGlobal() {
        if (ibmuiGroupScope == null)
            return true; // default is global

        return ibmuiGroupScope == IbmuiGroupScope.Global;
    }

    public boolean isIbmuiGroupScopeOcd() {
        if (ibmuiGroupScope == null)
            return false;

        return ibmuiGroupScope == IbmuiGroupScope.Ocd;
    }

    public MetaGenInstance getInstance() {
        return instance;
    }

    private void setGenerationMode(String mode) {
        if (mode == null || ((mode = mode.trim()).isEmpty()))
            throw new IllegalArgumentException("The metatype generator configuration property " + MetaGenConstants.KEY_GENERATION_MODE + " was not set or is empty.");

        mode = mode.toLowerCase();

        if (mode.equals(MetaGenConstants.VALUE_GENERATION_MODE_EXPLICIT))
            genMode = GenerationMode.ExplicitMode;
        else if (mode.equals(MetaGenConstants.VALUE_GENERATION_MODE_RAR))
            genMode = GenerationMode.RarMode;
        else
            throw new IllegalArgumentException("Invalid metatype generation mode: " + mode);
    }

    public GenerationMode getGenerationMode() {
        return genMode;
    }

    private void setMetatypeFiles(String inputPath, String outputPath) {
        if (inputPath != null && !(inputPath = inputPath.trim()).isEmpty() && inputPath.endsWith(InternalConstants.METATYPE_XML_FILE_NAME))
            metatypeInputFile = new File(inputPath);

        if (outputPath != null)
            if (outputPath.endsWith(InternalConstants.METATYPE_XML_FILE_NAME))
                metatypeOutputFile = new File(outputPath);
            else
                metatypeOutputFile = new File(outputPath + File.separatorChar + InternalConstants.METATYPE_XML_FILE_NAME);
    }

    public File getMetatypeOutputFile() {
        return metatypeOutputFile;
    }

    public File getMetatypeInputFile() {
        return metatypeInputFile;
    }

    private void setNLSFiles(String inputPath, String outputPath) {
        boolean outputRequired = false;

        if (inputPath != null && !(inputPath = inputPath.trim()).isEmpty()) {
            outputRequired = true;

            if (inputPath.endsWith(InternalConstants.METATYPE_PROPERTIES_FILE_NAME))
                nlsInputFile = new File(inputPath);
            else
                throw new IllegalArgumentException("Invalid translation file path: " + inputPath);
        }

        if (outputPath == null || (outputPath = outputPath.trim()).isEmpty()) {
            if (outputRequired)
                throw new IllegalArgumentException("The output translation file path is missing.");
        } else {
            if (outputPath.endsWith(InternalConstants.METATYPE_PROPERTIES_FILE_NAME))
                nlsOutputFile = new File(outputPath);
            else
                throw new IllegalArgumentException("Invalid translation file path: " + outputPath);
        }
    }

    public File getNLSInputFile() {
        return nlsInputFile;
    }

    public File getNLSOutputFile() {
        return nlsOutputFile;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MetaGenConfig{");

        if (adapterName != null)
            sb.append("adapterName='").append(adapterName).append("' ");

        if (rarPath != null)
            sb.append("rarPath='").append(rarPath).append("' ");

        if (raXmlPath != null)
            sb.append("raXmlPath='").append(raXmlPath).append("' ");

        if (wlpRaXmlPath != null)
            sb.append("wlpRaXmlPath='").append(wlpRaXmlPath).append("' ");

        if (metatypeInputFile != null)
            sb.append("metatypeInputFile='").append(metatypeInputFile.getAbsolutePath()).append("' ");
        if (metatypeOutputFile != null)
            sb.append("metatypeOutputFile='").append(metatypeOutputFile.getAbsolutePath()).append("' ");
        if (nlsInputFile != null)
            sb.append("nlsInputFile='").append(nlsInputFile.getAbsolutePath()).append("' ");
        sb.append("genMode='").append(genMode).append("' ");
        sb.append("translate='").append(translate).append("' ");
        sb.append("useAnnotations='").append(useAnnotations).append("' ");
        if (rarClassLoader != null)
            sb.append("rarClassLoader='").append(rarClassLoader).append("' ");
        if (rarContainer != null)
            sb.append("rarContainer='").append(rarContainer).append("' ");

        sb.append('}');
        return sb.toString();
    }
}
