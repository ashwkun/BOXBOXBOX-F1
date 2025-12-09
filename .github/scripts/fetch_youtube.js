const fs = require('fs');
const https = require('https');

// CONFIG
const API_KEY = process.env.YOUTUBE_API_KEY;
const F1_CHANNEL_ID = 'UCB_qr75-ydFVKSF9Dmo6izg';
const F1_UPLOADS_PLAYLIST = 'UULFB_qr75-ydFVKSF9Dmo6izg';

if (!API_KEY) {
    console.error("❌ Error: YOUTUBE_API_KEY missing.");
    process.exit(1);
}

// CHANNEL QUALITY SCORES
const CHANNEL_SCORES = {
    'FORMULA 1': 1.0,
    'ESPN F1': 0.9,
    'THE RACE': 0.85,
    'Sky Sports F1': 0.85,
    'Autosport': 0.75,
    'F1 TV': 0.9,
};

// CONTENT CLASSIFIERS
const CLASSIFIERS = {
    RACE: /(Race Highlights?|Grand Prix.*Highlights?)/i,
    QUALI: /(Qualifying|Pole Lap|Q[123])/i,
    FP: /(FP[123]|Free Practice)/i,
    SPRINT: /(Sprint Highlights?|Sprint Quali)/i,
    REACTION: /(React|Driver[s']?\s*Interview|Debrief|Press Conference)/i,
    ANALYSIS: /(Explain|Analysis|Breakdown|Deep Dive|Why.*\?)/i,
    ONBOARD: /(Onboard|Pole Lap|Hot Lap)/i,
    NEWS: /(Announce|Confirm|Sign|Contract|Moving to|Breaking)/i,
    TECH: /(Tech Talk|Upgrade|Aero|Floor|Wing|Car Launch)/i,
};

// NON-ENGLISH PATTERNS
const NON_ENGLISH_PATTERNS = [
    /🔴\s*(LIVE|EN VIVO|AO VIVO|DIRECTO)/i,
    /\bEN VIVO\b/i, /\bAO VIVO\b/i, /\bDIRECTO\b/i,
    /\bCLASIFICACIÓN\b/i, /\bCORRIDA\b/i, /\bGP DE\b/i,
    /【.*】/, /[\u3040-\u309F\u30A0-\u30FF]/, /[\uAC00-\uD7AF]/, /[\u4E00-\u9FFF]/,
    /\bFORMULA EXTREMA\b/i, /\bTRIBUNA MOTORI\b/i,
];

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
                } catch (e) { reject(e); }
            });
        }).on('error', reject);
    });
}

// Parse ISO 8601 duration to seconds
function parseDuration(duration) {
    const match = duration?.match(/PT(\d+H)?(\d+M)?(\d+S)?/);
    if (!match) return 0;
    return (parseInt(match[1]) || 0) * 3600 +
        (parseInt(match[2]) || 0) * 60 +
        (parseInt(match[3]) || 0);
}

// Classify video content
function classifyVideo(title) {
    const tags = [];
    for (const [tag, pattern] of Object.entries(CLASSIFIERS)) {
        if (pattern.test(title)) tags.push(tag);
    }
    return tags.length > 0 ? tags : ['GENERAL'];
}

// Check if English
function isEnglish(item) {
    const audioLang = item.snippet?.defaultAudioLanguage;
    const defaultLang = item.snippet?.defaultLanguage;
    if (audioLang && !audioLang.startsWith('en')) return false;
    if (defaultLang && !defaultLang.startsWith('en') && !audioLang) return false;
    const title = item.snippet?.title || '';
    return !NON_ENGLISH_PATTERNS.some(p => p.test(title));
}

// Get channel score
function getChannelScore(channelTitle) {
    return CHANNEL_SCORES[channelTitle] || 0.5;
}

// Process video details into standard format
function processVideo(item) {
    const durationSec = parseDuration(item.contentDetails?.duration);
    const channelTitle = item.snippet.channelTitle;

    return {
        id: item.id,
        title: item.snippet.title,
        description: item.snippet.description?.substring(0, 500),
        thumbnail: item.snippet.thumbnails?.maxres?.url ||
            item.snippet.thumbnails?.high?.url ||
            item.snippet.thumbnails?.medium?.url,
        publishedAt: item.snippet.publishedAt,
        channelTitle: channelTitle,
        channelScore: getChannelScore(channelTitle),
        viewCount: item.statistics?.viewCount || '0',
        likeCount: item.statistics?.likeCount || '0',
        durationSec: durationSec,
        tags: classifyVideo(item.snippet.title),
        url: `https://www.youtube.com/watch?v=${item.id}`
    };
}

