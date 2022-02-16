/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.image.test.topo;

import static com.ibm.ws.image.test.topo.BuildProperties.IMAGES_PATH;
import static com.ibm.ws.image.test.topo.ServerImages.ImageType.FEATURE_REPO;
import static com.ibm.ws.image.test.util.FileUtils.TEST_OUTPUT_PATH_ABS;
import static com.ibm.ws.image.test.util.FileUtils.ensureNonexistence;
import static org.junit.Assert.fail;

/**
 * Pointer to a feature repository.
 */
public class FeatureRepository {
    public static final String CLASS_NAME = FeatureRepository.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    //

    public FeatureRepository(String path) {
        this.path = path;
    }

    private final String path;

    public String getPath() {
        return path;
    }
    
    //
    
    private static boolean failedFeatureRepoPath;
    
    private static String featureImagePath;
    private static FeatureRepository featureRepo;
    
    public static String getFeatureImagePath() {
        return featureImagePath;
    }

    public static FeatureRepository getFeatureRepository() {
        return featureRepo;
    }

    public static FeatureRepository setupFeatureRepo() throws Throwable {
        if ( failedFeatureRepoPath ) {
            fail("Prior failure to setup feature repository [ " + FEATURE_REPO.getDescription() + " ]");
        } else if ( featureRepo != null ) {
            return featureRepo;
        }

        try {
            log("Locating feature repository image [ " + FEATURE_REPO.getDescription() + " ] in [ " + IMAGES_PATH + " ]");
            String imagePath = FEATURE_REPO.ensureImagePath();
            log("Located feature repository image [ " + imagePath + " ]");

            String repoPath = TEST_OUTPUT_PATH_ABS + "/featureRepo";
            log("Feature repository path [ " + repoPath + " ]");

            ensureNonexistence(repoPath);
            FeatureRepository useFeatureRepo = FEATURE_REPO.extract(repoPath);

            featureImagePath = imagePath;
            featureRepo = useFeatureRepo;

        } catch ( Throwable th ) {
            failedFeatureRepoPath = true;
            throw th;
        }

        return featureRepo;
    }        
    
//  private static boolean failedRepoPath;
//  private static String repoPath;
//
//  public static String setupRepository() throws IOException {
//      if ( failedRepoPath ) {
//          fail("Prior failure to process repo-*.zip");
//      } else if ( repoPath != null ) {
//          return repoPath;
//      }
//
//      try { 
//          log("Locating repository (repo-*.zip) in [ " + IMAGES_PATH + " ]");            
//
//          List<String> images = getImages("repo.", ".zip");
//      
//          if ( (images != null) && !images.isEmpty() ) {        
//              Iterator<String> useImages = images.iterator();
//              while ( useImages.hasNext() ) {
//                  String image = useImages.next();
//                  int nameLoc = image.lastIndexOf('/');
//                  String name = ( (nameLoc == -1) ? image : image.substring(nameLoc + 1) );
//                  if ( name.contains("beta") || name.contains("json") ) {
//                      log("Ignoring [ beta ] or [ json ] image: [ " + image + " ]");
//                      useImages.remove();
//                  }
//              }
//          }
//      
//          String image;
//          if ( (images == null) || images.isEmpty() ) {
//              fail("Failed to find repo-*.zip in [ " + IMAGES_PATH + " ]");
//              image = null;
//          } else if ( images.size() > 1 ) {
//              fail("Too many repo-*.zip in [ " + IMAGES_PATH + " ]");
//              image = null;            
//          } else {
//              image = images.get(0);
//          }
//          log("Located repository [ " + image + " ]");
//
//          String useRepoPath = TEST_OUTPUT_PATH_ABS + "/repo";
//          ensureNonexistence(useRepoPath);
//          extract(image, useRepoPath, ".esa", FLATTEN);
//
//          repoPath = useRepoPath;
//
//      } catch ( Throwable th ) {
//          failedRepoPath = true;
//          throw th;
//      }
//
//      return repoPath;
//  }
}
