# Fork builds and releases

This repository publishes GitHub releases for the RainerGa fork. It does not
deploy fork versions to Gluon's Maven Central namespace.

Use JDK 25. JavaFX is pinned to 25.0.4. The Noto dependency and integration tests
come from Mardexas commit `cc3ed08826ba43f7bdb912ddeaddd1fa2885535b`:

```sh
git clone https://github.com/RainerGa/Mardexas.git integration
git -C integration checkout cc3ed08826ba43f7bdb912ddeaddd1fa2885535b
mvn -B -ntp -f integration/emoji-noto/pom.xml install
mvn -B -ntp -f rta/pom.xml -Pfork-tests -Dmaven.source.skip=false -Dmaven.javadoc.skip=false install
mvn -B -ntp -f samples/pom.xml verify
mvn -B -ntp -f integration/pom.xml -Drichtextarea.version=3.5-RG verify
python3 -B -m unittest discover -s scripts -p 'test_*.py'
python3 scripts/release_bundle.py --emoji integration/emoji-noto
```

Do not initialize the integration checkout's submodule: the editor must use the
newly installed library rather than rebuild its older pinned revision.

The `fork-tests` profile runs compatible buffer/table/emoji/serialization tests
and current-API feature regressions. The default build retains the existing
test-skip settings for embedding in Mardexas. Legacy GUI tests and the old
Field/Style/StyleRegistry/PageFormat test classes target APIs this implementation
does not expose and still require porting; they are not claimed as passing.
The pinned editor suite supplies current GUI, caret, selection, undo/redo,
table and page-layout contracts on Linux and Windows.

Update all three POMs and the samples' parent version together. Add release notes
under `releases/<version>.md`. Push `main`, inspect its Build run, then push an
annotated tag matching the version exactly. The tag's Release workflow requires
a successful two-platform `main` push build (Linux and Windows)
for that exact commit, downloads
its retained assets and verifies every SHA-256 checksum before publishing.
It never uses a different commit's artifacts or a pull-request build. If the
main build failed or its artifacts expired, publication fails. Rerun that
commit's main push build before retrying the release. A dispatch on a tag retries
publication; a branch dispatch does not publish. No Sonatype or GPG secrets are
required.

Release assets include library, sources and Javadoc JARs, both Maven POMs, the
required `emoji-noto` JAR, a dependency/source bundle and SHA-256 checksums.
To consume the bundle, extract it and add its `repository/` directory as a local
Maven repository, or install the two binary JARs with their POMs using
`mvn install:install-file -Dfile=<jar> -DpomFile=<pom>`. Install emoji-noto first.
JavaFX remains an external, platform-specific dependency supplied by the host.
