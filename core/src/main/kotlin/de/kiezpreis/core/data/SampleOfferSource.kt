package de.kiezpreis.core.data

import de.kiezpreis.core.logic.PackSizeParser
import de.kiezpreis.core.model.BerlinAvailability
import de.kiezpreis.core.model.BerlinAvailability.BERLIN_WIDE
import de.kiezpreis.core.model.BerlinAvailability.NATIONAL_CONFIRMED
import de.kiezpreis.core.model.BerlinAvailability.OUTSIDE_BERLIN
import de.kiezpreis.core.model.BerlinAvailability.STORE_SPECIFIC
import de.kiezpreis.core.model.BerlinAvailability.UNCONFIRMED
import de.kiezpreis.core.model.Category
import de.kiezpreis.core.model.Cents
import de.kiezpreis.core.model.Offer
import de.kiezpreis.core.model.OfferCondition
import de.kiezpreis.core.model.ReferencePrice
import de.kiezpreis.core.model.ReferencePriceKind
import de.kiezpreis.core.model.Retailer
import de.kiezpreis.core.model.Retailer.ALDI_NORD
import de.kiezpreis.core.model.Retailer.EDEKA
import de.kiezpreis.core.model.Retailer.KAUFLAND
import de.kiezpreis.core.model.Retailer.LIDL
import de.kiezpreis.core.model.Retailer.NETTO_MD
import de.kiezpreis.core.model.Retailer.PENNY
import de.kiezpreis.core.model.Retailer.REWE
import de.kiezpreis.core.model.Store
import de.kiezpreis.core.model.Validity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Built-in demonstration data so the interface can be explored before real sources deliver.
 *
 * All prices are invented. Every offer carries [Offer.isSample] = true and the UI labels it as
 * "Beispiel"; no sample offer links to a retailer page, and the store names are fictitious.
 * Dates are placed in the current week so that validity labels can be demonstrated.
 */
class SampleOfferSource(private val zone: ZoneId = ZoneId.of("Europe/Berlin")) : OfferSource {

    override val descriptor = SourceCatalog.sample

    override suspend fun fetch(context: FetchContext): FetchResult {
        val today = Instant.ofEpochMilli(context.nowMillis).atZone(zone).toLocalDate()
        return FetchResult.Success(build(today, context.nowMillis), context.nowMillis)
    }

    private class Spec(
        val retailer: Retailer,
        val title: String,
        val brand: String?,
        val category: Category,
        val size: String?,
        val price: String?,
        val ref: Pair<String, ReferencePriceKind>? = null,
        val availability: BerlinAvailability = BERLIN_WIDE,
        val stores: List<Store> = emptyList(),
        val conditions: List<OfferCondition> = emptyList(),
        val week: Int = 0,
    )

