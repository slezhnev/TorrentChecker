/**
 * Интерфейс реализации загрузки торрентов с ресурсов
 */
package ru.lsv.torrentchecker.server.torrents;

import java.net.URL;

/**
 * Интерфейс реализации загрузки торрентов с ресурсов
 * 
 * @author admin
 */
public interface TorrentDownloaderInterface {

	/**
	 * Возвращает имя ресурса, с которым умеет работать
	 * @return см.описание
	 */
	public String getResource();
	
	/**
	 * Загрузка торрента с указанного URL'я <br>
	 * Загрузчик сам должен найти ссылку на загрузку торрента в указанном URL!
	 * 
	 * @param url
	 *            URL для загрузки (обычно - ссылка на страницу форума)
	 * @param userName
	 *            Имя пользователя для данного ресурса
	 * @param password
	 *            Пароль для данного ресурса
	 * @param pathToDownload
	 *            Куда загружать .torrent-файл
	 * @return Имя файла, куда был сохранен скачанный торрент
	 */
	public String downloadTorrent(URL url, String userName, String password,
			String pathToDownload) throws TorrentDownloaderException;

}
