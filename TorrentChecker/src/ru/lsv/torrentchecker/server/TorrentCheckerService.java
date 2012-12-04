/**
 * �������� ������ ��������  
 */
package ru.lsv.torrentchecker.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import ru.lsv.torrentchecker.server.Commons.ConfigLoadException;

/**
 * �������� ������ ��������  
 * 
 * @author admin
 */
public class TorrentCheckerService implements ServletContextListener, Runnable {

	/**
	 * �������
	 */
	private ScheduledExecutorService scheduler;

	/**
	 * ��.��������
	 * 
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 *      ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		// �������� ���
		scheduler.shutdownNow();
	}

	/**
	 * ��.��������
	 * 
	 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet
	 *      .ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// ��������� ������������ "�� ���������"
		try {
			Commons.loadConfig(null);
		} catch (ConfigLoadException e) {
			e.printStackTrace();
			return;
		}
		// ������� ������� � ��������� ��� � ������
		scheduler = Executors.newSingleThreadScheduledExecutor();
		scheduler.scheduleWithFixedDelay(this, 0, 10, TimeUnit.SECONDS);
	}

	/**
	 * �������� ����������
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
