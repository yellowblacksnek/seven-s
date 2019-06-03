package ru.snek;

import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private String email = null;
    private String login;
    private String password = null;
    private String token;
    private String registrationToken = null;
    private LocalDateTime regTokenDate = null;
    private SocketAddress address;
    private int attempts;

    public User(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public String getEmail() {
        return email;
    }

    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }

    public User setToken(String token) {
        this.token = token;
        return this;
    }

    public String getToken() {
        return token;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public User setRegistrationToken(String registrationToken) {
        this.registrationToken = registrationToken;
        return this;
    }

    public LocalDateTime getRegTokenDate() {
        return regTokenDate;
    }

    public User setRegTokenDate(LocalDateTime regTokenDate) {
        this.regTokenDate = regTokenDate;
        return this;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public User setAddress(SocketAddress address) {
        this.address = address;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return login.equals(user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }
}
