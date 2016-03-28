/**
 * Диалог для запроса удаления торрентов из списка
 */
package ru.lsv.torrentchecker.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;

/**
 * Диалог для запроса удаления торрентов из списка
 * 
 * @author s.lezhnev
 * 
 */
public class DeleteDialog extends DialogBox {
	/**
	 * Default constructor
	 * 
	 * @param delHandler
	 *            Обработчик нажатия на "Удалить
	 * @param title
	 *            Заголовок окна
	 * @param message
	 *            Сообщение
	 */
	public DeleteDialog(final ClickHandler delHandler, String title, String message) {

		this.setText(title);

		FlexTable flexTable = new FlexTable();
		flexTable.setCellSpacing(5);
		flexTable.setCellPadding(5);
		setWidget(flexTable);
		flexTable.setSize("100%", "100%");

		Label messageLabel = new Label(message);
		flexTable.setWidget(0, 0, messageLabel);

		Button delBtn = new Button("Удалить");
		flexTable.setWidget(1, 0, delBtn);
		delBtn.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				hide();
				delHandler.onClick(event);
			}			
		});

		Button cancelBtn = new Button("Отмена");
		flexTable.setWidget(1, 1, cancelBtn);
		cancelBtn.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				// Просто тупо закрываем. Печалька
				hide();
			}

		});

		flexTable.getFlexCellFormatter().setColSpan(0, 0, 2);
		flexTable.getCellFormatter().setHorizontalAlignment(0, 0,
				HasHorizontalAlignment.ALIGN_CENTER);
		flexTable.getCellFormatter().setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_CENTER);
		flexTable.getCellFormatter().setHorizontalAlignment(1, 1, HasHorizontalAlignment.ALIGN_CENTER);
	}

}
