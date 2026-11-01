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
| F3 | Cliente de Ollama y comandos `init`, `add`, `ask`, `status` | ⏳ |
| F4 | Resumen Stuff + MapReduce con progreso | ⏳ |
| F5 | `brief` (JSON estructurado) y `write` (Markdown) | ⏳ |
| F6 | Instaladores, README final y release con `.jar` | ⏳ |
| F7 | Extras opcionales (chat, indexado incremental, web local) | ⏳ |

## Requisitos

- **JDK 21** con `JAVA_HOME` apuntando a él.
- **Maven Wrapper** incluido en el repo: no hace falta tener Maven instalado.
- **Ollama nativo** (a partir de F3), escuchando en `http://localhost:11434`.

> En Windows, si `java -version` muestra 17, abre una terminal nueva o revisa
> `JAVA_HOME`. La sesión actual puede conservar el entorno anterior.

## Compilar y probar

```powershell
.\mvnw.cmd -v
.\mvnw.cmd test
```

En Linux/macOS:

```bash
./mvnw test
```

Las pruebas no necesitan Ollama: usan `FakeEmbedder`, `EchoLlmClient` y un PDF
generado en el propio test con PDFBox. La suite actual tiene 89 pruebas.

## Estructura

```
src/main/java/dev/abyssace/kumo/
  domain/    records inmutables (Document, Chunk, Answer, Citation, ...)
  loader/    DocumentLoader + loaders TXT/MD/PDF y su Factory
  chunk/     TokenEstimator, FixedSizeChunker (Strategy)
  embed/     Embedder (Strategy)
  store/     VectorStore, SimilarityMetric, InMemoryVectorStore, JsonVectorStore
  llm/       LlmClient (Port)
  prompt/    PromptBuilder + RagPromptBuilder
  rag/       RagService (Facade)
  config/    AppConfig (Builder), StoreFactory (Factory)
  i18n/      Messages (textos de interfaz)
```

`task/`, `render/`, `cli/` y `summarize/` se incorporan en las fases siguientes.

## Índice en disco (F2)

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
  necesaria para el comando `status` de F3.

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

## Limitaciones conocidas (F2)

- **Solo UTF-8** en TXT/Markdown. Un archivo en otra codificación puede leerse
  con caracteres extraños; se detectará y se documentará mejor en F3.
- **PDF escaneados no soportados**: si no hay texto extraíble, el loader falla
  con un mensaje claro. El OCR queda fuera del alcance del proyecto.
- **El chunker no entiende de estructura**: Markdown se trata como texto plano
  y los encabezados no fuerzan cortes. Es suficiente para RAG, pero se puede
  mejorar más adelante.
- **Rutas absolutas en el índice**: el `source` guardado es absoluto. Mover el
  corpus de carpeta obliga a reindexar; es el precio de tener citas
  inequívocas.
- **Sin indexado incremental todavía**: F2 sabe guardar y recargar, pero el
  comando que decide qué archivos han cambiado llega en F3.
- **El índice guarda el vector completo en JSON**: legible y depurable, pero
  más pesado que un formato binario. La interfaz `VectorStore` deja la puerta
  abierta a añadirlo después.

## Versiones

Verificadas en Maven Central con al menos dos semanas de antigüedad:
Jackson BOM `2.22.4`, JUnit BOM `6.1.3`, PDFBox `3.0.8`, Picocli `4.7.7`,
Surefire `3.6.0`, Compiler `3.16.0`, Jar `3.5.1`, Maven `3.10.0` y Wrapper
`3.3.4`.

## Licencia

MIT. Ver `LICENSE`.
