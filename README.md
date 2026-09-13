# Gas4Oil para Android

Precios de carburante por estación de servicio en España, Francia, Portugal, Italia y Croacia, con datos oficiales de cada país. Versión Android de [gas4oil-ios](https://github.com/aitorsola/gas4oil-ios).

## Descargar

El APK de cada versión está en [Releases](https://github.com/aitorsola/gas4oil-android/releases). Requiere Android 8.0 (API 26) o superior.

## Funciones

- Estaciones ordenadas por cercanía, precio ascendente o descendente
- Selector de país; con ubicación se detecta automáticamente
- Búsqueda por población y selector de población cuando no hay ubicación
- Filtro por marca y por combustible (95, 95+, 98, diésel, diésel+, GLP, E10, E85 según el país)
- Coste de llenar tu depósito y la estación más barata a 50 km, con ruta
- Favoritos con precios actualizados
- Tema claro, oscuro o del sistema

## Fuentes de datos

- España: Ministerio para la Transición Ecológica
- Francia: data.economie.gouv.fr
- Portugal: DGEG
- Italia: MIMIT
- Croacia: Ministarstvo gospodarstva

## Compilar

Android Studio con JDK 21. `./gradlew assembleRelease` genera el APK en `app/build/outputs/apk/release/`.
