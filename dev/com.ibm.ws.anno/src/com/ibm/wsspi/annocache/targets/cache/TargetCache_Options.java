/*******************************************************************************
 * Copyright (c) 2019
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.annocache.targets.cache;

// scanOptions="[Enable]/Disable,
//              [NoShareStrings]/ShareStrings"
//
// selectArchives  ="*"
// rejectArchives  =""
// selectClasses   ="*"
// rejectClasses   =""
// selectAnnotation="*"
// rejectAnnotation=""
//
// cacheOptions="[Enable]/Disable,
//               [TestForValid]/AlwaysValid,
//               [Save]/NoSave,
//               [DiscardDetail]/RetainDetail
//               [ExtraThreads]/NoExtraThreads"
//
// Select and reject are provided as distinct options because
// rejection is hard to do using regular expressions.
// See: http://www.perlmonks.org/?node_id=588315#588368

/**
 * Class scanning options.  These are set as system properties and
 * are used to control the behavior of the class canner.
 *
 * "anno.cache.noExtraThreads" [ true ]
 * 
 * Property used to enable/disable multi-threaded class scanning.
 * Defaults to true, meaning, by default no extra threads are used.  This
 * property is independent of on-disk caching.
 * 
 * "anno.cache.disabled" [ true ]
 * 
 * Property used to enable/disable on-disk caching.  Defaults true,
 * meaning, by default, the on-disk cache is not used.  When set to false
 * to enable on-disk caching, a cache directory is often set using the
 * "anno.cache.dir" property.
 * 
 * "anno.cache.dir" [ "." ]
 * 
 * Property used to specify the on-disk location of the annotation cache.
 * Defaults to the JVM current directory, which is usually the directory
 * from which the JVM was launched. Meaningful only when on-disk caching
 * is enabled.
 * 
 * "anno.cache.alwaysValid" [ false ]
 * 
 * Property used to prevent tests which determine if on-disk data has
 * become invalid and must be regenerated. Meaningful only when on-disk
 * caching is enabled. Used to optimize cases where no changes are being
 * made to classes which are inputs to the class scanner, with the
 * consequence that on-disk data may be relied upon to remain valid.
 * 
 * "anno.cache.readOnly" [ false ]
 * 
 * Property used to disable writes of new cache data. Meaningful only when
 * on-disk caching is enabled. Used to enable reads of pre-cached data, while
 * preventing writes of new scan results.
 */
public interface TargetCache_Options {
    boolean DISABLED_DEFAULT = false;
    String DISABLED_PROPERTY_NAME = "anno.cache.disabled";

    boolean getDisabled();
    void setDisabled(boolean disabled);

    String CACHE_NAME_DEFAULT = TargetCache_ExternalConstants.CACHE_NAME_DEFAULT;

    String DIR_DEFAULT = "./" + CACHE_NAME_DEFAULT;
    String DIR_PROPERTY_NAME = "anno.cache.dir";

    String getDir();
    void setDir(String dir);

    boolean READ_ONLY_DEFAULT = false;
    String READ_ONLY_PROPERTY_NAME = "anno.cache.readOnly";

    boolean getReadOnly();
    void setReadOnly(boolean readOnly);

    boolean ALWAYS_VALID_DEFAULT = false;
    String ALWAYS_VALID_PROPERTY_NAME = "anno.cache.alwaysValid";

    boolean getAlwaysValid();
    void setAlwaysValid(boolean alwaysValidate);

    // Validation (comparison of prior results with current results)
    // is not currently implemented.
    //
    // boolean VALIDATE_DEFAULT = false;
    // String VALIDATE_PROPERTY_NAME = "anno.cache.validate";

    // boolean getValidate();
    // void setValidate(boolean validate);

    int WRITE_THREADS_UNBOUNDED = -1;
    int WRITE_THREADS_MAX = 64;

    int WRITE_THREADS_DEFAULT = 1;
    String WRITE_THREADS_PROPERTY_NAME = "anno.cache.writeThreads";

    int getWriteThreads();
    void setWriteThreads(int writeThreads);

    // The minimum number of classes for which to write component annotations data.

    int WRITE_LIMIT_DEFAULT = 16;
    String WRITE_LIMIT_PROPERTY_NAME = "anno.cache.writeLimit";

    int getWriteLimit();
    void setWriteLimit(int writeLimit);

    // Whether to manage container data using jandex indexes.
    //
    // Jandex format is meaningful only if caching is enabled
    // and jandex writes are disabled.
    //
    // Using jandex indexes for container data has these implications:
    //
    // 1) When generating component data, if a jandex index is not available,
    //    data is generated first as a jandex index then is processed from
    //    that data.  The jandex index is retained as it will be saved
    //    to the cache.
    //
    // 2) When saving component data, the jandex index is written instead
    //    of writing using the internal format.
    //
    // 3) When reading component data, the data must be saved as a jandex
    //    index.

    boolean USE_JANDEX_FORMAT_DEFAULT = false;
    String USE_JANDEX_FORMAT_PROPERTY_NAME = "anno.cache.jandexFormat";

    boolean getUseJandexFormat();
    void setUseJandexFormat(boolean useJandexFormat);

    //

    boolean USE_BINARY_FORMAT_DEFAULT = true;
    String USE_BINARY_FORMAT_PROPERTY_NAME = "anno.cache.binaryFormat";

    boolean getUseBinaryFormat();
    void setUseBinaryFormat(boolean useBinaryFormat);

    //

    boolean LOG_QUERIES_DEFAULT = false;
    String LOG_QUERIES_PROPERTY_NAME = "anno.cache.logQueries";

    boolean getLogQueries();
    void setLogQueries(boolean logQueries);
}
