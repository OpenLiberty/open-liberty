/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.archive;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.archive.DirPattern.PatternStrategy;

/**
 * This class is used to add a directory to an archive while making an effort to strip sensitive information. At
 * the moment, it will replace configuration values that appear to be encoded and the value of wlp.password.encryption.key.
 *
 * More filters may need to be added in the future. If there end up being more than a few, it would probably make sense
 * to have a pluggable framework.
 */
public class FilteredDirEntryConfig extends DirEntryConfig {

    final String FILTER_REGEX = "\"(\\{aes\\}|\\{xor\\}).*\"";
    final String WLP_PASSWORD_ENCYRPTION_STRING = "wlp.password.encryption.key";
    final String WLP_PASSWORD_ENCRYPTION_REGEX = WLP_PASSWORD_ENCYRPTION_STRING + "=.*$";
    final String OBSCURED_VALUE = "\"*****\"";
    final Pattern obscuredValuePattern = Pattern.compile(FILTER_REGEX);
    final Pattern wlpPasswordEncryptionPattern = Pattern.compile(WLP_PASSWORD_ENCRYPTION_REGEX, Pattern.MULTILINE);

    public FilteredDirEntryConfig(File source, boolean includeByDefault, PatternStrategy strategy) throws IOException {
        super("", source, includeByDefault, strategy);
    }

    @Override
    public void configure(Archive archive) throws IOException {
        List<String> dirContent = new ArrayList<String>();
        filterDirectory(dirContent, dirPattern, "");

        for (String file : dirContent) {
            Path originalPath = new File(source, file).toPath();
            if (originalPath.toFile().isDirectory())
                continue;

            // Create a temporary file. We will write the filtered content to this file and include it in the archive.
            Path tempFile = Files.createTempFile(null, null);

            String originalFile = new String(Files.readAllBytes(originalPath));
            String newFile = obscuredValuePattern.matcher(originalFile).replaceAll(OBSCURED_VALUE);
            newFile = wlpPasswordEncryptionPattern.matcher(newFile).replaceAll(WLP_PASSWORD_ENCYRPTION_STRING + "=*****");
            Files.write(tempFile, newFile.getBytes());
            archive.addFileEntry(file, tempFile.toFile());
        }

    }

}
