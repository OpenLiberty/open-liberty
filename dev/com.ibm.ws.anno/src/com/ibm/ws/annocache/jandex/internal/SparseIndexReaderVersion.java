/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * All modifications made by IBM from initial source -
 * https://github.com/wildfly/jandex/blob/master/src/main/java/org/jboss/jandex/IndexReaderImpl.java
 * commit - 36c2b049b7858205c6504308a5e162a4e943ff21
 */
package com.ibm.ws.annocache.jandex.internal;

import java.io.IOException;

public interface SparseIndexReaderVersion {
	/**
	 * Answer the version of index which is being read.
	 * 
	 * @return The index version which is being read.
	 */
    int getVersion();
    
    /**
     * Read a sparse index.
     * 
     * @return The sparse index which was read.
     * 
     * @throws IOException Thrown in case of a read failure.
     */
    SparseIndex read() throws IOException;
}