# Guía — App Android de HemoGestión

App Android (Java) para Android 7.0 (API 24) o superior. Se conecta a la misma API PHP y a la misma base MySQL que usa
el programa de escritorio. No accede a MySQL directamente: todo pasa por la API.

## 1. Qué hace

| Pantalla | Para qué sirve | Quién la usa |
|---|---|---|
| Ingreso | Usuario y contraseña de HemoGestión (los mismos del escritorio) | Todos |
| Inicio | Saludo, rol y módulos | Todos |
| Donantes | Lista paginada, búsqueda por DNI o nombre (sin tildes), alta | Todos |
| Ficha del donante | Datos, **restricciones** (alta y baja), **levantar la espera** (con motivo), **historial de donaciones** (tocar una donación: corregir, editar tiempo de recuperación o eliminar; eliminar solo Director y SuperAdmin), **cargar historial externo** (y eliminarlo), accesos a agendar y registrar donación | Todos |
| Alta / edición de donante | Datos personales, contacto, **domicilio y localidad**, consentimientos, tatuaje | Todos |
| Agenda | Semana, turnos del día, confirmar, "no asistió", **reprogramar**, cancelar con motivo, **recordatorio por WhatsApp** (abre el chat) y envío masivo de pendientes | Todos |
| Agendar | Donante, tipo de donación, día y horario con cupo libre | Todos |
| Registrar donación | Signos vitales, hemograma, resultado. El equipo la carga **Pendiente**; el resultado lo define el Director o el SuperAdmin | Todos (resultado: Director y SuperAdmin) |
| Restricciones médicas | Catálogo del centro (por ejemplo "Embarazo", 300 días): alta, edición y baja | Todos lo ven; Director y SuperAdmin lo modifican |
| Búsqueda avanzada | Donantes para recontactar: elegibilidad, consentimiento y turno futuro de cada uno | Todos |
| Estadísticas | Resumen, tendencia por mes o semana, por procedimiento | Director y SuperAdmin |

Las reglas (quién puede donar, un solo turno activo, cupos, permisos por rol, auditoría) las aplica **la API**; la app solo
muestra lo que la API responde. Los mensajes de error de la API se muestran tal cual.

## 2. Requisitos

- Android Studio (versión Rabbit o posterior), con el JDK que trae (JBR). No hace falta instalar otro JDK.
- SDK de Android con la plataforma **Android 37** (ya instalada en esta PC).
- XAMPP con MySQL en el puerto 3307 y la API corriendo: ver `docs/guia-xampp.md` del proyecto de escritorio
  (`scripts\iniciar-api.ps1`).
- Un usuario de HemoGestión creado en el escritorio. La app ingresa con ese usuario y esa contraseña.

## 3. Abrir y ejecutar

1. Android Studio → **Open** → carpeta `proyecto-plaquetas-java`. Esperar la sincronización de Gradle.
2. Elegir el emulador `Medium_Phone_API_37.0` y apretar **Run ▶**.
3. Con el emulador abierto, **una vez por sesión**, en PowerShell:

   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:8090 tcp:8090
   ```

   Reenvía el puerto 8090 del emulador a la API de la PC, sin abrir nada a la red. Hay que repetirlo si el emulador o adb
   se reinician. Sin esto la app dice "No se pudo conectar con la API".

Desde la línea de comandos: `.\gradlew.bat :app:assembleDebug` (APK en `app\build\outputs\apk\debug\`) y
`.\gradlew.bat :app:testDebugUnitTest` (pruebas). Gradle usa el JDK de Android Studio:
`$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'`.

## 4. Dónde cambiar cada cosa

| Qué querés cambiar | Dónde |
|---|---|
| **Colores** (marca, fondos, textos, estados) | `app/src/main/res/values/colors.xml` |
| **Tipografías** (tamaño, negrita, color de títulos y textos) | `app/src/main/res/values/themes.xml`, bloques `TextAppearance.HemoGestion.*` |
| **Botones, campos y tarjetas de módulos** | `app/src/main/res/values/themes.xml`, bloques `Widget.HemoGestion.*` |
| **Radios, márgenes y alturas** | `app/src/main/res/values/dimens.xml` |
| **Fondo de las tarjetas** (color, borde, radio) | `app/src/main/res/drawable/bg_tarjeta.xml` |
| **Colores de los estados** (verde, rojo, ámbar, gris) | `drawable/bg_chip_*.xml` y los colores `hemo_*_suave` de `colors.xml` |
| **Barra de navegación de abajo** | `layout/view_barra_inferior.xml` y `drawable/bg_nav_item.xml` |
| **Textos de la app** (en español) | `app/src/main/res/values/strings.xml` |
| **Opciones de los desplegables** | `app/src/main/res/values/arrays.xml` |
| **Icono de la app** | `drawable/ic_launcher_foreground.xml` (el fondo es `hemo_primario`) |
| **Imagen de fondo** | Poner la imagen en `res/drawable/` y usarla como `android:background="@drawable/mi_fondo"` en el layout (`activity_inicio.xml`, `activity_login.xml`...) |
| **Tipografía propia** (.ttf/.otf) | Carpeta `res/font/` y `android:fontFamily="@font/nombre"` en los estilos de `themes.xml` |
| **Diseño de cada pantalla** | `app/src/main/res/layout/*.xml` |
| **URL de la API** | `gradle.properties`, línea `hemo.apiBaseUrl` |
| **Zona horaria del centro** | `util/Fechas.java` (`ZONA`) y `zona_horaria` en `apiPHP/config.php` (tienen que coincidir) |

