/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.signature;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The XMLSignature Input for processing {@link Path} or {@link File}.
 * <p>
 * NOTE: The stream may be closed in the process, but it is not guaranteed.
 */
public class XMLSignatureFileInput extends XMLSignatureStreamInput {

    /**
     * Construct a XMLSignatureInput from a File
     * <p>
     * NOTE: The stream may be closed in the process, but it is not guaranteed.
     *
     * @param file
     * @throws IOException
     */
    public XMLSignatureFileInput(Path file) throws IOException {
        super(new BufferedInputStream(Files.newInputStream(file), 8192));
    }


    /**
     * Construct a XMLSignatureInput from a File
     * <p>
     * NOTE: The stream may be closed in the process, but it is not guaranteed.
     *
     * @param file
     * @throws IOException
     */
    public XMLSignatureFileInput(File file) throws IOException {
        super(new BufferedInputStream(Files.newInputStream(file.toPath()), 8192));
    }
}
