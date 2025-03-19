package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

@StaticMetamodel(Segment.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Segment {

	String Y1 = "y1";
	String X2 = "x2";
	String X1 = "x1";
	String Y2 = "y2";
	String ID = "id";

	
	/**
	 * @see Segment#y1
	 **/
	SortableAttribute<Segment> y1 = new SortableAttributeRecord<>(Y1);
	
	/**
	 * @see Segment#x2
	 **/
	SortableAttribute<Segment> x2 = new SortableAttributeRecord<>(X2);
	
	/**
	 * @see Segment#x1
	 **/
	SortableAttribute<Segment> x1 = new SortableAttributeRecord<>(X1);
	
	/**
	 * @see Segment#y2
	 **/
	SortableAttribute<Segment> y2 = new SortableAttributeRecord<>(Y2);
	
	/**
	 * @see Segment#id
	 **/
	SortableAttribute<Segment> id = new SortableAttributeRecord<>(ID);

}

