# ACTIUM

Tu progreso. Tus datos. Tu evolución.

Aplicación de seguimiento de nutrición, entrenamiento, agua, sueño y medidas corporales. Web en React/TypeScript y Android en Kotlin/Jetpack Compose, con Supabase Auth, PostgreSQL y almacenamiento privado.

## Web

La publicación de GitHub Pages se realiza automáticamente al actualizar `main` mediante `.github/workflows/pages.yml`. En Settings → Pages, selecciona **GitHub Actions** como fuente de publicación.

Dirección prevista: https://acnmotocros3095-droid.github.io/ACTIUM/

`web/public-config.json` contiene únicamente la URL y la clave pública publishable/anon de Supabase. Es configuración visible para los clientes por diseño; los datos personales se protegen con autenticación y políticas RLS. Nunca añadas claves `secret`, `service_role` ni claves de proveedores de IA a ese archivo.

Para desarrollar:

```sh
cd web
npm ci
cp .env.example .env
# Configura la URL y la clave pública del proyecto en .env.
npm run dev
```

La web local utiliza http://localhost:3000/. En Windows puedes ejecutar `npm.cmd` si PowerShell bloquea el script `npm`.

Para comprobar la publicación localmente:

```sh
cd web
node scripts/configure-public-env.mjs
npm test
npm run build -- --base=/ACTIUM/
```

## Supabase

Proyecto nuevo: ejecuta `supabase/actium_mvp.sql` y después `supabase/actium_v2.sql`. Si ya utilizas ACTIUM, no recrees las tablas. Los cambios de interfaz no requieren una base nueva.

En Authentication → URL Configuration configura la dirección publicada como Site URL y añádela a Redirect URLs. Conserva http://localhost:3000/ en Redirect URLs si continúas desarrollando en local.

El modo local de la web guarda registros en cada navegador y origen; esos registros no se migran automáticamente a la nube. Los registros de una cuenta Supabase se consultan iniciando sesión con esa cuenta. Las preferencias de perfil continúan guardándose por navegador.

## Android

Abre `android/` en Android Studio. Requiere JDK 17 o 21 y Android SDK 35. Configura en tus propiedades de Gradle:

```properties
ACTIUM_SUPABASE_URL=https://TU-PROYECTO.supabase.co
ACTIUM_SUPABASE_ANON_KEY=TU_CLAVE_PUBLICA
```

No subas archivos de firma ni propiedades locales. Android v3 incluye dashboard con macros, gráficas de progreso y actividad, navegación inferior y formularios en paneles. El registro de series y el planificador semanal permanecen en la web.

## Asistente

La función de Supabase está en `supabase/functions/actium-coach/`. La clave del proveedor se configura exclusivamente como secreto del servidor. La interfaz solicita consentimiento antes de enviar un resumen de registros al proveedor. El despliegue de la web no activa por sí solo esta función.

## Pruebas

La web incluye pruebas del cálculo nutricional, fechas, metas, copias locales y promedios. Android incluye pruebas de fechas, promedios y límites de las gráficas. Compilar y publicar no sustituye la prueba de autenticación y permisos con cuentas reales.
