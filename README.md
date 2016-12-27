# Challenger
Challenger was built as a learning application, but it serves a useful purpose for me. It tracks my person walking goals. Specifically, I walk 1K miles per year. I target walking 90 miles per month so I have extra days in December to hit my goal if I fall behind.

I track my walks with Endomondo which is integrated with Google Fit. Challenger retrieves my  miles walked this year from Google Fit. I specify my targeted monthly pace. Challenger lets me know how many days I am from my target.

# Versions
Version 1 was built as an Android application and was not generalized at all. It retrieves miles walked from the Google Fit API but does all calculation locally.

Version 2 is being built as a Node.js application with a REST API, a Web UI, and a natural language UI (via API.AI). Version 2 is also being built to be a bit more generalized, but is still primarily focused on my personal use case.
