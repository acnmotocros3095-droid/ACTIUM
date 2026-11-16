# Verificación Android v3

- Código Kotlin compilado con Gradle 8.9, JDK 21 y Android SDK 35.
- APK de desarrollo generado con identificador `com.actium.app`, versión 3.0 y código 3.
- Firma del APK verificada con `apksigner` (esquema v2, un firmante).
- Se utilizó la firma de desarrollo existente en este equipo y la configuración pública de Supabase del proyecto. El APK no incluye claves privilegiadas.
- Cuatro pruebas de cálculo correctas: conversión de fecha según zona horaria; promedio semanal por días medidos; dominio válido para gráficas con pesos iguales o sin datos; progreso con metas cero, excesos y valores no finitos.
- La cuenta, los registros de Supabase y la web no se modificaron para preparar la actualización.

No se ejecutó el APK en un teléfono o emulador en esta sesión. La prueba con la cuenta y el dispositivo reales se realiza al abrir la actualización. Las pantallas no inventan historial: muestran estados vacíos cuando faltan registros y solo trazan tendencias con al menos dos días medidos.

La integración de red conserva los endpoints anteriores. No se ha probado con datos privados del usuario ni se ha enviado ningún registro al asistente IA durante el desarrollo.

La comprobación visual automática con el renderer de pruebas no se completó: el proceso quedó bloqueado y fue detenido. No se presentan capturas como si hubieran sido verificadas. Está pendiente la revisión visual en el teléfono real.
