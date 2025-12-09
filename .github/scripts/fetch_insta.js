const fs = require('fs');
const https = require('https');

// CONFIG
// CONFIG
const MAX_FEED_POSTS = 300;  // Increased to 300 for more content
const FEED_FILE = 'f1_feed.json';        // Mixed content (images + videos)
const REELS_FILE = 'f1_reels.json';      // Videos only
const ARCHIVE_FILE = 'f1_archive.json';  // Full history

// ACCOUNT LISTS (BRD Section 2.1)
const OFFICIAL_ACCOUNTS = [
    'f1', 'motorsportcom'
];

const MEME_ACCOUNTS = [
    'lollipopmancomics', 'f1troll', 'racingvacing',
    'f1_no_context', 'boxboxnightmares', '5secondpenalty', 'f1humor.official',
    'dailyf1center', 'f1memesig', 'f1wow', 'f1_edit33', 'shaaaarl', 'racingballsf1'
];

const ALL_ACCOUNTS = [...OFFICIAL_ACCOUNTS, ...MEME_ACCOUNTS];

// SECRETS
const token = process.env.IG_ACCESS_TOKEN;
const userId = process.env.IG_USER_ID;

if (!token || !userId) {
    console.error("❌ Error: Secrets missing (IG_ACCESS_TOKEN or IG_USER_ID).");
    process.exit(1);
}

