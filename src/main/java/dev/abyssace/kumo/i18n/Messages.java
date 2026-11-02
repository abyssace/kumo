package dev.abyssace.kumo.i18n;

/**
 * Patrón: Constant Holder.
 *
 * <p>Todos los textos visibles para el usuario viven aquí (en español) para
 * poder revisarlos y traducirlos en un solo sitio. Los prompts del modelo no
 * cuentan como mensajes de interfaz: viven en {@code prompt}.</p>
 */
public final class Messages {

    // --- Respuestas RAG -----------------------------------------------------

    public static final String NO_CONTEXT =
            "No encontré fragmentos relevantes en el índice para responder a esa pregunta.";
    public static final String SOURCES_HEADER = "Fuentes:";
    public static final String SOURCE_LINE = "  [%d] %s%s";
    public static final String SOURCE_EXCERPT = "      \"%s\"";
    public static final String SCORE_SUFFIX = " | similitud %.2f";
    public static final String QUESTION_EMPTY = "La pregunta no puede estar vacía.";
    public static final String EMPTY_ANSWER = "El modelo no devolvió texto.";

    // --- Configuración ------------------------------------------------------

    public static final String CONFIG_INVALID = "No se pudo leer la configuración %s: %s";
    public static final String CONFIG_EMPTY = "El archivo de configuración %s está vacío.";
    public static final String CONFIG_SAVE_ERROR = "No se pudo guardar la configuración %s: %s";
    public static final String CONFIG_HOME_UNKNOWN =
            "No se pudo determinar la carpeta personal del usuario. Define la variable KUMO_HOME.";
    public static final String CONFIG_CREATED = "Configuración creada en %s";
    public static final String CONFIG_FOUND = "Configuración: %s";

    // --- Ollama -------------------------------------------------------------

    public static final String OLLAMA_CONNECTED = "Ollama: conectado en %s";
    public static final String OLLAMA_UNAVAILABLE =
            "No se pudo conectar con Ollama en %s. ¿Está instalado y corriendo? Prueba `ollama serve`.";
    public static final String OLLAMA_HTTP_ERROR = "Ollama respondió %d en %s: %s";
    public static final String OLLAMA_BAD_RESPONSE = "Ollama devolvió una respuesta inesperada en %s: %s";
    public static final String OLLAMA_INVALID_BASE_URL =
            "URL de Ollama inválida o no local: %s (kumo solo habla con localhost).";
    public static final String OLLAMA_REQUEST_INTERRUPTED = "La petición a Ollama fue interrumpida.";
    public static final String OLLAMA_EMPTY_EMBEDDINGS =
            "El modelo de embeddings %s devolvió un número inesperado de vectores para %d texto(s).";
    public static final String OLLAMA_EMBEDDING_DIMENSIONS =
            "El modelo de embeddings %s devolvió vectores de dimensiones distintas (%d y %d).";
    public static final String OLLAMA_MODEL_LINE = "Modelo %s: %s";
    public static final String OLLAMA_MODEL_CONTEXT = "%s: contexto %d tokens";
    public static final String OLLAMA_MODEL_NOT_INSTALLED =
            "El modelo %s no está instalado. Descárgalo con: ollama pull %s";
    public static final String OLLAMA_MODEL_INSTALLED = "instalado";
    public static final String OLLAMA_MODEL_MISSING = "no instalado";
    public static final String OLLAMA_CONTEXT = "contexto %d tokens";
    public static final String OLLAMA_CONTEXT_UNKNOWN = "contexto desconocido";
    public static final String OLLAMA_MODEL_HINT = "Descárgalo con: ollama pull %s";
    public static final String OLLAMA_CONTEXT_TOO_SMALL =
            "El modelo %s tiene un contexto de %d tokens y el fragmento configurado es de %d. "
                    + "Baja chunkSizeTokens en %s o usa un modelo con más contexto.";

    // --- Índice -------------------------------------------------------------

    public static final String INDEX_MISSING =
            "Índice: no creado todavía en %s. Ejecuta `kumo add <ruta>`.";
    public static final String INDEX_STATUS = "Índice: %s (%d fragmentos, %d dimensiones, modelo %s)";
    public static final String INDEX_EMPTY =
            "El índice está vacío. Ejecuta `kumo add <ruta>` para indexar documentos.";
    public static final String INDEX_READ_ERROR = "No se pudo leer el índice %s: %s";

