/**
 * Основной сервис проверки  
 */
package ru.lsv.torrentchecker.server;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.io.FilenameUtils;

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
public class TorrentCheckerSheduler implements ServletContextListener, Runnable {

	/**
	 * Шедулер
	 */
	private ScheduledExecutorService scheduler;

	/**
	 * см.описание
	 * 
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 *      ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// Тормозим все
		scheduler.shutdownNow();
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
			Commons.setWorkingResult(new WorkingResult(e.getMessage()));
			return;
		}
		// Создаем шедулер и запускаем его в работу
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleWithFixedDelay(this, 0, 3, TimeUnit.HOURS);
	}

	/**
	 * Обработчик шедулера
	 */
	@Override
	public void run() {
		service();
	}

	/**
	 * Основной обработчик <br/>
	 * Сделан синхронным - поскольку его запуск может быть зафорсен (т.е. чтобы
	 * не работало их ДВА одновременно) <br>
	 */
	public static synchronized void service() {
		// Получаем список файлов в Commons.getTorrentsPath
		// Принимаем только файлы с расширением .torrent
		String[] torrents = new File(Commons.getTorrentsPath())
				.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return "torrent".equals(FilenameUtils
								.getExtension(name));
					}
				});
		// Теперь пройдемся по ним - и обработаем каждый
		TorrentDownloader downloader = new TorrentDownloader();
		List<FileResult> results = new ArrayList<>();
		for (String torrent : torrents) {
			try {
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
	}
}
