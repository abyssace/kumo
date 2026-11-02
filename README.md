# kumo

Asistente de documentos **100 % local** por línea de comandos. Indexa tus
archivos, responde preguntas con RAG y cita las fuentes. Todo ocurre en tu
máquina: kumo solo habla con Ollama en `localhost`, sin telemetría ni servicios
en la nube.

## Estado del proyecto

| Fase | Contenido | Estado |
|------|-----------|--------|
| F1 | Núcleo sin Ollama: dominio, interfaces, store en memoria, `RagService`, pruebas | ✅ |
| F2 | Loaders TXT/MD/PDF, chunker con solapamiento y persistencia JSON | ✅ |
| F3 | Cliente de Ollama y comandos `init`, `add`, `ask`, `status` | ✅ |
| F4 | Resumen Stuff + MapReduce con progreso | ✅ |
| F5 | `brief` (JSON estructurado) y `write` (Markdown) | ✅ |
| F6 | Instaladores, README final y release con `.jar` | ⏳ |
| F7 | Extras opcionales (chat, indexado incremental, web local) | ⏳ |

## Requisitos

- **JDK 21** con `JAVA_HOME` apuntando a él.
- **Maven Wrapper** incluido en el repo: no hace falta tener Maven instalado.
- **Ollama nativo**, escuchando en `http://localhost:11434`.

> En Windows, si `java -version` muestra 17, abre una terminal nueva o revisa
> `JAVA_HOME`. La sesión actual puede conservar el entorno anterior.

## Puesta en marcha

kumo **no instala Ollama ni descarga modelos**. Son pasos que decides tú:

```bash
ollama pull qwen3:8b
ollama pull embeddinggemma
ollama serve
```

Después:

```bash
./mvnw package
java -jar target/kumo.jar init
java -jar target/kumo.jar add --corpus ./documentos ./documentos
java -jar target/kumo.jar ask --corpus ./documentos "¿De qué trata el informe?"
```

El jar usa `target/lib/` para las dependencias, generado por Maven. Si mueves
`kumo.jar`, mueve también `lib/` a su lado.

### Comandos

| Comando | Qué hace |
|---------|----------|
| `kumo init` | Crea `~/.kumo/config.json` y comprueba Ollama y los modelos. |
| `kumo add [archivos...]` | Indexa archivos o carpetas en `<corpus>/.kumo/index.json`. |
| `kumo ask "<pregunta>"` | Recupera contexto y responde en streaming con fuentes. |
| `kumo status` | Muestra configuración, estado de Ollama e índice. |
| `kumo summarize <archivo>` | Resume un archivo directamente, sin índice ni embeddings. |
| `kumo brief <ruta>` | Extrae una ficha JSON por archivo, directamente y sin índice. |
| `kumo write "<instrucción>"` | Redacta Markdown a partir de fragmentos del índice y adjunta fuentes. |

`add`, `ask`, `status` y `write` aceptan `-c, --corpus <carpeta>` (por defecto `.`).
`add`, `ask` y `write` aceptan `-v, --verbose`; en `ask` y `write` añade la similitud
de cada fragmento. `ask` y `write` aceptan además `-k, --top-k`.

`init` comprueba, sin descargar nada, si Ollama responde y si los modelos
configurados están instalados. Si Ollama no está arrancado, avisa y sale con 0:
instalarlo o arrancarlo es decisión tuya, no un requisito para terminar la
configuración.

`add` indexa cada archivo por separado: si uno falla, el resto continúa y el
resumen indica cuántos se indexaron y cuántos fallaron. Reindexar un archivo
sustituye sus fragmentos en lugar de duplicarlos.

## Configuración

El archivo global vive en `~/.kumo/config.json`. Puedes mover esa carpeta con
la variable de entorno `KUMO_HOME` o, para pruebas y lanzadores avanzados, con
`-Dkumo.home=<ruta>`.

