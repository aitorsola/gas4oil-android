package com.aitorsola.gas4oil

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

private fun formatPrice(value: Double): String =
    String.format(Locale.ROOT, "%.3f", value).replace('.', ',')

private val frenchShortDays = listOf(
    "Lundi" to "L", "Mardi" to "M", "Mercredi" to "X", "Jeudi" to "J",
    "Vendredi" to "V", "Samedi" to "S", "Dimanche" to "D"
)

@Serializable
data class FranceRow(
    val id: Int,
    @SerialName("adresse") val address: String? = null,
    @SerialName("cp") val postalCode: String? = null,
    @SerialName("ville") val city: String? = null,
    @SerialName("departement") val department: String? = null,
    @SerialName("geom") val coordinates: Coordinates? = null,
    @SerialName("gazole_prix") val dieselPrice: Double? = null,
    @SerialName("sp95_prix") val sp95Price: Double? = null,
    @SerialName("e10_prix") val e10Price: Double? = null,
    @SerialName("sp98_prix") val sp98Price: Double? = null,
    @SerialName("e85_prix") val e85Price: Double? = null,
    @SerialName("gplc_prix") val lpgPrice: Double? = null,
    @SerialName("horaires_automate_24_24") val open24h: String? = null,
    @SerialName("horaires_jour") val dailyHours: String? = null,
    @SerialName("services_service") val services: List<String>? = null
) {
    @Serializable
    data class Coordinates(val lon: Double, val lat: Double)

    private fun price(value: Double?): String = value?.let { formatPrice(it) } ?: ""

    private val schedule: String
        get() {
            if (open24h == "Oui") return "L-D: 24H"
            val raw = dailyHours ?: return ""
            val entries = raw.split(", ").mapNotNull { part ->
                val day = frenchShortDays.firstOrNull { part.startsWith(it.first) } ?: return@mapNotNull null
                day.second to part.removePrefix(day.first).replace('.', ':')
            }
            val ranges = entries.map { it.second }.toSet()
            if (ranges.size == 1) {
                val range = ranges.first()
                return if (range == "00:00-00:00") "L-D: 24H" else "L-D: $range"
            }
            return entries.joinToString("; ") { "${it.first}: ${it.second}" }
        }

    fun toStation(): Station? {
        val point = coordinates ?: return null
        return Station(
            id = id,
            cp = postalCode ?: "",
            provincia = (department ?: "").lowercase(),
            municipio = (city ?: "").lowercase(),
            direccion = address ?: "",
            horario = schedule,
            latitude = point.lat,
            longitude = point.lon,
            gasoleoA = price(dieselPrice),
            gasolina95E5 = price(sp95Price),
            gasolina98E5 = price(sp98Price),
            glp = price(lpgPrice),
            gasolina95E10 = price(e10Price),
            e85 = price(e85Price),
            services = services ?: emptyList(),
            rotulo = "",
            country = Country.FRANCE
        )
    }
}

@Serializable
data class PortugalResponse(@SerialName("resultado") val results: List<Row> = emptyList()) {

    @Serializable
    data class Row(
        @SerialName("Id") val id: Int,
        @SerialName("Marca") val brand: String? = null,
        @SerialName("Morada") val address: String? = null,
        @SerialName("CodPostal") val postalCode: String? = null,
        @SerialName("Municipio") val municipality: String? = null,
        @SerialName("Distrito") val district: String? = null,
        @SerialName("Latitude") val latitude: Double = 0.0,
        @SerialName("Longitude") val longitude: Double = 0.0,
        @SerialName("Preco") val priceText: String? = null,
        @SerialName("Combustivel") val fuelName: String? = null
    ) {
        val fuel: FuelType?
            get() = when (fuelName) {
                "Gasolina simples 95" -> FuelType.GAS95
                "Gasolina especial 95" -> FuelType.GAS95_PREMIUM
                "Gasolina 98" -> FuelType.GAS98
                "Gasóleo simples" -> FuelType.DIESEL
                "Gasóleo especial" -> FuelType.DIESEL_PREMIUM
                "GPL Auto" -> FuelType.GLP
                else -> null
            }

        val price: String get() = (priceText ?: "").replace("€", "").trim()

        val brandName: String
            get() = (brand ?: "").trim().let { if (it.equals("genérico", true)) "" else it }
    }

