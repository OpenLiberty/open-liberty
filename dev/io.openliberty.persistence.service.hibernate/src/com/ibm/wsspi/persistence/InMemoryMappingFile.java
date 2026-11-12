/*******************************************************************************
 * Copyright (c) 2014,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.persistence;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * An in-memory representation of an orm.xml file.
 */
public final class InMemoryMappingFile {
     private final String _resourceName;
     private final byte[] _mappingFile;
     private static final AtomicLong _counter = new AtomicLong(0);

     /**
      * An in memory mapping file.
      *
      * @param mappingFile
      *             - A non-null byte[] that represents an orm.xml mapping file.
      */
     public InMemoryMappingFile(byte[] mappingFile) {
          _mappingFile = mappingFile;
          _resourceName =
               "mappingfile-" + _counter.incrementAndGet() + "-" + String.valueOf(Arrays.hashCode(_mappingFile));
     }

     /**
      * An in memory mapping file for records as entities in Jakarta Data.
      *
      * @param mappingFile
      *             - A non-null byte[] that represents an orm.xml mapping file.
      * @param resourceName
      *             - A non-null unique file name that does not begin with "mappingfile-", which is reserved for the other constructor.
      */
     public InMemoryMappingFile(byte[] file, String resourceName) {
         _mappingFile = file;
         _resourceName = resourceName;
     }

     @Trivial
     public String getName() {
          return _resourceName;
     }

     @Trivial
     public byte[] getMappingFile() {
          return _mappingFile;
     }

}
