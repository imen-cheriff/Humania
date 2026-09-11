package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;

    private final String URL;
    private final String USERNAME;
    private final String PASSWORD;

    private Connection cnx;

    private MyDataBase() {
        this.URL = PropertiesUtil.getProperty("db.url");
        this.USERNAME = PropertiesUtil.getProperty("db.username");
        this.PASSWORD = PropertiesUtil.getProperty("db.password");

        try {
            cnx = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected .... ");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null)
            instance = new MyDataBase();
        return instance;
    }

    public Connection getCnx() {
        return cnx;
    }
}