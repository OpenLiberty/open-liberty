/*******************************************************************************
 * Copyright (c) 2011,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.fat_bvt.servlet.filesystem;

/**
 * <p>Bundle test file set.</p>
 */
public class BundleFileSystem {
    public static FileSystem TESTDATA =
        FileSystem.root( null, null, null,
                 FileSystem.DOES_NOT_HAVE_DATA, null, 0,
                 null,
                 1, new String[] { "bundleentry://#/" },

                 FileSystem.dir( "d", "/d",
                         "bundleentry://#/d/", null,
                         1, new String[] { "bundleentry://#/d/" },
                         FileSystem.File( "d.txt", "/d/d.txt",
                                  FileSystem.DOES_HAVE_DATA, "File in fragment", 16,
                                  "bundleentry://#/d/d.txt", null ) ),

                 FileSystem.dir( "b", "/b",
                         "bundleentry://#/b/", null,
                         1, new String[] { "bundleentry://#/b/" },

                         FileSystem.dir( "bb", "/b/bb",
                                 "bundleentry://#/b/bb/", null,
                                 1, new String[] { "bundleentry://#/b/bb/" },

                                 FileSystem.root( "a.jar", "/b/bb/a.jar", "bundleentry://#/b/bb/a.jar",
                                          FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                          null,
                                          1, new String[] { "jar:file:#/b/bb/a.jar!/" },

                                          FileSystem.dir( "aa", "/aa",
                                                  "jar:file:#/b/bb/a.jar!/aa", null,
                                                  1, new String[] { "jar:file:#/b/bb/a.jar!/aa/" },

                                                  FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                           "jar:file:#/b/bb/a.jar!/aa/aa.txt", null ) ),

                                          FileSystem.dir( "ab", "/ab",
                                                  "jar:file:#/b/bb/a.jar!/ab", null,
                                                  1, new String[] { "jar:file:#/b/bb/a.jar!/ab/" },

                                                  FileSystem.dir( "aba", "/ab/aba",
                                                          "jar:file:#/b/bb/a.jar!/ab/aba", null,
                                                          1, new String[] { "jar:file:#/b/bb/a.jar!/ab/aba/" },

                                                          FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                              FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                       FileSystem.DOES_HAVE_DATA, "", 0, 
                                                       "jar:file:#/b/bb/a.jar!/ab/ab.txt", null ) ),

                                          FileSystem.File( "a.txt", "/a.txt",
                                                   FileSystem.DOES_HAVE_DATA, "", 0, 
                                                   "jar:file:#/b/bb/a.jar!/a.txt", null ) ,

                                          FileSystem.dir( "META-INF", "/META-INF",
                                                  "jar:file:#/b/bb/a.jar!/META-INF", 
                                                  null, 
                                                  1,  new String[] { "jar:file:#/b/bb/a.jar!/META-INF/" },

                                                  FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                           FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                           "jar:file:#/b/bb/a.jar!/META-INF/MANIFEST.MF",  null ) ) ) ),

