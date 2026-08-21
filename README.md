# Asistente Escolar — Android nativo

App Android nativa (Java + WebView + JavaScript) con alarma, sonido,
vibración y Text-to-Speech reales del sistema. Compila en la nube con
GitHub Actions: no necesitas instalar Android Studio ni SDKs pesados.

## Qué se revisó y corrigió

- Puente Java ↔ JavaScript verificado método por método: cada llamada desde
  `index.html` a `AndroidBridge` tiene su contraparte exacta en
  `MainActivity.Bridge`, y viceversa (`window.nativeAlarm`,
  `window.onNativeToneChanged`).
- `AndroidManifest.xml` validado: las 4 clases declaradas
  (`MainActivity`, `AlarmReceiver`, `AlarmService`, `BootReceiver`)
  existen y coinciden exactamente.
- Los 4 archivos Java pasaron validación de balance de llaves/paréntesis
  (0 errores estructurales).
- El JavaScript embebido en `index.html` pasó `node --check` sin errores.
- Se agregaron las dependencias mínimas de AndroidX
  (`core`, `appcompat`, `webkit`) que `compileSdk 35` puede requerir para
  resolver recursos correctamente.
- Se eliminó `sw.js` (Service Worker): es un residuo de una versión PWA
  anterior del proyecto y no tiene ningún efecto dentro de un WebView
  empacado como app nativa (los Service Workers no funcionan sobre
  `file://`), así que solo generaba ruido en la consola.
- Se eliminaron 5 carpetas `mipmap-*` vacías sin usar (el ícono real vive
  en `res/drawable/ic_launcher.xml` como vector, que sí está referenciado
  correctamente en el Manifest).
- Workflow de GitHub Actions reforzado: caché de Gradle activado y un
  paso que confirma explícitamente que el APK se generó antes de subirlo
  como artefacto, para que un fallo silencioso nunca pase desapercibido.
- Gradle 8.7 + Android Gradle Plugin 8.6.1: combinación verificada como
  correcta (8.7 es la versión mínima que requiere AGP 8.6).

### Sobre el Gradle Wrapper

Este proyecto **no incluye `gradlew`** a propósito. `setup-gradle` (la
acción oficial que usa el workflow) valida el checksum de cualquier
`gradle-wrapper.jar` que encuentre en el repo, y ese archivo solo se
puede generar de forma confiable con el propio Gradle instalado. En vez
de arriesgar un wrapper corrupto, el workflow le pide a `setup-gradle`
que instale y verifique Gradle 8.7 directamente en el runner de GitHub
— es un patrón oficialmente soportado y es exactamente como ya
funcionaba tu workflow original. No afecta en nada al resultado final.

## Cómo subirlo a GitHub y obtener tu APK

1. Crea un repositorio nuevo en GitHub (puede ser privado).
2. Sube **todo el contenido de la carpeta `project/`** a la raíz del
   repositorio (no subas el ZIP, y no metas todo dentro de una
   subcarpeta extra).
3. En cuanto GitHub reciba el push, la pestaña **Actions** del
   repositorio va a mostrar el workflow **Build APK** ejecutándose solo.
4. Espera a que termine (unos 3-6 minutos la primera vez; las
   siguientes veces será más rápido gracias al caché de Gradle).
5. Entra a esa ejecución terminada, baja hasta **Artifacts** y descarga
   `AsistenteEscolar-debug`.
6. Descomprime ese archivo: adentro está `app-debug.apk`, listo para
   instalar en tu teléfono.

También puedes lanzar la compilación manualmente sin hacer push:
**Actions → Build APK → Run workflow**.

## Compilación local (opcional)

Si algún día tienes Gradle instalado en una PC:

```bash
gradle --no-daemon assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Permisos importantes en Android

- Android 12+: puede pedirte activar manualmente el permiso especial
  **Alarmas y recordatorios** para que la alarma sea exacta.
- Android 13+: se solicita permiso de notificaciones al abrir la app.
- Para que la alarma no falle con la pantalla apagada, quita las
  restricciones agresivas de batería para esta app (ajustes específicos
  de cada fabricante: Xiaomi, Samsung, etc. suelen tener uno propio
  además del de Android).
