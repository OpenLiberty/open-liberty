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
 * <p>Loose configuration test file set.</p>
 */
public class LooseFileSystem {
    public static FileSystem TESTDATA =
        FileSystem.root( null, null, null,
                 FileSystem.DOES_NOT_HAVE_DATA, null, 0,
                 null,
                 0, new String[] {},

                 FileSystem.root( "TEST.jar", "/TEST.jar", "file:#/xmlContentTestData/a/",
                          FileSystem.DOES_NOT_HAVE_DATA, null, 0,
                          "#\\xmlContentTestData\\a",
                          3, new String[] {
                              "file:#/xmlContentTestData/a/",
                              "file:#/TESTDATA/a/",
                              "file:#/TESTDATA/a/aa/" },

                          FileSystem.File( "a.txt", "/a.txt",
                                   FileSystem.DOES_HAVE_DATA, "xml content is present.", 23,
                                   "file:#/xmlContentTestData/a/a.txt", "#\\xmlContentTestData\\a\\a.txt" ),

                          FileSystem.dir( "aa", "/aa",
                                  "file:#/TESTDATA/a/aa/", "#\\TESTDATA\\a\\aa",
                                  1, new String[] { "file:#/TESTDATA/a/aa/" },

                                  FileSystem.File( "aa.txt", "/aa/aa.txt",
                                           FileSystem.DOES_HAVE_DATA, "wibble", 6,
                                           "file:#/TESTDATA/a/aa/aa.txt", "#\\TESTDATA\\a\\aa\\aa.txt" ) ),

                          FileSystem.dir( "ab", "/ab",
                                  "file:#/TESTDATA/a/ab/", "#\\TESTDATA\\a\\ab",
                                  1, new String[] { "file:#/TESTDATA/a/ab/" },

                                  FileSystem.File( "ab.txt", "/ab/ab.txt",
                                           FileSystem.DOES_HAVE_DATA, "fish", 4,
                                           "file:#/TESTDATA/a/ab/ab.txt", "#\\TESTDATA\\a\\ab\\ab.txt" ),

                                  FileSystem.dir( "aba", "/ab/aba",
                                          "file:#/TESTDATA/a/ab/aba/", "#\\TESTDATA\\a\\ab\\aba",
                                          1, new String[] { "file:#/TESTDATA/a/ab/aba/" },

                                          FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                   FileSystem.DOES_HAVE_DATA, "cheese", 6,
                                                   "file:#/TESTDATA/a/ab/aba/aba.txt", "#\\TESTDATA\\a\\ab\\aba\\aba.txt" ) ) ),

                          FileSystem.File( "aa.txt", "/aa.txt",
                                   FileSystem.DOES_HAVE_DATA, "wibble", 6, "file:#/TESTDATA/a/aa/aa.txt",
                                   "#\\TESTDATA\\a\\aa\\aa.txt") ),

