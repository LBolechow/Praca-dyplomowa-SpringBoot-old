package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    public String opis;
    public boolean odczyt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    public Date date;

    @ManyToOne(targetEntity=User.class,fetch = FetchType.LAZY)
    @JsonBackReference
    @JsonIgnore
    private User user;

    public boolean isOdczyt() {
        return odczyt;
    }

    public Notification(String opis, Date date, User usr) {
        this.opis = opis;
        this.date = date;
        this.user = usr;
    }
    @JsonIgnore
    public User getUser() {
        return user;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Notification() {

    }
    public Notification (String opis, boolean odczyt) {
        this.opis = opis;
        this.odczyt = odczyt;
    }

    public void setOpis(String opis) {this.opis = opis;}

    public void setOdczyt(boolean odczyt) {this.odczyt = odczyt;}

    public String getOpis() {return opis;}
    public boolean getOdczyt(){return odczyt;}

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