// Helper: HTTP GET Promise (Returns Data + Headers)
function fetchJson(url) {
    return new Promise((resolve, reject) => {
        https.get(url, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    if (json.error) reject(new Error(json.error.message));
                    else resolve({ json, headers: res.headers });
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

        // 1. SEQUENTIAL FETCH FROM ALL ACCOUNTS (Rate Limit Safe)
        // Increased to 25 to get more fresh URLs - prevents stale content
        const fields = 'media.limit(25){id,caption,media_url,thumbnail_url,permalink,media_type,timestamp,like_count,comments_count,children{id,media_type,media_url,thumbnail_url,timestamp}}';
        let allPosts = [];

        for (const username of ALL_ACCOUNTS) {
            try {
                console.log(`   - Fetching @${username}...`);
                const url = `https://graph.facebook.com/v21.0/${userId}?fields=business_discovery.username(${username}){${fields}}&access_token=${token}`;

                const { json, headers } = await fetchJson(url);
                const posts = json.business_discovery.media.data;

                // Add author field to each post
                const processedPosts = posts.map(post => ({
                    ...post,
                    author: username,
                    language: detectLanguage(post.caption)
                }));

                allPosts = allPosts.concat(processedPosts);

                // --- DYNAMIC BACKOFF LOGIC (Step 3) ---
                let delay = 2000; // Default 2s (Step 2)

                // Check 'x-business-use-case-usage' header
                // Format: [{"id":"...","call_count":10,"total_cputime":15,"total_time":15,"type":"business_discovery"}]
                const usageHeader = headers['x-business-use-case-usage'];
                if (usageHeader) {
                    try {
                        const usageData = JSON.parse(usageHeader);
                        if (usageData && usageData.length > 0) {
                            const cpuTime = usageData[0].total_cputime; // Percentage used
                            console.log(`     📊 API Usage: ${cpuTime}%`);

                            if (cpuTime > 90) {
                                console.warn(`     🔥 HIGH LOAD (>90%). Cooling down for 2 minutes...`);
                                delay = 120000; // 2 minutes
                            } else if (cpuTime > 80) {
                                console.warn(`     ⚠️ WARN LOAD (>80%). Cooling down for 30 seconds...`);
                                delay = 30000; // 30 seconds
                            }
                        }
                    } catch (e) {
                        // Ignore header parse error
                    }
                }

                // Wait
                if (delay > 2000) console.log(`     ⏳ Pausing for ${delay / 1000}s...`);
                await new Promise(r => setTimeout(r, delay));

            } catch (error) {
                console.warn(`⚠️  Failed to fetch @${username}:`, error.message);
                // Continue to next account
            }
        }

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
        const freshPostIds = new Set(); // Track posts with fresh URLs

        // Existing data
        existingFeed.forEach(p => feedMap.set(p.id, p));
        existingArchive.forEach(p => archiveMap.set(p.id, p));

        // New data (overwrites to update metrics AND URLs)
        allPosts.forEach(p => {
            feedMap.set(p.id, p);
            archiveMap.set(p.id, p);
            freshPostIds.add(p.id); // Mark as having fresh URLs
        });

        // 5. DEDUPLICATE & MERGE (Audio Workaround)
        const groupedPosts = new Map();
        const ungroupedPosts = []; // Posts with null/empty captions - can't be grouped reliably

        // Group by Author + Caption (first 50 chars)
        // Skip posts with null/empty captions to avoid false matches
        Array.from(feedMap.values()).forEach(post => {
            const captionKey = post.caption ? post.caption.substring(0, 50).trim() : '';

            // If caption is empty or too short, don't group - keep separately
            if (!captionKey || captionKey.length < 10) {
                ungroupedPosts.push(post);
                return;
            }

            const key = `${post.author}|${captionKey}`;

            if (!groupedPosts.has(key)) {
                groupedPosts.set(key, []);
            }
            groupedPosts.get(key).push(post);
        });

        const finalFeedList = [];

        groupedPosts.forEach((group, key) => {
            // If we have multiple posts in a group, handle deduplication
            if (group.length > 1) {
                // Sort by timestamp (newest first) for deduplication
                group.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

                const videoPost = group.find(p => p.media_type === 'VIDEO');
                const imagePost = group.find(p => p.media_type === 'IMAGE' || p.media_type === 'CAROUSEL_ALBUM');

                if (videoPost && imagePost) {
                    // Case 1: VIDEO + IMAGE/CAROUSEL - merge audio
                    console.log(`🔀 Merging audio from Video ${videoPost.id} to ${imagePost.media_type} ${imagePost.id}`);
                    imagePost.audio_url = videoPost.media_url;
                    finalFeedList.push(imagePost);
                } else {
                    // Case 2: Same-type duplicates - keep only the newest one
                    console.log(`🔄 Deduplicating ${group.length} posts for "${key.substring(0, 50)}..." - keeping newest`);
                    finalFeedList.push(group[0]); // Already sorted, so first is newest
                }
            } else {
                // Single post, keep it
                finalFeedList.push(group[0]);
            }
        });

        // Add back ungrouped posts (those with null/empty captions)
        ungroupedPosts.forEach(post => finalFeedList.push(post));

        // 6. SORT & SLICE (Newest First)
        const sortedFeed = finalFeedList
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        const sortedArchive = Array.from(archiveMap.values())
            .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));

        // 7. GENERATE DUAL-FILE OUTPUT

        // A) MIXED FEED (f1_feed.json) - Images + Videos
        // Filter: Videos need fresh URLs (expire in 4-6h), photos can be stale (expire in 24-48h)
        const finalFeed = sortedFeed
            .filter(p => {
                // Videos require fresh URLs
                if (p.media_type === 'VIDEO' && !freshPostIds.has(p.id)) {
                    console.log(`   ⏭️ Skipping stale video from feed: ${p.id}`);
                    return false;
                }
                return true;
            })
            .slice(0, MAX_FEED_POSTS);
        fs.writeFileSync(FEED_FILE, JSON.stringify(finalFeed, null, 2));
        console.log(`💾 Saved ${finalFeed.length} posts to ${FEED_FILE}`);

        // B) REELS FEED (f1_reels.json) - Only posts with actual video content AND fresh URLs
        const videoFeed = sortedFeed
            .filter(p => {
                // CRITICAL: Only include posts with fresh URLs (fetched in this run)
                if (!freshPostIds.has(p.id)) {
                    console.log(`   ⏭️ Skipping stale URL post: ${p.id}`);
                    return false;
                }

                // Pure video posts - always include
                if (p.media_type === 'VIDEO') return true;

                // Carousel posts - only include if they have at least one VIDEO child
                if (p.media_type === 'CAROUSEL_ALBUM') {
                    const hasVideoChild = p.children?.data?.some(child => child.media_type === 'VIDEO');
                    if (!hasVideoChild) {
                        console.log(`   ⏭️ Skipping image-only carousel: ${p.id}`);
                    }
                    return hasVideoChild;
                }

                return false;
            })
            .slice(0, MAX_FEED_POSTS);
        fs.writeFileSync(REELS_FILE, JSON.stringify(videoFeed, null, 2));
        console.log(`🎬 Saved ${videoFeed.length} reels (videos only) to ${REELS_FILE}`);

        // C) FULL ARCHIVE (f1_archive.json) - Everything, unlimited
        fs.writeFileSync(ARCHIVE_FILE, JSON.stringify(sortedArchive, null, 2));
        console.log(`📚 Saved ${sortedArchive.length} posts to ${ARCHIVE_FILE}`);

        // 8. SUMMARY
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
