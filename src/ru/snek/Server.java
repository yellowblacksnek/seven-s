package ru.snek;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static ru.snek.Utils.Logger.*;
import static ru.snek.Utils.Utils.*;
import static ru.snek.Response.Status.*;
import static ru.snek.Command.Commands.*;
import static ru.snek.Generator.*;

import ru.snek.Collection.MapWrapper;
import ru.snek.Database.DBConnectException;
import ru.snek.Database.DatabaseInteractor;

public class Server {
    private CommandHandler commandHandler;
    private boolean alive;
//    private DatagramSocket socket;
    private UDPConnection con;
    private Notifier notifier;
    private TokensHandler tokensHandler;
    //private Notifier notifier;
//    private static final int maxBufferSize = 65507;

    public static final DatabaseInteractor DB = new DatabaseInteractor();

    private ConcurrentHashMap<String, LocalDateTime> tokens = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>(); // <token, user>
    private CopyOnWriteArraySet<User> usersOnRegistration = new CopyOnWriteArraySet<>();

    public Server(int port) {
        try {
            con = new UDPConnection(port);
//            SocketAddress a = new InetSocketAddress(port);
//            socket = new DatagramSocket(a);
            DB.connect();
            commandHandler = new CommandHandler(new MapWrapper());
            alive = true;
            notifier = new Notifier(con, users);
            tokensHandler = new TokensHandler(tokens, users, usersOnRegistration, notifier);
            Runtime.getRuntime().addShutdownHook(new Thread(commandHandler::save));
        } catch (DBConnectException | IOException e) {
            e.printStackTrace();
            handleException(e);
            System.exit(1);
        }
    }

    public void loop() {
        try {
            tokensHandler.checkTokens();
            while (!con.isClosed()) {
//                ByteBuffer buf = ByteBuffer.allocate(maxBufferSize);
//                DatagramPacket i = new DatagramPacket(buf.array(), buf.array().length);
//                socket.receive(i);
                DatagramPacket i = con.receive();
                new Thread(() -> {
                    handle(i);
                }).start();
            }
        } catch (IOException e) {
            handleException(e);
        }
    }

//    public void send(Response response, SocketAddress client) throws IOException{
//        byte[] objAsArr = objectAsByteArray(response);
//        int size = objAsArr.length;
//        byte[] sizeArr = ByteBuffer.allocate(10).putInt(size).array();
//        int amount = size <= maxBufferSize ? 1 : (size / maxBufferSize + 1);
//        DatagramPacket o = new DatagramPacket(sizeArr, 10, client);
//        socket.send(o);
//        ByteBuffer bb;
//        if (amount == 1) bb = ByteBuffer.allocate(objAsArr.length);
//        else bb = ByteBuffer.allocate(maxBufferSize);
//        for (int i = 0; i < amount; ++i) {
//            bb.clear();
//            int j;
//            for (j = 0; j < (i < (amount - 1) ? maxBufferSize : size % maxBufferSize); ++j) {
//                bb.put(objAsArr[(i * maxBufferSize) + j]);
//            }
//            while (j < bb.array().length) {
//                bb.put((byte) 0);
//                ++j;
//            }
//            DatagramPacket p = new DatagramPacket(bb.array(), bb.array().length, client);
//            socket.send(p);
//
//            if (i % 50 == 0) {
//                try {
//                    Thread.sleep(100);
//                } catch (InterruptedException e) {
//                }
//            }
//        }
//    }

    private void handle(DatagramPacket packet) {
        try {
            byte[] data = packet.getData();
            SocketAddress client = new InetSocketAddress(packet.getAddress(), packet.getPort());
            //int bufferSize = 2048;

            Command command;
            command = (Command) objectFromByteArray(data);
            Response response = handleCommand(command, client);
            if(response!=null) con.send(response, client);
        } catch (IOException e) {
            handleException(e);
        }
    }

    private Response handleCommand(Command com, SocketAddress client) {
        Command.Commands type = com.getType();
        Response res = null;
        switch(type) {
            case TEST:
                res = commandHandler.handleCommand(com, null);
                break;
            case QUIT:
                if(com.getToken() != null) {
                    tokens.remove(com.getToken());
                    notifier.notifyUsers(users.get(com.getToken()), Notifier.Type.LOGOUT);
                    users.remove(com.getToken());
                }
                break;
            case LOGIN:
                res = handleAuth(com, client);
                break;
            case REGISTER:
                res = handleReg(com, client);
                break;
            default:
                res = handleRealCommand(com);
                break;
        }
        return res;
    }

