/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.artifact.zip.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.artifact.ArtifactContainer;
import com.ibm.wsspi.artifact.ArtifactEntry;
import com.ibm.wsspi.kernel.service.utils.PathUtils;

/**
 * Zip container zip entry management utilities.
 *
 * Provides two capabilities:
 *
 * <ul><li>{@link #collectZipEntries} builds a table of the zip entries
 *         of a zip file.  The table is sorted using the standard path
 *         comparator {@link PathUtils#PATH_COMPARATOR}.  Leading and
 *         trailing slashes are removed from paths.</li>
 *     <li>{@link #collectIteratorData} builds a table of child paths.</li>
 * </ul>
 *
 * The capabilities replace function which was previously provided by
 * {@link java.util.TreeMap} and {@link java.util.NavigableMap}.  However,
 * these are poorly tuned for the needs of zip file containers.  Three problems
 * are prominent:
 *
 * First, {@link java.util.TreeMap} is tuned to enable dynamic updates, with
 * machinery to maintain a balanced tree.  This does not fit the usage pattern,
 * which has a fixed collection of elements.
 *
 * Dynamic addition to the tree map can be avoided by building a sorted mapping
 * and doing {@link java.util.SortedMap#putAll}, which is is tuned by tree map
 * to quickly add the sorted elements.  However, a second problem still remains.
 *
 * Second, using {@link java.util.NavigableMap#subMap} to select a subset for
 * iteration generates a range of all entries which are descendants of the range,
 * not just immediate children.  Code which performs iteration must iterate across
 * the entire sub-range to select the unique immediate children.  That leads to a
 * large amount of extra iteration, since iteration across sub-trees will encounter
 * the same entries.
 *
 * Third, a tree map uses entries which require considerably more storage than
 * simple map entries.  That is, type {@link java.util.TreeMap.Entry} has fields:
 *
 * <code>
 *     Entry<K,V> implements Map.Entry<K,V> {
 *         K key;
 *         V value;
 *         Entry<K,V> left;
 *         Entry<K,V> right;
 *         Entry<K,V> parent;
 *         boolean color = BLACK;
 *     }
 * </code>
 *
 * Whereas a simple implementation of {@link java.util.Map.Entry} requires only
 * a key pointer and a value pointer.
 */
public class ZipFileContainerUtils {

    public static class ZipFileEntryIterator implements Iterator<ArtifactEntry> {
        private final ZipFileContainer rootContainer;
        private final Map.Entry<String, ZipEntry>[] allZipEntries;

        private final ArtifactContainer nestedContainer;
        private final String parentPath;

        private final int[] locations;
        private int index;

        @Trivial
        public ZipFileEntryIterator(
            ZipFileContainer rootContainer,
            ArtifactContainer nestedContainer,
            Map.Entry<String, ZipEntry>[] allZipEntries,
            ZipFileContainerUtils.IteratorData iteratorData) {

            this.rootContainer = rootContainer;
            this.allZipEntries = allZipEntries;

            this.nestedContainer = nestedContainer;
            this.parentPath = iteratorData.path;

            this.locations = iteratorData.locations;

            this.index = 0;
        }

        @Trivial
        public boolean hasNext() {
            return ( index < locations.length );
        }

