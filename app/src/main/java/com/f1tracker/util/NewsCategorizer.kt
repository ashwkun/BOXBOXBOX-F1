package com.f1tracker.util

import java.util.regex.Pattern

enum class NewsCategory(val label: String) {
    NUCLEAR("Nuclear"),
    MAJOR("Major"),
    ALL("All"),
    HEADLINES("Headlines"),
    PADDOCK("Paddock"),
    OTHERS("Extras")
}

object NewsCategorizer {
    // --- NUCLEAR PATTERNS (Score 999) ---
    private val NUCLEAR_PATTERNS = Pattern.compile(
        "\\b(crash|accident|injured|hospitalized|fatal|death|died|red flag|red-flagged|cancelled|postponed)\\b|" +
        "\\b(wins|won|victory|victorious|pole position|takes pole|claims pole|grabs pole|snatches pole|sprint.*win|sprint.*victory)\\b|" +
        "\\b(clinches|secures|wins|seals).*(championship|title|wdc|wcc)|(mathematically|officially).*(eliminated|out of contention)\\b|" +
        "\\b(disqualified|banned|suspended)\\b|" +
        "\\b(leaves|exits|departs|replaced|retires|retirement|retiring|announces retirement)\\b|" +
        "\\b(breaks? record|all-time|historic|history-making|most wins|most poles|most podiums)\\b|" +
        "\\b(team.*withdraw|leaving f1|exits formula 1|new team|team entry|joins f1|entering formula 1|sold|bought|ownership|takeover)\\b|" +
        "\\b(cost cap|budget cap|breach|violation|exceeded|regulation change|rule change|technical directive|illegal|non-compliant|technical infringement|protest|appeal)\\b|" +
        // Driver Moves (All Teams)
        "\\b(moves to|joins|signs for|promoted to|in at|switches to|signed by).*(red bull|ferrari|mercedes|mclaren|aston martin|alpine|williams|haas|sauber|audi|racing bulls|rb|alphatauri)\\b|" +
        "\\b(replaces|replacing).*(driver|seat)\\b|" +
        "\\b(contract).*(extension|extended|renewed|signed)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // --- MAJOR PATTERNS (Score >= 85) ---
    private val MAJOR_PATTERNS = Pattern.compile(
        "\\b(dominates|dominated|dominating|podium|can clinch|championship.*lead|championship.*battle|points lead|points gap)\\b|" +
        "\\b(signs|signed|confirms|confirmed|joins|joined).*(2025|2026|2027|contract|deal)|(official:).*(driver|seat|signs|joins)\\b|" +
        "\\b(team principal|tp|ceo|horner|wolff|vasseur|brown|stella|newey).*(leaves|joins|appointed|names|confirms)\\b|" +
        "\\b(grid drop|grid penalty|grid-place penalty|penalty|penalised|penalized|stewards.*decision|stewards.*ruling)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Headlines Patterns (Standard)
    private val OUTCOMES = Pattern.compile("\\b(p1|fastest lap|results|standings|points)\\b", Pattern.CASE_INSENSITIVE)
    private val EVENTS = Pattern.compile("\\b(race|qualifying|q1|q2|q3|sprint|shootout|fp1|fp2|fp3|shakedown|test(ing)?|restart)\\b", Pattern.CASE_INSENSITIVE)
    private val INCIDENTS = Pattern.compile("\\b(shunt|collision|contact|retired|dnf|dns|mechanical|failure|investigation|summoned|fine)\\b", Pattern.CASE_INSENSITIVE)
    private val OFFICIAL_BUSINESS = Pattern.compile("\\b(statement|fia|regulations|calendar|rules|audi|ford|honda)\\b", Pattern.CASE_INSENSITIVE)
    private val TECH = Pattern.compile("\\b(upgrades|floor|wing|sidepod|chassis|engine|pu|gearbox|livery|launch|unveil|spec)\\b", Pattern.CASE_INSENSITIVE)

    // The Paddock Patterns
    private val CONFLICT = Pattern.compile("\\b(slams|furious|hits out|blames|accuses|blasts|rages|warning|warns|threatens|clash|row|tension|rift|toxic|rivalry|nemesis|feud)\\b", Pattern.CASE_INSENSITIVE)
    private val VERBAL_JABS = Pattern.compile("\\b(jab|dig|swipe|mock(s)?|laughs off|ridiculous|nonsense|bizarre|stupid|idiot|joke|circus|shambles|embarrassing|scathing|brutal)\\b", Pattern.CASE_INSENSITIVE)
    private val SPECULATION = Pattern.compile("\\b(rumour|speculation|silly season|hint(s)?|tease(s)?|claims|insists|denies|admits|confesses|reveals|opens up|opinion|verdict|column|analysis|hot take)\\b", Pattern.CASE_INSENSITIVE)
    private val EMOTIONS = Pattern.compile("\\b(fears|worried|concern|regret|sorry|apology|delighted|shocked|surprise|confused|doubt|pressure|nightmare|dream)\\b", Pattern.CASE_INSENSITIVE)

    // Cool Down Patterns (Now Others)
    private val MEDIA = Pattern.compile("\\b(podcast|video|vlog|interview|exclusive|chat|q&a|quiz|trivia|challenge|game|esports|sim racing|movie|film|drive to survive|gallery|photo)\\b", Pattern.CASE_INSENSITIVE)
    private val UTILITY = Pattern.compile("\\b(how to watch|start time|schedule|timetable|weather|forecast|tv guide|live stream|tickets|merch|shop|auction|sale)\\b", Pattern.CASE_INSENSITIVE)
    private val OTHER_SERIES = Pattern.compile("\\b(f1 academy|w series|formula 2|f2|formula 3|f3|formula e|fe|wec|le mans|indycar|nascar|motogp|karting)\\b", Pattern.CASE_INSENSITIVE)
    private val HISTORY_TECH = Pattern.compile("\\b(history|throwback|on this day|legend|tribute|memorial|tech talk|deep dive|strategy|tyres|pirelli|logistics)\\b", Pattern.CASE_INSENSITIVE)

    fun categorize(headline: String): NewsCategory {
        // 1. Check Nuclear (Highest Priority)
        if (NUCLEAR_PATTERNS.matcher(headline).find()) {
            return NewsCategory.NUCLEAR
        }

        // 2. Check Major
        if (MAJOR_PATTERNS.matcher(headline).find()) {
            return NewsCategory.MAJOR
        }

        // 3. Check Other Series -> Others
        if (OTHER_SERIES.matcher(headline).find()) {
            return NewsCategory.OTHERS
        }

        // 4. Check Headlines
        if (OUTCOMES.matcher(headline).find() ||
            EVENTS.matcher(headline).find() ||
            INCIDENTS.matcher(headline).find() ||
            OFFICIAL_BUSINESS.matcher(headline).find() ||
            TECH.matcher(headline).find()) {
            return NewsCategory.HEADLINES
        }

        // 5. Check The Paddock
        if (CONFLICT.matcher(headline).find() ||
            VERBAL_JABS.matcher(headline).find() ||
            SPECULATION.matcher(headline).find() ||
            EMOTIONS.matcher(headline).find()) {
            return NewsCategory.PADDOCK
        }

        // 6. Check Others (Rest)
        if (MEDIA.matcher(headline).find() ||
            UTILITY.matcher(headline).find() ||
            HISTORY_TECH.matcher(headline).find()) {
            return NewsCategory.OTHERS
        }

        // 7. Default -> Headlines
        return NewsCategory.HEADLINES
    }
}
