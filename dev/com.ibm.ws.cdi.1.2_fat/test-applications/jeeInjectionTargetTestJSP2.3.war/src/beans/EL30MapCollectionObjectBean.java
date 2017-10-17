/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package beans;

import java.util.Collection;
import java.util.Map;

/**
 * Simple bean used for Map Collection Object
 */
public class EL30MapCollectionObjectBean implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Integer, String> map;

    public EL30MapCollectionObjectBean() {
        map = null;
    }

    public void setMap(Map<Integer, String> m) {
        map = m;
    }

    public Collection<String> getMap() {
        return map.values();
    }
}