```json
{
  "ollamaBaseUrl": "http://localhost:11434",
  "chatModel": "qwen3:8b",
  "embeddingModel": "embeddinggemma",
  "numCtx": 8192,
  "embeddingContextTokens": 2048,
  "chunkSizeTokens": 400,
  "chunkOverlapTokens": 80,
  "topK": 5,
  "minScore": -1.0,
  "vectorStore": "JSON",
  "summaryInputRatio": 0.65,
  "writeTopK": 12
}
```

- Los modelos **nunca están hardcodeados** en el resto del código: se leen de
  aquí. Alternativas razonables: `llama3.1:8b` para comparar respuestas y
  `llama3.2:3b` para desarrollo rápido; `bge-m3` como embedding alternativo.
- `numCtx` es el contexto del chat. `embeddingContextTokens` es el límite del
  modelo de embeddings: son cosas distintas y no se deben confundir.
- `minScore = -1.0` desactiva el umbral (el coseno va de -1 a 1).
- `vectorStore` admite `JSON` (persistente) y `MEMORY` (solo para pruebas o uso
  como librería).
- `summaryInputRatio` limita la fracción de `numCtx` usada para contenido del
  resumen (valor entre 0 y 1, extremos excluidos). Los archivos de configuración
  anteriores siguen siendo válidos: si falta, se usa `0.65`.
- `writeTopK` es el top-k de redacción (12 por defecto, frente a 5 para `ask`).
  Puede sobrescribirse con `write --top-k`. Las configuraciones anteriores no
  necesitan editarse: los campos ausentes heredan sus valores predeterminados.

## Resumir documentos y código (F4)

```powershell
java -jar target/kumo.jar summarize "apuntes.pdf"
java -jar target/kumo.jar summarize "articulo.pdf" --language español --output "resumen.md"
java -jar target/kumo.jar summarize "Main.java" --language English
java -jar target/kumo.jar summarize "libro.txt" --strategy map-reduce
```

- **Un archivo por invocación**: TXT, MD/Markdown, PDF con texto y código UTF-8
  (`.java`, `.py`, `.js`, `.ts`, `.c`, `.cpp`, `.h`, `.cs`, `.go`, `.rs`, `.html`,
  `.css`, `.sql`). Las extensiones no distinguen mayúsculas. Las carpetas y los
  resúmenes conjuntos quedan fuera de F4.
- **Sin preparación de índice**: loader → chunker si hace falta → LLM. Solo
  necesita el modelo de chat configurado; no consulta ni necesita embeddings.
  El soporte de código de esta fase es para `summarize`; `add` conserva sus
  formatos de F3 (TXT/MD/PDF).
- **Resultado adaptativo**: se pide idea principal, puntos clave y conclusiones,
  breve salvo que haga falta ampliar para preservar lo esencial. En artículos
  científicos, objetivo, método, resultados y limitaciones solo si constan en
  el documento. Son instrucciones al modelo, no una garantía de exactitud.
- **Código como texto**: no se ejecuta, compila ni analiza un proyecto completo.
  Se pide explicar propósito, componentes, flujo y dependencias visibles, sin
  traducir identificadores ni afirmar que el programa funciona. Se advierte de
  este alcance en la consola y en el prompt.
- **Idioma**: por defecto se pide conservar el idioma predominante del documento.
  `--language español|es|spanish` e `--language inglés|en|english` fuerzan el idioma
  del resultado; también aceptan `espanol`, `ingles` y cualquier combinación de
  mayúsculas. No es una opción de traducción de la interfaz de consola.
- **Salida**: el Markdown completo se imprime por stdout; estrategia, motivo y
  progreso van por stderr. `--output` (o `-o`) guarda además el resultado UTF-8
  en un archivo nuevo. La carpeta debe existir; nunca se sobrescribe un destino
  existente ni se publica un resumen parcial si falla la generación.

### Estrategias y presupuesto

`--strategy auto|stuff|map-reduce` (por defecto `auto`):

