package com.aitorsola.gas4oil

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.Normalizer

@Serializable
data class StationsResponse(
    @SerialName("Fecha") val date: String = "",
    @SerialName("ListaEESSPrecio") val prices: List<Row> = emptyList()
) {
    @Serializable
    data class Row(
        @SerialName("IDEESS") val id: String = "",
        @SerialName("C.P.") val cp: String = "",
        @SerialName("Provincia") val provincia: String = "",
        @SerialName("Municipio") val municipio: String = "",
        @SerialName("Dirección") val direccion: String = "",
        @SerialName("Horario") val horario: String = "",
        @SerialName("Longitud (WGS84)") val longitud: String = "",
        @SerialName("Latitud") val latitud: String = "",
        @SerialName("Precio Gasoleo A") val gasoleoA: String = "",
        @SerialName("Precio Gasolina 95 E5") val gasolina95E5: String = "",
        @SerialName("Precio Gasolina 98 E5") val gasolina98E5: String = "",
        @SerialName("Precio Gases licuados del petróleo") val glp: String = "",
        @SerialName("Precio Gasoleo Premium") val gasoleoPremium: String = "",
        @SerialName("Precio Gasolina 95 E5 Premium") val gasolina95E5Premium: String = "",
        @SerialName("Precio Gasolina 95 E10") val gasolina95E10: String = "",
        @SerialName("Rótulo") val rotulo: String = ""
    )
}

@Serializable
data class Station(
    val id: Int,
    val cp: String = "",
    val provincia: String,
    val municipio: String,
    val direccion: String,
    val horario: String,
    val latitude: Double,
    val longitude: Double,
    var gasoleoA: String,
    var gasolina95E5: String,
    var gasolina98E5: String,
    var glp: String = "",
    var gasoleoPremium: String = "",
    var gasolina95E5Premium: String = "",
    var gasolina95E10: String = "",
    var e85: String = "",
    val services: List<String> = emptyList(),
    val rotulo: String,
    val country: Country = Country.SPAIN
) {
    fun rawPrice(fuel: FuelType): String = when (fuel) {
        FuelType.GAS95 -> gasolina95E5
        FuelType.GAS98 -> gasolina98E5
        FuelType.DIESEL -> gasoleoA
        FuelType.GLP -> glp
        FuelType.DIESEL_PREMIUM -> gasoleoPremium
        FuelType.GAS95_PREMIUM -> gasolina95E5Premium
        FuelType.E10 -> gasolina95E10
        FuelType.E85 -> e85
    }

    fun price(fuel: FuelType): Double? = rawPrice(fuel).replace(',', '.').toDoubleOrNull()

    val brandName: String get() = rotulo.trim(' ', '-', '.', ',')

    val displayTitle: String
        get() = brandName.ifEmpty { direccion.lowercase().replaceFirstChar { it.uppercase() } }

    val displayAddress: String
        get() = if (brandName.isEmpty()) "$cp ${municipio.replaceFirstChar { it.uppercase() }}"
        else direccion.lowercase().replaceFirstChar { it.uppercase() }
}

fun StationsResponse.Row.toStation(): Station? {
    val numericId = id.toIntOrNull() ?: return null
    return Station(
        id = numericId,
        cp = cp,
        provincia = provincia.lowercase(),
        municipio = municipio.lowercase(),
        direccion = direccion,
        horario = horario,
        latitude = latitud.replace(',', '.').toDoubleOrNull() ?: 0.0,
        longitude = longitud.replace(',', '.').toDoubleOrNull() ?: 0.0,
        gasoleoA = gasoleoA,
        gasolina95E5 = gasolina95E5,
        gasolina98E5 = gasolina98E5,
        glp = glp,
        gasoleoPremium = gasoleoPremium,
        gasolina95E5Premium = gasolina95E5Premium,
        gasolina95E10 = gasolina95E10,
        rotulo = rotulo
    )
}

enum class FuelType(val storageKey: String, val labelRes: Int) {
    GAS95("gas95", R.string.fuel_95),
    GAS95_PREMIUM("gas95Premium", R.string.fuel_gas95premium),
    GAS98("gas98", R.string.fuel_98),
    DIESEL("diesel", R.string.fuel_diesel),
    DIESEL_PREMIUM("dieselPremium", R.string.fuel_dieselpremium),
    GLP("glp", R.string.fuel_glp),
    E10("e10", R.string.fuel_e10),
    E85("e85", R.string.fuel_e85);

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.storageKey == key }
    }
}

