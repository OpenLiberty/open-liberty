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
      * @param resourceName
      *             - A non-null unique file name.
      * @param mappingFile
      *             - A non-null byte[] that represents an orm.xml mapping file.
      */
     public InMemoryMappingFile(byte[] mappingFile) {
          _mappingFile = mappingFile;
          _resourceName =
               "mappingfile-" + _counter.incrementAndGet() + "-" + String.valueOf(Arrays.hashCode(_mappingFile));
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
