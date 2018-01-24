package test.ejb;

import javax.ejb.Remote;
import javax.ejb.Stateful;


/**
 * Session Bean implementation class EJB1Impl
 */
@Stateful
@Remote(EJB1.class)
public class EJB1Impl implements EJB1 {

    /**
     * Default constructor. 
     */
    public EJB1Impl() {
        // TODO Auto-generated constructor stub
    }

	@Override
	public String getData() {
		return "DATA!";
	}

}
