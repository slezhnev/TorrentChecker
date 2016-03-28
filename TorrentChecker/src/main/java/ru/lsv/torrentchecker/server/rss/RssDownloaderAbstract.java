package ru.lsv.torrentchecker.server.rss;

import java.net.URL;
import java.util.List;

import lombok.Value;

import org.apache.http.protocol.HttpContext;

import ru.lsv.torrentchecker.server.DownloaderException;
import ru.lsv.torrentchecker.server.abstracts.FileDownloaderAbstract;

/**
 * Абстрактный класс загрузки с rss
 * 
 * @author s.lezhnev
 *
 */
public abstract class RssDownloaderAbstract extends FileDownloaderAbstract {

	public RssDownloaderAbstract(HttpContext httpContextIn)
			throws DownloaderException {
		super(httpContextIn);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Получает из rss список файлов для загрузки, подходящих под критерии
	 * configs
	 * 
	 * @param rssUrl
	 *            RSS URL
	 * @param configs
	 *            Список критериев для загрузки
	 * @return Список пар: имя файла - пара (url для загрузки - дополнительные
	 *         параметры загрузки)
	 * @throws DownloaderException
	 *             В случае проблем с получением данных
	 */
	public abstract List<RssPreparedUrlToDownload> getDownloadFilenames(
			URL rssUrl, List<RssConfigFile> configs) throws DownloaderException;

	/**
	 * Загружает торрент с указанного URL
	 * 
	 * @param downloadUrl
	 *            url для загрузки
	 * @param additionalData
	 *            Дополнительные параметры для загрузки
	 * @param pathToDownload
	 *            Куда загружать файл
	 * @throws DownloaderException
	 *             В случае проблем с получением данных
	 */
	public abstract void downloadFromRss(RssPreparedUrlToDownload downloadData,
			String pathToDownload) throws DownloaderException;

	/**
	 * Класс - storage для информации о загрузке непосредственно торрента
	 * 
	 * @author s.lezhnev
	 *
	 */
	@Value
	public static class RssPreparedUrlToDownload {
		private String fileName;
		private URL downloadUrl;
		private RssConfigFile config;
		private List<String> additionalInfo;

	}

}