1. **Stuff**: todo el texto en una petición cuando cabe en el presupuesto.
2. **MapReduce**: división por páginas y presupuesto estimado, resumen de cada
   fragmento y síntesis final. Si los parciales no caben juntos, se reducen por
   etapas, siempre secuencialmente y sin eliminar fragmentos del lote.
3. Forzar Stuff con un documento demasiado grande da un error antes de llamar
   al modelo, sin truncar. Forzar MapReduce aplica ese flujo incluso si cabe.

El techo del contenido es `floor(numCtx * summaryInputRatio)` (~65 % por defecto).
Se calcula también el coste de los prompts, se reservan para la respuesta
`max(256, floor(numCtx / 5))` tokens estimados y otros 64 para la plantilla de
chat. Si hace falta, el presupuesto se reduce aún más. **Cada petición** se
valida con su prompt completo y envía `options.num_ctx`, incluyendo todas las
etapas de reducción; `think:false` se conserva.

El chunker de resumen no usa solapamiento ni el límite del embedder: conserva
sangría y saltos de línea, prefiere cortes próximos en espacios y puede dividir
una línea/palabra muy larga sin perder su contenido. Las páginas PDF no se
mezclan en los fragmentos iniciales. El progreso muestra estrategia, motivo,
tokens estimados, división, parciales, cada etapa de reducción y resumen final.

**Límites conscientes:** `chars/4` es una estimación, no el tokenizador real del
modelo; con código u otros textos densos conviene bajar el ratio. La reserva
de respuesta no fuerza una longitud exacta de generación. Si el modelo no
reduce el contenido, devuelve texto vacío o falla, se aborta con un error, sin
guardar salida incompleta. Hay un límite de seguridad de 32 etapas de reducción.
MapReduce puede perder matices al comprimir: revisa siempre las conclusiones
contra la fuente. El archivo completo y los parciales se mantienen en memoria.

## Fichas estructuradas y plantilla editable (F5)

`brief` convierte documentos en fichas JSON: no es otro resumen Markdown.
Usa los mismos formatos que `summarize`, incluido código como texto sin ejecutarlo.
No necesita índice ni embeddings. Ejemplos desde la raíz del proyecto:

```powershell
java -jar target/kumo.jar brief "C:\Users\pedro\Documents\pruebakumo\prueba.txt" --language español
java -jar target/kumo.jar brief ".\documentos" --output ".\fichas.json"
java -jar target/kumo.jar brief ".\Main.java" --language English --template ".\examples\brief-template.json"
```

### Editar la plantilla JSON

`init` y la primera ejecución de `brief` sin `--template` crean, si no existe:

```text
~/.kumo/brief-template.json
```

En tu Windows, con la ubicación global predeterminada:

```powershell
java -jar target/kumo.jar init
notepad "$HOME\.kumo\brief-template.json"
```

La plantilla es un archivo distinto de `config.json`: la configuración elige
modelos y contexto; la plantilla elige **qué extraer**. Respeta también `KUMO_HOME`
y `-Dkumo.home`. Nunca se reemplaza una plantilla que ya exista. Incluimos una
copia completa editable en `examples/brief-template.json`.

Ejemplo completo de plantilla más sencilla:

```json
{
  "fields": ["title", "summary", "keyPoints", "requirements", "tasks"],
  "instructions": "Sé breve. Destaca los requisitos de instalación y las tareas explícitas."
}
```

Puedes quitar campos de `fields` y cambiar `instructions` (hasta 4000 caracteres).
`title` y `summary` son obligatorios, no se admiten campos desconocidos ni
duplicados. El archivo debe contener exactamente `fields` e `instructions`.
Es una plantilla de selección, **no un JSON Schema arbitrario ni una plantilla
de respuestas rellenadas a mano**. Para usar otra copia, añade `--template <ruta>`;
la ruta indicada debe existir y nunca se modifica.

