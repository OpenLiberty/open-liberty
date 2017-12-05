/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi;

import java.util.Set;

import org.eclipse.microprofile.openapi.OASModelReader;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.app.deploy.WebModuleInfo;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.core.util.Yaml;
import com.ibm.ws.microprofile.openapi.impl.jaxrs2.Reader;
import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;
import com.ibm.wsspi.adaptable.module.Container;

/**
 *
 */
@Component(service = { ApplicationProcessor.class }, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM" })
public class ApplicationProcessor {

    enum DocType {
        JSON,
        YAML
    }

    private OpenAPI document = null;
    private ApplicationInfo currentApp = null;

    public void activate(ComponentContext cc) {
        this.document = createBaseOpenAPIDocument();
    }

    public void processApplication(ApplicationInfo appInfo) {
        synchronized (this.document) {

            if (currentApp != null) {
                return;
            }

            if (appInfo == null) {
                return;
            }

            Container appContainer = appInfo.getContainer();
            if (appContainer == null) {
                return;
            }

            WebModuleInfo moduleInfo = OpenAPIUtils.getWebModuleInfo(appContainer);
            if (moduleInfo == null) {
                return;
            }
            ClassLoader appClassloader = moduleInfo.getClassLoader();

            boolean isOASApp = false;

            //read and process the MicroProfile config
            ConfigProcessor configProcessor = new ConfigProcessor(appClassloader);

            OpenAPI newDocument = null;
            //Retrieve model from model reader
            OASModelReader modelReader = OpenAPIUtils.getOASModelReader(appClassloader, configProcessor.getModelReaderClassName());
            if (modelReader != null) {
                try {
                    new OASFactoryResolverImpl();
                    OpenAPI model = modelReader.buildModel();
                    if (model != null) {
                        isOASApp = true;
                        newDocument = model;
                    }
                } catch (Throwable e) {
                    //TODO: add tracing
                }
            }

            //Retrieve OpenAPI document as a string
            String openAPIStaticFile = StaticFileProcessor.getOpenAPIFile(appContainer);
            if (openAPIStaticFile != null) {
                isOASApp = true;
            }

            //Parse a document into the model
            //TODO: Parse a document into the model

            //Scan for annotated classes
            AnnotationScanner scanner = OpenAPIUtils.creatAnnotationScanner(appClassloader, appContainer);
            if (scanner.anyAnnotatedClasses()) {
                isOASApp = true;
                Set<Class<?>> classes = scanner.getAnnotatedClasses();
                newDocument = new Reader(newDocument).read(classes);
            }

            if (isOASApp && currentApp == null) {
                currentApp = appInfo;
            }
            this.document = newDocument;
        }
    }

    public void removeApplication(ApplicationInfo appInfo) {

        synchronized (this.document) {
            if (currentApp != null && currentApp.getName().equals(appInfo.getName())) {
                currentApp = null;
                this.document = createBaseOpenAPIDocument();
            }
        }
    }

    @FFDCIgnore(JsonProcessingException.class)
    public String getOpenAPIDocument(DocType docType) {
        String oasResult = null;
        synchronized (this.document) {
            try {
                if (DocType.YAML == docType) {
                    oasResult = Yaml.mapper().writeValueAsString(this.document);
                } else {
                    oasResult = Json.mapper().writeValueAsString(this.document);
                }
            } catch (JsonProcessingException e) {

            }
        }
        return oasResult;
    }

    private OpenAPI createBaseOpenAPIDocument() {
        OpenAPI openAPI = new OpenAPIImpl();
        openAPI.info(new InfoImpl().title("Liberty APIs").version("1.0"));
        openAPI.paths(new PathsImpl());
        return openAPI;
    }

}