Todos los colores están en `colors.xml` con nombres `hemo_*`: cambiar un valor ahí cambia toda la app.

## 5. La URL de la API

En `gradle.properties`:

```properties
hemo.apiBaseUrl=http://127.0.0.1:8090/api/v1
```

- **Emulador:** `127.0.0.1` dentro del emulador apunta a la API de la PC gracias a `adb reverse` (sección 3).
- **Celular físico:** la API tiene que aceptar conexiones desde la WiFi, lo que implica cambiar `scripts\iniciar-api.ps1`
  para escuchar en `0.0.0.0` y permitir el puerto 8090 en el Firewall de Windows. Es una decisión de seguridad que se
  toma aparte. Después se pone `http://IP-DE-LA-PC:8090/api/v1` en `hemo.apiBaseUrl` y la IP en
  `app/src/main/res/xml/network_security_config.xml`.

El tráfico HTTP sin cifrar está permitido solo para `127.0.0.1`, `localhost` y `10.0.2.2`.

## 6. Estructura del código

```
app/src/main/java/com/hemogestion/app/
  api/      HemoApi (cliente HTTP), Sesion (token guardado), ApiException (errores de la API)
  modelo/   Donante (JSON completo de la API), Usuario, TipoDonacion, Permisos
  ui/       Una Activity por pantalla. BaseActivity (hilos, errores, sesión), BarraInferior, Bordes
  util/     Fechas (zona del centro), Texto, Hilos, Errores
app/src/test/                                  Pruebas unitarias (Fechas, Texto, Donante, Permisos, ApiException)
```

- Las llamadas a la API se hacen siempre en un hilo de fondo (`BaseActivity.cargar`) y el resultado vuelve a la pantalla.
- Si la API responde 401, la app borra el token y vuelve al ingreso.
- El token está en las preferencias privadas de la app (`allowBackup=false`).
- El formulario conserva el JSON completo del donante y solo cambia los campos que muestra: así no se pisan los
  consentimientos ni los diferimientos que la app no muestra.

## 7. Endpoints que usa la app

Los de siempre (`/sesiones`, `/donantes`, `/donaciones`, `/turnos`, `/configuracion/*`, `/mensajes/directo`) y los que
agrega la API para clientes móviles, que aplican las reglas del lado del servidor (ver `apiPHP/README.md`):

| Ruta | Para qué |
|---|---|
| `GET /agenda/disponibilidad` | Horarios libres de un día para un tipo de donación |
| `POST /agenda/turnos` | Agendar: valida donante activo, horario real, un solo turno activo, elegibilidad y cupos |
| `POST /turnos/{id}/accion` | Confirmar, cancelar (con motivo), asistió, no asistió |
| `GET /donantes/{id}/elegibilidad` | Si el donante puede donar cada tipo en una fecha, y por qué no |
| `GET /recontacto` | Donantes evaluados para recontactar |
| `GET /informes/resumen` | Estadísticas del período |
| `POST /donaciones/registrar` | Registrar una donación con sus reglas y avisos |

## 8. Pruebas realizadas

- 24 pruebas unitarias (`:app:testDebugUnitTest`) y análisis estático (`:app:lintDebug`) sin errores de compatibilidad con API 24.
- Recorrido completo en el emulador (Android 37) con datos de prueba, para los roles SuperAdmin y Equipo de trabajo:
  ingreso, donantes, ficha, restricciones, agenda, agendar, confirmar, registrar donación, búsqueda avanzada con
  invitación, estadísticas, catálogo de restricciones, alta de donante y permisos por rol.
- Todavía **no** se probó en un celular físico ni en un emulador con Android 7.0 (API 24): el emulador disponible es API 37.

## 9. Limitaciones conocidas

- La confirmación de turno por correo **no se envía** cuando el turno se crea desde la app (sí se envían los
  recordatorios, que los programa el escritorio para cualquier turno).
- Los recordatorios por WhatsApp los envía el servidor (OpenWA, ver `apiwhatsapp/README.md`). La app solo abre el chat de un
  turno (click-to-chat, necesita WhatsApp instalado) y dispara el envío masivo de pendientes. Si WhatsApp está apagado en el
  servidor, la app muestra el motivo.
- Solo en el escritorio (decisión del centro): gráficos, informes en PDF y la lista de donantes que dejaron de venir.
- Reprogramar, levantar la espera, corregir/eliminar donaciones y cargar historial externo **sí** están en la app y usan las rutas
  de la API descritas en `apiPHP/README.md`. Se probaron en el emulador (API 37); no en un celular físico.
- Las reglas de elegibilidad están implementadas **en la API** (PHP) y en el escritorio (C++). Una prueba automática
  (`ElegibilidadApiTest`) compara las dos; si se cambia una regla hay que cambiarla en ambas.
