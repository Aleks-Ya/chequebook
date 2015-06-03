package chequebook;

import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.vaadin.addon.touchkit.server.TouchKitServlet;
import com.vaadin.addon.touchkit.ui.NumberField;
import com.vaadin.addon.touchkit.ui.TabBarView;
import com.vaadin.addon.touchkit.ui.VerticalComponentGroup;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Property;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletRequest;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import javax.servlet.annotation.WebServlet;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.vaadin.server.FontAwesome.ARROW_CIRCLE_O_LEFT;
import static com.vaadin.server.FontAwesome.ARROW_CIRCLE_RIGHT;
import static java.math.BigDecimal.ZERO;
import static java.text.DecimalFormatSymbols.getInstance;
import static java.time.ZoneOffset.ofTotalSeconds;
import static java.util.Locale.GERMAN;

/**
 * Created by rurik
 */
@Theme("touchkit")
@Widgetset("com.vaadin.addon.touchkit.gwt.TouchKitWidgetSet")
public class ChequebookUI extends UI {
    BeanItemContainer<Person> personContainer = new BeanItemContainer<>(Person.class, Bank.instance.getPersons());
    BeanItemContainer<Place> placeContainer = new BeanItemContainer<>(Place.class, Places.instance.places);
    PersonTable personTable = new PersonTable();
    NewTransactionForm form = new NewTransactionForm();
    TransactionTable transactionTable = new TransactionTable();
    Person me;
    TabBarView tabBarView;

    @Override
    public void init(VaadinRequest request) {
        try {
            String ctx = ((VaadinServletRequest) request).getRequestURI().substring(1);
            if (Bank.instance.isAdmin(ctx)) {
                setContent(new VerticalLayout() {{
                    setSizeFull();
                    addComponent(new VerticalComponentGroup() {{
                        TextField username = new TextField("New user name");
                        addComponent(username);
                        addComponent(new Button("Create user", clickEvent -> {
                            if (!Strings.isNullOrEmpty(username.getValue())) {
                                open(Bank.instance.addPerson(username.getValue()).getKey());
                            }
                        }));
                        addComponent(new Button("Regenerate admin key", clickEvent -> {
                            open(Bank.instance.generateAdminKey());
                        }));
                    }});
                    addComponent(new Table() {{
                        setSizeFull();
                        setContainerDataSource(personContainer);
                        addGeneratedColumn("user", (source, itemId, columnId) -> {
                            Person person = (Person) itemId;
                            return new Link(person.getName(), new ExternalResource(
                                    Page.getCurrent().getLocation() + "/../" + person.getKey()));
                        });
                        setPageLength(personContainer.size());
                        setVisibleColumns("user");
                        setColumnHeaders("Users");
                    }});
                    setExpandRatio(getComponent(getComponentCount() - 1), 1);
                }});
            } else {
                me = Bank.instance.findPerson(ctx);
                getPage().setTitle("CheckBook - " + me.getName());
                tabBarView = new TabBarView() {{
                    addTab(personTable, "All", FontAwesome.LIST_OL);
                    addTab(form, "Add", FontAwesome.PLUS);
                    addTab(transactionTable, "My transactions", FontAwesome.TABLE);
                    addListener((SelectedTabChangeEvent event) -> {
                        Component component = event.getTabSheet().getSelelectedTab().getComponent();
                        if (component == personTable) {
                            personTable.load();
                        }
                        if (component == transactionTable) {
                            transactionTable.load();
                        }
                    });
                }};
                setContent(tabBarView);
            }
        } catch (Exception e) {
            Notification.show("Not authenticated", Notification.Type.ERROR_MESSAGE);
            close();
        }
    }

    private void open(String key) {
        JavaScript.eval("window.open('" + key + "', '_blank');");
    }

    private class PersonTable extends Table {
        {
            setSizeFull();
            setContainerDataSource(personContainer);
            setVisibleColumns("name", "balance", "change");
            setColumnHeaders("Name", "Balance", "Last 12h");
            setSortContainerPropertyId("balance");
            addGeneratedColumn("change", (source, itemId, columnId) -> {
                BigDecimal ch = ((Person) itemId).getChange();
                return ch.equals(ZERO) ? "" : new DecimalFormat("+0.##;−0.##", getInstance(GERMAN)).format(ch);
            });
            addItemClickListener(event -> {
                tabBarView.setSelectedTab(form);
                form.setValue((Person) event.getItemId());
            });
        }

        public void load() {
            personContainer.removeAllItems();
            personContainer.addAll(Bank.instance.getPersons());
            sort();
        }
    }

    private class NewTransactionForm extends VerticalComponentGroup {
        Property.ValueChangeListener selectPlace = new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Property property = event.getProperty();
                Place place = (Place) property.getValue();
                List<Integer> defaultPrices = place.getDefaultPrices();
                if (!defaultPrices.isEmpty()) {
                    amount.setValue(defaultPrices.get(0).toString());
                }
            }
        };
        NativeSelect place = new NativeSelect("Place", placeContainer) {{
            setNullSelectionAllowed(false);
            setItemCaptionPropertyId("shortTitle");
            addValueChangeListener(selectPlace);
        }};
        NativeSelect peer = new NativeSelect("User", personContainer) {{
            setNullSelectionAllowed(false);
            setItemCaptionPropertyId("name");
        }};
        NumberField amount = new NumberField("Amount");
        TextField comment = new TextField("Comment");
        Button send = new Button("Send", event -> {
            BigDecimal m = parse(amount.getValue());
            Person p = (Person) peer.getValue();
            if (p != null && m.compareTo(ZERO) > 0) {
                Bank.instance.addTransaction(Instant.now(), me, p, m, comment.getValue());
                setValue(null);
                tabBarView.setSelectedTab(transactionTable);
            }
        });

        {
            send.setWidth("100%");
            addComponents(place, amount, peer, comment, send);
        }

        public void setValue(Person person) {
            peer.setValue(person);
            amount.setValue(null);
            comment.setValue("");
        }
    }

    private class TransactionTable extends Table {
        private BeanItemContainer<Transaction> ds = new BeanItemContainer<>(Transaction.class);

        {
            setSizeFull();
            setContainerDataSource(ds);
            setVisibleColumns("created", "peerName", "amount", "comment");
            setColumnHeaders("Created", "Peer", "Amount", "Comment");
            setSortContainerPropertyId("created");
            setSortAscending(false);
            addGeneratedColumn("created", (source, itemId, columnId) -> DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                    .withZone(ofTotalSeconds(Page.getCurrent().getWebBrowser().getTimezoneOffset() / 1000))
                    .format(((Transaction) itemId).getCreated()));
            addGeneratedColumn("amount", (source, itemId, columnId) -> {
                BigDecimal amount = ((Transaction) itemId).getAmount();
                FontAwesome arrow = amount.compareTo(ZERO) < 0 ? ARROW_CIRCLE_O_LEFT : ARROW_CIRCLE_RIGHT;
                return new Label(arrow.getHtml() + " " + amount.abs(), ContentMode.HTML);
            });
        }

        public void load() {
            ds.removeAllItems();
            ds.addAll(me.getTransactions());
            sort();
        }
    }

    private static BigDecimal parse(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception e) {
            return ZERO;
        }
    }

    @WebServlet("/*")
    @VaadinServletConfiguration(productionMode = true, ui = ChequebookUI.class)
    public static class Servlet extends TouchKitServlet {
    }
}