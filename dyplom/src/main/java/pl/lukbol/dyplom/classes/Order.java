package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;


    private String clientName;

    private String clientEmail;

    private String phoneNumber;

    private String employeeName;

    private Date startDate;

    private Date endDate;

    private String status;

    private int price;

    private double duration;

    private String idCode;


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Material> materials;

    public Order(String description, String clientName, String clientEmail, String phoneNumber, String employeeName, Date startDate, Date endDate, String status, int price, double duration, List<Material> materials, String idCode) {
        this.description = description;
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.phoneNumber = phoneNumber;
        this.employeeName = employeeName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.price = price;
        this.duration = duration;
        this.materials = materials;
        this.idCode = idCode;
    }

}
