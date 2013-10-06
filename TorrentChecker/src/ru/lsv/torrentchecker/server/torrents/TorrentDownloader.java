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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import ru.lsv.torrentchecker.server.Commons;
import ru.lsv.torrentchecker.server.bcodec.BDecoder;
import ru.lsv.torrentchecker.server.bcodec.BEValue;
import ru.lsv.torrentchecker.server.bcodec.InvalidBEncodingException;
import ru.lsv.torrentchecker.server.torrents.impl.IPv6NNMClubDownloader;
import ru.lsv.torrentchecker.server.torrents.impl.NNMClubDownloader;
import ru.lsv.torrentchecker.shared.User;
import ru.lsv.torrentchecker.shared.WorkingResult.FileProcessingResult;

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
		downloaders.add(new IPv6NNMClubDownloader());
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
	 * @return Результат обработки файла
	 * @throws IOException
	 *             В случае возникновения проблем с работой
	 */
	public FileProcessingResult check(File torrentFile,
			Map<String, User> credentials, String pathToDownload,
			String autoloadPath) throws IOException, TorrentDownloaderException {
		FileProcessingResult res = FileProcessingResult.UNCHANGED;
		// Первое - парсим .torrent файл
		FileInputStream torrentFIS = new FileInputStream(torrentFile);
		Map<String, BEValue> decoded = BDecoder.bdecode(torrentFIS).getMap();
		torrentFIS.close();
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
				String downloadedTorrent = null;
				downloadedTorrent = implDownloader
						.downloadTorrent(torrentUrl,
								credentials.get(torrentHost).getUserName(),
								credentials.get(torrentHost).getPassword(),
								pathToDownload);
				if (downloadedTorrent != null) {
					// Поехали проверим - а он есть на самом дела?
					File downloadedTorrentFile = new File(downloadedTorrent);
					if (downloadedTorrentFile.exists()) {
						// Первое - было бы тупо неплохо сравнить файлы просто
						// по хэшу. Если хэш совпадает - дальше ничего не
						// парсим. Значит файлы совпадают
						// Дополнительно - надо проверить, что в директории
						// торрентов этот файл есть. Если его нет - то его
						// надо выкладывать ОБЯЗАТЕЛЬНО!
						boolean isFileInQueue = new File(
								Commons.getTorrentsInQueue()
										+ torrentFile.getName()).exists();
						if (org.apache.commons.io.FileUtils
								.checksumCRC32(torrentFile) != org.apache.commons.io.FileUtils
								.checksumCRC32(downloadedTorrentFile)
								|| (!isFileInQueue)) {
							List<String> newFiles = new ArrayList<>();
							// Отлично. Попарсим и его.
							// В результате - нам надо найти что ж там нового
							Set<String> torrentFiles = getFilesFromDecoded(
									torrentFile, decoded);
							// Парсим новый принятый файл
							torrentFIS = new FileInputStream(
									downloadedTorrentFile);
							Map<String, BEValue> decodedDownloaded = BDecoder
									.bdecode(torrentFIS).getMap();
							torrentFIS.close();
							// Получаем список файлов из него
							Set<String> downloadedTorrentFiles = getFilesFromDecoded(
									downloadedTorrentFile, decodedDownloaded);
							if ((torrentFiles != null)
									&& (downloadedTorrentFiles != null)) {
								for (String file : downloadedTorrentFiles) {
									if (!torrentFiles.contains(file)) {
										newFiles.add(file);
									}
								}
							} else {
								// Значит - дурацкая ситуация, что раздела
								// файлов нет - надо просто пускать его
								// скачиваться
								newFiles.add(torrentFile.toString());
							}
							// Проверяем на появление чего-то нового
							if ((!isFileInQueue) || (newFiles.size() > 0)) {
								// Первое - запускаем рассылку о том, что
								// скачано новое
								if (!isFileInQueue) {
									// Файл новый - надо просто поставить на
									// закачку
									fireMailAnnounce(
											downloadedTorrentFile.getName(),
											null);
								} else {
									// Файл старый - но есть изменения
									fireMailAnnounce(
											downloadedTorrentFile.getName(),
											newFiles);
								}
								// Удаляем старый файл
								torrentFile.delete();
								// Копируем на его место новый файл
								org.apache.commons.io.FileUtils.copyFile(
										downloadedTorrentFile, torrentFile);
								// Теперь копируем этот же файл в autoload
								org.apache.commons.io.FileUtils.copyFile(
										torrentFile,
										new File(Commons.getAutoloadPath()
												+ torrentFile.getName()));
								// И копируем его в inqueue
								org.apache.commons.io.FileUtils.copyFile(
										torrentFile,
										new File(Commons.getTorrentsInQueue()
												+ torrentFile.getName()));
								// Правим возвращаемый результат
								if (!isFileInQueue) {
									res = FileProcessingResult.NEW;
								} else {
									res = FileProcessingResult.MODIFYED;
								}
							}
						}
						// Удаляем скачанный торрент
						downloadedTorrentFile.delete();
					} else {
						throw new TorrentDownloaderException(
								"БАГ! Торрент скачан, но файл отсутствует! o_O");
					}
				} else {
					throw new TorrentDownloaderException(
							"Не удалось загрузить торрент с сайта. Проверьте имя пользователя и пароль");
				}
			}
		} else {
			throw new TorrentDownloaderException(
					"В торрент-файле отсутствует блок \"comment\"");
		}
		return res;
	}

	/**
	 * Выполняет отсылку письма о том, что найдено чойта новое
	 * 
	 * @param name
	 *            Имя файла торрента
	 * @param newFiles
	 *            Список новых файлов в торренте (null - если торрент просто
	 *            новый)
	 */
	public void fireMailAnnounce(String name, List<String> newFiles) {
		// Первое - нам надо проверить наличие пачки дополнительных
		// пропертей в credentials
		if ((Commons.getMailCredentials() != null)
				&& (Commons.getSendEmailTo() != null)) {
			Properties props = System.getProperties();
			// props.put("mail.smtp.starttls.enable", "false");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "465");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.debug", "true");
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class",
					"javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");
			//
			Session session = Session.getDefaultInstance(props,
					new javax.mail.Authenticator() {
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(Commons
									.getMailCredentials().getUserName(),
									Commons.getMailCredentials().getPassword());
						}
					});
			MimeMessage message = new MimeMessage(session);
			try {
				message.setFrom(new InternetAddress(Commons
						.getMailCredentials().getUserName()));
				message.addRecipient(Message.RecipientType.TO,
						new InternetAddress(Commons.getSendEmailTo()));
				message.setSubject("Обновление " + name);
				StringBuffer str = new StringBuffer();
				str.append("<html><head><title>").append(message.getSubject())
						.append("</title></head><body>");
				if (newFiles != null) {
					str.append("Новые файлы в торренте ").append(name)
							.append(": <p/>");
					for (String file : newFiles) {
						str.append(file).append("<br/>");
					}
				} else {
					str.append("Новый торрент - добавлен в закачки");
				}
				str.append("\n</body></html>");
				message.setDataHandler(new DataHandler(new ByteArrayDataSource(
						str.toString(), "text/html")));
				message.setHeader("X-Mailer", "Torrent checker");
				message.setSentDate(new Date());
				Transport.send(message);
			} catch (AddressException e) {
				// Do nothing
			} catch (MessagingException e) {
				// Do nothing
				e.printStackTrace();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * Получение списка файлов из декодированного торрента
	 * 
	 * @param torrentFile
	 *            Файл с торрентом (нужен для обработки однофайловых торрентов)
	 * @param decoded
	 *            Декодированный торент
	 * @return Список файлов в торренте или null - если раздела files в decoded
	 *         найти не удалось
	 * @throws InvalidBEncodingException
	 *             В случае возникновения проблем при парсинге строк
	 */
	public Set<String> getFilesFromDecoded(File torrentFile,
			Map<String, BEValue> decoded) throws InvalidBEncodingException {
		Set<String> torrentFiles = null;
		// Загружаем список файлов из текущего торрента
		if (decoded.containsKey("files")) {
			torrentFiles = new HashSet<>();
			for (BEValue file : decoded.get("files").getList()) {
				Map<String, BEValue> fileInfo = file.getMap();
				StringBuilder path = new StringBuilder();
				for (BEValue pathElement : fileInfo.get("path").getList()) {
					path.append(File.separator).append(pathElement.getString());
				}
				torrentFiles.add(path.toString());
			}
		} else {
			// Попробуем пройтись по всем ключам и проверить - а вдруг там еще
			// есть мапы?
			// А то как-то не очень понятно где может быть блок files - толи в
			// головной мапе, толи в info
			// Перебдим, короче
			for (BEValue val : decoded.values()) {
				if (val.getValue() instanceof Map) {
					// Значит это еще одна мапа - попробуем в ей поискать files
					torrentFiles = getFilesFromDecoded(torrentFile,
							val.getMap());
					if (torrentFiles != null) {
						// Нашли
						break;
					}
				}
			}
		}
		return torrentFiles;
	}

}
