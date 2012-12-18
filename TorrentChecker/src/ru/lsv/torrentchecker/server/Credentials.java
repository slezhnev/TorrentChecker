/**
 * Класс - хранитель имен пользователей и паролей к сайтам 
 */
package ru.lsv.torrentchecker.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import ru.lsv.torrentchecker.shared.User;

/**
 * Класс - хранитель имен пользователей и паролей к сайтам
 * 
 * @author s.lezhnev
 */
public class Credentials {

	private static Credentials instance = null;

	/**
	 * Синглтон <br/>
	 * Методы - thread safe, так что перебдим <br/>
	 * 
	 * @return Экземпляр объекта
	 */
	public static synchronized Credentials getInstance() {
		if (instance == null) {
			instance = new Credentials();
		}
		return instance;
	}

	/**
	 * Загружает список
	 * 
	 * @param credentialsFile
	 *            Файл, откуда загружать
	 * @return Загруженный список имен пользователей / паролей с сайтами
	 * @throws IOException
	 *             При ошибке ввода - вывода
	 * @throws InvalidPropertiesFormatException
	 *             При кривом формате файла
	 */
	public synchronized Map<String, User> loadCredentials(File credentialsFile)
			throws InvalidPropertiesFormatException, IOException {
		Map<String, User> res = Collections
				.synchronizedMap(new HashMap<String, User>());
		if (!credentialsFile.exists()) {
			return res;
		}
		Properties creds = new Properties();
		creds.loadFromXML(new FileInputStream(credentialsFile));
		// Поехали читать
		if (!creds.containsKey("count")) {
			throw new InvalidPropertiesFormatException(
					"Неверный формат файла credentials - отсутствует \"count\"");
		}
		int numCreds;
		try {
			numCreds = Integer.parseInt(creds.getProperty("count"));
		} catch (NumberFormatException e) {
			throw new InvalidPropertiesFormatException(
					"Неверный формат файла credentials - \"count\" не целое число");
		}
		for (int i = 0; i < numCreds; i++) {
			// Читаем имя файла
			if (!creds.containsKey("url" + i)) {
				throw new InvalidPropertiesFormatException(
						"Неверный формат файла credentials - отсутствует имя сайта для записи № "
								+ i);
			}
			if (!creds.containsKey("name" + i)) {
				throw new InvalidPropertiesFormatException(
						"Неверный формат файла credentials - отсутствует имя пользователя для записи № "
								+ i);
			}
			if (!creds.containsKey("password" + i)) {
				throw new InvalidPropertiesFormatException(
						"Неверный формат файла credentials - отсутствует пароль для записи № "
								+ i);
			}
			res.put(creds.getProperty("url" + i),
					new User(creds.getProperty("name" + i), creds
							.getProperty("password" + i)));
		}
		return res;
	}

	/**
	 * Сохраняет список в файл
	 * 
	 * @param credentials
	 *            Список сайтов с юзерами / паролями
	 * @param credentialsFile
	 *            Файл, куда сохранять
	 * @throws IOException
	 *             В случае ошибок записи
	 * @throws FileNotFoundException
	 *             см. FileOutputStream
	 */
	public synchronized void saveCredentials(Map<String, User> credentials,
			File credentialsFile) throws FileNotFoundException, IOException {
		Properties props = new Properties();
		int i = 0;
		Set<String> keys = credentials.keySet();
		synchronized (keys) {
			props.put("count", "" + keys.size());
			for (String url : keys) {
				props.put("url" + i, url);
				User user = credentials.get(url);
				props.put("name" + i, user.getUserName());
				props.put("password" + i, user.getPassword());
			}
		}
		props.storeToXML(new FileOutputStream(credentialsFile), "credentials");
	}
}
