package com.wordtiles.data

import com.wordtiles.core.QuizOption
import com.wordtiles.core.QuizQuestion

data class Topic(
    val id: String,
    val title: String,
    val subtitle: String,
    val words: List<String>,
)

object Catalog {
    val topics: List<Topic> = listOf(
        Topic(
            id = "rhetoric",
            title = "Rhetoric & Argument",
            subtitle = "Precision for claims, persuasion, and debate",
            words = listOf(
                "cogent", "equivocal", "fallacy", "nuance", "polemic",
                "rebuttal", "rhetoric", "specious", "syllogism", "equivocate",
            ),
        ),
        Topic(
            id = "thresholds",
            title = "Thresholds & Change",
            subtitle = "Language for beginnings, transitions, and traces",
            words = listOf(
                "culmination", "emergent", "ephemeral", "incipient", "latent",
                "liminal", "nascent", "precipitate", "transient", "vestigial",
            ),
        ),
        Topic(
            id = "systems",
            title = "Systems & Complexity",
            subtitle = "Patterns used in technical and analytical thinking",
            words = listOf(
                "deterministic", "emergent", "entropy", "equilibrium", "feedback",
                "heuristic", "invariant", "orthogonal", "recursive", "stochastic",
            ),
        ),
        Topic(
            id = "inquiry",
            title = "Inquiry & Evidence",
            subtitle = "Assess observations and build careful explanations",
            words = listOf(
                "anomalous", "conjecture", "corroborate", "discern", "empirical",
                "falsifiable", "heuristic", "inferential", "scrutinize", "synthesis",
            ),
        ),
        Topic(
            id = "ethics",
            title = "Ethics & Judgment",
            subtitle = "Distinctions for responsibility and moral reasoning",
            words = listOf(
                "deontology", "discretion", "equity", "exculpate", "impartial",
                "moral hazard", "normative", "pragmatic", "reciprocity", "utilitarian",
            ),
        ),
        Topic(
            id = "literary-tone",
            title = "Literary Tone",
            subtitle = "Describe voice, mood, and expressive restraint",
            words = listOf(
                "austere", "didactic", "elegiac", "elliptical", "evocative",
                "laconic", "lyrical", "mordant", "sardonic", "wistful",
            ),
        ),
        Topic(
            id = "craft",
            title = "Craft & Execution",
            subtitle = "Talk about care, skill, shortcuts, and refinement",
            words = listOf(
                "assiduous", "dexterous", "fastidious", "improvise", "iterate",
                "meticulous", "perfunctory", "pragmatic", "refine", "scrutinize",
            ),
        ),
        Topic(
            id = "power-society",
            title = "Power & Society",
            subtitle = "Analyze institutions, influence, and shared assumptions",
            words = listOf(
                "arbitrate", "consensus", "discourse", "hegemony", "orthodox",
                "pluralism", "salient", "tacit", "vested interest", "zeitgeist",
            ),
        ),
        Topic(
            id = "language-change",
            title = "Language & Change",
            subtitle = "Trace how expressions emerge, spread, and shift",
            words = listOf(
                "coinage", "colloquial", "etymology", "idiomatic", "lexicon",
                "neologism", "semantic drift", "vernacular", "obsolete", "polysemy",
            ),
        ),
        Topic(
            id = "causation",
            title = "Cause & Consequence",
            subtitle = "Separate origins, mechanisms, and downstream effects",
            words = listOf(
                "antecedent", "catalyst", "causal", "concomitant", "contingent",
                "corollary", "mediate", "precipitate", "ramification", "repercussion",
            ),
        ),
        Topic(
            id = "negotiation",
            title = "Negotiation & Accord",
            subtitle = "Navigate concessions, disputes, and durable agreements",
            words = listOf(
                "accede", "arbitrate", "broker", "compromise", "concession",
                "impasse", "mediate", "proviso", "reconcile", "stipulate",
            ),
        ),
        Topic(
            id = "perception",
            title = "Perception & Attention",
            subtitle = "Describe noticing, interpreting, and overlooking",
            words = listOf(
                "attentive", "discern", "discriminate", "inattentional", "intuitive",
                "perceptible", "salient", "sensory", "subliminal", "vigilant",
            ),
        ),
        Topic(
            id = "emotion",
            title = "Emotion & Temperament",
            subtitle = "Name subtle moods, impulses, and dispositions",
            words = listOf(
                "ambivalent", "equanimity", "exuberant", "indignant", "melancholy",
                "reticent", "sanguine", "solicitous", "stoic", "trepidation",
            ),
        ),
        Topic(
            id = "time-planning",
            title = "Time & Planning",
            subtitle = "Reason about sequence, urgency, and long horizons",
            words = listOf(
                "contingency", "defer", "expedite", "imminent", "interim",
                "protracted", "retrospective", "simultaneous", "tentative", "trajectory",
            ),
        ),
        Topic(
            id = "research-writing",
            title = "Research & Writing",
            subtitle = "Build precise arguments from sources and observations",
            words = listOf(
                "annotate", "bibliography", "corpus", "explicate", "methodology",
                "paraphrase", "qualify", "synthesize", "thesis", "triangulate",
            ),
        ),
        Topic(
            id = "search-navigation",
            title = "Search & Navigation",
            subtitle = "Phrases for locating, following, and reaching a target",
            words = listOf(
                "come across", "home in on", "look for", "map out", "narrow down",
                "seek out", "track down", "trace back", "zero in on", "wayfinding",
            ),
        ),
        Topic(
            id = "escape-resilience",
            title = "Escape & Resilience",
            subtitle = "Describe evasion, recovery, and resistance under pressure",
            words = listOf(
                "break free", "bounce back", "circumvent", "elude", "endure",
                "get away", "persevere", "rebound", "withstand", "weather",
            ),
        ),
        Topic(
            id = "construction-change",
            title = "Construction & Change",
            subtitle = "Build, revise, dismantle, and replace structures",
            words = listOf(
                "assemble", "build up", "consolidate", "dismantle", "erect",
                "retrofit", "shore up", "tear down", "transform", "undermine",
            ),
        ),
        Topic(
            id = "economics",
            title = "Economics & Incentives",
            subtitle = "Discuss scarcity, exchange, risk, and market behavior",
            words = listOf(
                "arbitrage", "externality", "fungible", "liquidity", "marginal",
                "opportunity cost", "scarcity", "subsidize", "trade-off", "volatile",
            ),
        ),
        Topic(
            id = "governance",
            title = "Governance & Policy",
            subtitle = "Examine mandates, institutions, and public decisions",
            words = listOf(
                "accountability", "bureaucratic", "constituency", "deliberative", "mandate",
                "oversight", "precedent", "ratify", "regulatory", "transparent",
            ),
        ),
    )

