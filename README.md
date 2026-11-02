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
| F4 | Resumen Stuff + MapReduce con progreso | ⏳ |
| F5 | `brief` (JSON estructurado) y `write` (Markdown) | ⏳ |
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

Opciones comunes: `-c, --corpus <carpeta>` (por defecto `.`) y `-v, --verbose`
(añade la similitud de cada fragmento). `ask` acepta además `-k, --top-k`.

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
  "vectorStore": "JSON"
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
loopback. La suite actual tiene 214 pruebas.

## Estructura

```
src/main/java/dev/abyssace/kumo/
  domain/    records inmutables (Document, Chunk, Answer, Citation, ...)
  loader/    DocumentLoader + loaders TXT/MD/PDF y su Factory
  chunk/     TokenEstimator, FixedSizeChunker (Strategy)
  embed/     Embedder (Strategy)
  store/     VectorStore, SimilarityMetric, InMemoryVectorStore, JsonVectorStore
  llm/       LlmClient (Port), cliente HTTP de Ollama, filtros de razonamiento
  prompt/    PromptBuilder + RagPromptBuilder
  rag/       RagService, ChunkIndexer, IndexingService, StatusService (Facade)
  config/    AppConfig (Builder), ConfigManager, StoreFactory (Factory)
  render/    ConsoleRenderer y observador de progreso
  cli/       Picocli: Main, comandos y mixins
  i18n/      Messages (textos de interfaz)
```

`task/` y `summarize/` se incorporan en las fases siguientes.

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
