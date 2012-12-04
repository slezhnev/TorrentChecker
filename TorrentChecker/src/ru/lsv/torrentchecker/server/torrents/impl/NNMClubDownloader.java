/**
 * Обеспечивает реализацию интерфейса загрузки с nnm-club.ru
 */
package ru.lsv.torrentchecker.server.torrents.impl;

import java.net.HttpURLConnection;
import java.net.URL;

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
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderInterface#
	 *      downloadTorrent(java.net.URL, java.lang.String, java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	public String downloadTorrent(URL url, String userName, String password,
			String pathToDownload) {
		// Пробуем загружать файл
		// http://stackoverflow.com/questions/10995378/httpurlconnection-downloaded-file-name 
		return "";
	}

}
