const fs = require('fs');
const https = require('https');

// CONFIG
const MAX_FEED_POSTS = 50;
const FEED_FILE = 'f1_feed.json';
const ARCHIVE_FILE = 'f1_archive.json';

// SECRETS
const token = process.env.IG_ACCESS_TOKEN;
const userId = process.env.IG_USER_ID;

if (!token || !userId) {
    console.error("❌ Error: Secrets missing (IG_ACCESS_TOKEN or IG_USER_ID).");
    process.exit(1);
}

// Helper: HTTP GET Promise
function fetchJson(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    if (json.error) reject(new Error(json.error.message));
                    else resolve(json);
                } catch (e) {
                    reject(e);
                }
            });
        }).on('error', reject);
    });
}

// Language Detection (Zero-dependency heuristic)
function detectLanguage(text) {
    if (!text) return 'en'; // Default to English if no caption

    const lowerText = text.toLowerCase();

    // Spanish indicators
    const spanishPatterns = [
        /\b(año|día|está|más|así|también|sólo|cómo|después|número|equipo|temporada|piloto)\b/i,
        /[¿¡]/,  // Spanish punctuation
        /ñ/i     // Spanish character
    ];

    // Portuguese indicators
    const portuguesePatterns = [
        /\b(não|está|é|são|com|para|pelo|pela|também|após|número)\b/i,
        /ã|õ|ç/i  // Portuguese characters
    ];

    // Check Spanish
    const spanishScore = spanishPatterns.filter(pattern => pattern.test(text)).length;

    // Check Portuguese
    const portugueseScore = portuguesePatterns.filter(pattern => pattern.test(text)).length;

    // Determine language
    if (spanishScore >= 2) return 'es';
    if (portugueseScore >= 2) return 'pt';
    if (spanishScore === 1 || portugueseScore === 1) {
        // If only 1 match, check for English indicators
        const englishPatterns = /\b(the|and|for|with|this|that|from|will|race|team|driver)\b/i;
        if (englishPatterns.test(text)) return 'en';
        return spanishScore > 0 ? 'es' : 'pt';
    }

    return 'en'; // Default to English
}

async function run() {
    try {
        // 1. Fetch Fresh Data (Limit 15 to catch bursts)
        // We request 'thumbnail_url' to handle video fallbacks safely
        const fields = 'media.limit(15){id,caption,media_url,thumbnail_url,permalink,media_type,timestamp,like_count,comments_count}';
        const url = `https://graph.facebook.com/v21.0/${userId}?fields=business_discovery.username(f1){${fields}}&access_token=${token}`;

        console.log("📡 Fetching Instagram data...");
        const response = await fetchJson(url);
        let newPosts = response.business_discovery.media.data;
        console.log(`✅ Fetched ${newPosts.length} new posts.`);

        // Add language detection
        newPosts = newPosts.map(post => ({
            ...post,
            language: detectLanguage(post.caption)
        }));

        // Filter to English only
        const englishPosts = newPosts.filter(p => p.language === 'en');
        console.log(`🌍 Language filtered: ${englishPosts.length} English posts (${newPosts.length - englishPosts.length} non-English filtered out)`);

        // Use English posts for feed
        newPosts = englishPosts;

        // ==========================================
        // PART A: UPDATE LIVE FEED (f1_feed.json)
        // ==========================================
        let existingFeed = [];
        if (fs.existsSync(FEED_FILE)) {
            try {
                existingFeed = JSON.parse(fs.readFileSync(FEED_FILE, 'utf8'));
            } catch (e) { console.warn("⚠️ Corrupt feed file, starting fresh."); }
        }

        // Merge & Deduplicate (Newest First)
        // We prioritize new data for existing posts to update metrics
        const feedMap = new Map();
        existingFeed.forEach(p => feedMap.set(p.id, p));
        newPosts.forEach(p => feedMap.set(p.id, p)); // Overwrites with fresh data

        const sortedFeed = Array.from(feedMap.values())
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        // Slice to Rolling Window (Max 50)
        const finalFeed = sortedFeed.slice(0, MAX_FEED_POSTS);

        fs.writeFileSync(FEED_FILE, JSON.stringify(finalFeed, null, 2));
        console.log(`✅ Saved ${finalFeed.length} posts to ${FEED_FILE}.`);


        // ==========================================
        // PART B: UPDATE ARCHIVE (f1_archive.json)
        // ==========================================
        let existingArchive = [];
        if (fs.existsSync(ARCHIVE_FILE)) {
            try {
                existingArchive = JSON.parse(fs.readFileSync(ARCHIVE_FILE, 'utf8'));
            } catch (e) { console.warn("⚠️ Corrupt archive file, starting fresh."); }
        }

        // Merge into Archive
        // Strategy: Update metrics for existing posts, add new ones. Never delete.
        const archiveMap = new Map();
        existingArchive.forEach(p => archiveMap.set(p.id, p));

        newPosts.forEach(p => {
            if (archiveMap.has(p.id)) {
                // Update existing post with fresh metrics/urls
                archiveMap.set(p.id, p);
            } else {
                // Add new post
                archiveMap.set(p.id, p);
            }
        });

        const finalArchive = Array.from(archiveMap.values())
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        fs.writeFileSync(ARCHIVE_FILE, JSON.stringify(finalArchive, null, 2));
        console.log(`✅ Saved ${finalArchive.length} posts to ${ARCHIVE_FILE}.`);

    } catch (error) {
        console.error("❌ Failed:", error.message);
        process.exit(1);
    }
}

run();
