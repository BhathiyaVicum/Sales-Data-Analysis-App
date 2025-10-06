/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package db;

import java.sql.Connection;
import java.sql.DriverManager;

public class db {
    
    public static void main(String[] args) {
        
        String url = "jdbc:mysql://localhost:3306/testdb"; // replace 'testdb' with your DB name
        String user = "root";  // phpMyAdmin default username
        String password = "";  // leave blank if no password (XAMPP default)

        try {
            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("Database connected successfully!");

            conn.close(); // close connection
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