enum class Country(
    val storageKey: String,
    val flag: String,
    val code: String,
    val nameRes: Int,
    val fuels: List<FuelType>,
    val defaultFuel: FuelType,
    val suggestedCities: List<String>,
    val idOffset: Int = 0
) {
    SPAIN(
        "spain", "\uD83C\uDDEA\uD83C\uDDF8", "ES", R.string.country_spain,
        listOf(
            FuelType.GAS95, FuelType.GAS95_PREMIUM, FuelType.GAS98,
            FuelType.DIESEL, FuelType.DIESEL_PREMIUM, FuelType.GLP
        ),
        FuelType.GAS95,
        listOf(
            "madrid", "barcelona", "valencia", "sevilla", "zaragoza", "málaga",
            "murcia", "palma de mallorca", "bilbao", "alicante/alacant",
            "valladolid", "vigo", "gijón", "córdoba"
        )
    ),
    FRANCE(
        "france", "\uD83C\uDDEB\uD83C\uDDF7", "FR", R.string.country_france,
        listOf(
            FuelType.E10, FuelType.GAS95, FuelType.GAS98,
            FuelType.E85, FuelType.DIESEL, FuelType.GLP
        ),
        FuelType.E10,
        listOf(
            "paris", "marseille", "lyon", "toulouse", "nice", "nantes",
            "montpellier", "strasbourg", "bordeaux", "lille", "rennes",
            "reims", "toulon", "grenoble"
        )
    ),
    PORTUGAL(
        "portugal", "\uD83C\uDDF5\uD83C\uDDF9", "PT", R.string.country_portugal,
        listOf(
            FuelType.GAS95, FuelType.GAS95_PREMIUM, FuelType.GAS98,
            FuelType.DIESEL, FuelType.DIESEL_PREMIUM, FuelType.GLP
        ),
        FuelType.GAS95,
        listOf(
            "lisboa", "porto", "vila nova de gaia", "braga", "coimbra", "funchal",
            "setúbal", "aveiro", "faro", "leiria", "sintra", "cascais",
            "évora", "guimarães"
        ),
        100_000_000
    ),
    ITALY(
        "italy", "\uD83C\uDDEE\uD83C\uDDF9", "IT", R.string.country_italy,
        listOf(
            FuelType.GAS95, FuelType.GAS95_PREMIUM,
            FuelType.DIESEL, FuelType.DIESEL_PREMIUM, FuelType.GLP
        ),
        FuelType.GAS95,
        listOf(
            "roma", "milano", "napoli", "torino", "palermo", "genova", "bologna",
            "firenze", "bari", "catania", "venezia", "verona", "messina", "padova"
        ),
        200_000_000
    ),
    CROATIA(
        "croatia", "\uD83C\uDDED\uD83C\uDDF7", "HR", R.string.country_croatia,
        listOf(
            FuelType.GAS95, FuelType.GAS95_PREMIUM, FuelType.GAS98,
            FuelType.DIESEL, FuelType.DIESEL_PREMIUM, FuelType.GLP
        ),
        FuelType.GAS95,
        listOf(
            "zagreb", "split", "rijeka", "osijek", "zadar", "pula", "slavonski brod",
            "karlovac", "varaždin", "šibenik", "dubrovnik", "sisak", "koprivnica", "bjelovar"
        ),
        300_000_000
    );

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.storageKey == key } ?: SPAIN
        fun fromIso(code: String?) = when (code?.uppercase()) {
            "ES" -> SPAIN
            "FR" -> FRANCE
            "PT" -> PORTUGAL
            "IT" -> ITALY
            "HR" -> CROATIA
            else -> null
        }
    }
}

enum class StationSort(val storageKey: String) {
    NEAREST("nearest"), CHEAPEST("cheapest");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.storageKey == key }
    }
}

