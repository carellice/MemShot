package com.memshot.app.analysis

/**
 * Categorie automatiche. Ogni categoria ha parole chiave pesate (italiano + inglese),
 * app di provenienza tipiche ed etichette visive di ML Kit che la suggeriscono.
 * Una parola che finisce con `*` vale anche come prefisso (es. "ricett*" → ricetta, ricette).
 */
data class Category(
    val id: String,
    val label: String,
    val color: Long,
    val keywords: Map<String, Float> = emptyMap(),
    val apps: List<String> = emptyList(),
    val imageLabels: Map<String, Float> = emptyMap(),
)

object Categories {
    const val PHOTO = "photo"
    const val OTHER = "other"

    val all: List<Category> = listOf(
        Category(
            id = "film", label = "Film & Serie", color = 0xFFE5484D,
            keywords = w(3f, "film", "serie tv", "stagione", "episodio", "episodi", "trailer", "regia", "regista", "netflix",
                "prime video", "disney+", "imdb", "letterboxd", "justwatch", "raiplay", "mediasetinfinity", "now tv", "apple tv",
                "movie", "season", "episode", "director", "cast", "sottotitoli", "guarda ora", "watch now", "rotten tomatoes",
                "al cinema", "cinema", "box office", "documentario", "anime", "sinossi", "trama") +
                w(1.5f, "attori", "attore", "attrice", "starring", "thriller", "horror", "commedia", "drammatico", "fantascienza",
                    "sci-fi", "azione", "riprendi", "continua a guardare", "la mia lista", "my list", "streaming", "puntata", "uscita"),
            apps = listOf("netflix", "primevideo", "prime video", "disney", "imdb", "letterboxd", "justwatch", "raiplay",
                "mediaset", "nowtv", "infinity", "crunchyroll", "tv time", "hbo", "paramount", "sky go", "tvtime"),
        ),
        Category(
            id = "prompt", label = "Prompt AI", color = 0xFF8E4EC6,
            keywords = w(3.5f, "prompt", "act as", "agisci come", "comportati come", "you are an", "you are a", "sei un esperto",
                "sei un", "immagina di essere", "chatgpt", "gpt-4", "gpt-5", "gpt4", "openai", "claude", "anthropic", "gemini",
                "midjourney", "dall-e", "dalle", "stable diffusion", "copilot", "llm", "perplexity", "negative prompt",
                "system prompt", "--ar", "--v", "--style", "step by step", "passo dopo passo", "intelligenza artificiale") +
                w(1.5f, "rispondi", "respond", "genera", "generate", "scrivi un", "scrivi una", "write a", "create a", "crea un",
                    "crea una", "tono", "tone", "formato", "format", "istruzioni", "instructions", "contesto", "context", "ruolo",
                    "role", "obiettivo", "goal", "output", "task", "audience", "pubblico", "esempi", "examples", "riassumi",
                    "summarize", "spiega", "explain", "call to action", "hashtag", "parole chiave", "keywords", "elenco puntato", "bullet points", "cinematic", "photorealistic",
                    "hyperrealistic", "ultra detailed", "8k", "4k", "lighting", "illustration", "render"),
            apps = listOf("chatgpt", "openai", "claude", "gemini", "midjourney", "perplexity", "copilot", "poe", "mistral", "le chat"),
        ),
        Category(
            id = "code", label = "Codice", color = 0xFF0090FF,
            keywords = w(3f, "function", "const ", "return", "import ", "public class", "def ", "println", "console.log",
                "npm install", "pip install", "git ", "sudo ", "github", "stack overflow", "stackoverflow", "localhost",
                "exception", "traceback", "null pointer", "kotlin", "python", "javascript", "typescript", "java", "html",
                "css", "sql", "select * from", "api", "json", "bash", "terminal", "compile", "build failed") +
                w(1.5f, "var ", "val ", "let ", "async", "await", "error", "errore", "debug", "repository", "commit", "branch"),
            apps = listOf("github", "vscode", "termux", "android studio", "stack"),
        ),
        Category(
            id = "recipe", label = "Ricette", color = 0xFFF76B15,
            keywords = w(3.5f, "ricett*", "ingredienti", "ingredients", "preparazione", "procedimento", "recipe", "giallozafferano",
                "cucchiai*", "cucchiain*", "forno", "preriscaldat*", "impasto", "q.b.", "qb", "tbsp", "tsp") +
                w(1.5f, "farina", "zucchero", "uova", "burro", "olio", "sale", "pepe", "latte", "aglio", "cipolla", "pomodor*",
                    "pasta", "cuocere", "cottura", "mescola*", "frulla*", "minuti", "porzioni", "dosi", "flour", "sugar",
                    "butter", "eggs", "bake", "oven", "cups", "grammi", "gr ", "ml "),
            apps = listOf("giallozafferano", "cookist", "yummly", "tasty", "cookpad", "fatto in casa"),
            imageLabels = mapOf("food" to 2f, "cuisine" to 2f, "dish" to 2f, "baked goods" to 2f, "pizza" to 1.5f, "bread" to 1.5f),
        ),
        Category(
            id = "shopping", label = "Shopping", color = 0xFFD6409F,
            keywords = w(3f, "aggiungi al carrello", "add to cart", "carrello", "acquista ora", "buy now", "compra",
                "spedizione gratuita", "free shipping", "amazon", "zalando", "ebay", "vinted", "shein", "temu", "aliexpress",
                "subito.it", "ikea", "taglia", "size", "disponibilità", "in stock", "sconto", "offerta", "coupon", "prime") +
                w(1.5f, "prezzo", "price", "spedizione", "consegna", "recensioni", "reviews", "stelle", "colore", "modello",
                    "iva inclusa", "€", "$", "saldi", "wishlist", "ordine", "order"),
            apps = listOf("amazon", "zalando", "ebay", "vinted", "shein", "temu", "aliexpress", "subito", "ikea", "etsy", "wallapop"),
            imageLabels = mapOf("shoe" to 1f, "bag" to 1f, "jacket" to 1f, "jeans" to 1f, "sunglasses" to 1f),
        ),
        Category(
            id = "chat", label = "Chat", color = 0xFF30A46C,
            keywords = w(3f, "whatsapp", "telegram", "messenger", "online", "ultimo accesso", "last seen", "sta scrivendo",
                "typing", "scrivi un messaggio", "messaggio", "type a message", "inoltrato", "forwarded", "vocale",
                "letto", "consegnato", "delivered", "gruppo", "ieri", "oggi", "yesterday", "today"),
            apps = listOf("whatsapp", "telegram", "messenger", "signal", "messages", "messaggi", "discord", "slack", "teams"),
        ),
        Category(
            id = "music", label = "Musica", color = 0xFF1DB954,
            keywords = w(3f, "spotify", "apple music", "youtube music", "shazam", "soundcloud", "album", "brano", "brani",
                "playlist", "canzone", "song", "lyrics", "testo della canzone", "artista", "artist", "concerto", "tour",
                "ascolta", "listen", "ascoltatori mensili", "monthly listeners", "podcast", "feat.", "ft."),
            apps = listOf("spotify", "music", "shazam", "soundcloud", "deezer", "amazon music", "tidal", "musixmatch"),
            imageLabels = mapOf("musical instrument" to 1.5f, "concert" to 2f, "guitar" to 1.5f),
        ),
        Category(
            id = "books", label = "Libri", color = 0xFFA18072,
            keywords = w(3f, "libro", "libri", "book", "capitolo", "chapter", "kindle", "goodreads", "audible", "romanzo",
                "novel", "autore", "autrice", "author", "edizione", "editore", "pagine", "isbn", "da leggere", "to read",
                "saggio", "lettura", "storytel", "feltrinelli", "mondadori"),
            apps = listOf("kindle", "goodreads", "audible", "storytel", "google play libri", "books", "kobo"),
        ),
        Category(
            id = "travel", label = "Viaggi", color = 0xFF12A594,
            keywords = w(3.5f, "volo", "flight", "gate", "imbarco", "boarding", "check-in", "carta d'imbarco", "boarding pass",
                "ryanair", "easyjet", "ita airways", "wizz", "trenitalia", "italo", "frecciarossa", "binario", "platform",
                "booking.com", "airbnb", "hotel", "prenotazione", "reservation", "itinerario", "partenza", "arrivo",
                "departure", "arrival", "passaporto", "ostello", "check-out", "volo diretto", "andata", "ritorno"),
            apps = listOf("ryanair", "easyjet", "booking", "airbnb", "trenitalia", "italo", "skyscanner", "tripadvisor",
                "expedia", "wizz", "ita airways", "flixbus", "trainline", "hopper"),
            imageLabels = mapOf("beach" to 1.5f, "airplane" to 2f, "mountain" to 1f, "lake" to 1f, "sky" to 0.5f),
        ),
        Category(
            id = "events", label = "Eventi", color = 0xFFFFB224,
            keywords = w(3f, "biglietto", "biglietti", "ticket", "tickets", "evento", "event", "ticketone", "ticketmaster",
                "dice", "eventbrite", "save the date", "invito", "invitation", "appuntamento", "calendario", "calendar",
                "porte aperte", "ingresso", "posto", "settore", "fila", "sold out", "festival", "mostra", "spettacolo"),
            apps = listOf("ticketone", "ticketmaster", "dice", "eventbrite", "calendar", "calendario", "fever", "vivaticket"),
        ),
        Category(
            id = "finance", label = "Finanza", color = 0xFF3E63DD,
            keywords = w(3f, "iban", "bonifico", "saldo", "balance", "transazione", "transaction", "estratto conto",
                "pagamento", "payment", "fattura", "invoice", "ricevuta", "receipt", "paypal", "satispay", "revolut",
                "postepay", "intesa", "unicredit", "fineco", "n26", "hype", "conto corrente", "carta di credito",
                "importo", "amount", "scadenza", "bolletta", "f24", "rata", "mutuo", "investimenti", "azioni", "crypto",
                "bitcoin", "etf", "portafoglio", "totale"),
            apps = listOf("paypal", "satispay", "revolut", "postepay", "bancoposta", "intesa", "unicredit", "fineco", "n26",
                "hype", "bper", "bnl", "mediolanum", "isybank", "trade republic", "scalable", "coinbase", "binance"),
        ),
        Category(
            id = "places", label = "Luoghi", color = 0xFF46A758,
            keywords = w(3f, "google maps", "indicazioni", "directions", "percorso", "route", "km", "via ", "viale",
                "piazza", "corso ", "largo", "località", "cap", "aperto", "chiuso", "open now", "orari", "ristorante",
                "restaurant", "bar", "pizzeria", "trattoria", "osteria", "recensioni google", "come arrivare", "posizione"),
            apps = listOf("maps", "waze", "tripadvisor", "thefork", "the fork", "yelp", "citymapper", "moovit"),
            imageLabels = mapOf("building" to 0.5f, "skyscraper" to 1f, "monument" to 1.5f),
        ),
        Category(
            id = "social", label = "Social", color = 0xFFE54666,
            keywords = w(2.5f, "instagram", "tiktok", "facebook", "twitter", "threads", "reddit", "linkedin", "follower",
                "followers", "seguiti", "following", "mi piace", "likes", "commenti", "comments", "condividi", "share",
                "reel", "reels", "storia", "stories", "post", "segui", "follow", "retweet", "repost", "visualizzazioni",
                "views", "upvote", "subreddit", "r/", "@"),
            apps = listOf("instagram", "tiktok", "facebook", "twitter", "x", "threads", "reddit", "linkedin", "pinterest",
                "snapchat", "bereal", "bluesky", "tumblr", "youtube"),
        ),
        Category(
            id = "news", label = "Notizie", color = 0xFF8D8D8D,
            keywords = w(3f, "ansa", "corriere", "repubblica", "il post", "sole 24 ore", "la stampa", "fanpage",
                "tgcom", "skytg24", "bbc", "cnn", "reuters", "notizia", "notizie", "news", "breaking", "ultima ora",
                "articolo", "leggi anche", "redazione", "giornalista", "aggiornato", "governo", "ministro"),
            apps = listOf("news", "notizie", "ansa", "corriere", "repubblica", "il post", "google news", "flipboard", "feedly"),
        ),
        Category(
            id = "quotes", label = "Citazioni", color = 0xFFAB4ABA,
            keywords = w(3f, "citazione", "citazioni", "quote", "quotes", "aforisma", "aforismi", "frase", "frasi",
                "motivazione", "motivation", "ispirazione", "inspiration") + w(1f, "vita", "life", "amore", "love", "felicità", "sogni"),
        ),
        Category(
            id = "study", label = "Studio & Note", color = 0xFF5B5BD6,
            keywords = w(3f, "appunti", "notes", "lezione", "lecture", "esame", "exam", "università", "university",
                "definizione", "definition", "teorema", "esercizio", "exercise", "formula", "riassunto", "summary",
                "slide", "corso", "course", "wikipedia", "studio", "tesi", "domanda", "risposta", "quiz", "udemy", "coursera",
                "duolingo", "to-do", "todo", "lista", "promemoria", "reminder", "nota", "note"),
            apps = listOf("notion", "keep", "notes", "note", "obsidian", "evernote", "onenote", "wikipedia", "duolingo",
                "classroom", "coursera", "udemy", "anki", "goodnotes"),
        ),
        Category(
            id = "work", label = "Lavoro & Email", color = 0xFF6E56CF,
            keywords = w(3f, "oggetto:", "subject:", "da:", "from:", "a:", "to:", "cc:", "inviato", "sent", "rispondi a tutti",
                "reply all", "gmail", "outlook", "meeting", "riunione", "call", "zoom", "google meet", "deadline", "scadenza",
                "cliente", "client", "progetto", "project", "report", "colloquio", "candidatura", "curriculum", "offerta di lavoro",
                "job", "jira", "trello", "asana"),
            apps = listOf("gmail", "outlook", "mail", "teams", "slack", "zoom", "meet", "linkedin", "jira", "trello", "asana", "indeed"),
        ),
        Category(
            id = "health", label = "Salute & Fitness", color = 0xFFE93D82,
            keywords = w(3f, "allenamento", "workout", "esercizi", "serie", "ripetizioni", "reps", "kcal", "calorie",
                "calories", "passi", "steps", "battito", "heart rate", "sonno", "sleep", "dieta", "diet", "proteine",
                "protein", "carboidrati", "carbs", "palestra", "gym", "corsa", "run", "medico", "farmaco", "farmacia",
                "prescrizione", "appuntamento medico", "peso", "bmi", "yoga", "meditazione"),
            apps = listOf("fitness", "health", "salute", "strava", "fitbit", "garmin", "samsung health", "myfitnesspal",
                "yazio", "nike run", "adidas running", "freeletics", "zepp", "huawei health", "fit"),
            imageLabels = mapOf("gym" to 1.5f, "muscle" to 1.5f),
        ),
        Category(
            id = "sport", label = "Sport", color = 0xFF2B9A66,
            keywords = w(3f, "partita", "match", "gol", "goal", "classifica", "standings", "serie a", "champions",
                "campionato", "calcio", "football", "formula 1", "f1", "motogp", "tennis", "basket", "nba", "risultato",
                "highlights", "squadra", "formazione", "fantacalcio", "diretta", "live score", "1° tempo", "2° tempo"),
            apps = listOf("onefootball", "livescore", "diretta", "sofascore", "fantacalcio", "dazn", "flashscore", "espn", "sky sport"),
            imageLabels = mapOf("sports" to 1.5f, "stadium" to 2f, "ball" to 1f),
        ),
        Category(
            id = "games", label = "Giochi", color = 0xFF7C66DC,
            keywords = w(3f, "level", "livello", "score", "punteggio", "game over", "vittoria", "victory", "xp", "quest",
                "missione", "achievement", "trofeo", "playstation", "xbox", "nintendo", "steam", "fortnite", "minecraft",
                "clash", "pokemon", "pokémon", "gameplay", "multiplayer", "battle pass"),
            apps = listOf("steam", "playstation", "xbox", "nintendo", "epic", "fortnite", "minecraft", "pokemon", "clash", "roblox"),
        ),
        Category(id = PHOTO, label = "Immagini", color = 0xFF00A2C7),
        Category(id = OTHER, label = "Altro", color = 0xFF8B8D98),
    )

    private val byId = all.associateBy { it.id }

    fun find(id: String): Category? = byId[id]

    /** Etichetta leggibile per un tag (categoria automatica o tag personalizzato). */
    fun label(tag: String): String = byId[tag]?.label ?: tag
    fun color(tag: String): Long = byId[tag]?.color ?: 0xFF6C4DFF

    private fun w(weight: Float, vararg words: String): Map<String, Float> = words.associateWith { weight }
}
