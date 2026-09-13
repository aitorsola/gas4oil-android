package com.aitorsola.gas4oil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import java.util.concurrent.TimeUnit

class G4OException(val kind: Kind, cause: Throwable? = null) : Exception(cause) {
    enum class Kind { NETWORK, BAD_STATUS, EMPTY, PARSE }
}

object StationsApi {

    private const val SPAIN_STATIONS =
        "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/"
    private const val FRANCE_STATIONS =
        "https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/exports/json"
    private const val PORTUGAL_STATIONS =
        "https://precoscombustiveis.dgeg.gov.pt/api/PrecoComb/PesquisarPostos?idsTiposComb=3201,3205,3400,2101,2105,1120&qtdPorPagina=30000&pagina=1"
    private const val ITALY_PRICES = "https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv"
    private const val ITALY_REGISTRY = "https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv"
    private const val CROATIA_STATIONS = "https://mzoe-gor.hr/data.json"
    private const val FRANCE_FIELDS =
        "id,adresse,cp,ville,departement,geom,gazole_prix,sp95_prix,e10_prix,sp98_prix,e85_prix,gplc_prix,horaires_automate_24_24,horaires_jour,services_service"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: OkHttpClient by lazy {
        val legacyTls = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2)
            .allEnabledCipherSuites()
            .build()
        OkHttpClient.Builder()
            .connectionSpecs(listOf(legacyTls, ConnectionSpec.CLEARTEXT))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    suspend fun allStations(country: Country): List<Station> = when (country) {
        Country.SPAIN -> fetch(SPAIN_STATIONS) { stream ->
            json.decodeFromStream<StationsResponse>(stream).prices.mapNotNull { it.toStation() }
        }
        Country.FRANCE -> fetch("$FRANCE_STATIONS?select=$FRANCE_FIELDS") { stream ->
            json.decodeFromStream<List<FranceRow>>(stream).mapNotNull { it.toStation() }
        }
        Country.PORTUGAL -> fetch(PORTUGAL_STATIONS) { stream ->
            json.decodeFromStream<PortugalResponse>(stream).stations()
        }
        Country.ITALY -> coroutineScope {
            val prices = async { fetchText(ITALY_PRICES) }
            val registry = async { fetchText(ITALY_REGISTRY) }
            val stations = ItalyFeed.stations(ItalyFeed.rows(registry.await()), ItalyFeed.rows(prices.await()))
            if (stations.isEmpty()) throw G4OException(G4OException.Kind.PARSE)
            stations
        }
        Country.CROATIA -> fetch(CROATIA_STATIONS) { stream ->
            json.decodeFromStream<CroatiaFeed>(stream).stations()
        }
    }

    private suspend fun fetchText(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw G4OException(G4OException.Kind.NETWORK, e)
        }
        response.use {
            if (!it.isSuccessful) throw G4OException(G4OException.Kind.BAD_STATUS)
            it.body?.string() ?: throw G4OException(G4OException.Kind.EMPTY)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun fetch(
        url: String,
        parse: (java.io.InputStream) -> List<Station>
    ): List<Station> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()
        val response = try {
            client.newCall(request).execute()
        } catch (e: Exception) {
            throw G4OException(G4OException.Kind.NETWORK, e)
        }
        response.use {
            if (!it.isSuccessful) throw G4OException(G4OException.Kind.BAD_STATUS)
            val body = it.body ?: throw G4OException(G4OException.Kind.EMPTY)
            val stations = try {
                body.byteStream().buffered(1 shl 16).use(parse)
            } catch (e: Exception) {
                throw G4OException(G4OException.Kind.PARSE, e)
            }
            if (stations.isEmpty()) throw G4OException(G4OException.Kind.EMPTY)
            stations
        }
    }
}
