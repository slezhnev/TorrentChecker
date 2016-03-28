/**
 * Основной сервис проверки  
 */
package ru.lsv.torrentchecker.server;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.filefilter.SuffixFileFilter;

import ru.lsv.torrentchecker.server.Commons.ConfigLoadException;
import ru.lsv.torrentchecker.server.rss.RssDownloader;
import ru.lsv.torrentchecker.server.torrents.TorrentsDownloader;
import ru.lsv.torrentchecker.shared.WorkingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

/**
 * Основной сервис проверки
 * 
 * @author admin
 */
public class TorrentCheckerSheduler implements ServletContextListener {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
			.getLogger(TorrentsDownloader.class);

	/**
	 * Шедулер
	 */
	private static volatile ScheduledExecutorService scheduler = null;

	/**
	 * @return the scheduler
	 */
	public static synchronized ScheduledExecutorService getScheduler() {
		if (scheduler == null) {
			scheduler = Executors.newSingleThreadScheduledExecutor();
			scheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					service();
				}

			}, 0, 1, TimeUnit.HOURS);
		}
		return scheduler;
	}

	/**
	 * см.описание
	 * 
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 *      ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// Тормозим все
		if (!getScheduler().isShutdown()) {
			scheduler.shutdownNow();
		}
	}

	/**
	 * см.описание
	 * 
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet
	 *      .ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// Загружаем конфигурацию "по умолчанию"
		try {
			Commons.loadConfig(null);
		} catch (ConfigLoadException e) {
			// Создаем результат работы с ошибкой.
			// До перезапуска сервиса он и будет висеть постоянно - поскольку
			// шедулер не запустится
			logger.error("contextInitialized exception ", e);
			Commons.setWorkingResult(new WorkingResult(e.getMessage()));
			return;
		}
		// Создаем шедулер и запускаем его в работу
		/*
		 * scheduler = Executors.newSingleThreadScheduledExecutor();
		 * scheduler.scheduleWithFixedDelay(this, 0, 3, TimeUnit.HOURS);
		 */
		getScheduler();
	}

	/**
	 * Основной обработчик <br/>
	 * Сделан синхронным - поскольку его запуск может быть зафорсен (т.е. чтобы
	 * не работало их ДВА одновременно)
	 */
	public static synchronized void service() {
		try {
			logger.info("Torrent checker - sheduler started");
			logger.info(Commons.getTorrentsInQueue());
			// Получаем список файлов в Commons.getTorrentsPath
			// Принимаем только файлы с расширением .torrent и .rss
			String[] torrents = new File(Commons.getTorrentsInQueue())
					.list(new SuffixFileFilter(".torrent"));
			logger.info("Total torrents to check: {}", torrents.length);
			String[] rss = new File(Commons.getTorrentsInQueue())
					.list(new SuffixFileFilter(".rss"));
			logger.info("Total rss to check: {}", rss.length);
			// Запускаем проверку
			logger.info("Starting torrent checked");
			TorrentsDownloader torrentDownloader = new TorrentsDownloader();
			List<FileResult> results = torrentDownloader.check(
					Arrays.asList(torrents)
							.stream()
							.map(fileName -> new File(Commons
									.getTorrentsInQueue() + fileName))
							.collect(Collectors.toList()),
					Commons.getCredentials(), Commons.getTempPath(),
					Commons.getAutoloadPath());
			logger.info("Torrent checked finished");

			logger.info("Starting rss checked");
			RssDownloader rssDownloader = new RssDownloader();
			results.addAll(rssDownloader.check(
					Arrays.asList(rss)
							.stream()
							.map(fileName -> new File(Commons
									.getTorrentsInQueue() + fileName))
							.collect(Collectors.toList()),
					Commons.getCredentials(), Commons.getAutoloadPath(),
					Commons.getAutoloadPath()));
			logger.info("Rss checked finished");
			// Сохраняем результат
			Commons.setWorkingResult(new WorkingResult(results));
			logger.info("Torrent checker - sheduler finished");
		} catch (Throwable e) {
			logger.error("Exception ", e);
		}
	}

}
