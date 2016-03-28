/**
 * Обеспечивает реализацию интерфейса загрузки с rutracker.org
 */
package ru.lsv.torrentchecker.server.rss.impl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ru.lsv.torrentchecker.server.DownloaderException;
import ru.lsv.torrentchecker.server.rss.RssConfigFile;
import ru.lsv.torrentchecker.server.rss.RssDownloaderAbstract;

/**
 * Обеспечивает реализацию интерфейса загрузки с lostfilm.to
 * 
 * @author s.lezhnev
 */
@SuppressWarnings("deprecation")
public class LostFilmDownloader extends RssDownloaderAbstract {

	/**
	 * см. TorrentDownloaderAbstract
	 * 
	 * @param httpContextIn
	 *            см. TorrentDownloaderAbstract
	 * @throws DownloaderException
	 *             см. TorrentDownloaderAbstract
	 */
	public LostFilmDownloader(HttpContext httpContextIn)
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
		return "lostfilm.tv";
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
		return "login1.bogi.ru";
	}

	@Override
	public boolean authenticate(URL url, String userName, String password)
			throws DownloaderException {
		HttpResponse response;
		HttpEntity entity;
		// Первое - делаем htpp-post запрос для авторизации
		// Логинимся
		HttpPost httppost = new HttpPost("http://" + getLoginResource()
				+ "/login.php?referer=https%3A%2F%2Fwww.lostfilm.tv%2F");
		;
		try {
			httppost.addHeader("referer", "https://www.lostfilm.tv/");
			List<NameValuePair> authFormParams = new ArrayList<NameValuePair>();
			authFormParams.add(new BasicNameValuePair("act", "login"));
			authFormParams.add(new BasicNameValuePair("module", "1"));
			authFormParams.add(new BasicNameValuePair("login", userName));
			authFormParams.add(new BasicNameValuePair("password", password));
			authFormParams.add(new BasicNameValuePair("repage", "user"));
			authFormParams.add(new BasicNameValuePair("target",
					"http://lostfilm.tv/"));

			try {
				httppost.setEntity(new UrlEncodedFormEntity(authFormParams,
						HTTP.UTF_8));
			} catch (UnsupportedEncodingException e1) {
				// Do nothing
			}

			try {
				response = httpclient.execute(httppost, httpContext);
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
			String page;
			try {
				page = EntityUtils.toString(entity);
			} catch (IOException e) {
				httpclient.getConnectionManager().shutdown();
				throw new DownloaderException(
						"Ошибка получения страницы авторизации ("
								+ e.getMessage() + ")");
			}
			Matcher matcher = Pattern.compile(
					"\"http://www.lostfilm.tv/blg.php\\?ref=\\w+\"").matcher(
					page);
			if (!matcher.find()) {
				// Опять что-то странное
				httpclient.getConnectionManager().shutdown();
				throw new DownloaderException(
						"Ошибка разбора страницы авторизации - не найден ref");
			}
			String refUrl = matcher.group().replaceAll("\"", "");

			httppost.abort();
			httppost = new HttpPost(refUrl);
			authFormParams = new ArrayList<NameValuePair>();
			httppost.addHeader("Referer",
					"http://login1.bogi.ru/login.php?referer=https%3A%2F%2Fwww.lostfilm.tv%2F");

			matcher = Pattern.compile("name=\"(\\w+)\".+value=\"(.*)\"")
					.matcher(page);
			while (matcher.find()) {
				String name = matcher.group(1);
				String value = matcher.group(2);
				authFormParams.add(new BasicNameValuePair(name, value));
			}
			try {
				httppost.setEntity(new UrlEncodedFormEntity(authFormParams,
						HTTP.UTF_8));
			} catch (UnsupportedEncodingException e1) {
				// Do nothing
			}

			try {
				response = httpclient.execute(httppost, httpContext);
			} catch (Exception e) {
				// Тут что-то сломалось. Выходим
				httpclient.getConnectionManager().shutdown();
				throw new DownloaderException(
						"Ошибка авторизации - ошибка выполнения post запроса на "
								+ refUrl + " (" + e.getMessage() + ")");
			}
			entity = response.getEntity();
			if (entity == null) {
				// Опять что-то странное
				httpclient.getConnectionManager().shutdown();
				throw new DownloaderException(
						"Ошибка авторизации - ошибка выполнения post запроса на "
								+ refUrl + " - entity is null");
			}
			/*
			 * try { page = EntityUtils.toString(entity); } catch (IOException
			 * e) { httpclient.getConnectionManager().shutdown(); throw new
			 * DownloaderException( "Ошибка получения страницы авторизации (" +
			 * e.getMessage() + ")"); }
			 * System.out.println(response.getStatusLine());
			 */
		} finally {
			httppost.abort();
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ru.lsv.torrentchecker.server.rss.RssDownloaderAbstract#getDownloadFilenames
	 * (java.net.URL, java.util.List)
	 */
	@Override
	public List<RssPreparedUrlToDownload> getDownloadFilenames(URL rssUrl,
			List<RssConfigFile> configs) throws DownloaderException {
		// Тут все сложно.
		HttpResponse response;
		HttpGet httpGet;
		HttpEntity entity;
		httpGet = null;
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser parser;
		SaxHelper helper = new SaxHelper(configs);
		try {
			parser = spf.newSAXParser();
		} catch (ParserConfigurationException e1) {
			return Collections.emptyList();
		} catch (SAXException e1) {
			return Collections.emptyList();
		}
		// Выполняем запрос на страницу - после авторизации
		try {
			httpGet = new HttpGet(rssUrl.toURI());
		} catch (URISyntaxException e) {
			throw new DownloaderException("Ошибка преобразования url в uri o_O");
		}
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpGet.abort();
			throw new DownloaderException("Ошибка получения потока rss ("
					+ e.getMessage() + ")");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpGet.abort();
			throw new DownloaderException(
					"Ошибка получения потока rss - entity is null");
		}
		try {
			parser.parse(entity.getContent(), helper);
		} catch (IllegalStateException e) {
			return Collections.emptyList();
		} catch (SAXException e) {
			return Collections.emptyList();
		} catch (IOException e) {
			return Collections.emptyList();
		}
		return helper.getRes();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * ru.lsv.torrentchecker.server.rss.RssDownloaderAbstract#downloadFromRss
	 * (java.net.URL, java.util.Set, java.lang.String)
	 */
	@Override
	public void downloadFromRss(RssPreparedUrlToDownload downloadData,
			String pathToDownload) throws DownloaderException {
		if (downloadData.getAdditionalInfo().size() < 2) {
			throw new DownloaderException(
					"Загрузка торрента - не хватает дополнительных данных для загрузки");
		}
		String season = downloadData.getAdditionalInfo().get(0);
		String episode = downloadData.getAdditionalInfo().get(1);
		HttpResponse response;
		HttpGet httpGet;
		HttpEntity entity;
		httpGet = null;
		// Выполняем запрос на страницу - после авторизации
		try {
			httpGet = new HttpGet(downloadData.getDownloadUrl().toURI());
		} catch (URISyntaxException e) {
			throw new DownloaderException(
					"Загрузка торрента - ошибка преобразования url в uri o_O");
		}
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - ошибка загрузки (" + e.getMessage()
							+ ")");
		}
		Header[] headers = response.getHeaders("refresh");
		if (headers.length != 1) {
			// Тут что-то сломалось. Выходим
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение первого перенаправления - перенаправление отсутствует");
		}
		Matcher matcher = Pattern.compile("url=(.+)").matcher(
				headers[0].getValue());
		if (!matcher.find()) {
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение первого перенаправления - matcher не может найти перенаправление");
		}
		httpGet.abort();
		String redirUrl = matcher.group(1);
		matcher = Pattern.compile("=(\\d+)").matcher(redirUrl);
		if (!matcher.find()) {
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение первого перенаправления - matcher не может найти номер категории");
		}
		String category = matcher.group(1);
		// Выполняем запрос на страницу - после авторизации
		try {
			httpGet = new HttpGet(new URL(downloadData.getDownloadUrl()
					.getProtocol(), downloadData.getDownloadUrl().getHost(),
					"/nrdr2.php?c=" + category + "&s=" + season + "&e="
							+ episode).toURI());
		} catch (URISyntaxException e) {
			throw new DownloaderException(
					"Загрузка торрента - получение второго перенаправления - ошибка преобразования url в uri o_O");
		} catch (MalformedURLException e) {
			throw new DownloaderException(
					"Загрузка торрента - получение второго перенаправления - ошибка формирования или ошибка преобразования url в uri o_O");
		}
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение второго перенаправления - ошибка загрузки ("
							+ e.getMessage() + ")");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение второго перенаправления - entity is null");
		}
		String page;
		try {
			page = EntityUtils.toString(entity);
		} catch (Exception e) {
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение второго перенаправления - ошибка получение содержимого entity ("
							+ e.getMessage() + ")");
		}
		matcher = Pattern.compile(";\\surl=(.+)\"").matcher(page);
		if (!matcher.find()) {
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение второго перенаправления - matcher не может найти перенаправления");
		}
		String retreUrl = matcher.group(1);
		httpGet.abort();
		// Идем в retre за страницей загрузки
		try {
			httpGet = new HttpGet(new URL(retreUrl).toURI());
		} catch (URISyntaxException e) {
			throw new DownloaderException(
					"Загрузка торрента - получение третьего перенаправления - ошибка преобразования url в uri o_O");
		} catch (MalformedURLException e) {
			throw new DownloaderException(
					"Загрузка торрента - получение третьего перенаправления - ошибка формирования или ошибка преобразования url в uri o_O");
		}
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение третьего перенаправления - ошибка загрузки ("
							+ e.getMessage() + ")");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение третьего перенаправления - entity is null");
		}
		try {
			page = EntityUtils.toString(entity);
		} catch (Exception e) {
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение третьего перенаправления - ошибка получение содержимого entity ("
							+ e.getMessage() + ")");
		}
		String quality = downloadData.getConfig().getQuality()
				.replaceAll(" ", "\\\\s");
		matcher = Pattern.compile("<a\\shref=\"(.+)\"\\s.+" + quality).matcher(
				page);
		if (!matcher.find()) {
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение третьего перенаправления - matcher не может найти перенаправления");
		}
		String tracktorUrl = matcher.group(1);
		httpGet.abort();
		try {
			httpGet = new HttpGet(new URL(tracktorUrl).toURI());
		} catch (Exception e) {
			throw new DownloaderException(
					"Загрузка торрента - получение торрента - кривой URL загрузки ("
							+ e.getMessage() + ")");
		}
		try {
			response = httpclient.execute(httpGet, httpContext);
		} catch (Exception e) {
			// Тут что-то сломалось. Выходим
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение торрента - ошибка загрузки торрента ("
							+ e.getMessage() + ")");
		}
		entity = response.getEntity();
		if (entity == null) {
			// Опять что-то странное
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение торрента - ошибка загрузки торрента - entity is null");
		}
		// Получаем имя файла, в которое надо будет сохранять торрент
		headers = response.getHeaders("Content-Disposition");
		// Если такого имени сервер не передаст - заранее генерим временное имя
		// файла
		String saveToFile = downloadData.getFileName();
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
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение торрента - ошибка сохранения торрента - FileNotFoundException o_O");
		} catch (IOException e) {
			httpGet.abort();
			throw new DownloaderException(
					"Загрузка торрента - получение торрента - ошибка сохранения торрента - ошибка ввода вывода. Может место кончилось?");
		}
		httpGet.abort();

		// http://stackoverflow.com/questions/10995378/httpurlconnection-downloaded-file-name
	}

	/**
	 * Sax parser for rss stream
	 * 
	 * @author s.lezhnev
	 *
	 */
	@Slf4j
	private static class SaxHelper extends DefaultHandler {

		private StringBuilder tempVal;
		private List<RssConfigFile> configs;
		private boolean mustProcessNextLink;
		private RssConfigFile currConfig;
		private Pattern addInfoExtractor;
		private Pattern fileNameExtractor;
		@Getter
		private List<RssPreparedUrlToDownload> res;

		public SaxHelper(List<RssConfigFile> confs) {
			this.configs = confs;
			res = new LinkedList<>();
			addInfoExtractor = Pattern.compile("\\.S(\\d+)E(\\d+)\\.");
			fileNameExtractor = Pattern.compile("&(.+\\.torrent)");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
		 */
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			tempVal.append(new String(ch, start, length));
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
		 * java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			String tempValStr = tempVal.toString();
			if ("title".equalsIgnoreCase(qName)) {
				// Пройдемся по конфигам - поанализируем
				log.debug("title: {}", tempValStr);
				for (RssConfigFile config : configs) {
					log.debug("title: {}, beginPart: {}", tempValStr,
							config.getBeginPart());
					if (tempValStr.startsWith(config.getBeginPart())) {
						boolean mustAdd = true;
						for (String contain : config.getMustContain()) {
							if (tempVal.indexOf(contain) == -1) {
								mustAdd = false;
								break;
							}
						}
						log.debug("mustAdd: {}", mustAdd);
						if (mustAdd) {
							currConfig = config;
							mustProcessNextLink = true;
							break;
						}
					}
				}
			} else if ("link".equalsIgnoreCase(qName) && mustProcessNextLink
					&& (currConfig != null)) {
				Matcher addI = addInfoExtractor.matcher(tempVal);
				Matcher fn = fileNameExtractor.matcher(tempVal);
				URL downloadURL;
				try {
					downloadURL = new URL(tempValStr);
					if (addI.find() && fn.find()) {
						res.add(new RssPreparedUrlToDownload(fn.group(1),
								downloadURL, currConfig, Arrays
										.asList(new String[] { addI.group(1),
												addI.group(2) })));
					} else {
						log.debug("Cannot add to processing - cannot find file name or addInfo");
					}
				} catch (MalformedURLException e) {
				}
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
		 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			tempVal = new StringBuilder();
			if ("title".equalsIgnoreCase(qName)) {
				mustProcessNextLink = false;
				currConfig = null;
			}
		}

	}
}
