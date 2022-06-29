/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2013
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.ui.fat;

import java.net.URLEncoder;

public class Bookmark {
    String id;
    String type;
    String name;
    String url;
    String icon;
    String description;

    public Bookmark(final String name, final String url, final String icon) {
        this(name, "bookmark", name, url, icon, null);
    }

    public Bookmark(final String name, final String url, final String icon, final String description) {
        this(name, "bookmark", name, url, icon, description);
    }

    public Bookmark(final String id, final String type, final String name, final String url, final String icon, final String description) {
        try {
            this.id = URLEncoder.encode(id, "UTF-8");
        } catch (Exception ex) {
            // ignore it
        }
        this.type = type;
        this.name = name;
        this.url = url;
        this.icon = icon;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getURL() {
        return url;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    String getJSONString(final String str) {
        if (str == null) {
            return "null";
        } else {
            return "\"" + str + "\"";
        }
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder("Bookmark {\"id\":");
        sb.append(getJSONString(id));
        sb.append(",\"type\":");
        sb.append(getJSONString(type));
        sb.append(",\"name\":");
        sb.append(getJSONString(name));
        sb.append(",\"url\":");
        sb.append(getJSONString(url));
        sb.append(",\"icon\":");
        sb.append(getJSONString(icon));
        sb.append(",\"description\":");
        sb.append(getJSONString(description));
        sb.append("}");
        return sb.toString();
    }    
}
