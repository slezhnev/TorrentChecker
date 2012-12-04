/**
 * ��������� ���������� �������� ��������� � ��������
 */
package ru.lsv.torrentchecker.server.torrents;

import java.net.URL;

/**
 * ��������� ���������� �������� ��������� � ��������
 * 
 * @author admin
 */
public interface TorrentDownloaderInterface {

	/**
	 * ���������� ��� �������, � ������� ����� ��������
	 * @return ��.��������
	 */
	public String getResource();
	
	/**
	 * �������� �������� � ���������� URL'� <br>
	 * ��������� ��� ������ ����� ������ �� �������� �������� � ��������� URL!
	 * 
	 * @param url
	 *            URL ��� �������� (������ - ������ �� �������� ������)
	 * @param userName
	 *            ��� ������������ ��� ������� �������
	 * @param password
	 *            ������ ��� ������� �������
	 * @param pathToDownload
	 *            ���� ��������� .torrent-����
	 * @return ��� �����, ���� ��� �������� ��������� �������
	 */
	public String downloadTorrent(URL url, String userName, String password,
			String pathToDownload);

}
