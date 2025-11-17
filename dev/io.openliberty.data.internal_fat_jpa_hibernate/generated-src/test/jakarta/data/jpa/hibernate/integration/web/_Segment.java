package test.jakarta.data.jpa.hibernate.integration.web;

import jakarta.annotation.Generated;
import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.impl.SortableAttributeRecord;

/**
 * Jakarta Data static metamodel for {@link test.jakarta.data.jpa.hibernate.integration.web.Segment}
 **/
@StaticMetamodel(Segment.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public interface _Segment {

	
	/**
	 * @see #id
	 **/
	String ID = "id";
	
	/**
	 * @see #x1
	 **/
	String X1 = "x1";
	
	/**
	 * @see #y1
	 **/
	String Y1 = "y1";
	
	/**
	 * @see #x2
	 **/
	String X2 = "x2";
	
	/**
	 * @see #y2
	 **/
	String Y2 = "y2";

	
	/**
	 * Static metamodel for attribute {@link Segment#id}
	 **/
	SortableAttribute<Segment> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * Static metamodel for attribute {@link Segment#x1}
	 **/
	SortableAttribute<Segment> x1 = new SortableAttributeRecord<>(X1);
	
	/**
	 * Static metamodel for attribute {@link Segment#y1}
	 **/
	SortableAttribute<Segment> y1 = new SortableAttributeRecord<>(Y1);
	
	/**
	 * Static metamodel for attribute {@link Segment#x2}
	 **/
	SortableAttribute<Segment> x2 = new SortableAttributeRecord<>(X2);
	
	/**
	 * Static metamodel for attribute {@link Segment#y2}
	 **/
	SortableAttribute<Segment> y2 = new SortableAttributeRecord<>(Y2);

}

