/**
 * Враппер для доступа до nnm-club.me с использованием IPv6
 */
package ru.lsv.torrentchecker.server.torrents.impl;

import org.apache.http.protocol.HttpContext;

import ru.lsv.torrentchecker.server.DownloaderException;

/**
 * Враппер для доступа до nnmclub.to с использованием IPv6 <br/>
 * По сути - просто унаследован от NNMClubDownloader с перекрытием одного метода
 * - getResource
 * 
 * @author s.lezhnev
 */
public class IPv6NNMClubDownloader extends NNMClubDownloader {

	/**
	 * см. TorrentDownloaderAbstract
	 * @param httpContextIn см. TorrentDownloaderAbstract
	 * @throws DownloaderException см. TorrentDownloaderAbstract
	 */
	public IPv6NNMClubDownloader(HttpContext httpContextIn)
			throws DownloaderException {
		super(httpContextIn);
	}

	/**
	 * см.описание
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderAbstract#getResource
	 *      ()
	 */
	@Override
	public String getResource() {
		return "ipv6.nnmclub.to";
	}

}
