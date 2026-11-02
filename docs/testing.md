# Pruebas de instalación y aceptación

## Automatizadas, sin Ollama

La suite Java tiene 302 pruebas deterministas (la de PowerShell solo se ejecuta en
Windows). Usa modelos y servidor HTTP falsos
en loopback; no requiere Ollama instalado, GPU, Internet para inferencia ni modelos
descargados. La primera compilación sí puede necesitar Maven y dependencias de red.

### Windows

```powershell
.\mvnw.cmd -B -ntp clean verify
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

### Linux

```bash
sh ./mvnw -B -ntp clean verify
sh scripts/smoke-test.sh
```

Los smoke tests extraen el ZIP en una carpeta temporal y prueban:

- Contenido del paquete y ejecución portable de `--version`.
- Instalación desde ZIP y desde el repositorio con build previo.
- Actualización: nueva referencia activa y versión anterior conservada.
- Rutas con espacios, directorio del corpus sin cambiar y códigos de salida 0/1/2.
- Los tests Java de lanzadores verifican además acentos, comillas, argumentos
  vacíos y barras finales con una clase de eco propia; no ejecutan código de documentos.
- Conservación byte a byte de configuración, plantilla e índice de prueba.
- Rechazo de carpetas ajenas, Java ausente y paquetes con versión incompatible.
- Limpieza del staging creado por un intento fallido sin tocar versiones activas.
- Linux: PATH autorizado en un HOME temporal, escape de un dólar literal y no
  duplicación de la entrada en `.profile`.
- Windows: no modificar el PATH real del usuario sin `-AddToPath`.

No se instalan aplicaciones en tu perfil real ni se usa tu configuración. La
opción Windows `-AddToPath` escribe el PATH de usuario solo cuando se solicita;
los smoke tests locales no activan esa opción para no cambiar tu registro.

En este equipo Windows también se ejecuta el script POSIX con Git Bash. Eso **no
equivale a probar Linux nativo**: la matriz de `.github/workflows/ci.yml` realiza
esa comprobación en `ubuntu-latest` y `windows-latest`. Consulta la ejecución de
GitHub Actions para confirmar el resultado de ambos sistemas; no se instala WSL.

## Pruebas manuales con Ollama y documentos reales

Instala y arranca Ollama por tu cuenta y configura modelos que ya tengas. Primero:

```bash
kumo init
kumo status --corpus ./documentos
```

No cambies el modelo de embeddings de un índice existente sin reindexar. Usa una
carpeta de pruebas propia y destinos de salida nuevos para no sobrescribir trabajo.

| Caso | Prueba | Resultado que debes revisar |
|---|---|---|
| TXT o Markdown | `kumo add --corpus ./documentos ./documentos` y luego `ask` | Respuesta sustentada, fuentes y extractos correctos. |
| PDF de varias páginas | Preguntar por un dato de una página concreta | Página citada coincide con el PDF; no se mezclan páginas al fragmentar. |
| PDF escaneado | Intentar resumir uno sin texto extraíble | Error claro; no se promete OCR. |
| Texto corto | `kumo summarize archivo.txt` | Elección Stuff, resumen fiel y breve. |
| Texto largo | `kumo summarize libro.txt --strategy auto` | Elección MapReduce, progreso por fragmento/etapa y sin truncado silencioso. |
| Código | `kumo summarize Main.java --language English` | Explica propósito y flujo, no ejecuta código ni afirma conocer el proyecto. |
| Ficha | `kumo brief archivo.txt` | JSON válido, requisitos separados de tareas y datos ausentes vacíos/nulos. |
| Tareas | Texto con una tarea, responsable y fecha explícitos | Evidencia literal y datos que realmente aparecen en la fuente. |
| Plantilla | Quitar un campo de `fields` y repetir `brief` | Ese campo deja de aparecer; `title` y `summary` se mantienen. |
| Carpeta | `kumo brief ./documentos --output fichas.json` | Una ficha por archivo; `results` no mezcla documentos. |
| Redacción | `kumo write --corpus ./documentos "Redacta una introducción"` | Aviso de cobertura parcial, citas y lista de fuentes recuperadas. |
| Presupuesto pequeño | Bajar `numCtx` o subir top-k en una copia de configuración | Error antes del chat si no cabe; no se recortan fuentes silenciosamente. |
| Salida existente | Repetir un comando con el mismo `--output` | Error sin sobrescribir el archivo. |
| Actualización | Repetir el instalador | Tu modelo elegido y plantilla personalizada siguen intactos. |

Las pruebas automáticas verifican contratos, no la calidad universal de los LLM.
Revisa especialmente las fechas, requisitos omitidos, conclusiones y respaldo
semántico de las citas. El conteo `chars/4` es aproximado y MapReduce puede perder
matices. No uses un borrador generado como evidencia independiente de la fuente.

## Solución de problemas

- **Java 17 o antiguo:** abre una terminal nueva y revisa `JAVA_HOME`. Los
  lanzadores dan prioridad a `JAVA_HOME` frente al ejecutable de PATH.
- **`kumo` no se reconoce:** verifica la carpeta `bin` del instalador y abre una
  sesión nueva. Puedes ejecutar el lanzador por su ruta completa sin tocar PATH.
- **PowerShell bloquea scripts:** usa el comando documentado con `-NoProfile
  -ExecutionPolicy Bypass -File`, limitado a ese proceso. Una política de dominio
  puede seguir bloqueándolo; no se intenta eludir controles de la organización.
- **Ollama no conecta:** abre la aplicación/servicio y comprueba localhost:11434;
  no descargues de nuevo los modelos si ya están instalados.
- **Falta un modelo:** revisa `ollama list` y cambia `chatModel` si corresponde.
  La configuración predeterminada no obliga a descargar un modelo que no quieras.
- **No abre el JSON:** ejecuta `kumo init`; crea la configuración y plantilla si
  faltan, pero no reemplaza ediciones existentes.
- **Falta `lib/`:** extrae todo el ZIP o reinstala; no copies solo el jar.
- **Incompatibilidad de versión:** conserva el paquete completo y verifica que
  `VERSION` corresponda al manifiesto; el instalador no activa un paquete que falle.
