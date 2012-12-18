package ru.lsv.torrentchecker.client;

import java.util.ArrayList;
import java.util.List;

import ru.lsv.torrentchecker.shared.WorkingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileProcessingResult;
import ru.lsv.torrentchecker.shared.WorkingResult.FileResult;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class TorrentChecker implements EntryPoint {
	/**
	 * Create a remote service proxy to talk to the server-side Greeting
	 * service.
	 */
	private final TorrentCheckerServiceAsync torrentCheckerService = GWT
			.create(TorrentCheckerService.class);
	/**
	 * Label для выдачи ошибок
	 */
	private Label errorLabel;
	/**
	 * Кнопка выполнения перезагрузки торрентов
	 */
	private Button forceReloadBtn;
	/**
	 * Кнопка удаления торрентов
	 */
	private Button deleteBtn;
	/**
	 * Таблица с отображением результатов
	 */
	private CellTable<TorrentOnControl> cellTable;
	/**
	 * Панель отображения всего, кроме errorLabel
	 */
	private VerticalPanel mainPanel;
	/**
	 * Текущий отображаемый список торрентов
	 */
	private List<TorrentOnControl> torrents = null;

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {

		// Add the nameField and sendButton to the RootPanel
		// Use RootPanel.get() to get the entire body element
		RootPanel rootPanel = RootPanel.get("root");

		VerticalPanel verticalPanel = new VerticalPanel();
		rootPanel.add(verticalPanel, 10, 10);
		verticalPanel.setSize("100%", "");

		errorLabel = new Label("Error label");
		verticalPanel.add(errorLabel);
		errorLabel.setVisible(false);
		errorLabel.setStyleName("errorLabel");

		mainPanel = new VerticalPanel();
		verticalPanel.add(mainPanel);
		verticalPanel.setCellHeight(mainPanel, "100%");
		mainPanel.setSize("100%", "100%");

		HorizontalPanel horizontalPanel = new HorizontalPanel();
		mainPanel.add(horizontalPanel);

		Button refreshBtn = new Button(
				"\u041E\u0431\u043D\u043E\u0432\u0438\u0442\u044C \u0438\u043D\u0444\u043E\u0440\u043C\u0430\u0446\u0438\u044E");
		refreshBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				cellTable.setVisibleRangeAndClearData(
						cellTable.getVisibleRange(), true);
				torrents = null;
				reloadTorrents();
			}
		});
		horizontalPanel.add(refreshBtn);

		HTML separator = new HTML("&nbsp;&nbsp;&nbsp;", true);
		horizontalPanel.add(separator);

		forceReloadBtn = new Button(
				"\u041F\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044C \u0442\u043E\u0440\u0440\u0435\u043D\u0442\u044B");
		forceReloadBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				cellTable.setVisibleRangeAndClearData(
						cellTable.getVisibleRange(), true);
				torrents = null;
				forceReloadBtn.setEnabled(false);
				forceReloadTorrents();
			}

		});
		horizontalPanel.add(forceReloadBtn);
		forceReloadBtn.setVisible(false);

		HTML separator1 = new HTML("&nbsp;&nbsp;&nbsp;", true);
		horizontalPanel.add(separator1);

		deleteBtn = new Button("Удалить выбранные торенты");
		deleteBtn.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				int toDelete = 0;
				for (TorrentOnControl torrent : torrents) {
					if (torrent.isChecked()) {
						toDelete++;
					}
				}
				// Что-то удалять будем только в том случае, если что-то выбрано
				if (toDelete > 0) {
					// Пройдемся по данным из cellTable
					final DeleteDialog dDialog = new DeleteDialog(
							new ClickHandler() {
								public void onClick(ClickEvent event) {
									// А вот тут-то мы и будем удалять!
									deleteTorrents();
								}
							}, "Удаление торрентов",
							"Вы уверены, что хотите удалить выбранные торренты из контроля?");
					dDialog.center();
					dDialog.show();
				}
			}

		});
		horizontalPanel.add(deleteBtn);
		deleteBtn.setVisible(false);

		HorizontalPanel horizontalPanel1 = new HorizontalPanel();
		mainPanel.add(horizontalPanel1);
		horizontalPanel1.setWidth("100%");
		mainPanel.setCellWidth(horizontalPanel1, "100%");

		Label lblNewLabel = new Label(
				"\u041A\u043E\u043D\u0442\u0440\u043E\u043B\u0438\u0440\u0443\u0435\u043C\u044B\u0435 \u0442\u043E\u0440\u0440\u0435\u043D\u0442\u044B");
		horizontalPanel1.add(lblNewLabel);
		horizontalPanel1.setCellVerticalAlignment(lblNewLabel,
				HasVerticalAlignment.ALIGN_MIDDLE);
		lblNewLabel.setStyleName("topLabel");

		HTML htmlNewHtml = new HTML(
				"<div class=\"nameCellNEW\">\u041D\u043E\u0432\u044B\u0439 \u0442\u043E\u0440\u0440\u0435\u043D\u0442</div>\r\n<div class=\"nameCellMODIFYED\">\u041E\u0431\u043D\u043E\u0432\u043B\u0435\u043D\u043D\u044B\u0439 \u0442\u043E\u0440\u0440\u0435\u043D\u0442</div>\r\n<div class=\"nameCellUNCHANGED\">\u0422\u043E\u0440\u0440\u0435\u043D\u0442 \u043D\u0435 \u043E\u0431\u043D\u043E\u0432\u043B\u044F\u043B\u0441\u044F</div>\r\n<div class=\"nameCellFAIL\">\u041E\u0448\u0438\u0431\u043A\u0430 \u043E\u0431\u0440\u0430\u0431\u043E\u0442\u043A\u0438 \u0442\u043E\u0440\u0440\u0435\u043D\u0442\u0430</div>",
				true);
		horizontalPanel1.add(htmlNewHtml);
		horizontalPanel1.setCellHorizontalAlignment(htmlNewHtml,
				HasHorizontalAlignment.ALIGN_CENTER);

		cellTable = new CellTable<TorrentOnControl>();
		mainPanel.add(cellTable);
		cellTable.setSize("100%", "100%");
		mainPanel.setCellHeight(cellTable, "100%");
		mainPanel.setCellWidth(cellTable, "100%");

		Column<TorrentOnControl, Boolean> selectionColumn = new Column<TorrentOnControl, Boolean>(
				new CheckboxCell()) {
			@Override
			public Boolean getValue(TorrentOnControl object) {
				return object.isChecked();
			}
		};
		cellTable.addColumn(selectionColumn, "");
		cellTable.setColumnWidth(selectionColumn, "10%");
		selectionColumn
				.setFieldUpdater(new FieldUpdater<TorrentOnControl, Boolean>() {

					@Override
					public void update(int index, TorrentOnControl object,
							Boolean value) {
						// Сохраним...
						object.setChecked(value);
					}

				});

		Column<TorrentOnControl, TorrentOnControl> nameColumn = new Column<TorrentOnControl, TorrentOnControl>(
				new NameCell()) {
			@Override
			public TorrentOnControl getValue(TorrentOnControl object) {
				return object;
			}
		};
		nameColumn.setSortable(true);
		cellTable.addColumn(nameColumn,
				"\u041D\u0430\u0437\u0432\u0430\u043D\u0438\u0435");
		cellTable.setColumnWidth(nameColumn, "40%");

		TextColumn<TorrentOnControl> statusColumn = new TextColumn<TorrentOnControl>() {
			@Override
			public String getValue(TorrentOnControl object) {
				if (object.getResult().getProcessingResult() != null) {
					return object.getResult().getProcessingResult();
				} else {
					return "";
				}
			}
		};
		cellTable.addColumn(statusColumn,
				"\u0421\u0442\u0430\u0442\u0443\u0441");
		cellTable.setColumnWidth(statusColumn, "40%");

		TextColumn<TorrentOnControl> reloadTimeColumn = new TextColumn<TorrentOnControl>() {

			/**
			 * Date formatter
			 */
			public DateTimeFormat dtf = DateTimeFormat
					.getFormat("dd.MM.yyyy HH:mm:ss");

			@Override
			public String getValue(TorrentOnControl object) {
				if (object.getResult().getProcessingDate() != null) {
					return dtf.format(object.getResult().getProcessingDate());
				} else {
					return "";
				}
			}
		};
		cellTable.addColumn(reloadTimeColumn, "Обновлено");
		cellTable.setColumnWidth(reloadTimeColumn, "20&");

		reloadTorrents();

	}

	/**
	 * Выполняет вызов сервиса и перезагрузку списка торрентов
	 */
	private void reloadTorrents() {
		torrentCheckerService.getResult(new AsyncCallback<WorkingResult>() {

			@Override
			public void onFailure(Throwable caught) {
				// Прячем кнопку перезагрузки + таблицу и выдаем ошибку
				torrents = null;
				cellTable.setVisible(false);
				forceReloadBtn.setVisible(false);
				deleteBtn.setVisible(false);
				errorLabel.setVisible(true);
				errorLabel
						.setText("Ошибка загрузки списка торрентов с сервера - "
								+ caught.getMessage());
			}

			@Override
			public void onSuccess(WorkingResult result) {
				forceReloadBtn.setEnabled(true);
				if (result == null) {
					// Это вполне возможно в случае, если у нас еще не
					// загрузились торренты
					torrents = null;
					forceReloadBtn.setVisible(false);
					deleteBtn.setVisible(false);
					List<TorrentOnControl> list = new ArrayList<TorrentOnControl>();
					list.add(new TorrentOnControl(
							new FileResult(
									"Ожидаем первоначальной загрузки торрентов на сервере...",
									FileProcessingResult.UNCHANGED)));
					cellTable.setRowData(list);
				} else if (result.getConfigLoadError() != null) {
					/**
					 * Если есть проблема с конфигурацией сервера - выдаем
					 */
					torrents = null;
					mainPanel.setVisible(false);
					errorLabel.setVisible(true);
					errorLabel.setText(SafeHtmlUtils.fromTrustedString(
							"Ошибка загрузки конфигурации сервера").asString());
				} else {
					cellTable.setVisible(true);
					forceReloadBtn.setVisible(true);
					deleteBtn.setVisible(true);
					errorLabel.setVisible(false);
					// Формируем список
					List<TorrentOnControl> list = new ArrayList<TorrentOnControl>();
					for (FileResult res : result.getFilesOnControl()) {
						list.add(new TorrentOnControl(res));
					}
					torrents = list;
					cellTable.setRowData(list);
				}
			}

		});
	}

	/**
	 * Выполняет запуск перезагрузки всех торрентов на сервере
	 */
	@SuppressWarnings("rawtypes")
	private void forceReloadTorrents() {
		torrentCheckerService.refreshAll(new AsyncCallback() {

			@Override
			public void onFailure(Throwable caught) {
				torrents = null;
				cellTable.setVisible(false);
				forceReloadBtn.setVisible(false);
				deleteBtn.setVisible(false);
				errorLabel.setVisible(true);
				errorLabel.setText("Ошибка перезагрузки торрентов - "
						+ caught.getMessage());
			}

			@Override
			public void onSuccess(Object result) {
				// Еще перезагружаем список торрентов
				reloadTorrents();
			}
		});

	}

	/**
	 * Удаление выбранных торрентов
	 */
	private void deleteTorrents() {
		List<String> toDelete = new ArrayList<String>();
		for (TorrentOnControl torrent : torrents) {
			if (torrent.isChecked()) {
				toDelete.add(torrent.getResult().getName());
			}
		}
		torrentCheckerService.removeFromControl(
				toDelete.toArray(new String[0]), new AsyncCallback<Integer>() {

					@Override
					public void onFailure(Throwable caught) {
						torrents = null;
						cellTable.setVisible(false);
						forceReloadBtn.setVisible(false);
						deleteBtn.setVisible(false);
						errorLabel.setVisible(true);
						errorLabel.setText("Ошибка удаления торрентов - "
								+ caught.getMessage());
					}

					@Override
					public void onSuccess(Integer result) {
						// Еще перезагружаем список торрентов
						reloadTorrents();
					}
				});
	}

	/**
	 * Custom rendered для имени торрента
	 * 
	 * @author s.lezhnev
	 */
	static class NameCell extends AbstractCell<TorrentOnControl> {
		/**
		 * Create a singleton instance of the templates used to render the cell.
		 */
		private static Templates templates = GWT.create(Templates.class);

		/**
		 * Темплейт для отображения имени
		 * 
		 * @author s.lezhnev
		 */
		interface Templates extends SafeHtmlTemplates {
			/**
			 * The template for this Cell, which includes styles and a value.
			 * 
			 * @param styles
			 *            the styles to include in the style attribute of the
			 *            div
			 * @param value
			 *            the safe value. Since the value type is
			 *            {@link SafeHtml}, it will not be escaped before
			 *            including it in the template. Alternatively, you could
			 *            make the value type String, in which case the value
			 *            would be escaped.
			 * @return a {@link SafeHtml} instance
			 */
			@SafeHtmlTemplates.Template("<div class=\"{0}\">{1}</div>")
			SafeHtml cell(String style, SafeHtml value);
		}

		@Override
		public void render(com.google.gwt.cell.client.Cell.Context context,
				TorrentOnControl value, SafeHtmlBuilder sb) {
			if (value == null) {
				// Do nothing
				return;
			}
			SafeHtml name = SafeHtmlUtils.fromString(value.getResult()
					.getName());
			SafeHtml rendered = templates.cell("nameCell"
					+ value.getResult().getResult(), name);
			sb.append(rendered);

		}

	}
}
