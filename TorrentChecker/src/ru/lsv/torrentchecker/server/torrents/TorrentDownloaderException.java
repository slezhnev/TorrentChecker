/**
 * Exception при обработке торрента
 */
package ru.lsv.torrentchecker.server.torrents;

/**
 * Exception при обработке торрента
 * 
 * @author admin
 */
public class TorrentDownloaderException extends Exception {

	/**
	 * For serializatiod
	 */
	private static final long serialVersionUID = -7175114806920914403L;

	/**
	 * Default constructor
	 * 
	 * @param arg0
	 */
	public TorrentDownloaderException(String arg0) {
		super(arg0);
	}

}
