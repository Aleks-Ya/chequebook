package chequebook;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Aleksey Yablokov.
 */
public class Places implements Serializable {
    public static final Places instance = new Places();
    public final List<Place> places = new ArrayList<>();

    {
        try {
            places.add(new Place(
                    1,
                    "La Cucina",
                    "La Cucina",
                    "пл. Александра Невского, д. 2, ТЦ Москва, 2ой этаж",
                    "уточнить",
                    new URL("http://vk.com/club.lacucina"),
                    350, 300));
            places.add(new Place(
                    2,
                    "Китай",
                    "Чайный дом по-восточному",
                    "ул. Александра Невского, д. 9",
                    "12:00 - 16:00",
                    new URL("http://chainaydom.ru"),
                    230));
            places.add(new Place(
                    3,
                    "Восемь",
                    "Beef bar Vosem",
                    "пл. Александра Невского, д. 2, гостиница Москва, 8ой этаж",
                    "уточнить",
                    new URL("http://barvosem.ru"),
                    250));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private Places() {
    }
}
