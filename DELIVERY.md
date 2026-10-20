# Delivered test build

- Artifact: StatusSplitter-v1.4.1-repair.apk
- applicationId: com.statussplitter; versionCode 15; versionName 1.4.1-repair
- 5,106,608 bytes; SHA256 7485e57d49e4de3c4ddc2a743f721d369fb89652868fbe1e10058d0d6ea60a88
- Parent reran testDebugUnitTest assembleRelease: BUILD SUCCESSFUL in 39s; parsed reports: 18 tests, zero failures/errors/skips.
- Signed using local Android debug keystore for testing. apksigner verification passed v2/v3.
- WhatsApp bridge send-media returned success, messageId 3EB06F37068EA18C80566B (not a read receipt).
- No device connected: install, video codec path, playback, WhatsApp UI not device-tested.
- Independent reviewer found versionCode downgrade conflict after Marvis moved original to code14; corrected repaired copy to15, rebuilt and checked APK metadata.
- Marvis original not modified. This repair snapshot does NOT merge Marvis's concurrent newer changes.
- Full Auto disabled, supervised sharing requires user confirmation. Compression not implemented; unsupported codecs/oversized clips rejected explicitly.
- README refers to intermediate v1.3.9 unsigned artifact; this delivery record supersedes its artifact/version/signing status.
