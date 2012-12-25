/**
 * Основной сервис проверки  
 */
package ru.lsv.torrentchecker.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.filefilter.SuffixFileFilter;

import ru.lsv.torrentchecker.server.Commons.ConfigLoadException;
import ru.lsv.torrentchecker.server.torrents.TorrentDownloader;
import ru.lsv.torrentchecker.server.torrents.TorrentDownloaderException;
import ru.lsv.torrentchecker.shared.WorkingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileProcessingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

/**
 * Основной сервис проверки
 * 
 * @author admin
 */
public class TorrentCheckerSheduler implements ServletContextListener {

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
			scheduler.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					service();
				}

			}, 0, 3, TimeUnit.HOURS);
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
			System.out.println("contextInitialized - exception - "
					+ e.getMessage());
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
		System.out.println("Torrent checker - sheduler started");
		// Получаем список файлов в Commons.getTorrentsPath
		// Принимаем только файлы с расширением .torrent
		String[] torrents = new File(Commons.getTorrentsPath())
				.list(new SuffixFileFilter(".torrent"));
		// Теперь пройдемся по ним - и обработаем каждый
		TorrentDownloader downloader = new TorrentDownloader();
		List<FileResult> results = new ArrayList<>();
		for (String torrent : torrents) {
			try {
				System.out.println("Torrent checker - process " + torrent);
				FileProcessingResult res = downloader.check(
						new File(Commons.getTorrentsPath() + torrent),
						Commons.getCredentials(), Commons.getTempPath(),
						Commons.getAutoloadPath());
				// Хм. Ничего не вылетело. Значит добавляем в результат работы
				// БЕЗ замечаний
				results.add(new FileResult(torrent, res));
			} catch (IOException e) {
				results.add(new FileResult(torrent, "IOException: "
						+ e.getMessage()));
			} catch (TorrentDownloaderException e) {
				results.add(new FileResult(torrent,
						"TorrentDownloaderException: " + e.getMessage()));
			}
		}
		// Сохраняем результат
		Commons.setWorkingResult(new WorkingResult(results));
		System.out.println("Torrent checker - sheduler finished");
	}

}
