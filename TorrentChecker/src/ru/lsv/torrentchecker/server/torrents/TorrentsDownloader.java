package ru.lsv.torrentchecker.server.torrents;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import ru.lsv.torrentchecker.server.Commons;
import ru.lsv.torrentchecker.server.bcodec.BDecoder;
import ru.lsv.torrentchecker.server.bcodec.BEValue;
import ru.lsv.torrentchecker.server.bcodec.InvalidBEncodingException;
import ru.lsv.torrentchecker.server.torrents.impl.IPv6NNMClubDownloader;
import ru.lsv.torrentchecker.server.torrents.impl.NNMClubDownloader;
import ru.lsv.torrentchecker.server.torrents.impl.TorrentRusEcDownloader;
import ru.lsv.torrentchecker.shared.User;
import ru.lsv.torrentchecker.shared.WorkingResult.FileProcessingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

import com.sun.istack.internal.logging.Logger;

/**
 * Класс, обеспечивающий загрузку торрентов
 * 
 * @author admin
 */
public class TorrentsDownloader {

	/**
	 * Логгер
	 */
	private Logger logger = Logger.getLogger(TorrentsDownloader.class);
	/**
	 * Общий список загрузчиков
	 */
	List<TorrentDownloaderAbstract> downloaders = new ArrayList<>();

	/**
	 * http context для работы
	 */
	private HttpContext httpContext;

	/**
	 * Default constructor
	 */
	public TorrentsDownloader() {
		// Инициализируем
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
		// Создаем внутренний список
		try {
			downloaders.add(new NNMClubDownloader(httpContext));
		} catch (TorrentDownloaderException e) {
			// Do nothing
		}
		try {
			downloaders.add(new IPv6NNMClubDownloader(httpContext));
		} catch (TorrentDownloaderException e) {
			// Do nothing
		}
		try {
			downloaders.add(new TorrentRusEcDownloader(httpContext));
		} catch (TorrentDownloaderException e) {
			// Do nothing
		}
	}

