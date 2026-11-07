from pathlib import Path
import tempfile
import unittest
import zipfile

from release_bundle import bundle


class ReleaseBundleTest(unittest.TestCase):
    def fixture(self, base):
        root, emoji = base / 'rta', base / 'emoji'
        (root / 'rta/target').mkdir(parents=True)
        (emoji / 'target').mkdir(parents=True)
        (emoji / 'src/main/resources').mkdir(parents=True)
        (root / 'rta/pom.xml').write_text('<project><version>3.5-RG</version><properties><emoji.version>1</emoji.version></properties></project>')
        (emoji / 'pom.xml').write_text('<project><version>1</version></project>')
        (emoji / 'src/main/resources/LICENSE').write_text('original notice')
        for parent, name in [(root / 'rta/target', f'rich-text-area-3.5-RG{c}.jar') for c in ('', '-sources', '-javadoc')] + [(emoji / 'target', 'emoji-noto-1.jar')]:
            with zipfile.ZipFile(parent / name, 'w') as archive:
                archive.writestr('example', 'content')
        (root / 'releases').mkdir()
        for name in ('README.md', 'LICENSE', 'RELEASING.md', 'releases/3.5-RG.md'):
            (root / name).write_text('documentation')
        return root, emoji

    def test_bundle_contains_dependency_sources_notices_maven_layout_and_checksums(self):
        with tempfile.TemporaryDirectory() as directory:
            root, emoji = self.fixture(Path(directory))
            archive = bundle(root, emoji)
            with zipfile.ZipFile(archive) as zipped:
                self.assertIn('repository/com/gluonhq/rich-text-area/3.5-RG/rich-text-area-3.5-RG.jar', zipped.namelist())
                self.assertIn('repository/de/richmarkdown/emoji-noto/1/emoji-noto-1.pom', zipped.namelist())
                self.assertEqual(b'original notice', zipped.read('sources/emoji-noto/src/main/resources/LICENSE'))
                self.assertFalse(any('/target/' in name for name in zipped.namelist()))
            self.assertEqual(7, len((archive.parent / 'SHA256SUMS').read_text().splitlines()))

    def test_missing_or_wrong_dependency_never_produces_release_archive(self):
        for broken in ('missing', 'version'):
            with self.subTest(broken=broken), tempfile.TemporaryDirectory() as directory:
                root, emoji = self.fixture(Path(directory))
                if broken == 'missing':
                    (emoji / 'target/emoji-noto-1.jar').unlink()
                else:
                    (emoji / 'pom.xml').write_text('<project><version>2</version></project>')
                with self.assertRaises(ValueError):
                    bundle(root, emoji)
                self.assertFalse((root / 'target/release').exists())
