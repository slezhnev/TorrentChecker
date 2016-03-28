package ru.lsv.torrentchecker.shared;

/**
 * Вспомогательный класс, обеспечивающий хранение пары username - password
 * 
 * @author admin
 * 
 */
public class User {

	/**
	 * Имя пользователя
	 */
	private String userName;
	/**
	 * Пароль
	 */
	private String password;

	/**
	 * Default constructor
	 * 
	 * @param userName
	 * @param password
	 */
	public User(String userName, String password) {
		this.userName = userName;
		this.password = password;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

}