                         FileSystem.dir( "ba", "/b/ba",
                                 "bundleentry://#/b/ba/", null,
                                 1, new String[] { "bundleentry://#/b/ba/" },

                                 FileSystem.dir( "baa", "/b/ba/baa",
                                         "bundleentry://#/b/ba/baa/", null,
                                         1, new String[] { "bundleentry://#/b/ba/baa/" },

                                         FileSystem.File( "baa1.txt", "/b/ba/baa/baa1.txt",
                                                  FileSystem.DOES_HAVE_DATA, "minion", 6,
                                                  "bundleentry://#/b/ba/baa/baa1.txt", null ),

                                         FileSystem.File( "baa2.txt", "/b/ba/baa/baa2.txt",
                                                  FileSystem.DOES_HAVE_DATA, "chain", 5,
                                                  "bundleentry://#/b/ba/baa/baa2.txt",null ) ) ) ) ,

                 FileSystem.dir( "META-INF", "/META-INF",
                         "bundleentry://#/META-INF/", null,
                         1, new String[] { "bundleentry://#/META-INF/" },

                         FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                  FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nBundle-RequiredExecutionEnvironment: Java", 192, 
                                  "bundleentry://#/META-INF/MANIFEST.MF", null ) ),

                 FileSystem.dir( "c", "/c",
                         "bundleentry://#/c/", null,
                         1, new String[] { "bundleentry://#/c/" },
                         FileSystem.root( "a.jar", "/c/a.jar", "bundleentry://#/c/a.jar",
                                  FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                  null,
                                  1, new String[] { "jar:file:#/c/a.jar!/" },

                                  FileSystem.dir( "aa", "/aa",
                                          "jar:file:#/c/a.jar!/aa", null,
                                          1, new String[] { "jar:file:#/c/a.jar!/aa/" },

                                          FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                   FileSystem.DOES_HAVE_DATA, "", 0, 
                                                   "jar:file:#/c/a.jar!/aa/aa.txt", null ) ),

                                  FileSystem.dir( "ab", "/ab",
                                          "jar:file:#/c/a.jar!/ab", null,
                                          1, new String[] { "jar:file:#/c/a.jar!/ab/" },

                                          FileSystem.dir( "aba", "/ab/aba",
                                                  "jar:file:#/c/a.jar!/ab/aba", null,
                                                  1, new String[] { "jar:file:#/c/a.jar!/ab/aba/" },
                                                  FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0, 
                                                           "jar:file:#/c/a.jar!/ab/aba/aba.txt",                                                            null ) ),

                                          FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                   FileSystem.DOES_HAVE_DATA, "", 0, 
                                                   "jar:file:#/c/a.jar!/ab/ab.txt", null ) ),

                                  FileSystem.File( "a.txt", "/a.txt",
                                           FileSystem.DOES_HAVE_DATA, "", 0, 
                                           "jar:file:#/c/a.jar!/a.txt", null ),

                                  FileSystem.dir( "META-INF", "/META-INF",
                                          "jar:file:#/c/a.jar!/META-INF", null,
                                          1, new String[] { "jar:file:#/c/a.jar!/META-INF/" },

                                          FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                   FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                   "jar:file:#/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),

                         FileSystem.root( "b.jar", "/c/b.jar", "bundleentry://#/c/b.jar",
                                  FileSystem.DOES_NOT_HAVE_DATA, null, 1227,
                                  null,
                                  1, new String[] { "jar:file:#/c/b.jar!/" },

                                  FileSystem.dir( "bb", "/bb",
                                          "jar:file:#/c/b.jar!/bb", null,
                                          1, new String[] { "jar:file:#/c/b.jar!/bb/" },

                                          FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/c/b.jar!/bb/a.jar",
                                                   FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                   null,
                                                   1, new String[] { "jar:file:#/c/.cache/b.jar/bb/a.jar!/" },

                                                   FileSystem.dir( "aa", "/aa",
                                                           "jar:file:#/c/.cache/b.jar/bb/a.jar!/aa", null,
                                                           1, new String[] { "jar:file:#/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                           FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                    "jar:file:#/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                   FileSystem.dir( "ab", "/ab",
                                                           "jar:file:#/c/.cache/b.jar/bb/a.jar!/ab", null,
                                                           1, new String[] { "jar:file:#/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                           FileSystem.dir( "aba", "/ab/aba",
                                                                   "jar:file:#/c/.cache/b.jar/bb/a.jar!/ab/aba", null,
                                                                   1, new String[] { "jar:file:#/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                   FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                            "jar:file:#/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ) ,

                                                           FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                    "jar:file:#/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                   FileSystem.File( "a.txt", "/a.txt",
                                                            FileSystem.DOES_HAVE_DATA, "", 0, 
                                                            "jar:file:#/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                   FileSystem.dir( "META-INF", "/META-INF",
                                                           "jar:file:#/c/.cache/b.jar/bb/a.jar!/META-INF", null,
                                                           1, new String[] { "jar:file:#/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                           FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                    FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                                    "jar:file:#/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF",  null ) ) ) ),

                                  FileSystem.dir( "META-INF", "/META-INF",
                                          "jar:file:#/c/b.jar!/META-INF", null,
                                          1, new String[] { "jar:file:#/c/b.jar!/META-INF/" },

                                          FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                   FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                   "jar:file:#/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                  FileSystem.dir( "ba", "/ba",
                                          "jar:file:#/c/b.jar!/ba", null,
                                          1, new String[] { "jar:file:#/c/b.jar!/ba/" },

                                          FileSystem.dir( "baa", "/ba/baa",
                                                  "jar:file:#/c/b.jar!/ba/baa", null,
                                                  1, new String[] { "jar:file:#/c/b.jar!/ba/baa/" },

                                                  FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0, 
                                                           "jar:file:#/c/b.jar!/ba/baa/baa1.txt", null ),

                                                  FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0, 
                                                           "jar:file:#/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ),

                 FileSystem.dir( "a", "/a",
                         "bundleentry://#/a/", null,
                         1, new String[] { "bundleentry://#/a/" },

                         FileSystem.File( "a.txt", "/a/a.txt",
                                  FileSystem.DOES_HAVE_DATA, "", 0,
                                  "bundleentry://#/a/a.txt", null ),

                         FileSystem.dir( "aa", "/a/aa",
                                 "bundleentry://#/a/aa/", null,
                                 1, new String[] { "bundleentry://#/a/aa/" },

                                 FileSystem.File( "aa.txt", "/a/aa/aa.txt",
                                          FileSystem.DOES_HAVE_DATA, "wibble", 6,
                                          "bundleentry://#/a/aa/aa.txt", null ) ),

                         FileSystem.dir( "ab", "/a/ab",
                                 "bundleentry://#/a/ab/", null,
                                 1, new String[] { "bundleentry://#/a/ab/" },

                                 FileSystem.dir( "aba", "/a/ab/aba",
                                         "bundleentry://#/a/ab/aba/", null,
                                         1, new String[] { "bundleentry://#/a/ab/aba/" },

                                         FileSystem.File( "aba.txt", "/a/ab/aba/aba.txt",
                                                  FileSystem.DOES_HAVE_DATA, "cheese", 6,
                                                  "bundleentry://#/a/ab/aba/aba.txt", null ) ),

                                 FileSystem.File( "ab.txt", "/a/ab/ab.txt",
                                          FileSystem.DOES_HAVE_DATA, "fish", 4,
                                          "bundleentry://#/a/ab/ab.txt", null ) ) ) );
}
