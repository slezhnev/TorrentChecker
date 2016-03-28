package ru.lsv.torrentchecker.client;

import ru.lsv.torrentchecker.shared.WorkingResult;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Основной RPC-сервис
 */
@RemoteServiceRelativePath("torrentchecker")
public interface TorrentCheckerService extends RemoteService {
	/**
	 * Получение результатов обработки
	 * 
	 * @return Результаты обработки
	 */
	WorkingResult getResult();

	/**
	 * Выполняет обновление всех торрентов
	 */
	void refreshAll();

	/**
	 * Удаляет торрент из наблюдения
	 * 
	 * @param torrents
	 *            Перечень торрентов для удаления
	 * @return Результат обработки: <br/>
	 *         0 - все нормально <br/>
	 *         -1 - не найден файл(ы). Часть из файлов при этом может быть
	 *         ударно удалена
	 */
	Integer removeFromControl(String[] torrents);
}
