#!/usr/bin/env python
from __future__ import print_function
import os
import pickle
import platform
import random
import re
import string
import sys
from time import sleep
import traceback
import unittest


#from flaky import flaky
import py
import pytest


class FooTests(unittest.TestCase):

    def test_foo_one(self):
        result = os.system('python -V')
        self.assertEqual(result, 0)


if __name__ == '__main__':

    dirname = "test-reports-%s%s" % (sys.version_info[0], sys.version_info[1])
    unittest.main()
