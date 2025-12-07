const fs = require('fs');
const https = require('https');

// CONFIG
const OUTPUT_FILE = 'f1_youtube.json';
const SEARCH_QUERY = 'FORMULA 1';
const MAX_RESULTS = 50;

// SECRETS
const API_KEY = process.env.YOUTUBE_API_KEY;

if (!API_KEY) {
    console.error("❌ Error: YOUTUBE_API_KEY missing.");
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

// Helper: Parse Duration (PT1H2M10S -> Seconds)
function parseDuration(duration) {
    const match = duration.match(/PT(\d+H)?(\d+M)?(\d+S)?/);
    if (!match) return 0;

    const hours = (parseInt(match[1]) || 0);
    const minutes = (parseInt(match[2]) || 0);
    const seconds = (parseInt(match[3]) || 0);

    return (hours * 3600) + (minutes * 60) + seconds;
}

async function run() {
    try {
        console.log(`📡 Fetching YouTube videos for query: "${SEARCH_QUERY}"...`);

        // 1. Search for videos
        // We fetch more than needed to account for Shorts filtering
        const searchUrl = `https://www.googleapis.com/youtube/v3/search?part=snippet&q=${encodeURIComponent(SEARCH_QUERY)}&type=video&order=relevance&maxResults=${MAX_RESULTS}&key=${API_KEY}`;
        const searchResults = await fetchJson(searchUrl);

        if (!searchResults.items || searchResults.items.length === 0) {
            console.log("⚠️ No videos found.");
            return;
        }

        const videoIds = searchResults.items.map(item => item.id.videoId).join(',');

        // 2. Fetch Video Details (Duration, Stats)
        console.log(`🔍 Fetching details for ${searchResults.items.length} videos...`);
        const detailsUrl = `https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails,statistics&id=${videoIds}&key=${API_KEY}`;
        const videoDetails = await fetchJson(detailsUrl);

        // 3. Process and Filter
        const videos = [];
        const SHORTS_THRESHOLD = 60; // Seconds

        // Non-English patterns in titles (common indicators)
        const NON_ENGLISH_PATTERNS = [
            /🔴\s*(LIVE|EN VIVO|AO VIVO|DIRECTO)/i,  // Spanish/Portuguese live streams
            /\bEN VIVO\b/i,                          // Spanish "live"
            /\bAO VIVO\b/i,                          // Portuguese "live"
            /\bDIRECTO\b/i,                          // Spanish "direct/live"
            /\bCLASIFICACIÓN\b/i,                    // Spanish "qualification"
            /\bCORRIDA\b/i,                          // Portuguese "race"
            /\bGP DE\b/i,                            // Spanish/Portuguese GP naming
            /【.*】/,                                 // Japanese brackets
            /[\u3040-\u309F\u30A0-\u30FF]/,           // Japanese characters
            /[\uAC00-\uD7AF]/,                       // Korean characters
            /[\u4E00-\u9FFF]/,                       // Chinese characters
            /\bFORMULA EXTREMA\b/i,                  // Spanish channel
            /\bTRIBUNA MOTORI\b/i,                   // Italian channel
            /\bPOLEMIC\b.*\bF1\b/i,                  // Non-English content
        ];

        function isEnglish(item) {
            // Check audio language if available
            const audioLang = item.snippet.defaultAudioLanguage;
            const defaultLang = item.snippet.defaultLanguage;

            // If language is explicitly set and not English, skip
            if (audioLang && !audioLang.startsWith('en')) {
                return false;
            }
            if (defaultLang && !defaultLang.startsWith('en') && !audioLang) {
                return false;
            }

            // Check title for non-English patterns
            const title = item.snippet.title;
            for (const pattern of NON_ENGLISH_PATTERNS) {
                if (pattern.test(title)) {
                    return false;
                }
            }

            return true;
        }

        for (const item of videoDetails.items) {
            const durationSec = parseDuration(item.contentDetails.duration);

            // Filter: Avoid Shorts (< 60s)
            const isShortsTag = item.snippet.title.toLowerCase().includes('#shorts');

            // Filter: English only
            const isEnglishContent = isEnglish(item);

            if (durationSec > SHORTS_THRESHOLD && !isShortsTag && isEnglishContent) {
                videos.push({
                    id: item.id,
                    title: item.snippet.title,
                    description: item.snippet.description,
                    thumbnail: item.snippet.thumbnails.maxres?.url || item.snippet.thumbnails.high?.url || item.snippet.thumbnails.medium?.url,
                    publishedAt: item.snippet.publishedAt,
                    channelTitle: item.snippet.channelTitle,
                    viewCount: item.statistics.viewCount,
                    likeCount: item.statistics.likeCount,
                    duration: item.contentDetails.duration,
                    durationSec: durationSec,
                    url: `https://www.youtube.com/watch?v=${item.id}`
                });
            }
        }

        console.log(`✅ Filtered ${videoDetails.items.length} -> ${videos.length} videos (removed shorts + non-English).`);

        // 4. Save to JSON
        fs.writeFileSync(OUTPUT_FILE, JSON.stringify(videos, null, 2));
        console.log(`💾 Saved to ${OUTPUT_FILE}`);

    } catch (error) {
        console.error("❌ Failed:", error.message);
        process.exit(1);
    }
}

run();