	/**
	 * Осуществляет проверку обновления указанных .torrent-файлов <br>
	 * Если файл изменился - то: <br>
	 * 1. Он будет выложен на место torrentFile <br>
	 * 2. Он будет скопирован в autoloadPath <br>
	 * Файлы, не содержащие блок "comment" с валидным URL - просто игнорируются
	 * 
	 * 
	 * @param torrentFiles
	 *            .torrent-файлы, обновление которых надо проверить
	 * @param credentials
	 *            Имена пользователей и паролей к ресурсам
	 * @param pathToDownload
	 *            Куда загружать временно .torrent-файл
	 * @param autoloadPath
	 *            Куда выкладывать .torrent-файл, если он изменился
	 * @return Результат обработки файлов
	 */
	public List<FileResult> check(List<File> torrentFiles, Map<String, User> credentials, String pathToDownload, String autoloadPath) {
		// Результаты обработки
		List<FileResult> results = new ArrayList<>();
		Map<TorrentDownloaderAbstract, DownloaderInProcess> preparedDownloaders = new HashMap<>();
		for (File torrentFile : torrentFiles) {
			// Первое - парсим .torrent файл
			Map<String, BEValue> decoded = null;
			try {
				try (FileInputStream torrentFIS = new FileInputStream(torrentFile)) {
					decoded = BDecoder.bdecode(torrentFIS).getMap();
				}
			} catch (Exception e) {
				// Добавляем FAIL и переходим к следующему
				results.add(new FileResult(torrentFile.getName(), "Exception while parsing torrent file"));
				continue;
			}
			if (!decoded.containsKey("comment")) {
				results.add(new FileResult(torrentFile.getName(), "Cannot find block \"comment\" in torrent"));
				continue;
			} else {
				URL torrentUrl = null;
				try {
					torrentUrl = new URL(decoded.get("comment").getString());
				} catch (InvalidBEncodingException e) {
					// Добавляем FAIL и переходим к следующему
					results.add(new FileResult(torrentFile.getName(), "Exception while getting \"comment\" from torrent file"));
					continue;
				} catch (MalformedURLException e) {
					// Добавляем FAIL и переходим к следующему
					results.add(new FileResult(torrentFile.getName(), "Malformed URL in comment"));
					continue;
				}
				// Достаем из него host
				String torrentHost = torrentUrl.getHost();
				if (torrentHost == null) {
					// Добавляем FAIL и переходим к следующему
					results.add(new FileResult(torrentFile.getName(), "Host in URL is empty"));
					continue;
				}
				// Теперь поедем поищем соответствующую реализацию
				// downloader'а
				Optional<TorrentDownloaderAbstract> implDownloader = downloaders.stream().filter(impl -> impl.getResource().equals(torrentHost)).findFirst();
				if (!implDownloader.isPresent()) {
					// Добавляем FAIL и переходим к следующему
					results.add(new FileResult(torrentFile.getName(), "Cannot find downloader for " + torrentHost));
					continue;
				}
				if (!preparedDownloaders.containsKey(implDownloader.get())) {
					// Авторизуемся вначале
					User user = credentials.get(torrentHost);
					if (user == null) {
						// Добавляем FAIL и переходим к следующему
						results.add(new FileResult(torrentFile.getName(), "Cannot find credentials for " + torrentHost));
						continue;
					}
					try {
						if (implDownloader.get().authenticate(torrentUrl, user.getUserName(), user.getPassword())) {
							// Добавляем к приготовленным
							DownloaderInProcess downloads = new DownloaderInProcess();
							downloads.downloads.add(new TorrentInProcess(torrentFile, torrentUrl, decoded));
							preparedDownloaders.put(implDownloader.get(), downloads);
						}
					} catch (TorrentDownloaderException e) {
						// Добавляем FAIL и переходим к следующему
						results.add(new FileResult(torrentFile.getName(), e.getMessage()));
						continue;
					}
				} else {
					// Добавляем URL и файл к скачиванию
					preparedDownloaders.get(implDownloader.get()).downloads.add(new TorrentInProcess(torrentFile, torrentUrl, decoded));
				}
			}
		}
		// Поехали по подготовленным к загрузке downloader'ам
		for (TorrentDownloaderAbstract implDownloader : preparedDownloaders.keySet()) {
			// Поехали по URL
			for (TorrentInProcess downloadInProcess : preparedDownloaders.get(implDownloader).downloads) {
				logger.info("Torrent checker - Processing " + downloadInProcess.file.getName());
				// Нашли имплементацию. Загружаем файлО во временное
				// место хранения
				String downloadedTorrent = null;
				try {
					downloadedTorrent = implDownloader.downloadTorrent(downloadInProcess.downloadURL, pathToDownload);
				} catch (TorrentDownloaderException e) {
					// Добавляем FAIL и переходим к следующему
					results.add(new FileResult(downloadInProcess.file.getName(), e.getMessage()));
					continue;
				}
				if (downloadedTorrent != null) {
					// Поехали проверим - а он есть на самом дела?
					File downloadedTorrentFile = new File(downloadedTorrent);
					if (downloadedTorrentFile.exists()) {
						// Первое - было бы тупо неплохо сравнить
						// файлы просто
						// по хэшу. Если хэш совпадает - дальше
						// ничего не
						// парсим. Значит файлы совпадают
						// Дополнительно - надо проверить, что в
						// директории
						// торрентов этот файл есть. Если его нет -
						// то его
						// надо выкладывать ОБЯЗАТЕЛЬНО!
						boolean isFileInQueue = new File(Commons.getTorrentsInQueue() + downloadInProcess.file.getName()).exists();
						boolean fileCompare = false;
						try {
							fileCompare = org.apache.commons.io.FileUtils.checksumCRC32(downloadInProcess.file) != org.apache.commons.io.FileUtils
									.checksumCRC32(downloadedTorrentFile);
						} catch (Exception e1) {
							// Добавляем FAIL и переходим к следующему
							results.add(new FileResult(downloadInProcess.file.getName(), "Failed on CRC32 check"));
							continue;
						}
						if (fileCompare || (!isFileInQueue)) {
							final List<String> newFiles = new ArrayList<>();
							// Отлично. Попарсим и его.
							// В результате - нам надо найти что ж
							// там нового
							Optional<Set<String>> filesInTorrent = getFilesFromDecoded(downloadInProcess.file, downloadInProcess.decoded);
							// Парсим новый принятый файл
							FileInputStream torrentFIS = null;
							try {
								torrentFIS = new FileInputStream(downloadedTorrentFile);
							} catch (Exception e1) {
								// Добавляем FAIL и переходим к
								// следующему
								results.add(new FileResult(downloadInProcess.file.getName(), "Failed on accessing downloaded torrent file"));
								continue;
							}
							Map<String, BEValue> decodedDownloaded = null;
							try {
								decodedDownloaded = BDecoder.bdecode(torrentFIS).getMap();
								torrentFIS.close();
							} catch (Exception e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							// Получаем список файлов из него
							Optional<Set<String>> downloadedTorrentFiles = getFilesFromDecoded(downloadedTorrentFile, decodedDownloaded);
							if (filesInTorrent.isPresent() && downloadedTorrentFiles.isPresent()) {
								downloadedTorrentFiles.get().stream().filter(file -> !torrentFiles.contains(file)).forEach(file -> newFiles.add(file));
								/*
								 * for (String file : downloadedTorrentFiles) {
								 * if (!torrentFiles.contains(file)) {
								 * newFiles.add(file); } }
								 */
							} else {
								// Значит - дурацкая ситуация, что
								// раздела
								// файлов нет - надо просто пускать его
								// скачиваться
								newFiles.add(downloadInProcess.file.toString());
							}
							// Проверяем на появление чего-то нового
							if ((!isFileInQueue) || (newFiles.size() > 0)) {
								// Первое - запускаем рассылку о
								// том, что
								// скачано новое
								if (!isFileInQueue) {
									// Файл новый - надо просто
									// поставить на
									// закачку
									fireMailAnnounce(downloadedTorrentFile.getName(), null);
								} else {
									// Файл старый - но есть
									// изменения
									fireMailAnnounce(downloadedTorrentFile.getName(), newFiles);
								}
								// Удаляем старый файл
								downloadInProcess.file.delete();
								// Копируем на его место новый файл
								try {
									org.apache.commons.io.FileUtils.copyFile(downloadedTorrentFile, downloadInProcess.file);
								} catch (IOException e) {
									// Добавляем FAIL и переходим к
									// следующему
									results.add(new FileResult(downloadInProcess.file.getName(), "Failed copying to storage"));
									continue;
								}
								try {
									// Теперь копируем этот же файл в
									// autoload
									org.apache.commons.io.FileUtils.copyFile(downloadInProcess.file, new File(Commons.getAutoloadPath() +
											downloadInProcess.file.getName()));
								} catch (IOException e) {
									// Добавляем FAIL и переходим к
									// следующему
									results.add(new FileResult(downloadInProcess.file.getName(), "Failed copying to \"autoload\""));
									continue;
								}
								try {
									// И копируем его в inqueue
									org.apache.commons.io.FileUtils.copyFile(downloadInProcess.file, new File(Commons.getTorrentsInQueue() +
											downloadInProcess.file.getName()));
								} catch (IOException e) {
									// Добавляем FAIL и переходим к
									// следующему
									results.add(new FileResult(downloadInProcess.file.getName(), "Failed copying to \"inqueue\""));
									continue;
								}
								// Возвращаем результат

								if (!isFileInQueue) {
									results.add(new FileResult(downloadInProcess.file.getName(), FileProcessingResult.NEW));
								} else {
									results.add(new FileResult(downloadInProcess.file.getName(), FileProcessingResult.MODIFYED));
								}
							} else {
								results.add(new FileResult(downloadInProcess.file.getName(), FileProcessingResult.UNCHANGED));
							}
						} else {
							results.add(new FileResult(downloadInProcess.file.getName(), FileProcessingResult.UNCHANGED));
						}
						// Удаляем скачанный торрент
						downloadedTorrentFile.delete();
					} else {
						results.add(new FileResult(downloadInProcess.file.getName(), "Torrent downloaded, but does not exist o_O"));
						continue;
					}
				} else {
					results.add(new FileResult(downloadInProcess.file.getName(), implDownloader.getClass().getName() +
							" returned null as downloaded torrent o_O"));
					continue;
				}
			}
		}
		// Теперь еще надо имплементации позакрывать - чтоб не текло
		for (TorrentDownloaderAbstract implDownloader : preparedDownloaders.keySet()) {
			try {
				implDownloader.close();
			} catch (Exception e) {
				// Do nothing
			}
		}
		return results;
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
		if ((Commons.getMailCredentials() != null) && (Commons.getSendEmailTo() != null)) {
			Properties props = System.getProperties();
			// props.put("mail.smtp.starttls.enable", "false");
			props.put("mail.smtp.host", "smtp.gmail.com");
			props.put("mail.smtp.port", "465");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.debug", "true");
			props.put("mail.smtp.socketFactory.port", "465");
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");
			//
			Session session = Session.getDefaultInstance(props, new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(Commons.getMailCredentials().getUserName(), Commons.getMailCredentials().getPassword());
				}
			});
			MimeMessage message = new MimeMessage(session);
			try {
				message.setFrom(new InternetAddress(Commons.getMailCredentials().getUserName()));
				message.addRecipient(Message.RecipientType.TO, new InternetAddress(Commons.getSendEmailTo()));
				message.setSubject("Обновление " + name);
				StringBuffer str = new StringBuffer();
				str.append("<html><head><title>").append(message.getSubject()).append("</title></head><body>");
				if (newFiles != null) {
					str.append("Новые файлы в торренте ").append(name).append(": <p/>");
					for (String file : newFiles) {
						str.append(file).append("<br/>");
					}
				} else {
					str.append("Новый торрент - добавлен в закачки");
				}
				str.append("\n</body></html>");
				message.setDataHandler(new DataHandler(new ByteArrayDataSource(str.toString(), "text/html")));
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
	 */
	public Optional<Set<String>> getFilesFromDecoded(File torrentFile, Map<String, BEValue> decoded) {
		final Set<String> torrentFiles = new HashSet<>();
		// Загружаем список файлов из текущего торрента
		if (decoded.containsKey("files")) {
			try {
				// for (BEValue file : decoded.get("files").getList()) {
				decoded.get("files").getList().forEach(file -> {
					try {
						Map<String, BEValue> fileInfo = file.getMap();
						final StringBuilder path = new StringBuilder();
						// for (BEValue pathElement : fileInfo
						// .get("path").getList()) {
						fileInfo.get("path").getList().forEach(pathElement -> {
							try {
								path.append(File.separator).append(pathElement.getString());
							} catch (Exception e) {
								// Do nothing - просто
								// перейдем к
								// следующему
							}
						});
						torrentFiles.add(path.toString());
					} catch (InvalidBEncodingException ex) {
						// Do nothing - просто перейдем к
						// следующему
					}
				});
			} catch (InvalidBEncodingException e) {
				return Optional.empty();
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
					Optional<Set<String>> files;
					try {
						files = getFilesFromDecoded(torrentFile, val.getMap());
						if (files.isPresent()) {
							// Нашли
							return files;
						}
					} catch (InvalidBEncodingException e) {
						// Do nothing - поедем смотреть следующее
					}
				}
			}
		}
		return Optional.ofNullable(torrentFiles);
	}

	/**
	 * Временное хранилище downloader'ов при обработке
	 * 
	 * @author s.lezhnev
	 */
	private class DownloaderInProcess {
		/**
		 * URL'и для скачивания файла
		 */
		private List<TorrentInProcess> downloads;

		/**
		 * Default constructor
		 * 
		 */
		public DownloaderInProcess() {
			super();
			this.downloads = new ArrayList<>();
		}

	}

	/**
	 * Временное хранилище обрабатываемого торента
	 * 
	 * @author s.lezhnev
	 */
	private class TorrentInProcess {
		/**
		 * Файл торрента
		 */
		private File file;
		/**
		 * URL для скачивания
		 */
		private URL downloadURL;
		/**
		 * Результаты парсинга торент-файла
		 */
		private Map<String, BEValue> decoded;

		/**
		 * Default constructor
		 * 
		 * @param file
		 *            Файл торрента
		 * @param downloadURL
		 *            URL для скачивания
		 */
		public TorrentInProcess(File file, URL downloadURL, Map<String, BEValue> decoded) {
			super();
			this.file = file;
			this.downloadURL = downloadURL;
			this.decoded = decoded;
		}

	}

}
