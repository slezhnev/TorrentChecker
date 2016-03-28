package ru.lsv.torrentchecker.server.abstracts;

import java.net.URL;

import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import ru.lsv.torrentchecker.server.DownloaderException;

/**
 * Интерфейс реализации загрузки c ресурсов
 * 
 * @author s.lezhnev
 */
public abstract class FileDownloaderAbstract implements AutoCloseable {

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
	 * @throws DownloaderException
	 *             В случае неинициализированного cookie store
	 */
	public FileDownloaderAbstract(HttpContext httpContextIn)
			throws DownloaderException {
		httpclient = new DefaultHttpClient();
		// Инициализируем cookie store
		httpContext = httpContextIn;
		cookieStore = (CookieStore) httpContext
				.getAttribute(ClientContext.COOKIE_STORE);
		if (cookieStore == null) {
			throw new DownloaderException(
					"У httpContext не инициализирован cookie store");
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
	 * Возвращает имя download ресурса. <br/>
	 * По умолчанию оно совпадает с именем ресурса
	 * 
	 * @return см.описание
	 */
	public String getDownloadResource() {
		return getResource();
	}

	/**
	 * Возвращает имя login ресурса. <br/>
	 * По умолчанию оно совпадает с именем ресурса
	 * 
	 * @return см.описание
	 */
	public String getLoginResource() {
		return getResource();
	}

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
	 * @throws DownloaderException
	 *             В случае проблем с аутентификацией
	 */
	public abstract boolean authenticate(URL url, String userName,
			String password) throws DownloaderException;

}
