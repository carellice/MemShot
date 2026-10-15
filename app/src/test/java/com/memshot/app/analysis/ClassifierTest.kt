package com.memshot.app.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassifierTest {
    private val classifier = Classifier()

    private fun tags(text: String, app: String? = null) = classifier.classify(text, emptyList(), app)

    @Test fun film() {
        val t = tags("Dune: Parte Due\n2024 · 2 h 46 min · Fantascienza\nRegia: Denis Villeneuve\nCast: Timothée Chalamet\nGuarda ora + La mia lista Trailer")
        assertEquals("film", t.first())
    }

    @Test fun filmFromSourceAppOnly() {
        assertEquals("film", tags("Stranger Things\nContinua a guardare", "Netflix").first())
    }

    @Test fun prompt() {
        val t = tags(
            "Agisci come un copywriter esperto. Scrivi una email di follow-up per un cliente che non ha risposto al preventivo. " +
                "Tono: professionale ma cordiale. Formato: massimo 120 parole. Contesto: [nome cliente], [servizio offerto]. " +
                "Rispondi passo dopo passo."
        )
        assertEquals("prompt", t.first())
    }

    @Test fun englishImagePrompt() {
        assertEquals("prompt", tags("a cozy cabin in the woods at night, cinematic lighting, photorealistic, ultra detailed --ar 16:9 --v 6").first())
    }

    @Test fun recipe() {
        assertEquals("recipe", tags("Carbonara\nIngredienti per 4 persone\nSpaghetti 320 g\nGuanciale 150 g\nPepe nero q.b.\nPreparazione: rosolate il guanciale").first())
    }

    @Test fun chat() {
        assertEquals("chat", tags("Marco\nonline\nCi vediamo stasera? 18:02\nSì perfetto 18:05\nOk 18:06\nRicordati il regalo 18:10\nScrivi un messaggio").first())
    }

    @Test fun shopping() {
        assertEquals("shopping", tags("Sony WH-1000XM5\n4,6 su 5 stelle 12.430 recensioni\n€ 299,00 −25%\nSpedizione GRATUITA con Prime\nAggiungi al carrello Acquista ora").first())
    }

    @Test fun travel() {
        assertEquals("travel", tags("Carta d'imbarco\nRoma Fiumicino (FCO) → Barcellona\nVolo FR 6512\nPartenza 06:40 Arrivo 08:55\nGate B12 Imbarco 06:10").first())
    }

    @Test fun code() {
        assertEquals("code", tags("import requests\ndef fetch_data(url):\n    response = requests.get(url)\n    return response.json()\nTraceback (most recent call last): KeyError").first())
    }

    @Test fun music() {
        assertEquals("music", tags("Blinding Lights\nThe Weeknd · After Hours\nBrano · 2020\nAscoltatori mensili 110.452.311\nAggiungi alla playlist").first())
    }

    @Test fun quote() {
        assertTrue("quotes" in tags("“La vita è ciò che ti accade mentre sei impegnato a fare altri progetti.” — John Lennon"))
    }

    @Test fun quoteWithStatusBar() {
        assertEquals("quotes", tags("14:32\n\"La vita è ciò che ti accade mentre sei impegnato a fare altri progetti.\"\n— John Lennon").first())
    }

    @Test fun promptWithStatusBar() {
        assertEquals("prompt", tags("9:41\nScrivi un post per LinkedIn sul lancio del nostro prodotto, massimo 150 parole, con 3 hashtag e una call to action finale").first())
    }

    @Test fun noText() {
        assertEquals(listOf(Categories.PHOTO), tags(""))
    }

    @Test fun sourceAppParsing() {
        assertEquals("Netflix", com.memshot.app.importer.ScreenshotImporter.parseSourceApp("Screenshot_20241012_101010_Netflix.jpg"))
        assertEquals(null, com.memshot.app.importer.ScreenshotImporter.parseSourceApp("Screenshot_20241012-101010.png"))
        assertTrue(com.memshot.app.importer.ScreenshotImporter.parseSourceApp("Screenshot_2024-10-12-10-10-10-123_com.netflix.mediaclient.jpg")!!.startsWith("netflix"))
    }
}