// Fetch video details for a list of IDs
async function fetchVideoDetails(videoIds) {
    if (videoIds.length === 0) return [];
    const url = `https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails,statistics&id=${videoIds.join(',')}&key=${API_KEY}`;
    const response = await fetchJson(url);
    return response.items || [];
}

// ═══════════════════════════════════════════════════════════════════
// FEED GENERATORS
// ═══════════════════════════════════════════════════════════════════

// 1. OFFICIAL FEED - F1 Channel uploads (uses playlistItems - 1 unit!)
async function fetchOfficialFeed() {
    console.log('📺 Fetching Official F1 feed...');
    const url = `https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=${F1_UPLOADS_PLAYLIST}&maxResults=50&key=${API_KEY}`;
    const response = await fetchJson(url);

    const videoIds = response.items?.map(item => item.snippet.resourceId.videoId) || [];
    const details = await fetchVideoDetails(videoIds);

    const videos = details
        .filter(item => parseDuration(item.contentDetails?.duration) > 60)
        .map(processVideo);

    console.log(`   ✅ ${videos.length} official videos`);
    return videos;
}

// 2. TRENDING FEED - Popular F1 content across channels
async function fetchTrendingFeed() {
    console.log('🔥 Fetching Trending feed...');
    const url = `https://www.googleapis.com/youtube/v3/search?part=snippet&q=Formula%201&type=video&order=viewCount&maxResults=50&key=${API_KEY}`;
    const searchResults = await fetchJson(url);

    const videoIds = searchResults.items?.map(item => item.id.videoId) || [];
    const details = await fetchVideoDetails(videoIds);

    const videos = details
        .filter(item => parseDuration(item.contentDetails?.duration) > 60)
        .filter(isEnglish)
        .map(processVideo);

    console.log(`   ✅ ${videos.length} trending videos`);
    return videos;
}

// 3. ANALYSIS FEED - Commentary and explainer content
async function fetchAnalysisFeed() {
    console.log('📊 Fetching Analysis feed...');
    const url = `https://www.googleapis.com/youtube/v3/search?part=snippet&q=F1%20analysis%20explained&type=video&order=date&maxResults=30&key=${API_KEY}`;
    const searchResults = await fetchJson(url);

    const videoIds = searchResults.items?.map(item => item.id.videoId) || [];
    const details = await fetchVideoDetails(videoIds);

    // Filter to trusted analysis channels + English
    const trustedChannels = ['THE RACE', 'ESPN F1', 'Autosport', 'Sky Sports F1', 'FORMULA 1', 'Chain Bear'];
    const videos = details
        .filter(item => parseDuration(item.contentDetails?.duration) > 60)
        .filter(isEnglish)
        .filter(item => trustedChannels.some(ch => item.snippet.channelTitle.includes(ch)) ||
            CLASSIFIERS.ANALYSIS.test(item.snippet.title))
        .map(processVideo);

    console.log(`   ✅ ${videos.length} analysis videos`);
    return videos;
}

// ═══════════════════════════════════════════════════════════════════
// MAIN
// ═══════════════════════════════════════════════════════════════════

async function run() {
    try {
        // Fetch all feeds in parallel
        const [official, trending, analysis] = await Promise.all([
            fetchOfficialFeed(),
            fetchTrendingFeed(),
            fetchAnalysisFeed()
        ]);

        // Save individual feeds
        fs.writeFileSync('f1_official.json', JSON.stringify(official, null, 2));
        fs.writeFileSync('f1_trending.json', JSON.stringify(trending, null, 2));
        fs.writeFileSync('f1_analysis.json', JSON.stringify(analysis, null, 2));

        // Create merged feed with deduplication
        const seenIds = new Set();
        const merged = [];

        // Priority: Official → Trending → Analysis
        for (const video of [...official, ...trending, ...analysis]) {
            if (!seenIds.has(video.id)) {
                seenIds.add(video.id);
                merged.push(video);
            }
        }

        // Sort merged by recency
        merged.sort((a, b) => new Date(b.publishedAt) - new Date(a.publishedAt));

        // Keep f1_youtube.json as the merged feed for backward compatibility
        fs.writeFileSync('data/f1_youtube.json', JSON.stringify(merged, null, 2));

        console.log('\n═══════════════════════════════════════');
        console.log(`💾 Saved: data/f1_official.json (${official.length})`);
        console.log(`💾 Saved: data/f1_trending.json (${trending.length})`);
        console.log(`💾 Saved: data/f1_analysis.json (${analysis.length})`);
        console.log(`💾 Saved: data/f1_youtube.json (${merged.length} merged)`);
        console.log('═══════════════════════════════════════');

    } catch (error) {
        console.error("❌ Failed:", error.message);
        process.exit(1);
    }
}

run();