    fun stations(): List<Station> {
        val byId = LinkedHashMap<Int, Station>()
        for (row in results) {
            val fuel = row.fuel ?: continue
            val station = byId.getOrPut(row.id) {
                Station(
                    id = Country.PORTUGAL.idOffset + row.id,
                    cp = row.postalCode ?: "",
                    provincia = (row.district ?: "").lowercase(),
                    municipio = (row.municipality ?: "").lowercase(),
                    direccion = row.address ?: "",
                    horario = "",
                    latitude = row.latitude,
                    longitude = row.longitude,
                    gasoleoA = "",
                    gasolina95E5 = "",
                    gasolina98E5 = "",
                    rotulo = row.brandName,
                    country = Country.PORTUGAL
                )
            }
            when (fuel) {
                FuelType.GAS95 -> station.gasolina95E5 = row.price
                FuelType.GAS95_PREMIUM -> station.gasolina95E5Premium = row.price
                FuelType.GAS98 -> station.gasolina98E5 = row.price
                FuelType.DIESEL -> station.gasoleoA = row.price
                FuelType.DIESEL_PREMIUM -> station.gasoleoPremium = row.price
                FuelType.GLP -> station.glp = row.price
                FuelType.E10, FuelType.E85 -> Unit
            }
        }
        return byId.values.toList()
    }
}

object ItalyFeed {

    private object RegistryColumn {
        const val ID = 0
        const val BRAND = 2
        const val ADDRESS = 5
        const val MUNICIPALITY = 6
        const val PROVINCE = 7
    }

    private object PriceColumn {
        const val ID = 0
        const val FUEL_NAME = 1
        const val PRICE = 2
        const val IS_SELF_SERVICE = 3
    }

    private val premiumPetrolNames = setOf(
        "Benzina speciale", "Blue Super", "HiQ Perform+", "Benzina WR 100", "Benzina Plus 98", "V-Power"
    )
    private val premiumDieselNames = setOf(
        "Blue Diesel", "Supreme Diesel", "Hi-Q Diesel", "Gasolio speciale", "Gasolio Premium",
        "Diesel Shell V Power", "DieselMax", "Excellium Diesel"
    )

    fun rows(text: String): List<List<String>> =
        text.split('\n').drop(2).map { it.trimEnd('\r').split('|') }

    private fun fuel(name: String): FuelType? = when {
        name == "Benzina" -> FuelType.GAS95
        name == "Gasolio" -> FuelType.DIESEL
        name == "GPL" -> FuelType.GLP
        name in premiumPetrolNames -> FuelType.GAS95_PREMIUM
        name in premiumDieselNames -> FuelType.DIESEL_PREMIUM
        else -> null
    }

    private fun brand(name: String): String = when (name.lowercase()) {
        "pompe bianche" -> ""
        "agip eni" -> "Eni"
        "api-ip" -> "IP"
        else -> name
    }

    private fun splitAddress(raw: String): Pair<String, String> {
        val parts = raw.trim().split(' ').filter { it.isNotEmpty() }
        val last = parts.lastOrNull()
        return if (last != null && last.length == 5 && last.toIntOrNull() != null) {
            parts.dropLast(1).joinToString(" ") to last
        } else parts.joinToString(" ") to ""
    }

