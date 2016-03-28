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
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import ru.lsv.torrentchecker.server.DownloaderException;
import ru.lsv.torrentchecker.server.torrents.TorrentDownloaderAbstract;

/**
 * Обеспечивает реализацию интерфейса загрузки с rutracker.org
 * 
 * @author s.lezhnev
 */
public class RutrackerDownloader extends TorrentDownloaderAbstract {

	/**
	 * см. TorrentDownloaderAbstract
	 * 
	 * @param httpContextIn
	 *            см. TorrentDownloaderAbstract
	 * @throws DownloaderException
	 *             см. TorrentDownloaderAbstract
	 */
	public RutrackerDownloader(HttpContext httpContextIn)
			throws DownloaderException {
		super(httpContextIn);
	}

	/**
	 * см.описание
	 * 
	 * @see ru.lsv.torrentchecker.server.abstracts.DownloaderAbstract#getResource
	 *      ()
	 */
	@Override
	public String getResource() {
		return "rutracker.org";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderAbstract#
	 * getDownloadResource()
	 */
	@Override
	public String getDownloadResource() {
		return "dl.rutracker.org";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderAbstract#
	 * getLoginResource()
	 */
	@Override
	public String getLoginResource() {
		return "login.rutracker.org";
	}

	@Override
	public boolean authenticate(URL url, String userName, String password)
			throws DownloaderException {
		HttpResponse response;
		HttpEntity entity;
		// Первое - делаем htpp-post запрос для авторизации
		// Логинимся
		HttpPost httpost = new HttpPost("http://" + getLoginResource()
				+ "/forum/login.php");

		List<NameValuePair> authFormParams = new ArrayList<NameValuePair>();
		authFormParams.add(new BasicNameValuePair("login_username", userName));
		authFormParams.add(new BasicNameValuePair("login_password", password));
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
		/*try {
			String page = EntityUtils.toString(entity);
			try (FileWriter fw = new FileWriter("login.txt")) {
				fw.write(page);
			}
		} catch (IOException e) {
			httpclient.getConnectionManager().shutdown();
			throw new TorrentDownloaderException(
					"Ошибка разбора страницы форума - ошибка получения страницы ("
							+ e.getMessage() + ")");
		}*/
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
		httpGet = null;
		// Выполняем запрос на страницу - после авторизации
		try {
			httpGet = new HttpGet(url.toURI());
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
			String page = EntityUtils.toString(entity);
			/*try (FileWriter fw = new FileWriter("page.txt")) {
				fw.write(page);
			}*/
			matcher = Pattern.compile("dl.php\\?t=\\d+\"").matcher(page);
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
		httpGet = new HttpGet("http://" + getDownloadResource() + "/forum/"
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
		String saveToFile = downloadLink.replaceAll("dl.php?", "")
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
