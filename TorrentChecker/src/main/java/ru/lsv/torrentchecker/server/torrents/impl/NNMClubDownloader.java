/**
 * Обеспечивает реализацию интерфейса загрузки с nnm-club.me
 */
package ru.lsv.torrentchecker.server.torrents.impl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import ru.lsv.torrentchecker.server.DownloaderException;
import ru.lsv.torrentchecker.server.torrents.TorrentDownloaderAbstract;

/**
 * Обеспечивает реализацию интерфейса загрузки с nnmclub.to
 * 
 * @author s.lezhnev
 */
public class NNMClubDownloader extends TorrentDownloaderAbstract {

	/**
	 * см. TorrentDownloaderAbstract
	 * 
	 * @param httpContextIn
	 *            см. TorrentDownloaderAbstract
	 * @throws DownloaderException
	 *             см. TorrentDownloaderAbstract
	 */
	public NNMClubDownloader(HttpContext httpContextIn)
			throws DownloaderException {
		super(httpContextIn);
	}

	/**
	 * см.описание
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.ru.lsv.torrentchecker.server.abstracts.TorrentDownloaderAbstract#getResource
	 *      ()
	 */
	@Override
	public String getResource() {
		return "nnmclub.to";
	}

	@Override
	public boolean authenticate(URL url, String userName, String password)
			throws DownloaderException {
		HttpResponse response;
		HttpEntity entity;
		// Первое - делаем htpp-post запрос для авторизации
		// Логинимся
		HttpPost httpost = new HttpPost("http://" + getResource()
				+ "/forum/login.php");

		List<NameValuePair> authFormParams = new ArrayList<NameValuePair>();
		authFormParams.add(new BasicNameValuePair("username", userName));
		authFormParams.add(new BasicNameValuePair("password", password));
		authFormParams.add(new BasicNameValuePair("autologin", "off"));
		authFormParams.add(new BasicNameValuePair("redirect", ""));
		authFormParams.add(new BasicNameValuePair("login", "Вход"));

		try {
			httpost.setEntity(new UrlEncodedFormEntity(authFormParams,
					HTTP.UTF_8));
		} catch (UnsupportedEncodingException e1) {
			// Do nothing
		}

		try {
			response = httpclient.execute(httpost, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка авторизации - ошибка выполнения post запроса на логин ("
							+ e.getMessage() + ")");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка авторизации - entity is null");
		}
		httpost.abort();
		return true;
	}

	@Override
	public String downloadTorrent(URL url, String pathToDownload)
			throws DownloaderException {
		// Тут все сложно.
		HttpResponse response;
		HttpGet httpGet;
		HttpEntity entity;
		// Ищем нужную куку
		String sid = null;
		for (Cookie cookie : cookieStore.getCookies()) {
			if ("phpbb2mysql_4_sid".equals(cookie.getName())) {
				sid = cookie.getValue();
				break;
			}
		}
		if (sid == null) {
			throw new DownloaderException(
					"Ошибка получения страницы с описанием торрента - отсутствует cookie phpbb2mysql_4_sid");
		}
		httpGet = null;
		// Выполняем запрос на страницу - после авторизации
		try {
			httpGet = new HttpGet(url.toURI() + "&sid=" + sid);
		} catch (URISyntaxException e) {
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка преобразования url в uri o_O");
		}
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка получения страницы с описанием торрента ("
							+ e.getMessage() + ")");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка получения страницы с описанием торрента - entity is null");
		}

		// Парсим файл
		Matcher matcher;
		try {
			matcher = Pattern.compile("download.php\\?id=\\d+\"").matcher(
					EntityUtils.toString(entity));
		} catch (ParseException e) {
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка разбора страницы форума - matcher упал o_O");
		} catch (IOException e) {
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка разбора страницы форума - ошибка получения страницы ("
							+ e.getMessage() + ")");
		}
		if (!matcher.find()) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка разбора страницы форума - не найдена ссылка для скачивания");
		}
		// Значит тут есть ссылка на загрузку торрента.
		// Нафиг грохнем " - и все будет хорошо
		String downloadLink = matcher.group().replaceAll("\"", "");
		httpGet.abort();
		httpGet = new HttpGet("http://" + getResource() + "/forum/"
				+ downloadLink);
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException("Ошибка загрузки торрента");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка загрузки торрента - entity is null");
		}
		// Получаем имя файла, в которое надо будет сохранять торрент
		Header[] headers = response.getHeaders("Content-Disposition");
		// Если такого имени сервер не передаст - заранее генерим временное имя
		// файла
		String saveToFile = downloadLink.replaceAll("download.php?", "")
				+ ".torrent";
		if ((headers != null) && (headers.length == 1)
				&& (headers[0].getValue().indexOf("=") != -1)) {
			saveToFile = headers[0].getValue().split("=")[1].replaceAll("\"",
					"");
		}
		saveToFile = pathToDownload + saveToFile;
		try {
			entity.writeTo(new FileOutputStream(saveToFile));
		} catch (FileNotFoundException e) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка сохранения торрента - FileNotFoundException o_O");
		} catch (IOException e) {
			httpclient.getConnectionManager().shutdown();
			throw new DownloaderException(
					"Ошибка сохранения торрента - ошибка ввода вывода. Может место кончилось?");
		}

		// http://stackoverflow.com/questions/10995378/httpurlconnection-downloaded-file-name
		return saveToFile;
	}
}