    fun stations(registry: List<List<String>>, prices: List<List<String>>): List<Station> {
        val selfServicePrices = HashMap<Int, MutableMap<FuelType, String>>()
        val attendedPrices = HashMap<Int, MutableMap<FuelType, String>>()
        for (row in prices) {
            if (row.size < 4) continue
            val id = row[PriceColumn.ID].toIntOrNull() ?: continue
            val fuel = fuel(row[PriceColumn.FUEL_NAME]) ?: continue
            val value = row[PriceColumn.PRICE].toDoubleOrNull() ?: continue
            val target = if (row[PriceColumn.IS_SELF_SERVICE] == "1") selfServicePrices else attendedPrices
            target.getOrPut(id) { mutableMapOf() }.putIfAbsent(fuel, formatPrice(value))
        }
        return registry.mapNotNull { row ->
            if (row.size < 10) return@mapNotNull null
            val id = row[RegistryColumn.ID].toIntOrNull() ?: return@mapNotNull null
            val latitude = row[row.size - 2].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = row[row.size - 1].toDoubleOrNull() ?: return@mapNotNull null
            if (latitude == 0.0 || longitude == 0.0) return@mapNotNull null
            val merged = HashMap<FuelType, String>()
            attendedPrices[id]?.let { merged.putAll(it) }
            selfServicePrices[id]?.let { merged.putAll(it) }
            if (merged.isEmpty()) return@mapNotNull null
            val (street, postalCode) = splitAddress(row[RegistryColumn.ADDRESS])
            Station(
                id = Country.ITALY.idOffset + id,
                cp = postalCode,
                provincia = row[RegistryColumn.PROVINCE].lowercase(),
                municipio = row[RegistryColumn.MUNICIPALITY].lowercase(),
                direccion = street,
                horario = "",
                latitude = latitude,
                longitude = longitude,
                gasoleoA = merged[FuelType.DIESEL] ?: "",
                gasolina95E5 = merged[FuelType.GAS95] ?: "",
                gasolina98E5 = "",
                glp = merged[FuelType.GLP] ?: "",
                gasoleoPremium = merged[FuelType.DIESEL_PREMIUM] ?: "",
                gasolina95E5Premium = merged[FuelType.GAS95_PREMIUM] ?: "",
                rotulo = brand(row[RegistryColumn.BRAND]),
                country = Country.ITALY
            )
        }
    }
}