| Campo permitido | Contenido |
|---|---|
| `documentType` | `notes`, `scientific`, `code` o `document`; código solo para extensiones de código identificadas por el loader. |
| `title` | Título descriptivo. |
| `summary` | Síntesis breve. |
| `keyPoints` | Puntos clave, como lista. |
| `requirements` | Requisitos explícitos; no se convierten automáticamente en tareas. |
| `conclusions` | Conclusiones respaldadas por la fuente. |
| `tasks` | Lista de objetos con `description`, `assignee`, `dueDate` y `evidence`. |
| `scientific` | Objeto con `objective`, `method`, `results`, `limitations`; `null` si no corresponde. |
| `technical` | Objeto con `purpose`, `components`, `flow`, `dependencies`; `null` si no corresponde. |

Se pide usar listas vacías y valores `null` para información ausente, no inventar
datos. En tareas, responsable y fecha deben copiarse literalmente o ser `null`;
`evidence` debe citar la fuente. Antes de aceptar la ficha, kumo comprueba que la
evidencia, los nombres y las fechas aparecen en el **documento original**,
normalizando espacios. Esto ayuda a detectar invenciones, pero no demuestra
automáticamente que la interpretación semántica de cada tarea sea correcta.

### Flujo, salida y límites de brief

- Archivo pequeño: loader → petición con `format` JSON Schema → validación local.
- Archivo extenso: loader → chunker → resúmenes y reducción de F4 con foco en
  extracción → petición estructurada. La salida lleva `compressed: true` y
  una advertencia: la compresión puede perder matices o requisitos. Si la
  síntesis aún no cabe, se aborta sin truncar.
- Se cuenta también el coste estimado del esquema y del prompt; se reserva
  respuesta y se envía siempre `options.num_ctx` y `think:false` en el chat.
- Los esquemas se generan localmente. El validador propio comprueba únicamente
  el subconjunto que kumo genera (tipos, objetos, propiedades requeridas,
  enumeraciones, listas, nulos y cadenas no vacías). No descarga esquemas ni
  implementa un validador general de JSON Schema.
- JSON roto, claves repetidas, campos extra, tipos incorrectos o evidencia
  inválida producen error: no se corrige ni se reintenta silenciosamente.
- Para un archivo se devuelve un objeto con `source`, `compressed`, `warnings`
  y `brief`. Para una carpeta se devuelve un objeto `results` con una ficha por
  archivo. No se mezcla el contenido de distintos documentos en una petición.
- Carpetas: recorrido recursivo ordenado, sin seguir enlaces simbólicos ni
  entrar en `.kumo`, `.git`, `target`, `build`, `dist`, `node_modules`. Un error
  aborta el lote completo: no se publica un JSON parcial. Las fichas ya
  generadas no se guardan si falla otro archivo.
- JSON final por stdout y progreso por stderr. `--output` guarda además el mismo
  JSON en un archivo nuevo. No se sobrescriben documentos, plantillas ni salidas.
- `--language` admite los alias de F4. Los nombres de campos y los códigos de
  `documentType` permanecen en inglés; los valores descriptivos siguen el idioma
  elegido. Los metadatos y advertencias de la aplicación permanecen en español.

## Redactar con el índice (F5)

Primero indexa las fuentes con `add`. Después:

```powershell
java -jar target/kumo.jar write --corpus "C:\Users\pedro\Documents\pruebakumo" "Redacta una descripción breve del proyecto kumo" --language español
java -jar target/kumo.jar write --corpus ".\documentos" "Redacta una introducción académica" --output ".\introduccion.md"
java -jar target/kumo.jar write --corpus ".\documentos" "Explain the architecture" --language English --top-k 8 --verbose
```

`write` acepta una instrucción libre para informes, explicaciones técnicas y
documentación. Usa `Embedder` → `VectorStore` → `LlmClient` sin streaming, con
progreso Observer. No ejecuta código ni modifica fuentes o el índice.

