package com.ibm.ws.jpa.embeddable.basic.model;

import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("serial")
public class XMLTemporalFieldAccessEmbed implements java.io.Serializable {

    private Date temporalValueFA;

    public XMLTemporalFieldAccessEmbed() {
    }

    public XMLTemporalFieldAccessEmbed(Date temporalValueFA) {
        this.temporalValueFA = temporalValueFA;
    }

    public Date getTemporalValueFA() {
        return this.temporalValueFA;
    }

    public void setTemporalValueFA(Date temporalValueFA) {
        this.temporalValueFA = temporalValueFA;
    }

    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLTemporalFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        return "temporalValueFA=" + ( temporalValueFA != null ? sdf.format(temporalValueFA).toString() : "null" );
    }

}