    private Response handleReg(Command com, SocketAddress client) {
        if(com.getStrData().length == 3) {
            String email = com.getStrData()[0];
            String login = com.getStrData()[1];
            String password = com.getStrData()[2];
            if(DB.checkField(email, "email"))
                return new Response<>(ERROR, REGISTER, "Почта занята.");
            if(DB.checkField(login, "login"))
                return new Response<>(ERROR, REGISTER, "Логин занят.");
            User user = new User(login).setEmail(email).setPassword(password).setAddress(client);
            String regToken = tokensHandler.createRegToken();
            user.setRegistrationToken(regToken);
            user.setRegTokenDate(LocalDateTime.now());
            usersOnRegistration.add(user);
            String token = tokensHandler.createToken();
            user.setToken(token);
            println(regToken);
            boolean success = MailHandler.sendEmail(email, regToken);
            if(success) return new Response<>(OK, REGISTER, token);
            else return new Response<>(ERROR, REGISTER, "Неправильная почта.");
        } else {
            String token = com.getToken();
            User user = null;
            for(User us : usersOnRegistration) {
                if(us.getToken().equals(token)) {
                    user = us;
                    break;
                }
            }
            if(user == null)
                return new Response<>(EXPIRED_TOKEN, REGISTER, null);
            if(user.getRegistrationToken().equals(com.getStrData()[0])) {
                registerUser(user);
                users.put(com.getToken(), user);
                tokens.put(token, LocalDateTime.now());
                return new Response<>(OK, REGISTER, null);
            } else
                return new Response<>(WRONG_TOKEN, REGISTER, null);
        }
    }

    private Response handleAuth(Command com, SocketAddress client) {
        String login = com.getStrData()[0];
        String password = com.getStrData()[1];
        boolean userExists = DB.checkField(login, "login");
        if(userExists) {
            if(users.values().contains(login))
                return new Response<>(ERROR, LOGIN, "Пользователь под этим логином уже авторизирован.");
            if(checkPassword(login, password)) {
                String token = tokensHandler.createToken();
                User thisOne = new User(login).setAddress(client);
                users.put(token, thisOne);
                tokens.put(token, LocalDateTime.now());
                notifier.notifyUsers(thisOne, Notifier.Type.LOGIN);
                return new Response<>(OK, LOGIN, token);
            }
        }
        return new Response<>(ERROR, LOGIN, "Неверное имя пользователя или пароль.");
    }

    private Response handleRealCommand(Command com) {
        String token = com.getToken();
        if(!tokens.containsKey(token))
            return new Response<>(WRONG_TOKEN, com.getType(), null);
        if(tokensHandler.isTokenExpired(token))
            return new Response<>(EXPIRED_TOKEN, com.getType(), null);
        tokensHandler.updateToken(token);
        return commandHandler.handleCommand(com, users.get(token).getLogin());
    }

//    private String createToken() {
//        String token;
//        boolean success = true;
//        do {
//            token = randomString(20);
//            if(users.containsKey(token)) success = false;
//        } while(!success);
//        return token;
//    }
//
//    private String createRegToken() {
//        String token;
//        boolean success = true;
//        do {
//            token = randomString(10);
//            for (User user : usersOnRegistration) {
//                String m_token = user.getRegistrationToken();
//                if(m_token.equals(token)) {
//                    success = false;
//                    break;
//                }
//            }
//        } while(!success);
//        return token;
//    }

    private boolean checkPassword(String login, String password) {
        String realPassword = DB.getField(login, "password");
        String salt = DB.getField(login, "salt");
        String curPassword = hashPassword(password, salt);
        boolean correct = realPassword.equals(curPassword);
        return correct;
    }

    private void registerUser(User user) {
        String salt = randomString(32);
        String pass = hashPassword(user.getPassword(), salt);
        DB.addUserToDB(user.getLogin(), pass, salt, user.getEmail());
    }

//    private boolean isTokenExpired(String token) {
//        LocalDateTime time = tokens.get(token);
//        LocalDateTime now = LocalDateTime.now();
//        return now.compareTo(time.plusMinutes(2).plusSeconds(30)) > 0;
//    }
//
//    private void updateToken(String token) {
//        if(tokens.containsKey(token))
//        tokens.put(token, LocalDateTime.now());
//    }
//
//    private void checkTokens() {
//        new Thread(() -> {
//            try {
//                while (!con.isClosed()) {
//                    Thread.sleep(2000);
//                    ArrayList<String> expired = new ArrayList<>();
//                    for (String token : tokens.keySet()) {
//                        if (isTokenExpired(token)) {
//                            expired.add(token);
//                        }
//                    }
//                    for(String token : expired) {
//                        tokens.remove(token);
//                        User removed = users.remove(token);
//                        notifier.notifyUsers(removed, Notifier.Type.TIMEOUT);
//                    }
//                }
//            } catch (InterruptedException e) { }
//        }).start();
//    }

    public void close() {
        alive = false;
        con.close();
    }

    public boolean isAlive() {return alive;}

}


