# Milestone 2 Pad 5 review

Install M2 over M1. The application ID and development signing key are preserved. Do not uninstall first; appearance settings should survive the update.

1. Launch and confirm the real library starts empty; design samples are separately available.
2. Add a small local folder with MP4, MKV, a non-video file and a nested folder. Cancel the picker once first; nothing should change.
3. Verify names, counts, search, duration/dimensions when Android can read them, and thumbnail placeholders for unsupported or damaged files. Episode hints should recognize S02E04 and 2x04; these are not authoritative metadata.
4. Rotate during a scan. Navigate away and back. The scan should continue without duplicates. Cancel a rescan and confirm the previous complete index remains.
5. Force-close and reopen; folders and index should persist. Restart the tablet and refresh to verify retained access.
6. Add a file to the folder using your file manager, refresh, then remove that file and refresh. Compare the index to the folder.
7. Import a USB folder via Android's picker if available. Disconnect while scanning and refresh while disconnected. The app should show a folder error and keep the prior index. Reconnect and refresh.
8. Revoke folder access externally if the OS exposes that control. Refresh should show access lost. Add the same folder again to restore access.
9. Add overlapping parent/child folders. Library should show each URI once; each folder should show its own entries. Removing one folder must leave the other indexed.
10. Remove a folder through the confirmation dialog. Check the actual files still exist. Restart and verify the removed folder stays removed.
11. Scroll a thumbnail-heavy list, test all themes, large font settings, and landscape/portrait. Report stutter, clipping or unreadable controls.

Playback remains disabled. A visible thumbnail or extracted duration does not establish AC-3/E-AC-3, HDR or video playback compatibility.

Known limits: scans are foreground process work, not a background service. Android may terminate the process; relaunch and refresh to retry. Cancel takes effect between provider/metadata operations, so a slow provider may delay it. Each selected folder is limited to 50,000 traversed entries; choose smaller folders when needed. Thumbnail memory is bounded to 12 MiB, with two extraction jobs. Disk thumbnail caching, automatic file watching and series grouping are deferred. USB support depends on the system DocumentsProvider. No device/emulator tests have been run in this workspace.
