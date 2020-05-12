package com.ibm.ws.jpa.embeddable.basic.model;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Embeddable
@SuppressWarnings("serial")
public class CollectionTemporalEmbed implements java.io.Serializable {

    public static final Collection<Date> INIT = Arrays.asList(new Date(System.currentTimeMillis() - 200000000), new Date(1));
    public static final Collection<Date> UPDATE = Arrays.asList(new Date(System.currentTimeMillis() - 200000000), new Date(), new Date(1));

    @CollectionTable(name = "ColDate", joinColumns = @JoinColumn(name = "parent_id"))
    @ElementCollection(fetch = FetchType.EAGER)
    @Temporal(TemporalType.DATE)
    @Column(name = "value")
//    @OrderColumn(name = "valueOrderColumn")
    private Collection<Date> collectionDate;

    public CollectionTemporalEmbed() {
    }

    public CollectionTemporalEmbed(Collection<Date> collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Collection<Date> getCollectionDate() {
        return this.collectionDate;
    }

    public void setCollectionDate(Collection<Date> collectionDate) {
        this.collectionDate = collectionDate;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof CollectionTemporalEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (collectionDate != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, yyyy");
            sb.append("collectionDate=[");
            for (Date date : collectionDate) {
                sb.append(sdf.format(date).toString());
                sb.append(", ");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.deleteCharAt(sb.length() - 1);
            sb.append("]");
        } else
            sb.append("collectionDate=null");
        return sb.toString();
    }

}