**Cobertura parcial:** el top-k mayor recupera más fragmentos, pero no lee todo
el corpus ni garantiza una síntesis exhaustiva. El aviso se imprime tanto en
stderr como en el propio Markdown. Se aplica `minScore`; sin fragmentos
relevantes, no se llama al chat. Si los fragmentos o las instrucciones no caben
con margen en el contexto, se aborta: aumenta `numCtx` o reduce `--top-k`.
No se descartan ni comprimen fuentes silenciosamente para hacerlas caber.

El prompt pide citas numéricas `[n]` para las afirmaciones. Se rechazan
redacciones vacías, sin ninguna cita numérica o con números fuera del contexto.
Kumo adjunta una lista determinista de **fuentes recuperadas** con archivo,
página/líneas, extracto y, con `--verbose`, similitud. Esa lista no implica que
el modelo haya usado cada fuente; la validación de números no demuestra que
cada afirmación esté realmente respaldada. Revisa siempre el borrador.

El cuerpo sigue el idioma de la instrucción, salvo `--language` explícito.
`--language English` también cambia el encabezado de fuentes y el aviso del
Markdown al inglés. La interfaz de consola sigue en español. El resultado
completo sale por stdout y opcionalmente con `--output`, siempre sin sobrescribir.

### Decisiones de diseño de F5

- `StructuredLlmClient` extiende `LlmClient` sin cambiar su contrato central;
  `OllamaLlmClient` implementa el envío de `format` y mantiene el filtrado de
  razonamiento. No se añade ningún framework ni dependencia nueva.
- `BriefTemplate` es un Value Object; `BriefTemplateManager` persiste la
  selección editable; `BriefSchema` construye y valida la estructura concreta.
- `BriefService` y `WriteService` son Facades con dependencias inyectadas por
  constructor. `ConsoleSummaryListener` reutiliza el Observer de F4 para progreso.
- `TaskOutputWriter` adapta la escritura segura de F4 a JSON y Markdown; el
  nombre del archivo o una instrucción del LLM nunca autoriza a sobrescribirlo.
- La reducción para `brief` añade foco de extracción a los prompts de resumen,
  sin alterar el comportamiento predeterminado de `summarize`.

## Compilar y probar

```powershell
.\mvnw.cmd -v
.\mvnw.cmd test
```

En Linux/macOS:

```bash
./mvnw test
```

Las pruebas **no necesitan Ollama**: usan `FakeEmbedder`, `EchoLlmClient`,
`FakeOllamaClient` y `FakeOllamaServer`, un servidor HTTP local real sobre
loopback. La suite actual tiene 300 pruebas, incluyendo selección de estrategia,
límites del presupuesto, reducción de múltiples etapas, cobertura de fragmentos,
idiomas, código sin ejecución, plantillas editables, JSON estructurado y validado,
tareas con evidencia, redacción con fuentes y guardado sin sobrescribir.
También se probaron manualmente `brief` y `write` con `qwen3.5:4b` y el archivo
`prueba.txt` del entorno local. No es una evaluación general de calidad: para
documentos largos, artículos y código debes revisar resultados contra la fuente.

## Estructura

```
src/main/java/dev/abyssace/kumo/
  domain/    records inmutables (Document, Chunk, Answer, Citation, ...)
  loader/    DocumentLoader + loaders TXT/MD/PDF y su Factory
  chunk/     TokenEstimator, FixedSizeChunker y SummaryChunker (Strategy)
  embed/     Embedder (Strategy)
  store/     VectorStore, SimilarityMetric, InMemoryVectorStore, JsonVectorStore
  llm/       LlmClient (Port), cliente HTTP de Ollama, filtros de razonamiento
  prompt/    PromptBuilder, RagPromptBuilder y SummaryPromptBuilder
  summarize/ SummaryService (Facade), Stuff/MapReduce (Strategy), progreso (Observer)
  task/      BriefService, BriefSchema, WriteService y PromptBudget
  rag/       RagService, ChunkIndexer, IndexingService, StatusService (Facade)
  config/    AppConfig (Builder), ConfigManager, StoreFactory (Factory)
  render/    ConsoleRenderer y observador de progreso
  cli/       Picocli: Main, comandos y mixins
  i18n/      Messages (textos de interfaz)
```

