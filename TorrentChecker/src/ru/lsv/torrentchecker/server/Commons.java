/**
 * ����� ��������� ������� � web (������������, �������������� �������� and so on)
 */
package ru.lsv.torrentchecker.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * ����� ��������� ������� � web (������������, �������������� �������� and so
 * on)
 * 
 * @author admin
 */
public class Commons {

	/**
	 * ���� ��� ��������� �������� ���������
	 */
	private static String tempPath;
	/**
	 * ����, ������ �������� ������������� ����������� � checker
	 */
	private static String autoloadPath;
	/**
	 * ����, ��� ������ ��� �������� �� �������� ���� ����������
	 */
	private static String torrentsPath;
	/**
	 * ���� �������������� �������� �������� ������� ��������
	 */
	private static String torrentsAutoloadPath;

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
	public static String getTorrentsAutoloadPath() {
		return torrentsAutoloadPath;
	}

	/**
	 * ������������ �������� ������������ �� ���������� ����� <br>
	 * �� ��������� ������������ �������� �������� �� system property
	 * "storagePath"
	 * 
	 * @param configPath
	 *            ������ ������ ������������. ���� null - ����� �������� ������
	 *            �� system property "storagePath"
	 */
	public static void loadConfig(String configPath) throws ConfigLoadException {
		if (configPath == null) {
			configPath = System.getProperty("storagePath");
		}
		// ��������� ����
		if (configPath == null) {
			throw new ConfigLoadException(
					"Missed config location (via parameter AND via system property)");
		}
		File paths = new File(configPath + File.pathSeparator
				+ "paths.properties");
		if (paths.exists()) {
			Properties props = new Properties();
			try {
				props.load(new FileInputStream(paths));
			} catch (IOException e) {
				// ������ �� ������ - ����� �������
				throw new ConfigLoadException(
						"IOException on loading paths.properties");
			}
			tempPath = props.getProperty("temp");
			autoloadPath = props.getProperty("autoload");
			torrentsPath = props.getProperty("torrents");
			torrentsAutoloadPath = props.getProperty("torrents_autoload");
		}
	}

	/**
	 * ��������������� ����� - ������ ��������� �� ������� �������� ������������
	 * 
	 * @author admin
	 */
	public static class ConfigLoadException extends Exception {

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
