# BOXBOXBOX v1.13.0 Release Notes

## Video Curation System

- New multi-feed backend system generating 4 specialized JSON feeds (official, trending, analysis, merged)
- Smart scoring algorithm combining freshness, engagement, channel quality, and content type
- 5 filter chips: Hot, Latest, Highlights, Popular, Official
- F1 official channel debuff (0.6x score penalty) with faster age decay to balance content diversity
- Channel diversity enforcement: maximum 3 videos from same channel per 6-video window in Popular filter
- Highlights filter with strict keyword matching, excluding shorts, reactions, F2, and F3 content
- English-only video filtering at backend level
- 3-source merge (RSS + f1_youtube.json + f1_highlights.json) with deduplication
- Age decay thresholds: week-level for Hot filter, year-level for Popular filter

## Hotlap Game Enhancements

- New sound effects: countdown beeps, engine loop, tire screech, sector time sounds (personal best, world record, slow), finish sounds, DNF sound
- 8 new car sprite frames for smoother animation
- Physics and mechanics improvements
- Sector timing bug fixes
- Updated game thumbnail images

## Daily Mix Redesign

- 2-row bento grid layout with horizontal scroll
- Icon badges for content type identification
- Larger cards with improved visual impact
- Podcast artwork backgrounds

## Social Feed Improvements

- Engagement-based sorting with time decay algorithm
- F1 official account debuff (0.4x to 0.5x) for fairer content ranking
- Video-only reels feed (excludes image-only carousels)
- Auto-refresh on video load error for expired CDN URLs

## Highlights Integration

- In-app YouTube player with forced landscape orientation
- Race Highlights section in RaceDetailScreen and SessionResultsScreen
- Historical highlights display for upcoming races (previous year content)
- Extended highlights coverage from March 2024 onwards

## Infrastructure

- Separate GitHub Actions workflows for Instagram and YouTube feeds
- Manual notification workflow for testing
- YouTube feed workflow runs every 3 hours for quota efficiency
