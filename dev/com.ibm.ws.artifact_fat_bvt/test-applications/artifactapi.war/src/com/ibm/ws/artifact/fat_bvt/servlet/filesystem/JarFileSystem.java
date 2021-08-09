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
 * <p>Zip archive test file set.</p>
 */
public class JarFileSystem {
    public static FileSystem TESTDATA =
        FileSystem.root( null, null, null,
                 FileSystem.DOES_NOT_HAVE_DATA, null, 0, 
                 "#\\TEST.JAR", 
                 1, new String[] { "jar:file:#/TEST.JAR!/" },

                FileSystem.dir( "a", "/a",
                        "jar:file:#/TEST.JAR!/a", null,
                        1, new String[] { "jar:file:#/TEST.JAR!/a/" },

                        FileSystem.dir( "aa", "/a/aa",
                                "jar:file:#/TEST.JAR!/a/aa", null,
                                1, new String[] { "jar:file:#/TEST.JAR!/a/aa/" },

                                FileSystem.File( "aa.txt", "/a/aa/aa.txt",
                                         FileSystem.DOES_HAVE_DATA, "", 0,
                                         "jar:file:#/TEST.JAR!/a/aa/aa.txt", null ) ),

                        FileSystem.dir( "ab", "/a/ab",
                                "jar:file:#/TEST.JAR!/a/ab", null,
                                1, new String[] { "jar:file:#/TEST.JAR!/a/ab/" },

                                FileSystem.dir( "aba", "/a/ab/aba",
                                        "jar:file:#/TEST.JAR!/a/ab/aba", null,
                                        1, new String[] { "jar:file:#/TEST.JAR!/a/ab/aba/" },

                                        FileSystem.File( "aba.txt", "/a/ab/aba/aba.txt",
                                                 FileSystem.DOES_HAVE_DATA, "", 0, 
                                                 "jar:file:#/TEST.JAR!/a/ab/aba/aba.txt", null ) ),

                                FileSystem.File( "ab.txt", "/a/ab/ab.txt",
                                         FileSystem.DOES_HAVE_DATA, "", 0, 
                                         "jar:file:#/TEST.JAR!/a/ab/ab.txt", null ) ),

                        FileSystem.File( "a.txt", "/a/a.txt",
                                 FileSystem.DOES_HAVE_DATA, "", 0, 
                                 "jar:file:#/TEST.JAR!/a/a.txt", null ) ),

                FileSystem.dir( "b", "/b",
                        "jar:file:#/TEST.JAR!/b", null, 
                        1, new String[] { "jar:file:#/TEST.JAR!/b/" },

                        FileSystem.dir( "bb", "/b/bb",
                                "jar:file:#/TEST.JAR!/b/bb", null, 
                                1, new String[] { "jar:file:#/TEST.JAR!/b/bb/" },

                                FileSystem.root( "a.jar", "/b/bb/a.jar", "jar:file:#/TEST.JAR!/b/bb/a.jar",
                                         FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                         null, 
                                         1, new String[] { "jar:file:#/cacheDir/.cache/b/bb/a.jar!/" },

                                         FileSystem.dir( "aa", "/aa",
                                                 "jar:file:#/cacheDir/.cache/b/bb/a.jar!/aa", null, 
                                                 1, new String[] { "jar:file:#/cacheDir/.cache/b/bb/a.jar!/aa/" },

                                                 FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                          FileSystem.DOES_HAVE_DATA, "", 0, 
                                                          "jar:file:#/cacheDir/.cache/b/bb/a.jar!/aa/aa.txt", null ) ),

                                         FileSystem.dir( "ab", "/ab",
                                                 "jar:file:#/cacheDir/.cache/b/bb/a.jar!/ab", null, 
                                                 1, new String[] { "jar:file:#/cacheDir/.cache/b/bb/a.jar!/ab/" },

                                                 FileSystem.dir( "aba", "/ab/aba",
                                                         "jar:file:#/cacheDir/.cache/b/bb/a.jar!/ab/aba", null, 
                                                         1, new String[] { "jar:file:#/cacheDir/.cache/b/bb/a.jar!/ab/aba/" },

                                                         FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                  FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                  "jar:file:#/cacheDir/.cache/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                 FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                          FileSystem.DOES_HAVE_DATA, "", 0, 
                                                          "jar:file:#/cacheDir/.cache/b/bb/a.jar!/ab/ab.txt", null ) ),

                                         FileSystem.File( "a.txt", "/a.txt",
                                                  FileSystem.DOES_HAVE_DATA, "", 0, 
                                                  "jar:file:#/cacheDir/.cache/b/bb/a.jar!/a.txt", null ),

                                         FileSystem.dir( "META-INF", "/META-INF",
                                                 "jar:file:#/cacheDir/.cache/b/bb/a.jar!/META-INF", null, 
                                                 1, new String[] { "jar:file:#/cacheDir/.cache/b/bb/a.jar!/META-INF/" },

                                                 FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                          FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                          "jar:file:#/cacheDir/.cache/b/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                        FileSystem.dir( "ba", "/b/ba",
                                "jar:file:#/TEST.JAR!/b/ba", null,
                                1, new String[] { "jar:file:#/TEST.JAR!/b/ba/" },

                                FileSystem.dir( "baa", "/b/ba/baa",
                                        "jar:file:#/TEST.JAR!/b/ba/baa", null,
                                        1, new String[] { "jar:file:#/TEST.JAR!/b/ba/baa/" },

                                        FileSystem.File( "baa1.txt", "/b/ba/baa/baa1.txt",
                                                 FileSystem.DOES_HAVE_DATA, "", 0,
                                                 "jar:file:#/TEST.JAR!/b/ba/baa/baa1.txt", null ),

                                        FileSystem.File( "baa2.txt", "/b/ba/baa/baa2.txt",
                                                 FileSystem.DOES_HAVE_DATA, "", 0,
                                                 "jar:file:#/TEST.JAR!/b/ba/baa/baa2.txt", null ) ) ) ),

                FileSystem.dir( "c", "/c",
                        "jar:file:#/TEST.JAR!/c", null, 
                        1, new String[] { "jar:file:#/TEST.JAR!/c/" },

                        FileSystem.root( "a.jar", "/c/a.jar", "jar:file:#/TEST.JAR!/c/a.jar",
                                 FileSystem.DOES_NOT_HAVE_DATA, null, 967, 
                                 null, 
                                 1, new String[] { "jar:file:#/cacheDir/.cache/c/a.jar!/" },

                                 FileSystem.dir( "aa", "/aa",
                                         "jar:file:#/cacheDir/.cache/c/a.jar!/aa", null, 
                                         1, new String[] { "jar:file:#/cacheDir/.cache/c/a.jar!/aa/" },

                                         FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                  FileSystem.DOES_HAVE_DATA, "", 0, 
                                                  "jar:file:#/cacheDir/.cache/c/a.jar!/aa/aa.txt", null ) ),

                                 FileSystem.dir( "ab", "/ab",
                                         "jar:file:#/cacheDir/.cache/c/a.jar!/ab", null, 
                                         1, new String[] { "jar:file:#/cacheDir/.cache/c/a.jar!/ab/" },

                                         FileSystem.dir( "aba", "/ab/aba",
                                                 "jar:file:#/cacheDir/.cache/c/a.jar!/ab/aba", null, 
                                                 1, new String[] { "jar:file:#/cacheDir/.cache/c/a.jar!/ab/aba/" },

                                                 FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                          FileSystem.DOES_HAVE_DATA, "", 0, 
                                                          "jar:file:#/cacheDir/.cache/c/a.jar!/ab/aba/aba.txt", null ) ),

                                         FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                  FileSystem.DOES_HAVE_DATA, "", 0, 
                                                  "jar:file:#/cacheDir/.cache/c/a.jar!/ab/ab.txt", null ) ),

                                 FileSystem.File( "a.txt", "/a.txt",
                                          FileSystem.DOES_HAVE_DATA, "", 0, 
                                          "jar:file:#/cacheDir/.cache/c/a.jar!/a.txt", null ),

                                 FileSystem.dir( "META-INF", "/META-INF",
                                         "jar:file:#/cacheDir/.cache/c/a.jar!/META-INF", null, 
                                         1, new String[] { "jar:file:#/cacheDir/.cache/c/a.jar!/META-INF/" },

                                         FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                  FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                  "jar:file:#/cacheDir/.cache/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),

                        FileSystem.root( "b.jar", "/c/b.jar", "jar:file:#/TEST.JAR!/c/b.jar",
                                 FileSystem.DOES_NOT_HAVE_DATA, null, 1227, 
                                 null, 
                                 1, new String[] { "jar:file:#/cacheDir/.cache/c/b.jar!/" },

                                 FileSystem.dir( "bb", "/bb",
                                         "jar:file:#/cacheDir/.cache/c/b.jar!/bb", null, 
                                         1, new String[] { "jar:file:#/cacheDir/.cache/c/b.jar!/bb/" },

                                         FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/cacheDir/.cache/c/b.jar!/bb/a.jar",
                                                  FileSystem.DOES_NOT_HAVE_DATA, null, 967, 
                                                  null, 
                                                  1, new String[] { "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/" },

                                                  FileSystem.dir( "aa", "/aa",
                                                          "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/aa", null, 
                                                          1, new String[] { "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                          FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                   "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                  FileSystem.dir( "ab", "/ab",
                                                          "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/ab", null, 
                                                          1, new String[] { "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                          FileSystem.dir( "aba", "/ab/aba",
                                                                  "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/ab/aba",                                                                         null, 
                                                                  1, new String[] { "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                  FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                           FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                           "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                          FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                   "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                  FileSystem.File( "a.txt", "/a.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0, 
                                                           "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                  FileSystem.dir( "META-INF", "/META-INF",
                                                          "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/META-INF", null, 
                                                          1, new String[] { "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                          FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                   FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                                   "jar:file:#/cacheDir/.cache/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                                 FileSystem.dir( "META-INF", "/META-INF",
                                         "jar:file:#/cacheDir/.cache/c/b.jar!/META-INF", null, 
                                         1, new String[] { "jar:file:#/cacheDir/.cache/c/b.jar!/META-INF/" },

                                         FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                  FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                                  "jar:file:#/cacheDir/.cache/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                           FileSystem.dir( "ba", "/ba",
                                                   "jar:file:#/cacheDir/.cache/c/b.jar!/ba", null, 
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/c/b.jar!/ba/" },

                                                   FileSystem.dir( "baa", "/ba/baa",
                                                           "jar:file:#/cacheDir/.cache/c/b.jar!/ba/baa", null, 
                                                           1, new String[] { "jar:file:#/cacheDir/.cache/c/b.jar!/ba/baa/" },

                                                           FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                    "jar:file:#/cacheDir/.cache/c/b.jar!/ba/baa/baa1.txt", null ),

                                                           FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0, 
                                                                    "jar:file:#/cacheDir/.cache/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ),

                FileSystem.dir( "META-INF", "/META-INF",
                        "jar:file:#/TEST.JAR!/META-INF", null,
                        1, new String[] { "jar:file:#/TEST.JAR!/META-INF/" },

                        FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                 FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62, 
                                 "jar:file:#/TEST.JAR!/META-INF/MANIFEST.MF", null ) ) );
}
