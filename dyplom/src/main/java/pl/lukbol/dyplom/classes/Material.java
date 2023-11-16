package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Value;

@Entity
@Table(name="materials")
public class Material {
    @Id
    @GeneratedValue
    private  Long id;

    private String item;

    private boolean checked;

    public Material() {
    }

    public Material(Long id, String item, Order order, Boolean checked) {
        this.id = id;
        this.item = item;
        this.order = order;
        this.checked=checked;
    }

    public Material(String item, Order order, Boolean checked) {
        this.item = item;
        this.order = order;
        this.checked=checked;
    }

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;


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


    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
