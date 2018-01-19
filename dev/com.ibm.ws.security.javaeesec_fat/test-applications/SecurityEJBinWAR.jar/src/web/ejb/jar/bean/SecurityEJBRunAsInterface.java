package web.ejb.jar.bean;

/**
 * Interface for Enterprise Bean
 */
public interface SecurityEJBRunAsInterface {

    public abstract String manager();

    public abstract String employee();

    public abstract String employeeAndManager();

}