    val quizzes: List<QuizQuestion> = listOf(
        question(
            id = "seek-advice",
            targetWord = "seek",
            definition = "Try to obtain guidance from another person.",
            example = "Before deciding, they will ___ expert advice.",
            correct = listOf(
                option("seek", "seek", "“Seek advice” is a standard direct-object construction."),
                option("look-for", "look for", "“Look for advice” is grammatical here, though less formal."),
            ),
            distractors = listOf(
                option("search", "search", "“Search” normally needs a place or collection as its object, or “for advice”."),
            ),
        ),
        question(
            id = "mitigate-impact",
            targetWord = "mitigate",
            definition = "Make a harmful effect less severe.",
            example = "The shade trees help ___ the impact of extreme heat.",
            correct = listOf(
                option("mitigate", "mitigate", "It directly means reducing the severity of an effect."),
                option("alleviate", "alleviate", "It can take “the impact” directly and means making it less severe."),
            ),
            distractors = listOf(
                option("aggravate", "aggravate", "It means making the impact worse, the opposite of the intended sense."),
            ),
        ),
        question(
            id = "corroborate-account",
            targetWord = "corroborate",
            definition = "Support a statement with additional evidence.",
            example = "The archived records ___ the witness's account.",
            correct = listOf(
                option("corroborate", "corroborate", "The records provide independent support for the account."),
                option("confirm", "confirm", "It fits both the grammar and the evidential sense here."),
            ),
            distractors = listOf(
                option("conjecture", "conjecture", "It means form a guess and does not take an account as supporting evidence."),
            ),
        ),
        question(
            id = "ephemeral-installation",
            targetWord = "ephemeral",
            definition = "Existing for only a short time.",
            example = "The outdoor installation was ___, lasting only one evening.",
            correct = listOf(
                option("ephemeral", "ephemeral", "It emphasizes a very brief existence."),
                option("transient", "transient", "It also describes something temporary or short-lived."),
            ),
            distractors = listOf(
                option("perpetual", "perpetual", "It means continuing indefinitely, which conflicts with the example."),
            ),
        ),
        question(
            id = "scrutinize-accounts",
            targetWord = "scrutinize",
            definition = "Inspect something closely and critically.",
            example = "Independent auditors will ___ the accounts.",
            correct = listOf(
                option("scrutinize", "scrutinize", "It conveys close, critical inspection and takes a direct object."),
                option("examine", "examine", "It is grammatically and semantically suitable in this sentence."),
            ),
            distractors = listOf(
                option("glance", "glance", "It suggests a brief look and normally requires “at” before the object."),
            ),
        ),
        question(
            id = "equivocal-reply",
            targetWord = "equivocal",
            definition = "Open to more than one interpretation and not clearly committed.",
            example = "Her reply remained ___, leaving both sides uncertain.",
            correct = listOf(
                option("equivocal", "equivocal", "It captures both ambiguity and lack of commitment."),
                option("ambiguous", "ambiguous", "It fits the sentence's emphasis on uncertain meaning."),
            ),
            distractors = listOf(
                option("unequivocal", "unequivocal", "It means completely clear, the opposite of the intended sense."),
            ),
        ),
    )

    private fun option(id: String, text: String, explanation: String) =
        QuizOption(id = id, text = text, explanation = explanation)

    private fun question(
        id: String,
        targetWord: String,
        definition: String,
        example: String,
        correct: List<QuizOption>,
        distractors: List<QuizOption>,
    ) = QuizQuestion(
        id = id,
        targetWord = targetWord,
        definition = definition,
        example = example,
        options = correct + distractors,
        correctIds = correct.mapTo(linkedSetOf()) { it.id },
    )
}
