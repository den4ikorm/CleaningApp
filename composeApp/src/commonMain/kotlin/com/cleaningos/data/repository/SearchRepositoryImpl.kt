package com.cleaningos.data.repository

import com.cleaningos.domain.model.*
import com.cleaningos.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * SearchRepositoryImpl — parses local MOD-files and runs keyword matching.
 *
 * In production: ships bundled JSON knowledge bases (from the Python kb_*.py modules
 * converted to JSON) and applies the same EDI + checklist selection logic.
 */
class SearchRepositoryImpl(
    private val modFileLoader: ModFileLoader
) : SearchRepository {

    override suspend fun query(text: String): SearchResult {
        val lower  = text.lowercase()
        val edi    = parseEdi(lower)
        val cl     = selectChecklist(edi, lower)
        val phrase = supportPhrase(edi.E)
        val kb     = searchKnowledgeBases(text)

        return SearchResult(
            input         = text,
            edi           = edi,
            checklist     = cl,
            supportPhrase = phrase,
            chemicalWarning = checkChemical(lower),
            stainInfo     = null,
            safetyAlert   = isSafetyCritical(lower),
            alertText     = if (isSafetyCritical(lower)) "Немедленно проветрите помещение!" else "",
            kbResults     = kb
        )
    }

    override suspend fun searchKnowledgeBases(query: String): List<KBResult> {
        return modFileLoader.loadAll()
            .filter { kb ->
                query.lowercase().split(" ").any { word ->
                    word.length > 2 && (kb.title.lowercase().contains(word) ||
                        kb.snippet.lowercase().contains(word))
                }
            }
            .take(5)
    }

    override fun getKnowledgeBases(): Flow<List<KBResult>> = flow {
        emit(modFileLoader.loadAll())
    }

    // ── EDI Parser ─────────────────────────────────────────────────────────────

    private fun parseEdi(text: String): EdiScore {
        val eSignals = listOf("устала", "нет сил", "сил нет", "выдохлась", "убита", "мертвая")
        val eHigh    = listOf("энергии полно", "заряжена", "полна сил", "бодрая")
        val dHigh    = listOf("клиент едет", "клиент ждет", "срочно", "экстренно", "уже едут")
        val iHigh    = listOf("генеральная", "послеремонт", "всё", "полностью")

        val E = when {
            eSignals.any { text.contains(it) } -> 1
            eHigh.any    { text.contains(it) } -> 5
            else                               -> 3
        }
        val D = if (dHigh.any { text.contains(it) }) 5 else 3
        val I = if (iHigh.any { text.contains(it) }) 5 else 3

        return EdiScore(E = E, D = D, I = I)
    }

    private fun selectChecklist(edi: EdiScore, text: String): Checklist? {
        val isGeneral  = text.contains("генеральная") || edi.I >= 5
        val isEmergency = edi.D >= 5 && edi.E <= 2
        val isPostRepair = text.contains("послеремонт") || text.contains("цемент")

        return when {
            isEmergency  -> Checklist(
                id = "CL_001", name = "Кухня. Экстренный",
                estimatedMinutes = 20,
                steps = listOf(
                    ChecklistStep("Побрызгать Fairy на плиту — откисает (2 мин)"),
                    ChecklistStep("Протереть мойку и кран влажной тряпкой (2 мин)"),
                    ChecklistStep("Быстро по столешнице одним проходом (2 мин)"),
                    ChecklistStep("Вернуться к плите — снять жир (3 мин)"),
                    ChecklistStep("Пол — швабра по периметру + центр (4 мин)"),
                    ChecklistStep("Проветрить 2 мин перед выходом")
                ),
                warnings = listOf("Не используй агрессивные средства — нет сил следить")
            )
            isGeneral    -> Checklist(
                id = "CL_020", name = "Кухня. Генеральная",
                estimatedMinutes = 90,
                steps = listOf(
                    ChecklistStep("[Замачивание] Конфорки в раковине: Fairy + горячая вода"),
                    ChecklistStep("[Замачивание] Решётки духовки в ванне с обезжиривателем"),
                    ChecklistStep("[Замачивание] Духовка изнутри: гель Off или Grass → оставить"),
                    ChecklistStep("[Пока замачивается] Холодильник внутри: полки, ящики, уплотнитель"),
                    ChecklistStep("[Пока замачивается] Верх шкафов: пыль + жир"),
                    ChecklistStep("[Пока замачивается] Вытяжка: сетка + корпус"),
                    ChecklistStep("[Пока замачивается] Микроволновка изнутри: лимонная вода"),
                    ChecklistStep("[Возврат к плите] Конфорки, решётки, духовка изнутри"),
                    ChecklistStep("[Возврат к плите] Фартук: швы (зубная щётка)"),
                    ChecklistStep("[Финиш] Все фасады шкафов"),
                    ChecklistStep("[Финиш] Столешница"),
                    ChecklistStep("[Финиш] Мойка до блеска"),
                    ChecklistStep("[Финиш] Пол: два прохода"),
                    ChecklistStep("[Финиш] Проветрить 5 мин")
                )
            )
            isPostRepair -> Checklist(
                id = "CL_080", name = "Послеремонт",
                estimatedMinutes = 180,
                steps = listOf(
                    ChecklistStep("Пылесос строительный — весь мусор с пола (15 мин)"),
                    ChecklistStep("Окна: снять защитную плёнку, вымыть стекло"),
                    ChecklistStep("Плитка: цемент — кислотный очиститель, выдержать 10 мин"),
                    ChecklistStep("Сантехника: снять наклейки, очистить"),
                    ChecklistStep("Радиаторы: пыль внутри секций (кисть)"),
                    ChecklistStep("Пол финальный: влажная уборка × 2")
                ),
                warnings = listOf("Работай в перчатках — строительная пыль опасна")
            )
            else         -> Checklist(
                id = "CL_010", name = "Стандартная уборка",
                estimatedMinutes = 45,
                steps = listOf(
                    ChecklistStep("Пыль с горизонтальных поверхностей"),
                    ChecklistStep("Пылесос / подметание"),
                    ChecklistStep("Влажная уборка пола"),
                    ChecklistStep("Санузел: унитаз, раковина, зеркало"),
                    ChecklistStep("Кухня: плита, мойка, столешница"),
                    ChecklistStep("Вынести мусор"),
                    ChecklistStep("Проверить — фото финала")
                )
            )
        }
    }

    private fun supportPhrase(energy: Int): String? = when (energy) {
        1    -> "Вы уже молодец, что вообще пришли. Делай минимум — и домой."
        2    -> "Тело знает предел. Сделай главное, остальное подождёт."
        3    -> "Рабочий режим. Всё по плану."
        4    -> "Хороший настрой! Клиент оценит."
        5    -> "Огонь! На таком заряде всё получится идеально."
        else -> null
    }

    private fun checkChemical(text: String): ChemicalWarning? {
        return when {
            text.contains("белизна") && text.contains("уксус")   ->
                ChemicalWarning("Белизна + уксус = хлор", "Срочно проветрить, выйди!", true)
            text.contains("белизна") && text.contains("аммиак")  ->
                ChemicalWarning("Белизна + аммиак = хлорамин", "Немедленно выйди из помещения!", true)
            text.contains("пермол") && (text.contains("кислот") || text.contains("domestos")) ->
                ChemicalWarning("Пермоль + кислота — реакция", "Не смешивай, смой по очереди", false)
            else -> null
        }
    }

    private fun isSafetyCritical(text: String): Boolean =
        listOf("голова кружится", "тошнит", "задыхаюсь", "потеряла сознание",
            "резкий запах", "слезятся глаза").any { text.contains(it) }
}

/** Loads bundled knowledge base files from assets / resources */
interface ModFileLoader {
    fun loadAll(): List<KBResult>
}