    // --- Indexado -----------------------------------------------------------

    public static final String INDEXING_START = "Indexando %d archivo(s) en %s";
    public static final String INDEXING_FILE = "[%d/%d] %s → %d fragmento(s)";
    public static final String INDEXING_FAILED = "[%d/%d] %s ✗ %s";
    public static final String INDEXING_SUMMARY = "Listo: %d archivo(s), %d fragmento(s), %d error(es).";
    public static final String NO_SUPPORTED_FILES =
            "No encontré archivos compatibles (%s) en %s.";
    public static final String UNSUPPORTED_FILE =
            "Formato no soportado: %s. Extensiones disponibles: %s";
    public static final String EMPTY_DOCUMENT = "El documento no contiene texto indexable.";
    public static final String FILE_NOT_FOUND = "No existe el archivo o la carpeta: %s";

    // --- Estado -------------------------------------------------------------

    public static final String STATUS_CONFIG = "Configuración : %s";
    public static final String STATUS_CORPUS = "Corpus        : %s";
    public static final String STATUS_INDEX = "Índice        : %s";
    public static final String STATUS_OLLAMA = "Ollama        : %s";
    public static final String STATUS_CHAT = "Modelo chat   : %s";
    public static final String STATUS_EMBEDDING = "Embeddings    : %s";
    public static final String STATUS_REACHABLE = "conectado (%s)";
    public static final String STATUS_UNREACHABLE = "no disponible (%s)";
    public static final String STATUS_INDEX_EMPTY = "no creado todavía (ejecuta `kumo add <ruta>`)";
    public static final String STATUS_INDEX_CORRUPT = "ilegible (%s)";
    public static final String STATUS_INDEX_DETAILS = "%d fragmentos, %d dimensiones, modelo %s";
    public static final String STATUS_INSTALLED = "instalado";
    public static final String STATUS_MISSING = "no instalado";
    public static final String STATUS_UNKNOWN = "no comprobado (Ollama no disponible)";
    public static final String STATUS_CONTEXT = "contexto %d tokens";
    public static final String STATUS_CONTEXT_UNKNOWN = "contexto desconocido";
    public static final String STATUS_EMBEDDING_CONTEXT = "instalado, contexto %d tokens";
    public static final String STATUS_EMBEDDING_UNKNOWN = "instalado, contexto desconocido";

    // --- CLI ----------------------------------------------------------------

    public static final String CLI_DESCRIPTION =
            "Asistente de documentos totalmente local. Indexa tus archivos y pregunta sobre ellos sin enviar nada a la nube.";
    public static final String CLI_VERSION = "kumo %s";
    public static final String CLI_DEV_VERSION = "dev";
    public static final String CLI_INIT_CREATED = "Configuración creada en %s";
    public static final String CLI_INIT_FOUND = "Configuración: %s";
    public static final String CLI_INIT_HINT =
            "Cuando Ollama esté listo, ejecuta `kumo add <ruta>` y luego `kumo ask \"tu pregunta\"`.";
    public static final String CLI_INIT_MODELS_HINT =
            "Descarga los modelos que falten con `ollama pull <modelo>`.";
    public static final String CLI_ERROR = "Error: %s";
    public static final String CLI_UNEXPECTED_ERROR = "Error inesperado: %s";
    public static final String CLI_UNKNOWN_ARGUMENT = "Opción o argumento no reconocido: %s";
    public static final String CLI_MISSING_PARAMETER = "Faltan parámetros obligatorios: %s";
    public static final String CLI_INVALID_VALUE = "Valor inválido: %s";
    public static final String CLI_INVALID_ARGUMENTS = "Argumentos inválidos: %s";
    public static final String CLI_OPT_HELP = "Muestra esta ayuda y sale.";
    public static final String CLI_OPT_VERSION = "Muestra la versión y sale.";
    public static final String CLI_USAGE_HEADING = "Uso: ";
    public static final String CLI_OPTIONS_HEADING = "Opciones: ";
    public static final String CLI_PARAMETERS_HEADING = "Parámetros: ";
    public static final String CLI_COMMANDS_HEADING = "Comandos: ";
    public static final String CLI_DESCRIPTION_HEADING = "Descripción: ";
    public static final String CLI_ADD_SUMMARY = "Índice actualizado: %s";
    public static final String CLI_ADD_NO_FILES = "No hay archivos para indexar.";
    public static final String CLI_OPT_CORPUS =
            "Carpeta del corpus (por defecto: el directorio actual).";
    public static final String CLI_OPT_VERBOSE =
            "Muestra rutas completas, puntajes y detalles.";
    public static final String CLI_OPT_TOP_K =
            "Número de fragmentos a recuperar (por defecto: el de la configuración).";
    public static final String CLI_OPT_PATHS = "Archivos o carpetas a indexar.";
    public static final String CLI_OPT_QUESTION = "Pregunta en lenguaje natural.";
    public static final String CLI_CMD_INIT = "Crea la configuración global y comprueba Ollama.";
    public static final String CLI_CMD_ADD = "Indexa archivos o carpetas en el corpus.";
    public static final String CLI_CMD_ASK = "Haz una pregunta sobre el corpus indexado.";
    public static final String CLI_CMD_STATUS = "Muestra la configuración, Ollama y el estado del índice.";
    public static final String CORPUS_NOT_FOUND = "No existe la carpeta del corpus: %s";
    public static final String TOP_K_INVALID = "topK debe ser >= 1, pero fue %d";

