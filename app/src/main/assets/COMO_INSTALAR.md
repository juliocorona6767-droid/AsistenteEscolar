# Asistente Escolar — App Android nativa

Este archivo vive dentro de los assets de la app solo como referencia interna;
no es necesario para el funcionamiento de la aplicación.

## Qué es esta app

Este es un **APK nativo de Android**, no una PWA. La interfaz (`index.html`,
CSS y JS) corre dentro de un `WebView` empacado en la app, pero las alarmas,
el sonido, la vibración y la voz (Text-to-Speech) los maneja código Java
nativo (`AlarmManager`, `AlarmService`, `TextToSpeech`), por lo que funcionan
aunque la app esté cerrada, en segundo plano o el teléfono se haya
reiniciado (gracias a `BootReceiver`).

## Sobre la alarma y la voz

- La alarma se programa con `AlarmManager` de Android y se reprograma
  automáticamente para el día siguiente al dispararse.
- Al reiniciar el teléfono, `BootReceiver` vuelve a programar la alarma
  guardada.
- El sonido y la vibración corren en un servicio de primer plano
  (`AlarmService`) con notificación de pantalla completa.
- Al tocar **Apagar Alarma**, se detiene el servicio y se reproduce un
  resumen por voz en español (México) usando `TextToSpeech` nativo de
  Android.

En Android 12+ puede ser necesario conceder el permiso especial
**Alarmas y recordatorios** para alarmas exactas, y en Android 13+ se pide
permiso de notificaciones. Para que la alarma no falle con la pantalla
apagada, conviene quitar restricciones agresivas de batería para la app.
