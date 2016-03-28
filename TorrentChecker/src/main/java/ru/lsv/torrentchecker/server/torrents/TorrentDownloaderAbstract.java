package ru.lsv.torrentchecker.server.torrents;

import java.net.URL;

import org.apache.http.protocol.HttpContext;

import ru.lsv.torrentchecker.server.DownloaderException;
import ru.lsv.torrentchecker.server.abstracts.FileDownloaderAbstract;

/**
 * Абстрактный класс загрузки торрентов с ресурсов
 * 
 * @author s.lezhnev
 */
public abstract class TorrentDownloaderAbstract extends FileDownloaderAbstract {

	public TorrentDownloaderAbstract(HttpContext httpContextIn)
			throws DownloaderException {
		super(httpContextIn);
	}

	/**
	 * Загрузка торрента с указанного URL'я <br>
	 * Загрузчик сам должен найти ссылку на загрузку торрента в указанном URL!
	 * 
	 * @param url
	 *            URL для загрузки (обычно - ссылка на страницу форума)
	 * @param pathToDownload
	 *            Куда загружать .torrent-файл
	 * @return Имя файла, куда был сохранен скачанный торрент
	 * @throws DownloaderException
	 *             В случае проблем со скачиванием файла
	 */
	public abstract String downloadTorrent(URL url, String pathToDownload)
			throws DownloaderException;

}
