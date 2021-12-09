/*********************************************************** distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initia

********************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies thl API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.parser.util;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.ibm.ws.microprofile.openapi.impl.parser.core.models.AuthorizationValue;
import com.ibm.ws.microprofile.openapi.impl.parser.models.RefFormat;

public class RefUtils {

    public static String computeDefinitionName(String ref, Set<String> reserved) {

        final String[] refParts = ref.split("#/");

        if (refParts.length > 2) {
            throw new RuntimeException("Invalid ref format: " + ref);
        }

        final String file = refParts[0];
        final String definitionPath = refParts.length == 2 ? refParts[1] : null;

        String plausibleName;

        if (definitionPath != null) { //the name will come from the last element of the definition path
            final String[] jsonPathElements = definitionPath.split("/");
            plausibleName = jsonPathElements[jsonPathElements.length - 1];
        } else { //no definition path, so we must come up with a name from the file
            final String[] filePathElements = file.split("/");
            plausibleName = filePathElements[filePathElements.length - 1];

            final String[] split = plausibleName.split("\\.");
            plausibleName = split[0];
        }
        String tryName = plausibleName;

        for (int i = 2; reserved.contains(tryName); i++) {
            tryName = plausibleName + "_" + i;
        }

        return tryName;
    }

    public static boolean isAnExternalRefFormat(RefFormat refFormat) {
        return refFormat == RefFormat.URL || refFormat == RefFormat.RELATIVE;
    }

    public static RefFormat computeRefFormat(String ref) {
        RefFormat result = RefFormat.INTERNAL;
        if (ref.startsWith("http")) {
            result = RefFormat.URL;
        } else if (ref.startsWith("#/")) {
            result = RefFormat.INTERNAL;
        } else if (ref.startsWith(".") || ref.startsWith("/")) {
            result = RefFormat.RELATIVE;
        }

        return result;
    }

    public static String readExternalUrlRef(String file, RefFormat refFormat, List<AuthorizationValue> auths,
                                            String rootPath) {

        if (!RefUtils.isAnExternalRefFormat(refFormat)) {
            throw new RuntimeException("Ref is not external");
        }

        String result;

        try {
            if (refFormat == RefFormat.URL) {
                result = RemoteUrl.urlToString(file, auths);
            } else {
                //its assumed to be a relative ref
                String url = buildUrl(rootPath, file);

                return readExternalRef(url, RefFormat.URL, auths, null);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + refFormat + " ref: " + file, e);
        }

        return result;

    }

    public static String buildUrl(String rootPath, String relativePath) {
        String[] rootPathParts = rootPath.split("/");
        String[] relPathParts = relativePath.split("/");

        if (rootPath == null || relativePath == null) {
            return null;
        }

        int trimRoot = 0;
        int trimRel = 0;

        if (!"".equals(rootPathParts[rootPathParts.length - 1])) {
            trimRoot = 1;
        }
        for (int i = 0; i < rootPathParts.length; i++) {
            if ("".equals(rootPathParts[i])) {
                trimRel += 1;
            } else {
                break;
            }
        }
        for (int i = 0; i < relPathParts.length; i++) {
            if (".".equals(relPathParts[i])) {
                trimRel += 1;
            } else if ("..".equals(relPathParts[i])) {
                trimRel += 1;
            }
        }

        String[] outputParts = new String[rootPathParts.length + relPathParts.length - trimRoot - trimRel];
        System.arraycopy(rootPathParts, 0, outputParts, 0, rootPathParts.length - trimRoot);
        System.arraycopy(relPathParts,
                         trimRel,
                         outputParts,
                         rootPathParts.length - trimRoot + trimRel - 1,
                         relPathParts.length - trimRel);

        return StringUtils.join(outputParts, "/");
    }

    public static String readExternalRef(String file, RefFormat refFormat, List<AuthorizationValue> auths,
                                         Path parentDirectory) {

        if (!RefUtils.isAnExternalRefFormat(refFormat)) {
            throw new RuntimeException("Ref is not external");
        }

        String result;

        try {
            if (refFormat == RefFormat.URL) {
                result = RemoteUrl.urlToString(file, auths);
            } else {
                //its assumed to be a relative file ref
                final Path pathToUse = parentDirectory.resolve(file).normalize();

                if (Files.exists(pathToUse)) {
                    result = IOUtils.toString(new FileInputStream(pathToUse.toFile()), StandardCharsets.UTF_8);
                } else {
                    result = ClasspathHelper.loadFileFromClasspath(file);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to load " + refFormat + " ref: " + file, e);
        }

        return result;

    }
}