`examples/brief-template.json` contiene una plantilla completa editable.

## Índice en disco

Cada corpus guarda su índice en:

```
<carpeta-del-corpus>/.kumo/index.json
```

El archivo es JSON legible, escrito con Jackson e indentado. Su estructura es:

```json
{
  "formatVersion": 1,
  "embeddingModel": "embeddinggemma",
  "dimensions": 768,
  "chunks": [
    {
      "id": "/ruta/al/documento.pdf#0",
      "source": "/ruta/al/documento.pdf",
      "index": 0,
      "text": "contenido del fragmento…",
      "location": { "type": "page", "page": 3 },
      "vector": [0.0123, -0.0456, …]
    }
  ]
}
```

- `formatVersion` permite evolucionar el esquema sin romper índices viejos.
- `embeddingModel` y `dimensions` detectan mezclas de modelos incompatibles.
- `location` es un objeto con `type` explícito: `page` (PDF), `lines` (TXT/MD)
  o `none`.
- La escritura es atómica: se escribe un `.tmp` y se renombra, para no dejar un
  índice a medias si el proceso se interrumpe.
- `.kumo/` está en `.gitignore`: los índices nunca se versionan.

**Si cambias el modelo de embeddings**, kumo se niega a abrir el índice y pide
reindexar. Borra la carpeta `.kumo/` del corpus y vuelve a indexar. Es
deliberado: vectores de modelos distintos no son comparables y mezclarlos daría
resultados sin sentido.

## Cómo habla con Ollama

kumo escribe el cliente HTTP a mano con `java.net.http.HttpClient`; no usa
LangChain4j, Spring AI ni ningún framework de IA. Solo llama a:

- `GET /api/tags` para saber qué modelos hay instalados.
- `POST /api/show` para leer el contexto real del modelo de embeddings.
- `POST /api/embed` para vectorizar en lotes.
- `POST /api/chat` para responder (con `stream: true` en `ask`).

El chat envía `think: false` en el nivel superior para desactivar el
razonamiento de Qwen3. Como red de seguridad, las etiquetas de razonamiento se
eliminan también del texto, incluso si llegan partidas entre tokens.

La URL base se valida como loopback y sin ruta, query ni fragmento: kumo no
puede apuntar a un servidor remoto por accidente.

## Decisiones de diseño (F1)

- **Todo depende de interfaces** (`Embedder`, `VectorStore`, `LlmClient`,
  `PromptBuilder`): los tests sustituyen Ollama por dobles deterministas y el
  store se cambia con una línea de configuración (`vectorStore=MEMORY`).
- **Records inmutables con copia defensiva** de los `float[]`, para que el
  dominio no se pueda modificar por accidente desde el store o el render.
- **`SourceLocation` sellado** (`PageRef`, `LineRange`, `None`): el dominio
  expresa la ubicación exacta que cada formato permite citar, sin obligar a los
  PDF a fingir líneas ni a los TXT a fingir páginas.
- **Configuración centralizada** en `AppConfig`: modelos, `num_ctx`, top-k,
  umbral y tamaño de fragmento no están hardcodeados en el resto del código.
  La URL de Ollama se valida como loopback para garantizar que nada salga de la
  máquina.
- **`size()` en `VectorStore`**: pequeña extensión sobre el contrato mínimo,
  necesaria para el comando `status`.

## Decisiones de diseño (F2)

- **Template Method en los loaders**: `AbstractDocumentLoader` fija el
  esqueleto (validar ruta, extraer páginas, construir `Document`) y cada formato
  solo decide extensiones y extracción. Añadir DOCX será un loader nuevo, no un
  cambio en los existentes.
