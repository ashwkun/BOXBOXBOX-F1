package com.f1tracker.util

import java.util.regex.Pattern

enum class NewsCategory(val label: String) {
    ALL("All"),
    HEADLINES("Headlines"),
    PADDOCK("Paddock"),
    OTHERS("Extras")
}

object NewsCategorizer {
    // Headlines Patterns
    private val OUTCOMES = Pattern.compile("\\b(wins|won|victory|pole|p1|podium|fastest lap|champion(ship)?|wdc|wcc|clinches|secures|results|standings|points)\\b", Pattern.CASE_INSENSITIVE)
    private val EVENTS = Pattern.compile("\\b(race|qualifying|q1|q2|q3|sprint|shootout|fp1|fp2|fp3|shakedown|test(ing)?|cancelled|suspended|red flag|restart)\\b", Pattern.CASE_INSENSITIVE)
    private val INCIDENTS = Pattern.compile("\\b(crash|accident|shunt|collision|contact|retired|dnf|dns|mechanical|failure|investigation|summoned|penalty|grid drop|fine)\\b", Pattern.CASE_INSENSITIVE)
    private val OFFICIAL_BUSINESS = Pattern.compile("\\b(signs|joins|leaves|confirmed|extension|contract|announce(s|ment)?|statement|fia|regulations|calendar|rules|cap|breach|audi|ford|honda|newey)\\b", Pattern.CASE_INSENSITIVE)
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
        // 1. Check Other Series -> Others (Highest Priority to keep main feed F1 focused)
        if (OTHER_SERIES.matcher(headline).find()) {
            return NewsCategory.OTHERS
        }

        // 2. Check Headlines
        if (OUTCOMES.matcher(headline).find() ||
            EVENTS.matcher(headline).find() ||
            INCIDENTS.matcher(headline).find() ||
            OFFICIAL_BUSINESS.matcher(headline).find() ||
            TECH.matcher(headline).find()) {
            return NewsCategory.HEADLINES
        }

        // 3. Check The Paddock
        if (CONFLICT.matcher(headline).find() ||
            VERBAL_JABS.matcher(headline).find() ||
            SPECULATION.matcher(headline).find() ||
            EMOTIONS.matcher(headline).find()) {
            return NewsCategory.PADDOCK
        }

        // 4. Check Others (Rest)
        if (MEDIA.matcher(headline).find() ||
            UTILITY.matcher(headline).find() ||
            HISTORY_TECH.matcher(headline).find()) {
            return NewsCategory.OTHERS
        }

        // 5. Default -> Headlines
        return NewsCategory.HEADLINES
    }
}
