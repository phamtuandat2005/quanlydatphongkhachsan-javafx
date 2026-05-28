package connectDatabase;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectDatabase {
	private static Connection con = null;
	private static ConnectDatabase instance = new ConnectDatabase();

	private final String URL = "jdbc:sqlserver://localhost:1433;databaseName=LuciaHT;encrypt=true;trustServerCertificate=true;";
	private final String USER = "sa";
	private final String PASSWORD = "sapassword";

	private ConnectDatabase() {
		// Constructor remains empty as connections are managed per request in getConnection()
	}

	public static ConnectDatabase getInstance() {
		return instance;
	}

	public synchronized Connection getConnection() {
		try {
			return DriverManager.getConnection(URL, USER, PASSWORD);
		} catch (SQLException e) {
			System.err.println("Lỗi kết nối Database: " + e.getMessage());
		}
		return null;
	}

	public void disconnect() {
		if (con != null) {
			try {
				con.close();
				System.out.println("Đã ngắt kết nối Database.");
			} catch (SQLException e) {
				System.out.println("Lỗi ngắt kết nối. Vui long kiểm tra lại.");
				e.printStackTrace();
			}
		}
	}
}
