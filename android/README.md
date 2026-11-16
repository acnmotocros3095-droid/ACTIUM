# ACTIUM Android v3 · Dark Performance

La interfaz Android se ha rediseñado manteniendo la cuenta, el identificador de la aplicación y las tablas de Supabase existentes. La web no necesita actualizarse.

## Actualizar en el teléfono

Abre `ACTIUM_ANDROID_V3.apk` en tu teléfono y elige **Actualizar**. Es una compilación de desarrollo para uso personal, con la configuración pública de Supabase del proyecto existente y la firma de desarrollo disponible en este equipo. No es una publicación en Google Play.

Los datos que ya guardaste en Supabase pertenecen a tu cuenta; entra con el mismo correo y contraseña. Las gráficas se completarán con esos registros al actualizar. No hace falta crear otra base de datos ni ejecutar de nuevo el SQL.

## Continuar desde Android Studio

También se actualizó el código en la carpeta Android del proyecto extraído `ACTIUM_V2_PUERTO_3000/ACTIUM_V2/android`. Abre ese proyecto, sincroniza Gradle y pulsa **Run** con tu teléfono conectado. Se conserva la configuración de Supabase que ya tenías.

El ZIP Android v3 contiene una copia limpia del proyecto para conservar el código. Si abres esa copia en otra carpeta o equipo, utiliza las mismas propiedades `ACTIUM_SUPABASE_URL` y `ACTIUM_SUPABASE_ANON_KEY` de tu proyecto. Se requieren JDK 17 o 21 y Android SDK 35. El teléfono debe tener Android 8 o posterior.

## Qué cambia

- Inicio con anillo de calorías, metas y tarjetas de proteína, carbohidratos y grasas.
- Navegación inferior fija: Hoy, Diario, Entrenar, Progreso y Más.
- Gráfico semanal de calorías registradas y accesos rápidos.
- Progreso de peso y cintura con períodos de 7, 30 y 90 días, tendencia y promedio semanal.
- Registro de comida en un panel, con valores por porción o por cada 100 g y teclado numérico.
- Agua con accesos rápidos, progreso diario y gráfico semanal.
- Sueño con calendario y reloj, calidad del descanso y gráfico semanal.
- Entrenamiento con volumen, marcas por ejercicio, sesiones y temporizador de descanso.
- Mediciones opcionales de peso, cintura, pecho, brazo y muslo.
- Pantalla del asistente renovada, manteniendo el consentimiento para compartir el resumen con IA.
- Icono propio de ACTIUM y adaptación a las barras del sistema.

## Cómo leer las gráficas

Se utilizan los registros de tu cuenta. Un día sin registros aparece como “—” y no se considera consumo cero. Para una línea de progreso hacen falta dos días con mediciones. El promedio semanal pondera cada día medido por igual, aunque un día tenga varias mediciones.

No se incluyen datos de ejemplo en el APK ni se insertan registros en tu cuenta. La revisión visual final se realiza en tu teléfono: el proceso de renderizado automático no llegó a completarse en este entorno.

## Alcance

El registro de series y el planificador semanal siguen en la web; Android muestra sus sesiones y estadísticas al actualizar. El asistente necesita la función `actium-coach` ya configurada. La sesión todavía requiere iniciar sesión de nuevo al cerrar completamente la aplicación.

La compilación y las pruebas de cálculo se realizan en este equipo; la comprobación con tu cuenta y teléfono real se completa al instalar y abrir la actualización.
