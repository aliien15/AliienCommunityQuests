#!/usr/bin/env python3
"""After mvn clean verify, inspect packaging and smoke-test embedded JDBC drivers."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET
import os
import re
import subprocess
import tempfile
import zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--offline', action='store_true', help='Use cached Maven dependencies only')
args = parser.parse_args()
pom = ET.parse(root / 'pom.xml').getroot()
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
artifact = pom.findtext('m:artifactId', namespaces=ns)
version = pom.findtext('m:version', namespaces=ns)
jar = root / f'target/{artifact}-{version}.jar'
private = 'com/aliiensmp/aliienCommunityQuests/core/'
with zipfile.ZipFile(jar) as archive:
    names = set(archive.namelist())
    for cls in ('AliienCore', 'lib/hikari/HikariDataSource', 'lib/boostedyaml/YamlDocument'):
        assert private + cls + '.class' in names, cls
    assert not any(name.startswith(('com/aliiensmp/core/', 'com/zaxxer/hikari/',
                                    'dev/dejvokep/boostedyaml/', 'com/mysql/', 'org/mariadb/jdbc/',
                                    'org/bukkit/', 'me/clip/placeholderapi/')) for name in names)
    driver_prefixes = ('org/h2/', 'org/sqlite/', 'com/mysql/', 'org/mariadb/jdbc/',
                       private + 'lib/mysql/', private + 'lib/mariadb/')
    assert not any(name.startswith(driver_prefixes) for name in names), 'Drivers must not be bundled'
    if 'META-INF/services/java.sql.Driver' in names:
        registrations = archive.read('META-INF/services/java.sql.Driver').decode()
        assert not any(line.split('#')[0].strip() for line in registrations.splitlines()), registrations
print('Framework relocation and external-driver packaging passed', flush=True)

maven = ['mvn', '--batch-mode', '--no-transfer-progress']
if args.offline:
    maven.append('--offline')
subprocess.run([*maven, 'dependency:build-classpath',
                '-Dmdep.includeScope=test', '-Dmdep.outputFile=target/integration-classpath.txt'],
               cwd=root, check=True)
# Keep server APIs on the harness classpath, but exercise framework code from the final JAR.
server_classpath = (root / 'target/integration-classpath.txt').read_text().strip().split(os.pathsep)
server_classpath = [path for path in server_classpath
                    if '/com/aliiensmp/AliienCore/' not in path.replace('\\', '/')]
# Resolve exactly the runtime libraries declared in plugin.yml, including their dependencies.
descriptor = (root / 'src/main/resources/plugin.yml').read_text()
block = re.search(r'(?m)^libraries:\n((?:[ \t]+.*\n)*)', descriptor)
if block is None:
    raise SystemExit('Missing server library declarations')
libraries = re.findall(r'(?m)^  - ([^\s]+)\s*$', block.group(1))
expected = {'com.mysql:mysql-connector-j', 'org.mariadb.jdbc:mariadb-java-client',
            'org.xerial:sqlite-jdbc', 'com.h2database:h2'}
assert {':'.join(library.split(':')[:2]) for library in libraries} == expected
with tempfile.TemporaryDirectory(prefix='cq-integration-') as output:
    output = Path(output)
    dependency_pom = ET.Element('project', xmlns=ns['m'])
    for key, value in [('modelVersion', '4.0.0'), ('groupId', 'integration'),
                       ('artifactId', 'server-libraries'), ('version', '1')]:
        ET.SubElement(dependency_pom, key).text = value
    dependencies = ET.SubElement(dependency_pom, 'dependencies')
    for library in libraries:
        dependency = ET.SubElement(dependencies, 'dependency')
        for key, value in zip(('groupId', 'artifactId', 'version'), library.split(':')):
            ET.SubElement(dependency, key).text = value
    pom_path = output / 'pom.xml'
    ET.ElementTree(dependency_pom).write(pom_path, encoding='unicode')
    driver_directory = output / 'libraries'
    subprocess.run([*maven, '-f', str(pom_path),
                    'org.apache.maven.plugins:maven-dependency-plugin:3.7.0:copy-dependencies',
                    '-DincludeScope=runtime', f'-DoutputDirectory={driver_directory}'], check=True)
    classpath = os.pathsep.join([str(jar), *server_classpath,
                                *(str(path) for path in sorted(driver_directory.glob('*.jar')))])
    subprocess.run(['javac', '-proc:none', '-cp', classpath, '-d', str(output),
                    str(root / 'tests/IntegrationSmoke.java')], check=True)
    subprocess.run(['java', '-cp', os.pathsep.join((str(output), classpath)), 'IntegrationSmoke'], check=True)
