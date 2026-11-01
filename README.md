# kumo

Asistente de documentos **100 % local** por línea de comandos. Indexa tus
archivos, responde preguntas con RAG y cita las fuentes. Todo ocurre en tu
máquina: kumo solo habla con Ollama en `localhost`, sin telemetría ni servicios
en la nube.

## Estado del proyecto

| Fase | Contenido | Estado |
|------|-----------|--------|
| F1 | Núcleo sin Ollama: dominio, interfaces, store en memoria, `RagService`, pruebas | ✅ |
| F2 | Loaders TXT/MD/PDF, chunker con solapamiento y persistencia en disco | ⏳ |
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

Las pruebas no necesitan Ollama: usan `FakeEmbedder` y `EchoLlmClient`.

## Estructura

```
src/main/java/dev/abyssace/kumo/
  domain/    records inmutables (Document, Chunk, Answer, Citation, ...)
  loader/    DocumentLoader (Strategy)
  chunk/     Chunker (Strategy)
  embed/     Embedder (Strategy)
  store/     VectorStore, SimilarityMetric, InMemoryVectorStore
  llm/       LlmClient (Port)
  prompt/    PromptBuilder + RagPromptBuilder
  rag/       RagService (Facade)
  config/    AppConfig (Builder), StoreFactory (Factory)
  i18n/      Messages (textos de interfaz)
```

`task/`, `render/`, `cli/` y `summarize/` se incorporan en las fases siguientes.

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

## Versiones

Verificadas en Maven Central con al menos dos semanas de antigüedad:
Jackson BOM `2.22.4`, JUnit BOM `6.1.3`, PDFBox `3.0.8`, Picocli `4.7.7`,
Surefire `3.6.0`, Compiler `3.16.0`, Jar `3.5.1`, Maven `3.10.0` y Wrapper
`3.3.4`.

## Licencia

MIT. Ver `LICENSE`.
