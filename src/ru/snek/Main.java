package ru.snek;

import ru.snek.Collection.MapValuesComparator;

import static ru.snek.Utils.FileInteractor.*;
import static ru.snek.Utils.Logger.*;
import static ru.snek.Utils.Utils.getConsoleInput;

public class Main {
    public static MapValuesComparator.Sorting sorting = MapValuesComparator.Sorting.LOC;
    private static Server server;


    public static void main(String[] args)  {
        checkInitialArgs(args);
        int port = Integer.valueOf(args[0]);
        try {
            server = new Server(port);
        } catch(Exception e) {
            e.printStackTrace();
            errprintln("Не удалось запустить сервер.\n" + e.getMessage());
            System.exit(1);
        }
        createConsoleListener();
        server.loop();
    }

    private static void checkInitialArgs(String[] args) {
        if(args.length < 1) {
            errprintln("Необходимо указать порт.");
            System.exit(1);
        }
        int port = Integer.valueOf(args[0]);
        if(port < 0 || port > 65535) {
            errprintln("Неправильный порт.");
            System.exit(1);
        }
    }

    private static void createConsoleListener() {
        Thread consoleListener =
        new Thread(() -> {
            while(server.isAlive()) {
                String input = getConsoleInput();
                if (input.trim().equals("log")) printLogs();
                else if (input.trim().equals("exit") || input.trim().equals("quit")) server.close();
            }
        });
        consoleListener.start();
    }
}