    // --- Resúmenes -----------------------------------------------------------
    public static final String CLI_CMD_SUMMARIZE = "Resume un archivo directamente, sin índice.";
    public static final String SUMMARY_FILE = "Archivo TXT, Markdown, PDF o código UTF-8 (no se ejecuta).";
    public static final String SUMMARY_STRATEGY_OPTION = "Estrategia: auto, stuff o map-reduce (por defecto: auto).";
    public static final String SUMMARY_LANGUAGE_OPTION = "Idioma de salida: español/es/spanish o inglés/en/english; por defecto, el del documento.";
    public static final String SUMMARY_OUTPUT_OPTION = "Guarda el Markdown sin sobrescribir archivos existentes.";
    public static final String SUMMARY_BAD_LANGUAGE = "Idioma no admitido: %s. Usa español/es/spanish o inglés/en/english.";
    public static final String SUMMARY_BAD_STRATEGY = "Estrategia no admitida: %s. Usa auto, stuff o map-reduce.";
    public static final String SUMMARY_SMALL_CONTEXT = "El contexto no deja espacio suficiente para el prompt y la respuesta; aumenta numCtx o reduce summaryInputRatio.";
    public static final String SUMMARY_TOO_LARGE = "El texto supera el presupuesto estimado (%d tokens). Usa auto o map-reduce; no se ha truncado.";
    public static final String SUMMARY_NO_CONTRACTION = "La reducción no disminuyó el contenido; se detiene sin descartar fragmentos. Prueba otro modelo o aumenta numCtx.";
    public static final String SUMMARY_EMPTY = "El modelo devolvió un resumen vacío; no se ha guardado ningún resultado.";
    public static final String SUMMARY_READ = "Lectura: %s";
    public static final String SUMMARY_CHOICE = "Estrategia: %s — %s (entrada: ~%d tokens; presupuesto: %d).";
    public static final String SUMMARY_FITS = "el documento cabe con margen";
    public static final String SUMMARY_EXCEEDS = "el documento excede el presupuesto";
    public static final String SUMMARY_FORCED = "selección explícita del usuario";
    public static final String SUMMARY_STAGE = "%s: [%d/%d]";
    public static final String SUMMARY_SPLIT = "División";
    public static final String SUMMARY_MAP = "Resúmenes parciales";
    public static final String SUMMARY_REDUCE = "Reducción (etapa %d)";
    public static final String SUMMARY_FINAL = "Resumen final";
    public static final String SUMMARY_DONE = "Resultado listo.";
    public static final String SUMMARY_CODE_WARNING = "Código: solo se analiza este archivo como texto, sin ejecutarlo ni conocer el proyecto completo.";
    public static final String SUMMARY_OUTPUT_EXISTS = "La salida ya existe; elige otro archivo: %s";
    public static final String SUMMARY_OUTPUT_ERROR = "No se pudo guardar el resumen en %s: %s";
    public static final String SUMMARY_OUTPUT_PARENT = "La carpeta de destino no existe.";
    public static final String SUMMARY_RATIO_INVALID = "summaryInputRatio debe ser mayor que 0 y menor que 1.";
    public static final String SUMMARY_BUDGET_INVALID = "El presupuesto debe ser positivo.";
    public static final String SUMMARY_SPLIT_ERROR = "El presupuesto no permite dividir el texto.";

