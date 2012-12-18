/**
 * Общие настройки сервиса и web (конфигурация, контролируемые торренты and so on)
 */
package ru.lsv.torrentchecker.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import ru.lsv.torrentchecker.shared.User;
import ru.lsv.torrentchecker.shared.WorkingResult;

/**
 * Общие настройки сервиса и web (конфигурация, контролируемые торренты and so
 * on)
 * 
 * @author admin
 */
public class Commons {

	/**
	 * Путь для временной загрузки торрентов
	 */
	private static String tempPath;
	/**
	 * Путь, откуда торренты автоматически загружаются в checker
	 */
	private static String autoloadPath;
	/**
	 * Путь, где лежать все торренты за которыми идет наблюдение
	 */
	private static String torrentsPath;
	/**
	 * Путь автоматический загрузки торрента торрент клиентом
	 */
	private static String torrentsInQueue;
	/**
	 * Куда отправлять email'ы с оповещением. Может отсутствовать
	 */
	private static String sendEmailTo;
	/**
	 * Имена пользователей с паролями
	 */
	private static volatile Map<String, User> credentials;
	private static volatile User mailCredentials;
	/**
	 * Имя директории, где лежит конфигурация
	 */
	private static String configPath;
	/**
	 * Результат обработки списка торрентов
	 */
	private static volatile WorkingResult workingResult;

	/**
	 * @return the tempPath
	 */
	public static String getTempPath() {
		return tempPath;
	}

	/**
	 * @return the autoloadPath
	 */
	public static String getAutoloadPath() {
		return autoloadPath;
	}

	/**
	 * @return the torrentsPath
	 */
	public static String getTorrentsPath() {
		return torrentsPath;
	}

	/**
	 * @return the torrentsAutoloadPath
	 */
	public static String getTorrentsInQueue() {
		return torrentsInQueue;
	}

	/**
	 * @return the credentials
	 */
	public static Map<String, User> getCredentials() {
		return credentials;
	}

	/**
	 * @return the mail credentials
	 */
	public static User getMailCredentials() {
		return mailCredentials;
	}

	/**
	 * @param creds
	 *            the credentials to set
	 */
	public static void setCredentials(Map<String, User> creds) {
		credentials = creds;
	}

	/**
	 * @return the workingResult
	 */
	public static WorkingResult getWorkingResult() {
		return workingResult;
	}

	/**
	 * @param workingResult
	 *            the workingResult to set
	 */
	public static void setWorkingResult(WorkingResult workingResult) {
		Commons.workingResult = workingResult;
	}

	/**
	 * @return the sendEmailTo
	 */
	public static String getSendEmailTo() {
		return sendEmailTo;
	}

	/**
	 * Осуществляет загрузку конфигурации из указанного места <br>
	 * По умолчанию конфигурация пытается читаться из system property
	 * "storagePath"
	 * 
	 * @param confPath
	 *            Откуда читать конфигурацию. Если null - будет пытаться читать
	 *            из system property "storagePath"
	 */
	public static void loadConfig(String confPath) throws ConfigLoadException {
		if (confPath == null) {
			confPath = System.getProperty("storagePath");
		}
		// Загружаем пути
		if (confPath == null) {
			throw new ConfigLoadException(
					"Missed config location (via parameter AND via system property)");
		}
		configPath = confPath;
		File paths = new File(confPath + File.separator + "paths.properties");
		if (paths.exists()) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(paths));
			} catch (IOException e) {
				// Ничего не грузим - молча выходим
				throw new ConfigLoadException(
						"IOException on loading paths.properties");
			}
			try {
				tempPath = new File(props.getProperty("temp"))
						.getCanonicalPath() + File.separator;
				autoloadPath = new File(props.getProperty("autoload"))
						.getCanonicalPath() + File.separator;
				torrentsPath = new File(props.getProperty("torrents"))
						.getCanonicalPath() + File.separator;
				torrentsInQueue = new File(
						props.getProperty("torrents_inqueue"))
						.getCanonicalPath()
						+ File.separator;
				sendEmailTo = props.getProperty("mail_to");
			} catch (IOException e) {
				throw new ConfigLoadException("Failed to normalize paths");
			}
		}
		try {
			credentials = Credentials.getInstance().loadCredentials(
					new File(configPath + File.separator
							+ "credentials.properties"));
			mailCredentials = Credentials.getInstance().getMailCredentials();
		} catch (IOException e) {
			// Rethrow под другим именем
			throw new ConfigLoadException(e.getMessage());
		}
	}

	/**
	 * Сохраняет имена пользователей и пароли
	 * 
	 * @throws IOException
	 *             В случае ошибки записи
	 */
	public static void saveCredentials() throws IOException {
		Credentials.getInstance()
				.saveCredentials(
						credentials,
						new File(configPath + File.separator
								+ "credentials.properties"));
	}

	/**
	 * Вспомогательный класс - выдача сообщений об ошибках загрузки конфигурации
	 * 
	 * @author admin
	 */
	public static class ConfigLoadException extends Exception {

		/**
		 * For serialization
		 */
		private static final long serialVersionUID = -8740996439183442067L;

		/**
		 * Default constructor
		 * 
		 * @param arg0
		 */
		public ConfigLoadException(String arg0) {
			super(arg0);
		}

	}

}
