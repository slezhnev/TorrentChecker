/**
 * Основной сервис проверки  
 */
package ru.lsv.torrentchecker.server;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.filefilter.SuffixFileFilter;

import com.sun.istack.internal.logging.Logger;

import ru.lsv.torrentchecker.server.Commons.ConfigLoadException;
import ru.lsv.torrentchecker.server.torrents.TorrentsDownloader;
import ru.lsv.torrentchecker.shared.WorkingResult;
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
	 * Логгер
	 */
	private static Logger logger = Logger.getLogger(TorrentCheckerSheduler.class);

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
			logger.severe("contextInitialized exception ", e);
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
		logger.info("Torrent checker - sheduler started");
		// Получаем список файлов в Commons.getTorrentsPath
		// Принимаем только файлы с расширением .torrent
		String[] torrents = new File(Commons.getTorrentsPath()).list(new SuffixFileFilter(".torrent"));
		// Теперь пройдемся по ним - и обработаем каждый
		TorrentsDownloader downloader = new TorrentsDownloader();
		// Конвертируем в список файлов
		List<File> files = new ArrayList<>();
		Arrays.asList(torrents).forEach(fileName -> {
			files.add(new File(Commons.getTorrentsPath() + fileName));
		});
		// Запускаем проверку
		List<FileResult> results = downloader.check(files, Commons.getCredentials(), Commons.getTempPath(), Commons.getAutoloadPath());

		// Сохраняем результат
		Commons.setWorkingResult(new WorkingResult(results));
		logger.info("Torrent checker - sheduler finished");
	}

}
