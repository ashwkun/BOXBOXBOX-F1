const fs = require('fs');
const https = require('https');

// CONFIG
const OUTPUT_FILE = 'f1_highlights.json';
const PLAYLIST_ID = 'UUB_qr75-ydFVKSF9Dmo6izg'; // F1 Channel Uploads (Derived from user provided Channel ID)
const START_DATE = new Date('2025-03-01T00:00:00Z');
const MAX_PAGES = 100; // Increased to cover full season history

// SECRETS
const API_KEY = process.env.YOUTUBE_API_KEY;

if (!API_KEY) {
    console.error("❌ Error: YOUTUBE_API_KEY missing.");
    process.exit(1);
}

// Regex for allowed titles
// Examples:
// Race Highlights | 2025 Qatar Grand Prix
// FP3 Highlights | 2025 Abu Dhabi Grand Prix
// Qualifying Highlights | 2025 Abu Dhabi Grand Prix
// Sprint Qualifying Highlights | 2025 United States Grand Prix
// Sprint Highlights | 2025 Qatar Grand Prix
const TITLE_REGEX = /^(Race|FP1|FP2|FP3|Qualifying|Sprint|Sprint Qualifying)\sHighlights\s\|\s(\d{4})\s(.+)\sGrand Prix$/i;

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

async function run() {
    try {
        // 1. Load Existing Data
        let existingVideos = [];
        if (fs.existsSync(OUTPUT_FILE)) {
            try {
                existingVideos = JSON.parse(fs.readFileSync(OUTPUT_FILE));
                console.log(`📚 Loaded ${existingVideos.length} existing videos.`);
            } catch (e) {
                console.warn("⚠️ Corrupt existing file, starting fresh.");
            }
        }
        const existingIds = new Set(existingVideos.map(v => v.id));

        console.log(`📡 Fetching F1 Highlights since ${START_DATE.toISOString()}...`);

        let newVideos = [];
        let nextPageToken = '';
        let pageCount = 0;
        let keepFetching = true;
        let duplicateMatchCount = 0;

        while (keepFetching && pageCount < MAX_PAGES) {
            pageCount++;
            console.log(`   📄 Fetching Page ${pageCount}...`);

            const url = `https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&playlistId=${PLAYLIST_ID}&maxResults=50&key=${API_KEY}&pageToken=${nextPageToken}`;

            const response = await fetchJson(url);
            const items = response.items || [];

            if (items.length === 0) {
                console.log("   ⚠️ No items found on this page.");
                break;
            }

            for (const item of items) {
                const pubDate = new Date(item.contentDetails.videoPublishedAt);
                const title = item.snippet.title;
                const videoId = item.contentDetails.videoId;

                // Stop if we go before March 1, 2025
                if (pubDate < START_DATE) {
                    console.log(`   🛑 Reached cutoff date: ${pubDate.toISOString()}`);
                    keepFetching = false;
                    break;
                }

                // Check for duplicates (Stop Condition)
                if (existingIds.has(videoId)) {
                    console.log(`   found existing video: "${title}"`);
                    duplicateMatchCount++;
                    if (duplicateMatchCount >= 2) {
                        console.log("   🛑 Found 2 consecutive matches. Stopping fetch (Incremental Update).");
                        keepFetching = false;
                        break;
                    }
                    continue; // Skip adding, it's already in existing
                }

                // Reset duplicate counter if we find a new video (interleaved?)
                // Actually, strictly speaking, if we find a video that is NOT in highlights, it's just ignored.
                // If we find a video that IS in highlights but not in our file (weird?), we add it. 
                // We shouldn't reset duplicateMatchCount because API returns sorted. 
                // Once we hit duplicates, we are in the "old" territory. 
                // However, user asked for "2 for better strictness".
                // If we find 1 duplicate, then a non-duplicate (new?), then 1 duplicate, that's weird.
                // But generally, the feed is chronological.
                // We will NOT reset the counter, effectively looking for 2 accumulated matches to stop.

                // Filter by Title Format
                if (TITLE_REGEX.test(title)) {
                    // Extract Session and Race info
                    const match = title.match(TITLE_REGEX);
                    const sessionType = match[1];
                    const year = match[2];
                    const raceName = match[3];

                    newVideos.push({
                        id: videoId,
                        title: title,
                        thumbnail: item.snippet.thumbnails.maxres?.url || item.snippet.thumbnails.high?.url,
                        publishedAt: item.contentDetails.videoPublishedAt,
                        sessionType: sessionType,
                        year: year,
                        raceName: raceName,
                        url: `https://www.youtube.com/watch?v=${videoId}`
                    });
                }
            }

            nextPageToken = response.nextPageToken;
            if (!nextPageToken) {
                console.log("   🛑 No more pages.");
                break;
            }
        }

        console.log(`✅ Fetched ${newVideos.length} NEW highlights.`);

        // Merge New + Existing
        // Since we are prepending new ones, and we filtered duplicates, this is clean.
        const allVideos = [...newVideos, ...existingVideos];

        // Sort by Date (Newest first)
        allVideos.sort((a, b) => new Date(b.publishedAt) - new Date(a.publishedAt));

        // Save
        fs.writeFileSync(OUTPUT_FILE, JSON.stringify(allVideos, null, 2));
        console.log(`💾 Saved ${allVideos.length} total videos to ${OUTPUT_FILE}`);

    } catch (error) {
        console.error("❌ Failed:", error.message);
        process.exit(1);
    }
}

run();
