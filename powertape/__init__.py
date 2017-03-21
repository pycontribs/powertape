# -*- coding: utf-8 -*-
"""Some documentation for PowerTape Python package that does nothing."""
from __future__ import unicode_literals
from pbr.version import VersionInfo

_v = VersionInfo(__name__).semantic_version()
__version__ = _v.release_string()
version_info = _v.version_tuple()


__all__ = (
    '__version__'
)
