/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.wsspi.anno.targets.cache;

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

    int WRITE_THREADS_DEFAULT = 8;
    String WRITE_THREADS_PROPERTY_NAME = "anno.cache.writeThreads";

    int getWriteThreads();
    void setWriteThreads(int writeThreads);
}