                 FileSystem.root( "webApp.war", "/webApp.war", "file:#/TESTDATA/",
                          FileSystem.DOES_NOT_HAVE_DATA, null, 0,
                          "#\\TESTDATA",
                          1, new String[] { "file:#/TESTDATA/" },

                          FileSystem.dir( "b", "/b",
                                  "file:#/TESTDATA/b/", "#\\TESTDATA\\b",
                                  1, new String[] { "file:#/TESTDATA/b/" },

                                  FileSystem.dir( "ba", "/b/ba",
                                          "file:#/TESTDATA/b/ba/", "#\\TESTDATA\\b\\ba",
                                          1, new String[] { "file:#/TESTDATA/b/ba/" },

                                          FileSystem.dir( "baa", "/b/ba/baa",
                                                  "file:#/TESTDATA/b/ba/baa/", "#\\TESTDATA\\b\\ba\\baa",
                                                  1, new String[] { "file:#/TESTDATA/b/ba/baa/" },

                                                  FileSystem.File( "baa1.txt", "/b/ba/baa/baa1.txt",
                                                           FileSystem.DOES_HAVE_DATA, "minion", 6,
                                                           "file:#/TESTDATA/b/ba/baa/baa1.txt", "#\\TESTDATA\\b\\ba\\baa\\baa1.txt" ),

                                                  FileSystem.File( "baa2.txt", "/b/ba/baa/baa2.txt",
                                                           FileSystem.DOES_HAVE_DATA, "chain", 5,
                                                           "file:#/TESTDATA/b/ba/baa/baa2.txt", "#\\TESTDATA\\b\\ba\\baa\\baa2.txt" ) ) ),
                                  FileSystem.dir( "bb", "/b/bb",
                                          "file:#/TESTDATA/b/bb/", "#\\TESTDATA\\b\\bb",
                                          1, new String[] { "file:#/TESTDATA/b/bb/" },

                                          FileSystem.root( "a.jar", "/b/bb/a.jar", "file:#/TESTDATA/b/bb/a.jar",
                                                   FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                   "#\\TESTDATA\\b\\bb\\a.jar",
                                                   1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/" },

                                                   FileSystem.dir( "aa", "/aa",
                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/aa", null,
                                                           1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/aa/" },

                                                           FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/aa/aa.txt", null ) ),

                                                   FileSystem.dir( "ab", "/ab",
                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/ab", null,
                                                           1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/" },

                                                           FileSystem.dir( "aba", "/ab/aba",
                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba", null,
                                                                   1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/" },

                                                                   FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                           FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/ab/ab.txt", null ) ),

                                                   FileSystem.File( "a.txt", "/a.txt",
                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                            "jar:file:#/TESTDATA/b/bb/a.jar!/a.txt", null ),

                                                   FileSystem.dir( "META-INF", "/META-INF",
                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF", null,
                                                           1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/" },

                                                           FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                    FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ) ),

                          FileSystem.dir( "c", "/c",
                                  "file:#/TESTDATA/c/", "#\\TESTDATA\\c",
                                  1, new String[] { "file:#/TESTDATA/c/" },

                                  FileSystem.root( "a.jar", "/c/a.jar", "file:#/TESTDATA/c/a.jar",
                                           FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                           "#\\TESTDATA\\c\\a.jar",
                                           1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/" },

                                           FileSystem.dir( "aa", "/aa",
                                                   "jar:file:#/TESTDATA/c/a.jar!/aa", null,
                                                   1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/aa/" },

                                                   FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                            "jar:file:#/TESTDATA/c/a.jar!/aa/aa.txt", null ) ),
                                           FileSystem.dir( "ab", "/ab",
                                                   "jar:file:#/TESTDATA/c/a.jar!/ab", null,
                                                   1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/" },

                                                   FileSystem.dir( "aba", "/ab/aba",
                                                           "jar:file:#/TESTDATA/c/a.jar!/ab/aba", null,
                                                           1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/aba/" },

                                                           FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/TESTDATA/c/a.jar!/ab/aba/aba.txt", null ) ),

                                                   FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                            "jar:file:#/TESTDATA/c/a.jar!/ab/ab.txt", null ) ),

                                           FileSystem.File( "a.txt", "/a.txt",
                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                    "jar:file:#/TESTDATA/c/a.jar!/a.txt", null ),

                                           FileSystem.dir( "META-INF", "/META-INF",
                                                   "jar:file:#/TESTDATA/c/a.jar!/META-INF", null,
                                                   1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/META-INF/" },

                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                            "jar:file:#/TESTDATA/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),

                                  FileSystem.root( "b.jar", "/c/b.jar", "file:#/TESTDATA/c/b.jar",
                                           FileSystem.DOES_NOT_HAVE_DATA, null, 1227,
                                           "#\\TESTDATA\\c\\b.jar",
                                           1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/" },

                                           FileSystem.dir( "bb", "/bb",
                                                   "jar:file:#/TESTDATA/c/b.jar!/bb", null,
                                                   1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/bb/" },

                                                   FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/TESTDATA/c/b.jar!/bb/a.jar",
                                                            FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                            null,
                                                            1, new String[] { "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/" },

                                                            FileSystem.dir( "aa", "/aa",
                                                                    "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/aa", null,
                                                                    1, new String[] { "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                                    FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                             "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                            FileSystem.dir( "ab", "/ab",
                                                                    "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/ab", null,
                                                                    1, new String[] { "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                                    FileSystem.dir( "aba", "/ab/aba",
                                                                            "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/ab/aba", null,
                                                                            1, new String[] { "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                            FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                     "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                    FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                             "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                            FileSystem.File( "a.txt", "/a.txt",
                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                     "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                            FileSystem.dir( "META-INF", "/META-INF",
                                                                    "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/META-INF", null,
                                                                    1, new String[] { "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                                    FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                             FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                             "jar:file:#/cacheDir/webApp.war/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                                           FileSystem.dir( "META-INF", "/META-INF",
                                                   "jar:file:#/TESTDATA/c/b.jar!/META-INF", null,
                                                   1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/META-INF/" },

                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                            "jar:file:#/TESTDATA/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                           FileSystem.dir( "ba", "/ba",
                                                   "jar:file:#/TESTDATA/c/b.jar!/ba", null,
                                                   1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/" },

                                                   FileSystem.dir( "baa", "/ba/baa",
                                                           "jar:file:#/TESTDATA/c/b.jar!/ba/baa", null,
                                                           1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/baa/" },

                                                           FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa1.txt", null ),

                                                           FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ),

                          FileSystem.dir( "WEB-INF", "/WEB-INF",
                                  "null", null,
                                  0, new String[] {},

                                  FileSystem.dir( "classes", "/WEB-INF/classes",
                                          "null", null,
                                          1, new String[] { "file:#/TESTDATA/" },

                                          FileSystem.dir( "a", "/WEB-INF/classes/a",
                                                  "file:#/TESTDATA/a/", "#\\TESTDATA\\a",
                                                  1, new String[] { "file:#/TESTDATA/a/" },

                                                  FileSystem.dir( "aa", "/WEB-INF/classes/a/aa",
                                                          "file:#/TESTDATA/a/aa/", "#\\TESTDATA\\a\\aa",
                                                          1, new String[] { "file:#/TESTDATA/a/aa/" }),

                                                  FileSystem.dir( "ab", "/WEB-INF/classes/a/ab",
                                                          "file:#/TESTDATA/a/ab/", "#\\TESTDATA\\a\\ab",
                                                          1, new String[] { "file:#/TESTDATA/a/ab/" },

                                                          FileSystem.dir( "aba", "/WEB-INF/classes/a/ab/aba",
                                                                  "file:#/TESTDATA/a/ab/aba/", "#\\TESTDATA\\a\\ab\\aba",
                                                                  1, new String[] { "file:#/TESTDATA/a/ab/aba/" }) ) ),

                                          FileSystem.dir( "b", "/WEB-INF/classes/b",
                                                  "file:#/TESTDATA/b/", "#\\TESTDATA\\b",
                                                  1, new String[] { "file:#/TESTDATA/b/" },

                                                  FileSystem.dir( "ba", "/WEB-INF/classes/b/ba",
                                                          "file:#/TESTDATA/b/ba/", "#\\TESTDATA\\b\\ba",
                                                          1, new String[] { "file:#/TESTDATA/b/ba/" },

                                                          FileSystem.dir( "baa", "/WEB-INF/classes/b/ba/baa",
                                                                  "file:#/TESTDATA/b/ba/baa/", "#\\TESTDATA\\b\\ba\\baa",
                                                                  1, new String[] { "file:#/TESTDATA/b/ba/baa/" }) ),

                                                  FileSystem.dir( "bb", "/WEB-INF/classes/b/bb",
                                                          "file:#/TESTDATA/b/bb/", "#\\TESTDATA\\b\\bb",
                                                          1, new String[] { "file:#/TESTDATA/b/bb/" },

                                                          FileSystem.root( "a.jar", "/WEB-INF/classes/b/bb/a.jar", "file:#/TESTDATA/b/bb/a.jar",
                                                                   FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                                   "#\\TESTDATA\\b\\bb\\a.jar",
                                                                   1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/" },

                                                                   FileSystem.dir( "aa", "/aa",
                                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/aa", null,
                                                                           1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/aa/" },

                                                                           FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/aa/aa.txt", null ) ),

                                                                   FileSystem.dir( "ab", "/ab",
                                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/ab", null,
                                                                           1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/" },

                                                                           FileSystem.dir( "aba", "/ab/aba",
                                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba", null,
                                                                                   1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/" },

                                                                                   FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                            "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                           FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/ab/ab.txt", null ) ),

                                                                   FileSystem.File( "a.txt", "/a.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/TESTDATA/b/bb/a.jar!/a.txt", null ),

                                                                   FileSystem.dir( "META-INF", "/META-INF",
                                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF", null,
                                                                           1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/" },

                                                                           FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                                    FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ) ),

                                          FileSystem.dir( "c", "/WEB-INF/classes/c",
                                                  "file:#/TESTDATA/c/", "#\\TESTDATA\\c",
                                                  1, new String[] { "file:#/TESTDATA/c/" },

                                                  FileSystem.root( "a.jar", "/WEB-INF/classes/c/a.jar", "file:#/TESTDATA/c/a.jar",
                                                           FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                           "#\\TESTDATA\\c\\a.jar",
                                                           1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/" },

                                                           FileSystem.dir( "aa", "/aa",
                                                                   "jar:file:#/TESTDATA/c/a.jar!/aa", null,
                                                                   1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/aa/" },

                                                                   FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/TESTDATA/c/a.jar!/aa/aa.txt", null ) ),

                                                           FileSystem.dir( "ab", "/ab",
                                                                   "jar:file:#/TESTDATA/c/a.jar!/ab", null,
                                                                   1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/" },

                                                                   FileSystem.dir( "aba", "/ab/aba",
                                                                           "jar:file:#/TESTDATA/c/a.jar!/ab/aba", null,
                                                                           1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/aba/" },

                                                                           FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                    "jar:file:#/TESTDATA/c/a.jar!/ab/aba/aba.txt", null ) ),

                                                                   FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/TESTDATA/c/a.jar!/ab/ab.txt", null ) ),

                                                           FileSystem.File( "a.txt", "/a.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/TESTDATA/c/a.jar!/a.txt", null ),

                                                           FileSystem.dir( "META-INF", "/META-INF",
                                                                   "jar:file:#/TESTDATA/c/a.jar!/META-INF", null,
                                                                   1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/META-INF/" },

                                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                            "jar:file:#/TESTDATA/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),

                                                  FileSystem.root( "b.jar", "/WEB-INF/classes/c/b.jar", "file:#/TESTDATA/c/b.jar",
                                                           FileSystem.DOES_NOT_HAVE_DATA, null, 1227,
                                                           "#\\TESTDATA\\c\\b.jar",
                                                           1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/" },

                                                           FileSystem.dir( "bb", "/bb",
                                                                   "jar:file:#/TESTDATA/c/b.jar!/bb", null,
                                                                   1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/bb/" },

                                                                   FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/TESTDATA/c/b.jar!/bb/a.jar",
                                                                            FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                                            null,
                                                                            1, new String[] { "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/" },

                                                                            FileSystem.dir( "aa", "/aa",
                                                                                    "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/aa", null,
                                                                                    1, new String[] { "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                                                    FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                             "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                                            FileSystem.dir( "ab", "/ab",
                                                                                    "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/ab", null,
                                                                                    1, new String[] { "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                                                    FileSystem.dir( "aba", "/ab/aba",
                                                                                            "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/ab/aba", null,
                                                                                            1, new String[] { "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                                            FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                                     "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                                    FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                             "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                                            FileSystem.File( "a.txt", "/a.txt",
                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                     "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                                            FileSystem.dir( "META-INF", "/META-INF",
                                                                                    "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/META-INF", null,
                                                                                    1, new String[] { "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                                                    FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                                             FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                                             "jar:file:#/cacheDir/webApp.war/WEB-INF/classes/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                                                           FileSystem.dir( "META-INF", "/META-INF",
                                                                   "jar:file:#/TESTDATA/c/b.jar!/META-INF", null,
                                                                   1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/META-INF/" },

                                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                            "jar:file:#/TESTDATA/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                                           FileSystem.dir( "ba", "/ba",
                                                                   "jar:file:#/TESTDATA/c/b.jar!/ba", null,
                                                                   1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/" },

                                                                   FileSystem.dir( "baa", "/ba/baa",
                                                                           "jar:file:#/TESTDATA/c/b.jar!/ba/baa", null,
                                                                           1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/baa/" },

                                                                           FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                    "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa1.txt", null ),

                                                                           FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                    "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ) ),

                                  FileSystem.dir( "lib", "/WEB-INF/lib",
                                          "null", null,
                                          0, new String[] {},

                                          FileSystem.root( "myutility.jar", "/WEB-INF/lib/myutility.jar", "file:#/TESTDATA/",
                                                   FileSystem.DOES_NOT_HAVE_DATA, null, 0,
                                                   "#\\TESTDATA",
                                                   1, new String[] { "file:#/TESTDATA/" },

                                                   FileSystem.dir( "a", "/a",
                                                           "file:#/TESTDATA/a/", "#\\TESTDATA\\a",
                                                           1, new String[] { "file:#/TESTDATA/a/" },

                                                           FileSystem.dir( "aa", "/a/aa",
                                                                   "file:#/TESTDATA/a/aa/", "#\\TESTDATA\\a\\aa",
                                                                   1, new String[] { "file:#/TESTDATA/a/aa/" }),

                                                           FileSystem.dir( "ab", "/a/ab",
                                                                   "file:#/TESTDATA/a/ab/", "#\\TESTDATA\\a\\ab",
                                                                   1, new String[] { "file:#/TESTDATA/a/ab/" },

                                                                   FileSystem.dir( "aba", "/a/ab/aba",
                                                                           "file:#/TESTDATA/a/ab/aba/", "#\\TESTDATA\\a\\ab\\aba",
                                                                           1, new String[] { "file:#/TESTDATA/a/ab/aba/" }) ) ),

                                                   FileSystem.dir( "b", "/b",
                                                           "file:#/TESTDATA/b/", "#\\TESTDATA\\b",
                                                           1, new String[] { "file:#/TESTDATA/b/" },

                                                           FileSystem.dir( "ba", "/b/ba",
                                                                   "file:#/TESTDATA/b/ba/", "#\\TESTDATA\\b\\ba",
                                                                   1, new String[] { "file:#/TESTDATA/b/ba/" },

                                                                   FileSystem.dir( "baa", "/b/ba/baa",
                                                                           "file:#/TESTDATA/b/ba/baa/", "#\\TESTDATA\\b\\ba\\baa",
                                                                           1, new String[] { "file:#/TESTDATA/b/ba/baa/" }) ),

                                                           FileSystem.dir( "bb", "/b/bb",
                                                                   "file:#/TESTDATA/b/bb/", "#\\TESTDATA\\b\\bb",
                                                                   1, new String[] { "file:#/TESTDATA/b/bb/" },

                                                                   FileSystem.root( "a.jar", "/b/bb/a.jar", "file:#/TESTDATA/b/bb/a.jar",
                                                                            FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                                            "#\\TESTDATA\\b\\bb\\a.jar",
                                                                            1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/" },

                                                                            FileSystem.dir( "aa", "/aa",
                                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/aa", null,
                                                                                    1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/aa/" },

                                                                                    FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                             "jar:file:#/TESTDATA/b/bb/a.jar!/aa/aa.txt", null ) ),

                                                                            FileSystem.dir( "ab", "/ab",
                                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/ab", null,
                                                                                    1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/" },

                                                                                    FileSystem.dir( "aba", "/ab/aba",
                                                                                            "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba", null,
                                                                                            1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/" },

                                                                                            FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                                     "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                                    FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                             "jar:file:#/TESTDATA/b/bb/a.jar!/ab/ab.txt", null ) ),

                                                                            FileSystem.File( "a.txt", "/a.txt",
                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                     "jar:file:#/TESTDATA/b/bb/a.jar!/a.txt", null ),

                                                                            FileSystem.dir( "META-INF", "/META-INF",
                                                                                    "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF", null,
                                                                                    1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/" },

                                                                                    FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                                             FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                                             "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ) ),

                                                   FileSystem.dir( "c", "/c",
                                                           "file:#/TESTDATA/c/", "#\\TESTDATA\\c",
                                                           1, new String[] { "file:#/TESTDATA/c/" },

                                                           FileSystem.root( "a.jar", "/c/a.jar", "file:#/TESTDATA/c/a.jar",
                                                                    FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                                    "#\\TESTDATA\\c\\a.jar",
                                                                    1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/" },

                                                                    FileSystem.dir( "aa", "/aa",
                                                                            "jar:file:#/TESTDATA/c/a.jar!/aa", null,
                                                                            1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/aa/" },

                                                                            FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                     "jar:file:#/TESTDATA/c/a.jar!/aa/aa.txt", null ) ),

                                                                    FileSystem.dir( "ab", "/ab",
                                                                            "jar:file:#/TESTDATA/c/a.jar!/ab", null,
                                                                            1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/" },

                                                                            FileSystem.dir( "aba", "/ab/aba",
                                                                                    "jar:file:#/TESTDATA/c/a.jar!/ab/aba", null,
                                                                                    1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/aba/" },

                                                                                    FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                             "jar:file:#/TESTDATA/c/a.jar!/ab/aba/aba.txt", null ) ),

                                                                            FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                     "jar:file:#/TESTDATA/c/a.jar!/ab/ab.txt", null ) ),

                                                                    FileSystem.File( "a.txt", "/a.txt",
                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                             "jar:file:#/TESTDATA/c/a.jar!/a.txt", null ),

                                                                    FileSystem.dir( "META-INF", "/META-INF",
                                                                            "jar:file:#/TESTDATA/c/a.jar!/META-INF", null,
                                                                            1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/META-INF/" },

                                                                            FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                                     FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                                     "jar:file:#/TESTDATA/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),
                                                           FileSystem.root( "b.jar", "/c/b.jar", "file:#/TESTDATA/c/b.jar",
                                                                    FileSystem.DOES_NOT_HAVE_DATA, null, 1227,
                                                                    "#\\TESTDATA\\c\\b.jar",
                                                                    1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/" },

                                                                    FileSystem.dir( "bb", "/bb",
                                                                            "jar:file:#/TESTDATA/c/b.jar!/bb", null,
                                                                            1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/bb/" },

                                                                            FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/TESTDATA/c/b.jar!/bb/a.jar",
                                                                                     FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                                                     null,
                                                                                     1, new String[] { "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/" },

                                                                                     FileSystem.dir( "aa", "/aa",
                                                                                             "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/aa", null,
                                                                                             1, new String[] { "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                                                             FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                                                      FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                                      "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                                                     FileSystem.dir( "ab", "/ab",
                                                                                             "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/ab", null,
                                                                                             1, new String[] { "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                                                             FileSystem.dir( "aba", "/ab/aba",
                                                                                                     "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/ab/aba", null,
                                                                                                     1, new String[] { "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                                                     FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                                              FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                                              "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                                             FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                                                      FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                                      "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                                                     FileSystem.File( "a.txt", "/a.txt",
                                                                                              FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                              "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                                                     FileSystem.dir( "META-INF", "/META-INF",
                                                                                             "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/META-INF", null,
                                                                                             1, new String[] { "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                                                             FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                                                      FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                                                      "jar:file:#/cacheDir/WEB-INF/lib/myutility.jar/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                                                                    FileSystem.dir( "META-INF", "/META-INF",
                                                                            "jar:file:#/TESTDATA/c/b.jar!/META-INF", null,
                                                                            1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/META-INF/" },

                                                                            FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                                     FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                                     "jar:file:#/TESTDATA/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                                                    FileSystem.dir( "ba", "/ba",
                                                                            "jar:file:#/TESTDATA/c/b.jar!/ba", null,
                                                                            1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/" },

                                                                            FileSystem.dir( "baa", "/ba/baa",
                                                                                    "jar:file:#/TESTDATA/c/b.jar!/ba/baa", null,
                                                                                    1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/baa/" },

                                                                                    FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                             "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa1.txt", null ),

                                                                                    FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                             "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ) ) ) ) ),

                 FileSystem.root( "myjar.jar", "/myjar.jar", "file:#/TEST.JAR",
                          FileSystem.DOES_NOT_HAVE_DATA, null, 3344,
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
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/" },

                                                   FileSystem.dir( "aa", "/aa",
                                                           "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/aa", null,
                                                           1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/aa/" },

                                                           FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/aa/aa.txt", null ) ),

                                                   FileSystem.dir( "ab", "/ab",
                                                           "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/ab", null,
                                                           1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/ab/" },

                                                           FileSystem.dir( "aba", "/ab/aba",
                                                                   "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/ab/aba", null,
                                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/ab/aba/" },

                                                                   FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                           FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/ab/ab.txt", null ) ),

                                                   FileSystem.File( "a.txt", "/a.txt",
                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                            "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/a.txt", null ),

                                                   FileSystem.dir( "META-INF", "/META-INF",
                                                           "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/META-INF", null,
                                                           1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/META-INF/" },

                                                           FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                    FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/b/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

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
                                           1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/" },

                                           FileSystem.dir( "aa", "/aa",
                                                   "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/aa", null,
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/aa/" },

                                                   FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                            "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/aa/aa.txt", null ) ),

                                           FileSystem.dir( "ab", "/ab",
                                                   "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/ab", null,
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/ab/" },

                                                   FileSystem.dir( "aba", "/ab/aba",
                                                           "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/ab/aba", null,
                                                           1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/ab/aba/" },

                                                           FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/ab/aba/aba.txt", null ) ),

                                                   FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                            "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/ab/ab.txt", null ) ),

                                           FileSystem.File( "a.txt", "/a.txt",
                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                    "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/a.txt", null ),

                                           FileSystem.dir( "META-INF", "/META-INF",
                                                   "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/META-INF", null,
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/META-INF/" },

                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                            "jar:file:#/cacheDir/.cache/myjar.jar/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),

                                  FileSystem.root( "b.jar", "/c/b.jar", "jar:file:#/TEST.JAR!/c/b.jar",
                                           FileSystem.DOES_NOT_HAVE_DATA, null, 1227,
                                           null,
                                           1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/" },

                                           FileSystem.dir( "bb", "/bb",
                                                   "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/bb", null,
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/bb/" },

                                                   FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/bb/a.jar",
                                                            FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                            null,
                                                            1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/" },

                                                            FileSystem.dir( "aa", "/aa",
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/aa", null,
                                                                    1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                                    FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                             "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                            FileSystem.dir( "ab", "/ab",
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/ab", null,
                                                                    1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                                    FileSystem.dir( "aba", "/ab/aba",
                                                                            "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/ab/aba", null,
                                                                            1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                            FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                     "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                    FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                             FileSystem.DOES_HAVE_DATA, "", 0,
                                                                             "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                            FileSystem.File( "a.txt", "/a.txt",
                                                                     FileSystem.DOES_HAVE_DATA, "", 0,
                                                                     "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                            FileSystem.dir( "META-INF", "/META-INF",
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/META-INF", null,
                                                                    1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                                    FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                             FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                             "jar:file:#/cacheDir/.cache/myjar.jar/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                                           FileSystem.dir( "META-INF", "/META-INF",
                                                   "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/META-INF", null,
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/META-INF/" },

                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                            "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                           FileSystem.dir( "ba", "/ba",
                                                   "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/ba", null,
                                                   1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/ba/" },

                                                   FileSystem.dir( "baa", "/ba/baa",
                                                           "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/ba/baa", null,
                                                           1, new String[] { "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/ba/baa/" },

                                                           FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/ba/baa/baa1.txt", null ),

                                                           FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/cacheDir/.cache/myjar.jar/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ),

                          FileSystem.dir( "META-INF", "/META-INF",
                                  "jar:file:#/TEST.JAR!/META-INF", null,
                                  1, new String[] { "jar:file:#/TEST.JAR!/META-INF/" },

                                  FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                           FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                           "jar:file:#/TEST.JAR!/META-INF/MANIFEST.MF", null ) ) ),

                 FileSystem.dir( "META-INF", "/META-INF",
                         "null", null,
                         1, new String[] { "file:#/TESTDATA/" },

                         FileSystem.dir( "a", "/META-INF/a",
                                 "file:#/TESTDATA/a/", "#\\TESTDATA\\a",
                                 1, new String[] { "file:#/TESTDATA/a/" },

                                 FileSystem.File( "a.txt", "/META-INF/a/a.txt",
                                          FileSystem.DOES_HAVE_DATA, "", 0,
                                          "file:#/TESTDATA/a/a.txt", "#\\TESTDATA\\a\\a.txt" ),

                                 FileSystem.dir( "aa", "/META-INF/a/aa",
                                         "file:#/TESTDATA/a/aa/", "#\\TESTDATA\\a\\aa",
                                         1, new String[] { "file:#/TESTDATA/a/aa/" }),

                                 FileSystem.dir( "ab", "/META-INF/a/ab",
                                         "file:#/TESTDATA/a/ab/", "#\\TESTDATA\\a\\ab",
                                         1, new String[] { "file:#/TESTDATA/a/ab/" },

                                         FileSystem.dir( "aba", "/META-INF/a/ab/aba",
                                                 "file:#/TESTDATA/a/ab/aba/", "#\\TESTDATA\\a\\ab\\aba",
                                                 1, new String[] { "file:#/TESTDATA/a/ab/aba/" }) ) ),

                         FileSystem.dir( "b", "/META-INF/b",
                                 "file:#/TESTDATA/b/", "#\\TESTDATA\\b",
                                 1, new String[] { "file:#/TESTDATA/b/" },

                                 FileSystem.dir( "ba", "/META-INF/b/ba",
                                         "file:#/TESTDATA/b/ba/", "#\\TESTDATA\\b\\ba",
                                         1, new String[] { "file:#/TESTDATA/b/ba/" },

                                         FileSystem.dir( "baa", "/META-INF/b/ba/baa",
                                                 "file:#/TESTDATA/b/ba/baa/", "#\\TESTDATA\\b\\ba\\baa",
                                                 1, new String[] { "file:#/TESTDATA/b/ba/baa/" },

                                                 FileSystem.File( "baa1.txt", "/META-INF/b/ba/baa/baa1.txt",
                                                          FileSystem.DOES_HAVE_DATA, "minion", 6,
                                                          "file:#/TESTDATA/b/ba/baa/baa1.txt", "#\\TESTDATA\\b\\ba\\baa\\baa1.txt" ),

                                                 FileSystem.File( "baa2.txt", "/META-INF/b/ba/baa/baa2.txt",
                                                          FileSystem.DOES_HAVE_DATA, "chain", 5,
                                                          "file:#/TESTDATA/b/ba/baa/baa2.txt", "#\\TESTDATA\\b\\ba\\baa\\baa2.txt" ) ) ),

                                 FileSystem.dir( "bb", "/META-INF/b/bb",
                                         "file:#/TESTDATA/b/bb/", "#\\TESTDATA\\b\\bb",
                                         1, new String[] { "file:#/TESTDATA/b/bb/" },

                                         FileSystem.root( "a.jar", "/META-INF/b/bb/a.jar", "file:#/TESTDATA/b/bb/a.jar",
                                                  FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                  "#\\TESTDATA\\b\\bb\\a.jar",
                                                  1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/" },

                                                  FileSystem.dir( "aa", "/aa",
                                                          "jar:file:#/TESTDATA/b/bb/a.jar!/aa", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/aa/" },

                                                          FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/aa/aa.txt", null ) ),

                                                  FileSystem.dir( "ab", "/ab",
                                                          "jar:file:#/TESTDATA/b/bb/a.jar!/ab", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/" },

                                                          FileSystem.dir( "aba", "/ab/aba",
                                                                  "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba", null,
                                                                  1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/" },

                                                                  FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                          FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/ab/ab.txt", null ) ),

                                                  FileSystem.File( "a.txt", "/a.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/a.txt", null ),

                                                  FileSystem.dir( "META-INF", "/META-INF",
                                                          "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/" },

                                                          FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                   FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ) ),

                         FileSystem.dir( "c", "/META-INF/c",
                                 "file:#/TESTDATA/c/", "#\\TESTDATA\\c",
                                 1, new String[] { "file:#/TESTDATA/c/" },

                                 FileSystem.root( "a.jar", "/META-INF/c/a.jar", "file:#/TESTDATA/c/a.jar",
                                          FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                          "#\\TESTDATA\\c\\a.jar",
                                          1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/" },

                                          FileSystem.dir( "aa", "/aa",
                                                  "jar:file:#/TESTDATA/c/a.jar!/aa", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/aa/" },

                                                  FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                           "jar:file:#/TESTDATA/c/a.jar!/aa/aa.txt", null ) ),

                                          FileSystem.dir( "ab", "/ab",
                                                  "jar:file:#/TESTDATA/c/a.jar!/ab", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/" },

                                                  FileSystem.dir( "aba", "/ab/aba",
                                                          "jar:file:#/TESTDATA/c/a.jar!/ab/aba", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/aba/" },

                                                          FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/c/a.jar!/ab/aba/aba.txt", null ) ),

                                                  FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                           "jar:file:#/TESTDATA/c/a.jar!/ab/ab.txt", null ) ),

                                          FileSystem.File( "a.txt", "/a.txt",
                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                   "jar:file:#/TESTDATA/c/a.jar!/a.txt", null ),

                                          FileSystem.dir( "META-INF", "/META-INF",
                                                  "jar:file:#/TESTDATA/c/a.jar!/META-INF", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/META-INF/" },

                                                  FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                           FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                           "jar:file:#/TESTDATA/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),

                                 FileSystem.root( "b.jar", "/META-INF/c/b.jar", "file:#/TESTDATA/c/b.jar",
                                          FileSystem.DOES_NOT_HAVE_DATA, null, 1227,
                                          "#\\TESTDATA\\c\\b.jar",
                                          1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/" },

                                          FileSystem.dir( "bb", "/bb",
                                                  "jar:file:#/TESTDATA/c/b.jar!/bb", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/bb/" },

                                                  FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/TESTDATA/c/b.jar!/bb/a.jar",
                                                           FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                           null,
                                                           1, new String[] { "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/" },

                                                           FileSystem.dir( "aa", "/aa",
                                                                   "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/aa", null,
                                                                   1, new String[] { "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                                   FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                           FileSystem.dir( "ab", "/ab",
                                                                   "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/ab", null,
                                                                   1, new String[] { "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                                   FileSystem.dir( "aba", "/ab/aba",
                                                                           "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/ab/aba", null,
                                                                           1, new String[] { "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                           FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                    "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                   FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                           FileSystem.File( "a.txt", "/a.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                           FileSystem.dir( "META-INF", "/META-INF",
                                                                   "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/META-INF", null,
                                                                   1, new String[] { "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                            "jar:file:#/cacheDir/META-INF/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                                          FileSystem.dir( "META-INF", "/META-INF",
                                                  "jar:file:#/TESTDATA/c/b.jar!/META-INF", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/META-INF/" },

                                                  FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                           FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                           "jar:file:#/TESTDATA/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                          FileSystem.dir( "ba", "/ba",
                                                  "jar:file:#/TESTDATA/c/b.jar!/ba", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/" },

                                                  FileSystem.dir( "baa", "/ba/baa",
                                                          "jar:file:#/TESTDATA/c/b.jar!/ba/baa", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/baa/" },

                                                          FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa1.txt", null ),

                                                          FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ) ),

                 FileSystem.dir( "withSlash", "/withSlash",
                         "null", null,
                         1, new String[] { "file:#/TESTDATA/" },

                         FileSystem.dir( "a", "/withSlash/a",
                                 "file:#/TESTDATA/a/", "#\\TESTDATA\\a",
                                 1, new String[] { "file:#/TESTDATA/a/" },

                                 FileSystem.File( "a.txt", "/withSlash/a/a.txt",
                                          FileSystem.DOES_HAVE_DATA, "", 0,
                                          "file:#/TESTDATA/a/a.txt", "#\\TESTDATA\\a\\a.txt" ),

                                 FileSystem.dir( "aa", "/withSlash/a/aa",
                                         "file:#/TESTDATA/a/aa/", "#\\TESTDATA\\a\\aa",
                                         1, new String[] { "file:#/TESTDATA/a/aa/" },

                                         FileSystem.File( "aa.txt", "/withSlash/a/aa/aa.txt",
                                                  FileSystem.DOES_HAVE_DATA, "wibble", 6,
                                                  "file:#/TESTDATA/a/aa/aa.txt", "#\\TESTDATA\\a\\aa\\aa.txt" ) ),

                                 FileSystem.dir( "ab", "/withSlash/a/ab",
                                         "file:#/TESTDATA/a/ab/", "#\\TESTDATA\\a\\ab",
                                         1, new String[] { "file:#/TESTDATA/a/ab/" },

                                         FileSystem.File( "ab.txt", "/withSlash/a/ab/ab.txt",
                                                  FileSystem.DOES_HAVE_DATA, "fish", 4,
                                                  "file:#/TESTDATA/a/ab/ab.txt", "#\\TESTDATA\\a\\ab\\ab.txt" ),

                                         FileSystem.dir( "aba", "/withSlash/a/ab/aba",
                                                 "file:#/TESTDATA/a/ab/aba/", "#\\TESTDATA\\a\\ab\\aba",
                                                 1, new String[] { "file:#/TESTDATA/a/ab/aba/" },

                                                 FileSystem.File( "aba.txt", "/withSlash/a/ab/aba/aba.txt",
                                                          FileSystem.DOES_HAVE_DATA, "cheese", 6,
                                                          "file:#/TESTDATA/a/ab/aba/aba.txt", "#\\TESTDATA\\a\\ab\\aba\\aba.txt" ) ) ) ),

                         FileSystem.dir( "b", "/withSlash/b",
                                 "file:#/TESTDATA/b/", "#\\TESTDATA\\b",
                                 1, new String[] { "file:#/TESTDATA/b/" },

                                 FileSystem.dir( "ba", "/withSlash/b/ba",
                                         "file:#/TESTDATA/b/ba/", "#\\TESTDATA\\b\\ba",
                                         1, new String[] { "file:#/TESTDATA/b/ba/" },

                                         FileSystem.dir( "baa", "/withSlash/b/ba/baa",
                                                 "file:#/TESTDATA/b/ba/baa/", "#\\TESTDATA\\b\\ba\\baa",
                                                 1, new String[] { "file:#/TESTDATA/b/ba/baa/" },

                                                 FileSystem.File( "baa1.txt", "/withSlash/b/ba/baa/baa1.txt",
                                                          FileSystem.DOES_HAVE_DATA, "minion", 6,
                                                          "file:#/TESTDATA/b/ba/baa/baa1.txt", "#\\TESTDATA\\b\\ba\\baa\\baa1.txt" ),

                                                 FileSystem.File( "baa2.txt", "/withSlash/b/ba/baa/baa2.txt",
                                                          FileSystem.DOES_HAVE_DATA, "chain", 5,
                                                          "file:#/TESTDATA/b/ba/baa/baa2.txt", "#\\TESTDATA\\b\\ba\\baa\\baa2.txt" ) ) ),

                                 FileSystem.dir( "bb", "/withSlash/b/bb",
                                         "file:#/TESTDATA/b/bb/", "#\\TESTDATA\\b\\bb",
                                         1, new String[] { "file:#/TESTDATA/b/bb/" },

                                         FileSystem.root( "a.jar", "/withSlash/b/bb/a.jar", "file:#/TESTDATA/b/bb/a.jar",
                                                  FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                  "#\\TESTDATA\\b\\bb\\a.jar",
                                                  1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/" },

                                                  FileSystem.dir( "aa", "/aa",
                                                          "jar:file:#/TESTDATA/b/bb/a.jar!/aa", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/aa/" },

                                                          FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/aa/aa.txt", null ) ),

                                                  FileSystem.dir( "ab", "/ab",
                                                          "jar:file:#/TESTDATA/b/bb/a.jar!/ab", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/" },

                                                          FileSystem.dir( "aba", "/ab/aba",
                                                                  "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba", null,
                                                                  1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/" },

                                                                  FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                          FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/ab/ab.txt", null ) ),

                                                  FileSystem.File( "a.txt", "/a.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                           "jar:file:#/TESTDATA/b/bb/a.jar!/a.txt", null ),

                                                  FileSystem.dir( "META-INF", "/META-INF",
                                                          "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/" },

                                                          FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                   FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                   "jar:file:#/TESTDATA/b/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ) ),

                         FileSystem.dir( "c", "/withSlash/c",
                                 "file:#/TESTDATA/c/", "#\\TESTDATA\\c",
                                 1, new String[] { "file:#/TESTDATA/c/" },

                                 FileSystem.root( "a.jar", "/withSlash/c/a.jar", "file:#/TESTDATA/c/a.jar",
                                          FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                          "#\\TESTDATA\\c\\a.jar",
                                          1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/" },

                                          FileSystem.dir( "aa", "/aa",
                                                  "jar:file:#/TESTDATA/c/a.jar!/aa", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/aa/" },

                                                  FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                           "jar:file:#/TESTDATA/c/a.jar!/aa/aa.txt", null ) ),

                                          FileSystem.dir( "ab", "/ab",
                                                  "jar:file:#/TESTDATA/c/a.jar!/ab", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/" },

                                                  FileSystem.dir( "aba", "/ab/aba",
                                                          "jar:file:#/TESTDATA/c/a.jar!/ab/aba", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/ab/aba/" },

                                                          FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/c/a.jar!/ab/aba/aba.txt", null ) ),

                                                  FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                           FileSystem.DOES_HAVE_DATA, "", 0,
                                                           "jar:file:#/TESTDATA/c/a.jar!/ab/ab.txt", null ) ),

                                          FileSystem.File( "a.txt", "/a.txt",
                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                   "jar:file:#/TESTDATA/c/a.jar!/a.txt", null ),

                                          FileSystem.dir( "META-INF", "/META-INF",
                                                  "jar:file:#/TESTDATA/c/a.jar!/META-INF", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/a.jar!/META-INF/" },

                                                  FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                           FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                           "jar:file:#/TESTDATA/c/a.jar!/META-INF/MANIFEST.MF", null ) ) ),

                                 FileSystem.root( "b.jar", "/withSlash/c/b.jar", "file:#/TESTDATA/c/b.jar",
                                          FileSystem.DOES_NOT_HAVE_DATA, null, 1227,
                                          "#\\TESTDATA\\c\\b.jar",
                                          1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/" },

                                          FileSystem.dir( "bb", "/bb",
                                                  "jar:file:#/TESTDATA/c/b.jar!/bb", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/bb/" },

                                                  FileSystem.root( "a.jar", "/bb/a.jar", "jar:file:#/TESTDATA/c/b.jar!/bb/a.jar",
                                                           FileSystem.DOES_NOT_HAVE_DATA, null, 967,
                                                           null,
                                                           1, new String[] { "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/" },

                                                           FileSystem.dir( "aa", "/aa",
                                                                   "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/aa", null,
                                                                   1, new String[] { "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/aa/" },

                                                                   FileSystem.File( "aa.txt", "/aa/aa.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/aa/aa.txt", null ) ),

                                                           FileSystem.dir( "ab", "/ab",
                                                                   "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/ab", null,
                                                                   1, new String[] { "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/ab/" },

                                                                   FileSystem.dir( "aba", "/ab/aba",
                                                                           "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/ab/aba", null,
                                                                           1, new String[] { "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/ab/aba/" },

                                                                           FileSystem.File( "aba.txt", "/ab/aba/aba.txt",
                                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                                    "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/ab/aba/aba.txt", null ) ),

                                                                   FileSystem.File( "ab.txt", "/ab/ab.txt",
                                                                            FileSystem.DOES_HAVE_DATA, "", 0,
                                                                            "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/ab/ab.txt", null ) ),

                                                           FileSystem.File( "a.txt", "/a.txt",
                                                                    FileSystem.DOES_HAVE_DATA, "", 0,
                                                                    "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/a.txt", null ),

                                                           FileSystem.dir( "META-INF", "/META-INF",
                                                                   "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/META-INF", null,
                                                                   1, new String[] { "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/META-INF/" },

                                                                   FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                                            FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                                            "jar:file:#/cacheDir/withSlash/c/.cache/b.jar/bb/a.jar!/META-INF/MANIFEST.MF", null ) ) ) ),

                                          FileSystem.dir( "META-INF", "/META-INF",
                                                  "jar:file:#/TESTDATA/c/b.jar!/META-INF", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/META-INF/" },

                                                  FileSystem.File( "MANIFEST.MF", "/META-INF/MANIFEST.MF",
                                                           FileSystem.DOES_HAVE_DATA, "Manifest-Version: 1.0\r\nCreated-By: 1.6.0 (IBM Corporation)\r\n\r\n", 62,
                                                           "jar:file:#/TESTDATA/c/b.jar!/META-INF/MANIFEST.MF", null ) ),

                                          FileSystem.dir( "ba", "/ba",
                                                  "jar:file:#/TESTDATA/c/b.jar!/ba", null,
                                                  1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/" },

                                                  FileSystem.dir( "baa", "/ba/baa",
                                                          "jar:file:#/TESTDATA/c/b.jar!/ba/baa", null,
                                                          1, new String[] { "jar:file:#/TESTDATA/c/b.jar!/ba/baa/" },

                                                          FileSystem.File( "baa1.txt", "/ba/baa/baa1.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa1.txt", null ),

                                                          FileSystem.File( "baa2.txt", "/ba/baa/baa2.txt",
                                                                   FileSystem.DOES_HAVE_DATA, "", 0,
                                                                   "jar:file:#/TESTDATA/c/b.jar!/ba/baa/baa2.txt", null ) ) ) ) ) ) );
}
