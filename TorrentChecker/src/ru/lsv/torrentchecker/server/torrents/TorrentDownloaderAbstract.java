/**
 * Интерфейс реализации загрузки торрентов с ресурсов
 */
package ru.lsv.torrentchecker.server.torrents;

import java.net.URL;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

/**
 * Интерфейс реализации загрузки торрентов с ресурсов
 * 
 * @author admin
 */
public abstract class TorrentDownloaderAbstract implements AutoCloseable {

	/**
	 * http client для работы
	 */
	protected DefaultHttpClient httpclient;
	/**
	 * cookie store для работы
	 */
	protected CookieStore cookieStore;
	/**
	 * http context для работы
	 */
	protected HttpContext httpContext;

	/**
	 * Default constructor
	 * 
	 * @param httpContextIn
	 *            http context для работы <br/>
	 *            У него должен быть инициализирован cookie store
	 * @throws TorrentDownloaderException
	 *             В случае неинициализированного cookie store
	 */
	public TorrentDownloaderAbstract(HttpContext httpContextIn) throws TorrentDownloaderException {
		httpclient = new DefaultHttpClient();
		// Инициализируем cookie store
		httpContext = httpContextIn;
		cookieStore = (CookieStore) httpContext.getAttribute(ClientContext.COOKIE_STORE);
		if (cookieStore == null) {
			throw new TorrentDownloaderException("У httpContext не инициализирован cookie store");
		}
	}

	@Override
	public void close() throws Exception {
		httpclient.getConnectionManager().shutdown();
	}

	/**
	 * Возвращает имя ресурса, с которым умеет работать
	 * 
	 * @return см.описание
	 */
	public abstract String getResource();

	/**
	 * Выполнить аутентификацию
	 * 
	 * @param url
	 *            URL для загрузки (обычно - ссылка на страницу форума)
	 * @param userName
	 *            Имя пользователя для данного ресурса
	 * @param password
	 *            Пароль для данного ресурса
	 * @return
	 * @throws TorrentDownloaderException
	 *             В случае проблем с аутентификацией
	 */
	public abstract boolean authenticate(URL url, String userName, String password) throws TorrentDownloaderException;

	/**
	 * Загрузка торрента с указанного URL'я <br>
	 * Загрузчик сам должен найти ссылку на загрузку торрента в указанном URL!
	 * 
	 * @param url
	 *            URL для загрузки (обычно - ссылка на страницу форума)
	 * @param pathToDownload
	 *            Куда загружать .torrent-файл
	 * @return Имя файла, куда был сохранен скачанный торрент
	 * @throws TorrentDownloaderException
	 *             В случае проблем со скачиванием файла
	 */
	public abstract String downloadTorrent(URL url, String pathToDownload) throws TorrentDownloaderException;

}
