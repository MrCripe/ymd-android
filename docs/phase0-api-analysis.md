# Phase 0 — Yandex Music API Analysis

## Sources
- Official docs index: https://ym.marshal.dev/ (HTML scraped to `docs/api-docs.html`)
- Reference Python client: https://github.com/MarshalX/yandex-music-api (used in `py-ref/ymd/`)
- Local Python source: `py-ref/ymd/api.py`, `py-ref/ymd/cli.py`, `py-ref/ymd/core.py`

## Endpoint Map (port from py-ref)

| Action | Endpoint | Notes |
|---|---|---|
| Get download URL + key | `GET https://api.music.yandex.net/get-file-info` | HMAC-SHA256 signed |
| Tracks by IDs | `GET /tracks?track-ids=<id>,<id>` | comma-separated |
| Albums with tracks | `GET /albums/{id}/with-tracks` | returns volumes |
| Artist direct albums | `GET /artists/{id}/direct-albums?page=<n>` | pager: page/per_page/total |
| Artist info | `GET /artists/{id}` | counts: tracks |
| User playlists | `GET /users/{user}/playlists/{kind}` | playlist + tracks |
| Playlist tracks | `GET /users/{user}/playlists/{kind}?page-size=10` | paginated |
| Track lyrics (text) | `GET /tracks/{id}/lyrics` | format=`TEXT` |
| Track lyrics (lrc)  | `GET /tracks/{id}/lyrics` | format=`LRC` |
| Cover bytes | `GET https://{cdn}/.../{size}x{size}` or `orig` | from track.cover_uri |

## Signing (HMAC-SHA256)

`DEFAULT_SIGN_KEY = "p93jhgh689SBReK6ghtw62"` (confirmed via `yandex_music.utils.sign_request`).

For `get-file-info`:
```
params = {
    "ts": <unix_timestamp>,
    "trackId": <track_id>,
    "quality": "lq" | "nq" | "lossless",
    "codecs": "flac,flac-mp4,mp3,aac,he-aac,aac-mp4,he-aac-mp4",
    "transports": "encraw"
}
sign = base64( HMAC-SHA256(DEFAULT_SIGN_KEY, "".join(str(v) for v in params.values()).replace(",", "")) )
       // trim trailing '='
params["sign"] = sign
```
Note Python code: `[:-1]` trims last char of base64 — that's the final `=` padding.

## AES-CTR Decryption

```python
AES.new(key=bytes.fromhex(hex_key), nonce=bytes(12), mode=AES.MODE_CTR)
```
- Counter starts at 0 (initial counter block = nonce ‖ 0x00000000)
- No padding
- Kotlin: `javax.crypto.Cipher` with `AES/CTR/NoPadding`; same key/IV via `IvParameterSpec(ByteArray(12))`

## Quality Mapping

| Enum | ApiQuality | Container | Codec |
|---|---|---|---|
| LOW=0 | lq | MP4 | AAC 64kbps |
| NORMAL=1 | nq | MP4 | AAC 192kbps |
| LOSSLESS=2 | lossless | FLAC or MP4 (flac-mp4) | FLAC |

Suffix rule (`to_downloadable_track`):
- Container=MP3 → `.mp3`
- Container=MP4 → `.flac` (codec contains "flac") else `.m4a`
- Container=FLAC → `.flac`

## URL Regex (`py-ref/ymd/cli.py`)

```
TRACK_RE    = track/(\d+)
ALBUM_RE    = album/(\d+)$
ARTIST_RE   = artist/(\d+)$
PLAYLIST_RE = ([\w\-._@]+)/playlists/(\d+)$
FETCH_PAGE_SIZE = 10
```

## Path Pattern

Default: `#album-artist/#album/#number - #title`
Placeholders:
- `#number` / `#number-padded` — track index (padded to len(total))
- `#track-artist` — first track artist
- `#album-artist` — first album artist
- `#title` — `track.title (track.version)` if version present
- `#album` — `album.title (album.version)`
- `#year` — release year (int)
- `#artist-id`, `#album-id`, `#track-id`

Sanitization:
- Safe mode: `SAFE_PATH_CLEAR_RE = re.compile(r"([^\w\-\'() ]|^\s+|\s+$)"` → `_`
- Unsafe mode: `UNSAFE_PATH_CLEAR_RE = re.compile(r"[/\\]+")` → `_` (only slash)
- Each part trimmed to `MAX_FILE_NAME_LENGTH_WITHOUT_SUFFIX = 255 - 4 = 251`

## Tags (set via jaudiotagger/vorbis-java)

| Frame | MP3 (ID3) | MP4 | FLAC |
|---|---|---|---|
| Title | TIT2 | ©nam | title |
| Album | TALB | ©alb | album |
| Artist | TPE1 | ©ART | artist |
| Album artist | TPE2 | aART | albumartist |
| Year | TDRC | ©day | date |
| Track# | TRCK | trkn[(N,0)] | tracknumber |
| Disc# | TPOS | disk[(N,0)] | discnumber |
| Genre | TCON | ©gen | genre |
| Lyrics | USLT | ©lyr | lyrics |
| Cover | APIC | covr[MP4Cover] | add_picture(Picture) |
| URL | WOAF | ©cmt | comment |

MP4 compat level 1: artists joined `"; "`; level 0: list.

## Lyrics Files

- `--lyrics-format lrc` → sidecar `<base>.lrc` via `tracks.getLyrics(format="LRC").fetchLyrics()`
- `--lyrics-format text` → embedded in tag only (no sidecar)

## Cover

- `track.coverUri` → URL like `avatars.yandex.net/get-music-content/.../%%/%%`
- Size replacement: `{size}x{size}` (e.g. `400x400`) or `orig`
- MIME detected via magic bytes (JPEG: `FF D8 FF`; PNG: `89 50 4E 47 0D 0A 1A 0A`)
- Save as `cover.jpg` / `cover.png` next to track when embed_cover=false
- Embed only when embed_cover=true; cache by `album.id`

## Constants

- `DEFAULT_COVER_RESOLUTION = 400`
- `MIN_COMPATIBILITY_LEVEL = 0`, `MAX_COMPATIBILITY_LEVEL = 1`
- `AUDIO_FILE_SUFFIXES = {.mp3, .flac, .m4a}`
- `TEMPORARY_FILE_NAME_TEMPLATE = ".yandex-music-downloader.{}.tmp"` ({} = sha256 of target name)
- `DEFAULT_PATH_PATTERN = Path("#album-artist", "#album", "#number - #title")`

## Network (Ktor)

- Timeout: 20s (configurable)
- Retries: 20 default; 0 = infinite; delay 5s
- Retry only on `NetworkError`

## Atomic Write

`write_via_temporary_file(data, target_path, hook=None)`:
1. Compute temp name: `.yandex-music-downloader.<sha256(target.name)>.tmp` in target.parent
2. Write bytes to temp
3. Run `hook(temp_path) → final_path` (used for FLAC pseudo-repack + tag writing)
4. `temp.rename(final_path)` (atomic on same FS)
