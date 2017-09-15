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
package com.ibm.ws.javaee.ddmetadata.processor;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import com.ibm.ws.javaee.ddmetadata.annotation.DDRootElement;
import com.ibm.ws.javaee.ddmetadata.annotation.LibertyNotInUse;
import com.ibm.ws.javaee.ddmetadata.generator.ComponentImplGenerator;
import com.ibm.ws.javaee.ddmetadata.generator.MetatypeFileGenerator;
import com.ibm.ws.javaee.ddmetadata.generator.ModelAdapterClassGenerator;
import com.ibm.ws.javaee.ddmetadata.generator.ModelInterfaceImplClassGenerator;
import com.ibm.ws.javaee.ddmetadata.generator.ModelPackageInfoClassGenerator;
import com.ibm.ws.javaee.ddmetadata.generator.ModelParserClassGenerator;
import com.ibm.ws.javaee.ddmetadata.model.Model;
import com.ibm.ws.javaee.ddmetadata.model.ModelInterfaceType;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ModelGeneratorProcessor extends AbstractProcessor {
    private static final String OPTION_SRC_DEST_DIR = "srcdestdir";
    private static final String OPTION_RESOURCE_DEST_DIR = "resourcedir";
    private static final String OPTION_PACKAGES = "packages";

    /**
     * Destination directory for generated .java.
     */
    private File srcDestDir;

    /**
     * Destination directory for generated resources
     */
    private File resourceDestDir;

    /**
     * The packages that should be written.
     */
    private Set<String> packages;

    /**
     * True if this processor initialized successfully.
     */
    private boolean initialized;

    private ModelBuilder builder;

    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>(Arrays.asList(OPTION_SRC_DEST_DIR,
                                                 OPTION_PACKAGES));
    }

    private void error(String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        String srcDestDir = processingEnv.getOptions().get(OPTION_SRC_DEST_DIR);
        if (srcDestDir == null) {
            error("Expected " + OPTION_SRC_DEST_DIR + " annotation processor option");
            return;
        }

        this.srcDestDir = new File(srcDestDir).getAbsoluteFile();
        if (!this.srcDestDir.isDirectory()) {
            error("Source destination directory does not exist: " + this.srcDestDir);
            return;
        }

        String resourceDestDir = processingEnv.getOptions().get(OPTION_RESOURCE_DEST_DIR);
        if (resourceDestDir == null) {
            error("Expected " + OPTION_RESOURCE_DEST_DIR + " annotation processor option");
        }

        this.resourceDestDir = new File(resourceDestDir).getAbsoluteFile();
        if (!this.resourceDestDir.isDirectory()) {
            error("Resource destination directory does not exist: " + this.resourceDestDir);
            return;
        }

        String packages = processingEnv.getOptions().get(OPTION_PACKAGES);
        if (packages == null) {
            error("Expected " + OPTION_PACKAGES + " annotation processor option");
            return;
        }

        this.packages = new HashSet<String>(Arrays.asList(packages.split("[\\s,]+")));

        initialized = true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<String>(Arrays.asList(DDRootElement.class.getName()));
    }

    private String getPackageName(String interfaceName) {
        return interfaceName.substring(0, interfaceName.lastIndexOf('.'));
    }

    private boolean outputType(String interfaceName, String implClassName, Set<String> implPackageNames) {
        if (packages.contains(getPackageName(interfaceName))) {
            try {
                // Check to see if the interface is annotated with @LibertyNotInuse
                Class<?> klass = Class.forName(interfaceName);
                if (klass.getAnnotation(LibertyNotInUse.class) != null)
                    return false;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
            implPackageNames.add(getPackageName(implClassName));
            return true;
        }
        return false;
    }

    private <T> T createGenerator(Class<T> baseClass, Model model, ModelInterfaceType type, File destinationDir) {
        String interfaceName = type == null ? model.getRootType().interfaceName : type.interfaceName;
        String className = interfaceName.replace("com.ibm.ws.javaee.dd.", "com.ibm.ws.javaee.ddmetadata.generator.") + baseClass.getSimpleName();

        Class<? extends T> klass;
        try {
            klass = Class.forName(className).asSubclass(baseClass);
        } catch (ClassNotFoundException e) {
            klass = baseClass;
        }

        try {
            if (type == null) {
                return klass.getConstructor(File.class, Model.class).newInstance(destinationDir, model);
            }
            return klass.getConstructor(File.class, ModelInterfaceType.class).newInstance(destinationDir, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!initialized) {
            return true;
        }

        if (roundEnv.processingOver()) {
            if (roundEnv.errorRaised()) {
                return false;
            }

            Set<ModelInterfaceType> dumped = new HashSet<ModelInterfaceType>();
            for (Model model : builder.models) {
                model.root.dump(System.out, new StringBuilder(), dumped);
                System.out.println();
            }

            Set<String> implPackages = new LinkedHashSet<String>();

            for (Model model : builder.models) {
                if (outputType(model.getRootType().interfaceName, model.parserImplClassName, implPackages)) {
                    createGenerator(ModelAdapterClassGenerator.class, model, null, srcDestDir).generate();
                    createGenerator(ModelParserClassGenerator.class, model, null, srcDestDir).generate();
                }
            }

            for (ModelInterfaceType intfType : builder.interfaceTypes.values()) {
                if (outputType(intfType.interfaceName, intfType.implClassName, implPackages)) {
                    createGenerator(ModelInterfaceImplClassGenerator.class, null, intfType, srcDestDir).generate();
                    createGenerator(MetatypeFileGenerator.class, null, intfType, resourceDestDir).generate();
                    createGenerator(ComponentImplGenerator.class, null, intfType, srcDestDir).generate();

                }
            }

            for (String packageName : implPackages) {
                new ModelPackageInfoClassGenerator(srcDestDir, packageName).generate();
            }
        } else {
            if (builder == null) {
                builder = new ModelBuilder(processingEnv);
            }

            for (TypeElement rootAnnotation : annotations) {
                builder.process(roundEnv.getElementsAnnotatedWith(rootAnnotation));
            }
        }

        return false;
    }
}
