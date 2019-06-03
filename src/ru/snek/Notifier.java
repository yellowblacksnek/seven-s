package ru.snek;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ru.snek.Response.Status.EXPIRED_TOKEN;
import static ru.snek.Response.Status.OK;
import static ru.snek.Utils.Logger.handleException;
import static ru.snek.Notifier.Type.*;

public class Notifier {
    enum Type {LOGIN, LOGOUT, TIMEOUT}

    private UDPConnection con;
    private Map<String, User> users;

    public Notifier(UDPConnection con, ConcurrentHashMap<String, User> users) {
        this.con = con;
        this.users = users;
    }

    public void notifyUsers(User theOne, Type type) {
        try {
            String action = null;
            if(type == LOGIN) action = " вошёл.";
            else if(type == LOGOUT) action = " вышел.";
            else if(type == TIMEOUT) action = " вылетел по таймауту.";
            String message = "Пользователь " + theOne.getLogin() + action;
            for (User user : users.values()) {
                if(user.equals(theOne)) continue;
                Response res = new Response<>(OK, message);
                con.send(res, user.getAddress());
            }
            if(type == TIMEOUT)
                con.send(new Response<>(EXPIRED_TOKEN, "Вы были выкинуты с сервера по таймауту."), theOne.getAddress());
        } catch(IOException e) { handleException(e);}
    }
}
