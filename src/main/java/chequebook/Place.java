package chequebook;

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * @author Aleksey Yablokov.
 */
public class Place implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final String shortTitle;
    private final String fullTitle;
    private final String address;
    private final String businessLunchTime;
    private final URL site;
    private final List<Integer> defaultPrices;

    public Place(
            int id,
            String shortTitle,
            String fullTitle,
            String address,
            String businessLunchTime,
            URL site,
            Integer... defaultPrices) {

        this.id = id;
        this.shortTitle = shortTitle;
        this.fullTitle = fullTitle;
        this.address = address;
        this.businessLunchTime = businessLunchTime;
        this.site = site;
        this.defaultPrices = Arrays.asList(defaultPrices);
    }

    public int getId() {
        return id;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public List<Integer> getDefaultPrices() {
        return defaultPrices;
    }

    public String getFullTitle() {
        return fullTitle;
    }

    public String getAddress() {
        return address;
    }

    public String getBusinessLunchTime() {
        return businessLunchTime;
    }

    public URL getSite() {
        return site;
    }
}
