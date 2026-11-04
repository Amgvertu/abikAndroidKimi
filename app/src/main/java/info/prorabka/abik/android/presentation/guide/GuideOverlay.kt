package info.prorabka.abik.android.presentation.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class GuideStep(val title: String, val text: String)

private fun steps(screen: String, role: String): List<GuideStep> {
    val manager = role == "OWNER" || role == "ADMIN" || role == "MODERATOR"
    val common = when (screen) {
        "tab_home" -> listOf(
            GuideStep("Главная", "Здесь показаны ближайшие события, ваш ответ в опросе и финансовая сводка."),
            GuideStep("Быстрые действия", "Нажмите на событие, чтобы открыть подробности и изменить ответ. Уведомления находятся под значком колокольчика."),
        )
        "tab_subscription" -> listOf(
            GuideStep("Абонемент", if (manager) "Показаны балансы участников. Откройте участника для деталей начислений и платежей." else "Здесь показаны ваши начисления, платежи и текущий баланс."),
            GuideStep("Операции", if (manager) "Платёж можно добавить из меню участника или кнопкой на экране. Перед удалением операции приложение попросит подтверждение." else "Нажмите на начисление, чтобы посмотреть его состояние и историю оплат."),
        )
        "tab_games" -> listOf(
            GuideStep("Календарь игр", "Все события показаны на календаре. Кнопки «Тренировки», «Регулярка» и «Двухсторонка» включают и выключают фильтры."),
            GuideStep("Опрос", "Нажмите на дату или откройте событие и выберите «Иду», «Не иду» или «Под вопросом». При переполнении основного состава формируется резерв."),
            GuideStep("Управление", if (manager || role == "TRAINER") "Создание событий доступно кнопкой добавления. Внутри события можно назначить звенья и цвета свитеров в соответствии с вашими правами." else "Если вы в резерве, это будет явно указано в карточке события. После окончания события опрос закрывается."),
        )
        "charge_lists" -> listOf(
            GuideStep("Создание списка", "Нажмите «+», введите понятное название и отметьте участников. Сохраните список кнопкой внизу диалога."),
            GuideStep("Использование", "Список появится на экране команды и в формах начислений. Чтобы изменить состав, нажмите карандаш; удаление выполняется после подтверждения."),
        )
        "charge_create" -> listOf(
            GuideStep("Новое начисление", "Выберите группу, укажите сумму и срок оплаты. В получателях можно выбрать отдельных участников или готовый список."),
            GuideStep("Проверка", "Проверьте сумму и получателей перед сохранением. После создания начисление появится в абонементе выбранных участников."),
        )
        "charge_templates" -> listOf(
            GuideStep("Шаблоны", "Шаблон сохраняет сумму, способ распределения и получателей для повторяющихся начислений."),
            GuideStep("Настройка", "Создайте шаблон кнопкой «+», выберите тип и участников. Активность шаблона можно изменить переключателем."),
        )
        "event_form" -> listOf(
            GuideStep("Создание события", "Выберите тип, дату, время и место. Для опроса укажите необходимое число полевых игроков и стоимость при необходимости."),
            GuideStep("Сохранение", "Проверьте параметры и нажмите «Сохранить». После создания участники увидят событие в календаре и смогут проголосовать."),
        )
        "expenses" -> listOf(
            GuideStep("Учёт расходов", "Нажмите «+», выберите категорию, укажите дату, сумму и комментарий. Общая сумма показана над списком."),
            GuideStep("Изменение", "Владелец и администратор могут изменить или удалить запись кнопками справа. Модератор добавляет и просматривает расходы."),
        )
        "reports" -> listOf(
            GuideStep("Отчёты", "Сверху показана финансовая сводка команды и балансы участников."),
            GuideStep("Excel", "Выберите фильтры и скачайте начисления, финансовую сводку, всю команду или отдельный именованный список. Файл откроется через системное меню «Поделиться»."),
        )
        "tab_team" -> listOf(
            GuideStep("Состав команды", "Игроки сгруппированы по амплуа. Первая вкладка содержит всю команду, остальные — именованные списки."),
            GuideStep("Действия с участниками", if (manager) "Долгое нажатие на участника открывает доступные действия: платёж, списки, роли и другие операции. Кнопка «+» отправляет приглашение." else "Нажмите на участника, чтобы открыть его карточку. Выход из команды находится в меню экрана."),
            GuideStep("Списки", if (manager) "Кнопка «+» среди вкладок создаёт список. Долгое нажатие на вкладку списка открывает редактирование и удаление." else "Именованные списки помогают команде формировать составы и получателей начислений."),
        )
        else -> listOf(
            GuideStep("Профиль", "Здесь изменяются личные данные, фотография, амплуа и размеры экипировки."),
            GuideStep("Настройки", "В профиле доступны тема, сервер подключения, смена телефона и выход из аккаунта."),
        )
    }
    return common
}

@Composable
fun GuideOverlay(
    key: String,
    viewModel: GuideViewModel,
) {
    val parts = key.split(':', limit = 2)
    if (parts.size != 2) return
    val items = remember(key) { steps(parts[0], parts[1]) }
    var index by remember(key) { mutableIntStateOf(0) }
    val step = items[index]
    AlertDialog(
        onDismissRequest = { viewModel.skip(key) },
        title = { Text(step.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { (index + 1).toFloat() / items.size },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(step.text, style = MaterialTheme.typography.bodyLarge)
                Text("Шаг ${index + 1} из ${items.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = { viewModel.skip(key) }) { Text("Пропустить") }
                TextButton(onClick = {
                    if (index == items.lastIndex) viewModel.complete(key) else index += 1
                }) { Text(if (index == items.lastIndex) "Готово" else "Далее") }
            }
        },
    )
}
