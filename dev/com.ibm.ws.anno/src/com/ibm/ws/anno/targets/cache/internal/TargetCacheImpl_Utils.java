/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.cache.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.ibm.ws.anno.util.internal.UtilImpl_FileUtils;

public class TargetCacheImpl_Utils {
    // Encode samples:

    // Value [ a ] [ a ]
    // Value [ a a ] [ a+a ]
    // Value [ a+b ] [ a%2Bb ]
    // Value [ a\b ] [ a%5Cb ]
    // Value [ a/b ] [ a%2Fb ]
    // Value [ a a/b b ] [ a+a%2Fb+b ]
    // Value [ a a\b b ] [ a+a%5Cb+b ]

    public static final String UTF_8_ENCODING_NAME = "UTF-8";

    public static final String encodePath(String path) {
        try {
            return URLEncoder.encode(path, UTF_8_ENCODING_NAME); // throws UnsupportedEncodingException
        } catch (UnsupportedEncodingException e) {
            // This should never occur.
            throw new IllegalArgumentException("Unable to use UTF-8 encoding [ " + UTF_8_ENCODING_NAME + " ]", e);
        }
    }

    public static final String decodePath(String e_path) {
        try {
            return URLDecoder.decode(e_path, UTF_8_ENCODING_NAME); // throws UnsupportedEncodingException
        } catch (UnsupportedEncodingException e) {
            // This should never occur.
            throw new IllegalArgumentException("Unable to use UTF-8 encoding [ " + UTF_8_ENCODING_NAME + " ]", e);
        }
    }

    //

    public static FilenameFilter createPrefixFilter(final String prefix) {
        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        };
    }

    public static FilenameFilter createPrefixFilter(PrefixWidget prefixWidget) {
        final String prefix = prefixWidget.getPrefix();

        return new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(prefix);
            }
        };
    }

    public interface ListWidget {
        String e_removePrefix(String e_fileName);
        String e_addPrefix(String e_fileName);

        //

        FilenameFilter getFilter();

        //

        void storeParent(File parentDir);
        void storeChild(File childDir);
    }

    public static class PrefixWidget {
        public PrefixWidget(String prefix) {
            this.prefix = prefix;
            this.e_prefix = TargetCacheImpl_Utils.encodePath(prefix);
        }

        private final String prefix;
        private final String e_prefix;

        public String getPrefix() {
            return prefix;
        }

        public String e_getPrefix() {
            return e_prefix;
        }

        //

        public String e_removePrefix(String e_name) {
            return e_name.substring( e_getPrefix().length() );
        }

        public String e_addPrefix(String e_name) {
            return e_getPrefix() + e_name;
        }
    }

    public static abstract class PrefixListWidget implements ListWidget {
        public PrefixListWidget(PrefixWidget prefixWidget) {
            this.prefixWidget = prefixWidget;
            this.filter = TargetCacheImpl_Utils.createPrefixFilter(prefixWidget);
        }

        //

        protected final PrefixWidget prefixWidget;

        public PrefixWidget getPrefixWidget() {
            return prefixWidget;
        }

        @Override
        public String e_addPrefix(String e_name) {
            return getPrefixWidget().e_addPrefix(e_name);
        }

        @Override
        public String e_removePrefix(String e_name) {
            return getPrefixWidget().e_removePrefix(e_name);
        }

        //

        protected final FilenameFilter filter;

        @Override
        public FilenameFilter getFilter() {
            return filter;
        }

        //

        @Override
        public void storeParent(File parentDir) {
            File[] childFiles = UtilImpl_FileUtils.listFiles( parentDir, getFilter() );

//            if ( childFiles.length == 0 ) {
//            	String path = parentDir.getAbsolutePath();
//            	System.out.println("Path [ " + path + " ]");
//            }

            for ( File childFile : childFiles ) {
                // Need to put the child file relative to the parent;
                // This information is NOT placed into the child files by File.list.
                storeChild( new File(parentDir, childFile.getName()) );
            }
        }

        @Override
        public void storeChild(File childFile) {
            String e_childFileName = childFile.getName();
            String e_childName = e_removePrefix(e_childFileName);
            String childName = decodePath(e_childName);

            storeChild(childName, e_childName, childFile);
        }

        public abstract void storeChild(String childName, String e_childName, File childFile);
    }
}
