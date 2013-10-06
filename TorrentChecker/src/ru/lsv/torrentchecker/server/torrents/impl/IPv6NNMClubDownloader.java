/**
 * Враппер для доступа до nnm-club.me с использованием IPv6
 */
package ru.lsv.torrentchecker.server.torrents.impl;

/**
 * Враппер для доступа до nnm-club.me с использованием IPv6 <br/>
 * По сути - просто унаследован от NNMClubDownloader с перекрытием одного метода
 * - getResource
 * 
 * @author s.lezhnev
 */
public class IPv6NNMClubDownloader extends NNMClubDownloader {

	/**
	 * см.описание
	 * 
	 * @see ru.lsv.torrentchecker.server.torrents.TorrentDownloaderInterface#getResource
	 *      ()
	 */
	@Override
	public String getResource() {
		return "ipv6.nnm-club.me";
	}

}
