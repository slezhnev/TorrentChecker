/**
 * Враппер для доступа до nnm-club.me с использованием IPv6
 */
package ru.lsv.torrentchecker.server.torrents.impl;

import org.apache.http.protocol.HttpContext;

import ru.lsv.torrentchecker.server.torrents.TorrentDownloaderException;

/**
 * Враппер для доступа до nnm-club.me с использованием IPv6 <br/>
 * По сути - просто унаследован от NNMClubDownloader с перекрытием одного метода
 * - getResource
 * 
 * @author s.lezhnev
 */
public class IPv6NNMClubDownloader extends NNMClubDownloader {

	/**
	 * см. TorrentDownloaderAbstract
	 * @param httpContextIn см. TorrentDownloaderAbstract
	 * @throws TorrentDownloaderException см. TorrentDownloaderAbstract
	 */
	public IPv6NNMClubDownloader(HttpContext httpContextIn)
			throws TorrentDownloaderException {
		super(httpContextIn);
		// TODO Auto-generated constructor stub
	}

	/**
	 * см.описание
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderAbstract#getResource
	 *      ()
	 */
	@Override
	public String getResource() {
		return "ipv6.nnm-club.me";
	}

}