    // --- Fichas y redacción --------------------------------------------------
    public static final String CLI_CMD_BRIEF = "Extrae una ficha JSON de un archivo o carpeta, sin índice.";
    public static final String CLI_CMD_WRITE = "Redacta Markdown con fuentes del índice (cobertura parcial).";
    public static final String BRIEF_PATH = "Archivo o carpeta para extraer fichas independientes.";
    public static final String BRIEF_TEMPLATE_OPTION = "Plantilla JSON editable; por defecto, brief-template.json en la carpeta global.";
    public static final String BRIEF_TEMPLATE_PATH = "Plantilla: %s";
    public static final String BRIEF_TEMPLATE_INVALID = "Plantilla de brief inválida en %s: %s";
    public static final String BRIEF_TEMPLATE_FIELDS = "Usa campos conocidos, sin duplicados, e incluye title y summary.";
    public static final String BRIEF_TEMPLATE_INSTRUCTIONS = "instructions debe ser texto (máximo 4000 caracteres).";
    public static final String BRIEF_TEMPLATE_PROPERTIES = "La plantilla debe contener solo fields e instructions.";
    public static final String BRIEF_TEMPLATE_MISSING = "No existe la plantilla indicada: %s";
    public static final String BRIEF_NO_FILES = "No hay archivos compatibles para brief en %s.";
    public static final String DOCUMENT_DISCOVERY_ERROR = "No se pudo recorrer la carpeta %s: %s";
    public static final String BRIEF_FILE = "Ficha [%d/%d]: %s";
    public static final String BRIEF_COMPRESSION = "Documento extenso: se prepara una síntesis intermedia; puede perder matices. Revisa la fuente.";
    public static final String BRIEF_EXTRACT = "Extracción JSON";
    public static final String BRIEF_VALIDATE = "Validación JSON";
    public static final String BRIEF_JSON_INVALID = "Ollama no devolvió una ficha JSON válida: %s";
    public static final String BRIEF_SCHEMA_ERROR = "El valor %s no cumple el esquema de la ficha.";
    public static final String BRIEF_EVIDENCE_ERROR = "La tarea contiene una evidencia, responsable o fecha que no aparece literalmente en el documento.";
    public static final String BRIEF_WARNING = "Ficha generada por un modelo: verifica los datos contra el documento original.";
    public static final String TASK_CONTEXT_ERROR = "El contenido, las instrucciones y el esquema no caben con margen en numCtx. Aumenta numCtx o reduce el contenido/top-k; no se ha truncado.";
    public static final String TASK_OUTPUT_ERROR = "No se pudo guardar la salida en %s: %s";
    public static final String TASK_OUTPUT_OPTION = "Guarda la salida completa en un archivo nuevo, sin sobrescribir.";
    public static final String WRITE_INSTRUCTION = "Instrucción de redacción, por ejemplo: redacta una introducción académica.";
    public static final String WRITE_LANGUAGE_OPTION = "Idioma de salida: español/es/spanish o inglés/en/english; por defecto, el de la instrucción.";
    public static final String WRITE_TOP_K_OPTION = "Fragmentos a recuperar (por defecto: writeTopK de la configuración, inicialmente 12).";
    public static final String WRITE_TOP_K_INVALID = "writeTopK debe ser >= 1.";
    public static final String WRITE_COVERAGE = "Cobertura parcial: se usan hasta %d fragmentos recuperados, no todo el corpus. No se garantiza una síntesis exhaustiva.";
    public static final String WRITE_RETRIEVE = "Recuperación de fuentes";
    public static final String WRITE_GENERATE = "Redacción Markdown";
    public static final String WRITE_NO_CONTEXT = "No hay fragmentos relevantes para redactar. Revisa el índice o minScore.";
    public static final String WRITE_EMPTY = "El modelo devolvió una redacción vacía.";
    public static final String WRITE_INVALID_CITATION = "La redacción no incluye citas numéricas válidas a los fragmentos recuperados, o cita fuentes inexistentes.";
    public static final String WRITE_SOURCES = "## Fuentes recuperadas";
    public static final String WRITE_SOURCES_EN = "## Retrieved sources";
    public static final String WRITE_NOTICE_EN = "> Partial coverage: this draft uses retrieved excerpts, not the entire corpus.";

    private Messages() {
        // Clase de constantes.
    }
}
