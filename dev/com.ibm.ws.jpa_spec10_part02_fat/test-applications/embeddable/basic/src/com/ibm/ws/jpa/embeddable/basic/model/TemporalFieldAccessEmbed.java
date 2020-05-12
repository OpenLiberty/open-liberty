package com.ibm.ws.jpa.embeddable.basic.model;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
@Access(AccessType.FIELD)
@SuppressWarnings("serial")
public class TemporalFieldAccessEmbed implements java.io.Serializable {

    @Temporal(TemporalType.DATE)
    private Date temporalValueFA;

    public TemporalFieldAccessEmbed() {
    }

    public TemporalFieldAccessEmbed(Date temporalValueFA) {
        this.temporalValueFA = temporalValueFA;
    }

    public Date getTemporalValueFA() {
        return this.temporalValueFA;
    }

    public void setTemporalValueFA(Date temporalValueFA) {
        this.temporalValueFA = temporalValueFA;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof TemporalFieldAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
        return "temporalValueFA=" + (temporalValueFA != null ? sdf.format(temporalValueFA).toString() : "null");
    }

}
