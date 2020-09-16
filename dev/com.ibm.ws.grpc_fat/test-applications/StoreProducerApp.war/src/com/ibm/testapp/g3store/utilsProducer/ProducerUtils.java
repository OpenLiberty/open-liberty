/**
 *
 */
package com.ibm.testapp.g3store.utilsProducer;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import com.ibm.testapp.g3store.restProducer.model.AppStructure;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.GenreType;
import com.ibm.testapp.g3store.restProducer.model.AppStructure.SecurityType;
import com.ibm.testapp.g3store.restProducer.model.Creator;
import com.ibm.testapp.g3store.restProducer.model.Price;
import com.ibm.testapp.g3store.restProducer.model.Price.PurchaseType;

/**
 *
 */
public class ProducerUtils {

    static Logger _log = Logger.getLogger(ProducerUtils.class.getName());

    public static boolean isBlank(String str) {
        boolean isBlank = false;

        if (str == null || str.trim().length() == 0) {
            isBlank = true;
        }
        return isBlank;
    }

    /**
     * @param key
     * @return
     */
    public static String getSysProp(String key) {
        return AccessController.doPrivileged(
                                             (PrivilegedAction<String>) () -> System.getProperty(key));
    }

    /**
     * default value "bvt.prop.HTTP_default" added where Store server.xml is using
     *
     * @return
     */
    public static int getStoreServerPort() {
        String port = getSysProp("bvt.prop.HTTP_default");
        return Integer.valueOf(port);
    }

    /**
     * default value "localhost" added in bootstrap.properties
     *
     * @return
     */
    public static String getStoreServerHost() {
        return getSysProp("testing.StoreServer.hostname");
    }

    /**
     * @param name
     * @param desc
     * @param isfree
     * @param securityType
     * @param genreType
     * @param purchaseType
     * @param sellingPrice
     * @param companyName
     * @param email
     * @return
     */
    public static AppStructure createAppData(String name, String desc, Boolean isfree, SecurityType securityType,
                                             GenreType genreType, List<Price> priceList, String companyName, String email) {

        AppStructure appStruct = new AppStructure();

        appStruct.setName(name);
        appStruct.setDesc(desc);
        appStruct.setFree(isfree);
        appStruct.setSecurityType(securityType);
        appStruct.setGenreType(genreType);
        appStruct.setPriceList(priceList);

        Creator cr = new Creator();
        cr.setCompanyName(companyName);
        cr.setEmail(email);

        appStruct.setCreator(cr);

        return appStruct;

    }

    /**
     * @param purchaseType1
     * @param sellingPrice1
     * @param purchaseType2
     * @param sellingPrice2
     * @return
     */
    public static List<Price> createPriceList(PurchaseType purchaseType1, double sellingPrice1, PurchaseType purchaseType2,
                                              double sellingPrice2) {

        List<Price> priceList = null;

        Price price1 = new Price();
        price1.setPurchaseType(purchaseType1);
        price1.setSellingPrice(sellingPrice1);

        if (purchaseType2 != null) {
            Price price2 = new Price();
            price2.setPurchaseType(purchaseType2);
            price2.setSellingPrice(sellingPrice2);

            priceList = Arrays.asList(price1, price2);
        } else {
            priceList = Arrays.asList(price1);
        }

        return priceList;
    }

}
