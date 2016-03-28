package ru.lsv.torrentchecker.server.rss;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import ru.lsv.torrentchecker.server.DownloaderException;
import ru.lsv.torrentchecker.server.abstracts.DownloaderAbstract;
import ru.lsv.torrentchecker.server.rss.RssDownloaderAbstract.RssPreparedUrlToDownload;
import ru.lsv.torrentchecker.server.rss.impl.LostFilmDownloader;
import ru.lsv.torrentchecker.shared.User;
import ru.lsv.torrentchecker.shared.WorkingResult.FileProcessingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

/**
 * Check and download torrents from rss streams
 * 
 * @author s.lezhnev
 */
@SuppressWarnings("deprecation")
@Slf4j
public class RssDownloader extends DownloaderAbstract {
	/**
	 * Общий список загрузчиков
	 */
	List<RssDownloaderAbstract> downloaders = new ArrayList<>();

	/**
	 * http context для работы
	 */
	private HttpContext httpContext;

	/**
	 * Default constructor
	 */
	public RssDownloader() {
		httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE,
				new BasicCookieStore());
		// Создаем внутренний список
		try {
			downloaders.add(new LostFilmDownloader(httpContext));
		} catch (DownloaderException e) {
			// Do nothing
		}

	}

	/**
	 * Осуществляет проверку обновления указанных .rss-файлов <br>
	 * Если появились новые файлы - то он будет выложен в autoloadPath
	 * 
	 * 
	 * @param rssFiles
	 *            .torrent-файлы, обновление которых надо проверить
	 * @param credentials
	 *            Имена пользователей и паролей к ресурсам
	 * @param pathToDownload
	 *            Куда загружать временно .torrent-файл
	 * @param autoloadPath
	 *            Куда выкладывать .torrent-файл, если он изменился
	 * @return Результат обработки файлов
	 */
	@Override
	public List<FileResult> check(List<File> rssFiles,
			Map<String, User> credentials, String pathToDownload,
			String autoloadPath) {
		// Результаты обработки
		Map<RssDownloaderAbstract, DownloaderInProcess> preparedDownloaders = new HashMap<>();
		List<RssConfigFile> configFiles = rssFiles.stream()
				.map(el -> RssConfigFile.load(el)).filter(el -> el != null)
				.collect(Collectors.toList());
		Map<RssConfigFile, FileResult> resultMap = new HashMap<>();
		for (RssConfigFile configFile : configFiles) {
			resultMap.put(configFile, new FileResult(configFile.getName(),
					FileProcessingResult.UNCHANGED));
			URL rssUrl;
			try {
				rssUrl = new URL(configFile.getRssLink());
			} catch (MalformedURLException e) {
				// Добавляем FAIL и переходим к следующему
				resultMap.put(configFile, new FileResult(configFile.getName(),
						"Exception while parsing torrent file"));
				continue;
			}
			// Достаем из него host
			String rssHost = rssUrl.getHost();
			if (rssHost == null) {
				// Добавляем FAIL и переходим к следующему
				resultMap.put(configFile, new FileResult(configFile.getName(),
						"Host in URL is empty"));
				continue;
			}
			// Теперь поедем поищем соответствующую реализацию
			// downloader'а
			Optional<RssDownloaderAbstract> implDownloader = downloaders
					.stream()
					.filter(impl -> impl.getResource().equals(rssHost))
					.findFirst();
			if (!implDownloader.isPresent()) {
				// Добавляем FAIL и переходим к следующему
				resultMap.put(configFile, new FileResult(configFile.getName(),
						"Cannot find downloader for " + rssHost));
				continue;
			}
			if (!preparedDownloaders.containsKey(implDownloader.get())) {
				// Авторизуемся вначале
				User user = credentials.get(rssHost);
				if (user == null) {
					// Добавляем FAIL и переходим к следующему
					resultMap.put(configFile,
							new FileResult(configFile.getName(),
									"Cannot find credentials for " + rssHost));
					continue;
				}
				try {
					if (implDownloader.get().authenticate(rssUrl,
							user.getUserName(), user.getPassword())) {
						// Добавляем к приготовленным
						DownloaderInProcess downloads = new DownloaderInProcess();
						downloads.addTo(configFile);
						preparedDownloaders
								.put(implDownloader.get(), downloads);
					}
				} catch (DownloaderException e) {
					// Добавляем FAIL и переходим к следующему
					resultMap
							.put(configFile,
									new FileResult(configFile.getName(), e
											.getMessage()));
					log.error("Failed on authenticate.", e);
					continue;
				}
			} else {
				// Добавляем URL и файл к скачиванию
				preparedDownloaders.get(implDownloader.get()).addTo(configFile);
			}
		}
		for (Entry<RssDownloaderAbstract, DownloaderInProcess> downloader : preparedDownloaders
				.entrySet()) {
			for (Entry<String, List<RssConfigFile>> rss : downloader.getValue().downloads
					.entrySet()) {
				URL rssUrl;
				try {
					rssUrl = new URL(rss.getKey());
				} catch (MalformedURLException e) {
					// Добавляем FAIL и переходим к следующему
					for (RssConfigFile configFile : rss.getValue()) {
						resultMap.put(configFile,
								new FileResult(configFile.getName(),
										"Bad URL: " + rss.getKey()));
					}
					continue;
				}
				List<RssPreparedUrlToDownload> downloadData;
				try {
					rss.getValue().forEach(
							el -> log.info("Processing rss {}", el.getName()));
					downloadData = downloader.getKey().getDownloadFilenames(
							rssUrl, rss.getValue());
				} catch (DownloaderException e) {
					// Добавляем FAIL и переходим к следующему
					for (RssConfigFile configFile : rss.getValue()) {
						resultMap.put(
								configFile,
								new FileResult(configFile.getName(),
										"Failed download from rss : "
												+ rss.getKey()));
					}
					log.error("Failed on getFiles. ", e);
					continue;
				}
				log.debug("Find total files: {}", downloadData.size());
				// Проверим, что в autoload path нет уже таких файлов
				for (RssPreparedUrlToDownload toDownload : downloadData) {
					if (!new File(pathToDownload + File.separator
							+ toDownload.getFileName() + ".added").exists()) {
						try {
							downloader.getKey().downloadFromRss(toDownload,
									pathToDownload);
							resultMap.put(toDownload.getConfig(),
									new FileResult(toDownload.getConfig()
											.getName(),
											FileProcessingResult.MODIFYED));
							fireMailAnnounce(toDownload.getConfig().getName(),
									Arrays.asList(new String[] { toDownload
											.getFileName() }));
						} catch (DownloaderException e) {
							resultMap.put(
									toDownload.getConfig(),
									new FileResult(toDownload.getConfig()
											.getName(),
											"Failed download from rss : "
													+ rss.getKey()));
							log.error("Failed on downloading. ", e);
							continue;
						}
					}
				}
			}
		}
		List<FileResult> results = new LinkedList<>();
		resultMap.forEach((config, result) -> results.add(result));
		return results;
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
		private Map<String, List<RssConfigFile>> downloads;

		/**
		 * Default constructor
		 * 
		 */
		public DownloaderInProcess() {
			super();
			this.downloads = new HashMap<>();
		}

		public void addTo(RssConfigFile config) {
			if (downloads.containsKey(config.getRssLink())) {
				downloads.get(config.getRssLink()).add(config);
			} else {
				List<RssConfigFile> files = new LinkedList<>();
				files.add(config);
				downloads.put(config.getRssLink(), files);
			}
		}
	}

}
