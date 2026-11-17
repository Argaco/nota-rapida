# Nota Rápida (versión 1, Android)

App para guardar apuntes mientras estudias, sin salir de lo que estás leyendo.
Un botón amarillo flotante abre una nota adhesiva encima de cualquier app o página:
escribes o dictas con el micrófono, y el apunte queda guardado en la lista.

## Cómo abrirla y probarla

1. Instala [Android Studio](https://developer.android.com/studio) (versión Ladybug o más nueva).
2. Abre la carpeta `NotaRapida` con **File > Open**. Android Studio descargará Gradle y las
   librerías la primera vez (necesita internet).
3. Conecta un teléfono Android con la depuración USB activada, o crea un emulador, y pulsa **Run**.
4. En la app: **Empezar > Ajustes > Mostrar el botón sobre otras apps**. Android te llevará a una
   pantalla para dar el permiso; actívalo y vuelve a la app.
5. Abre cualquier otra app o página. Toca el botón amarillo, escribe o toca el micrófono, y pulsa
   **Guardar** (o toca fuera de la nota).

> Nunca se compiló antes de entregarse este proyecto: el código se escribió sin acceso al
> Android SDK. Si Android Studio muestra algún error al sincronizar o compilar, pásalo tal cual
> para corregirlo.

## Qué incluye

- Lista de apuntes, el último guardado primero, con buscador, edición y eliminación.
- Nota rápida transparente sobre cualquier app, con dictado por voz en tiempo real.
- Botón flotante arrastrable que se pega al borde de la pantalla.
- Ajustes: activar o desactivar el botón, idioma del dictado, empezar a dictar al abrir la nota.
- Todo se guarda en el teléfono (base de datos Room). No hay cuentas ni servidores.

## Cómo está organizado

| Carpeta | Contenido |
|---|---|
| `data/` | Base de datos de apuntes (Room) |
| `service/FloatingButtonService.kt` | El botón flotante, que vive en un servicio en primer plano |
| `voice/VoiceDictation.kt` | Dictado con el reconocimiento de voz de Android |
| `ui/` | Pantallas: lista, editor, ajustes, bienvenida y la nota rápida |
| `QuickNoteActivity.kt` | La pantalla transparente que muestra la nota adhesiva |

## Límites conocidos

- El dictado usa el reconocimiento de voz de Google: en muchos teléfonos necesita internet.
- Mientras el botón está activo, Android muestra un aviso fijo en las notificaciones. Es obligatorio
  para que el servicio no se cierre.
- Decir la palabra "nota" para abrir la nota queda para la versión 2.
- No hay copias de seguridad ni sincronización entre dispositivos.
- El proyecto no trae `gradlew`: Android Studio se encarga de Gradle al abrirlo.
