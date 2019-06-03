package ru.snek;

import ru.snek.Collection.Malefactor;
import ru.snek.Collection.MapWrapper;
import ru.snek.Utils.Pair;

import static ru.snek.Command.Commands.*;
import static ru.snek.Response.Status.*;
import java.util.ArrayList;
import java.util.Collection;


import static ru.snek.Utils.Logger.log;

public class CommandHandler {

    private MapWrapper wrapper;

    public CommandHandler(MapWrapper wrapper) {
        this.wrapper = wrapper;
    }

    public Response handleCommand(Command com, String login) {
        if(com == null) log("null");
        switch(com.getType()) {
            case INSERT:
                return insert(com.getStrData()[0],
                        (Malefactor) com.getData(), login);
            case SHOW:
                return show();
            case CLEAR:
                return clear();
            case SAVE:
                return save();
            case INFO:
                return info();
            case REMOVE:
                return remove(com.getStrData()[0], login);
            case REMOVE_GREATER_KEY:
                return removeGreaterKey(com.getStrData()[0], login);
            case HELP:
                if (com.getStrData() == null) return help(false);
                else return help(true);
            case TEST:
                return test();
            case IMPORT:
                return importCom(com.getStrData()[0], login);
            case LOAD:
                return load();
            default:
                break;
        }
        return null;
    }

    private Response help(boolean insert) {
        String data;
        if(!insert)
            data =  "Список комманд:\n" +
                    "help: список всех комманд\n" +
                    "help insert: информация про команду insert\n" +
                    "insert \"String key\" {element}: добавить новый элемент с заданным ключом\n" +
                    "import path: импорт из файла со стороны клиента\n" +
                    "show: вывести все элементы коллекции в строковом представлении\n" +
                    "clear: очистить коллекцию\n" +
                    "save: сохранить коллекцию в файл\n" +
                    "load: загрузить коллекцию из файла на сервере\n" +
                    "info: вывести информацию о коллекции (тип, количество элементов)\n" +
                    "remove \"String key\": удалить элемент из коллекции по его ключу\n" +
                    "remove_greater_key \"String key\": удалить из коллекции все элементы, ключ которых превышает заданный\n" +
                    "quit, exit: завершение";
        else data = "insert \"String key\" {element}: добавить новый элемент с заданным ключом.\n" +
                "Элемент вводить в формате json. При отсутсвии значения того или иного поля \nбудет использовано значение по умолчанию или случайно сгенерированные данные\n"+
                "Далее приведены значения полей по умолчанию, если их не прописывать:\n"+
                "name : Безымянный\n" +
                "age : случайно\n"+
                "x : случайно\n" +
                "y : случайно\n" +
                "birthDate : текущие дата и время\n"+
                "condition : AWAKEN\n" +
                "canSleep : false\n" +
                "abilityToLift : HEAVY";
        return new Response<>(HELP, data);
    }

    public Response insert(String key, Malefactor element, String login) {
        Response res = new Response<>(INSERT, null);
        String messageString;
        try {
            messageString = wrapper.addElement(key, element, login);
        } catch (Exception e) {
            e.printStackTrace();
            messageString = "Элемент не добавлен:\n" + e.getMessage();
        }
        res.setData(messageString);
        return res;
    }

    public Response show() {
        return new Response<>(SHOW, wrapper.show());
    }

    public Response clear() {
        return new Response<>(CLEAR, wrapper.clear());
    }

    public Response save() {
        return new Response<>(SAVE, wrapper.save());
    }

    public Response info() {
        return new Response<>(INFO, wrapper.info());
    }

    public Response remove(String key, String login) {
        String data = wrapper.removeByKey(key, login);
        return new Response<>(REMOVE, data);
    }

    public Response removeGreaterKey(String key, String login) {
        return new Response<>(REMOVE_GREATER_KEY, wrapper.removeGreaterKey(key, login));
    }

	private Response  test() {
        return new Response<>(TEST, null);
    }

    private Response importCom(String fileStr, String login) {
        String data;
        ArrayList<String> lines = new ArrayList<>();
        for (String str : fileStr.split("\n")) {
            lines.add(str);
        }
        Pair<Boolean, Exception> result = wrapper.addElements(lines, login);
        if (result.getSecond() == null) {
            data = "Данные импортированы.";
        } else {
            data = "Произошла ошибка: \n" +
                    result.getSecond().getMessage() + "\n";
            if (result.getFirst()) {
                data += "Часть данных импортирована.";
            } else data += "Данные не импортированы.";
        }
        return new Response<>(IMPORT, data);
    }

    private Response load() {
        return new Response<>(LOAD, wrapper.load());
    }

}
