/**
 *  Результат работы сервиса
 */
package ru.lsv.torrentchecker.shared;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.google.gwt.i18n.client.DateTimeFormat;

/**
 * Результат работы сервиса
 * 
 * @author s.lezhnev
 */
public class WorkingResult implements Serializable {

	/**
	 * For serialization
	 */
	private static final long serialVersionUID = 307971321500087338L;

	/**
	 * Список файлов с результатами обработки<br/>
	 * key - файлы, value - результаты работы <br/>
	 * Если value.length == 0 - то файл обработан без проблем. Иначе - сообщение
	 * о проблеме
	 */
	private List<FileResult> filesOnControl;

	/**
	 * Сообщение (возможное) о том, что что-то там упало при загрузке
	 */
	private String configLoadError;

	/**
	 * Конструктор ТОЛЬКО для возможности использования в RPC-сервисе! Не делает
	 * ваапчего ничего
	 */
	public WorkingResult() {

	}

	/**
	 * Создание результата работы со списком контролируемых файлов<br>
	 * key - файлы, value - результаты обработки
	 * 
	 * @param filesOnControl
	 *            Список обработанных файлов
	 */
	public WorkingResult(List<FileResult> filesOnControl) {
		this.filesOnControl = filesOnControl;
		configLoadError = null;
	}

	/**
	 * Создание результата работы при ошибке инициализации
	 * 
	 * @param configLoadError
	 *            Сообщение ошибки инициализации
	 */
	public WorkingResult(String configLoadError) {
		this.configLoadError = configLoadError;
		filesOnControl = null;
	}

	/**
	 * @return the filesOnControl
	 */
	public List<FileResult> getFilesOnControl() {
		return filesOnControl;
	}

	/**
	 * @return the configLoadError
	 */
	public String getConfigLoadError() {
		return configLoadError;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		DateTimeFormat sdf = DateTimeFormat.getFormat("dd.MM.yyyy HH:mm:ss");
		if (configLoadError != null) {
			return "Ошибка загрузки конфига: \n " + configLoadError;
		} else {
			StringBuffer str = new StringBuffer(
					"Результаты обработки файлов:\n");
			for (FileResult file : filesOnControl) {
				str.append(file.getName()).append(" - ");
				switch (file.getResult()) {
				case NEW: {
					str.append("новый торрент");
					break;
				}
				case MODIFYED: {
					str.append("торрент изменен");
					break;
				}
				case UNCHANGED: {
					str.append("торрент не изменился");
					break;
				}
				default: {
					str.append("ошибка обработки торрента: ").append(
							file.getProcessingResult());
				}
				}
				str.append(", время - ")
						.append(sdf.format(file.getProcessingDate()))
						.append("\n");
			}
			return str.toString();
		}
	}

	/**
	 * Вспомогательный класс - результат обработки одного файла
	 * 
	 * @author s.lezhnev
	 */
	public static class FileResult implements Serializable {

		/**
		 * For serialization
		 */
		private static final long serialVersionUID = 8625174220600334535L;
		/**
		 * Имя файла
		 */
		private String name;
		/**
		 * Результат обработки
		 */
		private String processingResult;
		/**
		 * Время обработки
		 */
		private Date processingDate;
		/**
		 * Результат обреботки файла
		 */
		private FileProcessingResult result;

		/**
		 * ONLY for serialization!
		 */
		public FileResult() {
			
		}
		
		/**
		 * Конструктор успешной обработки файла - без сообщения
		 * 
		 * @param name
		 *            Имя файла
		 * @param result
		 *            Результат обработки файла
		 */
		public FileResult(String name, FileProcessingResult result) {
			this.name = name;
			this.result = result;
			processingDate = new Date();
			processingResult = "Ok";
		}

		/**
		 * Конструктор результата с сообщением об ошибке <br/>
		 * Результат обработки автоматически ставится в FAIL
		 * 
		 * @param name
		 *            Имя файла
		 * @param processingResult
		 *            Результат обработки - сообщение об ошибке
		 */
		public FileResult(String name, String processingResult) {
			if (processingResult == null) {
				this.processingResult = "";
			} else {
				this.processingResult = processingResult;
			}
			this.result = FileProcessingResult.FAIL;
			this.name = name;
			processingDate = new Date();
		}

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the processingResult
		 */
		public String getProcessingResult() {
			return processingResult;
		}

		/**
		 * @return the processingDate
		 */
		public Date getProcessingDate() {
			return processingDate;
		}

		/**
		 * @return the result
		 */
		public FileProcessingResult getResult() {
			return result;
		}

	}

	/**
	 * Результаты обработки файла
	 * 
	 * @author s.lezhnev
	 * 
	 */
	public static enum FileProcessingResult {
		/**
		 * Новый торрент - отправлен в загрузку
		 */
		NEW,
		/**
		 * Торрент на контроле - и он изменился
		 */
		MODIFYED,
		/**
		 * Торрент на контроле - и он не менялся
		 */
		UNCHANGED,
		/**
		 * Что-то упало при обработке файла
		 */
		FAIL
	}

}