- **PDF page-aware**: se extrae página a página y se conserva el número real.
  Las páginas en blanco se omiten sin renumerar las siguientes, así una cita a
  «página 3» sigue siendo correcta.
- **Chunker por tokens, no por caracteres**: `TokenEstimator` aísla la
  heurística (`chars / 4`, redondeo hacia arriba). El chunker corta en límites
  de palabra y aplica solapamiento por cola de palabras, sin generar fragmentos
  compuestos solo por texto repetido.
- **Tamaño validado contra el contexto del embedder**: `AppConfig` rechaza un
  `chunkSizeTokens` que no quepa con margen en `embeddingContextTokens`. El
  default es 400 sobre 2048, muy por debajo del límite real del modelo.
- **`JsonVectorStore` como Decorator**: delega la búsqueda en
  `InMemoryVectorStore` y solo añade carga/guardado. La lógica de similitud y
  de reemplazo por `id` estable se escribe una vez.
- **El dominio no conoce Jackson**: `IndexFile` y sus records anidados hacen de
  DTO y mapper. Si mañana se añade un store binario, el dominio no cambia.

## Decisiones de diseño (F3)

- **Puerto y adaptador para Ollama**: `OllamaClient` es un puerto interno y
  `HttpOllamaClient` el adaptador. Los comandos piden el puerto; las pruebas
  inyectan un doble sin red.
- **`CliFactory` como único punto de construcción**: los comandos no crean
  clientes HTTP ni stores. Eso mantiene la CLI delgada y permite probarla
  entera con `FakeOllamaServer`.
- **Streaming en `ask`**: `LlmClient.stream` entrega tokens según llegan.
  `ReasoningFilter` es una máquina de estados que tolera etiquetas partidas
  entre tokens.
- **Validación del contexto de embeddings**: antes de indexar se consulta
  `/api/show` y se busca una clave terminada en `.context_length`. Si el
  fragmento no cabe, se aborta con un mensaje que dice qué cambiar.
- **Reindexado por archivo**: `ChunkIndexer.replace` sustituye solo los
  fragmentos del archivo indicado. Reindexar una carpeta no borra documentos
  que no se hayan vuelto a pasar.
- **Errores separados por alcance**: un archivo ilegible no aborta el lote; una
  caída de Ollama o un índice corrupto sí, porque afectan a todo.
- **`status` tolerante**: informa incluso si Ollama está caído, el índice es de
  otro modelo o la carpeta no existe. Es la herramienta para diagnosticar.

## Limitaciones conocidas

- **Solo UTF-8** en TXT/Markdown. Un archivo en otra codificación puede leerse
  con caracteres extraños.
- **PDF escaneados no soportados**: si no hay texto extraíble, el loader falla
  con un mensaje claro. El OCR queda fuera del alcance del proyecto.
- **El chunker no entiende de estructura**: Markdown se trata como texto plano
  y los encabezados no fuerzan cortes.
- **Rutas absolutas en el índice**: el `source` guardado es absoluto. Mover el
  corpus de carpeta obliga a reindexar; es el precio de tener citas
  inequívocas.
- **Sin indexado incremental por fecha**: `add` reindexa los archivos que le
  pasas, no decide por sí solo qué ha cambiado. Ese extra queda para F7.
- **El índice guarda el vector completo en JSON**: legible y depurable, pero
  más pesado que un formato binario. La interfaz `VectorStore` deja la puerta
  abierta a añadirlo después.
- **Una pregunta por invocación**: `ask` no mantiene conversación. El chat
  interactivo es un extra de F7.

## Versiones

Verificadas en Maven Central con al menos dos semanas de antigüedad:
Jackson BOM `2.22.4`, JUnit BOM `6.1.3`, PDFBox `3.0.8`, Picocli `4.7.7`,
Surefire `3.6.0`, Compiler `3.16.0`, Jar `3.5.1`, Maven `3.10.0` y Wrapper
`3.3.4`.

## Licencia

MIT. Ver `LICENSE`.