@Serializable
data class CroatiaFeed(
    @SerialName("postajas") val stationRows: List<StationRow> = emptyList(),
    @SerialName("gorivos") val fuels: List<Fuel> = emptyList(),
    @SerialName("obvezniks") val operators: List<Operator> = emptyList()
) {
    @Serializable
    data class StationRow(
        val id: Int,
        @SerialName("adresa") val address: String? = null,
        @SerialName("mjesto") val town: String? = null,
        @SerialName("obveznik_id") val operatorId: Int? = null,
        @SerialName("long") val latitudeText: String? = null,
        @SerialName("lat") val longitudeText: String? = null,
        @SerialName("radnaVremena") val openingHours: List<OpeningHours>? = null,
        @SerialName("cjenici") val prices: List<Price>? = null
    )

    @Serializable
    data class OpeningHours(
        @SerialName("vrsta_dana_id") val dayTypeId: Int = 0,
        @SerialName("pocetak") val opens: String? = null,
        @SerialName("kraj") val closes: String? = null
    )

    @Serializable
    data class Price(@SerialName("gorivo_id") val fuelId: Int = 0, @SerialName("cijena") val value: Double? = null)

    @Serializable
    data class Fuel(val id: Int, @SerialName("vrsta_goriva_id") val kindId: Int? = null)

    @Serializable
    data class Operator(val id: Int, @SerialName("naziv") val name: String? = null)

    private object FuelKind {
        const val PETROL_95_WITH_ADDITIVES = 1
        const val PETROL_95 = 2
        const val PETROL_100_WITH_ADDITIVES = 5
        const val PETROL_100 = 6
        const val DIESEL_WITH_ADDITIVES = 7
        const val DIESEL = 8
        const val LPG = 9
    }

    private object DayType {
        const val WEEKDAY = 1
        const val SATURDAY = 2
        const val SUNDAY = 3
    }

    private fun brand(name: String?): String {
        var value = name ?: ""
        for (separator in listOf(" – ", " - ")) {
            val index = value.indexOf(separator)
            if (index >= 0) value = value.substring(0, index)
        }
        for (suffix in listOf(" j.d.o.o.", " d.o.o.", " d.d.", " d.o.o", " d.d")) {
            if (value.lowercase().endsWith(suffix)) value = value.dropLast(suffix.length)
        }
        value = value.trim()
        return if (value == "-") "" else value
    }

    private fun schedule(hours: List<OpeningHours>?): String {
        if (hours.isNullOrEmpty()) return ""
        val labels = mapOf(DayType.WEEKDAY to "L-V", DayType.SATURDAY to "S", DayType.SUNDAY to "D")
        val parts = listOf(DayType.WEEKDAY, DayType.SATURDAY, DayType.SUNDAY).mapNotNull { day ->
            val entry = hours.firstOrNull { it.dayTypeId == day } ?: return@mapNotNull null
            val opens = entry.opens?.take(5) ?: return@mapNotNull null
            val closes = entry.closes?.take(5) ?: return@mapNotNull null
            val allDay = opens == closes || (opens == "00:00" && (closes == "23:59" || closes == "24:00"))
            labels.getValue(day) to if (allDay) "24H" else "$opens-$closes"
        }
        val ranges = parts.map { it.second }.toSet()
        if (parts.size == 3 && ranges.size == 1) return "L-D: ${ranges.first()}"
        return parts.joinToString("; ") { "${it.first}: ${it.second}" }
    }

    fun stations(): List<Station> {
        val kindByFuelId = fuels.associate { it.id to (it.kindId ?: 0) }
        val brandByOperatorId = operators.associate { it.id to brand(it.name) }
        return stationRows.mapNotNull { row ->
            val latitude = row.latitudeText?.toDoubleOrNull() ?: return@mapNotNull null
            val longitude = row.longitudeText?.toDoubleOrNull() ?: return@mapNotNull null
            val cheapestByKind = HashMap<Int, Double>()
            for (entry in row.prices.orEmpty()) {
                val value = entry.value ?: continue
                if (value <= 0) continue
                val kind = kindByFuelId[entry.fuelId] ?: continue
                cheapestByKind[kind] = minOf(cheapestByKind[kind] ?: value, value)
            }
            fun basePlusPremium(kinds: List<Int>): Pair<String, String> {
                val values = kinds.mapNotNull { cheapestByKind[it] }.sorted()
                val base = values.firstOrNull() ?: return "" to ""
                val premium = if (values.size > 1) formatPrice(values.last()) else ""
                return formatPrice(base) to premium
            }
            val petrol = basePlusPremium(listOf(FuelKind.PETROL_95_WITH_ADDITIVES, FuelKind.PETROL_95))
            val diesel = basePlusPremium(listOf(FuelKind.DIESEL_WITH_ADDITIVES, FuelKind.DIESEL))
            val petrol100 = basePlusPremium(listOf(FuelKind.PETROL_100_WITH_ADDITIVES, FuelKind.PETROL_100))
            val lpg = cheapestByKind[FuelKind.LPG]
            if (petrol.first.isEmpty() && diesel.first.isEmpty() && lpg == null) return@mapNotNull null
            Station(
                id = Country.CROATIA.idOffset + row.id,
                provincia = "",
                municipio = (row.town ?: "").lowercase(),
                direccion = (row.address ?: "").lowercase().replaceFirstChar { it.uppercase() },
                horario = schedule(row.openingHours),
                latitude = latitude,
                longitude = longitude,
                gasoleoA = diesel.first,
                gasolina95E5 = petrol.first,
                gasolina98E5 = petrol100.first,
                glp = lpg?.let { formatPrice(it) } ?: "",
                gasoleoPremium = diesel.second,
                gasolina95E5Premium = petrol.second,
                rotulo = brandByOperatorId[row.operatorId] ?: "",
                country = Country.CROATIA
            )
        }
    }
}