enum class ThemePreference(val storageKey: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

enum class StationBrandLogo(val key: String, val drawable: Int) {
    ALCAMPO("alcampo", R.drawable.logo_alcampo),
    AVIA("avia", R.drawable.logo_avia),
    BALLENOIL("ballenoil", R.drawable.logo_ballenoil),
    BONAREA("bonarea", R.drawable.logo_bonarea),
    BP("bp", R.drawable.logo_bp),
    CAMPSA("campsa", R.drawable.logo_campsa),
    CARREFOUR("carrefour", R.drawable.logo_carrefour),
    CEPSA("cepsa", R.drawable.logo_cepsa),
    ENI("eni", R.drawable.logo_eni),
    EROSKI("eroski", R.drawable.logo_eroski),
    GALP("galp", R.drawable.logo_galp),
    MEROIL("meroil", R.drawable.logo_meroil),
    MOEVE("moeve", R.drawable.logo_moeve),
    NATURGY("naturgy", R.drawable.logo_naturgy),
    PETRONOR("petronor", R.drawable.logo_petronor),
    PETROPRIX("petroprix", R.drawable.logo_petroprix),
    PLENERGY("plenergy", R.drawable.logo_plenergy),
    Q8("q8", R.drawable.logo_q8),
    REPSOL("repsol", R.drawable.logo_repsol),
    SHELL("shell", R.drawable.logo_shell),
    TAMOIL("tamoil", R.drawable.logo_tamoil);

    val displayName: String
        get() = when (this) {
            BP -> "BP"
            Q8 -> "Q8"
            BONAREA -> "bonÀrea"
            else -> key.replaceFirstChar { it.uppercase() }
        }

    companion object {
        fun from(key: String) = entries.firstOrNull { it.key == key }
    }
}

data class BrandOption(
    val key: String,
    val title: String,
    val logo: StationBrandLogo?,
    val stationCount: Int
)

object Text {

    fun searchNormalized(value: String): String =
        Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")

    fun townSearchKey(name: String): String {
        if (!name.endsWith(")")) return name
        val open = name.lastIndexOf('(')
        if (open < 0) return name
        val article = name.substring(open + 1, name.length - 1).trim()
        val base = name.substring(0, open).trim()
        if (article.isEmpty() || base.isEmpty()) return name
        val separator = if (article.endsWith("'")) "" else " "
        return "$name $article$separator$base"
    }

    fun brandTokens(rotulo: String): List<String> =
        rotulo.lowercase().split(Regex("[^0-9a-záéíóúñüç]+")).filter { it.isNotEmpty() }

    fun brandMatches(rotulo: String, key: String) = brandTokens(rotulo).contains(key)

    private val genericBrandTokens = setOf(
        "estacion", "estación", "servicio", "gasolinera", "area", "área", "oil", "energy",
        "energia", "energía", "carburantes", "combustibles", "petrol", "gasoleos", "gasóleos",
        "auto", "gas", "genérico", "generico"
    )

    fun brandOptions(stations: List<Station>): List<BrandOption> {
        val bare = HashMap<String, Int>()
        for (s in stations) {
            val t = brandTokens(s.rotulo)
            if (t.size == 1) bare[t[0]] = (bare[t[0]] ?: 0) + 1
        }
        val counts = HashMap<String, Int>()
        val logos = HashMap<String, StationBrandLogo>()
        for (s in stations) {
            val tokens = brandTokens(s.rotulo)
            val known = tokens.firstNotNullOfOrNull { StationBrandLogo.from(it) }
            if (known != null) {
                counts[known.key] = (counts[known.key] ?: 0) + 1
                logos[known.key] = known
                continue
            }
            val discovered = tokens.firstOrNull {
                it !in genericBrandTokens && it.length >= 3 && (bare[it] ?: 0) >= 5
            } ?: continue
            counts[discovered] = (counts[discovered] ?: 0) + 1
        }
        val branded = counts.filterKeys { logos[it] != null }
            .map { BrandOption(it.key, logos[it.key]!!.displayName, logos[it.key], it.value) }
            .sortedByDescending { it.stationCount }
        val discovered = counts.filterKeys { logos[it] == null && (counts[it] ?: 0) >= 20 }
            .map {
                BrandOption(it.key, it.key.replaceFirstChar { c -> c.uppercase() }, null, it.value)
            }
            .sortedByDescending { it.stationCount }
        return branded + discovered
    }
}
