/**
 * Обеспечивает реализацию интерфейса загрузки с nnm-club.ru
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
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import ru.lsv.torrentchecker.server.torrents.TorrentDownloaderException;
import ru.lsv.torrentchecker.server.torrents.TorrentDownloaderInterface;

/**
 * Обеспечивает реализацию интерфейса загрузки с nnm-club.ru
 * 
 * @author admin
 */
public class NNMClubDownloader implements TorrentDownloaderInterface {

	/**
	 * см.описание
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderInterface#getResource
	 *      ()
	 */
	@Override
	public String getResource() {
		return "nnm-club.ru";
	}

	/**
	 * см.описание
	 * 
	 * @throws TorrentDownloaderException
	 *             В случае возникновения проблем со скачиванием
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderInterface#
	 *      downloadTorrent(java.net.URL, java.lang.String, java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public String downloadTorrent(URL url, String userName, String password,
			String pathToDownload) throws TorrentDownloaderException {
		// Тут все сложно.
		// Первое - делаем htpp-post запрос для авторизации
		DefaultHttpClient httpclient = new DefaultHttpClient();
		// Инициализируем cookie store
		CookieStore cookieStore = new BasicCookieStore();
		HttpContext httpContext = new BasicHttpContext();
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		//
		HttpPost httpost = new HttpPost("http://nnm-club.ru/forum/login.php");

		List<NameValuePair> authFornParams = new ArrayList<NameValuePair>();
		authFornParams.add(new BasicNameValuePair("username", userName));
		authFornParams.add(new BasicNameValuePair("password", password));
		authFornParams.add(new BasicNameValuePair("autologin", "off"));
		authFornParams.add(new BasicNameValuePair("redirect", ""));
		authFornParams.add(new BasicNameValuePair("login", "Вход"));

		try {
			httpost.setEntity(new UrlEncodedFormEntity(authFornParams,
					HTTP.UTF_8));
		} catch (UnsupportedEncodingException e) {
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Получили UnsupportedEncodingException на UTF-8 o_O");
		}

		HttpResponse response;
		try {
			response = httpclient.execute(httpost, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка авторизации - ошибка выполнения post запроса на логин");
		}

		httpost.abort();
		// Выполняем запрос на страницу - после удачной авторизации
		HttpGet httpGet = null;
		try {
			httpGet = new HttpGet(url.toURI());
		} catch (URISyntaxException e) {
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка преобразования url в uri o_O");
		}
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка получения страницы с описанием торрента");
		}
		HttpEntity entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка получения страницы с описанием торрента - entity is null");
		}

		// Парсим файл
		Matcher matcher;
		try {
			matcher = Pattern.compile("download.php\\?id=\\d+\"").matcher(
					EntityUtils.toString(entity));
		} catch (ParseException e) {
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка разбора страницы форума - matcher упал o_O");
		} catch (IOException e) {
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка разбора страницы форума - ошибка получения страницы");
		}
		if (!matcher.find()) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка разбора страницы форума - не найдена ссылка для скачивания");
		}
		// Значит тут есть ссылка на загрузку торрента.
		// Нафиг грохнем " - и все будет хорошо
		String downloadLink = matcher.group().replaceAll("\"", "");
		httpGet.abort();
		httpGet = new HttpGet("http://nnm-club.ru/forum/" + downloadLink);
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException("Ошибка загрузки торрента");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
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
			saveToFile = headers[0].getValue().split("=")[1]
					.replaceAll("\"", "");
		}
		saveToFile = pathToDownload + saveToFile;
		try {
			entity.writeTo(new FileOutputStream(saveToFile));
		} catch (FileNotFoundException e) {
			// Опять что-то странное
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка аохранения торрента - FileNotFoundException o_O");
		} catch (IOException e) {
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка аохранения торрента - ошибка ввода вывода. Может место кончилось?");
		}

		httpclient.getConnectionManager().shutdown();

		// http://stackoverflow.com/questions/10995378/httpurlconnection-downloaded-file-name
		return saveToFile;
	}

}
