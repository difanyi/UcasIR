package com.news.db;

import java.sql.Connection;     
import java.sql.DriverManager;       
import java.sql.SQLException;


public class JDBCUtil {
	private static Connection conn = null;
	private static final String URL = "jdbc:mysql:"
			+ "//127.0.0.1/news?autoReconnect=true&characterEncoding=utf8";
	private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
	private static final String USER_NAME = "root";
	private static final String PASSWORD = "qin123456";
	
	public static Connection getConnection() {
		try {
			Class.forName(JDBC_DRIVER);
			System.out.println("���ݿ��������سɹ���");
			conn = DriverManager.getConnection(URL,USER_NAME,PASSWORD);
			System.out.println("���ݿ����ӳɹ���");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}
	
	/**
	 * ���ݿ����Ӳ���
	 */
//	public static void main(String[] args) {
//		Connection connection = JDBCUtil.getConnection();//�����������ݿ�ķ���
//		try {
//			connection.close();
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
//		
//	}
}
