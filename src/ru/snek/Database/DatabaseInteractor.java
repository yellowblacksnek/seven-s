package ru.snek.Database;

import ru.snek.Collection.Malefactor;
import static ru.snek.Collection.Malefactor.*;
import ru.snek.Utils.Pair;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class DatabaseInteractor {
    static final String DB_URL = "jdbc:postgresql://localhost:9999/example";
    static final String USER = "postgres";
    static final String PASS = "987123";

    private Connection connection;

    private HashMap<Integer, Condition> conditionHashMap = new HashMap<>();
    private HashMap<Integer, Weight> weightHashMap = new HashMap<>();


    public void connect() throws DBConnectException{
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager
                    .getConnection(DB_URL, USER, PASS);

            String deleteUserTable = "Drop table users";
            String createUserTable = "Create table if not exists users(id SERIAL PRIMARY KEY,login TEXT NOT NULL UNIQUE, password TEXT NOT NULL, salt TEXT NOT NULL, email TEXT NOT NULL UNIQUE)";
            String createCollectionTable =
                    "Create table if not exists collection(id SERIAL PRIMARY KEY, " +
                            "map_key TEXT NOT NULL UNIQUE, " +
                            "mf_name TEXT NOT NULL, " +
                            "mf_age INTEGER NOT NULL, " +
                            "mf_x INTEGER NOT NULL, " +
                            "mf_y INTEGER NOT NULL, " +
                            "mf_birthDate DATE NOT NULL," +
                            "mf_conditionId INTEGER NOT NULL, " +
                            "mf_abToLiftId INTEGER NOT NULL, " +
                            "mf_canSleep BOOLEAN NOT NULL, " +
                            "login TEXT NOT NULL)";
            //try (PreparedStatement ps = connection.prepareStatement(deleteUserTable)) { ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement(createUserTable)) { ps.executeUpdate(); }
            try (PreparedStatement ps = connection.prepareStatement(createCollectionTable)) { ps.executeUpdate(); }
            createConditionTable();
            createWeightTable();
        } catch (ClassNotFoundException e) {
            throw new DBConnectException("Не удалось загрузить PostgreSQL драйвер.");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DBConnectException("Не удалось подключиться к БД.\n" + e.getMessage());
        }
    }

    private void createConditionTable() throws SQLException{
        boolean exists = false;
        String checkExistense = "select * from information_schema.tables where table_schema='public' AND table_name='condition'";
        try (PreparedStatement ps = connection.prepareStatement(checkExistense)) {
            ResultSet rs = ps.executeQuery();
            exists = rs.next();
        }
        if(!exists) {
            String createConditionTable = "Create table condition(id SERIAL PRIMARY KEY, c_name TEXT NOT NULL UNIQUE)";
            try (PreparedStatement ps = connection.prepareStatement(createConditionTable)) {
                ps.executeUpdate();
            }
            String st = "INSERT INTO condition(c_name) VALUES(?)";
            try (PreparedStatement ps = connection.prepareStatement(st)) {
                for (Malefactor.Condition con : Malefactor.Condition.values()) {
                    ps.setString(1, con.toString());
                    ps.executeUpdate();
                }
            }
        }
        String getAll = "SELECT * FROM condition";
        try (PreparedStatement ps = connection.prepareStatement(getAll)) {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                String c_name = rs.getString("c_name");
                conditionHashMap.put(id, Condition.valueOf(c_name));
            }
        }
    }

    private void createWeightTable() throws SQLException{
        boolean exists = false;
        String checkExistense = "select * from information_schema.tables where table_schema='public' AND table_name='weight'";
        try (PreparedStatement ps = connection.prepareStatement(checkExistense)) {
            ResultSet rs = ps.executeQuery();
            exists = rs.next();
        }
        if(!exists) {
            String createWeightTable = "Create table weight(id SERIAL PRIMARY KEY, w_name TEXT NOT NULL UNIQUE)";
            try (PreparedStatement ps = connection.prepareStatement(createWeightTable)) {
                ps.executeUpdate();
            }
            String st = "INSERT INTO weight(w_name) VALUES(?)";
            try (PreparedStatement ps = connection.prepareStatement(st)) {
                for (Malefactor.Weight weight : Malefactor.Weight.values()) {
                    ps.setString(1, weight.toString());
                    ps.executeUpdate();
                }
            }
        }
        String getAll = "SELECT * FROM weight";
        try (PreparedStatement ps = connection.prepareStatement(getAll)) {
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                String w_name = rs.getString("w_name");
                weightHashMap.put(id, Weight.valueOf(w_name));
            }
        }
    }

    public boolean checkField(String s, String fieldName) {
        String SQL = "SELECT * FROM users WHERE "+fieldName+" = ?";
        try (PreparedStatement st = connection.prepareStatement(SQL)){
            st.setString(1, s);
            ResultSet rs = st.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public String getField(String login, String fieldName) {
        String SQL = "SELECT "+fieldName+" FROM users WHERE login = ?";
        try (PreparedStatement st = connection.prepareStatement(SQL)){
            st.setString(1, login);
            ResultSet rs = st.executeQuery();
            if(rs.next()) {
                return rs.getString(fieldName);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public void addUserToDB(String login, String password, String salt, String email) {
        String SQL = "INSERT INTO users(login, password, salt, email) VALUES(?, ?, ?, ?)";
        try (PreparedStatement st = connection.prepareStatement(SQL)){
            st.setString(1, login);
            st.setString(2, password);
            st.setString(3, salt);
            st.setString(4, email);
            st.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Pair<Map<String, Malefactor>, Map<String, String>> loadCollection() throws SQLException{
        String SQL = "SELECT * FROM collection";
        Map<String, Malefactor> collection = new ConcurrentSkipListMap<>();
        Map<String, String> associations = new ConcurrentHashMap<>();
        try (PreparedStatement st = connection.prepareStatement(SQL)){
            ResultSet rs = st.executeQuery();
            while(rs.next()) {
                String key = rs.getString("map_key");

                Malefactor mf = new Malefactor();
                mf.setName(rs.getString("mf_name"));
                mf.setAge(rs.getInt("mf_age"));
                mf.setX(rs.getInt("mf_x"));
                mf.setY(rs.getInt("mf_y"));
                mf.setBirthDate(rs.getTimestamp("mf_birthDate").toLocalDateTime());
                mf.setCondition(conditionHashMap.get(rs.getInt("mf_conditionId")));
                mf.setAbilityToLift(weightHashMap.get(rs.getInt("mf_abToLiftId")));
                mf.setCanSleep(rs.getBoolean("mf_canSleep"));

                String owner = rs.getString("login");
                collection.put(key, mf);
                associations.put(key, owner);
            }
        }
        return new Pair<>(collection, associations);
    }

    public void saveCollection(ArrayList<String[]> list) throws SQLException {
        String a = "INSERT INTO collection(map_key, mf_name, mf_age, mf_x, mf_y, mf_birthDate, mf_conditionId, mf_abToLiftId, mf_canSleep, login) " +
                "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        connection.prepareStatement("TRUNCATE TABLE collection").executeUpdate();
        for (String[] arr : list) {
            try (PreparedStatement st = connection.prepareStatement(a)) {
                st.setString(1, arr[0]);
                st.setString(2, arr[1]);
                st.setInt(3, Integer.valueOf(arr[2]));
                st.setInt(4, Integer.valueOf(arr[3]));
                st.setInt(5, Integer.valueOf(arr[4]));
                st.setTimestamp(6, Timestamp.valueOf(LocalDateTime.ofEpochSecond(Long.valueOf(arr[5]), 0, ZoneOffset.of("+03:00:00"))));
                st.setInt(7, Condition.valueOf(arr[6]).ordinal()+1);
                st.setInt(8, Weight.valueOf(arr[7]).ordinal()+1);
                st.setBoolean(9, Boolean.valueOf(arr[8]));
                st.setString(10, arr[9]);
                st.executeUpdate();
            }
        }
    }
//"VALUES(%s, %s, %d, %d, %d, %t, %d, %d, %b, %s)";
//        map_key TEXT NOT NULL UNIQUE, " +
//        "mf_name TEXT NOT NULL, " +
//                "mf_age INTEGER NOT NULL, " +
//                "mf_x INTEGER NOT NULL, " +
//                "mf_y INTEGER NOT NULL, " +
//                "mf_birthDate DATE NOT NULL," +
//                "mf_conditionId INTEGER NOT NULL, " +
//                "mf_abToLiftId INTEGER NOT NULL, " +
//                "mf_canSleep BOOLEAN NOT NULL, " +
//                "login TEXT NOT NULL)";

}
