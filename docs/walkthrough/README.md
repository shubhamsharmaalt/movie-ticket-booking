# Code walkthrough video

[Watch the 4 minute 50 second walkthrough](movie-ticket-booking-walkthrough.mp4).

The video uses actual source excerpts, highlighted lines, synthetic English narration, and burned-in captions. It covers the major classes and functions through the booking flow.

| Time | Topic |
| --- | --- |
| 00:00 | Application entry point, packages, and configuration |
| 00:27 | Security and controllers |
| 00:54 | Request records and API errors |
| 01:16 | Catalog management and show creation |
| 01:45 | Entities, repositories, and database locks |
| 02:15 | Seat holds and concurrent allocation |
| 02:36 | Pricing and discounts |
| 02:55 | Simulated payment and confirmation |
| 03:15 | Cancellation and refunds |
| 03:34 | Automatic expiry |
| 03:53 | Background notifications |
| 04:16 | Tests, setup, and verification limits |

Supporting files:

- [Transcript](transcript.md)
- [SRT captions](walkthrough.srt)
- [Editable scenes and narration](scenes.json)
- [Renderer](render_walkthrough.py)
- [Timing and source-hash manifest](render-manifest.json)

The MP4 is 1920 × 1080 at 30 fps, with H.264 video and AAC audio. It is approximately 12.3 MiB and includes chapter metadata. The voice is macOS Samantha.

To regenerate on macOS, the optional video tooling requires Python with Pillow, FFmpeg, and the built-in `say` command:

```bash
python3 docs/walkthrough/render_walkthrough.py narrate
python3 docs/walkthrough/render_walkthrough.py build
```

Intermediate frames and audio are written to `target/walkthrough-build/`. The renderer reads the current source line ranges in `scenes.json`; revise those ranges if the implementation changes. `mvn clean` removes the intermediate media but preserves this folder.

The source review also corrected the optional PostgreSQL test's `spring.datasource.username` configuration key. The local suite passes 17 tests; the PostgreSQL test remains skipped without `TEST_DB_URL`. The video describes that verification limit and the simulated payment/refund and REST-inbox choices explicitly.
