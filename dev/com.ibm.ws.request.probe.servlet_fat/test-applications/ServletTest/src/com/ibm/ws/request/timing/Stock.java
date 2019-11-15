package com.ibm.ws.request.timing;


public class Stock
{

    public Stock(String name, Double value)
    {
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Double getValue()
    {
        return value;
    }

    public void setValue(Double value)
    {
        this.value = value;
    }

    private String name;
    private Double value;
}