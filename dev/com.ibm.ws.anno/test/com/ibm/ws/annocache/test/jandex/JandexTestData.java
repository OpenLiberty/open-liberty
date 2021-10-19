package com.ibm.ws.annocache.test.jandex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.ws.annocache.test.utils.TestLocalization;

public class JandexTestData {
    public static final String SPARSE_INDEX_ROOT_PATH = "publish/jandex/";
    
    public static final String[] INDEX_NAMES = {
        "com.ibm.ws.anno-jarV1.idx",  // V1
        "com.ibm.ws.anno-jarV2.idx",  // V2

        "jandex-1.2.6.Final-SNAPSHOT-jar.idx", // V1
        "jandex-2.0.6.Final-SNAPSHOT-jar.idx", // V2

        "com.ibm.websphere.org.osgi.core-jar.idx",        // V1
        "com.ibm.websphere.appserver.api.basics-jar.idx", // V2

        "petclinic.idx",
        "hibernate.idx",
        "typeannotation-test-1.8-jar-v10.idx" // v10
      };

    public static final int[] INDEX_SIZES = {
        166,
        166,
        44,
        98,
        158,
        38,
        47,
        456,
        113
    };

    public static Collection<Object[]> data() {
        List<Object[]> testParameters = new ArrayList<Object[]>(INDEX_NAMES.length);

        for ( int indexNo = 0; indexNo < INDEX_NAMES.length; indexNo++ ) {
            String indexName = INDEX_NAMES[indexNo];
            int indexSize = INDEX_SIZES[indexNo];

            String indexPath = TestLocalization.putIntoProject(SPARSE_INDEX_ROOT_PATH, indexName);
            testParameters.add( new Object[] { indexNo, indexPath, Integer.valueOf(indexSize) } );
        }

        return testParameters;
    }

    public static final int ITERATIONS = 200;

    //

    public static final String FAT_CDI_ROOT = "publish/appData";
    public static final String FAT_CDI_EAR_NAME = "fat-cdi-meetings.ear";
    public static final String FAT_CDI_WAR_NAME = "fat-cdi-meetings.war";
    public static final String FAT_CDI_JANDEX_ROOT_NAME = "jandex";

    public static final String FAT_CDI_JANDEX_ROOT_RELATIVE_PATH =
        FAT_CDI_ROOT + '/' +
        FAT_CDI_EAR_NAME + '/' + FAT_CDI_WAR_NAME + '/' +
        FAT_CDI_JANDEX_ROOT_NAME + '/';

    public static final int FAT_CDI_ITERATIONS = 5;
}
