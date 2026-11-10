#!/usr/bin/env python3
"""The same build and packaged-JAR checks used by pre-push and CI."""
import argparse
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--isolated', action='store_true',
                    help='Build a temporary source copy without changing tracked build outputs')
args = parser.parse_args()


def verify(directory):
    subprocess.run(['mvn', '--batch-mode', '--no-transfer-progress', 'clean', 'verify'],
                   cwd=directory, check=True)
    subprocess.run([sys.executable, str(directory / 'tests/verify-integration.py')],
                   cwd=directory, check=True)


if args.isolated:
    with tempfile.TemporaryDirectory(prefix='cq-pre-push-') as temporary:
        checkout = Path(temporary)
        shutil.copy2(root / 'pom.xml', checkout / 'pom.xml')
        for directory in ('src', 'tests', '.mvn'):
            if (root / directory).exists():
                shutil.copytree(root / directory, checkout / directory)
        verify(checkout)
else:
    verify(root)
