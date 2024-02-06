package pl.lukbol.dyplom.classes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

import java.util.Collection;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")Long id;
    public String name;
    @Column(unique=true)
    @NotNull
    @NotEmpty
    public String email;
    public String password;

    public String code;

    boolean activated;
    boolean enabled;

    @OneToMany(targetEntity=Notification.class,cascade = CascadeType.ALL,
            fetch = FetchType.LAZY, mappedBy = "user")
    private List<Notification> notifications = new ArrayList<Notification>();

    @ManyToMany
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    @JsonIgnore
    private Collection<Role> roles;

    @ManyToMany(mappedBy = "participants", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Conversation> conversations;

    public List<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
    }

    public User() {}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public User(Long id, String name, String email, String password, String code, Boolean enabled, Boolean activated) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.code=code;
        this.enabled=enabled;
        this.activated=activated;
    }



    public User(String name, String email, String password, String code, Boolean enabled, Boolean activated) {
        this.email = email;
        this.name = name;
        this.password=password;
        this.code=code;
        this.enabled=enabled;
        this.activated=activated;
    }
    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Collection<Role> getRoles() {
        return roles;
    }

    public void setRoles(Collection<Role> roles) {
        this.roles = roles;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }
}
