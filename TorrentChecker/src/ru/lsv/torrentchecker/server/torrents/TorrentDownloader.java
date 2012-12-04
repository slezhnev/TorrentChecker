/**
 * Класс, обеспечивающий загрузку торрента
 */
package ru.lsv.torrentchecker.server.torrents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.lsv.torrentchecker.server.Commons;
import ru.lsv.torrentchecker.server.torrents.impl.NNMClubDownloader;

import com.turn.ttorrent.bcodec.BDecoder;
import com.turn.ttorrent.bcodec.BEValue;
import com.turn.ttorrent.bcodec.InvalidBEncodingException;

/**
 * Класс, обеспечивающий загрузку торрента
 * 
 * @author admin
 */
public class TorrentDownloader {

	/**
	 * Общий список загрузчиков
	 */
	List<TorrentDownloaderInterface> downloaders = new ArrayList<>();

	/**
	 * Default constructor
	 */
	public TorrentDownloader() {
		// Создаем внутренний список
		downloaders.add(new NNMClubDownloader());
	}

	/**
	 * Осуществляет проверку обновления указанного .torrent-файла <br>
	 * Если файл изменился - то: <br>
	 * 1. Он будет выложен на место torrentFile <br>
	 * 2. Он будет скопирован в autoloadPath <br>
	 * Файлы, не содержащие блок "comment" с валидным URL - просто игнорируются
	 * *
	 * 
	 * @param torrentFile
	 *            .torrent-файл, обновление которого надо проверить
	 * @param credentials
	 *            Имена пользователей и паролей к ресурсам
	 * @param pathToDownload
	 *            Куда загружать временно .torrent-файл
	 * @param autoloadPath
	 *            Куда выкладывать .torrent-файл, если он изменился
	 * @throws IOException
	 *             В случае возникновения проблем с работой
	 */
	public void check(File torrentFile, Map<String, User> credentials,
			String pathToDownload, String autoloadPath) throws IOException,
			TorrentDownloaderException {
		// Первое - парсим .torrent файл
		Map<String, BEValue> decoded = BDecoder.bdecode(
				new FileInputStream(torrentFile)).getMap();
		if (decoded.containsKey("comment")) {
			URL torrentUrl = null;
			try {
				torrentUrl = new URL(decoded.get("comment").getString());
			} catch (MalformedURLException e) {
				throw new TorrentDownloaderException("Malformed URL in comment");
			}
			if (torrentUrl != null) {
				// Достаем из него host
				String torrentHost = torrentUrl.getHost();
				// Первое - ищем credentials
				if (!credentials.containsKey(torrentHost)) {
					throw new TorrentDownloaderException(
							"Cannot find credentials for " + torrentHost);
				}
				// Теперь поедем поищем соответствующую реализацию downloader'а
				TorrentDownloaderInterface implDownloader = null;
				for (TorrentDownloaderInterface impl : downloaders) {
					if ((impl.getResource() != null)
							&& (impl.getResource().equals(torrentHost))) {
						implDownloader = impl;
						break;
					}
				}
				if (implDownloader == null) {
					throw new TorrentDownloaderException(
							"Cannot find downloader for " + torrentHost);
				}
				// Нашли имплементацию. Загружаем файлО во временное место
				// хранения
				String downloadedTorrent = implDownloader.downloadTorrent(
						torrentUrl, credentials.get(torrentHost).getUserName(),
						credentials.get(torrentHost).getPassword(),
						pathToDownload);
				if (downloadedTorrent != null) {
					// Поехали проверим - а он есть на самом дела?
					File downloadedTorrentFile = new File(downloadedTorrent);
					if (downloadedTorrentFile.exists()) {
						// Первое - было бы тупо неплохо сравнить файлы просто
						// по хэшу. Если хэш совпадает - дальше ничего не
						// парсим. Значит файлы совпадают
						if (org.apache.commons.io.FileUtils
								.checksumCRC32(torrentFile) != org.apache.commons.io.FileUtils
								.checksumCRC32(downloadedTorrentFile)) {
							// Отлично. Попарсим и его.
							// В результате - нам надо найти что ж там нового
							Set<String> torrentFiles = getFilesFromDecoded(
									torrentFile, decoded);
							// Парсим новый принятый файл
							Map<String, BEValue> decodedDownloaded = BDecoder
									.bdecode(
											new FileInputStream(
													downloadedTorrentFile))
									.getMap();
							// Получаем список файлов из него
							Set<String> downloadedTorrentFiles = getFilesFromDecoded(
									downloadedTorrentFile, decodedDownloaded);
							List<String> newFiles = new ArrayList<>();
							for (String file : torrentFiles) {
								if (!downloadedTorrentFiles.contains(file)) {
									newFiles.add(file);
								}
							}
							// Проверяем на появление чего-то нового
							if (newFiles.size() > 0) {
								// Отлично! Значит в скачанном торренте есть
								// что-то новое!
								// Первое - запускаем рассылку о том, что
								// скачано новое
								fireMailAnnounce(
										downloadedTorrentFile.getName(),
										newFiles);
								// Удаляем старый файл
								torrentFile.delete();
								// Копируем на его место новый файл
								org.apache.commons.io.FileUtils.copyFile(
										downloadedTorrentFile, torrentFile);
								// Теперь копируем этот же файл в autoload
								org.apache.commons.io.FileUtils.copyFile(
										torrentFile,
										new File(Commons.getAutoloadPath()
												+ File.pathSeparator
												+ torrentFile.getName()));
							}
						}
						// Удаляем скачанный торрент
						downloadedTorrentFile.delete();
					}
				}
			}
		}
	}

	/**
	 * Выполняет отсылку письма о том, что найдено чойта новое
	 * 
	 * @param name
	 *            Имя файла торрента
	 * @param newFiles
	 *            Список новых файлов в торренте
	 */
	private void fireMailAnnounce(String name, List<String> newFiles) {
		// TODO Допилить

	}

	/**
	 * Получение списка файлов из декодированного торрента
	 * 
	 * @param torrentFile
	 *            Файл с торрентом (нужен для обработки однофайловых торрентов)
	 * @param decoded
	 *            Декодированный торент
	 * @return Список файлов в торренте
	 * @throws InvalidBEncodingException
	 */
	private Set<String> getFilesFromDecoded(File torrentFile,
			Map<String, BEValue> decoded) throws InvalidBEncodingException {
		Set<String> torrentFiles = new HashSet<>();
		// Загружаем список файлов из текущего торрента
		if (decoded.containsKey("files")) {
			for (BEValue file : decoded.get("files").getList()) {
				Map<String, BEValue> fileInfo = file.getMap();
				StringBuilder path = new StringBuilder();
				for (BEValue pathElement : fileInfo.get("path").getList()) {
					path.append(File.separator).append(pathElement.getString());
				}
				torrentFiles.add(path.toString());
			}
		} else {
			torrentFiles.add(torrentFile.getName());
		}
		return torrentFiles;
	}

	/**
	 * Вспомогательный класс, обеспечивающий хранение пары username - password
	 * 
	 * @author admin
	 * 
	 */
	public static class User {

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

	/**
	 * Вспомогательный класс для exception'ов, произошедших при обработке
	 * торрента
	 * 
	 * @author admin
	 */
	public static class TorrentDownloaderException extends Exception {

		/**
		 * for serialization
		 */
		private static final long serialVersionUID = -2771650402084251961L;

		/**
		 * @param message
		 */
		public TorrentDownloaderException(String message) {
			super(message);
		}

	}

}
