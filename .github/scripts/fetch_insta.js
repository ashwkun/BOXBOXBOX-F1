const fs = require('fs');
const https = require('https');

// CONFIG
// CONFIG
const MAX_FEED_POSTS = 150;  // Increased to 150 for more content
const FEED_FILE = 'f1_feed.json';        // Mixed content (images + videos)
const REELS_FILE = 'f1_reels.json';      // Videos only
const ARCHIVE_FILE = 'f1_archive.json';  // Full history

// ACCOUNT LISTS (BRD Section 2.1)
const OFFICIAL_ACCOUNTS = [
    'f1', 'redbullracing', 'scuderiaferrari', 'mclaren', 'mercedesamgf1'
];

const MEME_ACCOUNTS = [
    'lollipopmancomics', 'f1troll', 'badf1stats',
    'sim3744', 'f1humor', 'paddock.legends'
];

const ALL_ACCOUNTS = [...OFFICIAL_ACCOUNTS, ...MEME_ACCOUNTS];

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
        console.log(`📡 Fetching from ${ALL_ACCOUNTS.length} accounts...`);

        // 1. PARALLEL FETCH FROM ALL ACCOUNTS
        const fields = 'media.limit(50){id,caption,media_url,thumbnail_url,permalink,media_type,timestamp,like_count,comments_count}';

        const fetchPromises = ALL_ACCOUNTS.map(async (username) => {
            try {
                const url = `https://graph.facebook.com/v21.0/${userId}?fields=business_discovery.username(${username}){${fields}}&access_token=${token}`;
                const response = await fetchJson(url);
                const posts = response.business_discovery.media.data;

                // Add author field to each post
                return posts.map(post => ({
                    ...post,
                    author: username,
                    language: detectLanguage(post.caption)
                }));
            } catch (error) {
                console.warn(`⚠️  Failed to fetch @${username}:`, error.message);
                return []; // Return empty array on failure, don't crash
            }
        });

        const results = await Promise.all(fetchPromises);
        let allPosts = results.flat(); // Flatten array of arrays

        console.log(`✅ Fetched ${allPosts.length} total posts from ${ALL_ACCOUNTS.length} accounts.`);

        // 2. LANGUAGE FILTERING (English only)
        const englishPosts = allPosts.filter(p => p.language === 'en');
        console.log(`🌍 Language filtered: ${englishPosts.length} English posts (${allPosts.length - englishPosts.length} filtered out)`);

        allPosts = englishPosts;

        // 3. LOAD EXISTING DATA
        let existingFeed = [];
        let existingArchive = [];

        if (fs.existsSync(FEED_FILE)) {
            try {
                existingFeed = JSON.parse(fs.readFileSync(FEED_FILE, 'utf8'));
            } catch (e) { console.warn("⚠️  Corrupt feed file, starting fresh."); }
        }

        if (fs.existsSync(ARCHIVE_FILE)) {
            try {
                existingArchive = JSON.parse(fs.readFileSync(ARCHIVE_FILE, 'utf8'));
            } catch (e) { console.warn("⚠️  Corrupt archive file, starting fresh."); }
        }

        // 4. MERGE & DEDUPLICATE
        const feedMap = new Map();
        const archiveMap = new Map();

        // Existing data
        existingFeed.forEach(p => feedMap.set(p.id, p));
        existingArchive.forEach(p => archiveMap.set(p.id, p));

        // New data (overwrites to update metrics)
        allPosts.forEach(p => {
            feedMap.set(p.id, p);
            archiveMap.set(p.id, p);
        });

        // 5. SORT & SLICE (Newest First)
        const sortedFeed = Array.from(feedMap.values())
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        const sortedArchive = Array.from(archiveMap.values())
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        // 6. GENERATE DUAL-FILE OUTPUT

        // A) MIXED FEED (f1_feed.json) - Images + Videos, limit 60
        const finalFeed = sortedFeed.slice(0, MAX_FEED_POSTS);
        fs.writeFileSync(FEED_FILE, JSON.stringify(finalFeed, null, 2));
        console.log(`💾 Saved ${finalFeed.length} posts to ${FEED_FILE}`);

        // B) REELS FEED (f1_reels.json) - Videos only, limit 60
        const videoFeed = sortedFeed
            .filter(p => p.media_type === 'VIDEO')
            .slice(0, MAX_FEED_POSTS);
        fs.writeFileSync(REELS_FILE, JSON.stringify(videoFeed, null, 2));
        console.log(`🎬 Saved ${videoFeed.length} videos to ${REELS_FILE}`);

        // C) FULL ARCHIVE (f1_archive.json) - Everything, unlimited
        fs.writeFileSync(ARCHIVE_FILE, JSON.stringify(sortedArchive, null, 2));
        console.log(`📚 Saved ${sortedArchive.length} posts to ${ARCHIVE_FILE}`);

        // 7. SUMMARY
        console.log(`\n✨ Success! Summary:`);
        console.log(`   - Total accounts: ${ALL_ACCOUNTS.length}`);
        console.log(`   - Mixed feed: ${finalFeed.length} posts`);
        console.log(`   - Reels feed: ${videoFeed.length} videos`);
        console.log(`   - Archive: ${sortedArchive.length} items`);

    } catch (error) {
        console.error("❌ Failed:", error.message);
        process.exit(1);
    }
}

run();