        public ZipFileEntry next() {
            if ( index >= locations.length ) {
                throw new NoSuchElementException();
            }

            int location = locations[index++];

            Map.Entry<String, ZipEntry> nextEntry = allZipEntries[location];
            String nextPath = nextEntry.getKey();
            ZipEntry nextZipEntry = nextEntry.getValue();

            int parentLen;
            if ( parentPath.isEmpty() ) {
                parentLen = 0;
            } else {
                parentLen = parentPath.length() + 1;
            }

            // The data at the next location represents a unique child,
            // but may be for an immediate child *or* for a grandchild.
            //
            // For example:
            //
            // parent:    gp/p
            // locations: gp/p/c1
            //            gp/p/c2/gc1

            String entryName;
            String entryPath;

            int slashLoc = nextPath.indexOf('/', parentLen);
            if ( slashLoc == -1 ) {
                // The location is an immediate child.
                entryName = nextPath.substring(parentLen);
                entryPath = nextPath;
            } else {
                // The location is a grandchild.
                entryName = nextPath.substring(parentLen, slashLoc);
                entryPath = nextPath.substring(0, slashLoc);
                nextZipEntry = null;
            }

            String a_entryPath = "/" + entryPath;

            ZipFileEntry nextZipFileEntry = rootContainer.createEntry(
                nestedContainer,
                entryName, a_entryPath,
                location, nextZipEntry);

            return nextZipFileEntry;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    /**
     * Data for one path of a zip file.
     *
     * The locations are offsets into the all zip entries collection of the
     * root zip file.
     *
     * The locations array should never be empty: No iterator data is stored
     * for paths which have no children.
     *
     * Each location represents a unique child of the path.  Each location may
     * represent either an immediate child or a grandchild.  For example, the
     * paths:
     *
     * <pre>
     *     0 gp/p1
     *     1 gp/p1/c1
     *     2 gp/p2/c2
     * </pre>
     *
     * Generate the locations:
     *
     * <pre>
     *     { gp    : 0 = gp/p1, 2 = gp/p2/c2 }
     *     { gp/p1 : 1 = gp/p1/c1 }
     *     { gp/p2 : 2 = gp/p2/c2 }
     * </pre>
     *
     * Note the use of "gp/p2/c2" to represent the child "gp/p2" of "gp".
     */
    public static class IteratorData {
        public final String path;
        public final int[] locations;

        @Trivial
        public IteratorData(String path, int[] locations) {
            this.path = path;
            this.locations = locations;
        }
    }

    private static final List<Integer> EMPTY_OFFSETS = null;

	private static final int[] EMPTY_OFFSETS_ARRAY = new int[0];

	/**
	 * Allocate an offsets list.
	 * 
	 * If no discarded list is available, allocate a new list.
	 * 
	 * Otherwise, use one of the discarded lists.
	 * 
	 * @param offsetsStorage Storage of discarded offset lists.
	 * 
	 * @return An allocated offsets list.
	 */
    private static List<Integer> allocateOffsets(List<List<Integer>> offsetsStorage) {
    	if ( offsetsStorage.isEmpty() ) {
    		return new ArrayList<Integer>();
    	} else {
    		return ( offsetsStorage.remove(0) );
    	}
    }
    
    /**
     * Release an offsets list to storage.
     * 
     * Answer the offsets list converted to a raw array of integers.
     * 
     * @param offsetsStorage Storage into which to place the
     *     offset list.
     * @param offsets The offset list which is to be released to storage.
     * 
     * @return The offset list converted to a raw list of integers.
     */
    private static int[] releaseOffsets(List<List<Integer>> offsetsStorage, List<Integer> offsets) {    	
    	int numValues = offsets.size();
    	
    	int[] extractedValues;
    	
    	if ( numValues == 0 ) {
    		extractedValues = EMPTY_OFFSETS_ARRAY;
    	} else {
    		extractedValues = new int[numValues];
    		for ( int valueNo = 0; valueNo < numValues; valueNo++ ) {
    			extractedValues[valueNo] = offsets.get(valueNo).intValue();
    		}
    	}

    	offsets.clear();
    	offsetsStorage.add(offsets);

    	return extractedValues;
	}

    /**
     * Collect the iterator data for a collection of zip entries.
     *
     * Keys of the entries must be paths stripped of leading and trailing slashes.
     *
     * The entries must be in ascending order per {@link PathUtils#PATH_COMPARATOR}.
     *
     * Only place data for paths which have no children.  In particular, leaf entries,
     * which should be most of the entries of the enclosing zip file, have no children.
     *
     * See {@link IteratorData} for additional details.
     *
     * @param zipEntryData The data for which to collect iterator data.
     *
     * @return A table of iterator data for the zip entries.
     */
    @Trivial
    public static Map<String, IteratorData> collectIteratorData(Map.Entry<String, ZipEntry>[] zipEntryData) {
        Map<String, IteratorData> allNestingData = new HashMap<String, IteratorData>();

        // Re-use offset lists.  There can be a lot of these
        // created for a busy tree.  Offset lists are only needed
        // for entries from the current entry back to the root.

        List<List<Integer>> offsetsStorage = new ArrayList<List<Integer>>(32);

        String r_lastPath = ""; // Root
        int r_lastPathLen = 0;

        List<Integer> offsets = EMPTY_OFFSETS;

        int nestingDepth = 0;

        List<String> r_pathStack = new ArrayList<String>(32);
        List<List<Integer>> offsetsStack = new ArrayList<List<Integer>>(32);

        for ( int nextOffset = 0; nextOffset < zipEntryData.length; nextOffset++ ) {
            String r_nextPath = zipEntryData[nextOffset].getKey();
            int r_nextPathLen = r_nextPath.length();

            // The next path may be on a different branch.
            //
            // Backup until a common branch is located, emitting nesting data
            // for each branch we cross.

            while ( !isChildOf(r_nextPath, r_nextPathLen, r_lastPath, r_lastPathLen) ) {
                if ( offsets != EMPTY_OFFSETS ) {
                    allNestingData.put(
                    	r_lastPath,
                    	new IteratorData( r_lastPath, releaseOffsets(offsetsStorage, offsets) ) );
                }

                nestingDepth--;
                r_lastPath = r_pathStack.remove(nestingDepth);
                r_lastPathLen = r_lastPath.length();
                offsets = offsetsStack.remove(nestingDepth);
            }

            // The entry is now guaranteed to be on the
            // same branch as the last entry.
            //
            // But, the entry be more than one nesting level
            // deeper than the last entry.
            //
            // Create and add new offset collections for each
            // new nesting level.  There may be additional
            // entries for the new nesting levels besides the
            // entry.

            Integer nextOffsetObj = Integer.valueOf(nextOffset);

            int lastSlashLoc = r_lastPathLen + 1;
            while ( lastSlashLoc != -1 ) {
                int nextSlashLoc = r_nextPath.indexOf('/', lastSlashLoc);
                String r_nextPartialPath;
                int r_nextPartialPathLen;
                if ( nextSlashLoc == -1 ) {
                    r_nextPartialPath = r_nextPath;
                    r_nextPartialPathLen = r_nextPathLen;
                    lastSlashLoc = nextSlashLoc;
                } else {
                    r_nextPartialPath = r_nextPath.substring(0, nextSlashLoc);
                    r_nextPartialPathLen = nextSlashLoc;
                    lastSlashLoc = nextSlashLoc + 1;
                }

                if ( offsets == EMPTY_OFFSETS ) {
                    offsets = allocateOffsets(offsetsStorage);
                }
                offsets.add(nextOffsetObj);

                nestingDepth++;
                r_pathStack.add(r_lastPath);
                offsetsStack.add(offsets);

                r_lastPath = r_nextPartialPath;
                r_lastPathLen = r_nextPartialPathLen;

                offsets = EMPTY_OFFSETS;
            }
        }

        // Usually, we are left some nestings beneath the root.
        //
        // Finish off each of those nestings.

        while ( nestingDepth > 0 ) {
            if ( offsets != EMPTY_OFFSETS ) {
                allNestingData.put(
                	r_lastPath,
                	new IteratorData( r_lastPath, releaseOffsets(offsetsStorage, offsets) ) );
            }
            nestingDepth--;
            r_lastPath = r_pathStack.remove(nestingDepth);
            offsets = offsetsStack.remove(nestingDepth);
        }

        // If root data remains, finish that off too.

        if ( offsets != EMPTY_OFFSETS ) {
            allNestingData.put(
            	r_lastPath,
            	new IteratorData( r_lastPath, releaseOffsets(offsetsStorage, offsets) ) );
        }

        return allNestingData;
    }

    @Trivial
    private static boolean isChildOf(
        String childPath, int childLen,
        String parentPath, int parentLen) {

        if ( parentLen == 0 ) {
            return true;
        } else if ( childLen <= parentLen ) {
            return false;
        } else  if ( childPath.charAt(parentLen) != '/' ) {
            return false;
        } else  if ( !childPath.regionMatches(0, parentPath, 0, parentLen) ) {
            return false;
        }
        return true;
    }

    //

    /**
     * Comparator of map entries which compares the entry keys using the path comparat.r
     * See {@link PathUtils#PATH_COMPARATOR}.
     */
    public static class ZipEntryComparator implements Comparator<Map.Entry<String, ZipEntry>> {
        @Trivial
        public int compare(Map.Entry<String, ZipEntry> e1, Map.Entry<String, ZipEntry> e2) {
            return PathUtils.PATH_COMPARATOR.compare( e1.getKey(), e2.getKey() );
        }
    }

    public static final ZipEntryComparator ZIP_ENTRY_COMPARATOR = new ZipEntryComparator();

    /**
     * Collect the zip entries of a zip file.  Answer these as an array of map entries.
     *
     * The paths of the map entries are the paths of the zip file entries with leading
     * and trailing slashes removed.  See {@link #stripPath(String)}.
     *
     * Intermediate / implied paths are not added to the result.
     *
     * Answer the zip entries sorted using the path comparator.
     * See {@link PathUtils#PATH_COMPARATOR}.
     *
     * @param zipFile The zip file for which to collect zip entries.
     *
     * @return The sorted zip entries of the zip file.
     */
    @Trivial
    public static Map.Entry<String, ZipEntry>[] collectZipEntries(ZipFile zipFile) {
        final List<Map.Entry<String, ZipEntry>> entriesList =
            new ArrayList<Map.Entry<String, ZipEntry>>();

        final Enumeration<? extends ZipEntry> useEntries = zipFile.entries();
        while ( useEntries.hasMoreElements() ) {
            entriesList.add( new Map.Entry<String, ZipEntry>() {
                final ZipEntry entry = useEntries.nextElement();
                final String path = stripPath( entry.getName() );

                @Override
                @Trivial
                public String getKey() {
                    return path;
                }

                @Override
                @Trivial
                public ZipEntry getValue() {
                    return entry;
                }

                @Override
                @Trivial
                public ZipEntry setValue(ZipEntry zipEntry) {
                    throw new UnsupportedOperationException();
                }
            });
        }
        
        @SuppressWarnings("unchecked")
        Map.Entry<String, ZipEntry>[] entries =
            entriesList.toArray( new Map.Entry[entriesList.size()] );

        Arrays.sort(entries, ZIP_ENTRY_COMPARATOR);

        return entries;
    }

    //

    /**
     * Paths used in the zip entry table are adjusted to never have a leading
     * slash and to never have a trailing slash.
     *
     * @param path The path which is to be adjusted.
     *
     * @return The path with leading and trailing slashes removed.
     */
    @Trivial
    private static String stripPath(String path) {
        int pathLen = path.length();

        if ( pathLen == 0 ) {
            return path;

        } else if ( pathLen == 1 ) {
            if ( path.charAt(0) == '/' ) {
                return "";
            } else {
                return path;
            }

        } else {
            if ( path.charAt(0) == '/' ) {
                if ( path.charAt(pathLen - 1) == '/' ) {
                    return path.substring(1,  pathLen - 1);
                } else {
                    return path.substring(1, pathLen);
                }
            } else {
                if ( path.charAt(pathLen - 1) == '/' ) {
                    return path.substring(0, pathLen - 1);
                } else {
                    return path;
                }
            }
        }
    }
}
