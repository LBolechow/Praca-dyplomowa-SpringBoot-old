package pl.lukbol.dyplom.classes;

import jakarta.persistence.*;

@Entity
@Table(name="price")
public class Price {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private  Long id;

    private String item;

    private String price;

    public Price() {
    }

    public Price(String item, String price) {
        this.item = item;
        this.price = price;
    }

    public Price(Long id, String item, String price) {
        this.id = id;
        this.item = item;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}
