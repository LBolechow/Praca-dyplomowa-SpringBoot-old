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

    public String description;
    public String creator;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    public Date date;

    @ManyToOne(targetEntity=User.class,fetch = FetchType.LAZY)
    @JsonBackReference
    @JsonIgnore
    private User user;



    public Notification(String description, Date date, User usr, String creator) {
        this.description = description;
        this.date = date;
        this.user = usr;
        this.creator = creator;
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
    public Notification (String description) {
        this.description = description;
    }

    public void setDescription(String description) {this.description = description;}


    public String getDescription() {return description;}

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}

