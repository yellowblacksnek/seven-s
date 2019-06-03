package ru.snek;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static ru.snek.Generator.randomString;

public class TokensHandler {

    private Map<String, LocalDateTime> tokens;
    private Map<String, User> users; // <token, user>
    private CopyOnWriteArraySet<User> usersOnRegistration;
    private Notifier notifier;

    public TokensHandler(Map<String, LocalDateTime> tokens, Map<String, User> users, CopyOnWriteArraySet<User> usersOnRegistration, Notifier notifier) {
        this.tokens = tokens;
        this.users = users;
        this.usersOnRegistration = usersOnRegistration;
        this.notifier = notifier;
    }

    public boolean isTokenExpired(String token) {
        LocalDateTime time = tokens.get(token);
        LocalDateTime now = LocalDateTime.now();
        return now.compareTo(time.plusMinutes(2).plusSeconds(30)) > 0;
    }

    public void updateToken(String token) {
        if(tokens.containsKey(token))
            tokens.put(token, LocalDateTime.now());
    }

    public void checkTokens() {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(2000);
                    ArrayList<String> expired = new ArrayList<>();
                    for (String token : tokens.keySet()) {
                        if (isTokenExpired(token)) {
                            expired.add(token);
                        }
                    }
                    for(String token : expired) {
                        tokens.remove(token);
                        User removed = users.remove(token);
                        notifier.notifyUsers(removed, Notifier.Type.TIMEOUT);
                    }
                }
            } catch (InterruptedException e) { }
        }).start();
    }

    public String createToken() {
        String token;
        boolean success = true;
        do {
            token = randomString(20);
            if(users.containsKey(token)) success = false;
        } while(!success);
        return token;
    }

    public String createRegToken() {
        String token;
        boolean success = true;
        do {
            token = randomString(10);
            for (User user : usersOnRegistration) {
                String m_token = user.getRegistrationToken();
                if(m_token.equals(token)) {
                    success = false;
                    break;
                }
            }
        } while(!success);
        return token;
    }
}