    fun build(today: LocalDate, now: Long): List<Offer> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        fun period(week: Int): Validity {
            val start = monday.plusWeeks(week.toLong())
            return Validity(start.toString(), start.plusDays(5).toString())
        }
        return specs().mapIndexed { i, s ->
            Offer(
                id = "sample-$i",
                sourceId = descriptor.id,
                retailer = s.retailer,
                title = s.title,
                brand = s.brand,
                category = s.category,
                sizeText = s.size,
                packSize = PackSizeParser.parse(s.size),
                price = s.price?.let { Cents.ofEuro(it.toDouble()) },
                referencePrice = s.ref?.let { ReferencePrice(Cents.ofEuro(it.first.toDouble()), it.second) },
                validity = period(s.week),
                availability = s.availability,
                stores = s.stores,
                conditions = s.conditions,
                sourceUrl = null,
                fetchedAt = now,
                isSample = true,
            )
        }
    }

    private fun store(r: Retailer, district: String, pc: String) =
        Store(id = "sample-${r.name}-$pc", retailer = r, name = "${r.shortName} Beispielfiliale $district", postalCode = pc, city = "Berlin")

    private fun specs(): List<Spec> {
        val strike = ReferencePriceKind.STRIKE_THROUGH
        val low30 = ReferencePriceKind.LOWEST_30_DAYS
        val deposit = listOf(OfferCondition.PLUS_DEPOSIT)
        return listOf(
            // Monster Energy Ultra White: the brief's main scenario, incl. a different variant and pack.
            Spec(LIDL, "Monster Energy Ultra White", "Monster", Category.DRINKS, "je 0,5-l-Dose", "0.99", "1.39" to strike, NATIONAL_CONFIRMED, conditions = deposit),
            Spec(KAUFLAND, "Monster Energy Ultra White", "Monster", Category.DRINKS, "0,5 l", "1.11", "1.49" to low30, STORE_SPECIFIC,
                listOf(store(KAUFLAND, "Friedrichshain", "10243"), store(KAUFLAND, "Neukölln", "12051")), deposit),
            Spec(REWE, "Monster Energy Drink Ultra White", "Monster", Category.DRINKS, "je 0,5-l-Dose (1 l = 2,38 €)", "1.19", null, BERLIN_WIDE, conditions = deposit),
            Spec(NETTO_MD, "Monster Energy Ultra White", "Monster", Category.DRINKS, "4 x 0,5 l", "4.44", "5.56" to strike, UNCONFIRMED, conditions = deposit),
            Spec(PENNY, "Monster Energy Ultra Paradise", "Monster", Category.DRINKS, "0,5 l", "1.09", "1.45" to strike, BERLIN_WIDE, conditions = deposit),
            Spec(EDEKA, "Monster Energy Ultra White", "Monster", Category.DRINKS, "0,5 l", "0.95", null, OUTSIDE_BERLIN,
                listOf(Store("sample-edeka-potsdam", EDEKA, "EDEKA Beispielmarkt Potsdam", postalCode = "14467", city = "Potsdam")), deposit),
            // Coca-Cola Zero in several pack sizes: comparable only via base price.
            Spec(ALDI_NORD, "Coca-Cola Zero Sugar", "Coca-Cola", Category.DRINKS, "1,25 l", "1.29", "1.69" to low30, NATIONAL_CONFIRMED, conditions = deposit),
            Spec(KAUFLAND, "Coca-Cola Zero Sugar", "Coca-Cola", Category.DRINKS, "6 x 1,5 l", "7.49", "10.14" to strike, STORE_SPECIFIC,
                listOf(store(KAUFLAND, "Lichtenberg", "10365")), deposit),
            Spec(EDEKA, "Coca-Cola Zero Sugar", "Coca-Cola", Category.DRINKS, "1,25 l", "1.11", null, BERLIN_WIDE,
                conditions = deposit + OfferCondition.APP_OR_CARD_ONLY),
            Spec(REWE, "Coca-Cola Zero Sugar", "Coca-Cola", Category.DRINKS, "0,33 l", "0.69", null, UNCONFIRMED, conditions = deposit),
            // Fresh produce and meat
            Spec(PENNY, "Hähnchenbrustfilet, frisch", "Gut Ponholz", Category.MEAT, "600 g", "5.49", "6.99" to strike, BERLIN_WIDE),
            Spec(NETTO_MD, "Hähnchen-Brustfilet", "Mühlenhof", Category.MEAT, "1 kg", "8.88", null, BERLIN_WIDE),
            Spec(LIDL, "Hähnchenbrustfilet XXL", "Metzgerfrisch", Category.MEAT, "1,2 kg", "9.99", "12.49" to low30, NATIONAL_CONFIRMED),
            Spec(KAUFLAND, "Hähnchenbrustfilet Teilstücke", "K-Purland", Category.MEAT, null, "6.99", null, STORE_SPECIFIC,
                listOf(store(KAUFLAND, "Spandau", "13597"))),
            Spec(REWE, "Rinderhackfleisch", "REWE Beste Wahl", Category.MEAT, "500 g", "4.99", "5.99" to strike, BERLIN_WIDE),
            Spec(ALDI_NORD, "Bananen", null, Category.FRUIT_VEG, "1 kg", "1.19", null, NATIONAL_CONFIRMED),
            Spec(EDEKA, "Avocado Hass, essreif", null, Category.FRUIT_VEG, "Stück", "0.79", "1.29" to strike, BERLIN_WIDE),
            // Sports nutrition
            Spec(LIDL, "Whey Proteinpulver Schoko", "Crivit", Category.SPORTS_NUTRITION, "500 g", "9.99", "12.99" to strike, NATIONAL_CONFIRMED),
            Spec(KAUFLAND, "Whey Protein Vanille", "ESN", Category.SPORTS_NUTRITION, "1000 g", "24.99", "29.99" to low30, STORE_SPECIFIC,
                listOf(store(KAUFLAND, "Friedrichshain", "10243"))),
            Spec(ALDI_NORD, "Proteinpulver Neutral", "Sports", Category.SPORTS_NUTRITION, "750 g", "13.99", null, UNCONFIRMED),
            // Dairy
            Spec(ALDI_NORD, "Deutsche Markenbutter", "Milsani", Category.DAIRY, "250 g", "1.79", "2.19" to low30, NATIONAL_CONFIRMED),
            Spec(REWE, "Kerrygold Original Irische Butter", "Kerrygold", Category.DAIRY, "250 g", "2.49", "3.29" to strike, BERLIN_WIDE),
            Spec(EDEKA, "Kerrygold Original Irische Butter", "Kerrygold", Category.DAIRY, "250 g", "2.22", null, BERLIN_WIDE),
            Spec(NETTO_MD, "Kerrygold Original Irische Butter", "Kerrygold", Category.DAIRY, "250 g", "2.39", null, UNCONFIRMED),
            Spec(PENNY, "Leerdammer Original Scheiben", "Leerdammer", Category.DAIRY, "140 g", "1.69", "2.49" to strike, BERLIN_WIDE),
            Spec(LIDL, "Frische Vollmilch 3,5 %", "Milbona", Category.DAIRY, "1 l", "0.99", null, NATIONAL_CONFIRMED),
            Spec(REWE, "Haferdrink Barista", "Oatly", Category.DRINKS, "1 l", "1.99", "2.79" to strike, BERLIN_WIDE),
            // Household
            Spec(KAUFLAND, "Persil Universal Gel", "Persil", Category.HOUSEHOLD, "20 WL, 1 l", "4.99", "8.49" to strike, STORE_SPECIFIC,
                listOf(store(KAUFLAND, "Lichtenberg", "10365"), store(KAUFLAND, "Spandau", "13597"))),
            Spec(REWE, "Ariel Colorwaschmittel Pulver", "Ariel", Category.HOUSEHOLD, "1,3 kg", "5.99", "7.99" to strike, BERLIN_WIDE),
            Spec(ALDI_NORD, "Vollwaschmittel Pulver", "Tandil", Category.HOUSEHOLD, "1,35 kg", "2.99", null, NATIONAL_CONFIRMED),
            Spec(PENNY, "Toilettenpapier 4-lagig", "Happy End", Category.HOUSEHOLD, "10 Stück", "4.49", null, BERLIN_WIDE),
            // Pantry and sweets
            Spec(EDEKA, "Lavazza Caffè Crema e Gusto", "Lavazza", Category.PANTRY, "1000 g", "11.99", "16.99" to strike, BERLIN_WIDE),
            Spec(REWE, "Barilla Spaghetti n.5", "Barilla", Category.PANTRY, "500 g", "1.29", "1.99" to strike, BERLIN_WIDE),
            Spec(NETTO_MD, "Nutella Nuss-Nougat-Creme", "Nutella", Category.SWEETS, "450 g", "2.99", "3.79" to low30, BERLIN_WIDE),
            Spec(LIDL, "Pringles Original", "Pringles", Category.SWEETS, "185 g", "1.99", null, NATIONAL_CONFIRMED,
                conditions = listOf(OfferCondition.APP_OR_CARD_ONLY)),
            Spec(PENNY, "Ristorante Pizza Mozzarella", "Dr. Oetker", Category.FROZEN, "335 g", "1.99", "2.79" to strike, BERLIN_WIDE),
            Spec(KAUFLAND, "Red Bull Energy Drink", "Red Bull", Category.DRINKS, "0,25 l", "0.95", "1.45" to strike, STORE_SPECIFIC,
                listOf(store(KAUFLAND, "Neukölln", "12051")), deposit),
            // Without price: listed by the retailer but price unknown. Must not be shown as 0 €.
            Spec(EDEKA, "Bergader Bavaria Blu", "Bergader", Category.DAIRY, "150 g", null, null, BERLIN_WIDE),
            // Next week and already expired: exercise validity handling.
            Spec(ALDI_NORD, "Monster Energy Ultra White", "Monster", Category.DRINKS, "0,5 l", "0.89", null, NATIONAL_CONFIRMED, week = 1, conditions = deposit),
            Spec(REWE, "Kerrygold Original Irische Butter", "Kerrygold", Category.DAIRY, "250 g", "1.99", null, BERLIN_WIDE, week = -1),
        )
    }
}
