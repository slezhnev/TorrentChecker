/**
 * Основной сервис проверки  
 */
package ru.lsv.torrentchecker.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ru.lsv.torrentchecker.server.Commons.ConfigLoadException;

/**
 * Основной сервис проверки  
 * 
 * @author admin
 */
public class TorrentCheckerService implements ServletContextListener, Runnable {

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
			e.printStackTrace();
			return;
		}
		// Создаем шедулер и запускаем его в работу
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleWithFixedDelay(this, 0, 10, TimeUnit.SECONDS);
	}

	/**
	 * Основной обработчик
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
