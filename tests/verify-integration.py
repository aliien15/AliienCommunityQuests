#!/usr/bin/env python3
"""After mvn clean verify, inspect packaging and smoke-test embedded JDBC drivers."""
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET
import os
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
    for cls in ('AliienCore', 'lib/hikari/HikariDataSource', 'lib/boostedyaml/YamlDocument',
                'lib/mysql/cj/jdbc/Driver', 'lib/mariadb/Driver'):
        assert private + cls + '.class' in names, cls
    assert not any(name.startswith(('com/aliiensmp/core/', 'com/zaxxer/hikari/',
                                    'dev/dejvokep/boostedyaml/', 'com/mysql/', 'org/mariadb/jdbc/',
                                    'org/bukkit/', 'me/clip/placeholderapi/')) for name in names)
    for cls in ('org/h2/Driver.class', 'org/sqlite/JDBC.class'):
        assert cls in names, f'Missing bundled driver: {cls}'
    assert any(name.startswith('org/sqlite/native/') for name in names), 'Missing SQLite native libraries'
    providers = archive.read('META-INF/services/java.sql.Driver').decode().splitlines()
    for provider in providers:
        provider = provider.split('#')[0].strip()
        if provider:
            assert provider.replace('.', '/') + '.class' in names, provider
    assert any('lib.mysql' in provider for provider in providers)
    assert any('lib.mariadb' in provider for provider in providers)
    assert 'org.h2.Driver' in providers
    assert 'org.sqlite.JDBC' in providers
print('Relocation and JDBC service packaging passed', flush=True)

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
# Drivers must come from the final plugin JAR, with no separately downloaded copies.
classpath = os.pathsep.join([str(jar), *server_classpath])
with tempfile.TemporaryDirectory(prefix='cq-integration-') as output:
    subprocess.run(['javac', '-proc:none', '-cp', classpath, '-d', output,
                    str(root / 'tests/IntegrationSmoke.java')], check=True)
    subprocess.run(['java', '-cp', os.pathsep.join((output, classpath)), 'IntegrationSmoke'], check=True)
