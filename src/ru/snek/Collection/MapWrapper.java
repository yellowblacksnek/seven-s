package ru.snek.Collection;

import ru.snek.Main;
import ru.snek.Utils.Pair;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalField;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static ru.snek.Server.DB;
import static ru.snek.Utils.Logger.log;

public class MapWrapper {
    private ConcurrentSkipListMap<String, Malefactor> collection;
    private ConcurrentHashMap<String, String> assosiations;
    private static final long maxMapSize = 50000;

    private static class EmptyFileException extends Exception {}
    private static class CollectionOverflowException extends Exception {
        @Override
        public String getMessage() {
            return "Коллекция достигла максимального размера: " + maxMapSize +".";
        }
    }

    public MapWrapper() {
        System.out.println(load());
    }

    public String addElement(String key, Malefactor element, String login) throws Exception {
        if(element == null) {
            throw new Exception("Элемент почему-то null.");
        }
        String message = "Элемент добавлен!";
        if (collection.containsKey(key)) {
            message = "Ключ уже используется.";
            if(assosiations.get(key).equals(login)) {
                message += " Значение переписано.";
                collection.put(key, element);
            }
        }
        else if(collection.size() >= maxMapSize) {
            throw new Exception("Коллекция достигла максимального размера: " + maxMapSize + ".");
        }
        else {
            collection.put(key, element);
            assosiations.put(key, login);

        }
        return message;
    }

    /*private void addElementNoCheck (String str) throws Exception {
        if (str.equals("")) {
            throw new EmptyFileException();
        }
        Malefactor mf = elementFromString(str);
        String key = str.substring(1, str.indexOf(' ') - 2);
        if(collection.size() >= maxMapSize) {
            throw new CollectionOverflowException();
        }
        if(mf == null) {
            throw new Exception("Элемент почему-то null.");
        }
        collection.put(key, mf);
    }*/

    public synchronized Pair<Boolean, Exception> addElements(ArrayList<String> list, String login) {
        int counter = 0;
        try {
            for (String str : list) {
                try {
                    Malefactor mf = elementFromString(str);
                    String key = str.substring(1, str.indexOf(' ') - 2);
                    addElement(key, mf, login);
                    ++counter;
                } catch (EmptyFileException e) {}
            }
        } catch (Exception e) {
            return new Pair<>(counter > 0, e);
        }
        return new Pair<>(counter > 0, null);
    }

    private static Malefactor elementFromString(String str) throws Exception {
        String[] arr = str.split(";");
        if (arr.length != 9) {
            throw new Exception("Неверный формат данных!");
        }
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = arr[i].trim();
        }
        Malefactor mf = new Malefactor();
        mf.setName(arr[1].substring(1, arr[1].length() - 1));
        mf.setAge(Integer.valueOf(arr[2]));
        mf.setX(Integer.valueOf(arr[3]));
        mf.setY(Integer.valueOf(arr[4]));
        mf.setBirthDate(LocalDateTime.ofEpochSecond(Long.valueOf(arr[5]),0,ZoneOffset.of("+03:00:00")));
        mf.setCondition(Malefactor.Condition.valueOf(arr[6]));
        mf.setAbilityToLift(Malefactor.Weight.valueOf(arr[7]));
        mf.setCanSleep(Boolean.valueOf(arr[8]));
        return mf;
    }

    private String[] elementToString(String key) {
        Malefactor mf = collection.get(key);
        String arr[] = new String[10];
        arr[0] = key;
        arr[1] = mf.getName();
        arr[2] = Integer.toString(mf.getAge());
        arr[3] = Integer.toString(mf.getX());
        arr[4] = Integer.toString(mf.getY());
        arr[5] = Long.toString(mf.getBirthDate().toEpochSecond(ZoneOffset.of("+03:00:00")));
        arr[6] = mf.getCondition().name();
        arr[7] = mf.getAbilityToLift().name();
        arr[8] = Boolean.toString(mf.isCanSleep());
        arr[9] = assosiations.get(key);
        /*StringBuilder str = new StringBuilder();
        for(int i = 0; i < arr.length; ++i) {
            str.append(arr[i]);
            if(i < arr.length-1) str.append("; ");
        }*/
        return arr;
    }

    public ConcurrentSkipListMap<String, Malefactor> getCollection() {return collection;}

    public ConcurrentSkipListMap<String, Malefactor> show() {
        MapValuesComparator comp = new MapValuesComparator(collection, Main.sorting);
        ConcurrentSkipListMap<String, Malefactor> sorted = new ConcurrentSkipListMap<>(comp);
        sorted.putAll(collection);
        return sorted;
    }

    public synchronized String clear() {
        collection.clear();
        return "Коллекция очищена.";
    }

    public synchronized String save() {
        ArrayList<String[]> arr = new ArrayList<>();
        collection.keySet().stream().map(this::elementToString).forEach(arr::add);
        //boolean success = toFile(arr, file);
        //if (success) return "Данные сохранены.";
        try {
            DB.saveCollection(arr);
        } catch (SQLException e) {
            return "Произошла ошибка при работе с БД.";
        }
        return "Данные сохранены.";
    }

    public synchronized String load() {
        Pair<Map<String, Malefactor>, Map<String, String>> pair;
        try {
            pair = DB.loadCollection();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Произошла ошибка при работе с БД.";
        }
        collection = (ConcurrentSkipListMap<String, Malefactor>) pair.getFirst();
        assosiations = (ConcurrentHashMap<String, String>) pair.getSecond();
        return "Коллекция загружена из БД.";
    }

    public String info() {
        String message = "Тип коллекции: " + collection.getClass().toString().substring(16) + ".\n" +
        "Колличество элементов в коллекции: " + collection.size() + ".";
		return message;
    }

    public synchronized String removeByKey(String key, String login){
        if(!collection.containsKey(key)) return "Нет элемента с таким ключом.";
        if(!assosiations.get(key).equals(login)) return "Вы не можете удалить это.";
        collection.remove(key);
        assosiations.remove(key);
        return "Элемент удалён.";
    }

    public synchronized String removeGreaterKey(String keyToCompare, String login) {
        ArrayList<String> keysToRemove = new ArrayList<>();
        collection.keySet().stream().filter(i -> i.compareTo(keyToCompare) > 0).forEach(keysToRemove::add);
        List<String> keysNotRemoved = new ArrayList<>();
        keysToRemove.stream().forEach((i) -> {
            if(assosiations.get(i).equals(login)) {
                collection.remove(i);
                assosiations.remove(i);
            }
            else keysNotRemoved.add(i);
        });
        if(keysToRemove.size() == 0) return "Ни один элемент не был удален.";
        if(!keysNotRemoved.isEmpty()) return "Некоторые элементы не были удалены.";
        return "Удалено всё, что нужно.";
    }
}
